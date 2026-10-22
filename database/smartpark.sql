-- =============================================================================
-- Database: smartpark_db
-- System: SmartPark — Intelligent Parking and Congestion Management System
-- Compatible with MySQL 8.0+
-- =============================================================================

CREATE DATABASE IF NOT EXISTS smartpark_db;
USE smartpark_db;

-- Drop dependent tables in reverse order
DROP TABLE IF EXISTS sensor_readings;
DROP TABLE IF EXISTS route_edges;
DROP TABLE IF EXISTS route_nodes;
DROP TABLE IF EXISTS system_settings;
DROP TABLE IF EXISTS alternative_facilities;
DROP TABLE IF EXISTS parking_disputes;
DROP TABLE IF EXISTS audit_logs;
DROP TABLE IF EXISTS anomalies;
DROP TABLE IF EXISTS charging_sessions;
DROP TABLE IF EXISTS ev_chargers;
DROP TABLE IF EXISTS reservations;
DROP TABLE IF EXISTS parking_tariffs;
DROP TABLE IF EXISTS parking_records;
DROP TABLE IF EXISTS parking_slots;
DROP TABLE IF EXISTS vehicles;
DROP TABLE IF EXISTS parking_zones;
DROP TABLE IF EXISTS parking_floors;
DROP TABLE IF EXISTS admins;

