package views.admin;

import controllers.BusController;
import controllers.RouteController;
import controllers.ScheduleController;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
import models.Bus;
import models.Route;
import models.Schedule;

// Admin-facing panel that lets staff create, edit, and delete bus schedules while keeping UI selections in sync.
public class AddSchedulePanel extends JPanel {
    private JComboBox<String> busCombo, routeCombo;
    private JTextField departureDateField, priceField;
    private JButton loadDataBtn, addBtn, updateBtn, deleteBtn, clearBtn, refreshBtn;
    private JTable scheduleTable;
    private final DefaultTableModel tableModel;
    
    private List<Bus> buses;
    private List<Route> routes;
    private List<Schedule> schedules = new ArrayList<>();
    private Schedule selectedSchedule;

    public AddSchedulePanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(Color.WHITE);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Load Data:"), gbc);
        loadDataBtn = new JButton("Refresh Lists");
        gbc.gridx = 1;
        formPanel.add(loadDataBtn, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Select Bus:"), gbc);
        busCombo = new JComboBox<>();
        gbc.gridx = 1;
        formPanel.add(busCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("Select Route:"), gbc);
        routeCombo = new JComboBox<>();
        gbc.gridx = 1;
        formPanel.add(routeCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(new JLabel("Departure (yyyy-MM-dd HH:mm):"), gbc);
        departureDateField = new JTextField(LocalDateTime.now().plusDays(1)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        gbc.gridx = 1;
        formPanel.add(departureDateField, gbc);

        gbc.gridx = 0; gbc.gridy = 4;
        formPanel.add(new JLabel("Price ($):"), gbc);
        priceField = new JTextField();
        gbc.gridx = 1;
        formPanel.add(priceField, gbc);

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        buttonRow.setOpaque(false);
        addBtn = new JButton("Add Schedule");
        updateBtn = new JButton("Update");
        deleteBtn = new JButton("Delete");
        clearBtn = new JButton("Clear");
        refreshBtn = new JButton("Refresh");
        buttonRow.add(addBtn);
        buttonRow.add(updateBtn);
        buttonRow.add(deleteBtn);
        buttonRow.add(clearBtn);
        buttonRow.add(refreshBtn);
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2;
        formPanel.add(buttonRow, gbc);

        add(formPanel, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(new Object[]{"ID", "Bus", "Route", "Departure", "Arrival", "Price", "Available Seats"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        scheduleTable = new JTable(tableModel);
        scheduleTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        scheduleTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                syncSelectionFromTable();
            }
        });
        JScrollPane scrollPane = new JScrollPane(scheduleTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(230, 230, 230)));
        add(scrollPane, BorderLayout.CENTER);

        loadDataBtn.addActionListener(e -> loadLists());
        addBtn.addActionListener(e -> createSchedule());
        updateBtn.addActionListener(e -> updateSchedule());
        deleteBtn.addActionListener(e -> deleteSchedule());
        clearBtn.addActionListener(e -> clearSelection());
        refreshBtn.addActionListener(e -> loadSchedules());

        // Populate initial lists and table
        loadLists();
    }

    private void loadLists() {
        // Refresh bus/route dropdowns off the EDT to avoid freezing the UI, then reload schedules with the new context.
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                buses = BusController.getInstance().getAllBuses();
                routes = RouteController.getInstance().getAllRoutes();
                return null;
            }
            @Override
            protected void done() {
                busCombo.removeAllItems();
                routeCombo.removeAllItems();
                if (buses != null) {
                    for (Bus b : buses) {
                        busCombo.addItem(b.getBusNumber());
                    }
                }
                if (routes != null) {
                    for (Route r : routes) {
                        routeCombo.addItem(r.getSourceCity() + " -> " + r.getDestinationCity());
                    }
                }
                loadSchedules();
            }
        }.execute();
    }

    private void createSchedule() {
        try {
            if (busCombo.getSelectedIndex() < 0 || routeCombo.getSelectedIndex() < 0) {
                JOptionPane.showMessageDialog(this, "Please select Bus and Route.");
                return;
            }
            
            Bus selectedBus = buses.get(busCombo.getSelectedIndex());
            Route selectedRoute = routes.get(routeCombo.getSelectedIndex());
            LocalDateTime departure = LocalDateTime.parse(departureDateField.getText(), 
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            LocalDateTime arrival = departure.plusHours(4); // Default 4-hour journey
            BigDecimal price = new BigDecimal(priceField.getText());
            
            Schedule schedule = new Schedule(null, selectedBus, selectedRoute, departure, arrival, price);
            schedule.setAvailableSeats(selectedBus.getTotalSeats());

            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() {
                    ScheduleController.getInstance().addSchedule(schedule);
                    return null;
                }
                @Override
                protected void done() {
                    JOptionPane.showMessageDialog(AddSchedulePanel.this, "Schedule Created!");
                    clearSelection();
                    loadSchedules();
                }
            }.execute();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: Check date/price format. " + ex.getMessage());
        }
    }

    private void updateSchedule() {
        if (selectedSchedule == null) {
            JOptionPane.showMessageDialog(this, "Select a schedule to update.");
            return;
        }

        try {
            if (busCombo.getSelectedIndex() < 0 || routeCombo.getSelectedIndex() < 0) {
                JOptionPane.showMessageDialog(this, "Please select Bus and Route.");
                return;
            }

            Bus selectedBus = buses.get(busCombo.getSelectedIndex());
            Route selectedRoute = routes.get(routeCombo.getSelectedIndex());
            LocalDateTime departure = LocalDateTime.parse(departureDateField.getText(),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            LocalDateTime arrival = departure.plusHours(4);
            BigDecimal price = new BigDecimal(priceField.getText());

            Schedule updated = new Schedule(selectedSchedule.getId(), selectedBus, selectedRoute, departure, arrival, price);
            // Preserve current available seats to avoid wiping bookings
            updated.setAvailableSeats(selectedSchedule.getAvailableSeats());

            new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() {
                    return ScheduleController.getInstance().updateSchedule(updated);
                }

                @Override
                protected void done() {
                    try {
                        if (get()) {
                            JOptionPane.showMessageDialog(AddSchedulePanel.this, "Schedule updated.");
                            clearSelection();
                            loadSchedules();
                        } else {
                            JOptionPane.showMessageDialog(AddSchedulePanel.this, "Update failed. Please try again.");
                        }
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(AddSchedulePanel.this, "Failed to update schedule: " + ex.getMessage());
                    }
                }
            }.execute();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: Check date/price format. " + ex.getMessage());
        }
    }

    private void deleteSchedule() {
        if (selectedSchedule == null) {
            JOptionPane.showMessageDialog(this, "Select a schedule to delete.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete schedule ID " + selectedSchedule.getId() + "?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                return ScheduleController.getInstance().deleteSchedule(selectedSchedule.getId());
            }

            @Override
            protected void done() {
                try {
                    if (get()) {
                        JOptionPane.showMessageDialog(AddSchedulePanel.this, "Schedule deleted.");
                        clearSelection();
                        loadSchedules();
                    } else {
                        JOptionPane.showMessageDialog(AddSchedulePanel.this, "Delete failed. Please try again.");
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(AddSchedulePanel.this, "Failed to delete schedule: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void clearSelection() {
        selectedSchedule = null;
        departureDateField.setText(LocalDateTime.now().plusDays(1)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        priceField.setText("");
        busCombo.setSelectedIndex(-1);
        routeCombo.setSelectedIndex(-1);
        scheduleTable.clearSelection();
        updateBtn.setEnabled(false);
        deleteBtn.setEnabled(false);
    }

    private void syncSelectionFromTable() {
        int selectedRow = scheduleTable.getSelectedRow();
        if (selectedRow >= 0 && selectedRow < schedules.size()) {
            selectedSchedule = schedules.get(selectedRow);
            departureDateField.setText(selectedSchedule.getDepartureTime()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            priceField.setText(selectedSchedule.getTicketPrice().toString());

            // Set combo selections to match the selected schedule
            if (buses != null) {
                for (int i = 0; i < buses.size(); i++) {
                    if (buses.get(i).getId().equals(selectedSchedule.getBus().getId())) {
                        busCombo.setSelectedIndex(i);
                        break;
                    }
                }
            }
            if (routes != null) {
                for (int i = 0; i < routes.size(); i++) {
                    if (routes.get(i).getId().equals(selectedSchedule.getRoute().getId())) {
                        routeCombo.setSelectedIndex(i);
                        break;
                    }
                }
            }
            updateBtn.setEnabled(true);
            deleteBtn.setEnabled(true);
        }
    }

    private void loadSchedules() {
        new SwingWorker<List<Schedule>, Void>() {
            @Override
            protected List<Schedule> doInBackground() {
                return ScheduleController.getInstance().getAllSchedules();
            }

            @Override
            protected void done() {
                try {
                    schedules = get();
                    tableModel.setRowCount(0);
                    if (schedules != null) {
                        for (Schedule schedule : schedules) {
                            tableModel.addRow(new Object[]{
                                    schedule.getId(),
                                    schedule.getBus().getBusNumber(),
                                    schedule.getRoute().getSourceCity() + " -> " + schedule.getRoute().getDestinationCity(),
                                    schedule.getDepartureTime(),
                                    schedule.getArrivalTime(),
                                    schedule.getTicketPrice(),
                                    schedule.getAvailableSeats()
                            });
                        }
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(AddSchedulePanel.this, "Error loading schedules: " + e.getMessage());
                }
            }
        }.execute();
    }
}
