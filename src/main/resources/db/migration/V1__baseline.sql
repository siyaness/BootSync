CREATE TABLE member (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    username VARCHAR(20) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(20) NOT NULL,
    recovery_email VARCHAR(255) NULL,
    recovery_email_verified_at DATETIME(6) NULL,
    status ENUM('ACTIVE', 'PENDING_DELETE', 'DISABLED') NOT NULL DEFAULT 'ACTIVE',
    delete_requested_at DATETIME(6) NULL,
    delete_due_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_member PRIMARY KEY (id),
    CONSTRAINT uk_member_username UNIQUE (username),
    CONSTRAINT uk_member_recovery_email UNIQUE (recovery_email),
    INDEX idx_member_status_delete_due_at (status, delete_due_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE recovery_email_verification_token (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    member_id BIGINT UNSIGNED NOT NULL,
    purpose ENUM('SIGNUP_VERIFY', 'RECOVERY_EMAIL_CHANGE') NOT NULL,
    target_email VARCHAR(255) NOT NULL,
    token_hash CHAR(64) NOT NULL,
    issued_at DATETIME(6) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    consumed_at DATETIME(6) NULL,
    invalidated_at DATETIME(6) NULL,
    CONSTRAINT pk_recovery_email_verification_token PRIMARY KEY (id),
    CONSTRAINT uk_recovery_email_verification_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_recovery_email_verification_token_member FOREIGN KEY (member_id) REFERENCES member (id),
    INDEX idx_recovery_email_token_active_lookup (member_id, purpose, invalidated_at, expires_at),
    INDEX idx_recovery_email_token_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE attendance_record (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    member_id BIGINT UNSIGNED NOT NULL,
    attendance_date DATE NOT NULL,
    status VARCHAR(32) NOT NULL,
    memo VARCHAR(255) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_attendance_record PRIMARY KEY (id),
    CONSTRAINT uk_attendance_record_member_date UNIQUE (member_id, attendance_date),
    CONSTRAINT fk_attendance_record_member FOREIGN KEY (member_id) REFERENCES member (id),
    INDEX idx_attendance_record_member_date_status (member_id, attendance_date, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE snippet (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    member_id BIGINT UNSIGNED NOT NULL,
    title VARCHAR(200) NOT NULL,
    content_markdown MEDIUMTEXT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_snippet PRIMARY KEY (id),
    CONSTRAINT fk_snippet_member FOREIGN KEY (member_id) REFERENCES member (id),
    CONSTRAINT uk_snippet_id_member UNIQUE (id, member_id),
    INDEX idx_snippet_member_updated_at (member_id, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE tag (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    member_id BIGINT UNSIGNED NOT NULL,
    name VARCHAR(20) NOT NULL,
    normalized_name VARCHAR(20) NOT NULL,
    CONSTRAINT pk_tag PRIMARY KEY (id),
    CONSTRAINT fk_tag_member FOREIGN KEY (member_id) REFERENCES member (id),
    CONSTRAINT uk_tag_member_normalized_name UNIQUE (member_id, normalized_name),
    CONSTRAINT uk_tag_id_member UNIQUE (id, member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE snippet_tag (
    member_id BIGINT UNSIGNED NOT NULL,
    snippet_id BIGINT UNSIGNED NOT NULL,
    tag_id BIGINT UNSIGNED NOT NULL,
    CONSTRAINT pk_snippet_tag PRIMARY KEY (member_id, snippet_id, tag_id),
    CONSTRAINT fk_snippet_tag_member FOREIGN KEY (member_id) REFERENCES member (id),
    CONSTRAINT fk_snippet_tag_snippet FOREIGN KEY (snippet_id, member_id)
        REFERENCES snippet (id, member_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_snippet_tag_tag FOREIGN KEY (tag_id, member_id)
        REFERENCES tag (id, member_id)
        ON DELETE CASCADE,
    INDEX idx_snippet_tag_member_tag_snippet (member_id, tag_id, snippet_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE attendance_audit_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    attendance_record_id BIGINT UNSIGNED NULL,
    member_id BIGINT UNSIGNED NULL,
    action ENUM('CREATE', 'UPDATE', 'DELETE') NOT NULL,
    before_attendance_date DATE NULL,
    after_attendance_date DATE NULL,
    before_status VARCHAR(32) NULL,
    after_status VARCHAR(32) NULL,
    changed_by_member_id BIGINT UNSIGNED NULL,
    changed_at DATETIME(6) NOT NULL,
    request_ip_hmac CHAR(64) NULL,
    CONSTRAINT pk_attendance_audit_log PRIMARY KEY (id),
    INDEX idx_attendance_audit_log_member_changed_at (member_id, changed_at),
    INDEX idx_attendance_audit_log_record_changed_at (attendance_record_id, changed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
