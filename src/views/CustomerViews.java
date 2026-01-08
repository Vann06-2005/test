package views;

import controllers.BookingController;
import controllers.ScheduleController;
import models.Booking;
import models.Schedule;
import models.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class CustomerViews extends JPanel {
    private MainApp mainApp;
    private User currentUser;
    
    // UI Components
    private JComboBox<String> fromCombo, toCombo; // Dropdowns for user-friendly city selection
    private JPanel resultsListPanel; // Container for the cards
    private JButton searchBtn, historyBtn;
    private static final String SELECT_CITY = "Select City";

    public CustomerViews(MainApp app, User user) {
        this.mainApp = app;
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
        logoutBtn.addActionListener(e -> mainApp.logout());
        
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
    }

    // Helper to style inputs
    private JComboBox<String> createStyledComboBox() {
        JComboBox<String> combo = new JComboBox<>(new String[] { SELECT_CITY });
        combo.setPreferredSize(new Dimension(999, 40));
        combo.setMaximumSize(new Dimension(999, 40)); // Prevent horizontal shrinking
        combo.setFont(new Font("SansSerif", Font.PLAIN, 16));
        return combo;
    }
    
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
                    List<Schedule> results = get();
                    if (results.isEmpty()) {
                        JLabel noData = new JLabel("No buses found.", SwingConstants.CENTER);
                        noData.setBorder(new EmptyBorder(20,0,0,0));
                        resultsListPanel.add(noData);
                    } else {
                        // Create a "Card" for each result using the external BusCardPanel class
                        for (Schedule s : results) {
                            BusCardPanel card = new BusCardPanel(s, e -> initiateBooking(s));
                            resultsListPanel.add(card);
                            resultsListPanel.add(Box.createVerticalStrut(10)); // Gap between cards
                        }
                    }
                    resultsListPanel.revalidate();
                    resultsListPanel.repaint();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    searchBtn.setEnabled(true);
                    searchBtn.setText("Search Buses");
                }
            }
        }.execute();
    }

    // --- LOGIC: BOOKING ---
    private void initiateBooking(Schedule schedule) {
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
                    if(!taken.isEmpty()) msg.append("Taken: ").append(String.join(", ", taken));
                    
                    String input = JOptionPane.showInputDialog(CustomerViews.this, msg.toString());
                    if(input != null && !input.trim().isEmpty()) {
                         // Check number format first
                        try {
                            int seatNum = Integer.parseInt(input.trim());
                            if (seatNum < 1 || seatNum > totalSeats) {
                                JOptionPane.showMessageDialog(CustomerViews.this, "Invalid Seat Number!");
                                return;
                            }
                            Booking newBooking = new Booking(null, currentUser, schedule, String.valueOf(seatNum), schedule.getTicketPrice());
                            performBooking(newBooking);
                        } catch (NumberFormatException nfe) {
                            JOptionPane.showMessageDialog(CustomerViews.this, "Please enter a number.");
                        }
                    }
                } catch(Exception ex) { ex.printStackTrace(); }
            }
        }.execute();
    }

    private void performBooking(Booking newBooking) {
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                return BookingController.getInstance().createBooking(newBooking);
            }
            @Override
            protected void done() {
                try {
                    if (get()) {
                        JOptionPane.showMessageDialog(CustomerViews.this, "Booking Confirmed!");
                        performSearch(); // Refresh list to update seats
                    } else {
                        JOptionPane.showMessageDialog(CustomerViews.this, "Booking Failed (Seat Taken).");
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }
        }.execute();
    }

    // --- LOGIC: HISTORY POPUP ---
    private void showHistoryPopup() {
        JDialog historyDialog = new JDialog(SwingUtilities.getWindowAncestor(this), "My History", Dialog.ModalityType.APPLICATION_MODAL);
        historyDialog.setSize(400, 600);
        historyDialog.setLocationRelativeTo(this);
        historyDialog.add(new HistoryPanel(currentUser)); // Uses the reusable file
        historyDialog.setVisible(true);
    }
}
