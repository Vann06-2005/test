package controllers;

import db.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import models.Route;

public class RouteController {

    private static RouteController instance;

    private RouteController() {}

    public static RouteController getInstance() {
        if (instance == null) instance = new RouteController();
        return instance;
    }

    public boolean addRoute(Route route) {
        // Prevent duplicate routes with the same source/destination pair (case-insensitive)
        if (routeExists(route.getSourceCity(), route.getDestinationCity())) {
            System.out.println("Route already exists: " + route.getSourceCity() + " -> " + route.getDestinationCity());
            return false;
        }

        String sql = "INSERT INTO routes (source_city, destination_city, distance_km, estimated_duration) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, route.getSourceCity());
            stmt.setString(2, route.getDestinationCity());
            stmt.setDouble(3, route.getDistanceKm());
            stmt.setString(4, route.getEstimatedDuration());

            stmt.executeUpdate();
            System.out.println("Route added: " + route.getSourceCity() + " -> " + route.getDestinationCity());
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean routeExists(String source, String destination) {
        return routeExists(source, destination, null);
    }

    public boolean routeExists(String source, String destination, Long excludeRouteId) {
        StringBuilder sql = new StringBuilder(
                "SELECT 1 FROM routes WHERE LOWER(source_city) = LOWER(?) AND LOWER(destination_city) = LOWER(?)");
        if (excludeRouteId != null) {
            sql.append(" AND id <> ?");
        }
        sql.append(" LIMIT 1");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            stmt.setString(1, source);
            stmt.setString(2, destination);
            if (excludeRouteId != null) {
                stmt.setLong(3, excludeRouteId);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Route> getAllRoutes() {
        List<Route> routes = new ArrayList<>();
        String sql = "SELECT * FROM routes";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                routes.add(new Route(
                    rs.getLong("id"),
                    rs.getString("source_city"),
                    rs.getString("destination_city"),
                    rs.getDouble("distance_km"),
                    rs.getString("estimated_duration")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return routes;
    }

    public Route getRouteById(Long id) {
        String sql = "SELECT * FROM routes WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new Route(
                    rs.getLong("id"),
                    rs.getString("source_city"),
                    rs.getString("destination_city"),
                    rs.getDouble("distance_km"),
                    rs.getString("estimated_duration")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean updateRoute(Route route) {
        if (route.getId() == null) {
            System.err.println("Route ID is required to update a record.");
            return false;
        }

        String sql = "UPDATE routes SET source_city = ?, destination_city = ?, distance_km = ?, estimated_duration = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, route.getSourceCity());
            stmt.setString(2, route.getDestinationCity());
            stmt.setDouble(3, route.getDistanceKm());
            stmt.setString(4, route.getEstimatedDuration());
            stmt.setLong(5, route.getId());

            int rowsUpdated = stmt.executeUpdate();
            System.out.println("Route updated: " + route.getSourceCity() + " -> " + route.getDestinationCity());
            return rowsUpdated > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteRoute(Long routeId) {
        String sql = "DELETE FROM routes WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, routeId);
            int rowsDeleted = stmt.executeUpdate();
            System.out.println("Route deleted with ID: " + routeId);
            return rowsDeleted > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
