ALTER TABLE repair_requests ADD COLUMN IF NOT EXISTS device_brand VARCHAR(100);
ALTER TABLE repair_requests ADD COLUMN IF NOT EXISTS device_model VARCHAR(100);
ALTER TABLE repair_requests ADD COLUMN IF NOT EXISTS serial_number VARCHAR(100);
