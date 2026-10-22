package com.smartpark;

import com.smartpark.service.ReportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Analytics and CSV Report Export Tests")
public class ReportExportTest {

    @Test
    @DisplayName("Should generate valid formatted CSV file with headers")
    void testExportRecordsToCsv() throws IOException {
        ReportService reportService = new ReportService();
        File tempFile = File.createTempFile("smartpark_test_export_", ".csv");
        tempFile.deleteOnExit();

        File exported = reportService.exportRecordsToCsv(tempFile, null, null, "ALL");
        assertNotNull(exported);
        assertTrue(exported.exists());
        assertTrue(exported.length() > 0);

        List<String> lines = Files.readAllLines(exported.toPath());
        assertFalse(lines.isEmpty());

        // Header check
        String header = lines.get(0);
        assertTrue(header.contains("Ticket ID"));
        assertTrue(header.contains("Vehicle Number"));
        assertTrue(header.contains("Duration"));
        assertTrue(header.contains("Fee"));
    }
}
