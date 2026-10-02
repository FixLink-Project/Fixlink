package com.fixlink.infrastructure.config;

import com.fixlink.adapter.out.persistence.entity.*;
import com.fixlink.adapter.out.persistence.repository.*;
import com.fixlink.domain.model.AppointmentStatus;
import com.fixlink.domain.model.Media;
import com.fixlink.domain.model.MediaType;
import com.fixlink.domain.model.QuotationStatus;
import com.fixlink.domain.model.RequestStatus;
import com.fixlink.domain.model.Role;
import com.fixlink.domain.model.UserStatus;
import com.fixlink.domain.model.VerificationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final SpringDataUserRepository userRepository;
    private final SpringDataCustomerProfileRepository customerProfileRepository;
    private final SpringDataTechnicianProfileRepository technicianProfileRepository;
    private final SpringDataServiceAreaRepository serviceAreaRepository;
    private final SpringDataRepairRequestRepository repairRequestRepository;
    private final SpringDataMediaRepository mediaRepository;
    private final SpringDataAppointmentRepository appointmentRepository;
    private final SpringDataQuotationRepository quotationRepository;
    private final SpringDataWorkProgressRepository workProgressRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(SpringDataUserRepository userRepository,
                           SpringDataCustomerProfileRepository customerProfileRepository,
                           SpringDataTechnicianProfileRepository technicianProfileRepository,
                           SpringDataServiceAreaRepository serviceAreaRepository,
                           SpringDataRepairRequestRepository repairRequestRepository,
                           SpringDataMediaRepository mediaRepository,
                           SpringDataAppointmentRepository appointmentRepository,
                           SpringDataQuotationRepository quotationRepository,
                           SpringDataWorkProgressRepository workProgressRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.customerProfileRepository = customerProfileRepository;
        this.technicianProfileRepository = technicianProfileRepository;
        this.serviceAreaRepository = serviceAreaRepository;
        this.repairRequestRepository = repairRequestRepository;
        this.mediaRepository = mediaRepository;
        this.appointmentRepository = appointmentRepository;
        this.quotationRepository = quotationRepository;
        this.workProgressRepository = workProgressRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // 1. Seed Admin
        if (userRepository.findByUsernameAndDeletedAtIsNull("admin").isEmpty()) {
            UserJpaEntity admin = new UserJpaEntity();
            admin.setUsername("admin");
            admin.setPasswordHash(passwordEncoder.encode("Admin@123"));
            admin.setRole(Role.ADMIN);
            admin.setStatus(UserStatus.ACTIVE);
            userRepository.save(admin);
            log.info(">>> Đã khởi tạo tài khoản Admin mặc định: username=admin / password=Admin@123");
        }

        // 2. Seed Customer sample
        if (userRepository.findByUsernameAndDeletedAtIsNull("customer01").isEmpty()) {
            UserJpaEntity customer = new UserJpaEntity();
            customer.setUsername("customer01");
            customer.setPasswordHash(passwordEncoder.encode("Password@123"));
            customer.setRole(Role.CUSTOMER);
            customer.setStatus(UserStatus.ACTIVE);
            UserJpaEntity savedCustomer = userRepository.save(customer);

            CustomerProfileJpaEntity custProfile = new CustomerProfileJpaEntity();
            custProfile.setUser(savedCustomer);
            custProfile.setFullName("Nguyễn Văn A");
            custProfile.setPhone("0901234567");
            custProfile.setEmail("nguyenvana@gmail.com");
            custProfile.setAvatarUrl("https://s3.fixlink.vn/avatars/usr_customer01.jpg");
            custProfile.setMembershipTier("VIP");
            customerProfileRepository.save(custProfile);
            log.info(">>> Đã khởi tạo tài khoản Khách hàng mẫu: username=customer01 / password=Password@123");
        }

        // 3. Seed Technician sample
        if (userRepository.findByUsernameAndDeletedAtIsNull("tho_dien_lanh_01").isEmpty()) {
            UserJpaEntity tech = new UserJpaEntity();
            tech.setUsername("tho_dien_lanh_01");
            tech.setPasswordHash(passwordEncoder.encode("Password@123"));
            tech.setRole(Role.TECHNICIAN);
            tech.setStatus(UserStatus.ACTIVE);
            UserJpaEntity savedTech = userRepository.save(tech);

            TechnicianProfileJpaEntity techProfile = new TechnicianProfileJpaEntity();
            techProfile.setUser(savedTech);
            techProfile.setFullName("Trần Văn B (Thợ Điện Lạnh)");
            techProfile.setPhone("0987654321");
            techProfile.setEmail("tranvanb@gmail.com");
            techProfile.setCitizenId("012345678901");
            techProfile.setIdCardFrontUrl("https://s3.fixlink.vn/temp/id_front.jpg");
            techProfile.setIdCardBackUrl("https://s3.fixlink.vn/temp/id_back.jpg");
            techProfile.setBio("Chuyên sửa máy lạnh, tủ lạnh, bảo dưỡng điều hòa 5 năm kinh nghiệm");
            techProfile.setYearsExperience(5);
            techProfile.setVerificationStatus(VerificationStatus.PENDING);
            techProfile.setAvgRating(new BigDecimal("4.85"));
            techProfile.setCompletedJobs(42);
            techProfile.setWalletBalance(new BigDecimal("5000000.00"));
            techProfile.setIsOnline(false);
            techProfile.setCategoryIds(new LinkedHashSet<>(List.of(1L, 2L)));
            techProfile.setAreaIds(new LinkedHashSet<>(List.of(1L, 2L)));
            technicianProfileRepository.save(techProfile);
            log.info(">>> Đã khởi tạo tài khoản Thợ mẫu: username=tho_dien_lanh_01 / password=Password@123");
        }

        // 3b. Seed Approved Technician tech01
        if (userRepository.findByUsernameAndDeletedAtIsNull("tech01").isEmpty()) {
            UserJpaEntity tech1 = new UserJpaEntity();
            tech1.setUsername("tech01");
            tech1.setPasswordHash(passwordEncoder.encode("Password@123"));
            tech1.setRole(Role.TECHNICIAN);
            tech1.setStatus(UserStatus.ACTIVE);
            UserJpaEntity savedTech1 = userRepository.save(tech1);

            TechnicianProfileJpaEntity techProfile1 = new TechnicianProfileJpaEntity();
            techProfile1.setUser(savedTech1);
            techProfile1.setFullName("Lê Minh Thợ Máy");
            techProfile1.setPhone("0912345678");
            techProfile1.setEmail("tech01@fixlink.vn");
            techProfile1.setCitizenId("012345678999");
            techProfile1.setIdCardFrontUrl("https://s3.fixlink.vn/temp/id_front.jpg");
            techProfile1.setIdCardBackUrl("https://s3.fixlink.vn/temp/id_back.jpg");
            techProfile1.setBio("Thợ sửa chữa điện gia dụng, khóa và cơ điện tử chuyên nghiệp");
            techProfile1.setYearsExperience(7);
            techProfile1.setVerificationStatus(VerificationStatus.APPROVED);
            techProfile1.setAvgRating(new BigDecimal("4.92"));
            techProfile1.setCompletedJobs(68);
            techProfile1.setWalletBalance(new BigDecimal("12500000.00"));
            techProfile1.setIsOnline(true);
            techProfile1.setCategoryIds(new LinkedHashSet<>(List.of(1L, 2L, 3L)));
            techProfile1.setAreaIds(new LinkedHashSet<>(List.of(1L, 2L, 3L)));
            technicianProfileRepository.save(techProfile1);
            log.info(">>> Đã khởi tạo tài khoản Thợ mẫu đã duyệt: username=tech01 / password=Password@123");
        }

        // 3c. Seed Pending Technician awaiting verification
        if (userRepository.findByUsernameAndDeletedAtIsNull("tho_cho_duyet_01").isEmpty()) {
            UserJpaEntity pendingTech = new UserJpaEntity();
            pendingTech.setUsername("tho_cho_duyet_01");
            pendingTech.setPasswordHash(passwordEncoder.encode("Password@123"));
            pendingTech.setRole(Role.TECHNICIAN);
            pendingTech.setStatus(UserStatus.PENDING);
            UserJpaEntity savedPendingTech = userRepository.save(pendingTech);

            TechnicianProfileJpaEntity pendingTechProfile = new TechnicianProfileJpaEntity();
            pendingTechProfile.setUser(savedPendingTech);
            pendingTechProfile.setFullName("Hoàng Văn C (Thợ Chờ Duyệt)");
            pendingTechProfile.setPhone("0934567890");
            pendingTechProfile.setEmail("hoangvanc@gmail.com");
            pendingTechProfile.setCitizenId("079201009876");
            pendingTechProfile.setIdCardFrontUrl("https://picsum.photos/seed/cccd-front/800/500");
            pendingTechProfile.setIdCardBackUrl("https://picsum.photos/seed/cccd-back/800/500");
            pendingTechProfile.setBio("Thợ lắp đặt và sửa chữa đường ống nước, máy bơm, điện dân dụng 4 năm kinh nghiệm.");
            pendingTechProfile.setYearsExperience(4);
            pendingTechProfile.setVerificationStatus(VerificationStatus.PENDING);
            pendingTechProfile.setAvgRating(BigDecimal.ZERO);
            pendingTechProfile.setCompletedJobs(0);
            pendingTechProfile.setWalletBalance(BigDecimal.ZERO);
            pendingTechProfile.setIsOnline(false);
            technicianProfileRepository.save(pendingTechProfile);
            log.info(">>> Đã khởi tạo tài khoản Thợ chờ duyệt KYC: username=tho_cho_duyet_01 / password=Password@123");
        }

        // 4. Seed Blocked User
        if (userRepository.findByUsernameAndDeletedAtIsNull("user_blocked").isEmpty()) {
            UserJpaEntity blockedUser = new UserJpaEntity();
            blockedUser.setUsername("user_blocked");
            blockedUser.setPasswordHash(passwordEncoder.encode("Password@123"));
            blockedUser.setRole(Role.CUSTOMER);
            blockedUser.setStatus(UserStatus.BLOCKED);
            userRepository.save(blockedUser);
            log.info(">>> Đã khởi tạo tài khoản Khóa mẫu: username=user_blocked / password=Password@123");
        }

        // 5. Seed Inactive User
        if (userRepository.findByUsernameAndDeletedAtIsNull("user_inactive").isEmpty()) {
            UserJpaEntity inactiveUser = new UserJpaEntity();
            inactiveUser.setUsername("user_inactive");
            inactiveUser.setPasswordHash(passwordEncoder.encode("Password@123"));
            inactiveUser.setRole(Role.CUSTOMER);
            inactiveUser.setStatus(UserStatus.INACTIVE);
            userRepository.save(inactiveUser);
            log.info(">>> Đã khởi tạo tài khoản Ngừng hoạt động: username=user_inactive / password=Password@123");
        }

        // 6. Khởi tạo danh sách khu vực hoạt động mẫu
        if (serviceAreaRepository.count() == 0) {
            List<ServiceAreaJpaEntity> areas = List.of(
                    ServiceAreaJpaEntity.builder().code("Q1_HCM").name("Quận 1").city("Hồ Chí Minh").isActive(true).build(),
                    ServiceAreaJpaEntity.builder().code("Q7_HCM").name("Quận 7").city("Hồ Chí Minh").isActive(true).build(),
                    ServiceAreaJpaEntity.builder().code("BT_HCM").name("Quận Bình Thạnh").city("Hồ Chí Minh").isActive(true).build(),
                    ServiceAreaJpaEntity.builder().code("CG_HN").name("Quận Cầu Giấy").city("Hà Nội").isActive(true).build(),
                    ServiceAreaJpaEntity.builder().code("TX_HN").name("Quận Thanh Xuân").city("Hà Nội").isActive(true).build()
            );
            serviceAreaRepository.saveAll(areas);
            log.info(">>> Đã khởi tạo 5 khu vực hoạt động mẫu (Service Areas)");
        }

        // 7. Seed Yêu cầu Sửa chữa demo (tab trạng thái + phân trang đánh số + ảnh Firebase Storage)
        if (repairRequestRepository.count() == 0) {
            Long customerId = userRepository.findByUsernameAndDeletedAtIsNull("customer01")
                    .map(UserJpaEntity::getId)
                    .orElse(null);
            Long thoId = userRepository.findByUsernameAndDeletedAtIsNull("tho_dien_lanh_01")
                    .map(UserJpaEntity::getId)
                    .orElse(null);
            Long tech01Id = userRepository.findByUsernameAndDeletedAtIsNull("tech01")
                    .map(UserJpaEntity::getId)
                    .orElse(null);

            if (customerId != null) {
                List<RepairRequestJpaEntity> demoRequests = new ArrayList<>();

                // Đơn 1: Giao cho thợ đã duyệt tech01
                demoRequests.add(demoRequest("REQ-DEMO-0001", customerId, tech01Id, 1L, RequestStatus.ASSIGNED,
                        "Máy lạnh không mát và chảy nước dàn lạnh",
                        "Máy lạnh Daikin 1.5HP chạy 2 tiếng vẫn không mát, nước chảy xuống sàn phòng khách.",
                        "123 Nguyễn Thị Minh Khai, Quận 1, TP.HCM", 1, "350000"));

                // Đơn 2: Giao cho thợ tho_dien_lanh_01
                demoRequests.add(demoRequest("REQ-DEMO-0002", customerId, thoId, 2L, RequestStatus.IN_PROGRESS,
                        "Rò rỉ ống nước âm tường nhà bếp",
                        "Ống nước âm tường sau bồn rửa bị rỉ, tường bị ố vàng và thấm xuống trần tầng dưới.",
                        "45 Lê Văn Việt, Quận 9, TP.HCM", 3, "450000"));

                demoRequests.add(demoRequest("REQ-DEMO-0003", customerId, null, 3L, RequestStatus.PENDING,
                        "Bếp từ báo lỗi E6 không nấm được",
                        "Bếp từ đôi hiệu Bosch đang dùng bình thường thì báo lỗi E6, mặt bếp không nóng.",
                        "88 Trần Hưng Đạo, Quận 5, TP.HCM", 2, "0"));

                demoRequests.add(demoRequest("REQ-DEMO-0004", customerId, thoId, 1L, RequestStatus.COMPLETED,
                        "Vệ sinh và nạp ga máy lạnh treo tường",
                        "Đã vệ sinh dàn lạnh, dàn nóng và nạp ga R32, máy chạy êm và lạnh sâu.",
                        "210 Phan Xích Long, Phú Nhuận, TP.HCM", 12, "250000"));

                demoRequests.add(demoRequest("REQ-DEMO-0005", customerId, null, 4L, RequestStatus.DRAFT,
                        "Thay khóa cửa chính bị kẹt",
                        "Khóa tay gạt bị kẹt, đôi khi phải đẩy mạnh mới mở được. Cần thợ khảo sát và báo giá.",
                        "17 Nguyễn Văn Cừ, Quận Bình Thạnh, TP.HCM", 0, "0"));

                demoRequests.add(demoRequest("REQ-DEMO-0006", customerId, thoId, 2L, RequestStatus.OPEN,
                        "Thông tắc đường ống thoát sàn nhà tắm",
                        "Sàn nhà tắm thoát nước rất chậm, có mùi hôi bốc lên, nghi ngờ tắc tại cổ góp.",
                        "56 Điện Biên Phủ, Quận 3, TP.HCM", 4, "0"));

                demoRequests.add(demoRequest("REQ-DEMO-0007", customerId, null, 1L, RequestStatus.ASSIGNED,
                        "Sửa tủ lạnh kêu to và không đủ lạnh",
                        "Tủ lạnh Side-by-side kêu rè rè, ngăn mát chỉ đạt 12 độ, ngăn đá vẫn đông bình thường.",
                        "302 Võ Văn Ngân, Thủ Đức, TP.HCM", 5, "380000"));

                // Đơn 8: Giao cho tech01
                demoRequests.add(demoRequest("REQ-DEMO-0008", customerId, tech01Id, 2L, RequestStatus.INSPECTING,
                        "Xử lý chập điện tại ổ cắm phòng ngủ",
                        "Ổ cắm phòng ngủ có mùi khét, aptomat tổng nhảy liên tục khi cắm máy sấy tóc.",
                        "91 Hoàng Diệu, Quận 4, TP.HCM", 6, "320000"));

                // Đơn 9: Giao cho tech01
                demoRequests.add(demoRequest("REQ-DEMO-0009", customerId, tech01Id, 1L, RequestStatus.ASSIGNED,
                        "Lắp aptomat riêng cho khu vực bếp",
                        "Khu vực bếp dùng chung aptomat với phòng khách, cần tách riêng để tránh nhảy tổng.",
                        "12 Cách Mạng Tháng 8, Quận 10, TP.HCM", 7, "550000"));

                demoRequests.add(demoRequest("REQ-DEMO-0010", customerId, thoId, 4L, RequestStatus.IN_PROGRESS,
                        "Cửa cuốn bị kẹt nan và kêu to",
                        "Cửa cuốn bấm điều khiển chỉ lên được 1/3 rồi dừng, phát ra tiếng kêu rít ở ray trái.",
                        "77 Tô Hiến Thành, Quận 10, TP.HCM", 8, "700000"));

                demoRequests.add(demoRequest("REQ-DEMO-0011", customerId, tech01Id, 2L, RequestStatus.AWAITING_ACCEPTANCE,
                        "Thay vòi sen và xử lý rò rỉ lavabo",
                        "Đã thay vòi sen mới, xử lý gioăng lavabo và kiểm tra áp lực nước, chờ khách nghiệm thu.",
                        "23 Lý Thường Kiệt, Quận Tân Bình, TP.HCM", 9, "280000"));

                demoRequests.add(demoRequest("REQ-DEMO-0012", customerId, thoId, 3L, RequestStatus.COMPLETED,
                        "Sửa lò vi sóng không nóng",
                        "Thay cầu chì cao áp và kiểm tra magnetron, lò hoạt động lại bình thường và đã bàn giao.",
                        "145 Nguyễn Oanh, Gò Vấp, TP.HCM", 15, "220000"));

                demoRequests.add(demoRequest("REQ-DEMO-0013", customerId, null, 4L, RequestStatus.CANCELLED,
                        "Mở khóa cửa phòng trọ khẩn cấp",
                        "Khách đã tự xoay được chìa trước khi thợ tới nên xin hủy yêu cầu này.",
                        "9 Tạ Quang Bửu, Quận 8, TP.HCM", 10, "0",
                        LocalDateTime.now().plusDays(2), "Khách đã tự xoay được chìa"));

                demoRequests.add(demoRequest("REQ-DEMO-0014", customerId, null, 1L, RequestStatus.PENDING,
                        "Máy giặt không vắt và báo lỗi UE",
                        "Máy giặt cửa trước báo lỗi UE, lồng giặt không vắt được và còn nhiều nước.",
                        "66 Nguyễn Ảnh Thủ, Quận 12, TP.HCM", 11, "0",
                        LocalDateTime.now().plusDays(3), null));

                demoRequests.add(demoRequest("REQ-DEMO-0015", customerId, null, 2L, RequestStatus.OPEN,
                        "Lắp đặt bình nóng lạnh năng lượng mặt trời",
                        "Cần khảo sát mái nhà và lắp bình nóng lạnh 150L đã mua sẵn, kèm đường ống và van.",
                        "404 Lạc Long Quân, Tây Hồ, Hà Nội", 1, "400000",
                        LocalDateTime.now().plusDays(2).plusHours(14), null));

                // Đơn 16: OPEN - SẮP HẾT HẠN HIỆU LỰC (còn 4 giờ)
                demoRequests.add(demoRequest("REQ-DEMO-0016", customerId, null, 1L, RequestStatus.OPEN,
                        "Sửa tủ đông Sanaky đóng tuyết dày và rỉ nước",
                        "Tủ đông 2 ngăn Sanaky bị đóng tuyết dày bất thường ở ngăn mát, nước đọng thành vũng.",
                        "82 Nguyễn Thị Thập, Quận 7, TP.HCM", 2, "300000",
                        LocalDateTime.now().plusHours(4), null));

                // Đơn 17: OPEN - ĐÃ QUÁ HẠN HIỆU LỰC (hết hạn 2 giờ trước) -> Test chặn nhận việc & cảnh báo
                demoRequests.add(demoRequest("REQ-DEMO-0017", customerId, null, 1L, RequestStatus.OPEN,
                        "Bảo dưỡng máy giặt sấy Electrolux kêu to khi vắt",
                        "Máy giặt rung lắc dữ dội khi vắt ở tốc độ 1200 vòng/phút, cần thợ kiểm tra giảm chấn.",
                        "158 Bạch Đằng, Bình Thạnh, TP.HCM", 3, "250000",
                        LocalDateTime.now().minusHours(2), null));

                // Đơn 18: CANCELLED - TỰ ĐỘNG HỦY DO HẾT HẠN 3 NGÀY (Scheduler dọn dẹp)
                demoRequests.add(demoRequest("REQ-DEMO-0018", customerId, null, 2L, RequestStatus.CANCELLED,
                        "Sửa ampli nghe nhạc Denon mất kênh tiếng trái",
                        "Ampli Denon PMA-800NE bật lên chỉ nghe loa phải, vặn volume có tiếng xẹt xẹt.",
                        "245 Hoàng Văn Thụ, Tân Bình, TP.HCM", 4, "350000",
                        LocalDateTime.now().minusDays(1), "Hết hạn 3 ngày không có thợ nhận"));

                List<RepairRequestJpaEntity> savedRequests = repairRequestRepository.saveAll(demoRequests);

                List<MediaJpaEntity> demoMedia = new ArrayList<>();
                for (int index = 0; index < savedRequests.size(); index++) {
                    demoMedia.addAll(demoMediaFor(savedRequests.get(index).getId(), index, customerId));
                }
                mediaRepository.saveAll(demoMedia);

                // Khởi tạo Báo giá thợ nhận việc (Quotation accepted) cho các đơn đã giao thợ
                List<QuotationJpaEntity> demoQuotes = new ArrayList<>();
                for (RepairRequestJpaEntity r : savedRequests) {
                    if (r.getTechnicianId() != null && r.getStatus() != RequestStatus.OPEN && r.getStatus() != RequestStatus.DRAFT) {
                        QuotationJpaEntity q = QuotationJpaEntity.builder()
                                .requestId(r.getId())
                                .technicianId(r.getTechnicianId())
                                .priceLaborVnd(r.getAgreedPrice() != null ? r.getAgreedPrice() : BigDecimal.valueOf(300000))
                                .priceMaterialsVnd(BigDecimal.ZERO)
                                .solution("Nhận việc theo ngân sách và mô tả yêu cầu của khách hàng")
                                .status(QuotationStatus.ACCEPTED)
                                .acceptedAt(r.getCreatedAt().plusHours(2))
                                .note("Cam kết có mặt đúng hẹn, linh kiện chính hãng bảo hành 90 ngày.")
                                .build();
                        q.setCreatedBy(r.getTechnicianId());
                        q.setCreatedAt(r.getCreatedAt().plusHours(2));
                        demoQuotes.add(q);
                    }
                }
                quotationRepository.saveAll(demoQuotes);

                // Khởi tạo lịch sử chuyển trạng thái (WorkProgress) mẫu theo từng nấc thực tế
                List<WorkProgressJpaEntity> demoProgress = new ArrayList<>();
                for (RepairRequestJpaEntity r : savedRequests) {
                    LocalDateTime base = r.getCreatedAt();
                    switch (r.getStatus()) {
                        case ASSIGNED -> {
                            demoProgress.add(createProgress(r.getId(), RequestStatus.DRAFT, RequestStatus.OPEN, "Khách hàng đăng yêu cầu lên hệ thống", base, customerId));
                            demoProgress.add(createProgress(r.getId(), RequestStatus.OPEN, RequestStatus.ASSIGNED, "Kỹ thuật viên đã bấm nhận việc thành công", base.plusHours(2), r.getTechnicianId()));
                        }
                        case INSPECTING -> {
                            demoProgress.add(createProgress(r.getId(), RequestStatus.OPEN, RequestStatus.ASSIGNED, "Thợ nhận việc", base.minusDays(1), r.getTechnicianId()));
                            demoProgress.add(createProgress(r.getId(), RequestStatus.ASSIGNED, RequestStatus.INSPECTING, "Thợ đã có mặt tại hiện trường, tiến hành đo đạc và lập biên bản khảo sát", base.plusHours(3), r.getTechnicianId()));
                        }
                        case IN_PROGRESS -> {
                            demoProgress.add(createProgress(r.getId(), RequestStatus.OPEN, RequestStatus.ASSIGNED, "Thợ nhận việc", base.minusDays(2), r.getTechnicianId()));
                            demoProgress.add(createProgress(r.getId(), RequestStatus.ASSIGNED, RequestStatus.INSPECTING, "Khảo sát hiện trường và chốt phương án thi công", base.minusDays(1), r.getTechnicianId()));
                            demoProgress.add(createProgress(r.getId(), RequestStatus.INSPECTING, RequestStatus.IN_PROGRESS, "Bắt đầu triển khai sửa chữa và thay thế linh kiện", base.plusHours(1), r.getTechnicianId()));
                        }
                        case AWAITING_ACCEPTANCE -> {
                            demoProgress.add(createProgress(r.getId(), RequestStatus.OPEN, RequestStatus.ASSIGNED, "Thợ nhận việc", base.minusDays(3), r.getTechnicianId()));
                            demoProgress.add(createProgress(r.getId(), RequestStatus.ASSIGNED, RequestStatus.INSPECTING, "Khảo sát và kiểm tra thiết bị", base.minusDays(2), r.getTechnicianId()));
                            demoProgress.add(createProgress(r.getId(), RequestStatus.INSPECTING, RequestStatus.IN_PROGRESS, "Tiến hành sửa chữa và thay gioăng chống rò", base.minusDays(1), r.getTechnicianId()));
                            demoProgress.add(createProgress(r.getId(), RequestStatus.IN_PROGRESS, RequestStatus.AWAITING_ACCEPTANCE, "Thợ đã hoàn thành công việc và chạy thử 30 phút. Mời khách hàng nghiệm thu.", base.plusHours(2), r.getTechnicianId()));
                        }
                        case COMPLETED -> {
                            demoProgress.add(createProgress(r.getId(), RequestStatus.OPEN, RequestStatus.ASSIGNED, "Thợ nhận việc", base.minusDays(5), r.getTechnicianId()));
                            demoProgress.add(createProgress(r.getId(), RequestStatus.ASSIGNED, RequestStatus.INSPECTING, "Khảo sát lỗi", base.minusDays(4), r.getTechnicianId()));
                            demoProgress.add(createProgress(r.getId(), RequestStatus.INSPECTING, RequestStatus.IN_PROGRESS, "Sửa chữa và thay thế linh kiện", base.minusDays(3), r.getTechnicianId()));
                            demoProgress.add(createProgress(r.getId(), RequestStatus.IN_PROGRESS, RequestStatus.AWAITING_ACCEPTANCE, "Bàn giao thiết bị và hướng dẫn sử dụng", base.minusDays(2), r.getTechnicianId()));
                            demoProgress.add(createProgress(r.getId(), RequestStatus.AWAITING_ACCEPTANCE, RequestStatus.COMPLETED, "Khách hàng nghiệm thu đạt yêu cầu, giải ngân thanh toán và kích hoạt bảo hành điện tử 90 ngày", base.minusDays(1), customerId));
                        }
                        case CANCELLED -> {
                            String note = r.getCancelReason() != null ? r.getCancelReason() : "Yêu cầu đã bị hủy";
                            demoProgress.add(createProgress(r.getId(), RequestStatus.OPEN, RequestStatus.CANCELLED, note, base.plusHours(1), customerId));
                        }
                        default -> {
                            // DRAFT hoặc OPEN mới tạo
                        }
                    }
                }
                workProgressRepository.saveAll(demoProgress);

                log.info(">>> Đã khởi tạo {} yêu cầu sửa chữa demo, {} ảnh, {} báo giá và {} mốc tiến trình WorkProgress",
                        savedRequests.size(), demoMedia.size(), demoQuotes.size(), demoProgress.size());

                // 8. Seed Lịch hẹn mẫu (Jira RC-48: Appointment Status Lifecycle)
                if (appointmentRepository.count() == 0 && !savedRequests.isEmpty()) {
                    List<AppointmentJpaEntity> demoAppts = new ArrayList<>();

                    // Cuộc hẹn 1: Khảo sát cho Đơn 1 (Thợ tech01)
                    if (tech01Id != null) {
                        AppointmentJpaEntity appt1 = AppointmentJpaEntity.builder()
                                .repairRequestId(savedRequests.get(0).getId())
                                .customerId(customerId)
                                .technicianId(tech01Id)
                                .appointmentType("SURVEY")
                                .scheduledDate(LocalDate.now().plusDays(2))
                                .scheduledTime(LocalTime.of(9, 30))
                                .status(AppointmentStatus.CONFIRMED)
                                .address("123 Nguyễn Thị Minh Khai, Quận 1, TP.HCM")
                                .notes("Khách yêu cầu thợ mang theo đồng hồ đo gas và dụng cụ bảo hộ.")
                                .build();
                        appt1.setCreatedBy(customerId);
                        demoAppts.add(appt1);
                    }

                    // Cuộc hẹn 2: Sửa chữa cho Đơn 2 (Thợ tho_dien_lanh_01)
                    if (thoId != null && savedRequests.size() > 1) {
                        AppointmentJpaEntity appt2 = AppointmentJpaEntity.builder()
                                .repairRequestId(savedRequests.get(1).getId())
                                .customerId(customerId)
                                .technicianId(thoId)
                                .appointmentType("REPAIR")
                                .scheduledDate(LocalDate.now().plusDays(3))
                                .scheduledTime(LocalTime.of(14, 0))
                                .status(AppointmentStatus.CONFIRMED)
                                .address("45 Lê Văn Việt, Quận 9, TP.HCM")
                                .notes("Thợ chuẩn bị máy dò rò rỉ âm tường.")
                                .build();
                        appt2.setCreatedBy(thoId);
                        demoAppts.add(appt2);
                    }

                    appointmentRepository.saveAll(demoAppts);
                    log.info(">>> Đã khởi tạo {} lịch hẹn mẫu (RC-48) cho tech01 và tho_dien_lanh_01", demoAppts.size());
                }
            }
        }

        // 9. Seed Dữ liệu kiểm thử nâng cao cho Thợ: Hiệu lực nhận việc, Quy trình hoàn thành & Lịch dời lịch hẹn (RC-48)
        seedTechnicianTestingScenarios();
    }

    private void seedTechnicianTestingScenarios() {
        if (repairRequestRepository.findByRequestCode("REQ-TEST-VALID-01").isPresent()) {
            return;
        }

        Long customerId = userRepository.findByUsernameAndDeletedAtIsNull("customer01")
                .map(UserJpaEntity::getId)
                .orElse(null);
        Long thoId = userRepository.findByUsernameAndDeletedAtIsNull("tho_dien_lanh_01")
                .map(UserJpaEntity::getId)
                .orElse(null);
        Long tech01Id = userRepository.findByUsernameAndDeletedAtIsNull("tech01")
                .map(UserJpaEntity::getId)
                .orElse(null);

        if (customerId == null || tech01Id == null) {
            log.warn(">>> Không tìm thấy customer01 hoặc tech01, bỏ qua seed kịch bản kiểm thử thợ.");
            return;
        }

        log.info(">>> Đang nạp dữ liệu kiểm thử nâng cao cho Thợ (Hiệu lực nhận việc, Vòng đời hoàn thành, Lịch hẹn RC-48)...");

        // ---------------------------------------------------------------------------------
        // GROUP A: KIỂM THỬ HIỆU LỰC NHẬN VIỆC (Job Validity & Matching)
        // ---------------------------------------------------------------------------------

        // 1. Việc hợp lệ trong hạn 3 ngày, khớp chuyên môn & khu vực -> THỢ NHẬN ĐƯỢC NGAY
        RepairRequestJpaEntity reqValid1 = createTestJob("REQ-TEST-VALID-01", customerId, null,
                1L, 1L, RequestStatus.OPEN,
                "[Nhận việc ngay] Sửa máy lạnh Daikin 1.5HP rò rỉ nước tại Quận 1",
                "Máy lạnh Daikin Inverter 1.5HP chảy nước dàn lạnh xuống sàn gỗ, cần thợ kiểm tra máng thoát nước và nạp bổ sung ga R32.",
                "120 Nguyễn Đình Chiểu, Quận 1, TP.HCM",
                "450000",
                LocalDateTime.now().plusDays(2).plusHours(12),
                LocalDateTime.now().plusDays(2), 0);
        saveMedia(reqValid1.getId(), customerId, 0);
        recordProgress(reqValid1.getId(), null, RequestStatus.OPEN, "Khách tạo yêu cầu mới (Đang mở nhận việc)", customerId, LocalDateTime.now());

        // 2. Việc sắp hết hạn (< 4 giờ), khớp chuyên môn & khu vực -> CÒN HIỆU LỰC
        RepairRequestJpaEntity reqValid2 = createTestJob("REQ-TEST-VALID-02", customerId, null,
                2L, 3L, RequestStatus.OPEN,
                "[Sắp hết hạn] Khắc phục sự cố chập điện Aptomat tổng tại Bình Thạnh",
                "Aptomat tổng tầng trệt nhảy liên tục khi cắm tải lớn, nghi ngờ rò điện âm tường sau ổ cắm. Cần thợ có đồng hồ đo điện qua gấp trong hôm nay.",
                "45 Bạch Đằng, Quận Bình Thạnh, TP.HCM",
                "350000",
                LocalDateTime.now().plusHours(4),
                LocalDateTime.now().plusHours(6), 0);
        saveMedia(reqValid2.getId(), customerId, 1);
        recordProgress(reqValid2.getId(), null, RequestStatus.OPEN, "Khách tạo yêu cầu mới (Sắp hết hạn trong 4 giờ)", customerId, LocalDateTime.now());

        // 3. Việc ĐÃ QUÁ HẠN 3 ngày -> BỊ CHẶN KHÔNG CHO NHẬN
        RepairRequestJpaEntity reqExpired = createTestJob("REQ-TEST-EXPIRED", customerId, null,
                3L, 2L, RequestStatus.OPEN,
                "[Đã hết hạn] Sửa bếp hồng ngoại Sanaky không nóng tại Quận 7",
                "Bếp hồng ngoại đơn Sanaky cắm điện vẫn lên đèn nhưng mâm nhiệt không đỏ, không nóng.",
                "88 Nguyễn Thị Thập, Quận 7, TP.HCM",
                "250000",
                LocalDateTime.now().minusHours(2),
                LocalDateTime.now().minusHours(1), 3);
        saveMedia(reqExpired.getId(), customerId, 2);
        recordProgress(reqExpired.getId(), null, RequestStatus.OPEN, "Khách tạo yêu cầu (Đã quá hạn 3 ngày)", customerId, LocalDateTime.now().minusDays(3));

        // 4. Việc KHÁC KHU VỰC (Cầu Giấy, Hà Nội vs TP.HCM) -> tech01 không khớp khu vực
        RepairRequestJpaEntity reqDiffArea = createTestJob("REQ-TEST-DIFF-AREA", customerId, null,
                2L, 4L, RequestStatus.OPEN,
                "[Khác khu vực] Thông tắc đường ống bồn rửa chén tại Cầu Giấy, Hà Nội",
                "Đường ống thoát bồn rửa chén bị tắc mỡ, nước trào ngược ra sàn bếp.",
                "123 Cầu Giấy, P. Dịch Vọng, Quận Cầu Giấy, Hà Nội",
                "300000",
                LocalDateTime.now().plusDays(3),
                LocalDateTime.now().plusDays(1), 0);
        saveMedia(reqDiffArea.getId(), customerId, 3);
        recordProgress(reqDiffArea.getId(), null, RequestStatus.OPEN, "Khách tạo yêu cầu tại Hà Nội", customerId, LocalDateTime.now());

        // 5. Việc KHÁC CHUYÊN MÔN (Khóa & Cửa Cuốn vs Điện / Lạnh) -> tech01 không khớp chuyên môn
        RepairRequestJpaEntity reqDiffCat = createTestJob("REQ-TEST-DIFF-CAT", customerId, null,
                4L, 1L, RequestStatus.OPEN,
                "[Khác chuyên môn] Mở khóa tay gạt cửa nhôm kính bị kẹt chìa tại Quận 1",
                "Khóa tay gạt cửa nhôm Xingfa bị gãy chìa kẹt bên trong ổ, cần thợ khóa chuyên nghiệp xử lý.",
                "18 Lê Duẩn, Quận 1, TP.HCM",
                "250000",
                LocalDateTime.now().plusDays(3),
                LocalDateTime.now().plusDays(1), 0);
        saveMedia(reqDiffCat.getId(), customerId, 4);
        recordProgress(reqDiffCat.getId(), null, RequestStatus.OPEN, "Khách tạo yêu cầu khóa cửa", customerId, LocalDateTime.now());

        // 6. Việc ĐÃ CÓ THỢ KHÁC NHẬN TRƯỚC (Ai nhận trước được trước -> 409 JOB_ALREADY_TAKEN)
        Long otherTechId = thoId != null ? thoId : tech01Id;
        RepairRequestJpaEntity reqTaken = createTestJob("REQ-TEST-TAKEN", customerId, otherTechId,
                1L, 2L, RequestStatus.ASSIGNED,
                "[Đã có thợ khác nhận] Vệ sinh 2 máy giặt lồng ngang LG tại Quận 7",
                "Khách cần vệ sinh lồng giặt và bảo dưỡng khử khuẩn 2 máy giặt lồng ngang 9kg.",
                "60 Đường số 7, KDC Jamona, Quận 7, TP.HCM",
                "500000",
                LocalDateTime.now().plusDays(2),
                LocalDateTime.now().plusDays(1), 1);
        saveMedia(reqTaken.getId(), customerId, 0);
        QuotationJpaEntity qTaken = recordAcceptedQuotation(reqTaken.getId(), otherTechId, new BigDecimal("500000"), LocalDateTime.now().minusHours(12));
        reqTaken.setSelectedQuotationId(qTaken.getId());
        repairRequestRepository.save(reqTaken);
        recordProgress(reqTaken.getId(), null, RequestStatus.OPEN, "Khách tạo yêu cầu", customerId, LocalDateTime.now().minusDays(1));
        recordProgress(reqTaken.getId(), RequestStatus.OPEN, RequestStatus.ASSIGNED, "Thợ khác đã nhanh tay nhận trước", otherTechId, LocalDateTime.now().minusHours(12));

        // ---------------------------------------------------------------------------------
        // GROUP B: QUY TRÌNH TIẾN TRÌNH & HOÀN THÀNH CÔNG VIỆC CỦA THỢ tech01
        // ---------------------------------------------------------------------------------

        // 7. Đơn MỚI NHẬN VIỆC (ASSIGNED)
        RepairRequestJpaEntity reqAssigned = createTestJob("REQ-TEST-ASSIGNED", customerId, tech01Id,
                1L, 1L, RequestStatus.ASSIGNED,
                "[Đã nhận việc] Sửa tủ lạnh Hitachi Inverter không đông đá tại Quận 1",
                "Tủ lạnh Hitachi Inverter ngăn mát vẫn lạnh nhưng ngăn đá không đông kem và thịt cá, quạt gió ngăn đông chạy yếu.",
                "72 Lê Thánh Tôn, Quận 1, TP.HCM",
                "480000",
                LocalDateTime.now().plusDays(2),
                LocalDateTime.now().plusDays(1), 1);
        saveMedia(reqAssigned.getId(), customerId, 1);
        QuotationJpaEntity qAssigned = recordAcceptedQuotation(reqAssigned.getId(), tech01Id, new BigDecimal("480000"), LocalDateTime.now().minusHours(8));
        reqAssigned.setSelectedQuotationId(qAssigned.getId());
        repairRequestRepository.save(reqAssigned);
        recordProgress(reqAssigned.getId(), null, RequestStatus.OPEN, "Khách tạo yêu cầu", customerId, LocalDateTime.now().minusDays(1));
        recordProgress(reqAssigned.getId(), RequestStatus.OPEN, RequestStatus.ASSIGNED, "Thợ tech01 nhận việc thành công (ai nhận trước được trước)", tech01Id, LocalDateTime.now().minusHours(8));

        // 8. Đơn ĐANG KHẢO SÁT HIỆN TRƯỜNG (INSPECTING)
        RepairRequestJpaEntity reqInspect = createTestJob("REQ-TEST-INSPECT", customerId, tech01Id,
                2L, 3L, RequestStatus.INSPECTING,
                "[Đang khảo sát] Dò tìm chập điện âm tường phòng ngủ tại Bình Thạnh",
                "Đường điện ổ cắm phòng ngủ tầng 2 bị nhảy CB sau mưa lớn, tường có hiện tượng rò điện tê tay.",
                "88 Bạch Đằng, P. 24, Quận Bình Thạnh, TP.HCM",
                "400000",
                LocalDateTime.now().plusDays(2),
                LocalDateTime.now().plusDays(1), 2);
        saveMedia(reqInspect.getId(), customerId, 2);
        QuotationJpaEntity qInspect = recordAcceptedQuotation(reqInspect.getId(), tech01Id, new BigDecimal("400000"), LocalDateTime.now().minusDays(1));
        reqInspect.setSelectedQuotationId(qInspect.getId());
        repairRequestRepository.save(reqInspect);
        recordProgress(reqInspect.getId(), null, RequestStatus.OPEN, "Khách tạo yêu cầu", customerId, LocalDateTime.now().minusDays(2));
        recordProgress(reqInspect.getId(), RequestStatus.OPEN, RequestStatus.ASSIGNED, "Thợ tech01 nhận việc", tech01Id, LocalDateTime.now().minusDays(1));
        recordProgress(reqInspect.getId(), RequestStatus.ASSIGNED, RequestStatus.INSPECTING, "Thợ có mặt tại hiện trường đang đo điện trở cách điện bằng Megohmmeter", tech01Id, LocalDateTime.now().minusHours(2));

        // 9. Đơn ĐANG THI CÔNG SỬA CHỮA (IN_PROGRESS)
        RepairRequestJpaEntity reqProgress = createTestJob("REQ-TEST-PROGRESS", customerId, tech01Id,
                2L, 2L, RequestStatus.IN_PROGRESS,
                "[Đang thi công] Lắp máy bơm tăng áp điện tử Wilo cho căn hộ tại Quận 7",
                "Áp lực nước vòi sen và bồn tắm yếu, cần lắp máy bơm tăng áp điện tử tự động 200W.",
                "15 Đường Nguyễn Lương Bằng, Phú Mỹ Hưng, Quận 7, TP.HCM",
                "650000",
                LocalDateTime.now().plusDays(2),
                LocalDateTime.now().plusDays(1), 3);
        saveMedia(reqProgress.getId(), customerId, 3);
        QuotationJpaEntity qProgress = recordAcceptedQuotation(reqProgress.getId(), tech01Id, new BigDecimal("650000"), LocalDateTime.now().minusDays(2));
        reqProgress.setSelectedQuotationId(qProgress.getId());
        repairRequestRepository.save(reqProgress);
        recordProgress(reqProgress.getId(), null, RequestStatus.OPEN, "Khách tạo yêu cầu", customerId, LocalDateTime.now().minusDays(3));
        recordProgress(reqProgress.getId(), RequestStatus.OPEN, RequestStatus.ASSIGNED, "Thợ tech01 nhận việc", tech01Id, LocalDateTime.now().minusDays(2));
        recordProgress(reqProgress.getId(), RequestStatus.ASSIGNED, RequestStatus.INSPECTING, "Khảo sát vị trí lắp đặt trên trần thạch cao", tech01Id, LocalDateTime.now().minusDays(1));
        recordProgress(reqProgress.getId(), RequestStatus.INSPECTING, RequestStatus.IN_PROGRESS, "Đang thi công cắt nối đường ống PPR và cố định chân máy bơm", tech01Id, LocalDateTime.now().minusHours(3));

        // 10. Đơn CHỜ KHÁCH NGHIỆM THU (AWAITING_ACCEPTANCE)
        RepairRequestJpaEntity reqAwaiting = createTestJob("REQ-TEST-AWAITING", customerId, tech01Id,
                3L, 1L, RequestStatus.AWAITING_ACCEPTANCE,
                "[Chờ nghiệm thu] Sửa bo mạch nguồn bếp từ âm Hafele báo lỗi F1 tại Quận 1",
                "Bếp từ Hafele đôi bật nguồn báo lỗi F1 quạt không quay. Thợ đã thay linh kiện quạt và sửa bo nguồn.",
                "45 Hai Bà Trưng, Quận 1, TP.HCM",
                "520000",
                LocalDateTime.now().plusDays(2),
                LocalDateTime.now().plusDays(1), 4);
        saveMedia(reqAwaiting.getId(), customerId, 4);
        QuotationJpaEntity qAwaiting = recordAcceptedQuotation(reqAwaiting.getId(), tech01Id, new BigDecimal("520000"), LocalDateTime.now().minusDays(3));
        reqAwaiting.setSelectedQuotationId(qAwaiting.getId());
        repairRequestRepository.save(reqAwaiting);
        recordProgress(reqAwaiting.getId(), null, RequestStatus.OPEN, "Khách tạo yêu cầu", customerId, LocalDateTime.now().minusDays(4));
        recordProgress(reqAwaiting.getId(), RequestStatus.OPEN, RequestStatus.ASSIGNED, "Thợ tech01 nhận việc", tech01Id, LocalDateTime.now().minusDays(3));
        recordProgress(reqAwaiting.getId(), RequestStatus.ASSIGNED, RequestStatus.INSPECTING, "Tháo bếp kiểm tra bo nguồn và cảm biến", tech01Id, LocalDateTime.now().minusDays(2));
        recordProgress(reqAwaiting.getId(), RequestStatus.INSPECTING, RequestStatus.IN_PROGRESS, "Thay quạt DC 18V và tụ lọc nguồn 5uF", tech01Id, LocalDateTime.now().minusDays(1));
        recordProgress(reqAwaiting.getId(), RequestStatus.IN_PROGRESS, RequestStatus.AWAITING_ACCEPTANCE, "Đã sửa xong, đun thử nước sôi 15 phút. Bàn giao chờ khách kiểm tra nghiệm thu", tech01Id, LocalDateTime.now().minusHours(4));

        // 11. Đơn ĐÃ HOÀN THÀNH NGHIỆM THU (COMPLETED)
        RepairRequestJpaEntity reqCompleted = createTestJob("REQ-TEST-COMPLETED", customerId, tech01Id,
                1L, 3L, RequestStatus.COMPLETED,
                "[Đã hoàn thành] Thay lốc máy lạnh Daikin Inverter tại Bình Thạnh",
                "Thay máy nén (lốc) Daikin Inverter 1.5HP, hút chân không và nạp đủ gas R32 chuẩn áp suất.",
                "210 Phan Xích Long, Quận Bình Thạnh, TP.HCM",
                "1500000",
                LocalDateTime.now().minusDays(3),
                LocalDateTime.now().minusDays(4), 5);
        saveMedia(reqCompleted.getId(), customerId, 0);
        QuotationJpaEntity qCompleted = recordAcceptedQuotation(reqCompleted.getId(), tech01Id, new BigDecimal("1500000"), LocalDateTime.now().minusDays(5));
        reqCompleted.setSelectedQuotationId(qCompleted.getId());
        repairRequestRepository.save(reqCompleted);
        recordProgress(reqCompleted.getId(), null, RequestStatus.OPEN, "Khách tạo yêu cầu", customerId, LocalDateTime.now().minusDays(5));
        recordProgress(reqCompleted.getId(), RequestStatus.OPEN, RequestStatus.ASSIGNED, "Thợ tech01 nhận việc", tech01Id, LocalDateTime.now().minusDays(4));
        recordProgress(reqCompleted.getId(), RequestStatus.ASSIGNED, RequestStatus.INSPECTING, "Kiểm tra xác định cháy cuộn dây lốc máy nén", tech01Id, LocalDateTime.now().minusDays(3));
        recordProgress(reqCompleted.getId(), RequestStatus.INSPECTING, RequestStatus.IN_PROGRESS, "Hàn lốc mới, hút chân không và nạp gas R32", tech01Id, LocalDateTime.now().minusDays(2));
        recordProgress(reqCompleted.getId(), RequestStatus.IN_PROGRESS, RequestStatus.AWAITING_ACCEPTANCE, "Máy chạy lạnh sâu 18 độ C, bàn giao khách", tech01Id, LocalDateTime.now().minusDays(1));
        recordProgress(reqCompleted.getId(), RequestStatus.AWAITING_ACCEPTANCE, RequestStatus.COMPLETED, "Khách hàng nghiệm thu đạt chuẩn, ký biên bản và thanh toán 100%", customerId, LocalDateTime.now().minusHours(6));

        // ---------------------------------------------------------------------------------
        // GROUP C: LỊCH HẸN & DỜI LỊCH HẸN (RC-48 APPOINTMENT LIFECYCLE)
        // ---------------------------------------------------------------------------------

        // Lịch 1: CONFIRMED - Khảo sát (Test chức năng Dời lịch hẹn Reschedule)
        createAppointment(reqAssigned.getId(), customerId, tech01Id, "SURVEY",
                LocalDate.now().plusDays(1), LocalTime.of(9, 30),
                AppointmentStatus.CONFIRMED,
                "72 Lê Thánh Tôn, Quận 1, TP.HCM",
                "Thợ mang theo đồng hồ đo gas và kìm bấm cos kiểm tra lốc.",
                null);

        // Lịch 2: RESCHEDULED - Sửa chữa (Đã từng dời lịch, test dời tiếp hoặc bấm Hoàn thành)
        createAppointment(reqInspect.getId(), customerId, tech01Id, "REPAIR",
                LocalDate.now().plusDays(2), LocalTime.of(14, 0),
                AppointmentStatus.RESCHEDULED,
                "88 Bạch Đằng, P. 24, Quận Bình Thạnh, TP.HCM",
                "Khách bận buổi sáng, đã thống nhất dời sang 14:00 chiều để đục tường kiểm tra ống gen.",
                null);

        // Lịch 3: CANCELLED - Khảo sát sơ bộ (Test trạng thái kết thúc Đã hủy)
        createAppointment(reqInspect.getId(), customerId, tech01Id, "SURVEY",
                LocalDate.now().minusDays(2), LocalTime.of(15, 0),
                AppointmentStatus.CANCELLED,
                "88 Bạch Đằng, P. 24, Quận Bình Thạnh, TP.HCM",
                null,
                "Khách bận họp đột xuất nên hủy khảo sát sơ bộ, đổi sang hẹn sửa trực tiếp.");

        // Lịch 4: COMPLETED - Sửa chữa (Test trạng thái kết thúc Đã hoàn thành)
        createAppointment(reqCompleted.getId(), customerId, tech01Id, "REPAIR",
                LocalDate.now().minusDays(3), LocalTime.of(10, 0),
                AppointmentStatus.COMPLETED,
                "210 Phan Xích Long, Quận Bình Thạnh, TP.HCM",
                "Đã hoàn thành sửa chữa thay lốc và bàn giao nghiệm thu đúng hẹn.",
                null);

        // Lịch 5: CONFIRMED - BẢO HÀNH (Test cuộc hẹn bảo hành định kỳ sau hoàn thành)
        createAppointment(reqCompleted.getId(), customerId, tech01Id, "WARRANTY",
                LocalDate.now().plusDays(5), LocalTime.of(15, 30),
                AppointmentStatus.CONFIRMED,
                "210 Phan Xích Long, Quận Bình Thạnh, TP.HCM",
                "Tái kiểm tra áp suất ga và bảo dưỡng định kỳ sau 1 tuần thay lốc theo cam kết bảo hành 6 tháng.",
                null);

        log.info(">>> Đã nạp thành công 11 yêu cầu kiểm thử và 5 lịch hẹn (RC-48) cho Thợ!");
    }

    private RepairRequestJpaEntity createTestJob(String requestCode, Long customerId, Long technicianId,
                                                 Long categoryId, Long areaId, RequestStatus status,
                                                 String title, String description, String address,
                                                 String budgetRef, LocalDateTime applyDeadline,
                                                 LocalDateTime requestedTime, int daysAgo) {
        RepairRequestJpaEntity req = new RepairRequestJpaEntity();
        req.setRequestCode(requestCode);
        req.setCustomerId(customerId);
        req.setTechnicianId(technicianId);
        req.setCategoryId(categoryId);
        req.setAreaId(areaId);
        req.setStatus(status);
        req.setTitle(title);
        req.setDescription(description);
        req.setAddress(address);
        BigDecimal price = new BigDecimal(budgetRef);
        req.setBudgetRef(price);
        req.setAgreedPrice(technicianId != null ? price : BigDecimal.ZERO);
        req.setDepositAmount(technicianId != null
                ? price.multiply(new BigDecimal("0.3")).setScale(0, RoundingMode.CEILING)
                : BigDecimal.ZERO);
        req.setApplyDeadline(applyDeadline);
        req.setRequestedTime(requestedTime);
        req.setCreatedBy(customerId);
        req.setUpdatedBy(customerId);
        req.setCreatedAt(LocalDateTime.now().minusDays(daysAgo));
        req.setUpdatedAt(LocalDateTime.now().minusDays(daysAgo));
        return repairRequestRepository.save(req);
    }

    private void recordProgress(Long requestId, RequestStatus from, RequestStatus to, String note, Long userId, LocalDateTime time) {
        WorkProgressJpaEntity wp = new WorkProgressJpaEntity();
        wp.setRequestId(requestId);
        wp.setFromStatus(from != null ? from : to);
        wp.setToStatus(to);
        wp.setNote(note);
        wp.setCreatedBy(userId);
        wp.setCreatedAt(time != null ? time : LocalDateTime.now());
        workProgressRepository.save(wp);
    }

    private QuotationJpaEntity recordAcceptedQuotation(Long requestId, Long technicianId, BigDecimal price, LocalDateTime acceptedAt) {
        QuotationJpaEntity q = new QuotationJpaEntity();
        q.setRequestId(requestId);
        q.setTechnicianId(technicianId);
        q.setSolution("Thợ nhận việc theo ngân sách và tiêu chuẩn chất lượng FixLink");
        q.setPriceLaborVnd(price);
        q.setPriceMaterialsVnd(BigDecimal.ZERO);
        q.setStatus(QuotationStatus.ACCEPTED);
        q.setAcceptedAt(acceptedAt != null ? acceptedAt : LocalDateTime.now());
        q.setCreatedBy(technicianId);
        return quotationRepository.save(q);
    }

    private AppointmentJpaEntity createAppointment(Long requestId, Long customerId, Long technicianId,
                                                   String type, LocalDate date, LocalTime time,
                                                   AppointmentStatus status, String address, String notes, String cancelReason) {
        AppointmentJpaEntity appt = AppointmentJpaEntity.builder()
                .repairRequestId(requestId)
                .customerId(customerId)
                .technicianId(technicianId)
                .appointmentType(type)
                .scheduledDate(date)
                .scheduledTime(time)
                .status(status)
                .address(address)
                .notes(notes)
                .cancelReason(cancelReason)
                .build();
        appt.setCreatedBy(technicianId);
        return appointmentRepository.save(appt);
    }

    private void saveMedia(Long requestId, Long uploaderId, int seedIndex) {
        mediaRepository.saveAll(demoMediaFor(requestId, seedIndex, uploaderId));
    }

    private WorkProgressJpaEntity createProgress(Long requestId, RequestStatus from, RequestStatus to,
                                                 String note, LocalDateTime time, Long createdBy) {
        WorkProgressJpaEntity wp = new WorkProgressJpaEntity();
        wp.setRequestId(requestId);
        wp.setFromStatus(from);
        wp.setToStatus(to);
        wp.setNote(note);
        wp.setCreatedAt(time != null ? time : LocalDateTime.now());
        wp.setCreatedBy(createdBy);
        return wp;
    }

    private RepairRequestJpaEntity demoRequest(String requestCode, Long customerId, Long technicianId,
                                               Long categoryId, RequestStatus status, String title,
                                               String description, String address, int daysAgo, String agreedPrice) {
        return demoRequest(requestCode, customerId, technicianId, categoryId, status, title, description, address, daysAgo, agreedPrice,
                LocalDateTime.now().plusDays(3), null);
    }

    private RepairRequestJpaEntity demoRequest(String requestCode, Long customerId, Long technicianId,
                                               Long categoryId, RequestStatus status, String title,
                                               String description, String address, int daysAgo, String agreedPrice,
                                               LocalDateTime applyDeadline, String cancelReason) {
        RepairRequestJpaEntity request = new RepairRequestJpaEntity();
        request.setRequestCode(requestCode);
        request.setCustomerId(customerId);
        request.setTechnicianId(technicianId);
        request.setCategoryId(categoryId);
        request.setStatus(status);
        request.setTitle(title);
        request.setDescription(description);
        request.setAddress(address);
        Long areaId = 1L;
        if (address != null && address.contains("Quận 7")) areaId = 2L;
        else if (address != null && address.contains("Bình Thạnh")) areaId = 3L;
        else if (address != null && address.contains("Cầu Giấy")) areaId = 4L;
        else if (address != null && address.contains("Thanh Xuân")) areaId = 5L;
        request.setAreaId(areaId);
        request.setApplyDeadline(applyDeadline);
        request.setCancelReason(cancelReason);
        request.setRequestedTime(LocalDateTime.now().plusDays(2).minusDays(daysAgo));
        BigDecimal price = new BigDecimal(agreedPrice != null ? agreedPrice : "0");
        request.setAgreedPrice(price);
        request.setBudgetRef(price);
        request.setDepositAmount(BigDecimal.ZERO);
        request.setCreatedBy(customerId);
        request.setUpdatedBy(customerId);
        request.setCreatedAt(LocalDateTime.now().minusDays(daysAgo));
        request.setUpdatedAt(LocalDateTime.now().minusDays(daysAgo));
        return request;
    }

    private List<MediaJpaEntity> demoMediaFor(Long requestId, int index, Long uploaderId) {
        String[] seeds = {"fixlink-dien-lanh", "fixlink-nuoc", "fixlink-dien", "fixlink-gia-dung", "fixlink-khoa-cua"};
        String seed = seeds[index % seeds.length];

        List<MediaJpaEntity> mediaList = new ArrayList<>();
        mediaList.add(demoMedia(requestId, "https://picsum.photos/seed/" + seed + "-truoc/800/600", uploaderId));
        if (index % 2 == 0) {
            mediaList.add(demoMedia(requestId, "https://picsum.photos/seed/" + seed + "-sau/800/600", uploaderId));
        }
        return mediaList;
    }

    private MediaJpaEntity demoMedia(Long requestId, String url, Long uploaderId) {
        MediaJpaEntity media = new MediaJpaEntity();
        media.setOwnerType(Media.OWNER_REPAIR_REQUEST);
        media.setOwnerId(requestId);
        media.setMediaType(MediaType.IMAGE);
        media.setUrl(url);
        media.setUploadedBy(uploaderId);
        media.setCreatedBy(uploaderId);
        return media;
    }
}
