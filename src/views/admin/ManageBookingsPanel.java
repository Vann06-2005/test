package views.admin;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import controllers.BookingController;
import models.Booking;
import models.Schedule;

// Simple admin view for browsing and cancelling user bookings.
public class ManageBookingsPanel extends JPanel {
    private final DefaultTableModel tableModel;
    private final JTable bookingTable;
    private final JTextField userIdField;
    private final JButton loadAllBtn, filterBtn, clearBtn, cancelBtn, refreshBtn;

    private List<Booking> bookings = new ArrayList<>();
    private Booking selectedBooking;
    private Long currentFilterUserId;

    public ManageBookingsPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(Color.WHITE);

        JPanel filterPanel = new JPanel(new GridBagLayout());
        filterPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        gbc.gridx = 0;
        gbc.gridy = 0;
        filterPanel.add(new JLabel("Filter by User ID:"), gbc);

        userIdField = new JTextField();
        gbc.gridx = 1;
        filterPanel.add(userIdField, gbc);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        buttons.setOpaque(false);
        loadAllBtn = new JButton("Load All");
        filterBtn = new JButton("Apply Filter");
        clearBtn = new JButton("Clear");
        refreshBtn = new JButton("Refresh");
        cancelBtn = new JButton("Cancel Booking");
        cancelBtn.setEnabled(false);
        buttons.add(loadAllBtn);
        buttons.add(filterBtn);
        buttons.add(clearBtn);
        buttons.add(refreshBtn);
        buttons.add(cancelBtn);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        filterPanel.add(buttons, gbc);

        add(filterPanel, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(
                new Object[] { "ID", "User", "Route", "Bus", "Departure", "Seat", "Amount", "Status", "Booked At" },
                0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        bookingTable = new JTable(tableModel);
        bookingTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        bookingTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                syncSelectionFromTable();
            }
        });

        JScrollPane scrollPane = new JScrollPane(bookingTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(230, 230, 230)));
        add(scrollPane, BorderLayout.CENTER);

        loadAllBtn.addActionListener(e -> loadBookings(null));
        filterBtn.addActionListener(e -> handleFilter());
        clearBtn.addActionListener(e -> {
            userIdField.setText("");
            loadBookings(null);
        });
        refreshBtn.addActionListener(e -> loadBookings(currentFilterUserId));
        cancelBtn.addActionListener(e -> handleCancel());

        loadBookings(null);
    }

    private void handleFilter() {
        String text = userIdField.getText().trim();
        if (text.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter a user ID to filter bookings.");
            return;
        }
        try {
            Long userId = Long.parseLong(text);
            loadBookings(userId);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "User ID must be a number.");
        }
    }

    private void handleCancel() {
        if (selectedBooking == null) {
            JOptionPane.showMessageDialog(this, "Select a booking to cancel.");
            return;
        }
        if ("CANCELLED".equalsIgnoreCase(selectedBooking.getStatus())) {
            JOptionPane.showMessageDialog(this, "This booking is already cancelled.");
            return;
        }

        int choice = JOptionPane.showConfirmDialog(
                this,
                "Cancel booking #" + selectedBooking.getId() + " for " + selectedBooking.getCustomer().getFullName()
                        + "?",
                "Confirm Cancel", JOptionPane.YES_NO_OPTION);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        cancelBtn.setEnabled(false);
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                return BookingController.getInstance().cancelBooking(selectedBooking.getId());
            }

            @Override
            protected void done() {
                try {
                    if (get()) {
                        JOptionPane.showMessageDialog(ManageBookingsPanel.this, "Booking cancelled.");
                        loadBookings(currentFilterUserId);
                    } else {
                        JOptionPane.showMessageDialog(ManageBookingsPanel.this,
                                "Unable to cancel booking. Please try again.");
                        cancelBtn.setEnabled(true);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ManageBookingsPanel.this,
                            "Error cancelling booking: " + ex.getMessage());
                    cancelBtn.setEnabled(true);
                }
            }
        }.execute();
    }

    private void syncSelectionFromTable() {
        int selectedRow = bookingTable.getSelectedRow();
        if (selectedRow >= 0 && selectedRow < bookings.size()) {
            selectedBooking = bookings.get(selectedRow);
            cancelBtn.setEnabled(!"CANCELLED".equalsIgnoreCase(selectedBooking.getStatus()));
        } else {
            selectedBooking = null;
            cancelBtn.setEnabled(false);
        }
    }

    private void loadBookings(Long userId) {
        currentFilterUserId = userId;
        cancelBtn.setEnabled(false);
        new SwingWorker<List<Booking>, Void>() {
            @Override
            protected List<Booking> doInBackground() {
                BookingController controller = BookingController.getInstance();
                if (userId != null) {
                    return controller.getBookingsForUser(userId);
                }
                return controller.getAllBookings();
            }

            @Override
            protected void done() {
                try {
                    bookings = get();
                    populateTable();
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(ManageBookingsPanel.this,
                            "Error loading bookings: " + e.getMessage());
                }
            }
        }.execute();
    }

    private void populateTable() {
        tableModel.setRowCount(0);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        if (bookings == null) {
            return;
        }

        for (Booking booking : bookings) {
            Schedule schedule = booking.getSchedule();
            String routeLabel = schedule.getRoute().getSourceCity() + " -> "
                    + schedule.getRoute().getDestinationCity();
            tableModel.addRow(new Object[] {
                    booking.getId(),
                    booking.getCustomer().getFullName(),
                    routeLabel,
                    schedule.getBus().getBusNumber(),
                    schedule.getDepartureTime().format(fmt),
                    booking.getSeatNumber(),
                    booking.getTotalAmount(),
                    booking.getStatus(),
                    booking.getBookingDate().format(fmt)
            });
        }
    }
}
