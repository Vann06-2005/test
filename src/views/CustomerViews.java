package views;

import controllers.BookingController;
import controllers.ScheduleController;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import models.Booking;
import models.Schedule;
import models.User;

public class CustomerViews extends JPanel {
    private Login loginApp;
    private User currentUser;

    // UI Components
    private JComboBox<String> fromCombo, toCombo; // Dropdowns for user-friendly city selection
    private JPanel resultsListPanel; // Container for the cards
    private JButton searchBtn, historyBtn;
    private static final String SELECT_CITY = "Select City";

    public CustomerViews(Login app, User user) {
        this.loginApp = app;
        this.currentUser = user;

        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        // --- TOP SECTION: Header & Search Form ---
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setBackground(Color.WHITE);
        topPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // 1. Header (Hello User + Logout)
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        JLabel title = new JLabel("Hello, " + user.getFullName());
        title.setFont(new Font("SansSerif", Font.BOLD, 18));

        JButton logoutBtn = new JButton("Logout");
        logoutBtn.addActionListener(e -> loginApp.logout());

        header.add(title, BorderLayout.WEST);
        header.add(logoutBtn, BorderLayout.EAST);
        topPanel.add(header);
        topPanel.add(Box.createVerticalStrut(20)); // Spacer

        // 2. Search Inputs (From, To)
        topPanel.add(createInputLabel("From City:"));
        fromCombo = createStyledComboBox();
        topPanel.add(fromCombo);
        topPanel.add(Box.createVerticalStrut(10));

        topPanel.add(createInputLabel("To Destination:"));
        toCombo = createStyledComboBox();
        topPanel.add(toCombo);
        topPanel.add(Box.createVerticalStrut(10));

        topPanel.add(Box.createVerticalStrut(20));

        // 3. Action Buttons (Search & History)
        JPanel buttonRow = new JPanel(new GridLayout(1, 2, 10, 0)); // 2 columns, 10px gap
        buttonRow.setBackground(Color.WHITE);

        searchBtn = new JButton("Search Buses");
        searchBtn.setBackground(new Color(50, 150, 250));
        searchBtn.setForeground(Color.WHITE);
        searchBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        searchBtn.setPreferredSize(new Dimension(0, 45));

        historyBtn = new JButton("My History");
        historyBtn.setBackground(new Color(240, 240, 240));
        historyBtn.setForeground(Color.BLACK);
        historyBtn.setFont(new Font("SansSerif", Font.BOLD, 14));

        buttonRow.add(searchBtn);
        buttonRow.add(historyBtn);
        topPanel.add(buttonRow);

        add(topPanel, BorderLayout.NORTH);

        // --- BOTTOM SECTION: Scrollable Results ---
        resultsListPanel = new JPanel();
        resultsListPanel.setLayout(new BoxLayout(resultsListPanel, BoxLayout.Y_AXIS));
        resultsListPanel.setBackground(new Color(245, 245, 245)); // Light gray background for list

        // Wrap it in a ScrollPane
        JScrollPane scrollPane = new JScrollPane(resultsListPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16); // Smooth scrolling

        add(scrollPane, BorderLayout.CENTER);

        // --- ACTIONS ---
        searchBtn.addActionListener(e -> performSearch());
        historyBtn.addActionListener(e -> showHistoryPopup());

        loadCityOptions(); // Populate dropdowns from available schedules
        showAvailableBuses(); // Fill empty space with available buses
    }

