package com.fixlink.application.service;

import com.fixlink.adapter.out.persistence.entity.QuotationJpaEntity;
import com.fixlink.adapter.out.persistence.entity.RepairRequestJpaEntity;
import com.fixlink.adapter.out.persistence.entity.TechnicianProfileJpaEntity;
import com.fixlink.adapter.out.persistence.entity.UserJpaEntity;
import com.fixlink.adapter.out.persistence.repository.SpringDataQuotationRepository;
import com.fixlink.adapter.out.persistence.repository.SpringDataRepairRequestRepository;
import com.fixlink.adapter.out.persistence.repository.SpringDataTechnicianProfileRepository;
import com.fixlink.adapter.out.persistence.repository.SpringDataUserRepository;
import com.fixlink.adapter.out.persistence.repository.SpringDataWorkProgressRepository;
import com.fixlink.application.port.in.QuotationUseCase;
import com.fixlink.domain.exception.DomainException;
import com.fixlink.domain.model.QuotationStatus;
import com.fixlink.domain.model.RequestStatus;
import com.fixlink.domain.model.Role;
import com.fixlink.domain.model.UserStatus;
import com.fixlink.domain.model.VerificationStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Race condition của luồng "Nhận việc" (pivot first-come-first-served).
 *
 * <p>Đây là bài test quan trọng nhất của lần pivot: mô hình mới bỏ bước khách
 * xác nhận, nên không còn "chỗ đệm" nào giữa hai thợ bấm nhận cùng lúc — khóa ở
 * tầng DB phải chặn được thật.
 *
 * <p>Test <b>bắn hai lời gọi song song thật</b> (hai thread, đồng bộ bằng
 * {@link CyclicBarrier} để cùng vào một thời điểm) và gọi trực tiếp vào service
 * để chạy qua đúng transaction + {@code SELECT ... FOR UPDATE} thật, thay vì gọi
 * tuần tự rồi suy luận.
 *
 * <p>Lớp test này <b>không</b> đánh {@code @Transactional}: mỗi thread cần
 * transaction riêng và dữ liệu phải được commit thật, nếu bọc transaction test
 * thì sẽ không còn tranh chấp nào để đo.
 */
@SpringBootTest
@ActiveProfiles("test")
class ApplyForJobConcurrencyTest {

    private static final String TECH_A = "tech01";
    private static final String TECH_B = "tho_dien_lanh_01";
    private static final long CATEGORY_ID = 1L;
    private static final long AREA_ID = 1L;
    private static final BigDecimal BUDGET = new BigDecimal("800000");

    @Autowired
    private QuotationUseCase quotationUseCase;

    @Autowired
    private SpringDataRepairRequestRepository requestRepo;

    @Autowired
    private SpringDataQuotationRepository quotationRepo;

    @Autowired
    private SpringDataUserRepository userRepo;

    @Autowired
    private SpringDataTechnicianProfileRepository techProfileRepo;

    @Autowired
    private SpringDataWorkProgressRepository progressRepo;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private Long techAId;
    private Long techBId;
    private Long customerId;

    /**
     * H2 in-memory dùng chung cho cả JVM test và lớp này không bọc
     * {@code @Transactional}, nên mọi thay đổi đều commit thật. Phải trả nguyên
     * trạng hồ sơ thợ sau mỗi test, nếu không sẽ làm hỏng các test khác vốn
     * mong thợ ở trạng thái PENDING (rò trạng thái phụ thuộc thứ tự chạy).
     */
    private final Map<Long, VerificationStatus> originalVerification = new HashMap<>();
    private final Map<Long, LinkedHashSet<Long>> originalCategories = new HashMap<>();
    private final Map<Long, LinkedHashSet<Long>> originalAreas = new HashMap<>();
    private final java.util.List<Long> createdRequestIds = new java.util.ArrayList<>();

    /** Thợ được tạo mới trong test (không phải seed), phải xóa hẳn khi dọn. */
    private final java.util.List<Long> createdTechIds = new java.util.ArrayList<>();

