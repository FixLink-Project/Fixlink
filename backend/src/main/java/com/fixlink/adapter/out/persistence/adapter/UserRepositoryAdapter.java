package com.fixlink.adapter.out.persistence.adapter;

import com.fixlink.adapter.out.persistence.entity.UserJpaEntity;
import com.fixlink.adapter.out.persistence.mapper.UserMapper;
import com.fixlink.adapter.out.persistence.repository.SpringDataCustomerProfileRepository;
import com.fixlink.adapter.out.persistence.repository.SpringDataTechnicianProfileRepository;
import com.fixlink.adapter.out.persistence.repository.SpringDataUserRepository;
import com.fixlink.application.port.in.UserManagementUseCase.UserPageResult;
import com.fixlink.application.port.in.UserManagementUseCase.UserQuery;
import com.fixlink.application.port.out.UserRepositoryPort;
import com.fixlink.domain.model.Role;
import com.fixlink.domain.model.User;
import com.fixlink.domain.model.UserStatus;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepositoryPort {

    private final SpringDataUserRepository userRepository;
    private final SpringDataCustomerProfileRepository customerProfileRepository;
    private final SpringDataTechnicianProfileRepository technicianProfileRepository;
    private final UserMapper userMapper;

    @Override
    public Optional<User> findById(Long id) {
        return userRepository.findByIdAndDeletedAtIsNull(id)
                .map(this::enrichWithProfile);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsernameAndDeletedAtIsNull(username)
                .map(this::enrichWithProfile);
    }

    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsernameAndDeletedAtIsNull(username);
    }

    @Override
    public User save(User user) {
        UserJpaEntity entity;
        if (user.getId() != null) {
            entity = userRepository.findById(user.getId()).orElse(userMapper.toJpaEntity(user));
            entity.setUsername(user.getUsername());
            entity.setPasswordHash(user.getPasswordHash());
            entity.setRole(user.getRole());
            entity.setStatus(user.getStatus());
        } else {
            entity = userMapper.toJpaEntity(user);
        }
        UserJpaEntity saved = userRepository.save(entity);
        return enrichWithProfile(saved);
    }

    @Override
    public UserPageResult findAll(UserQuery query) {
        int page = Math.max(1, query.page());
        int limit = Math.min(Math.max(1, query.limit()), 100);

        String sortProp = (query.sortBy() != null && !query.sortBy().isBlank()) ? query.sortBy() : "createdAt";
        Sort.Direction direction = "ASC".equalsIgnoreCase(query.sortOrder()) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by(direction, sortProp));

        Specification<UserJpaEntity> spec = (root, q, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNull(root.get("deletedAt")));

            if (query.role() != null && !query.role().isBlank() && !"ALL".equalsIgnoreCase(query.role())) {
                try {
                    Role roleEnum = Role.valueOf(query.role().toUpperCase());
                    predicates.add(cb.equal(root.get("role"), roleEnum));
                } catch (IllegalArgumentException ignored) {
                }
            }

            if (query.status() != null && !query.status().isBlank() && !"ALL".equalsIgnoreCase(query.status())) {
                try {
                    UserStatus statusEnum = UserStatus.valueOf(query.status().toUpperCase());
                    predicates.add(cb.equal(root.get("status"), statusEnum));
                } catch (IllegalArgumentException ignored) {
                }
            }

            if (query.search() != null && !query.search().isBlank()) {
                String searchPattern = "%" + query.search().trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("username")), searchPattern));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<UserJpaEntity> pageEntity = userRepository.findAll(spec, pageable);
        List<User> users = pageEntity.getContent().stream()
                .map(this::enrichWithProfile)
                .toList();

        return new UserPageResult(
                users,
                page,
                limit,
                pageEntity.getTotalElements(),
                pageEntity.getTotalPages()
        );
    }

    private User enrichWithProfile(UserJpaEntity entity) {
        User user = userMapper.toDomain(entity);
        if (user.getRole() == Role.CUSTOMER) {
            customerProfileRepository.findByUserIdAndDeletedAtIsNull(user.getId())
                    .ifPresent(cp -> user.setCustomerProfile(userMapper.toDomain(cp)));
        } else if (user.getRole() == Role.TECHNICIAN) {
            technicianProfileRepository.findByUserIdAndDeletedAtIsNull(user.getId())
                    .ifPresent(tp -> user.setTechnicianProfile(userMapper.toDomain(tp)));
        }
        return user;
    }
}
