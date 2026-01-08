package views.admin;

import controllers.BusController;
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
import models.Bus;

public class ManageBusesPanel extends JPanel {
    private JTextField numberField, seatsField;
    private JComboBox<String> typeBox;
    private JTable busTable;
    private DefaultTableModel tableModel;
    private JButton addBtn, updateBtn, deleteBtn, refreshBtn, clearBtn;
    private List<Bus> buses = new ArrayList<>();
    private Bus selectedBus;

    public ManageBusesPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(Color.WHITE);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Bus Number (Plate):"), gbc);
        numberField = new JTextField();
        gbc.gridx = 1;
        formPanel.add(numberField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Total Seats:"), gbc);
        seatsField = new JTextField();
        gbc.gridx = 1;
        formPanel.add(seatsField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("Type:"), gbc);
        typeBox = new JComboBox<>(new String[]{"AC Sleeper", "Seater", "Luxury Volvo", "Mini Bus", "Electric"});
        typeBox.setEditable(true);
        gbc.gridx = 1;
        formPanel.add(typeBox, gbc);

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        buttonRow.setOpaque(false);
        addBtn = new JButton("Add Bus");
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
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        formPanel.add(buttonRow, gbc);

        add(formPanel, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(new Object[]{"ID", "Bus #", "Seats", "Type"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        busTable = new JTable(tableModel);
        busTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        busTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                syncSelectionFromTable();
            }
        });
        JScrollPane scrollPane = new JScrollPane(busTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(230, 230, 230)));
        add(scrollPane, BorderLayout.CENTER);

        addBtn.addActionListener(e -> handleAdd());
        updateBtn.addActionListener(e -> handleUpdate());
        deleteBtn.addActionListener(e -> handleDelete());
        clearBtn.addActionListener(e -> clearSelection());
        refreshBtn.addActionListener(e -> loadBuses());

        loadBuses();
    }

    private void handleAdd() {
        Bus bus;
        try {
            bus = buildBusFromForm(null);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage());
            return;
        }

        toggleActions(false);
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                return BusController.getInstance().addBus(bus);
            }

            @Override
            protected void done() {
                toggleActions(true);
                try {
                    if (get()) {
                        JOptionPane.showMessageDialog(ManageBusesPanel.this, "Bus added successfully.");
                        clearSelection();
                        loadBuses();
                    } else {
                        JOptionPane.showMessageDialog(ManageBusesPanel.this, "Unable to add bus. Check logs for details.");
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ManageBusesPanel.this, "Error adding bus: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void handleUpdate() {
        if (selectedBus == null) {
            JOptionPane.showMessageDialog(this, "Select a bus to update.");
            return;
        }

        Bus bus;
        try {
            bus = buildBusFromForm(selectedBus.getId());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage());
            return;
        }

        toggleActions(false);
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                return BusController.getInstance().updateBus(bus);
            }

            @Override
            protected void done() {
                toggleActions(true);
                try {
                    if (get()) {
                        JOptionPane.showMessageDialog(ManageBusesPanel.this, "Bus updated.");
                        clearSelection();
                        loadBuses();
                    } else {
                        JOptionPane.showMessageDialog(ManageBusesPanel.this, "Update failed. Check constraints and try again.");
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ManageBusesPanel.this, "Error updating bus: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void handleDelete() {
        if (selectedBus == null) {
            JOptionPane.showMessageDialog(this, "Select a bus to delete.");
            return;
        }

        int choice = JOptionPane.showConfirmDialog(this,
                "Delete bus " + selectedBus.getBusNumber() + "?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (choice != JOptionPane.YES_OPTION) return;

        toggleActions(false);
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                return BusController.getInstance().deleteBus(selectedBus.getId());
            }

            @Override
            protected void done() {
                toggleActions(true);
                try {
                    if (get()) {
                        JOptionPane.showMessageDialog(ManageBusesPanel.this, "Bus deleted along with its schedules and unused routes.");
                        clearSelection();
                        loadBuses();
                    } else {
                        JOptionPane.showMessageDialog(ManageBusesPanel.this, "Delete failed. Ensure no schedules depend on this bus.");
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ManageBusesPanel.this, "Error deleting bus: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void loadBuses() {
        toggleActions(false);
        new SwingWorker<List<Bus>, Void>() {
            @Override
            protected List<Bus> doInBackground() {
                return BusController.getInstance().getAllBuses();
            }

            @Override
            protected void done() {
                toggleActions(true);
                try {
                    List<Bus> busList = get();
                    buses = busList != null ? busList : new ArrayList<>();
                    tableModel.setRowCount(0);
                    for (Bus bus : buses) {
                        tableModel.addRow(new Object[]{ bus.getId(), bus.getBusNumber(), bus.getTotalSeats(), bus.getType() });
                    }
                    busTable.clearSelection();
                    selectedBus = null;
                    updateBtn.setEnabled(false);
                    deleteBtn.setEnabled(false);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ManageBusesPanel.this, "Failed to load buses: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private Bus buildBusFromForm(Long id) {
        String num = numberField.getText().trim();
        String seatsText = seatsField.getText().trim();
        Object typeValue = typeBox.getSelectedItem();
        String type = typeValue != null ? typeValue.toString().trim() : "";

        if (num.isEmpty() || seatsText.isEmpty() || type.isEmpty()) {
            throw new IllegalArgumentException("Bus number, seat count, and type are required.");
        }

        int seats;
        try {
            seats = Integer.parseInt(seatsText);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Seats must be a whole number.");
        }
        if (seats <= 0) {
            throw new IllegalArgumentException("Seats must be greater than zero.");
        }

        Bus bus = new Bus(id, num, seats, type);
        bus.setOperational(true); // all buses operational by default
        return bus;
    }

    private void syncSelectionFromTable() {
        int row = busTable.getSelectedRow();
        if (row < 0 || row >= buses.size()) {
            return;
        }

        selectedBus = buses.get(row);
        numberField.setText(selectedBus.getBusNumber());
        seatsField.setText(String.valueOf(selectedBus.getTotalSeats()));
        typeBox.setSelectedItem(selectedBus.getType());
        updateBtn.setEnabled(true);
        deleteBtn.setEnabled(true);
    }

    private void clearSelection() {
        selectedBus = null;
        busTable.clearSelection();
        numberField.setText("");
        seatsField.setText("");
        typeBox.setSelectedIndex(0);
        updateBtn.setEnabled(false);
        deleteBtn.setEnabled(false);
    }

    private void toggleActions(boolean enabled) {
        addBtn.setEnabled(enabled);
        refreshBtn.setEnabled(enabled);
        clearBtn.setEnabled(enabled);
        updateBtn.setEnabled(enabled && selectedBus != null);
        deleteBtn.setEnabled(enabled && selectedBus != null);
    }
}