    @BeforeEach
    void setUp() {
        techAId = approveTechnician(TECH_A);
        techBId = approveTechnician(TECH_B);
        customerId = userRepo.findByUsernameAndDeletedAtIsNull("customer01").orElseThrow().getId();
    }

    @AfterEach
    void tearDown() {
        for (Long requestId : createdRequestIds) {
            // work_progress có FK trỏ về repair_requests nên phải xóa trước.
            progressRepo.deleteAll(progressRepo.findByRequestIdOrderByCreatedAtAsc(requestId));
            quotationRepo.deleteAll(quotationRepo.findByRequestIdOrderByCreatedAtDesc(requestId));
            requestRepo.findById(requestId).ifPresent(requestRepo::delete);
        }
        createdRequestIds.clear();

        originalVerification.forEach((techId, status) -> {
            TechnicianProfileJpaEntity profile = techProfileRepo.findById(techId).orElseThrow();
            profile.setVerificationStatus(status);
            profile.setCategoryIds(originalCategories.get(techId));
            profile.setAreaIds(originalAreas.get(techId));
            techProfileRepo.save(profile);
        });
        originalVerification.clear();
        originalCategories.clear();
        originalAreas.clear();

        // Thợ tạo mới trong test: xóa profile trước rồi tới user (FK user_id).
        for (Long techId : createdTechIds) {
            techProfileRepo.findById(techId).ifPresent(techProfileRepo::delete);
            userRepo.findById(techId).ifPresent(userRepo::delete);
        }
        createdTechIds.clear();
    }

    /**
     * Tạo một thợ mới, đã APPROVED và khớp category + area, dùng cho test nhiều thợ.
     *
     * <p>Vì {@code @MapsId} cần entity user còn được quản lý (managed) đúng lúc
     * persist profile, phải tạo cả user lẫn profile trong <b>cùng một transaction</b>;
     * nếu để hai lần {@code save()} riêng, user sẽ bị detach và persist báo lỗi.
     */
    private Long createApprovedTechnician(int index) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        Long techId = tx.execute(statusIgnored -> {
            UserJpaEntity user = new UserJpaEntity();
            user.setUsername("race_tech_" + index + "_" + System.nanoTime());
            user.setPasswordHash("$2a$10$placeholderplaceholderplaceholderplaceholderplacehold");
            user.setRole(Role.TECHNICIAN);
            user.setStatus(UserStatus.ACTIVE);
            UserJpaEntity savedUser = userRepo.saveAndFlush(user);

            long unique = System.nanoTime();
            TechnicianProfileJpaEntity profile = new TechnicianProfileJpaEntity();
            profile.setUser(savedUser);
            profile.setFullName("Thợ tranh chấp #" + index);
            profile.setPhone("09" + String.format("%09d", unique % 1_000_000_000L));
            profile.setEmail("race_tech_" + index + "_" + unique + "@test.local");
            profile.setCitizenId(String.format("%012d", unique % 1_000_000_000_000L));
            profile.setVerificationStatus(VerificationStatus.APPROVED);
            profile.setCategoryIds(new LinkedHashSet<>(List.of(CATEGORY_ID)));
            profile.setAreaIds(new LinkedHashSet<>(List.of(AREA_ID)));
            techProfileRepo.save(profile);
            return savedUser.getId();
        });

