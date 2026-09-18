package com.fixlink.config;

import com.fixlink.entity.CustomerProfile;
import com.fixlink.entity.ServiceCategory;
import com.fixlink.entity.ServiceItem;
import com.fixlink.entity.TechnicianProfile;
import com.fixlink.entity.User;
import com.fixlink.enums.UserRole;
import com.fixlink.enums.UserStatus;
import com.fixlink.enums.VerificationStatus;
import com.fixlink.repository.ServiceCategoryRepository;
import com.fixlink.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ServiceCategoryRepository serviceCategoryRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Database already contains data, skipping DataInitializer.");
            return;
        }

        log.info("Seeding initial data for FixLink application...");

        // 1. Admin account
        User admin = User.builder()
                .username("admin")
                .passwordHash(passwordEncoder.encode("admin123"))
                .role(UserRole.ADMIN)
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();
        userRepository.save(admin);
        log.info("Created Admin account: username='admin', password='admin123'");

        // 2. Approved Technician (tech_an)
        User tech1User = User.builder()
                .username("tech_an")
                .passwordHash(passwordEncoder.encode("tech123"))
                .role(UserRole.TECHNICIAN)
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now().minusDays(30))
                .build();

        TechnicianProfile tech1Profile = TechnicianProfile.builder()
                .user(tech1User)
                .name("Nguyễn Văn An")
                .phone("0901234567")
                .email("an.nguyen@example.com")
                .avatarUrl("https://ui-avatars.com/api/?name=Nguyen+Van+An")
                .idCardNumber("079090123456")
                .bio("Chuyên sửa chữa điện gia dụng, tủ lạnh, điều hòa 5 năm kinh nghiệm.")
                .yearsExperience((short) 5)
                .isVerified(true)
                .verificationStatus(VerificationStatus.APPROVED)
                .avgRating(BigDecimal.valueOf(4.85))
                .completedJobs(42)
                .verifiedAt(LocalDateTime.now().minusDays(20))
                .verifiedBy(admin.getId())
                .verificationNote("Hồ sơ và chứng chỉ tay nghề đầy đủ, xác thực CCCD hợp lệ")
                .build();
        tech1User.setTechnicianProfile(tech1Profile);
        userRepository.save(tech1User);

        // 3. Pending Technician (tech_binh) - KYC cần duyệt (RC-22)
        User tech2User = User.builder()
                .username("tech_binh")
                .passwordHash(passwordEncoder.encode("tech123"))
                .role(UserRole.TECHNICIAN)
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now().minusDays(3))
                .build();

        TechnicianProfile tech2Profile = TechnicianProfile.builder()
                .user(tech2User)
                .name("Trần Thị Bình")
                .phone("0902345678")
                .email("binh.tran@example.com")
                .avatarUrl("https://ui-avatars.com/api/?name=Tran+Thi+Binh")
                .idCardNumber("079090234567")
                .bio("Thợ sửa máy giặt, lò vi sóng các dòng máy cửa trước & cửa trên.")
                .yearsExperience((short) 3)
                .isVerified(false)
                .verificationStatus(VerificationStatus.PENDING)
                .avgRating(BigDecimal.valueOf(5.0))
                .completedJobs(0)
                .build();
        tech2User.setTechnicianProfile(tech2Profile);
        userRepository.save(tech2User);

        // 4. Banned Technician (tech_cuong) - Tài khoản bị khóa (RC-21)
        User tech3User = User.builder()
                .username("tech_cuong")
                .passwordHash(passwordEncoder.encode("tech123"))
                .role(UserRole.TECHNICIAN)
                .status(UserStatus.BANNED)
                .createdAt(LocalDateTime.now().minusDays(60))
                .statusChangedBy(admin.getId())
                .statusChangedAt(LocalDateTime.now().minusDays(5))
                .statusReason("Nhận tiền trực tiếp không qua hệ thống nhiều lần, bị khách hàng phản ánh")
                .build();

        TechnicianProfile tech3Profile = TechnicianProfile.builder()
                .user(tech3User)
                .name("Lê Văn Cường")
                .phone("0903456789")
                .email("cuong.le@example.com")
                .avatarUrl("https://ui-avatars.com/api/?name=Le+Van+Cuong")
                .idCardNumber("079090345678")
                .bio("Sửa chữa điện nước, ống nước gia đình.")
                .yearsExperience((short) 2)
                .isVerified(true)
                .verificationStatus(VerificationStatus.APPROVED)
                .avgRating(BigDecimal.valueOf(3.2))
                .completedJobs(15)
                .build();
        tech3User.setTechnicianProfile(tech3Profile);
        userRepository.save(tech3User);

        // 5. Active Customer (cust_dung)
        User cust1User = User.builder()
                .username("cust_dung")
                .passwordHash(passwordEncoder.encode("cust123"))
                .role(UserRole.CUSTOMER)
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now().minusDays(15))
                .build();

        CustomerProfile cust1Profile = CustomerProfile.builder()
                .user(cust1User)
                .name("Phạm Thị Dung")
                .phone("0904567890")
                .email("dung.pham@example.com")
                .avatarUrl("https://ui-avatars.com/api/?name=Pham+Thi+Dung")
                .loyaltyPoints(150)
                .build();
        cust1User.setCustomerProfile(cust1Profile);
        userRepository.save(cust1User);

        // 6. Inactive Customer (cust_em)
        User cust2User = User.builder()
                .username("cust_em")
                .passwordHash(passwordEncoder.encode("cust123"))
                .role(UserRole.CUSTOMER)
                .status(UserStatus.INACTIVE)
                .createdAt(LocalDateTime.now().minusDays(45))
                .build();

        CustomerProfile cust2Profile = CustomerProfile.builder()
                .user(cust2User)
                .name("Hoàng Văn Em")
                .phone("0905678901")
                .email("em.hoang@example.com")
                .avatarUrl("https://ui-avatars.com/api/?name=Hoang+Van+Em")
                .loyaltyPoints(0)
                .build();
        cust2User.setCustomerProfile(cust2Profile);
        userRepository.save(cust2User);

        // 7. Dedicated Banned User (blocked_user) for testing 403 Forbidden login
        User blockedUser = User.builder()
                .username("blocked_user")
                .passwordHash(passwordEncoder.encode("blocked123"))
                .role(UserRole.CUSTOMER)
                .status(UserStatus.BANNED)
                .createdAt(LocalDateTime.now().minusDays(10))
                .statusReason("Tài khoản bị khóa vĩnh viễn do vi phạm điều khoản")
                .build();
        userRepository.save(blockedUser);

        log.info("Sample data successfully seeded! Total users: {}", userRepository.count());

        // ===============================================================
        // RC-4: Seed Service Catalog data
        // ===============================================================
        seedServiceCatalog();
    }

    /**
     * RC-4: Seed service categories and services.
     */
    private void seedServiceCatalog() {
        log.info("Seeding service catalog data (RC-4)...");

        // --- Category 1: Điện ---
        ServiceCategory catDien = ServiceCategory.builder()
                .name("Điện")
                .description("Sửa chữa, lắp đặt hệ thống điện gia đình và công nghiệp")
                .iconUrl("https://img.icons8.com/fluency/96/electrical.png")
                .sortOrder(1)
                .isActive(true)
                .build();

        catDien.getServices().add(ServiceItem.builder()
                .category(catDien)
                .name("Sửa chập điện")
                .description("Kiểm tra, xác định và sửa chữa các điểm chập, rò rỉ điện trong nhà")
                .estimatedPrice(BigDecimal.valueOf(200000))
                .unit("lần")
                .sortOrder(1)
                .build());

        catDien.getServices().add(ServiceItem.builder()
                .category(catDien)
                .name("Lắp đặt ổ cắm, công tắc")
                .description("Lắp mới hoặc thay thế ổ cắm điện, công tắc các loại")
                .estimatedPrice(BigDecimal.valueOf(100000))
                .unit("điểm")
                .sortOrder(2)
                .build());

        catDien.getServices().add(ServiceItem.builder()
                .category(catDien)
                .name("Thay bóng đèn, đèn LED")
                .description("Thay thế bóng đèn, lắp đặt đèn LED chiếu sáng")
                .estimatedPrice(BigDecimal.valueOf(80000))
                .unit("bóng")
                .sortOrder(3)
                .build());

        serviceCategoryRepository.save(catDien);

        // --- Category 2: Nước ---
        ServiceCategory catNuoc = ServiceCategory.builder()
                .name("Nước")
                .description("Sửa chữa hệ thống cấp thoát nước, đường ống, vòi nước")
                .iconUrl("https://img.icons8.com/fluency/96/plumbing.png")
                .sortOrder(2)
                .isActive(true)
                .build();

        catNuoc.getServices().add(ServiceItem.builder()
                .category(catNuoc)
                .name("Sửa ống nước rò rỉ")
                .description("Xác định và sửa chữa các vị trí ống nước bị rò rỉ, vỡ")
                .estimatedPrice(BigDecimal.valueOf(250000))
                .unit("lần")
                .sortOrder(1)
                .build());

        catNuoc.getServices().add(ServiceItem.builder()
                .category(catNuoc)
                .name("Thông tắc cống, bồn cầu")
                .description("Thông tắc các loại đường ống cống, bồn cầu, lavabo")
                .estimatedPrice(BigDecimal.valueOf(300000))
                .unit("lần")
                .sortOrder(2)
                .build());

        catNuoc.getServices().add(ServiceItem.builder()
                .category(catNuoc)
                .name("Lắp đặt vòi nước, sen tắm")
                .description("Lắp mới hoặc thay thế vòi nước, sen tắm các loại")
                .estimatedPrice(BigDecimal.valueOf(150000))
                .unit("cái")
                .sortOrder(3)
                .build());

        serviceCategoryRepository.save(catNuoc);

        // --- Category 3: Điều hòa ---
        ServiceCategory catDieuHoa = ServiceCategory.builder()
                .name("Điều hòa")
                .description("Lắp đặt, bảo trì, sửa chữa điều hòa không khí các loại")
                .iconUrl("https://img.icons8.com/fluency/96/air-conditioner.png")
                .sortOrder(3)
                .isActive(true)
                .build();

        catDieuHoa.getServices().add(ServiceItem.builder()
                .category(catDieuHoa)
                .name("Vệ sinh điều hòa")
                .description("Vệ sinh dàn lạnh, dàn nóng, bổ sung gas cho điều hòa")
                .estimatedPrice(BigDecimal.valueOf(200000))
                .unit("bộ")
                .sortOrder(1)
                .build());

        catDieuHoa.getServices().add(ServiceItem.builder()
                .category(catDieuHoa)
                .name("Sửa điều hòa không lạnh")
                .description("Kiểm tra và sửa chữa điều hòa không làm lạnh, rò gas, lỗi board")
                .estimatedPrice(BigDecimal.valueOf(350000))
                .unit("lần")
                .sortOrder(2)
                .build());

        catDieuHoa.getServices().add(ServiceItem.builder()
                .category(catDieuHoa)
                .name("Lắp đặt điều hòa mới")
                .description("Lắp đặt điều hòa mới bao gồm khoan tường, đi ống đồng")
                .estimatedPrice(BigDecimal.valueOf(500000))
                .unit("bộ")
                .sortOrder(3)
                .build());

        serviceCategoryRepository.save(catDieuHoa);

        // --- Category 4: Thiết bị điện tử ---
        ServiceCategory catDienTu = ServiceCategory.builder()
                .name("Thiết bị điện tử")
                .description("Sửa chữa TV, máy giặt, tủ lạnh, lò vi sóng và thiết bị gia dụng")
                .iconUrl("https://img.icons8.com/fluency/96/electronics.png")
                .sortOrder(4)
                .isActive(true)
                .build();

        catDienTu.getServices().add(ServiceItem.builder()
                .category(catDienTu)
                .name("Sửa máy giặt")
                .description("Sửa chữa máy giặt không quay, không xả nước, lỗi board mạch")
                .estimatedPrice(BigDecimal.valueOf(300000))
                .unit("lần")
                .sortOrder(1)
                .build());

        catDienTu.getServices().add(ServiceItem.builder()
                .category(catDienTu)
                .name("Sửa tủ lạnh")
                .description("Sửa chữa tủ lạnh không lạnh, rò gas, thay block, thermostat")
                .estimatedPrice(BigDecimal.valueOf(350000))
                .unit("lần")
                .sortOrder(2)
                .build());

        catDienTu.getServices().add(ServiceItem.builder()
                .category(catDienTu)
                .name("Sửa lò vi sóng")
                .description("Sửa chữa lò vi sóng không nóng, lỗi bảng điều khiển")
                .estimatedPrice(BigDecimal.valueOf(200000))
                .unit("lần")
                .sortOrder(3)
                .build());

        catDienTu.getServices().add(ServiceItem.builder()
                .category(catDienTu)
                .name("Sửa TV")
                .description("Sửa chữa TV không lên hình, mất tiếng, lỗi main board")
                .estimatedPrice(BigDecimal.valueOf(400000))
                .unit("lần")
                .sortOrder(4)
                .build());

        serviceCategoryRepository.save(catDienTu);

        log.info("Service catalog seeded: 4 categories, 13 services.");
    }
}
