package controllers;

import db.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import models.Bus;
public class BusController {

    private static BusController instance;

    private BusController() {}

    public static BusController getInstance() {
        if (instance == null) instance = new BusController();
        return instance;
    }

    public boolean addBus(Bus bus) {
        String sql = "INSERT INTO buses (bus_number, total_seats, type, is_operational) VALUES (?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, bus.getBusNumber());
            stmt.setInt(2, bus.getTotalSeats());
            stmt.setString(3, bus.getType());
            stmt.setBoolean(4, bus.isOperational());
            
            int rows = stmt.executeUpdate();
            System.out.println("Bus added: " + bus.getBusNumber());
            return rows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Bus> getAllBuses() {
        List<Bus> buses = new ArrayList<>();
        String sql = "SELECT * FROM buses";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                Bus bus = new Bus(
                    rs.getLong("id"),
                    rs.getString("bus_number"),
                    rs.getInt("total_seats"),
                    rs.getString("type")
                );
                bus.setOperational(rs.getBoolean("is_operational"));
                buses.add(bus);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return buses;
    }

    public boolean updateBus(Bus bus) {
        if (bus.getId() == null) {
            System.err.println("Bus ID is required to update a record.");
            return false;
        }

        String sql = "UPDATE buses SET bus_number = ?, total_seats = ?, type = ?, is_operational = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, bus.getBusNumber());
            stmt.setInt(2, bus.getTotalSeats());
            stmt.setString(3, bus.getType());
            stmt.setBoolean(4, bus.isOperational());
            stmt.setLong(5, bus.getId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteBus(Long busId) {
        String selectRoutesSql = "SELECT DISTINCT route_id FROM schedules WHERE bus_id = ?";
        String deleteSchedulesSql = "DELETE FROM schedules WHERE bus_id = ?";
        String deleteRoutesSql = "DELETE FROM routes WHERE id = ? AND NOT EXISTS (SELECT 1 FROM schedules WHERE route_id = ?)";
        String deleteBusSql = "DELETE FROM buses WHERE id = ?";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            if (conn == null) {
                System.err.println("No DB connection; cannot delete bus.");
                return false;
            }
            conn.setAutoCommit(false);

            // Capture routes tied to this bus's schedules (so we can clean up orphan routes later)
            List<Long> routeIds = new ArrayList<>();
            try (PreparedStatement routeStmt = conn.prepareStatement(selectRoutesSql)) {
                routeStmt.setLong(1, busId);
                try (ResultSet rs = routeStmt.executeQuery()) {
                    while (rs.next()) {
                        routeIds.add(rs.getLong("route_id"));
                    }
                }
            }

            // Remove schedules for this bus
            try (PreparedStatement schedStmt = conn.prepareStatement(deleteSchedulesSql)) {
                schedStmt.setLong(1, busId);
                schedStmt.executeUpdate();
            }

            // Delete the bus itself
            int busRows;
            try (PreparedStatement busStmt = conn.prepareStatement(deleteBusSql)) {
                busStmt.setLong(1, busId);
                busRows = busStmt.executeUpdate();
            }

            // Remove routes that became orphaned (no schedules left)
            if (!routeIds.isEmpty()) {
                try (PreparedStatement routeDeleteStmt = conn.prepareStatement(deleteRoutesSql)) {
                    for (Long routeId : routeIds) {
                        routeDeleteStmt.setLong(1, routeId);
                        routeDeleteStmt.setLong(2, routeId);
                        routeDeleteStmt.addBatch();
                    }
                    routeDeleteStmt.executeBatch();
                }
            }

            if (conn != null) conn.commit();
            return busRows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ignored) {}
            }
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ignored) {}
            }
        }
    }

    public Bus findBusByNumber(String busNumber) {
        String sql = "SELECT * FROM buses WHERE bus_number = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, busNumber);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                Bus bus = new Bus(
                    rs.getLong("id"),
                    rs.getString("bus_number"),
                    rs.getInt("total_seats"),
                    rs.getString("type")
                );
                bus.setOperational(rs.getBoolean("is_operational"));
                return bus;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
