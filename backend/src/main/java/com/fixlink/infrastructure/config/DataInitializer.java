package com.fixlink.infrastructure.config;

import com.fixlink.adapter.out.persistence.entity.CustomerProfileJpaEntity;
import com.fixlink.adapter.out.persistence.entity.ServiceAreaJpaEntity;
import com.fixlink.adapter.out.persistence.entity.TechnicianProfileJpaEntity;
import com.fixlink.adapter.out.persistence.entity.UserJpaEntity;
import com.fixlink.adapter.out.persistence.repository.SpringDataCustomerProfileRepository;
import com.fixlink.adapter.out.persistence.repository.SpringDataServiceAreaRepository;
import com.fixlink.adapter.out.persistence.repository.SpringDataTechnicianProfileRepository;
import com.fixlink.adapter.out.persistence.repository.SpringDataUserRepository;
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
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final SpringDataUserRepository userRepository;
    private final SpringDataCustomerProfileRepository customerProfileRepository;
    private final SpringDataTechnicianProfileRepository technicianProfileRepository;
    private final SpringDataServiceAreaRepository serviceAreaRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(SpringDataUserRepository userRepository,
                           SpringDataCustomerProfileRepository customerProfileRepository,
                           SpringDataTechnicianProfileRepository technicianProfileRepository,
                           SpringDataServiceAreaRepository serviceAreaRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.customerProfileRepository = customerProfileRepository;
        this.technicianProfileRepository = technicianProfileRepository;
        this.serviceAreaRepository = serviceAreaRepository;
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
            tech.setStatus(UserStatus.PENDING);
            UserJpaEntity savedTech = userRepository.save(tech);

            TechnicianProfileJpaEntity techProfile = new TechnicianProfileJpaEntity();
            techProfile.setUser(savedTech);
            techProfile.setFullName("Trần Văn B");
            techProfile.setPhone("0987654321");
            techProfile.setEmail("tranvanb@gmail.com");
            techProfile.setCitizenId("012345678901");
            techProfile.setIdCardFrontUrl("https://s3.fixlink.vn/temp/id_front.jpg");
            techProfile.setIdCardBackUrl("https://s3.fixlink.vn/temp/id_back.jpg");
            techProfile.setBio("Chuyên sửa máy lạnh, tủ lạnh, bảo dưỡng điều hòa 5 năm kinh nghiệm");
            techProfile.setYearsExperience(5);
            techProfile.setVerificationStatus(VerificationStatus.PENDING);
            techProfile.setAvgRating(BigDecimal.ZERO);
            techProfile.setCompletedJobs(0);
            techProfile.setWalletBalance(BigDecimal.ZERO);
            techProfile.setIsOnline(false);
            technicianProfileRepository.save(techProfile);
            log.info(">>> Đã khởi tạo tài khoản Thợ mẫu chờ duyệt: username=tho_dien_lanh_01 / password=Password@123");
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

        // 6. Seed Service Areas (RC-18)
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
    }
}
