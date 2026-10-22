package com.smartpark.util;

import com.smartpark.enums.VehicleType;
import com.smartpark.model.ParkingTariff;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * ParkingFeeCalculator implements configurable tiered pricing:
 * - Vehicle-specific first hour and subsequent hour rates
 * - Grace period exemption (e.g. <= 15 mins)
 * - EV charging surcharges
 * - 24-hour daily maximum rate caps
 */
public class ParkingFeeCalculator {

    public static class FeeBreakdown {
        private final long actualMinutes;
        private final long billedHours;
        private final double firstHourRate;
        private final double additionalHourRate;
        private final double baseParkingFee;
        private final double evChargingSurcharge;
        private final double dailyMaxCap;
        private final boolean gracePeriodApplied;
        private final double totalFee;

        public FeeBreakdown(long actualMinutes, long billedHours, double firstHourRate,
                            double additionalHourRate, double baseParkingFee, double evChargingSurcharge,
                            double dailyMaxCap, boolean gracePeriodApplied, double totalFee) {
            this.actualMinutes = actualMinutes;
            this.billedHours = billedHours;
            this.firstHourRate = firstHourRate;
            this.additionalHourRate = additionalHourRate;
            this.baseParkingFee = baseParkingFee;
            this.evChargingSurcharge = evChargingSurcharge;
            this.dailyMaxCap = dailyMaxCap;
            this.gracePeriodApplied = gracePeriodApplied;
            this.totalFee = Math.round(totalFee * 100.0) / 100.0;
        }

        public long getActualMinutes() { return actualMinutes; }
        public long getBilledHours() { return billedHours; }
        public double getFirstHourRate() { return firstHourRate; }
        public double getAdditionalHourRate() { return additionalHourRate; }
        public double getBaseParkingFee() { return baseParkingFee; }
        public double getEvChargingSurcharge() { return evChargingSurcharge; }
        public double getDailyMaxCap() { return dailyMaxCap; }
        public boolean isGracePeriodApplied() { return gracePeriodApplied; }
        public double getTotalFee() { return totalFee; }

        public String getFormattedDuration() {
            long hours = actualMinutes / 60;
            long mins = actualMinutes % 60;
            if (hours > 0 && mins > 0) return hours + "h " + mins + "m";
            if (hours > 0) return hours + "h";
            return mins + "m";
        }
    }

    public static FeeBreakdown calculate(LocalDateTime entryTime, LocalDateTime exitTime,
                                         VehicleType type, ParkingTariff tariff, boolean isEvCharging) {
        if (entryTime == null) entryTime = LocalDateTime.now();
        if (exitTime == null) exitTime = LocalDateTime.now();

        Duration duration = Duration.between(entryTime, exitTime);
        long minutes = Math.max(0, duration.toMinutes());

        int gracePeriod = (tariff != null) ? tariff.getGracePeriodMins() : 15;
        double firstHr = (tariff != null) ? tariff.getFirstHourRate() : type.getFirstHourRate();
        double extraHr = (tariff != null) ? tariff.getAdditionalHourRate() : type.getAdditionalHourRate();
        double dailyCap = (tariff != null) ? tariff.getDailyMaximum() : 250.0;
        double evHourlySurcharge = (tariff != null) ? tariff.getEvSurchargePerHr() : 15.0;

        // Check grace period
        if (minutes <= gracePeriod) {
            return new FeeBreakdown(minutes, 0, firstHr, extraHr, 0.0, 0.0, dailyCap, true, 0.0);
        }

        // Billed hours rounded up to ceiling
        long billedHours = (long) Math.ceil((double) minutes / 60.0);
        if (billedHours < 1) billedHours = 1;

        double baseFee;
        if (billedHours <= 1) {
            baseFee = firstHr;
        } else {
            baseFee = firstHr + ((billedHours - 1) * extraHr);
        }

        double evSurcharge = 0.0;
        if (isEvCharging) {
            evSurcharge = billedHours * evHourlySurcharge;
        }

        double total = baseFee + evSurcharge;

        // Check 24-hr daily cap if single-day parking
        if (billedHours <= 24 && total > dailyCap) {
            total = dailyCap;
        }

        return new FeeBreakdown(minutes, billedHours, firstHr, extraHr, baseFee, evSurcharge, dailyCap, false, total);
    }

    public static FeeBreakdown calculate(LocalDateTime entryTime, LocalDateTime exitTime, VehicleType type) {
        return calculate(entryTime, exitTime, type, null, false);
    }
}