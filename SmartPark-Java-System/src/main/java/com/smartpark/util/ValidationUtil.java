package com.smartpark.util;

import java.util.regex.Pattern;

public class ValidationUtil {
    // Regex for standard Indian & alphanumeric vehicle plates (e.g. TN01AB1234, DL08CK9012, KA05EV8821)
    private static final Pattern PLATE_PATTERN = Pattern.compile("^[A-Z0-9-]{4,12}$", Pattern.CASE_INSENSITIVE);

    public static boolean isValidVehicleNumber(String vehicleNumber) {
        if (vehicleNumber == null) return false;
        String clean = vehicleNumber.trim().replaceAll("\\s+", "").toUpperCase();
        return PLATE_PATTERN.matcher(clean).matches();
    }

    public static String cleanPlate(String raw) {
        if (raw == null) return "";
        return raw.trim().replaceAll("\\s+", "").toUpperCase();
    }
}