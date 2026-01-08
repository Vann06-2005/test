package controllers;

import db.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import models.User;

public class UserController {
    //Singleton Pattern
    private static UserController instance;

    private UserController() {
        // No need to initialize an ArrayList here
    }

    public static UserController getInstance() {
        if (instance == null) {
            instance = new UserController();
        }
        return instance;
    }

    // Business Logic with Database
    //Register part
    public boolean register(String username, String password, String role) {
        String checkSql = "SELECT id FROM users WHERE full_name = ?";
        String insertSql = "INSERT INTO users (full_name, password, role) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection()) {
            // 1. Check if user exists
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, username);
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next()) {
                    return false; // User already exists
                }
            }

            // 2. Insert new user
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                insertStmt.setString(1, username);
                insertStmt.setString(2, password);
                insertStmt.setString(3, role);
                insertStmt.executeUpdate();
                System.out.println("User registered: " + username);
                return true;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    //login part
    public User login(String username, String password) {
        String sql = "SELECT * FROM users WHERE full_name = ? AND password = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, username);
            stmt.setString(2, password);
            
            ResultSet rs = stmt.executeQuery(); // for select query  \\\\ 
            
            if (rs.next()) {
                // Map DB row to User object
                return new User(
                    rs.getLong("id"),
                    rs.getString("full_name"),
                    rs.getString("password"),
                    rs.getString("role")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null; // Login failed
    }
}