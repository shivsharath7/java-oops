# SmartPark – Intelligent Parking Management System
A complete Java, JavaFX, and MySQL Smart Parking Management System for college projects and commercial facilities.

## Quick Start Guide

### 1. Prerequisites
- Java JDK 17 or higher
- MySQL Server 8.0+
- Apache Maven 3.8+ (or use IntelliJ IDEA / Eclipse with built-in Maven)

### 2. Database Setup
1. Open MySQL Workbench or terminal.
2. Execute the database script located at `database/smartpark.sql`.
3. Verify the `smartpark_db` database is created with initial seeds.
4. If your MySQL root password is not "password", update `DatabaseConnection.java`.

### 3. Running the Application
Open a terminal in this directory and execute:
```bash
mvn clean compile
mvn javafx:run
```
Or simply open the project in IntelliJ IDEA, let Maven sync, and run `src/main/java/com/smartpark/application/Main.java`.

### Default Credentials
- Admin Username: `admin`
- Admin Password: `admin123`
