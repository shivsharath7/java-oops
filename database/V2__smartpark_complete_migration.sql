-- =============================================================================
-- Migration: V2__smartpark_complete_migration.sql
-- Description: Non-destructive schema migration for SmartPark.
-- Preserves existing data in admins, parking_records, parking_slots, and vehicles,
-- while adding required columns, enums, and creating missing feature tables.
-- =============================================================================

USE smartpark_db;

-- -----------------------------------------------------------------------------
-- 1. Upgrade admins table
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS upgrade_admins_table;
DELIMITER $$
CREATE PROCEDURE upgrade_admins_table()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = DATABASE() AND table_name = 'admins' AND column_name = 'password_hash'
    ) THEN
        ALTER TABLE admins ADD COLUMN password_hash VARCHAR(128) NULL AFTER username;
        -- Populate password_hash from existing password column if available
        IF EXISTS (
            SELECT 1 FROM information_schema.columns 
            WHERE table_schema = DATABASE() AND table_name = 'admins' AND column_name = 'password'
        ) THEN
            UPDATE admins SET password_hash = password WHERE password_hash IS NULL;
        END IF;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = DATABASE() AND table_name = 'admins' AND column_name = 'role'
    ) THEN
        ALTER TABLE admins ADD COLUMN role VARCHAR(30) DEFAULT 'ADMINISTRATOR' AFTER full_name;
    END IF;
END$$
DELIMITER ;
CALL upgrade_admins_table();
DROP PROCEDURE IF EXISTS upgrade_admins_table;

-- Ensure default operator account exists alongside existing admin
INSERT IGNORE INTO admins (username, password_hash, full_name, role) 
VALUES ('operator', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', 'Shift Operator 1', 'OPERATOR');

-- -----------------------------------------------------------------------------
-- 2. Upgrade parking_slots table
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS upgrade_parking_slots_table;
DELIMITER $$
CREATE PROCEDURE upgrade_parking_slots_table()
BEGIN
    -- Update ENUMs and slot_number length
    ALTER TABLE parking_slots MODIFY COLUMN slot_number VARCHAR(20) NOT NULL;
    ALTER TABLE parking_slots MODIFY COLUMN status 
        ENUM('AVAILABLE', 'OCCUPIED', 'RESERVED', 'EV_CHARGING', 'MAINTENANCE') NOT NULL DEFAULT 'AVAILABLE';
    ALTER TABLE parking_slots MODIFY COLUMN vehicle_type 
        ENUM('CAR', 'BIKE', 'EV_CAR', 'ACCESSIBLE_CAR') NOT NULL DEFAULT 'CAR';

    -- Add missing columns
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'parking_slots' AND column_name = 'is_near_exit') THEN
        ALTER TABLE parking_slots ADD COLUMN is_near_exit BOOLEAN DEFAULT FALSE AFTER is_near_entrance;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'parking_slots' AND column_name = 'is_accessible') THEN
        ALTER TABLE parking_slots ADD COLUMN is_accessible BOOLEAN DEFAULT FALSE AFTER has_charging_station;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'parking_slots' AND column_name = 'distance_to_entrance') THEN
        ALTER TABLE parking_slots ADD COLUMN distance_to_entrance INT DEFAULT 20 AFTER is_accessible;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'parking_slots' AND column_name = 'distance_to_exit') THEN
        ALTER TABLE parking_slots ADD COLUMN distance_to_exit INT DEFAULT 25 AFTER distance_to_entrance;
    END IF;
END$$
DELIMITER ;
CALL upgrade_parking_slots_table();
DROP PROCEDURE IF EXISTS upgrade_parking_slots_table;

-- Update attributes for existing slots
UPDATE parking_slots SET is_accessible = TRUE, distance_to_entrance = 8 WHERE slot_number IN ('A01', 'A02', 'G-A01', 'G-A02');
UPDATE parking_slots SET is_near_entrance = TRUE, distance_to_entrance = 12 WHERE slot_number IN ('A01', 'A02', 'A03', 'A04', 'G-A01', 'G-A02', 'G-A03', 'G-A04');
UPDATE parking_slots SET is_near_exit = TRUE, distance_to_exit = 10 WHERE zone IN ('B', 'GF-B') AND slot_number LIKE '%01';

