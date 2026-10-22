package com.smartpark.service;

import com.smartpark.dao.ParkingSlotDAO;
import com.smartpark.dao.SettingsDAO;
import com.smartpark.enums.SlotStatus;
import com.smartpark.enums.VehicleType;
import com.smartpark.model.ParkingSlot;
import com.smartpark.model.Vehicle;
import com.smartpark.strategy.AllocationDecision;
import com.smartpark.strategy.CongestionAwareAllocationStrategy;
import com.smartpark.strategy.SlotAllocationStrategy;
import com.smartpark.strategy.StandardAllocationStrategy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service orchestrating vehicle slot allocation strategies.
 * Demonstrates Strategy Pattern context and fallback handling.
 */
public class SlotAllocationService {

    private final ParkingSlotDAO slotDAO;
    private final SettingsDAO settingsDAO;
    private final Map<String, SlotAllocationStrategy> strategies = new HashMap<>();
    private SlotAllocationStrategy currentStrategy;

    public SlotAllocationService() {
        this(new ParkingSlotDAO(), new SettingsDAO());
    }

    public SlotAllocationService(ParkingSlotDAO slotDAO, SettingsDAO settingsDAO) {
        this.slotDAO = slotDAO;
        this.settingsDAO = settingsDAO;

        StandardAllocationStrategy standard = new StandardAllocationStrategy();
        CongestionAwareAllocationStrategy congestion = new CongestionAwareAllocationStrategy();

        strategies.put(standard.getName(), standard);
        strategies.put(congestion.getName(), congestion);

        String activeKey = settingsDAO.getSetting("active_strategy", "CONGESTION_AWARE");
        this.currentStrategy = strategies.getOrDefault(activeKey, congestion);
    }

    public void setStrategy(String strategyName) {
        if (strategies.containsKey(strategyName)) {
            this.currentStrategy = strategies.get(strategyName);
            settingsDAO.updateSetting("active_strategy", strategyName);
        }
    }

    public SlotAllocationStrategy getCurrentStrategy() {
        return currentStrategy;
    }

    public AllocationDecision allocateSlot(Vehicle vehicle, String preferredExit) {
        List<ParkingSlot> allSlots = slotDAO.getAllSlots();
        Map<String, Double> zoneOccupancyMap = computeZoneOccupancyMap(allSlots);
        return currentStrategy.allocate(vehicle, allSlots, zoneOccupancyMap, preferredExit);
    }

    public AllocationDecision evaluateAlternativeStrategy(String strategyName, Vehicle vehicle, String preferredExit) {
        SlotAllocationStrategy strat = strategies.getOrDefault(strategyName, currentStrategy);
        List<ParkingSlot> allSlots = slotDAO.getAllSlots();
        Map<String, Double> zoneOccupancyMap = computeZoneOccupancyMap(allSlots);
        return strat.allocate(vehicle, allSlots, zoneOccupancyMap, preferredExit);
    }

    public Map<String, Double> computeZoneOccupancyMap(List<ParkingSlot> allSlots) {
        Map<String, Double> map = new HashMap<>();
        Map<String, Integer> totalByZone = new HashMap<>();
        Map<String, Integer> occupiedByZone = new HashMap<>();

        for (ParkingSlot s : allSlots) {
            String key = s.getFloor() + ":" + s.getZone();
            totalByZone.put(key, totalByZone.getOrDefault(key, 0) + 1);
            if (s.getStatus() != SlotStatus.AVAILABLE) {
                occupiedByZone.put(key, occupiedByZone.getOrDefault(key, 0) + 1);
            }
        }

        for (Map.Entry<String, Integer> entry : totalByZone.entrySet()) {
            int total = entry.getValue();
            int occ = occupiedByZone.getOrDefault(entry.getKey(), 0);
            double pct = total > 0 ? (occ * 100.0) / total : 0.0;
            map.put(entry.getKey(), pct);
        }
        return map;
    }
}