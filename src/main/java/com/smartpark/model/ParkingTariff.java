package com.smartpark.model;

import com.smartpark.enums.VehicleType;

public class ParkingTariff {
    private int id;
    private VehicleType vehicleType;
    private double firstHourRate;
    private double additionalHourRate;
    private double dailyMaximum;
    private double evSurchargePerHr;
    private int gracePeriodMins;

    public ParkingTariff() {}

    public ParkingTariff(int id, VehicleType vehicleType, double firstHourRate, double additionalHourRate,
                         double dailyMaximum, double evSurchargePerHr, int gracePeriodMins) {
        this.id = id;
        this.vehicleType = vehicleType;
        this.firstHourRate = firstHourRate;
        this.additionalHourRate = additionalHourRate;
        this.dailyMaximum = dailyMaximum;
        this.evSurchargePerHr = evSurchargePerHr;
        this.gracePeriodMins = gracePeriodMins;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public VehicleType getVehicleType() { return vehicleType; }
    public void setVehicleType(VehicleType vehicleType) { this.vehicleType = vehicleType; }

    public double getFirstHourRate() { return firstHourRate; }
    public void setFirstHourRate(double firstHourRate) { this.firstHourRate = firstHourRate; }

    public double getAdditionalHourRate() { return additionalHourRate; }
    public void setAdditionalHourRate(double additionalHourRate) { this.additionalHourRate = additionalHourRate; }

    public double getDailyMaximum() { return dailyMaximum; }
    public void setDailyMaximum(double dailyMaximum) { this.dailyMaximum = dailyMaximum; }

    public double getEvSurchargePerHr() { return evSurchargePerHr; }
    public void setEvSurchargePerHr(double evSurchargePerHr) { this.evSurchargePerHr = evSurchargePerHr; }

    public int getGracePeriodMins() { return gracePeriodMins; }
    public void setGracePeriodMins(int gracePeriodMins) { this.gracePeriodMins = gracePeriodMins; }
}
