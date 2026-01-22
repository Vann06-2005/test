package views.admin;

import controllers.BookingController;
import controllers.RouteController;
import controllers.ScheduleController;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import models.Route;

public class AddRoutePanel extends JPanel {
    private static final String[] CAMBODIA_PROVINCES = {
            "Banteay Meanchey",
            "Battambang",
            "Kampong Cham",
            "Kampong Chhnang",
            "Kampong Speu",
            "Kampong Thom",
            "Kampot",
            "Kandal",
            "Kep",
            "Koh Kong",
            "Kratie",
            "Mondulkiri",
            "Oddar Meanchey",
            "Pailin",
            "Phnom Penh",
            "Preah Vihear",
            "Prey Veng",
            "Pursat",
            "Ratanakiri",
            "Siem Reap",
            "Sihanoukville",
            "Stung Treng",
            "Svay Rieng",
            "Takeo",
            "Tboung Khmum"
    };

    private JComboBox<String> srcField, dstField;
    private JTextField distField, durField;
    private final JTable routeTable;
    private final DefaultTableModel tableModel;
    private JButton addBtn, updateBtn, deleteBtn, refreshBtn, clearBtn;
    private List<Route> routes = new ArrayList<>();
    private Route selectedRoute;

    public AddRoutePanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(Color.WHITE);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(new JLabel("Source City:"), gbc);
        srcField = new JComboBox<>(CAMBODIA_PROVINCES);
        srcField.setSelectedIndex(-1);
        gbc.gridx = 1;
        formPanel.add(srcField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        formPanel.add(new JLabel("Destination City:"), gbc);
        dstField = new JComboBox<>(CAMBODIA_PROVINCES);
        dstField.setSelectedIndex(-1);
        gbc.gridx = 1;
        formPanel.add(dstField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        formPanel.add(new JLabel("Distance (km):"), gbc);
        distField = new JTextField();
        gbc.gridx = 1;
        formPanel.add(distField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        formPanel.add(new JLabel("Duration (e.g., 4h 30m):"), gbc);
        durField = new JTextField();
        gbc.gridx = 1;
        formPanel.add(durField, gbc);

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        buttonRow.setOpaque(false);
        addBtn = new JButton("Add Route");
        updateBtn = new JButton("Update");
        deleteBtn = new JButton("Delete");
        clearBtn = new JButton("Clear");
        refreshBtn = new JButton("Refresh");
        updateBtn.setEnabled(false);
        deleteBtn.setEnabled(false);
        buttonRow.add(addBtn);
        buttonRow.add(updateBtn);
        buttonRow.add(deleteBtn);
        buttonRow.add(clearBtn);
        buttonRow.add(refreshBtn);
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        formPanel.add(buttonRow, gbc);
        add(formPanel, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(new Object[] { "ID", "Source", "Destination", "Distance (km)", "Duration" },
                0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        routeTable = new JTable(tableModel);
        routeTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        routeTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                syncSelectionFromTable();
            }
        });
        JScrollPane scrollPane = new JScrollPane(routeTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(230, 230, 230)));
        add(scrollPane, BorderLayout.CENTER);

        addBtn.addActionListener(e -> handleAdd());
        updateBtn.addActionListener(e -> handleUpdate());
        deleteBtn.addActionListener(e -> handleDelete());
        clearBtn.addActionListener(e -> clearSelection());
        refreshBtn.addActionListener(e -> loadRoutes());

        loadRoutes();
    }

