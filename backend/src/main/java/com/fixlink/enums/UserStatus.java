package com.fixlink.enums;

/**
 * User account status.
 * DB stores: ACTIVE, INACTIVE, BANNED
 * API spec uses: ACTIVE, BLOCKED, PENDING
 * We map BANNED ↔ BLOCKED in DTOs.
 */
public enum UserStatus {
    ACTIVE,
    INACTIVE,
    BANNED
}
