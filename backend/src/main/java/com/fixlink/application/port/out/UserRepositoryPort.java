package com.fixlink.application.port.out;

import com.fixlink.application.port.in.UserManagementUseCase.UserPageResult;
import com.fixlink.application.port.in.UserManagementUseCase.UserQuery;
import com.fixlink.domain.model.User;

import java.util.Optional;

public interface UserRepositoryPort {
    Optional<User> findById(Long id);
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    User save(User user);
    UserPageResult findAll(UserQuery query);
}