        createdTechIds.add(techId);
        return techId;
    }

    /** Đảm bảo thợ đã APPROVED và khớp category + area của yêu cầu sẽ tạo. */
    private Long approveTechnician(String username) {
        UserJpaEntity user = userRepo.findByUsernameAndDeletedAtIsNull(username).orElseThrow();
        TechnicianProfileJpaEntity profile = techProfileRepo.findById(user.getId()).orElseThrow();
        originalVerification.put(user.getId(), profile.getVerificationStatus());
        originalCategories.put(user.getId(), new LinkedHashSet<>(profile.getCategoryIds()));
        originalAreas.put(user.getId(), new LinkedHashSet<>(profile.getAreaIds()));
        profile.setVerificationStatus(VerificationStatus.APPROVED);
        profile.setCategoryIds(new LinkedHashSet<>(List.of(CATEGORY_ID)));
        profile.setAreaIds(new LinkedHashSet<>(List.of(AREA_ID)));
        techProfileRepo.save(profile);
        return user.getId();
    }

    private RepairRequestJpaEntity createOpenRequest() {
        RepairRequestJpaEntity request = new RepairRequestJpaEntity();
        request.setRequestCode("REQ-RACE-" + System.nanoTime());
        request.setCustomerId(customerId);
        request.setCategoryId(CATEGORY_ID);
        request.setAreaId(AREA_ID);
        request.setTitle("Tranh chấp nhận việc");
        request.setDescription("Hai thợ bấm Nhận việc cùng một thời điểm");
        request.setAddress("1 Lê Lợi, Quận 1");
        request.setBudgetRef(BUDGET);
        request.setStatus(RequestStatus.OPEN);
        request.setApplyDeadline(LocalDateTime.now().plusDays(3));
        request.setRequestedTime(LocalDateTime.now().plusDays(1));
        request.setCreatedBy(customerId);
        RepairRequestJpaEntity saved = requestRepo.save(request);
        createdRequestIds.add(saved.getId());
        return saved;
    }

    @Test
    @DisplayName("Pivot: hai thợ nhận việc cùng lúc → đúng 1 thắng, người còn lại nhận JOB_ALREADY_TAKEN")
    void twoTechniciansApplyingSimultaneously_onlyOneWins() throws Exception {
        RepairRequestJpaEntity request = createOpenRequest();
        Long requestId = request.getId();

        // Hai thread cùng chờ ở barrier rồi nhả ra một lúc -> tranh chấp thật.
        CyclicBarrier startLine = new CyclicBarrier(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);

        try {
            List<Future<Throwable>> results = new ArrayList<>();
            for (Long technicianId : List.of(techAId, techBId)) {
                Callable<Throwable> attempt = () -> {
                    try {
                        startLine.await(10, TimeUnit.SECONDS);
                        quotationUseCase.apply(requestId, technicianId);
                        return null; // null = nhận việc thành công
                    } catch (Throwable t) {
                        return t;
                    }
                };
                results.add(pool.submit(attempt));
            }

            List<Throwable> outcomes = new ArrayList<>();
            for (Future<Throwable> future : results) {
                outcomes.add(future.get(30, TimeUnit.SECONDS));
            }

            long winners = outcomes.stream().filter(java.util.Objects::isNull).count();
            List<Throwable> losers = outcomes.stream().filter(java.util.Objects::nonNull).toList();

            assertEquals(1, winners, "Phải có đúng một thợ nhận được việc, thực tế: " + winners);
            assertEquals(1, losers.size(), "Phải có đúng một thợ bị từ chối");

            Throwable loser = losers.get(0);
            if (!(loser instanceof DomainException domainException)) {
                fail("Thợ thua phải nhận DomainException rõ ràng, không phải lỗi chung chung: " + loser);
                return;
            }
            assertEquals("JOB_ALREADY_TAKEN", domainException.getErrorCode(),
                    "Thợ thua phải nhận đúng errorCode JOB_ALREADY_TAKEN");
            assertEquals(409, domainException.getStatusCode(),
                    "JOB_ALREADY_TAKEN phải map sang HTTP 409 Conflict");
        } finally {
            pool.shutdownNow();
        }

        // Trạng thái cuối cùng phải nhất quán: đúng 1 thợ được gán, đúng 1 bản ghi nhận việc.
        RepairRequestJpaEntity saved = requestRepo.findById(requestId).orElseThrow();
        assertEquals(RequestStatus.ASSIGNED, saved.getStatus());
        assertNotNull(saved.getTechnicianId(), "Yêu cầu phải được gán cho thợ thắng");
        assertTrue(List.of(techAId, techBId).contains(saved.getTechnicianId()));
        assertEquals(0, BUDGET.compareTo(saved.getAgreedPrice()),
                "Giá chốt phải bằng ngân sách khách đưa ra, không thương lượng");
        assertEquals(0, new BigDecimal("240000").compareTo(saved.getDepositAmount()),
                "Cọc phải là 30% của 800000");

        List<QuotationJpaEntity> records = quotationRepo.findByRequestIdOrderByCreatedAtDesc(requestId);
        assertEquals(1, records.size(), "Chỉ được tạo đúng một bản ghi nhận việc");
        assertEquals(QuotationStatus.ACCEPTED, records.get(0).getStatus());
        assertEquals(saved.getTechnicianId(), records.get(0).getTechnicianId());
        assertNotNull(records.get(0).getAcceptedAt(), "Phải ghi lại thời điểm nhận việc");
    }

    @Test
    @DisplayName("Pivot: 5 thợ nhận việc cùng lúc → đúng 1 thắng, 4 người còn lại đều JOB_ALREADY_TAKEN")
    void manyTechniciansApplyingSimultaneously_onlyOneWins() throws Exception {
        int techCount = 5;
        List<Long> technicianIds = new ArrayList<>();
        for (int i = 0; i < techCount; i++) {
            technicianIds.add(createApprovedTechnician(i));
        }

        RepairRequestJpaEntity request = createOpenRequest();
        Long requestId = request.getId();

        // Tất cả thread cùng chờ ở barrier rồi nhả ra một lúc -> tranh chấp thật giữa 5 thợ.
        CyclicBarrier startLine = new CyclicBarrier(techCount);
        ExecutorService pool = Executors.newFixedThreadPool(techCount);

        try {
            List<Future<Throwable>> results = new ArrayList<>();
            for (Long technicianId : technicianIds) {
                Callable<Throwable> attempt = () -> {
                    try {
                        startLine.await(10, TimeUnit.SECONDS);
                        quotationUseCase.apply(requestId, technicianId);
                        return null; // null = nhận việc thành công
                    } catch (Throwable t) {
                        return t;
                    }
                };
                results.add(pool.submit(attempt));
            }

            List<Throwable> outcomes = new ArrayList<>();
            for (Future<Throwable> future : results) {
                outcomes.add(future.get(30, TimeUnit.SECONDS));
            }

            long winners = outcomes.stream().filter(java.util.Objects::isNull).count();
            List<Throwable> losers = outcomes.stream().filter(java.util.Objects::nonNull).toList();

            assertEquals(1, winners, "Phải có đúng một thợ nhận được việc, thực tế: " + winners);
            assertEquals(techCount - 1, losers.size(),
                    "Bốn thợ còn lại đều phải bị từ chối, thực tế: " + losers.size());

            for (Throwable loser : losers) {
                if (!(loser instanceof DomainException domainException)) {
                    fail("Mọi thợ thua phải nhận DomainException rõ ràng, không phải lỗi chung: " + loser);
                    return;
                }
                assertEquals("JOB_ALREADY_TAKEN", domainException.getErrorCode(),
                        "Mọi thợ thua phải nhận đúng errorCode JOB_ALREADY_TAKEN");
                assertEquals(409, domainException.getStatusCode(),
                        "JOB_ALREADY_TAKEN phải map sang HTTP 409 Conflict");
            }
        } finally {
            pool.shutdownNow();
        }

        // Trạng thái cuối cùng vẫn phải nhất quán dù có 5 thợ tranh nhau.
        RepairRequestJpaEntity saved = requestRepo.findById(requestId).orElseThrow();
        assertEquals(RequestStatus.ASSIGNED, saved.getStatus());
        assertNotNull(saved.getTechnicianId(), "Yêu cầu phải được gán cho thợ thắng");
        assertTrue(technicianIds.contains(saved.getTechnicianId()),
                "Thợ được gán phải là một trong 5 thợ dự tranh");

        List<QuotationJpaEntity> records = quotationRepo.findByRequestIdOrderByCreatedAtDesc(requestId);
        assertEquals(1, records.size(), "Dù 5 thợ bấm, chỉ được tạo đúng một bản ghi nhận việc");
        assertEquals(QuotationStatus.ACCEPTED, records.get(0).getStatus());
        assertEquals(saved.getTechnicianId(), records.get(0).getTechnicianId());
    }
}
