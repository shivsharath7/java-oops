package com.smartpark;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FXML Loading & Controller Binding Integrity Tests")
public class FxmlIntegrityTest {

    private static final AtomicBoolean jfxInitialized = new AtomicBoolean(false);

    @BeforeAll
    static void initJavaFX() throws InterruptedException {
        if (!jfxInitialized.get()) {
            CountDownLatch latch = new CountDownLatch(1);
            try {
                Platform.startup(latch::countDown);
                latch.await(5, TimeUnit.SECONDS);
                jfxInitialized.set(true);
            } catch (IllegalStateException e) {
                // Platform already started in another test runner
                jfxInitialized.set(true);
            }
        }
    }

    @ParameterizedTest(name = "FXML Screen Integrity: {0}")
    @ValueSource(strings = {
            "/fxml/main_shell.fxml",
            "/fxml/dashboard_view.fxml",
            "/fxml/parking_map_view.fxml",
            "/fxml/vehicle_entry_view.fxml",
            "/fxml/vehicle_exit_view.fxml",
            "/fxml/find_vehicle_view.fxml",
            "/fxml/smart_allocation_view.fxml",
            "/fxml/reservations_view.fxml",
            "/fxml/ev_charging_view.fxml",
            "/fxml/anomalies_view.fxml",
            "/fxml/parking_history_view.fxml",
            "/fxml/analytics_view.fxml",
            "/fxml/admin_settings_view.fxml",
            "/fxml/login_dialog.fxml"
    })
    void testFxmlScreenLoadsWithoutErrors(String fxmlPath) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        final Throwable[] error = new Throwable[1];
        final Object[] controllerRef = new Object[1];

        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                Parent root = loader.load();
                assertNotNull(root, "Root element should not be null for " + fxmlPath);
                Object controller = loader.getController();
                assertNotNull(controller, "Controller must be instantiated and bound for " + fxmlPath);
                controllerRef[0] = controller;
            } catch (Throwable t) {
                error[0] = t;
            } finally {
                latch.countDown();
            }
        });

        boolean completed = latch.await(10, TimeUnit.SECONDS);
        assertTrue(completed, "FXML loading timed out on JavaFX application thread for " + fxmlPath);
        if (error[0] != null) {
            fail("Failed to load and bind FXML view " + fxmlPath + ": " + error[0].getMessage(), error[0]);
        }
        assertNotNull(controllerRef[0], "Controller was not bound for " + fxmlPath);
    }
}