-- -----------------------------------------------------------------------------
-- 3. Upgrade vehicles table
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS upgrade_vehicles_table;
DELIMITER $$
CREATE PROCEDURE upgrade_vehicles_table()
BEGIN
    ALTER TABLE vehicles MODIFY COLUMN vehicle_type 
        ENUM('CAR', 'BIKE', 'EV_CAR', 'ACCESSIBLE_CAR') NOT NULL;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'vehicles' AND column_name = 'accessibility_needed') THEN
        ALTER TABLE vehicles ADD COLUMN accessibility_needed BOOLEAN DEFAULT FALSE AFTER contact_number;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'vehicles' AND column_name = 'requires_charging') THEN
        ALTER TABLE vehicles ADD COLUMN requires_charging BOOLEAN DEFAULT FALSE AFTER accessibility_needed;
    END IF;
END$$
DELIMITER ;
CALL upgrade_vehicles_table();
DROP PROCEDURE IF EXISTS upgrade_vehicles_table;

UPDATE vehicles SET requires_charging = TRUE WHERE vehicle_type = 'EV_CAR';

-- -----------------------------------------------------------------------------
-- 4. Upgrade parking_records table
-- -----------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS upgrade_parking_records_table;
DELIMITER $$
CREATE PROCEDURE upgrade_parking_records_table()
BEGIN
    ALTER TABLE parking_records MODIFY COLUMN payment_status 
        ENUM('PENDING', 'PAID', 'WAIVED', 'DISPUTED') DEFAULT 'PENDING';
    ALTER TABLE parking_records MODIFY COLUMN status 
        ENUM('ACTIVE', 'COMPLETED', 'DISPUTED') DEFAULT 'ACTIVE';

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'parking_records' AND column_name = 'payment_method') THEN
        ALTER TABLE parking_records ADD COLUMN payment_method VARCHAR(30) DEFAULT 'NONE' AFTER payment_status;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'parking_records' AND column_name = 'entry_operator') THEN
        ALTER TABLE parking_records ADD COLUMN entry_operator VARCHAR(50) DEFAULT 'System' AFTER status;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'parking_records' AND column_name = 'exit_operator') THEN
        ALTER TABLE parking_records ADD COLUMN exit_operator VARCHAR(50) NULL AFTER entry_operator;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'parking_records' AND column_name = 'notes') THEN
        ALTER TABLE parking_records ADD COLUMN notes TEXT NULL AFTER exit_operator;
    END IF;
END$$
DELIMITER ;
CALL upgrade_parking_records_table();
DROP PROCEDURE IF EXISTS upgrade_parking_records_table;

-- -----------------------------------------------------------------------------
-- 5. Create Parking Floors Table
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS parking_floors (
    id INT AUTO_INCREMENT PRIMARY KEY,
    floor_name VARCHAR(50) NOT NULL UNIQUE,
    floor_level INT NOT NULL,
    total_slots INT DEFAULT 0
);

INSERT IGNORE INTO parking_floors (floor_name, floor_level, total_slots) VALUES
('Basement 1', -1, 16),
('Ground Floor', 0, 24),
('First Floor', 1, 18);

-- -----------------------------------------------------------------------------
-- 6. Create Parking Zones Table
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS parking_zones (
    id INT AUTO_INCREMENT PRIMARY KEY,
    floor_name VARCHAR(50) NOT NULL,
    zone_name VARCHAR(10) NOT NULL,
    congestion_threshold INT DEFAULT 75,
    UNIQUE KEY uk_floor_zone (floor_name, zone_name)
);

INSERT IGNORE INTO parking_zones (floor_name, zone_name, congestion_threshold) VALUES
('Basement 1', 'B1-A', 75),
('Basement 1', 'B1-B', 75),
('Ground Floor', 'A', 75),
('Ground Floor', 'B', 75),
('Ground Floor', 'GF-A', 75),
('Ground Floor', 'GF-B', 75),
('First Floor', 'C', 70),
('First Floor', 'D', 70),
('First Floor', 'FF-A', 70),
('First Floor', 'FF-B', 70);

