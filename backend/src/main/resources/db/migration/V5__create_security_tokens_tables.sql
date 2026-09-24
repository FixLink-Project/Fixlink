-- =============================================================================
-- FIXLINK - SECURITY TOKENS & BLACKLIST SCHEMA
-- Tables for password reset, refresh token rotation, and JWT blacklist
-- =============================================================================

CREATE TABLE IF NOT EXISTS password_reset_tokens (
    token       VARCHAR(255) PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    email       VARCHAR(100),
    expires_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    is_used     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_password_reset_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_password_reset_user ON password_reset_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_password_reset_email ON password_reset_tokens(email);

CREATE TABLE IF NOT EXISTS refresh_tokens (
    token       VARCHAR(255) PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    expires_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    is_revoked  BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user ON refresh_tokens(user_id);

CREATE TABLE IF NOT EXISTS token_blacklist (
    jti         VARCHAR(255) PRIMARY KEY,
    user_id     BIGINT,
    expires_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_token_blacklist_expires ON token_blacklist(expires_at);
