package controllers;

import db.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import models.*;

public class BookingController {

    private static BookingController instance;

    private BookingController() {
    }

    public static BookingController getInstance() {
        if (instance == null)
            instance = new BookingController();
        return instance;
    }

    public boolean createBooking(Booking booking) {
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

                    userBookings.add(booking);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return userBookings;
    }
}