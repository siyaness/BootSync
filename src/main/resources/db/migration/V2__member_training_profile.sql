CREATE TABLE member_training_profile (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    member_id BIGINT UNSIGNED NOT NULL,
    course_label VARCHAR(80) NULL,
    course_start_date DATE NOT NULL,
    course_end_date DATE NOT NULL,
    attendance_threshold_percent INT NOT NULL,
    monthly_base_amount INT NOT NULL,
    absence_deduction_amount INT NOT NULL,
    training_days_csv VARCHAR(64) NOT NULL,
    holiday_dates_csv TEXT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_member_training_profile PRIMARY KEY (id),
    CONSTRAINT uk_member_training_profile_member UNIQUE (member_id),
    CONSTRAINT fk_member_training_profile_member FOREIGN KEY (member_id) REFERENCES member (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
