package com.smartpark;

import com.smartpark.enums.VehicleType;
import com.smartpark.model.ParkingTariff;
import com.smartpark.util.ParkingFeeCalculator;
import com.smartpark.util.ParkingFeeCalculator.FeeBreakdown;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Parking Fee Calculator Tests")
public class ParkingFeeCalculatorTest {

    private final ParkingTariff carTariff = new ParkingTariff(1, VehicleType.CAR, 30.0, 20.0, 250.0, 0.0, 15);
    private final ParkingTariff evTariff = new ParkingTariff(2, VehicleType.EV_CAR, 40.0, 25.0, 300.0, 15.0, 15);
    private final ParkingTariff bikeTariff = new ParkingTariff(3, VehicleType.BIKE, 10.0, 10.0, 80.0, 0.0, 15);

    @Test
    @DisplayName("Grace Period Exemption: Stay under 15 minutes should incur zero fee")
    void testGracePeriod() {
        LocalDateTime entry = LocalDateTime.now().minusMinutes(12);
        LocalDateTime exit = LocalDateTime.now();

        FeeBreakdown fee = ParkingFeeCalculator.calculate(entry, exit, VehicleType.CAR, carTariff, false);
        assertTrue(fee.isGracePeriodApplied());
        assertEquals(0.0, fee.getTotalFee(), 0.001);
        assertEquals(0, fee.getBilledHours());
    }

    @Test
    @DisplayName("Single hour car stay (45 minutes) should charge first-hour rate")
    void testFirstHourCar() {
        LocalDateTime entry = LocalDateTime.now().minusMinutes(45);
        LocalDateTime exit = LocalDateTime.now();

        FeeBreakdown fee = ParkingFeeCalculator.calculate(entry, exit, VehicleType.CAR, carTariff, false);
        assertFalse(fee.isGracePeriodApplied());
        assertEquals(1, fee.getBilledHours());
        assertEquals(30.0, fee.getTotalFee(), 0.001);
    }

    @Test
    @DisplayName("Multi-hour car stay (2 hours 15 minutes) rounds up to 3 hours")
    void testMultiHourCar() {
        // 2 hrs 15 mins = 135 mins => rounds up to 3 billed hours
        // 1st hour: ₹30, 2 additional hours: 2 * ₹20 = ₹40 => Total: ₹70
        LocalDateTime entry = LocalDateTime.now().minusMinutes(135);
        LocalDateTime exit = LocalDateTime.now();

        FeeBreakdown fee = ParkingFeeCalculator.calculate(entry, exit, VehicleType.CAR, carTariff, false);
        assertEquals(3, fee.getBilledHours());
        assertEquals(70.0, fee.getTotalFee(), 0.001);
    }

    @Test
    @DisplayName("EV charging surcharge should be added to base fee")
    void testEvChargingSurcharge() {
        // 2 hours EV: 1st hr ₹40, + 1 hr ₹25 = ₹65 base.
        // EV surcharge: 2 hrs * ₹15 = ₹30 => Total: ₹95
        LocalDateTime entry = LocalDateTime.now().minusMinutes(120);
        LocalDateTime exit = LocalDateTime.now();

        FeeBreakdown fee = ParkingFeeCalculator.calculate(entry, exit, VehicleType.EV_CAR, evTariff, true);
        assertEquals(2, fee.getBilledHours());
        assertEquals(65.0, fee.getBaseParkingFee(), 0.001);
        assertEquals(30.0, fee.getEvChargingSurcharge(), 0.001);
        assertEquals(95.0, fee.getTotalFee(), 0.001);
    }

    @Test
    @DisplayName("Daily maximum cap should restrict excessive fees")
    void testDailyMaxCap() {
        // 18 hours car: 30 + 17 * 20 = 370 => should be capped at daily max ₹250
        LocalDateTime entry = LocalDateTime.now().minusHours(18);
        LocalDateTime exit = LocalDateTime.now();

        FeeBreakdown fee = ParkingFeeCalculator.calculate(entry, exit, VehicleType.CAR, carTariff, false);
        assertEquals(18, fee.getBilledHours());
        assertEquals(250.0, fee.getTotalFee(), 0.001);
    }
}
