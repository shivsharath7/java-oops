package com.smartpark.controller;

import com.smartpark.service.AuthenticationService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginDialogController {

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblLoginError;

    private boolean loginSuccessful = false;

    @FXML
    public void handleLogin() {
        lblLoginError.setText("");
        String u = txtUsername.getText();
        String p = txtPassword.getText();

        if (u == null || u.isBlank() || p == null || p.isBlank()) {
            lblLoginError.setText("Please enter both username and password.");
            return;
        }

        boolean ok = AuthenticationService.getInstance().login(u, p);
        if (ok) {
            loginSuccessful = true;
            Stage stage = (Stage) txtUsername.getScene().getWindow();
            stage.close();
        } else {
            lblLoginError.setText("Invalid credentials. Try admin / admin123");
        }
    }

    public boolean isLoginSuccessful() {
        return loginSuccessful;
    }
}
