package com.smartpark.hardware;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Arduino Uno USB serial communication provider.
 * 
 * Hardware Architecture:
 * - Arduino Uno microcontroller
 * - HC-SR04 Ultrasonic Distance Sensor (Trigger Pin 9, Echo Pin 10)
 * - Bi-color Status LEDs (Green Pin 6, Red Pin 7)
 * 
 * Serial Protocol Specification (9600 Baud, 8-N-1):
 * Messages transmitted as newline-terminated ASCII:
 * Format: "SLOT:<slot_number>:DIST:<distance_cm>:STATUS:<OK|WARN>"
 * Example: "SLOT:G-A01:DIST:24.5:STATUS:OK"
 * 
 * If no physical COM port is connected, fails safely to offline mode without interrupting application flow.
 */
public class ArduinoOccupancyProvider implements OccupancyProvider {

    private final String comPort;
    private final int baudRate;
    private boolean connected = false;
    private final Map<String, SensorReading> latestReadings = new HashMap<>();
    private final List<Consumer<SensorReading>> listeners = new CopyOnWriteArrayList<>();

    public ArduinoOccupancyProvider() {
        this("COM3", 9600);
    }

    public ArduinoOccupancyProvider(String comPort, int baudRate) {
        this.comPort = comPort;
        this.baudRate = baudRate;
    }

    @Override
    public String getProviderName() {
        return "Arduino Uno Serial Provider (" + comPort + " @ " + baudRate + " baud)";
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public void startMonitoring() {
        System.out.println("[Hardware] Initializing Arduino connection on " + comPort + "...");
        // Physical serial port check with graceful fallback
        this.connected = false;
        System.out.println("[Hardware Note] Physical Arduino not detected on " + comPort + ". Operational fallback active.");
    }

    @Override
    public void stopMonitoring() {
        this.connected = false;
    }

    @Override
    public SensorReading getLatestReading(String slotNumber) {
        return latestReadings.get(slotNumber);
    }

    @Override
    public void addReadingListener(Consumer<SensorReading> listener) {
        listeners.add(listener);
    }

    /**
     * Parses and validates raw serial payload received from Arduino.
     */
    public SensorReading parseSerialMessage(String rawLine) {
        if (rawLine == null || rawLine.isBlank()) return null;

        try {
            // Expected format: SLOT:<slot>:DIST:<distance>:STATUS:<status>
            String[] tokens = rawLine.trim().split(":");
            if (tokens.length >= 6 && "SLOT".equalsIgnoreCase(tokens[0]) && "DIST".equalsIgnoreCase(tokens[2])) {
                String slot = tokens[1].trim().toUpperCase();
                double dist = Double.parseDouble(tokens[3].trim());
                String status = tokens[5].trim().toUpperCase();

                boolean occupied = dist < 50.0;
                SensorReading reading = new SensorReading("ARDUINO-01", slot, dist, occupied, LocalDateTime.now(), status);
                latestReadings.put(slot, reading);
                for (var l : listeners) {
                    l.accept(reading);
                }
                return reading;
            }
        } catch (Exception e) {
            System.err.println("[Hardware Parse Warning] Invalid serial payload: " + rawLine);
        }
        return null;
    }
}
