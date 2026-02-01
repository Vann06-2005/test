package controllers;

import db.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import models.*;

public class BookingController {

    private static BookingController instance; // Singleton Pattern

    private BookingController() {
    }

    public static BookingController getInstance() {
        if (instance == null)
            instance = new BookingController();
        return instance;
    }

    // Count bookings tied to any schedule on a given route
    public int countBookingsByRoute(Long routeId) {
        String sql = "SELECT COUNT(*) AS cnt FROM bookings WHERE schedule_id IN (SELECT id FROM schedules WHERE route_id = ?)";

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

    // Remove bookings tied to any schedule on a given route
    public int deleteBookingsByRoute(Long routeId) {
        String sql = "DELETE FROM bookings WHERE schedule_id IN (SELECT id FROM schedules WHERE route_id = ?)";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, routeId);
            return stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public boolean createBooking(Booking booking) {
        if (booking == null || booking.getSchedule() == null || booking.getSchedule().getDepartureTime() == null) {
            System.out.println("Booking failed: schedule info missing.");
            return false;
        }
        LocalDateTime departureTime = booking.getSchedule().getDepartureTime();
        if (departureTime.isBefore(LocalDateTime.now())) {
            System.out.println("Booking rejected: schedule already departed.");
            return false;
        }

        String insertBookingSql = "INSERT INTO bookings (user_id, schedule_id, seat_number, status, total_amount, booking_date) VALUES (?, ?, ?, ?, ?, ?)";
        String updateScheduleSql = "UPDATE schedules SET available_seats = available_seats - 1 WHERE id = ? AND available_seats > 0";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // Start Transaction

            // 1. Try to decrement seat count (atomically ensures we don't overbook)
            try (PreparedStatement updateStmt = conn.prepareStatement(updateScheduleSql)) {
                updateStmt.setLong(1, booking.getSchedule().getId());
                int rowsUpdated = updateStmt.executeUpdate();

                if (rowsUpdated > 0) {
                    // Seats were available, proceed to book
                    try (PreparedStatement insertStmt = conn.prepareStatement(insertBookingSql)) {
                        insertStmt.setLong(1, booking.getCustomer().getId());
                        insertStmt.setLong(2, booking.getSchedule().getId());
                        insertStmt.setString(3, booking.getSeatNumber());
                        insertStmt.setString(4, booking.getStatus());
                        insertStmt.setBigDecimal(5, booking.getTotalAmount());
                        insertStmt.setTimestamp(6, Timestamp.valueOf(booking.getBookingDate()));
                        insertStmt.executeUpdate();
                    }

                    conn.commit(); // Commit Transaction
                    System.out.println("Booking successful for: " + booking.getCustomer().getFullName());
                    return true;
                } else {
                    System.out.println("Booking failed: No seats available.");
                    conn.rollback();
                    return false;
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public List<String> getTakenSeats(Long scheduleId) {
        List<String> takenSeats = new ArrayList<>();
        String sql = "SELECT seat_number FROM bookings WHERE schedule_id = ? AND status = 'CONFIRMED'";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, scheduleId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    takenSeats.add(rs.getString("seat_number"));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return takenSeats;
    }

    public List<Booking> getBookingsForUser(Long userId) {
        List<Booking> userBookings = new ArrayList<>();
        // Join Tables to reconstruct the full context (Booking -> Schedule ->
        // Route/Bus)
        String sql = "SELECT bk.*, " +
                "s.departure_time, s.arrival_time, s.ticket_price, " +
                "b.id as bus_id, b.bus_number, b.total_seats, b.type as bus_type, " +
                "r.id as route_id, r.source_city, r.destination_city, r.distance_km, r.estimated_duration, " +
                "u.full_name, u.role, u.password " +
                "FROM bookings bk " +
                "JOIN schedules s ON bk.schedule_id = s.id " +
                "JOIN buses b ON s.bus_id = b.id " +
                "JOIN routes r ON s.route_id = r.id " +
                "JOIN users u ON bk.user_id = u.id " +
                "WHERE bk.user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    // Reconstruct Objects
                    User user = new User(rs.getLong("user_id"), rs.getString("full_name"), rs.getString("password"),
                            rs.getString("role"));
                    // Reconstruct Objects
                    Bus bus = new Bus(rs.getLong("bus_id"), rs.getString("bus_number"), rs.getInt("total_seats"),
                            rs.getString("bus_type"));
                    // Reconstruct Objects
                    Route route = new Route(rs.getLong("route_id"), rs.getString("source_city"),
                            rs.getString("destination_city"), rs.getDouble("distance_km"),
                            rs.getString("estimated_duration"));

                    Schedule schedule = new Schedule(rs.getLong("schedule_id"), bus, route,
                            rs.getTimestamp("departure_time").toLocalDateTime(),
                            rs.getTimestamp("arrival_time").toLocalDateTime(), rs.getBigDecimal("ticket_price"));

                    Booking booking = new Booking(
                            rs.getLong("id"), user, schedule,
                            rs.getString("seat_number"),
                            rs.getBigDecimal("total_amount"));

                    booking.setStatus(rs.getString("status"));
                    booking.setBookingDate(rs.getTimestamp("booking_date").toLocalDateTime());

                    userBookings.add(booking);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return userBookings;
    }

    public boolean cancelBooking(Long bookingId, Long userId) {
        String selectSql = "SELECT schedule_id FROM bookings WHERE id = ? AND user_id = ? AND status = 'CONFIRMED' FOR UPDATE";
        String updateBookingSql = "UPDATE bookings SET status = 'CANCELLED' WHERE id = ? AND user_id = ? AND status = 'CONFIRMED'";
        String updateScheduleSql = "UPDATE schedules SET available_seats = available_seats + 1 WHERE id = ?";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            if (conn == null) {
                return false;
            }
            conn.setAutoCommit(false);

            Long scheduleId = null;
            try (PreparedStatement stmt = conn.prepareStatement(selectSql)) {
                stmt.setLong(1, bookingId);
                stmt.setLong(2, userId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        scheduleId = rs.getLong("schedule_id");
                    }
                }
            }

            if (scheduleId == null) {
                conn.rollback();
                return false; // Nothing to cancel (not found or already cancelled)
            }

            try (PreparedStatement stmt = conn.prepareStatement(updateBookingSql)) {
                stmt.setLong(1, bookingId);
                stmt.setLong(2, userId);
                int affected = stmt.executeUpdate();
                if (affected == 0) {
                    conn.rollback();
                    return false;
                }
            }

            try (PreparedStatement stmt = conn.prepareStatement(updateScheduleSql)) {
                stmt.setLong(1, scheduleId);
                stmt.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public boolean deleteCancelledBooking(Long bookingId, Long userId) {
        String deleteSql = "DELETE FROM bookings WHERE id = ? AND user_id = ? AND status = 'CANCELLED'";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(deleteSql)) {

            stmt.setLong(1, bookingId);
            stmt.setLong(2, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Booking> getAllBookings() {
        List<Booking> bookings = new ArrayList<>();
        String sql = "SELECT bk.*, " +
                "s.departure_time, s.arrival_time, s.ticket_price, " +
                "b.id as bus_id, b.bus_number, b.total_seats, b.type as bus_type, " +
                "r.id as route_id, r.source_city, r.destination_city, r.distance_km, r.estimated_duration, " +
                "u.full_name, u.role, u.password " +
                "FROM bookings bk " +
                "JOIN schedules s ON bk.schedule_id = s.id " +
                "JOIN buses b ON s.bus_id = b.id " +
                "JOIN routes r ON s.route_id = r.id " +
                "JOIN users u ON bk.user_id = u.id";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                User user = new User(rs.getLong("user_id"), rs.getString("full_name"), rs.getString("password"),
                        rs.getString("role"));

                Bus bus = new Bus(rs.getLong("bus_id"), rs.getString("bus_number"), rs.getInt("total_seats"),
                        rs.getString("bus_type"));

                Route route = new Route(rs.getLong("route_id"), rs.getString("source_city"),
                        rs.getString("destination_city"), rs.getDouble("distance_km"),
                        rs.getString("estimated_duration"));

                Schedule schedule = new Schedule(rs.getLong("schedule_id"), bus, route,
                        rs.getTimestamp("departure_time").toLocalDateTime(),
                        rs.getTimestamp("arrival_time").toLocalDateTime(), rs.getBigDecimal("ticket_price"));

                Booking booking = new Booking(
                        rs.getLong("id"), user, schedule,
                        rs.getString("seat_number"),
                        rs.getBigDecimal("total_amount"));

                booking.setStatus(rs.getString("status"));
                booking.setBookingDate(rs.getTimestamp("booking_date").toLocalDateTime());

                bookings.add(booking);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return bookings;
    }

    public Booking getBookingById(Long bookingId) {
        String sql = "SELECT bk.*, " +
                "s.departure_time, s.arrival_time, s.ticket_price, " +
                "b.id as bus_id, b.bus_number, b.total_seats, b.type as bus_type, " +
                "r.id as route_id, r.source_city, r.destination_city, r.distance_km, r.estimated_duration, " +
                "u.full_name, u.role, u.password " +
                "FROM bookings bk " +
                "JOIN schedules s ON bk.schedule_id = s.id " +
                "JOIN buses b ON s.bus_id = b.id " +
                "JOIN routes r ON s.route_id = r.id " +
                "JOIN users u ON bk.user_id = u.id " +
                "WHERE bk.id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, bookingId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    User user = new User(rs.getLong("user_id"), rs.getString("full_name"), rs.getString("password"),
                            rs.getString("role"));

                    Bus bus = new Bus(rs.getLong("bus_id"), rs.getString("bus_number"), rs.getInt("total_seats"),
                            rs.getString("bus_type"));

                    Route route = new Route(rs.getLong("route_id"), rs.getString("source_city"),
                            rs.getString("destination_city"), rs.getDouble("distance_km"),
                            rs.getString("estimated_duration"));

                    Schedule schedule = new Schedule(rs.getLong("schedule_id"), bus, route,
                            rs.getTimestamp("departure_time").toLocalDateTime(),
                            rs.getTimestamp("arrival_time").toLocalDateTime(), rs.getBigDecimal("ticket_price"));

                    Booking booking = new Booking(
                            rs.getLong("id"), user, schedule,
                            rs.getString("seat_number"),
                            rs.getBigDecimal("total_amount"));

                    booking.setStatus(rs.getString("status"));
                    booking.setBookingDate(rs.getTimestamp("booking_date").toLocalDateTime());

                    return booking;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean cancelBooking(Long bookingId) {
        String updateBookingSql = "UPDATE bookings SET status = 'CANCELLED' WHERE id = ?";
        String getScheduleIdSql = "SELECT schedule_id FROM bookings WHERE id = ?";
        String updateScheduleSql = "UPDATE schedules SET available_seats = available_seats + 1 WHERE id = ?";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            if (conn == null) {
                System.err.println("No DB connection; cannot cancel booking.");
                return false;
            }
            conn.setAutoCommit(false);

            Long scheduleId = null;
            try (PreparedStatement stmt = conn.prepareStatement(getScheduleIdSql)) {
                stmt.setLong(1, bookingId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        scheduleId = rs.getLong("schedule_id");
                    }
                }
            }

            if (scheduleId == null) {
                conn.rollback();
                return false;
            }

            try (PreparedStatement stmt = conn.prepareStatement(updateBookingSql)) {
                stmt.setLong(1, bookingId);
                stmt.executeUpdate();
            }

            try (PreparedStatement stmt = conn.prepareStatement(updateScheduleSql)) {
                stmt.setLong(1, scheduleId);
                stmt.executeUpdate();
            }

            conn.commit();
            System.out.println("Booking cancelled with ID: " + bookingId);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
