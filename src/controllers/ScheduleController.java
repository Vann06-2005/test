package controllers;

import db.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import models.*;

public class ScheduleController {

    private static ScheduleController instance;

    private ScheduleController() {
    };

    public static ScheduleController getInstance() {
        if (instance == null)
            instance = new ScheduleController();
        return instance;
    }

    public void addSchedule(Schedule schedule) {
        String sql = "INSERT INTO schedules (bus_id, route_id, departure_time, arrival_time, ticket_price, available_seats) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, schedule.getBus().getId());
            stmt.setLong(2, schedule.getRoute().getId());
            stmt.setTimestamp(3, Timestamp.valueOf(schedule.getDepartureTime()));
            stmt.setTimestamp(4, Timestamp.valueOf(schedule.getArrivalTime()));
            stmt.setBigDecimal(5, schedule.getTicketPrice());
            stmt.setInt(6, schedule.getAvailableSeats());

            stmt.executeUpdate();
            System.out.println("Schedule added for bus: " + schedule.getBus().getBusNumber());

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Schedule> getAllSchedules() {
        return fetchSchedules("SELECT s.*, b.bus_number, b.total_seats, b.type, b.is_operational, " +
                "r.source_city, r.destination_city, r.distance_km, r.estimated_duration " +
                "FROM schedules s " +
                "JOIN buses b ON s.bus_id = b.id " +
                "JOIN routes r ON s.route_id = r.id");
    }

    public List<Schedule> searchTrips(String from, String to) {
        String sql = "SELECT s.*, b.bus_number, b.total_seats, b.type, b.is_operational, " +
                "r.source_city, r.destination_city, r.distance_km, r.estimated_duration " +
                "FROM schedules s " +
                "JOIN buses b ON s.bus_id = b.id " +
                "JOIN routes r ON s.route_id = r.id " +
                "WHERE LOWER(r.source_city) = LOWER(?) AND LOWER(r.destination_city) = LOWER(?)";

        List<Schedule> results = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, from);
            stmt.setString(2, to);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(mapResultSetToSchedule(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return results;
    }

    // Helper method to execute query and map results
    private List<Schedule> fetchSchedules(String sql) {
        List<Schedule> schedules = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                schedules.add(mapResultSetToSchedule(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return schedules;
    }

    // Helper to construct object from joined query
    private Schedule mapResultSetToSchedule(ResultSet rs) throws SQLException {
        Bus bus = new Bus(
                rs.getLong("bus_id"),
                rs.getString("bus_number"),
                rs.getInt("total_seats"),
                rs.getString("type"));
        bus.setOperational(rs.getBoolean("is_operational"));

        Route route = new Route(
                rs.getLong("route_id"),
                rs.getString("source_city"),
                rs.getString("destination_city"),
                rs.getDouble("distance_km"),
                rs.getString("estimated_duration"));

        Schedule schedule = new Schedule(
                rs.getLong("id"),
                bus,
                route,
                rs.getTimestamp("departure_time").toLocalDateTime(),
                rs.getTimestamp("arrival_time").toLocalDateTime(),
                rs.getBigDecimal("ticket_price"));
        schedule.setAvailableSeats(rs.getInt("available_seats"));

        return schedule;
    }

    public Schedule getScheduleById(Long scheduleId) {
        String sql = "SELECT s.*, b.bus_number, b.total_seats, b.type, b.is_operational, " +
                "r.source_city, r.destination_city, r.distance_km, r.estimated_duration " +
                "FROM schedules s " +
                "JOIN buses b ON s.bus_id = b.id " +
                "JOIN routes r ON s.route_id = r.id " +
                "WHERE s.id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, scheduleId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToSchedule(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Schedule> getSchedulesByRoute(Long routeId) {
        String sql = "SELECT s.*, b.bus_number, b.total_seats, b.type, b.is_operational, " +
                "r.source_city, r.destination_city, r.distance_km, r.estimated_duration " +
                "FROM schedules s " +
                "JOIN buses b ON s.bus_id = b.id " +
                "JOIN routes r ON s.route_id = r.id " +
                "WHERE r.id = ?";

        List<Schedule> schedules = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, routeId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    schedules.add(mapResultSetToSchedule(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return schedules;
    }

    // New methods for route deletion handling
    public int countSchedulesByRoute(Long routeId) {
        String sql = "SELECT COUNT(*) AS cnt FROM schedules WHERE route_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, routeId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("cnt");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    // Reassign schedules from one route to another
    public int reassignSchedulesToRoute(Long fromRouteId, Long toRouteId) {
        String sql = "UPDATE schedules SET route_id = ? WHERE route_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, toRouteId);
            stmt.setLong(2, fromRouteId);
            return stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    // Delete schedules tied to a specific route
    public int deleteSchedulesByRoute(Long routeId) {
        String sql = "DELETE FROM schedules WHERE route_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, routeId);
            return stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public boolean updateSchedule(Schedule schedule) {
        if (schedule.getId() == null) {
            System.err.println("Schedule ID is required to update a record.");
            return false;
        }

        String sql = "UPDATE schedules SET bus_id = ?, route_id = ?, departure_time = ?, " +
                "arrival_time = ?, ticket_price = ?, available_seats = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, schedule.getBus().getId());
            stmt.setLong(2, schedule.getRoute().getId());
            stmt.setTimestamp(3, Timestamp.valueOf(schedule.getDepartureTime()));
            stmt.setTimestamp(4, Timestamp.valueOf(schedule.getArrivalTime()));
            stmt.setBigDecimal(5, schedule.getTicketPrice());
            stmt.setInt(6, schedule.getAvailableSeats());
            stmt.setLong(7, schedule.getId());

            int rowsUpdated = stmt.executeUpdate();
            System.out.println("Schedule updated with ID: " + schedule.getId());
            return rowsUpdated > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteSchedule(Long scheduleId) {
        String sql = "DELETE FROM schedules WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, scheduleId);
            int rowsDeleted = stmt.executeUpdate();
            System.out.println("Schedule deleted with ID: " + scheduleId);
            return rowsDeleted > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