    private void handleAdd() {
        try {
            String src = (String) srcField.getSelectedItem();
            String dst = (String) dstField.getSelectedItem();
            double dist = Double.parseDouble(distField.getText().trim());
            String dur = durField.getText().trim();

            if (src == null || dst == null || dur.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill all fields.");
                return;
            }

            Route r = new Route(null, src, dst, dist, dur);
            new SwingWorker<Boolean, Void>() {
                private boolean duplicate;

                @Override
                protected Boolean doInBackground() {
                    RouteController controller = RouteController.getInstance();
                    duplicate = controller.routeExists(src, dst);
                    if (duplicate)
                        return false;
                    return controller.addRoute(r);
                }

                @Override
                protected void done() {
                    try {
                        boolean added = get();
                        if (duplicate) {
                            JOptionPane.showMessageDialog(AddRoutePanel.this,
                                    "This route already exists.",
                                    "Duplicate Route",
                                    JOptionPane.WARNING_MESSAGE);
                            return;
                        }

                        if (added) {
                            JOptionPane.showMessageDialog(AddRoutePanel.this, "Route Added!");
                            clearSelection();
                            loadRoutes();
                        } else {
                            JOptionPane.showMessageDialog(AddRoutePanel.this, "Unable to add route. Please try again.");
                        }
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(AddRoutePanel.this, "Failed to add route: " + ex.getMessage());
                    }
                }
            }.execute();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid distance. Please enter a number.");
        }
    }

    private void handleUpdate() {
        if (selectedRoute == null) {
            JOptionPane.showMessageDialog(this, "Select a route to update.");
            return;
        }

        try {
            String src = (String) srcField.getSelectedItem();
            String dst = (String) dstField.getSelectedItem();
            double dist = Double.parseDouble(distField.getText().trim());
            String dur = durField.getText().trim();

            if (src == null || dst == null || dur.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill all fields.");
                return;
            }

            Long routeId = selectedRoute.getId();
            if (routeId == null) {
                JOptionPane.showMessageDialog(this, "Selected route has no ID.");
                return;
            }

            Route updated = new Route(routeId, src, dst, dist, dur);

            new SwingWorker<Boolean, Void>() {
                private boolean duplicate;

                @Override
                protected Boolean doInBackground() {
                    RouteController controller = RouteController.getInstance();
                    duplicate = controller.routeExists(src, dst, routeId);
                    if (duplicate)
                        return false;
                    return controller.updateRoute(updated);
                }

                @Override
                protected void done() {
                    try {
                        boolean updatedOk = get();
                        if (duplicate) {
                            JOptionPane.showMessageDialog(AddRoutePanel.this,
                                    "Another route with the same source and destination already exists.",
                                    "Duplicate Route",
                                    JOptionPane.WARNING_MESSAGE);
                            return;
                        }

                        if (updatedOk) {
                            JOptionPane.showMessageDialog(AddRoutePanel.this, "Route updated.");
                            clearSelection();
                            loadRoutes();
                        } else {
                            JOptionPane.showMessageDialog(AddRoutePanel.this, "Update failed. Please try again.");
                        }
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(AddRoutePanel.this, "Failed to update route: " + ex.getMessage());
                    }
                }
            }.execute();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid distance. Please enter a number.");
        }
    }