    // Helper to style inputs
    private JComboBox<String> createStyledComboBox() {
        JComboBox<String> combo = new JComboBox<>(new String[] { SELECT_CITY });
        combo.setPreferredSize(new Dimension(999, 40));
        combo.setMaximumSize(new Dimension(999, 40)); // Prevent horizontal shrinking
        combo.setFont(new Font("SansSerif", Font.PLAIN, 16));
        return combo;
    }
    //
    private JLabel createInputLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.PLAIN, 12));
        l.setForeground(Color.GRAY);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    // Load unique cities to keep dropdown options consistent with the database
    private void loadCityOptions() {
        new SwingWorker<String[], Void>() {
            @Override
            protected String[] doInBackground() {
                List<Schedule> schedules = ScheduleController.getInstance().getAllSchedules();
                Set<String> cities = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
                for (Schedule schedule : schedules) {
                    cities.add(schedule.getRoute().getSourceCity());
                    cities.add(schedule.getRoute().getDestinationCity());
                }

                ArrayList<String> cityList = new ArrayList<>();
                cityList.add(SELECT_CITY);
                cityList.addAll(cities);
                return cityList.toArray(new String[0]);
            }

            @Override
            protected void done() {
                try {
                    String[] cities = get();
                    fromCombo.setModel(new DefaultComboBoxModel<>(cities));
                    toCombo.setModel(new DefaultComboBoxModel<>(cities));
                    fromCombo.setSelectedIndex(0);
                    toCombo.setSelectedIndex(0);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.execute();
    }

    private void showAvailableBuses() {
        resultsListPanel.removeAll();
        resultsListPanel.revalidate();
        resultsListPanel.repaint();

        new SwingWorker<List<Schedule>, Void>() {
            @Override
            protected List<Schedule> doInBackground() {
                return ScheduleController.getInstance().getAllSchedules();
            }

            @Override
            protected void done() {
                try {
                    List<Schedule> results = get();
                    renderSchedules(results);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.execute();
    }

    // --- LOGIC: SEARCH & POPULATE CARDS ---
    private void performSearch() {
        String from = (String) fromCombo.getSelectedItem();
        String to = (String) toCombo.getSelectedItem();

        if (from == null || to == null || SELECT_CITY.equals(from) || SELECT_CITY.equals(to)) {
            JOptionPane.showMessageDialog(this, "Please select both departure and destination.");
            return;
        }

        resultsListPanel.removeAll(); // Clear old results
        resultsListPanel.revalidate();
        resultsListPanel.repaint();

        searchBtn.setEnabled(false);
        searchBtn.setText("Searching...");

        new SwingWorker<List<Schedule>, Void>() {
            @Override
            protected List<Schedule> doInBackground() {
                return ScheduleController.getInstance().searchTrips(from, to);
            }

            @Override
            protected void done() {
                try {
                    renderSchedules(get());
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    searchBtn.setEnabled(true);
                    searchBtn.setText("Search Buses");
                }
            }
        }.execute();
    }

    private void renderSchedules(List<Schedule> schedules) {
        resultsListPanel.removeAll();

        List<Schedule> upcoming = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (Schedule schedule : schedules) {
            LocalDateTime departure = schedule.getDepartureTime();
            if (departure == null || !departure.isBefore(now)) {
                upcoming.add(schedule);
            }
        }

        if (upcoming.isEmpty()) {
            JLabel noData = new JLabel("No buses available.", SwingConstants.CENTER);
            noData.setBorder(new EmptyBorder(20, 0, 0, 0));
            resultsListPanel.add(noData);
        } else {
            // Create a "Card" for each result using the external BusCardPanel class
            for (Schedule s : upcoming) {
                BusCardPanel card = new BusCardPanel(s, e -> initiateBooking(s));
                resultsListPanel.add(card);
                resultsListPanel.add(Box.createVerticalStrut(10)); // Gap between cards
            }
        }

        resultsListPanel.revalidate();
        resultsListPanel.repaint();
    }

    // --- LOGIC: BOOKING ---
    private void initiateBooking(Schedule schedule) {
        LocalDateTime departureTime = schedule.getDepartureTime();
        if (departureTime != null && departureTime.isBefore(LocalDateTime.now())) {
            JOptionPane.showMessageDialog(this, "This bus has already departed.");
            return;
        }
        int totalSeats = schedule.getBus().getTotalSeats();

        new SwingWorker<List<String>, Void>() {
            @Override
            protected List<String> doInBackground() {
                return BookingController.getInstance().getTakenSeats(schedule.getId());
            }

            @Override
            protected void done() {
                try {
                    List<String> taken = get();
                    StringBuilder msg = new StringBuilder("Enter Seat (1-" + totalSeats + "):\n");
                    if (!taken.isEmpty())
                        msg.append("Taken: ").append(String.join(", ", taken));

                    String input = JOptionPane.showInputDialog(CustomerViews.this, msg.toString());
                    if (input != null && !input.trim().isEmpty()) {
                        // Check number format first
                        try {
                            int seatNum = Integer.parseInt(input.trim());
                            if (seatNum < 1 || seatNum > totalSeats) {
                                JOptionPane.showMessageDialog(CustomerViews.this, "Invalid Seat Number!");
                                return;
                            }
                            Booking newBooking = new Booking(null, currentUser, schedule, String.valueOf(seatNum),
                                    schedule.getTicketPrice());
                            performBooking(newBooking);
                        } catch (NumberFormatException nfe) {
                            JOptionPane.showMessageDialog(CustomerViews.this, "Please enter a number.");
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }.execute();
    }

    private void performBooking(Booking newBooking) {
        BigDecimal price = newBooking.getTotalAmount();
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                return BookingController.getInstance().createBooking(newBooking);
            }

            @Override
            protected void done() {
                try {
                    if (get()) {
                        JOptionPane.showMessageDialog(CustomerViews.this, "Booking Confirmed!" + "   Price: " + price);
                        performSearch(); // Refresh list to update seats
                    } else {
                        LocalDateTime departureTime = newBooking.getSchedule().getDepartureTime();
                        if (departureTime != null && departureTime.isBefore(LocalDateTime.now())) {
                            JOptionPane.showMessageDialog(CustomerViews.this, "Booking Failed: This bus already departed.");
                        } else {
                            JOptionPane.showMessageDialog(CustomerViews.this, "Booking Failed (Seat Taken).");
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.execute();
    }

    // --- LOGIC: HISTORY POPUP ---
    private void showHistoryPopup() {
        Window owner = SwingUtilities.getWindowAncestor(this);
        JDialog historyDialog = new JDialog(owner, "My History", Dialog.ModalityType.APPLICATION_MODAL);
        HistoryPanel historyPanel = new HistoryPanel(currentUser);
        historyDialog.add(historyPanel);

        // Match phone-friendly size
        int width = (owner != null) ? owner.getWidth() - 20 : 430;
        int height = (owner != null) ? Math.min(owner.getHeight() - 80, 800) : 720;
        historyDialog.setSize(width, height);
        historyDialog.setLocationRelativeTo(this);
        historyDialog.setVisible(true);
    }
}
