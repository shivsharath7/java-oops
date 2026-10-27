-- =============================================================================
-- Database: smartpark_db
-- Description: SmartPark Intelligent Parking Management System Schema
-- =============================================================================

CREATE DATABASE IF NOT EXISTS smartpark_db;
USE smartpark_db;

-- 1. Parking Slots Table
DROP TABLE IF EXISTS parking_records;
DROP TABLE IF EXISTS parking_slots;
DROP TABLE IF EXISTS vehicles;
DROP TABLE IF EXISTS admins;

CREATE TABLE parking_slots (
    id INT AUTO_INCREMENT PRIMARY KEY,
    slot_number VARCHAR(10) NOT NULL UNIQUE,
    floor VARCHAR(30) NOT NULL,
    zone VARCHAR(10) NOT NULL,
    vehicle_type ENUM('CAR', 'BIKE', 'EV_CAR') NOT NULL,
    status ENUM('AVAILABLE', 'OCCUPIED', 'RESERVED') DEFAULT 'AVAILABLE',
    is_near_entrance BOOLEAN DEFAULT FALSE,
    has_charging_station BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Vehicles Table
CREATE TABLE vehicles (
    id INT AUTO_INCREMENT PRIMARY KEY,
    vehicle_number VARCHAR(20) NOT NULL UNIQUE,
    vehicle_type ENUM('CAR', 'BIKE', 'EV_CAR') NOT NULL,
    customer_name VARCHAR(100),
    contact_number VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 3. Parking Records Table
CREATE TABLE parking_records (
    id INT AUTO_INCREMENT PRIMARY KEY,
    ticket_id VARCHAR(30) NOT NULL UNIQUE,
    vehicle_id INT NOT NULL,
    slot_id INT NOT NULL,
    entry_time DATETIME NOT NULL,
    exit_time DATETIME NULL,
    duration_minutes INT DEFAULT 0,
    parking_fee DECIMAL(10, 2) DEFAULT 0.00,
    payment_status ENUM('PAID', 'PENDING', 'WAIVED') DEFAULT 'PENDING',
    status ENUM('ACTIVE', 'COMPLETED') DEFAULT 'ACTIVE',
    FOREIGN KEY (vehicle_id) REFERENCES vehicles(id) ON DELETE CASCADE,
    FOREIGN KEY (slot_id) REFERENCES parking_slots(id) ON DELETE RESTRICT
);

-- 4. Admins Table
CREATE TABLE admins (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    full_name VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Initial Admin Record
INSERT INTO admins (username, password, full_name) 
VALUES ('admin', 'admin123', 'System Administrator');

-- Sample Parking Slots (Ground, 1st, 2nd Floors)
INSERT INTO parking_slots (slot_number, floor, zone, vehicle_type, status, is_near_entrance, has_charging_station) VALUES
('A01', 'Ground Floor', 'A', 'CAR', 'OCCUPIED', TRUE, FALSE),
('A02', 'Ground Floor', 'A', 'CAR', 'AVAILABLE', TRUE, FALSE),
('A03', 'Ground Floor', 'A', 'EV_CAR', 'OCCUPIED', TRUE, TRUE),
('A04', 'Ground Floor', 'A', 'EV_CAR', 'AVAILABLE', TRUE, TRUE),
('A05', 'Ground Floor', 'A', 'CAR', 'OCCUPIED', FALSE, FALSE),
('A06', 'Ground Floor', 'A', 'CAR', 'OCCUPIED', FALSE, FALSE),
('A07', 'Ground Floor', 'A', 'CAR', 'AVAILABLE', FALSE, FALSE),
('A08', 'Ground Floor', 'A', 'CAR', 'RESERVED', FALSE, FALSE),
('A09', 'Ground Floor', 'A', 'BIKE', 'OCCUPIED', FALSE, FALSE),
('A10', 'Ground Floor', 'A', 'BIKE', 'AVAILABLE', FALSE, FALSE),
('A11', 'Ground Floor', 'A', 'BIKE', 'AVAILABLE', FALSE, FALSE),
('A12', 'Ground Floor', 'A', 'CAR', 'AVAILABLE', FALSE, FALSE),

('B01', 'Ground Floor', 'B', 'CAR', 'OCCUPIED', FALSE, FALSE),
('B02', 'Ground Floor', 'B', 'CAR', 'OCCUPIED', FALSE, FALSE),
('B03', 'Ground Floor', 'B', 'CAR', 'AVAILABLE', FALSE, FALSE),
('B04', 'Ground Floor', 'B', 'CAR', 'OCCUPIED', FALSE, FALSE),
('B05', 'Ground Floor', 'B', 'EV_CAR', 'OCCUPIED', FALSE, TRUE),
('B06', 'Ground Floor', 'B', 'EV_CAR', 'AVAILABLE', FALSE, TRUE),
('B07', 'Ground Floor', 'B', 'BIKE', 'OCCUPIED', FALSE, FALSE),
('B08', 'Ground Floor', 'B', 'BIKE', 'OCCUPIED', FALSE, FALSE),
('B09', 'Ground Floor', 'B', 'BIKE', 'AVAILABLE', FALSE, FALSE),
('B10', 'Ground Floor', 'B', 'CAR', 'RESERVED', FALSE, FALSE),
('B11', 'Ground Floor', 'B', 'CAR', 'OCCUPIED', FALSE, FALSE),
('B12', 'Ground Floor', 'B', 'CAR', 'AVAILABLE', FALSE, FALSE),

('C01', 'First Floor', 'C', 'CAR', 'OCCUPIED', FALSE, FALSE),
('C02', 'First Floor', 'C', 'CAR', 'AVAILABLE', FALSE, FALSE),
('C03', 'First Floor', 'C', 'CAR', 'AVAILABLE', FALSE, FALSE),
('C04', 'First Floor', 'C', 'CAR', 'OCCUPIED', FALSE, FALSE),
('C05', 'First Floor', 'C', 'EV_CAR', 'AVAILABLE', FALSE, TRUE),
('C06', 'First Floor', 'C', 'EV_CAR', 'OCCUPIED', FALSE, TRUE),
('C07', 'First Floor', 'C', 'BIKE', 'AVAILABLE', FALSE, FALSE),
('C08', 'First Floor', 'C', 'BIKE', 'AVAILABLE', FALSE, FALSE),
('C09', 'First Floor', 'C', 'CAR', 'OCCUPIED', FALSE, FALSE),
('C10', 'First Floor', 'C', 'CAR', 'AVAILABLE', FALSE, FALSE),

('D01', 'First Floor', 'D', 'CAR', 'AVAILABLE', FALSE, FALSE),
('D02', 'First Floor', 'D', 'CAR', 'OCCUPIED', FALSE, FALSE),
('D03', 'First Floor', 'D', 'CAR', 'AVAILABLE', FALSE, FALSE),
('D04', 'First Floor', 'D', 'CAR', 'RESERVED', FALSE, FALSE),
('D05', 'First Floor', 'D', 'BIKE', 'AVAILABLE', FALSE, FALSE),
('D06', 'First Floor', 'D', 'BIKE', 'OCCUPIED', FALSE, FALSE),
('D07', 'First Floor', 'D', 'CAR', 'AVAILABLE', FALSE, FALSE),
('D08', 'First Floor', 'D', 'CAR', 'AVAILABLE', FALSE, FALSE);

-- Sample Vehicles
INSERT INTO vehicles (vehicle_number, vehicle_type, customer_name, contact_number) VALUES
('TN01AB1234', 'CAR', 'Rahul Sharma', '9876543210'),
('KA05EV8821', 'EV_CAR', 'Priya Sundaram', '9123456780'),
('MH02BZ4499', 'CAR', 'Amit Patel', '9988776655');

-- Sample Active Parking Record
INSERT INTO parking_records (ticket_id, vehicle_id, slot_id, entry_time, payment_status, status) VALUES
('SP-2026-0801', 1, 1, DATE_SUB(NOW(), INTERVAL 195 MINUTE), 'PENDING', 'ACTIVE'),
('SP-2026-0802', 2, 3, DATE_SUB(NOW(), INTERVAL 105 MINUTE), 'PENDING', 'ACTIVE'),
('SP-2026-0803', 3, 5, DATE_SUB(NOW(), INTERVAL 50 MINUTE), 'PENDING', 'ACTIVE');