    private void handleDelete() {
        if (selectedRoute == null) {
            JOptionPane.showMessageDialog(this, "Select a route to delete.");
            return;
        }

        if (selectedRoute.getId() == null) {
            JOptionPane.showMessageDialog(this, "Selected route has no ID.");
            return;
        }

        final int scheduleCount = ScheduleController.getInstance().countSchedulesByRoute(selectedRoute.getId());
        if (scheduleCount < 0) {
            JOptionPane.showMessageDialog(this, "Unable to check schedules for this route. Please try again.");
            return;
        }

        final int bookingCount = BookingController.getInstance().countBookingsByRoute(selectedRoute.getId());
        if (bookingCount < 0) {
            JOptionPane.showMessageDialog(this, "Unable to check bookings for this route. Please try again.");
            return;
        }

        final Long routeId = selectedRoute.getId();
        final String routeLabel = selectedRoute.getSourceCity() + " -> " + selectedRoute.getDestinationCity();
        StringBuilder messageBuilder = new StringBuilder("Delete route " + routeLabel);
        if (scheduleCount > 0 || bookingCount > 0) {
            messageBuilder.append(" and remove ");
            if (scheduleCount > 0) {
                messageBuilder.append(scheduleCount).append(" schedule(s)");
            }
            if (bookingCount > 0) {
                if (scheduleCount > 0) {
                    messageBuilder.append(" and ");
                }
                messageBuilder.append(bookingCount).append(" booking(s)");
            }
            messageBuilder.append(" linked to it?");
            if (scheduleCount > 0) {
                messageBuilder.append("\nAll schedules from ").append(routeLabel).append(" will be removed.");
            }
            if (bookingCount > 0) {
                messageBuilder.append("\nAll bookings on those schedules will be removed.");
            }
        } else {
            messageBuilder.append("?");
        }
        String message = messageBuilder.toString();
        int messageType = (scheduleCount > 0 || bookingCount > 0)
                ? JOptionPane.WARNING_MESSAGE
                : JOptionPane.QUESTION_MESSAGE;

        int choice = JOptionPane.showConfirmDialog(this, message, "Confirm Delete",
                JOptionPane.YES_NO_OPTION, messageType);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        new SwingWorker<Boolean, Void>() {
            private int deletedSchedules;
            private int deletedBookings;
            private boolean scheduleDeleteFailed;
            private boolean bookingDeleteFailed;

            @Override
            protected Boolean doInBackground() {
                if (bookingCount > 0) {
                    deletedBookings = BookingController.getInstance().deleteBookingsByRoute(routeId);
                    if (deletedBookings < 0) {
                        bookingDeleteFailed = true;
                        return false;
                    }
                }
                if (scheduleCount > 0) {
                    deletedSchedules = ScheduleController.getInstance().deleteSchedulesByRoute(routeId);
                    if (deletedSchedules < 0) {
                        scheduleDeleteFailed = true;
                        return false;
                    }
                }
                return RouteController.getInstance().deleteRoute(routeId);
            }

            @Override
            protected void done() {
                try {
                    if (get()) {
                        StringBuilder success = new StringBuilder("Route deleted.");
                        if (bookingCount > 0) {
                            success.append(" ").append(deletedBookings).append(" booking(s) removed.");
                        }
                        if (scheduleCount > 0) {
                            success.append(" ").append(deletedSchedules)
                                    .append(" schedule(s) removed from ").append(routeLabel).append(".");
                        }
                        JOptionPane.showMessageDialog(AddRoutePanel.this, success.toString());
                        clearSelection();
                        loadRoutes();
                    } else if (bookingDeleteFailed) {
                        JOptionPane.showMessageDialog(AddRoutePanel.this,
                                "Delete failed. Unable to remove bookings for this route.");
                    } else if (scheduleDeleteFailed) {
                        JOptionPane.showMessageDialog(AddRoutePanel.this,
                                "Delete failed. Unable to remove schedules for this route.");
                    } else {
                        JOptionPane.showMessageDialog(AddRoutePanel.this, "Delete failed. Please try again.");
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(AddRoutePanel.this, "Error deleting route: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void syncSelectionFromTable() {
        int selectedRow = routeTable.getSelectedRow();
        if (selectedRow >= 0 && selectedRow < routes.size()) {
            selectedRoute = routes.get(selectedRow);
            srcField.setSelectedItem(selectedRoute.getSourceCity());
            dstField.setSelectedItem(selectedRoute.getDestinationCity());
            distField.setText(String.valueOf(selectedRoute.getDistanceKm()));
            durField.setText(selectedRoute.getEstimatedDuration());
            updateBtn.setEnabled(true);
            deleteBtn.setEnabled(true);
        }
    }

    private void clearSelection() {
        selectedRoute = null;
        srcField.setSelectedIndex(-1);
        dstField.setSelectedIndex(-1);
        distField.setText("");
        durField.setText("");
        routeTable.clearSelection();
        updateBtn.setEnabled(false);
        deleteBtn.setEnabled(false);
    }

    private void loadRoutes() {
        new SwingWorker<List<Route>, Void>() {
            @Override
            protected List<Route> doInBackground() {
                return RouteController.getInstance().getAllRoutes();
            }

            @Override
            protected void done() {
                try {
                    routes = get();
                    tableModel.setRowCount(0);
                    if (routes != null) {
                        for (Route route : routes) {
                            tableModel.addRow(new Object[] {
                                    route.getId(),
                                    route.getSourceCity(),
                                    route.getDestinationCity(),
                                    route.getDistanceKm(),
                                    route.getEstimatedDuration()
                            });
                        }
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(AddRoutePanel.this, "Error loading routes: " + e.getMessage());
                }
            }
        }.execute();
    }
}