-- 1. Admins Table
CREATE TABLE admins (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(128) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role VARCHAR(30) DEFAULT 'ADMINISTRATOR',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Parking Floors Table
CREATE TABLE parking_floors (
    id INT AUTO_INCREMENT PRIMARY KEY,
    floor_name VARCHAR(50) NOT NULL UNIQUE,
    floor_level INT NOT NULL,
    total_slots INT DEFAULT 0
);

-- 3. Parking Zones Table
CREATE TABLE parking_zones (
    id INT AUTO_INCREMENT PRIMARY KEY,
    floor_name VARCHAR(50) NOT NULL,
    zone_name VARCHAR(10) NOT NULL,
    congestion_threshold INT DEFAULT 75,
    UNIQUE KEY uk_floor_zone (floor_name, zone_name)
);

-- 4. Parking Slots Table
CREATE TABLE parking_slots (
    id INT AUTO_INCREMENT PRIMARY KEY,
    slot_number VARCHAR(15) NOT NULL UNIQUE,
    floor VARCHAR(50) NOT NULL,
    zone VARCHAR(10) NOT NULL,
    vehicle_type ENUM('CAR', 'BIKE', 'EV_CAR', 'ACCESSIBLE_CAR') NOT NULL DEFAULT 'CAR',
    status ENUM('AVAILABLE', 'OCCUPIED', 'RESERVED', 'EV_CHARGING', 'MAINTENANCE') NOT NULL DEFAULT 'AVAILABLE',
    is_near_entrance BOOLEAN DEFAULT FALSE,
    is_near_exit BOOLEAN DEFAULT FALSE,
    has_charging_station BOOLEAN DEFAULT FALSE,
    is_accessible BOOLEAN DEFAULT FALSE,
    distance_to_entrance INT DEFAULT 20,
    distance_to_exit INT DEFAULT 25,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_floor_zone (floor, zone),
    INDEX idx_status (status),
    INDEX idx_type (vehicle_type)
);

-- 5. Vehicles Table
CREATE TABLE vehicles (
    id INT AUTO_INCREMENT PRIMARY KEY,
    vehicle_number VARCHAR(20) NOT NULL UNIQUE,
    vehicle_type ENUM('CAR', 'BIKE', 'EV_CAR', 'ACCESSIBLE_CAR') NOT NULL,
    customer_name VARCHAR(100),
    contact_number VARCHAR(20),
    accessibility_needed BOOLEAN DEFAULT FALSE,
    requires_charging BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_vehicle_num (vehicle_number)
);

-- 6. Parking Records Table
CREATE TABLE parking_records (
    id INT AUTO_INCREMENT PRIMARY KEY,
    ticket_id VARCHAR(40) NOT NULL UNIQUE,
    vehicle_id INT NOT NULL,
    slot_id INT NOT NULL,
    entry_time DATETIME NOT NULL,
    exit_time DATETIME NULL,
    duration_minutes INT DEFAULT 0,
    parking_fee DECIMAL(10, 2) DEFAULT 0.00,
    payment_status ENUM('PENDING', 'PAID', 'WAIVED', 'DISPUTED') DEFAULT 'PENDING',
    payment_method VARCHAR(30) DEFAULT 'NONE',
    status ENUM('ACTIVE', 'COMPLETED', 'DISPUTED') DEFAULT 'ACTIVE',
    entry_operator VARCHAR(50) DEFAULT 'System',
    exit_operator VARCHAR(50) NULL,
    notes TEXT NULL,
    FOREIGN KEY (vehicle_id) REFERENCES vehicles(id) ON DELETE CASCADE,
    FOREIGN KEY (slot_id) REFERENCES parking_slots(id) ON DELETE RESTRICT,
    INDEX idx_ticket (ticket_id),
    INDEX idx_entry_time (entry_time),
    INDEX idx_status (status)
);

-- 7. Parking Tariffs Table
CREATE TABLE parking_tariffs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    vehicle_type ENUM('CAR', 'BIKE', 'EV_CAR', 'ACCESSIBLE_CAR') NOT NULL UNIQUE,
    first_hour_rate DECIMAL(8, 2) NOT NULL,
    additional_hour_rate DECIMAL(8, 2) NOT NULL,
    daily_maximum DECIMAL(8, 2) NOT NULL,
    ev_surcharge_per_hr DECIMAL(8, 2) DEFAULT 0.00,
    grace_period_mins INT DEFAULT 15,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 8. Temporary Reservations Table
CREATE TABLE reservations (
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

-- 9. EV Chargers Table
CREATE TABLE ev_chargers (
    id INT AUTO_INCREMENT PRIMARY KEY,
    charger_code VARCHAR(30) NOT NULL UNIQUE,
    slot_id INT NOT NULL,
    charger_type VARCHAR(50) DEFAULT 'Fast DC (50kW)',
    power_kw DECIMAL(5, 2) DEFAULT 50.00,
    status ENUM('AVAILABLE', 'IN_USE', 'MAINTENANCE', 'OFFLINE') DEFAULT 'AVAILABLE',
    FOREIGN KEY (slot_id) REFERENCES parking_slots(id) ON DELETE CASCADE
);

-- 10. Charging Sessions Table
CREATE TABLE charging_sessions (
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

-- 11. Parking Anomalies Table
CREATE TABLE anomalies (
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

-- 12. Audit Logs Table (Immutable audit trail)
CREATE TABLE audit_logs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    timestamp DATETIME NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    target_ref VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    actor VARCHAR(50) DEFAULT 'System',
    INDEX idx_audit_time (timestamp)
);

-- 13. Parking Disputes Table
CREATE TABLE parking_disputes (
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

-- 14. Alternative Facilities Table
CREATE TABLE alternative_facilities (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    address VARCHAR(200) NOT NULL,
    total_capacity INT NOT NULL,
    available_estimate INT NOT NULL,
    vehicle_types VARCHAR(100) DEFAULT 'CAR, BIKE, EV_CAR',
    distance_km DECIMAL(4, 2) NOT NULL,
    contact_info VARCHAR(100)
);

-- 15. System Settings Table
CREATE TABLE system_settings (
    id INT AUTO_INCREMENT PRIMARY KEY,
    setting_key VARCHAR(50) NOT NULL UNIQUE,
    setting_value VARCHAR(255) NOT NULL,
    description VARCHAR(255)
);

-- 16. Route Nodes Table (for Graph Pathfinding)
CREATE TABLE route_nodes (
    id INT AUTO_INCREMENT PRIMARY KEY,
    node_id VARCHAR(30) NOT NULL UNIQUE,
    floor VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    node_type ENUM('SLOT', 'AISLE', 'RAMP', 'ELEVATOR', 'EXIT', 'ENTRANCE') NOT NULL,
    x_coord INT DEFAULT 0,
    y_coord INT DEFAULT 0
);

-- 17. Route Edges Table
CREATE TABLE route_edges (
    id INT AUTO_INCREMENT PRIMARY KEY,
    from_node VARCHAR(30) NOT NULL,
    to_node VARCHAR(30) NOT NULL,
    distance_weight INT NOT NULL,
    direction_info VARCHAR(100) DEFAULT 'Proceed straight',
    FOREIGN KEY (from_node) REFERENCES route_nodes(node_id) ON DELETE CASCADE,
    FOREIGN KEY (to_node) REFERENCES route_nodes(node_id) ON DELETE CASCADE
);

-- 18. Sensor Readings Table (for IoT / Hardware Integration)
CREATE TABLE sensor_readings (
    id INT AUTO_INCREMENT PRIMARY KEY,
    sensor_id VARCHAR(30) NOT NULL,
    slot_number VARCHAR(15) NOT NULL,
    distance_cm DECIMAL(6, 2) NOT NULL,
    is_occupied BOOLEAN NOT NULL,
    reading_timestamp DATETIME NOT NULL,
    status VARCHAR(30) DEFAULT 'VALID'
);

-- =============================================================================
-- SEED DATA
-- =============================================================================

-- Default Administrators (SHA-256 for 'admin123' is 240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9)
INSERT INTO admins (username, password_hash, full_name, role) VALUES 
('admin', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', 'Master Administrator', 'ADMINISTRATOR'),
('operator', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', 'Shift Operator 1', 'OPERATOR');

-- Parking Floors
INSERT INTO parking_floors (floor_name, floor_level, total_slots) VALUES
('Basement 1', -1, 16),
('Ground Floor', 0, 24),
('First Floor', 1, 18);

-- Parking Zones
INSERT INTO parking_zones (floor_name, zone_name, congestion_threshold) VALUES
('Basement 1', 'B1-A', 75),
('Basement 1', 'B1-B', 75),
('Ground Floor', 'GF-A', 75),
('Ground Floor', 'GF-B', 75),
('First Floor', 'FF-A', 70),
('First Floor', 'FF-B', 70);

-- Tariffs
INSERT INTO parking_tariffs (vehicle_type, first_hour_rate, additional_hour_rate, daily_maximum, ev_surcharge_per_hr, grace_period_mins) VALUES
('CAR', 30.00, 20.00, 250.00, 0.00, 15),
('BIKE', 10.00, 10.00, 80.00, 0.00, 15),
('EV_CAR', 40.00, 25.00, 300.00, 15.00, 15),
('ACCESSIBLE_CAR', 20.00, 15.00, 150.00, 0.00, 20);

-- System Settings
INSERT INTO system_settings (setting_key, setting_value, description) VALUES
('active_strategy', 'CONGESTION_AWARE', 'Active slot allocation strategy: STANDARD or CONGESTION_AWARE'),
('low_congestion_threshold', '40', 'Threshold percentage below which zone is LOW congestion'),
('high_congestion_threshold', '75', 'Threshold percentage above which zone is HIGH congestion'),
('reservation_hold_minutes', '15', 'Default temporary reservation hold duration in minutes'),
('peak_hour_start_am', '08:30', 'Morning peak traffic start time'),
('peak_hour_end_am', '10:30', 'Morning peak traffic end time'),
('peak_hour_start_pm', '17:00', 'Evening peak traffic start time'),
('peak_hour_end_pm', '20:00', 'Evening peak traffic end time'),
('max_allowed_stay_hours', '24', 'Threshold hours to flag unusually long parking session');

-- Parking Slots
-- Ground Floor - Zone GF-A (near main entrance & accessible & EV)
INSERT INTO parking_slots (slot_number, floor, zone, vehicle_type, status, is_near_entrance, is_near_exit, has_charging_station, is_accessible, distance_to_entrance, distance_to_exit) VALUES
('G-A01', 'Ground Floor', 'GF-A', 'ACCESSIBLE_CAR', 'AVAILABLE', TRUE, FALSE, FALSE, TRUE, 5, 30),
('G-A02', 'Ground Floor', 'GF-A', 'ACCESSIBLE_CAR', 'OCCUPIED', TRUE, FALSE, FALSE, TRUE, 8, 32),
('G-A03', 'Ground Floor', 'GF-A', 'EV_CAR', 'EV_CHARGING', TRUE, FALSE, TRUE, FALSE, 12, 35),
('G-A04', 'Ground Floor', 'GF-A', 'EV_CAR', 'AVAILABLE', TRUE, FALSE, TRUE, FALSE, 15, 36),
('G-A05', 'Ground Floor', 'GF-A', 'CAR', 'OCCUPIED', TRUE, FALSE, FALSE, FALSE, 18, 38),
('G-A06', 'Ground Floor', 'GF-A', 'CAR', 'OCCUPIED', TRUE, FALSE, FALSE, FALSE, 20, 40),
('G-A07', 'Ground Floor', 'GF-A', 'CAR', 'AVAILABLE', FALSE, FALSE, FALSE, FALSE, 25, 42),
('G-A08', 'Ground Floor', 'GF-A', 'CAR', 'RESERVED', FALSE, FALSE, FALSE, FALSE, 28, 44),
('G-A09', 'Ground Floor', 'GF-A', 'BIKE', 'OCCUPIED', TRUE, FALSE, FALSE, FALSE, 10, 25),
('G-A10', 'Ground Floor', 'GF-A', 'BIKE', 'AVAILABLE', TRUE, FALSE, FALSE, FALSE, 12, 26),
('G-A11', 'Ground Floor', 'GF-A', 'BIKE', 'AVAILABLE', FALSE, FALSE, FALSE, FALSE, 16, 28),
('G-A12', 'Ground Floor', 'GF-A', 'CAR', 'AVAILABLE', FALSE, FALSE, FALSE, FALSE, 30, 45);

-- Ground Floor - Zone GF-B (near main exit)
INSERT INTO parking_slots (slot_number, floor, zone, vehicle_type, status, is_near_entrance, is_near_exit, has_charging_station, is_accessible, distance_to_entrance, distance_to_exit) VALUES
('G-B01', 'Ground Floor', 'GF-B', 'CAR', 'OCCUPIED', FALSE, TRUE, FALSE, FALSE, 35, 10),
('G-B02', 'Ground Floor', 'GF-B', 'CAR', 'OCCUPIED', FALSE, TRUE, FALSE, FALSE, 38, 12),
('G-B03', 'Ground Floor', 'GF-B', 'CAR', 'AVAILABLE', FALSE, TRUE, FALSE, FALSE, 40, 15),
('G-B04', 'Ground Floor', 'GF-B', 'CAR', 'OCCUPIED', FALSE, TRUE, FALSE, FALSE, 42, 18),
('G-B05', 'Ground Floor', 'GF-B', 'EV_CAR', 'OCCUPIED', FALSE, FALSE, TRUE, FALSE, 45, 20),
('G-B06', 'Ground Floor', 'GF-B', 'EV_CAR', 'AVAILABLE', FALSE, FALSE, TRUE, FALSE, 46, 22),
('G-B07', 'Ground Floor', 'GF-B', 'BIKE', 'OCCUPIED', FALSE, TRUE, FALSE, FALSE, 30, 8),
('G-B08', 'Ground Floor', 'GF-B', 'BIKE', 'AVAILABLE', FALSE, TRUE, FALSE, FALSE, 32, 10),
('G-B09', 'Ground Floor', 'GF-B', 'BIKE', 'AVAILABLE', FALSE, FALSE, FALSE, FALSE, 35, 14),
('G-B10', 'Ground Floor', 'GF-B', 'CAR', 'MAINTENANCE', FALSE, FALSE, FALSE, FALSE, 48, 25),
('G-B11', 'Ground Floor', 'GF-B', 'CAR', 'OCCUPIED', FALSE, FALSE, FALSE, FALSE, 50, 26),
('G-B12', 'Ground Floor', 'GF-B', 'CAR', 'AVAILABLE', FALSE, FALSE, FALSE, FALSE, 52, 28);

-- Basement 1 - Zone B1-A & B1-B
INSERT INTO parking_slots (slot_number, floor, zone, vehicle_type, status, is_near_entrance, is_near_exit, has_charging_station, is_accessible, distance_to_entrance, distance_to_exit) VALUES
('B-A01', 'Basement 1', 'B1-A', 'CAR', 'AVAILABLE', FALSE, FALSE, FALSE, FALSE, 25, 30),
('B-A02', 'Basement 1', 'B1-A', 'CAR', 'AVAILABLE', FALSE, FALSE, FALSE, FALSE, 28, 32),
('B-A03', 'Basement 1', 'B1-A', 'EV_CAR', 'AVAILABLE', FALSE, FALSE, TRUE, FALSE, 30, 35),
('B-A04', 'Basement 1', 'B1-A', 'EV_CAR', 'AVAILABLE', FALSE, FALSE, TRUE, FALSE, 32, 36),
('B-A05', 'Basement 1', 'B1-A', 'CAR', 'OCCUPIED', FALSE, FALSE, FALSE, FALSE, 35, 40),
('B-A06', 'Basement 1', 'B1-A', 'CAR', 'OCCUPIED', FALSE, FALSE, FALSE, FALSE, 38, 42),
('B-A07', 'Basement 1', 'B1-A', 'BIKE', 'AVAILABLE', FALSE, FALSE, FALSE, FALSE, 20, 25),
('B-A08', 'Basement 1', 'B1-A', 'BIKE', 'AVAILABLE', FALSE, FALSE, FALSE, FALSE, 22, 26),
('B-B01', 'Basement 1', 'B1-B', 'CAR', 'AVAILABLE', FALSE, TRUE, FALSE, FALSE, 40, 15),
('B-B02', 'Basement 1', 'B1-B', 'CAR', 'AVAILABLE', FALSE, TRUE, FALSE, FALSE, 42, 18),
('B-B03', 'Basement 1', 'B1-B', 'CAR', 'OCCUPIED', FALSE, TRUE, FALSE, FALSE, 45, 20),
('B-B04', 'Basement 1', 'B1-B', 'CAR', 'AVAILABLE', FALSE, FALSE, FALSE, FALSE, 48, 24),
('B-B05', 'Basement 1', 'B1-B', 'ACCESSIBLE_CAR', 'AVAILABLE', FALSE, FALSE, FALSE, TRUE, 22, 28),
('B-B06', 'Basement 1', 'B1-B', 'BIKE', 'AVAILABLE', FALSE, FALSE, FALSE, FALSE, 25, 20),
('B-B07', 'Basement 1', 'B1-B', 'BIKE', 'AVAILABLE', FALSE, FALSE, FALSE, FALSE, 26, 22),
('B-B08', 'Basement 1', 'B1-B', 'CAR', 'AVAILABLE', FALSE, FALSE, FALSE, FALSE, 50, 30);

-- First Floor - Zone FF-A & FF-B
INSERT INTO parking_slots (slot_number, floor, zone, vehicle_type, status, is_near_entrance, is_near_exit, has_charging_station, is_accessible, distance_to_entrance, distance_to_exit) VALUES
('F-A01', 'First Floor', 'FF-A', 'CAR', 'AVAILABLE', FALSE, FALSE, FALSE, FALSE, 30, 35),
('F-A02', 'First Floor', 'FF-A', 'CAR', 'AVAILABLE', FALSE, FALSE, FALSE, FALSE, 32, 37),
('F-A03', 'First Floor', 'FF-A', 'CAR', 'OCCUPIED', FALSE, FALSE, FALSE, FALSE, 35, 40),
('F-A04', 'First Floor', 'FF-A', 'CAR', 'AVAILABLE', FALSE, FALSE, FALSE, FALSE, 38, 42),
('F-A05', 'First Floor', 'FF-A', 'EV_CAR', 'AVAILABLE', FALSE, FALSE, TRUE, FALSE, 35, 40),
('F-A06', 'First Floor', 'FF-A', 'EV_CAR', 'AVAILABLE', FALSE, FALSE, TRUE, FALSE, 38, 42),
('F-A07', 'First Floor', 'FF-A', 'BIKE', 'AVAILABLE', FALSE, FALSE, FALSE, FALSE, 22, 28),
('F-A08', 'First Floor', 'FF-A', 'BIKE', 'AVAILABLE', FALSE, FALSE, FALSE, FALSE, 25, 30),
('F-A09', 'First Floor', 'FF-A', 'CAR', 'AVAILABLE', FALSE, FALSE, FALSE, FALSE, 40, 45),
('F-B01', 'First Floor', 'FF-B', 'CAR', 'AVAILABLE', FALSE, TRUE, FALSE, FALSE, 42, 20),
('F-B02', 'First Floor', 'FF-B', 'CAR', 'AVAILABLE', FALSE, TRUE, FALSE, FALSE, 45, 22),
('F-B03', 'First Floor', 'FF-B', 'CAR', 'AVAILABLE', FALSE, TRUE, FALSE, FALSE, 48, 25),
('F-B04', 'First Floor', 'FF-B', 'CAR', 'AVAILABLE', FALSE, FALSE, FALSE, FALSE, 50, 28),
('F-B05', 'First Floor', 'FF-B', 'BIKE', 'AVAILABLE', FALSE, FALSE, FALSE, FALSE, 28, 20),
('F-B06', 'First Floor', 'FF-B', 'BIKE', 'AVAILABLE', FALSE, FALSE, FALSE, FALSE, 30, 22),
('F-B07', 'First Floor', 'FF-B', 'CAR', 'AVAILABLE', FALSE, FALSE, FALSE, FALSE, 52, 30),
('F-B08', 'First Floor', 'FF-B', 'CAR', 'AVAILABLE', FALSE, FALSE, FALSE, FALSE, 55, 32),
('F-B09', 'First Floor', 'FF-B', 'ACCESSIBLE_CAR', 'AVAILABLE', FALSE, FALSE, FALSE, TRUE, 25, 28);

-- EV Chargers
INSERT INTO ev_chargers (charger_code, slot_id, charger_type, power_kw, status) VALUES
('EV-GF-01', 3, 'Fast DC (50kW)', 50.00, 'IN_USE'),
('EV-GF-02', 4, 'Fast DC (50kW)', 50.00, 'AVAILABLE'),
('EV-GF-03', 17, 'Standard AC (22kW)', 22.00, 'AVAILABLE'),
('EV-GF-04', 18, 'Standard AC (22kW)', 22.00, 'AVAILABLE'),
('EV-B1-01', 27, 'Standard AC (22kW)', 22.00, 'AVAILABLE'),
('EV-B1-02', 28, 'Standard AC (22kW)', 22.00, 'AVAILABLE'),
('EV-FF-01', 45, 'Standard AC (22kW)', 22.00, 'AVAILABLE'),
('EV-FF-02', 46, 'Standard AC (22kW)', 22.00, 'AVAILABLE');

-- Vehicles
INSERT INTO vehicles (vehicle_number, vehicle_type, customer_name, contact_number, accessibility_needed, requires_charging) VALUES
('TN01AB1234', 'CAR', 'Rahul Sharma', '9876543210', FALSE, FALSE),
('KA05EV8821', 'EV_CAR', 'Priya Sundaram', '9123456780', FALSE, TRUE),
('MH02BZ4499', 'CAR', 'Amit Patel', '9988776655', FALSE, FALSE),
('DL04CC7711', 'ACCESSIBLE_CAR', 'David Miller', '9811223344', TRUE, FALSE),
('HR26BK9001', 'BIKE', 'Vikram Singh', '9711002233', FALSE, FALSE),
('KA03MG5566', 'CAR', 'Sunita Rao', '9844332211', FALSE, FALSE),
('TS09XY3322', 'CAR', 'Kiran Kumar', '9440112233', FALSE, FALSE),
('MH12QQ1122', 'BIKE', 'Rohan Deshmukh', '9822334455', FALSE, FALSE);

-- Active and Historical Parking Records
INSERT INTO parking_records (ticket_id, vehicle_id, slot_id, entry_time, exit_time, duration_minutes, parking_fee, payment_status, payment_method, status, entry_operator) VALUES
('SP-2026-0801', 1, 5, DATE_SUB(NOW(), INTERVAL 195 MINUTE), NULL, 0, 0.00, 'PENDING', 'NONE', 'ACTIVE', 'operator'),
('SP-2026-0802', 2, 3, DATE_SUB(NOW(), INTERVAL 105 MINUTE), NULL, 0, 0.00, 'PENDING', 'NONE', 'ACTIVE', 'operator'),
('SP-2026-0803', 3, 6, DATE_SUB(NOW(), INTERVAL 45 MINUTE), NULL, 0, 0.00, 'PENDING', 'NONE', 'ACTIVE', 'admin'),
('SP-2026-0804', 4, 2, DATE_SUB(NOW(), INTERVAL 80 MINUTE), NULL, 0, 0.00, 'PENDING', 'NONE', 'ACTIVE', 'operator'),
('SP-2026-0805', 5, 9, DATE_SUB(NOW(), INTERVAL 60 MINUTE), NULL, 0, 0.00, 'PENDING', 'NONE', 'ACTIVE', 'admin'),
('SP-2026-0806', 6, 13, DATE_SUB(NOW(), INTERVAL 140 MINUTE), NULL, 0, 0.00, 'PENDING', 'NONE', 'ACTIVE', 'operator'),
('SP-2026-0807', 7, 14, DATE_SUB(NOW(), INTERVAL 250 MINUTE), NULL, 0, 0.00, 'PENDING', 'NONE', 'ACTIVE', 'operator'),
('SP-2026-0808', 8, 19, DATE_SUB(NOW(), INTERVAL 35 MINUTE), NULL, 0, 0.00, 'PENDING', 'NONE', 'ACTIVE', 'operator'),
-- Historical Completed Records
('SP-2026-0701', 1, 5, DATE_SUB(NOW(), INTERVAL 28 HOUR), DATE_SUB(NOW(), INTERVAL 26 HOUR), 120, 50.00, 'PAID', 'UPI', 'COMPLETED', 'operator'),
('SP-2026-0702', 3, 6, DATE_SUB(NOW(), INTERVAL 27 HOUR), DATE_SUB(NOW(), INTERVAL 24 HOUR), 180, 70.00, 'PAID', 'CARD', 'COMPLETED', 'admin'),
('SP-2026-0703', 5, 9, DATE_SUB(NOW(), INTERVAL 25 HOUR), DATE_SUB(NOW(), INTERVAL 23 HOUR), 120, 20.00, 'PAID', 'CASH', 'COMPLETED', 'operator');

-- Active EV Charging Session (for KA05EV8821 in slot 3 with EV-GF-01)
INSERT INTO charging_sessions (charger_id, vehicle_number, slot_id, start_time, estimated_end_time, actual_end_time, kwh_consumed, status, charging_fee) VALUES
(1, 'KA05EV8821', 3, DATE_SUB(NOW(), INTERVAL 75 MINUTE), DATE_ADD(NOW(), INTERVAL 45 MINUTE), NULL, 28.5, 'IN_PROGRESS', 150.00);

-- Sample Temporary Reservation
INSERT INTO reservations (reservation_code, vehicle_number, vehicle_type, slot_id, customer_name, contact_number, reserved_at, expires_at, status) VALUES
('RES-9801', 'DL01XY9988', 'CAR', 8, 'Ananya Sen', '9810992211', DATE_SUB(NOW(), INTERVAL 5 MINUTE), DATE_ADD(NOW(), INTERVAL 10 MINUTE), 'PENDING');

-- Sample Initial Anomalies
INSERT INTO anomalies (anomaly_type, severity, vehicle_number, slot_id, description, suggested_resolution, status, detected_at) VALUES
('SLOT_OCCUPIED_NO_RECORD', 'WARNING', NULL, 16, 'Slot G-B04 is marked OCCUPIED but has no active session registered.', 'Verify physical vehicle in slot G-B04 and create entry ticket or mark slot AVAILABLE.', 'OPEN', DATE_SUB(NOW(), INTERVAL 30 MINUTE)),
('OVERDUE_CHARGING_BAY', 'INFO', 'KA05EV8821', 3, 'EV Charging bay EV-GF-01 is nearing scheduled completion in 45 minutes.', 'Notify driver via SMS reminder to vacate bay once charging ends.', 'OPEN', DATE_SUB(NOW(), INTERVAL 10 MINUTE));

-- Initial Audit Logs
INSERT INTO audit_logs (timestamp, event_type, target_ref, description, actor) VALUES
(DATE_SUB(NOW(), INTERVAL 195 MINUTE), 'ENTRY', 'SP-2026-0801', 'Vehicle TN01AB1234 entered and allocated slot G-A05', 'operator'),
(DATE_SUB(NOW(), INTERVAL 105 MINUTE), 'ENTRY', 'SP-2026-0802', 'Vehicle KA05EV8821 entered and allocated EV slot G-A03', 'operator'),
(DATE_SUB(NOW(), INTERVAL 75 MINUTE), 'EV_START', 'EV-GF-01', 'Charging started for vehicle KA05EV8821 at 50kW', 'operator'),
(DATE_SUB(NOW(), INTERVAL 5 MINUTE), 'RESERVATION', 'RES-9801', 'Slot G-A08 temporarily reserved for DL01XY9988', 'System');

-- Alternative Nearby Parking Facilities
INSERT INTO alternative_facilities (name, address, total_capacity, available_estimate, vehicle_types, distance_km, contact_info) VALUES
('City Central Metro Parking', 'Opposite Metro Station Gate 2, Ring Road', 250, 85, 'CAR, BIKE', 0.6, '+91-80-23456789'),
('Grand Galleria Multi-Level Parking', 'Block C, Commercial Hub, 4th Cross', 400, 140, 'CAR, BIKE, EV_CAR', 1.2, '+91-80-87654321'),
('Civic Center Underground Parking', 'Near Town Hall & Central Park', 180, 22, 'CAR, ACCESSIBLE_CAR', 1.8, '+91-80-55443322');

-- Graph Navigation Nodes for Ground Floor
INSERT INTO route_nodes (node_id, floor, name, node_type, x_coord, y_coord) VALUES
('N_ENTRANCE', 'Ground Floor', 'Main Entrance Gate A', 'ENTRANCE', 50, 100),
('N_AISLE_GA', 'Ground Floor', 'GF Zone A Central Aisle', 'AISLE', 150, 100),
('N_SLOT_GA01', 'Ground Floor', 'Slot G-A01 (Accessible)', 'SLOT', 150, 50),
('N_SLOT_GA03', 'Ground Floor', 'Slot G-A03 (EV Bay)', 'SLOT', 150, 70),
('N_SLOT_GA05', 'Ground Floor', 'Slot G-A05 (Car)', 'SLOT', 150, 130),
('N_RAMP_B1', 'Ground Floor', 'Basement 1 Access Ramp', 'RAMP', 250, 50),
('N_RAMP_F1', 'Ground Floor', 'First Floor Access Ramp', 'RAMP', 250, 150),
('N_AISLE_GB', 'Ground Floor', 'GF Zone B Central Aisle', 'AISLE', 350, 100),
('N_SLOT_GB01', 'Ground Floor', 'Slot G-B01 (Car)', 'SLOT', 350, 50),
('N_SLOT_GB05', 'Ground Floor', 'Slot G-B05 (EV Bay)', 'SLOT', 350, 150),
('N_EXIT_NORTH', 'Ground Floor', 'North Exit Gate (Express)', 'EXIT', 450, 50),
('N_EXIT_SOUTH', 'Ground Floor', 'South Exit Gate (Ring Road)', 'EXIT', 450, 150);

-- Graph Route Edges (Bi-directional weights in meters)
INSERT INTO route_edges (from_node, to_node, distance_weight, direction_info) VALUES
('N_ENTRANCE', 'N_AISLE_GA', 15, 'Enter through Gate A, follow Zone A signs'),
('N_AISLE_GA', 'N_SLOT_GA01', 5, 'Turn left into Bay G-A01'),
('N_AISLE_GA', 'N_SLOT_GA03', 8, 'Turn left into EV Bay G-A03'),
('N_AISLE_GA', 'N_SLOT_GA05', 6, 'Turn right into Bay G-A05'),
('N_AISLE_GA', 'N_RAMP_B1', 25, 'Proceed towards Down Ramp to Basement'),
('N_AISLE_GA', 'N_RAMP_F1', 25, 'Proceed towards Up Ramp to First Floor'),
('N_AISLE_GA', 'N_AISLE_GB', 30, 'Continue straight through central corridor to Zone B'),
('N_AISLE_GB', 'N_SLOT_GB01', 8, 'Turn left into Bay G-B01'),
('N_AISLE_GB', 'N_SLOT_GB05', 10, 'Turn right into EV Bay G-B05'),
('N_AISLE_GB', 'N_EXIT_NORTH', 20, 'Take north corridor to North Exit Gate'),
('N_AISLE_GB', 'N_EXIT_SOUTH', 22, 'Take south corridor to South Exit Gate'),
-- Reverse connections for exit navigation from slots
('N_SLOT_GA01', 'N_AISLE_GA', 5, 'Back out of bay into Zone A aisle'),
('N_SLOT_GA03', 'N_AISLE_GA', 8, 'Back out of EV bay into Zone A aisle'),
('N_SLOT_GA05', 'N_AISLE_GA', 6, 'Back out of bay into Zone A aisle'),
('N_SLOT_GB01', 'N_AISLE_GB', 8, 'Back out of bay into Zone B aisle'),
('N_SLOT_GB05', 'N_AISLE_GB', 10, 'Back out of EV bay into Zone B aisle'),
('N_AISLE_GA', 'N_EXIT_NORTH', 45, 'Follow express overhead signage to North Exit Gate'),
('N_AISLE_GA', 'N_EXIT_SOUTH', 48, 'Follow ring road signage to South Exit Gate');
