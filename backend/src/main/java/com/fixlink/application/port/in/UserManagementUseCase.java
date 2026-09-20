package com.fixlink.application.port.in;

import com.fixlink.domain.model.User;
import com.fixlink.domain.model.UserStatus;

import java.util.List;

public interface UserManagementUseCase {

    UserPageResult getUsers(UserQuery query);

    User getUserById(Long id);

    void updateUserStatus(Long userId, UserStatus status);

    record UserQuery(
            int page,
            int limit,
            String search,
            String role,
            String status,
            String sortBy,
            String sortOrder
    ) {}

    record UserPageResult(
            List<User> users,
            int currentPage,
            int limit,
            long totalItems,
            int totalPages
    ) {}
}
