-- Cleanup legacy allowance columns after V3 backfilled the new training profile model.
ALTER TABLE member_training_profile
    DROP COLUMN monthly_base_amount,
    DROP COLUMN absence_deduction_amount;
