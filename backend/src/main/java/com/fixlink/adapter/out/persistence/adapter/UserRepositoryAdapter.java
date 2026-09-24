package com.fixlink.adapter.out.persistence.adapter;

import com.fixlink.adapter.out.persistence.entity.CustomerProfileJpaEntity;
import com.fixlink.adapter.out.persistence.entity.TechnicianProfileJpaEntity;
import com.fixlink.adapter.out.persistence.entity.UserJpaEntity;
import com.fixlink.adapter.out.persistence.mapper.UserMapper;
import com.fixlink.adapter.out.persistence.repository.SpringDataUserRepository;
import com.fixlink.application.port.in.UserManagementUseCase.UserPageResult;
import com.fixlink.application.port.in.UserManagementUseCase.UserQuery;
import com.fixlink.application.port.out.UserRepositoryPort;
import com.fixlink.domain.model.Role;
import com.fixlink.domain.model.User;
import com.fixlink.domain.model.UserStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class UserRepositoryAdapter implements UserRepositoryPort {

    private final SpringDataUserRepository springDataUserRepository;
    private final UserMapper userMapper;

    public UserRepositoryAdapter(SpringDataUserRepository springDataUserRepository, UserMapper userMapper) {
        this.springDataUserRepository = springDataUserRepository;
        this.userMapper = userMapper;
    }

    @Override
    public User save(User user) {
        UserJpaEntity entity = userMapper.toEntity(user);
        UserJpaEntity saved = springDataUserRepository.save(entity);
        return userMapper.toDomain(saved);
    }

    @Override
    public Optional<User> findById(Long id) {
        return springDataUserRepository.findByIdAndDeletedAtIsNull(id)
                .map(userMapper::toDomain);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return springDataUserRepository.findByUsernameAndDeletedAtIsNull(username)
                .map(userMapper::toDomain);
    }

    @Override
    public boolean existsByUsername(String username) {
        return springDataUserRepository.existsByUsernameAndDeletedAtIsNull(username);
    }

    @Override
    public UserPageResult findAll(UserQuery query) {
        int pageNumber = Math.max(query.page() - 1, 0); // Convert 1-indexed to 0-indexed
        int pageSize = query.limit() > 0 ? query.limit() : 10;

        Sort.Direction direction = "ASC".equalsIgnoreCase(query.sortOrder()) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String sortProperty = (query.sortBy() != null && !query.sortBy().isBlank()) ? query.sortBy() : "createdAt";
        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(direction, sortProperty));

        Specification<UserJpaEntity> spec = (root, criteriaQuery, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Exclude soft-deleted records
            predicates.add(cb.isNull(root.get("deletedAt")));

            // Filter by role
            if (query.role() != null && !query.role().isBlank() && !"ALL".equalsIgnoreCase(query.role())) {
                try {
                    Role roleEnum = Role.valueOf(query.role().toUpperCase());
                    predicates.add(cb.equal(root.get("role"), roleEnum));
                } catch (IllegalArgumentException ignored) {}
            }

            // Filter by status
            if (query.status() != null && !query.status().isBlank() && !"ALL".equalsIgnoreCase(query.status())) {
                try {
                    UserStatus statusEnum = UserStatus.valueOf(query.status().toUpperCase());
                    predicates.add(cb.equal(root.get("status"), statusEnum));
                } catch (IllegalArgumentException ignored) {}
            }

            // Search by keyword (username, customer fullName, technician fullName, phone, email)
            if (query.search() != null && !query.search().trim().isBlank()) {
                String pattern = "%" + query.search().trim().toLowerCase() + "%";

                Join<UserJpaEntity, CustomerProfileJpaEntity> custJoin = root.join("customerProfile", JoinType.LEFT);
                Join<UserJpaEntity, TechnicianProfileJpaEntity> techJoin = root.join("technicianProfile", JoinType.LEFT);

                Predicate usernamePredicate = cb.like(cb.lower(root.get("username")), pattern);
                Predicate custNamePredicate = cb.like(cb.lower(custJoin.get("fullName")), pattern);
                Predicate custPhonePredicate = cb.like(cb.lower(custJoin.get("phone")), pattern);
                Predicate custEmailPredicate = cb.like(cb.lower(custJoin.get("email")), pattern);

                Predicate techNamePredicate = cb.like(cb.lower(techJoin.get("fullName")), pattern);
                Predicate techPhonePredicate = cb.like(cb.lower(techJoin.get("phone")), pattern);
                Predicate techEmailPredicate = cb.like(cb.lower(techJoin.get("email")), pattern);

                predicates.add(cb.or(
                        usernamePredicate,
                        custNamePredicate, custPhonePredicate, custEmailPredicate,
                        techNamePredicate, techPhonePredicate, techEmailPredicate
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<UserJpaEntity> pageResult = springDataUserRepository.findAll(spec, pageable);
        List<User> users = pageResult.getContent().stream()
                .map(userMapper::toDomain)
                .toList();

        return new UserPageResult(
                users,
                query.page(),
                pageSize,
                pageResult.getTotalElements(),
                pageResult.getTotalPages()
        );
    }
}
