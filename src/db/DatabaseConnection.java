package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    // Update these with your specific database details
    private static final String URL = "jdbc:postgresql://localhost:5432/Bus_DB";
    private static final String USER = "admin";
    private static final String PASSWORD = "mypassword";

    public static Connection getConnection() {
        Connection conn = null;
        try {
            // Ensure the PostgreSQL driver is loaded
            Class.forName("org.postgresql.Driver");
            conn = DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (ClassNotFoundException e) {
            System.err.println("PostgreSQL JDBC Driver not found. Include the JAR in your library.");
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("Connection failed!");
            e.printStackTrace();
        }
        return conn;
    }
}
