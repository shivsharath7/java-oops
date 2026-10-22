package com.smartpark;

import com.smartpark.util.ValidationUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Vehicle Registration Plate Validation Tests")
public class ValidationUtilTest {

    @ParameterizedTest
    @ValueSource(strings = {"KA05EV8821", "TN01AB1234", "MH02BZ4499", "DL04CC7711", "HR26-BK-9001", "UP16AX1122"})
    @DisplayName("Should accept valid vehicle registration plates")
    void testValidPlates(String plate) {
        assertTrue(ValidationUtil.isValidVehicleNumber(plate), "Expected valid plate: " + plate);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "A", "AB1", "THISISWAYTOOLONGFORAPLATE", "KA@#$1234", "INVALID!*"})
    @DisplayName("Should reject invalid vehicle registration plates")
    void testInvalidPlates(String plate) {
        assertFalse(ValidationUtil.isValidVehicleNumber(plate), "Expected invalid plate: " + plate);
    }

    @Test
    @DisplayName("Should handle null plate gracefully")
    void testNullPlate() {
        assertFalse(ValidationUtil.isValidVehicleNumber(null));
        assertEquals("", ValidationUtil.cleanPlate(null));
    }

    @Test
    @DisplayName("Should normalize whitespace and uppercase characters")
    void testCleanPlate() {
        assertEquals("KA05EV8821", ValidationUtil.cleanPlate("  ka 05 ev 8821 "));
    }
}
