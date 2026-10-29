-- V19: Add payment_method to payments and bills tables for Cash vs VietQR settlement
ALTER TABLE payments ADD COLUMN IF NOT EXISTS payment_method VARCHAR(30) DEFAULT 'VIETQR';
ALTER TABLE bills ADD COLUMN IF NOT EXISTS payment_method VARCHAR(30) DEFAULT 'VIETQR';
