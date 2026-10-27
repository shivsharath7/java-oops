package com.smartpark.util;

import com.smartpark.enums.VehicleType;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * ParkingFeeCalculator implements the college project pricing matrix:
 * - Car: 1st hr ₹30, +₹20 each extra hr
 * - Bike: 1st hr ₹10, +₹10 each extra hr
 * - EV Car: 1st hr ₹40, +₹25 each extra hr
 * Duration rounded up to the nearest hour.
 */
public class ParkingFeeCalculator {

    public static class FeeBreakdown {
        private final long actualMinutes;
        private final long billedHours;
        private final double firstHourRate;
        private final double additionalHourRate;
        private final double totalFee;

        public FeeBreakdown(long actualMinutes, long billedHours, double firstHourRate, double additionalHourRate, double totalFee) {
            this.actualMinutes = actualMinutes;
            this.billedHours = billedHours;
            this.firstHourRate = firstHourRate;
            this.additionalHourRate = additionalHourRate;
            this.totalFee = totalFee;
        }

        public long getActualMinutes() { return actualMinutes; }
        public long getBilledHours() { return billedHours; }
        public double getFirstHourRate() { return firstHourRate; }
        public double getAdditionalHourRate() { return additionalHourRate; }
        public double getTotalFee() { return totalFee; }

        public String getFormattedDuration() {
            long hours = actualMinutes / 60;
            long mins = actualMinutes % 60;
            if (hours > 0 && mins > 0) return hours + " Hours " + mins + " Minutes";
            if (hours > 0) return hours + " Hours";
            return mins + " Minutes";
        }
    }

    public static FeeBreakdown calculate(LocalDateTime entryTime, LocalDateTime exitTime, VehicleType type) {
        if (entryTime == null || exitTime == null) {
            return new FeeBreakdown(0, 1, type.getFirstHourRate(), type.getAdditionalHourRate(), type.getFirstHourRate());
        }

        Duration duration = Duration.between(entryTime, exitTime);
        long minutes = Math.max(1, duration.toMinutes());

        // Round up to nearest hour (e.g. 2 hr 15 min -> 3 hr; 45 min -> 1 hr)
        long billedHours = (long) Math.ceil((double) minutes / 60.0);
        if (billedHours < 1) billedHours = 1;

        double firstHr = type.getFirstHourRate();
        double extraRate = type.getAdditionalHourRate();
        long extraHours = Math.max(0, billedHours - 1);
        double totalFee = firstHr + (extraHours * extraRate);

        return new FeeBreakdown(minutes, billedHours, firstHr, extraRate, totalFee);
    }
}