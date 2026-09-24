package com.fixlink.application.port.out;

import com.fixlink.application.port.in.UserManagementUseCase.UserPageResult;
import com.fixlink.application.port.in.UserManagementUseCase.UserQuery;
import com.fixlink.domain.model.User;

import java.util.Optional;

public interface UserRepositoryPort {

    User save(User user);

    Optional<User> findById(Long id);

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    UserPageResult findAll(UserQuery query);
}