-- -----------------------------------------------------------------------------
-- 7. Create Parking Tariffs Table
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS parking_tariffs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    vehicle_type ENUM('CAR', 'BIKE', 'EV_CAR', 'ACCESSIBLE_CAR') NOT NULL UNIQUE,
    first_hour_rate DECIMAL(8, 2) NOT NULL,
    additional_hour_rate DECIMAL(8, 2) NOT NULL,
    daily_maximum DECIMAL(8, 2) NOT NULL,
    ev_surcharge_per_hr DECIMAL(8, 2) DEFAULT 0.00,
    grace_period_mins INT DEFAULT 15,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

INSERT IGNORE INTO parking_tariffs (vehicle_type, first_hour_rate, additional_hour_rate, daily_maximum, ev_surcharge_per_hr, grace_period_mins) VALUES
('CAR', 30.00, 20.00, 250.00, 0.00, 15),
('BIKE', 10.00, 10.00, 80.00, 0.00, 15),
('EV_CAR', 40.00, 25.00, 300.00, 15.00, 15),
('ACCESSIBLE_CAR', 20.00, 15.00, 150.00, 0.00, 20);

-- -----------------------------------------------------------------------------
-- 8. Create System Settings Table
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS system_settings (
    id INT AUTO_INCREMENT PRIMARY KEY,
    setting_key VARCHAR(50) NOT NULL UNIQUE,
    setting_value VARCHAR(255) NOT NULL,
    description VARCHAR(255)
);

INSERT IGNORE INTO system_settings (setting_key, setting_value, description) VALUES
('active_strategy', 'CONGESTION_AWARE', 'Active slot allocation strategy: STANDARD or CONGESTION_AWARE'),
('low_congestion_threshold', '40', 'Threshold percentage below which zone is LOW congestion'),
('high_congestion_threshold', '75', 'Threshold percentage above which zone is HIGH congestion'),
('reservation_hold_minutes', '15', 'Default temporary reservation hold duration in minutes'),
('peak_hour_start_am', '08:30', 'Morning peak traffic start time'),
('peak_hour_end_am', '10:30', 'Morning peak traffic end time'),
('peak_hour_start_pm', '17:00', 'Evening peak traffic start time'),
('peak_hour_end_pm', '20:00', 'Evening peak traffic end time'),
('max_allowed_stay_hours', '24', 'Threshold hours to flag unusually long parking session');

-- -----------------------------------------------------------------------------
-- 9. Create Reservations Table
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS reservations (
    id INT AUTO_INCREMENT PRIMARY KEY,
    reservation_code VARCHAR(30) NOT NULL UNIQUE,
    vehicle_number VARCHAR(20) NOT NULL,
    vehicle_type ENUM('CAR', 'BIKE', 'EV_CAR', 'ACCESSIBLE_CAR') NOT NULL,
    slot_id INT NOT NULL,
    customer_name VARCHAR(100),
    contact_number VARCHAR(20),
    reserved_at DATETIME NOT NULL,
    expires_at DATETIME NOT NULL,
    status ENUM('PENDING', 'CLAIMED', 'EXPIRED', 'CANCELLED') DEFAULT 'PENDING',
    FOREIGN KEY (slot_id) REFERENCES parking_slots(id) ON DELETE CASCADE,
    INDEX idx_res_expires (expires_at, status)
);

-- -----------------------------------------------------------------------------
-- 10. Create EV Chargers Table
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ev_chargers (
    id INT AUTO_INCREMENT PRIMARY KEY,
    charger_code VARCHAR(30) NOT NULL UNIQUE,
    slot_id INT NOT NULL,
    charger_type VARCHAR(50) DEFAULT 'Fast DC (50kW)',
    power_kw DECIMAL(5, 2) DEFAULT 50.00,
    status ENUM('AVAILABLE', 'IN_USE', 'MAINTENANCE', 'OFFLINE') DEFAULT 'AVAILABLE',
    FOREIGN KEY (slot_id) REFERENCES parking_slots(id) ON DELETE CASCADE
);

