package com.smartpark.hardware;

import java.util.function.Consumer;

/**
 * Interface defining contract for physical and simulated occupancy providers.
 */
public interface OccupancyProvider {
    String getProviderName();
    boolean isConnected();
    void startMonitoring();
    void stopMonitoring();
    SensorReading getLatestReading(String slotNumber);
    void addReadingListener(Consumer<SensorReading> listener);
}
