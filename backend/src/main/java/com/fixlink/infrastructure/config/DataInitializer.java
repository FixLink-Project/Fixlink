package com.fixlink.infrastructure.config;

import com.fixlink.adapter.out.persistence.entity.*;
import com.fixlink.adapter.out.persistence.repository.*;
import com.fixlink.domain.model.AppointmentStatus;
import com.fixlink.domain.model.Media;
import com.fixlink.domain.model.MediaType;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(SpringDataUserRepository userRepository,
                           SpringDataCustomerProfileRepository customerProfileRepository,
                           SpringDataTechnicianProfileRepository technicianProfileRepository,
                           SpringDataServiceAreaRepository serviceAreaRepository,
                           SpringDataRepairRequestRepository repairRequestRepository,
                           SpringDataMediaRepository mediaRepository,
                           SpringDataAppointmentRepository appointmentRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.customerProfileRepository = customerProfileRepository;
        this.technicianProfileRepository = technicianProfileRepository;
        this.serviceAreaRepository = serviceAreaRepository;
        this.repairRequestRepository = repairRequestRepository;
        this.mediaRepository = mediaRepository;
        this.appointmentRepository = appointmentRepository;
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

                demoRequests.add(demoRequest("REQ-DEMO-0006", customerId, thoId, 2L, RequestStatus.BIDDING_OPEN,
                        "Thông tắc đường ống thoát sàn nhà tắm",
                        "Sàn nhà tắm thoát nước rất chậm, có mùi hôi bốc lên, nghi ngờ tắc tại cổ góp.",
                        "56 Điện Biên Phủ, Quận 3, TP.HCM", 4, "0"));

                demoRequests.add(demoRequest("REQ-DEMO-0007", customerId, null, 1L, RequestStatus.MATCHED_AWAITING_DEPOSIT,
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
                        "9 Tạ Quang Bửu, Quận 8, TP.HCM", 10, "0"));

                demoRequests.add(demoRequest("REQ-DEMO-0014", customerId, null, 1L, RequestStatus.PENDING,
                        "Máy giặt không vắt và báo lỗi UE",
                        "Máy giặt cửa trước báo lỗi UE, lồng giặt không vắt được và còn nhiều nước.",
                        "66 Nguyễn Ảnh Thủ, Quận 12, TP.HCM", 11, "0"));

                demoRequests.add(demoRequest("REQ-DEMO-0015", customerId, thoId, 2L, RequestStatus.BIDDING_OPEN,
                        "Lắp đặt bình nóng lạnh năng lượng mặt trời",
                        "Cần khảo sát mái nhà và lắp bình nóng lạnh 150L đã mua sẵn, kèm đường ống và van.",
                        "404 Lạc Long Quân, Tây Hồ, Hà Nội", 13, "0"));

                List<RepairRequestJpaEntity> savedRequests = repairRequestRepository.saveAll(demoRequests);

                List<MediaJpaEntity> demoMedia = new ArrayList<>();
                for (int index = 0; index < savedRequests.size(); index++) {
                    demoMedia.addAll(demoMediaFor(savedRequests.get(index).getId(), index, customerId));
                }
                mediaRepository.saveAll(demoMedia);

                log.info(">>> Đã khởi tạo {} yêu cầu sửa chữa demo kèm {} tệp đính kèm (ảnh Firebase Storage demo)",
                        savedRequests.size(), demoMedia.size());

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
    }

    private RepairRequestJpaEntity demoRequest(String requestCode, Long customerId, Long technicianId,
                                               Long categoryId, RequestStatus status, String title,
                                               String description, String address, int daysAgo, String agreedPrice) {
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
        request.setBiddingDeadline(LocalDateTime.now().plusDays(7));
        request.setRequestedTime(LocalDateTime.now().plusDays(2).minusDays(daysAgo));
        request.setAgreedPrice(new BigDecimal(agreedPrice));
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