-- Populate ev_chargers from slots that have has_charging_station = 1
INSERT IGNORE INTO ev_chargers (charger_code, slot_id, charger_type, power_kw, status)
SELECT 
    CONCAT('EV-CHG-', ps.slot_number),
    ps.id,
    'Fast DC (50kW)',
    50.00,
    CASE WHEN ps.status = 'OCCUPIED' THEN 'IN_USE' ELSE 'AVAILABLE' END
FROM parking_slots ps
WHERE ps.has_charging_station = 1;

-- -----------------------------------------------------------------------------
-- 11. Create Charging Sessions Table
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS charging_sessions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    charger_id INT NOT NULL,
    vehicle_number VARCHAR(20) NOT NULL,
    slot_id INT NOT NULL,
    start_time DATETIME NOT NULL,
    estimated_end_time DATETIME NOT NULL,
    actual_end_time DATETIME NULL,
    kwh_consumed DECIMAL(8, 2) DEFAULT 0.00,
    status ENUM('IN_PROGRESS', 'COMPLETED', 'OVERDUE', 'CANCELLED') DEFAULT 'IN_PROGRESS',
    charging_fee DECIMAL(10, 2) DEFAULT 0.00,
    FOREIGN KEY (charger_id) REFERENCES ev_chargers(id) ON DELETE CASCADE,
    FOREIGN KEY (slot_id) REFERENCES parking_slots(id) ON DELETE CASCADE
);

-- -----------------------------------------------------------------------------
-- 12. Create Anomalies Table
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS anomalies (
    id INT AUTO_INCREMENT PRIMARY KEY,
    anomaly_type VARCHAR(60) NOT NULL,
    severity ENUM('INFO', 'WARNING', 'CRITICAL') NOT NULL DEFAULT 'WARNING',
    vehicle_number VARCHAR(20) NULL,
    slot_id INT NULL,
    description TEXT NOT NULL,
    suggested_resolution TEXT NOT NULL,
    status ENUM('OPEN', 'ACKNOWLEDGED', 'RESOLVED', 'IGNORED') DEFAULT 'OPEN',
    detected_at DATETIME NOT NULL,
    resolved_at DATETIME NULL,
    resolved_by VARCHAR(50) NULL,
    resolution_notes TEXT NULL,
    FOREIGN KEY (slot_id) REFERENCES parking_slots(id) ON DELETE SET NULL
);

-- -----------------------------------------------------------------------------
-- 13. Create Audit Logs Table
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS audit_logs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    timestamp DATETIME NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    target_ref VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    actor VARCHAR(50) DEFAULT 'System',
    INDEX idx_audit_time (timestamp)
);

-- -----------------------------------------------------------------------------
-- 14. Create Parking Disputes Table
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS parking_disputes (
    id INT AUTO_INCREMENT PRIMARY KEY,
    record_id INT NULL,
    ticket_id VARCHAR(40) NOT NULL,
    vehicle_number VARCHAR(20) NOT NULL,
    category VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,
    disputed_fee DECIMAL(10, 2) DEFAULT 0.00,
    status ENUM('OPEN', 'UNDER_INVESTIGATION', 'RESOLVED', 'REJECTED') DEFAULT 'OPEN',
    created_at DATETIME NOT NULL,
    resolved_at DATETIME NULL,
    resolution_notes TEXT NULL,
    resolved_by VARCHAR(50) NULL,
    FOREIGN KEY (record_id) REFERENCES parking_records(id) ON DELETE SET NULL
);

-- -----------------------------------------------------------------------------
-- 15. Create Alternative Facilities Table
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS alternative_facilities (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    address VARCHAR(200) NOT NULL,
    total_capacity INT NOT NULL,
    available_estimate INT NOT NULL,
    vehicle_types VARCHAR(100) DEFAULT 'CAR, BIKE',
    distance_km DECIMAL(4, 2) NOT NULL,
    contact_info VARCHAR(100)
);

