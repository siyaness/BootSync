ALTER TABLE member_training_profile
    ADD COLUMN daily_allowance_amount INT NOT NULL DEFAULT 15800 AFTER attendance_threshold_percent,
    ADD COLUMN payable_day_cap INT NOT NULL DEFAULT 20 AFTER daily_allowance_amount;

UPDATE member_training_profile
SET
    daily_allowance_amount = CASE
        WHEN absence_deduction_amount > 0 THEN absence_deduction_amount
        WHEN monthly_base_amount > 0 THEN monthly_base_amount
        ELSE 15800
    END,
    payable_day_cap = 20;
