package com.smartpark.service;

import com.smartpark.dao.AdminDAO;
import com.smartpark.dao.AuditDAO;
import com.smartpark.model.Admin;
import com.smartpark.model.AuditEvent;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Service managing user authentication, SHA-256 password hashing, and active session state.
 */
public class AuthenticationService {

    private static AuthenticationService instance;

    private final AdminDAO adminDAO = new AdminDAO();
    private final AuditDAO auditDAO = new AuditDAO();
    private Admin currentAdmin = null;

    private AuthenticationService() {}

    public static synchronized AuthenticationService getInstance() {
        if (instance == null) {
            instance = new AuthenticationService();
        }
        return instance;
    }

    public boolean login(String username, String rawPassword) {
        if (username == null || rawPassword == null) return false;
        Admin admin = adminDAO.findByUsername(username.trim());
        if (admin == null) return false;

        String hashedInput = hashPassword(rawPassword);
        // Compare with stored hash
        if (hashedInput.equalsIgnoreCase(admin.getPasswordHash()) || rawPassword.equals(admin.getPasswordHash())) {
            this.currentAdmin = admin;
            auditDAO.logEvent(new AuditEvent("AUTH_LOGIN", admin.getUsername(),
                    "Admin user " + admin.getFullName() + " logged in successfully.", admin.getUsername()));
            return true;
        }
        return false;
    }

    public void logout() {
        if (currentAdmin != null) {
            auditDAO.logEvent(new AuditEvent("AUTH_LOGOUT", currentAdmin.getUsername(),
                    "Admin user " + currentAdmin.getUsername() + " logged out.", currentAdmin.getUsername()));
            currentAdmin = null;
        }
    }

    public boolean isLoggedIn() {
        return currentAdmin != null;
    }

    public Admin getCurrentAdmin() {
        return currentAdmin;
    }

    public static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }
}
