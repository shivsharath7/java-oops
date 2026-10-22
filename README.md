# SmartPark — Intelligent Parking & Congestion Management System

[![Java](https://img.shields.io/badge/Java-21%20LTS-blue.svg)](https://www.oracle.com/java/)
[![JavaFX](https://img.shields.io/badge/JavaFX-21-orange.svg)](https://openjfx.io/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue.svg)](https://www.mysql.com/)
[![Maven](https://img.shields.io/badge/Apache%20Maven-3.9+-red.svg)](https://maven.apache.org/)
[![Tests](https://img.shields.io/badge/Tests-37%2F37%20Passed-brightgreen.svg)]()
[![License](https://img.shields.io/badge/License-Academic%20Project-green.svg)]()

> A robust, full-stack desktop application developed in Java 21, JavaFX, and MySQL 8. Built specifically to demonstrate advanced Object-Oriented Programming (OOP) paradigms, real-time graph algorithms (Dijkstra's Shortest Path), multi-factor weighted allocation heuristics, automated anomaly detection, hardware IoT sensor abstraction, and an enterprise dark-theme UI.

---

## Table of Contents
1. [System Overview & Architecture](#system-overview--architecture)
2. [12 Functional Modules](#12-functional-modules)
3. [Object-Oriented Design & Design Patterns](#object-oriented-design--design-patterns)
4. [Algorithms & Heuristics](#algorithms--heuristics)
5. [Hardware Integration Architecture](#hardware-integration-architecture)
6. [Database Schema & ER Design](#database-schema--er-design)
7. [Installation, Configuration & Execution](#installation-configuration--execution)
8. [Automated Test Suite](#automated-test-suite)
9. [College Viva Voce Q&A Guide](#college-viva-voce-qa-guide)

---

## 1. System Overview & Architecture

Modern multi-floor parking complexes frequently suffer from entrance queuing, localized bottlenecks, uneven floor utilization, lost vehicle disputes, and lack of real-time visibility. **SmartPark** solves these challenges by combining:
- Dynamic congestion estimation and balanced multi-factor slot allocation.
- In-facility navigation using Dijkstra's shortest path graph solver.
- 8 automated anomaly detection checks to prevent fraud, ghost occupancy, and tariff mismatches.
- Modular IoT hardware support (Ultrasonic HC-SR04 sensors with Arduino or hardware simulation).
- Enterprise-grade JavaFX dark UI (`#0f172a` navy background, `#1e293b` cards, emerald/amber/rose accent status indicators).

```
                      +---------------------------------------+
                      |       JavaFX Presentation Layer       |
                      | (Main Shell + 12 Controllers + CSS)   |
                      +-------------------+-------------------+
                                          |
                                          v
                      +---------------------------------------+
                      |          Business Service Layer       |
                      | - ParkingService     - AllocationSvc  |
                      | - ReservationService - ChargingService|
                      | - RoutePlanningSvc   - AnomalyDetector|
                      +---------+-------------------+---------+
                                |                   |
            +-------------------+---+           +---+-------------------+
            | Algorithm & Strategy  |           | Hardware Abstraction  |
            | - DijkstraRoutePlanner|           | - OccupancyProvider   |
            | - StandardAllocation  |           | - SimulatedProvider   |
            | - CongestionAwareStrat|           | - ArduinoProvider     |
            +-----------------------+           +-----------------------+
                                |
                                v
                      +---------------------------------------+
                      |         Data Access Objects (DAO)     |
                      | (13 DAOs with Connection Pooling)     |
                      +-------------------+-------------------+
                                          |
                                          v
                      +---------------------------------------+
                      |        MySQL 8.0 Relational DB        |
                      | (18 Normalized Tables, Triggers, FKs) |
                      +---------------------------------------+
```

---

## 2. 12 Functional Modules

SmartPark provides 12 persistent functional modules accessible from the sidebar navigation:

| # | Navigation Module | Key Functionality & Viva Points |
|---|---|---|
| **1** | **Live Dashboard** | Real-time floor occupancy cards (Total/Occupied/Available/Rate), visual floor utilization meters, active EV chargers gauge, live system metrics, Quick Check-In / Quick Check-Out action modals, and recent activity log. |
| **2** | **Interactive Map** | Floor switcher (Basement 1, Ground Floor, First Floor), Zone and Type filters, visual grid representation of parking bays with color-coded status badges (Available, Occupied, Reserved, Maintenance, EV Charging, Accessible), and instant slot inspection cards. |
| **3** | **Vehicle Entry** | License plate lookup, vehicle type selection (`CAR`, `BIKE`, `EV_CAR`), driver name & phone input, accessibility toggle, EV charging toggle, one-click smart slot assignment, duplicate active session guard, and thermal-style ticket printing modal. |
| **4** | **Vehicle Exit & Billing** | Vehicle lookup by Plate or Ticket Number, entry time calculation, tiered tariff calculation with grace period, peak-hour surcharge, EV charging consumption billing, payment method selection (Cash, Card, UPI), discount voucher engine, receipt generator, and automatic slot release. |
| **5** | **Find My Vehicle & Route** | Reverse vehicle locator by license plate or ticket ID, display of assigned floor, zone, and slot number, and graphical step-by-step walking path from any entrance/elevator to the parking bay with distance and estimated walking time. |
| **6** | **Smart Slot Allocation** | Dual allocation strategy selector (`Standard` vs `Congestion-Aware`), real-time weight sliders (Congestion, Entrance Proximity, Exit Proximity), top 3 ranked recommendation candidates with transparent scoring breakdown and textual justification. |
| **7** | **Reservations** | Temporary advance booking system, customer contact capture, arrival time specification, deposit payment handling, background daemon thread auto-expiring unfulfilled reservations past grace period (default 30 mins) with automated slot release. |
| **8** | **EV Charging Bays** | Dedicated electric vehicle charger bay monitoring, charger type (Level 2 AC / DC Fast Charger), power rating (kW), real-time session tracking, kWh consumed calculation, and auto-integration into the main exit billing system. |
| **9** | **Anomaly & Fraud Detection** | 8 real-time security heuristics: sensor-software state mismatch, ghost vehicle (occupied in DB, vacant sensor), unauthorized vehicle, overstaying threshold (>24 hrs), unassigned parking, tariff bypass, rapid re-entry, and reservation no-show. One-click dismiss or resolve with operator notes. |
| **10**| **Parking Records & History** | Searchable audit log of completed and active parking sessions, filterable by date range, vehicle type, and payment status, with dispute filing dialog for overcharging/mismatches. |
| **11**| **Analytics & Export** | Executive metrics, peak parking hours bar chart, vehicle distribution pie chart, revenue breakdown by payment method, floor utilization comparisons, and one-click export of data to CSV reports. |
| **12**| **Admin & System Settings** | Hourly tariff configuration per vehicle type, grace period & lost ticket penalty settings, peak hour definitions and multipliers, sensor hardware simulation toggle, alternative facility redirection URLs, and secure operator login/logout. |

---

## 3. Object-Oriented Design & Design Patterns

### 3.1 OOP Principles Demonstrated
- **Inheritance**:
  - Base class: `Vehicle` (`vehicleNumber`, `vehicleType`, `ownerName`, `ownerPhone`, `accessibilityNeeded`, `requiresCharging`).
  - Derived classes: `Car`, `Bike`, `ElectricCar` (extending `Vehicle` with specialized attributes such as `batteryCapacityKwh`, `currentChargePercentage`).
- **Polymorphism**:
  - `SlotAllocationStrategy` interface implemented by `StandardAllocationStrategy` and `CongestionAwareAllocationStrategy`.
  - Polymorphic tariff fee computation in `ParkingRecord` and `Vehicle` subclasses.
  - Sensor hardware interface `OccupancyProvider` implemented by `SimulatedOccupancyProvider` and `ArduinoOccupancyProvider`.
- **Encapsulation**:
  - All model attributes are private with strict getters, setters, and validation guards in `ValidationUtil` (e.g., regex license plate validation, non-negative fee constraints).
- **Composition**:
  - `ParkingSlot` composes status, floor, zone, distance attributes, and optional `EvCharger`.
  - `ParkingRecord` composes `Vehicle`, `ParkingSlot`, and `PaymentStatus`.

### 3.2 Design Patterns Implemented
1. **Strategy Pattern**: Decouples slot allocation heuristics from the `ParkingService`. Operators can swap between proximity-first standard allocation and congestion-aware load balancing at runtime without code changes.
2. **Data Access Object (DAO) Pattern**: Complete separation of business rules and database SQL queries across 13 dedicated DAO classes (`ParkingSlotDAO`, `ParkingRecordDAO`, `VehicleDAO`, etc.).
3. **Singleton Pattern**: Managed database connection pool in `DatabaseConnection.java` ensuring thread-safe, efficient JDBC connection reuse.
4. **Observer / Scheduled Daemon Pattern**: Background `ScheduledExecutorService` in `ReservationService` running at fixed intervals to evaluate and auto-expire stale reservations.

---

## 4. Algorithms & Heuristics

### 4.1 Dijkstra's Shortest Path Route Planner
- **Graph Representation**: Multi-floor facility modeled as a directed weighted graph $G = (V, E)$, stored in `route_nodes` and `route_edges`.
  - Vertices $V$: Entrances, Elevators, Staircases, Floor Ramps, Zone Intersections, and Parking Bays.
  - Edges $E$: Drive aisles and pedestrian walkways with distance weights in meters.
- **Algorithm**: Dijkstra's algorithm implemented with `java.util.PriorityQueue` to achieve $O((|V| + |E|) \log |V|)$ time complexity.
- **Output**: Turn-by-turn navigation path, total distance in meters, and estimated walking/driving duration.

### 4.2 Multi-Factor Weighted Slot Allocation Heuristic
In `CongestionAwareAllocationStrategy`, candidate slots are evaluated using a multi-criteria scoring function:

$$\text{Total Score} = W_c \cdot S_{\text{congestion}} + W_{\text{ent}} \cdot S_{\text{ent\_distance}} + W_{\text{ext}} \cdot S_{\text{ext\_distance}} + B_{\text{pref}}$$

Where:
- $S_{\text{congestion}} = \max(0, 100 - \text{ZoneOccupancyPercentage})$: Penalizes crowded zones to prevent floor bottlenecks.
- $S_{\text{ent\_distance}} = \max(0, 100 - (\text{DistanceToEntrance} \times 1.5)) + \text{Bonus}_{\text{nearEntrance}}$: Prioritizes convenient access.
- $S_{\text{ext\_distance}} = \max(0, 100 - (\text{DistanceToExit} \times 1.5)) + \text{Bonus}_{\text{nearExit}}$: Optimizes smooth post-parking departure.
- $B_{\text{pref}}$: Special bonus (+30 for accessible vehicles in designated spots; +25 for EVs in charging bays).

---

## 5. Hardware Integration Architecture

SmartPark cleanly decouples software business logic from physical hardware via the `OccupancyProvider` interface:

```java
public interface OccupancyProvider {
    SlotOccupancyState readSlotState(String slotNumber);
    void updateSlotState(String slotNumber, boolean isOccupied, double measuredDistanceCm);
    boolean isHardwareConnected();
}
```

### Supported Implementations:
1. **`SimulatedOccupancyProvider`**:
   - Emulates ultrasonic sensor readings based on software parking records and manual toggle events.
   - Any reading below **50.0 cm** signifies physical vehicle presence.
2. **`ArduinoOccupancyProvider`**:
   - Supports HC-SR04 ultrasonic sensors mounted over bays connected via USB-UART COM port.
   - Baud Rate: `9600`, 8 Data Bits, 1 Stop Bit, No Parity.
   - Serial Packet Protocol: `SLOT_ID:OCCUPIED:DISTANCE_CM\n` (e.g., `G-A01:1:28.4`).

---

## 6. Database Schema & ER Design

The database `smartpark_db` contains **18 normalized relational tables**:

```
+--------------------+        +--------------------+        +--------------------+
|   parking_floors   |<-------|   parking_zones    |<-------|   parking_slots    |
+--------------------+        +--------------------+        +--------------------+
                                                                      |
                     +--------------------+                           |
                     |      vehicles      |<---------+                |
                     +--------------------+          |                |
                               |                     |                |
                               v                     |                v
                     +--------------------+          |      +--------------------+
                     |  parking_records   |----------+----->|    ev_chargers     |
                     +--------------------+                 +--------------------+
                               |                                      |
                               v                                      v
                     +--------------------+                 +--------------------+
                     |  parking_disputes  |                 | charging_sessions  |
                     +--------------------+                 +--------------------+
```

### Key Tables:
- `admins`: Salted and hashed credentials (`SHA-256`), roles (`ADMIN`, `OPERATOR`, `MANAGER`).
- `parking_slots`: Unique slot numbers, vehicle type compatibility, entrance/exit distances, accessibility and EV flags.
- `parking_records`: Complete ticket transaction records, timestamps, payment calculations, cashier identifiers.
- `reservations`: Advance booking records with expiry timestamps and customer phone numbers.
- `anomalies`: Flagged sensor mismatches, overstays, and resolution audit trails.
- `audit_logs`: Immutable security audit trail recording all critical mutations.

---

## 7. Installation, Configuration & Execution

### 7.1 Prerequisites
- **Java 17 LTS or Java 21 LTS** (`java -version`)
- **Apache Maven 3.8+** (`mvn -version`)
- **MySQL Server 8.0+** running on `localhost:3306`

### 7.2 Database Initialization
Log into MySQL and execute the schema:
```bash
mysql -u root -p < database/smartpark.sql
```
*(Default database user is `root`, password configured in `config/db.properties`)*.

### 7.3 Configuration
External connection settings can be altered in `config/db.properties` or `src/main/resources/config/db.properties`:
```properties
db.url=jdbc:mysql://localhost:3306/smartpark_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
db.user=root
db.password=SmartPark@123
db.pool.max=10
```

### 7.4 Compile, Test & Run
To run the automated test suite:
```bash
mvn clean test
```
To launch the JavaFX application:
```bash
mvn javafx:run
```

### 7.5 Default Login Credentials
- **System Administrator**: `admin` / `admin123`
- **Parking Operator**: `operator` / `admin123`

---

## 8. Automated Test Suite

SmartPark includes a comprehensive JUnit 5 test suite consisting of **37 unit and integration tests**:

| Test Class | Focus Area | Tests | Status |
|---|---|---|---|
| `AllocationStrategyTest` | Proximity vs Congestion-aware algorithms, EV & Accessible filtering | 6 | **PASSED** |
| `DijkstraRoutePlannerTest` | Graph traversal, shortest path correctness, unreachable node handling | 4 | **PASSED** |
| `ParkingFeeCalculatorTest` | Tiered rates, grace periods, peak-hour surcharges, overnight rates | 5 | **PASSED** |
| `AnomalyDetectionServiceTest`| Sensor-software mismatch, ghost vehicles, overstay detection | 2 | **PASSED** |
| `ReservationServiceTest` | Booking creation, duplicate prevention, automated expiry | 3 | **PASSED** |
| `ValidationUtilTest` | License plate formats, phone numbers, tariff inputs, date boundaries | 15 | **PASSED** |
| `ReportExportTest` | CSV formatting, record sanitization, output generation | 1 | **PASSED** |
| `ParkingIntegrationTest` | End-to-end vehicle lifecycle (Check-in -> Billing -> Exit -> Slot Release) | 1 | **PASSED** |
| **Total** | **Comprehensive Full-System Coverage** | **37** | **ALL PASSED** |

---

## 9. College Viva Voce Q&A Guide

### Q1: Why did you use JavaFX instead of Swing?
**Answer**: JavaFX is the modern standard for Java client applications. It provides full separation of concerns by utilizing FXML for declarative UI layout, CSS for enterprise styling, and Java controllers for event handling. Unlike Swing, JavaFX runs on a hardware-accelerated graphics pipeline (Prism) and offers modern reactive properties and data-binding.

### Q2: How does your Dijkstra Route Planner handle multiple parking floors?
**Answer**: In our graph model, floor transitions are represented as weighted directional edges connecting ramp and staircase nodes across different floors (e.g., node `B1-RAMP-UP` connects to `GF-RAMP-DOWN` with a traversal distance weight). Dijkstra's algorithm treats the entire multi-floor facility as a single continuous directed graph, seamlessly computing cross-floor paths.

### Q3: Explain how the Strategy Pattern is used in slot allocation.
**Answer**: We defined a common interface `SlotAllocationStrategy` declaring `allocate(vehicle, slots, congestionMap, exit)`. We implemented two distinct algorithms: `StandardAllocationStrategy` (focusing purely on minimum walking distance to entrance) and `CongestionAwareAllocationStrategy` (balancing zone occupancy to avoid bottlenecks). The controller dynamically switches strategies via polymorphism without modifying any client code.

### Q4: How do you prevent SQL injection in database transactions?
**Answer**: We strictly employ parameterized `PreparedStatement` across all 13 DAO classes. User inputs are never concatenated directly into SQL queries. Additionally, inputs are validated using `ValidationUtil` before reaching the DAO layer.

### Q5: How is hardware failure handled if a sensor disconnects?
**Answer**: The system uses the `OccupancyProvider` abstraction. If physical serial communication with the Arduino times out or is disconnected, `isHardwareConnected()` returns false, and the system gracefully falls back to database software-tracked occupancy while alerting operators through the Anomaly Detection screen.

### Q6: How does the reservation system prevent slots from remaining reserved indefinitely?
**Answer**: The `ReservationService` spawns a background daemon thread using `ScheduledExecutorService` that checks the database at fixed intervals. Any reservation exceeding the configured grace period (e.g., 30 minutes past scheduled arrival) is automatically updated to `EXPIRED` status, and the assigned slot is freed back to `AVAILABLE`.

### Q7: Why did you implement custom anomaly detection?
**Answer**: In commercial parking, revenue leakage and security risks arise from ghost vehicles (sensor detects a car, but cashier ticket was never created), tailgating, or unauthorized parking in reserved/accessible bays. Our 8 automated heuristics continuously compare live sensor state against active billing records to detect and log fraudulent or erroneous activity.

---

*SmartPark — Intelligent Parking & Congestion Management System © 2026. All Rights Reserved.*
