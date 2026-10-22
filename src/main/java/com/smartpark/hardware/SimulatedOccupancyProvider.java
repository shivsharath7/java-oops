package com.smartpark.hardware;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Software-based ultrasonic sensor simulator.
 * Distance threshold < 50cm indicates vehicle presence.
 */
public class SimulatedOccupancyProvider implements OccupancyProvider {

    private final Map<String, SensorReading> latestReadings = new HashMap<>();
    private final List<java.util.function.Consumer<SensorReading>> listeners = new CopyOnWriteArrayList<>();
    private boolean active = false;

    public static final double OCCUPANCY_DISTANCE_THRESHOLD_CM = 50.0;

    public SimulatedOccupancyProvider() {
        initDefaultSensors();
    }

    private void initDefaultSensors() {
        // Initial simulated sensor states for Ground Floor demo slots
        recordSimulatedReading("SENS-GA01", "G-A01", 120.0); // > 50cm: Empty
        recordSimulatedReading("SENS-GA02", "G-A02", 22.0);  // < 50cm: Occupied
        recordSimulatedReading("SENS-GA03", "G-A03", 28.5);  // < 50cm: EV Occupied
        recordSimulatedReading("SENS-GA04", "G-A04", 145.0); // > 50cm: Empty
        recordSimulatedReading("SENS-GB01", "G-B01", 18.0);  // < 50cm: Occupied
    }

    public synchronized void recordSimulatedReading(String sensorId, String slotNumber, double distanceCm) {
        boolean isOccupied = distanceCm < OCCUPANCY_DISTANCE_THRESHOLD_CM;
        SensorReading reading = new SensorReading(
                sensorId, slotNumber, distanceCm, isOccupied, LocalDateTime.now(), "VALID"
        );
        latestReadings.put(slotNumber, reading);
        for (var listener : listeners) {
            listener.accept(reading);
        }
    }

    @Override
    public String getProviderName() {
        return "Software Ultrasonic Simulator";
    }

    @Override
    public boolean isConnected() {
        return true;
    }

    @Override
    public void startMonitoring() {
        this.active = true;
    }

    @Override
    public void stopMonitoring() {
        this.active = false;
    }

    @Override
    public SensorReading getLatestReading(String slotNumber) {
        return latestReadings.get(slotNumber);
    }

    @Override
    public void addReadingListener(java.util.function.Consumer<SensorReading> listener) {
        listeners.add(listener);
    }

    public Map<String, SensorReading> getAllReadings() {
        return Collections.unmodifiableMap(latestReadings);
    }
}