INSERT IGNORE INTO alternative_facilities (name, address, total_capacity, available_estimate, vehicle_types, distance_km, contact_info) VALUES
('City Central Metro Parking', 'Opposite Metro Station Gate 2, Ring Road', 250, 85, 'CAR, BIKE', 0.6, '+91-80-23456789'),
('Grand Galleria Multi-Level Parking', 'Block C, Commercial Hub, 4th Cross', 400, 140, 'CAR, BIKE, EV_CAR', 1.2, '+91-80-87654321'),
('Civic Center Underground Parking', 'Near Town Hall & Central Park', 180, 22, 'CAR, ACCESSIBLE_CAR', 1.8, '+91-80-55443322');

-- -----------------------------------------------------------------------------
-- 16. Create Route Nodes Table
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS route_nodes (
    id INT AUTO_INCREMENT PRIMARY KEY,
    node_id VARCHAR(30) NOT NULL UNIQUE,
    floor VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    node_type ENUM('SLOT', 'AISLE', 'RAMP', 'ELEVATOR', 'EXIT', 'ENTRANCE') NOT NULL,
    x_coord INT DEFAULT 0,
    y_coord INT DEFAULT 0
);

INSERT IGNORE INTO route_nodes (node_id, floor, name, node_type, x_coord, y_coord) VALUES
('N_ENTRANCE', 'Ground Floor', 'Main Entrance Gate A', 'ENTRANCE', 50, 100),
('N_AISLE_GA', 'Ground Floor', 'GF Zone A Central Aisle', 'AISLE', 150, 100),
('N_SLOT_A01', 'Ground Floor', 'Slot A01 (Accessible)', 'SLOT', 150, 50),
('N_SLOT_A02', 'Ground Floor', 'Slot A02 (Accessible)', 'SLOT', 150, 60),
('N_SLOT_A03', 'Ground Floor', 'Slot A03 (EV Bay)', 'SLOT', 150, 70),
('N_SLOT_A04', 'Ground Floor', 'Slot A04 (EV Bay)', 'SLOT', 150, 80),
('N_SLOT_A05', 'Ground Floor', 'Slot A05 (Car)', 'SLOT', 150, 130),
('N_SLOT_GA01', 'Ground Floor', 'Slot G-A01 (Accessible)', 'SLOT', 150, 50),
('N_SLOT_GA03', 'Ground Floor', 'Slot G-A03 (EV Bay)', 'SLOT', 150, 70),
('N_SLOT_GA05', 'Ground Floor', 'Slot G-A05 (Car)', 'SLOT', 150, 130),
('N_RAMP_B1', 'Ground Floor', 'Basement 1 Access Ramp', 'RAMP', 250, 50),
('N_RAMP_F1', 'Ground Floor', 'First Floor Access Ramp', 'RAMP', 250, 150),
('N_AISLE_GB', 'Ground Floor', 'GF Zone B Central Aisle', 'AISLE', 350, 100),
('N_SLOT_B01', 'Ground Floor', 'Slot B01 (Car)', 'SLOT', 350, 50),
('N_SLOT_B05', 'Ground Floor', 'Slot B05 (EV Bay)', 'SLOT', 350, 150),
('N_SLOT_GB01', 'Ground Floor', 'Slot G-B01 (Car)', 'SLOT', 350, 50),
('N_SLOT_GB05', 'Ground Floor', 'Slot G-B05 (EV Bay)', 'SLOT', 350, 150),
('N_EXIT_NORTH', 'Ground Floor', 'North Exit Gate (Express)', 'EXIT', 450, 50),
('N_EXIT_SOUTH', 'Ground Floor', 'South Exit Gate (Ring Road)', 'EXIT', 450, 150);

-- -----------------------------------------------------------------------------
-- 17. Create Route Edges Table
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS route_edges (
    id INT AUTO_INCREMENT PRIMARY KEY,
    from_node VARCHAR(30) NOT NULL,
    to_node VARCHAR(30) NOT NULL,
    distance_weight INT NOT NULL,
    direction_info VARCHAR(100) DEFAULT 'Proceed straight',
    FOREIGN KEY (from_node) REFERENCES route_nodes(node_id) ON DELETE CASCADE,
    FOREIGN KEY (to_node) REFERENCES route_nodes(node_id) ON DELETE CASCADE
);

