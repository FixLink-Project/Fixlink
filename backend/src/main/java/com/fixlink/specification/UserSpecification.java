package com.fixlink.specification;

import com.fixlink.entity.CustomerProfile;
import com.fixlink.entity.TechnicianProfile;
import com.fixlink.entity.User;
import com.fixlink.enums.UserRole;
import com.fixlink.enums.UserStatus;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Dynamic Specification builder for User queries.
 * Supports: search (multi-field), filter by role, filter by status, soft-delete exclusion.
 */
public class UserSpecification {

    private UserSpecification() {
    }

    /**
     * Build composite specification from query parameters (RC-19).
     */
    public static Specification<User> buildSpec(String search, String role, String status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Exclude soft-deleted users
            predicates.add(cb.isNull(root.get("deletedAt")));

            // Filter by role
            if (role != null && !role.isBlank() && !role.equalsIgnoreCase("ALL")) {
                try {
                    UserRole userRole = UserRole.valueOf(role.toUpperCase());
                    predicates.add(cb.equal(root.get("role"), userRole));
                } catch (IllegalArgumentException ignored) {
                    // Invalid role filter → ignore
                }
            }

            // Filter by status
            if (status != null && !status.isBlank() && !status.equalsIgnoreCase("ALL")) {
                String mappedStatus = mapApiStatusToDb(status);
                if (mappedStatus != null) {
                    try {
                        UserStatus userStatus = UserStatus.valueOf(mappedStatus);
                        predicates.add(cb.equal(root.get("status"), userStatus));
                    } catch (IllegalArgumentException ignored) {
                        // Invalid status filter → ignore
                    }
                }
            }

            // Search across multiple fields (username, customer name, technician name, email, phone)
            if (search != null && !search.isBlank()) {
                String searchPattern = "%" + search.toLowerCase() + "%";

                // Join to profiles for searching by name, email, phone
                Join<User, CustomerProfile> customerJoin = root.join("customerProfile", JoinType.LEFT);
                Join<User, TechnicianProfile> technicianJoin = root.join("technicianProfile", JoinType.LEFT);

                Predicate searchPredicate = cb.or(
                        cb.like(cb.lower(root.get("username")), searchPattern),
                        cb.like(cb.lower(customerJoin.get("name")), searchPattern),
                        cb.like(cb.lower(customerJoin.get("phone")), searchPattern),
                        cb.like(cb.lower(customerJoin.get("email")), searchPattern),
                        cb.like(cb.lower(technicianJoin.get("name")), searchPattern),
                        cb.like(cb.lower(technicianJoin.get("phone")), searchPattern),
                        cb.like(cb.lower(technicianJoin.get("email")), searchPattern),
                        cb.like(cb.lower(technicianJoin.get("idCardNumber")), searchPattern)
                );
                predicates.add(searchPredicate);

                // Prevent duplicate results from joins
                query.distinct(true);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Map API status names to DB enum values.
     * API uses BLOCKED/PENDING, DB uses BANNED/INACTIVE.
     */
    private static String mapApiStatusToDb(String apiStatus) {
        return switch (apiStatus.toUpperCase()) {
            case "BLOCKED" -> "BANNED";
            case "PENDING" -> "INACTIVE";
            case "ACTIVE" -> "ACTIVE";
            default -> apiStatus.toUpperCase();
        };
    }
}
