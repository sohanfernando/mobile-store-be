CREATE TABLE revoked_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT,
    jti VARCHAR(36) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_revoked_tokens_jti UNIQUE (jti)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