INSERT IGNORE INTO route_edges (from_node, to_node, distance_weight, direction_info) VALUES
('N_ENTRANCE', 'N_AISLE_GA', 15, 'Enter through Gate A, follow Zone A signs'),
('N_AISLE_GA', 'N_SLOT_A01', 5, 'Turn left into Bay A01'),
('N_AISLE_GA', 'N_SLOT_A02', 6, 'Turn left into Bay A02'),
('N_AISLE_GA', 'N_SLOT_A03', 8, 'Turn left into EV Bay A03'),
('N_AISLE_GA', 'N_SLOT_A04', 9, 'Turn left into EV Bay A04'),
('N_AISLE_GA', 'N_SLOT_A05', 6, 'Turn right into Bay A05'),
('N_AISLE_GA', 'N_SLOT_GA01', 5, 'Turn left into Bay G-A01'),
('N_AISLE_GA', 'N_SLOT_GA03', 8, 'Turn left into EV Bay G-A03'),
('N_AISLE_GA', 'N_SLOT_GA05', 6, 'Turn right into Bay G-A05'),
('N_AISLE_GA', 'N_RAMP_B1', 25, 'Proceed towards Down Ramp to Basement'),
('N_AISLE_GA', 'N_RAMP_F1', 25, 'Proceed towards Up Ramp to First Floor'),
('N_AISLE_GA', 'N_AISLE_GB', 30, 'Continue straight through central corridor to Zone B'),
('N_AISLE_GB', 'N_SLOT_B01', 8, 'Turn left into Bay B01'),
('N_AISLE_GB', 'N_SLOT_B05', 10, 'Turn right into EV Bay B05'),
('N_AISLE_GB', 'N_SLOT_GB01', 8, 'Turn left into Bay G-B01'),
('N_AISLE_GB', 'N_SLOT_GB05', 10, 'Turn right into EV Bay G-B05'),
('N_AISLE_GB', 'N_EXIT_NORTH', 20, 'Take north corridor to North Exit Gate'),
('N_AISLE_GB', 'N_EXIT_SOUTH', 22, 'Take south corridor to South Exit Gate'),
-- Reverse connections for exit navigation from slots
('N_SLOT_A01', 'N_AISLE_GA', 5, 'Back out of bay into Zone A aisle'),
('N_SLOT_A02', 'N_AISLE_GA', 6, 'Back out of bay into Zone A aisle'),
('N_SLOT_A03', 'N_AISLE_GA', 8, 'Back out of EV bay into Zone A aisle'),
('N_SLOT_A04', 'N_AISLE_GA', 9, 'Back out of EV bay into Zone A aisle'),
('N_SLOT_A05', 'N_AISLE_GA', 6, 'Back out of bay into Zone A aisle'),
('N_SLOT_GA01', 'N_AISLE_GA', 5, 'Back out of bay into Zone A aisle'),
('N_SLOT_GA03', 'N_AISLE_GA', 8, 'Back out of EV bay into Zone A aisle'),
('N_SLOT_GA05', 'N_AISLE_GA', 6, 'Back out of bay into Zone A aisle'),
('N_SLOT_B01', 'N_AISLE_GB', 8, 'Back out of bay into Zone B aisle'),
('N_SLOT_B05', 'N_AISLE_GB', 10, 'Back out of EV bay into Zone B aisle'),
('N_SLOT_GB01', 'N_AISLE_GB', 8, 'Back out of bay into Zone B aisle'),
('N_SLOT_GB05', 'N_AISLE_GB', 10, 'Back out of EV bay into Zone B aisle'),
('N_AISLE_GA', 'N_EXIT_NORTH', 45, 'Follow express overhead signage to North Exit Gate'),
('N_AISLE_GA', 'N_EXIT_SOUTH', 48, 'Follow ring road signage to South Exit Gate');

-- -----------------------------------------------------------------------------
-- 18. Create Sensor Readings Table
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sensor_readings (
    id INT AUTO_INCREMENT PRIMARY KEY,
    sensor_id VARCHAR(30) NOT NULL,
    slot_number VARCHAR(15) NOT NULL,
    distance_cm DECIMAL(6, 2) NOT NULL,
    is_occupied BOOLEAN NOT NULL,
    reading_timestamp DATETIME NOT NULL,
    status VARCHAR(30) DEFAULT 'VALID'
);
