package views;

import controllers.BookingController;
import java.awt.*;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import models.Booking;
import models.User;

public class HistoryPanel extends JPanel {
    private User currentUser;
    private JPanel listPanel;
    private JButton refreshBtn;
    private JLabel emptyLabel;
    private final DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final NumberFormat currencyFmt = NumberFormat.getCurrencyInstance();
    private static final int CARD_HEIGHT = 110;

    public HistoryPanel(User user) {
        this.currentUser = user;
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("My Bookings");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        JLabel subtitle = new JLabel("View or cancel your tickets");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        subtitle.setForeground(Color.DARK_GRAY);
        header.add(title, BorderLayout.NORTH);
        header.add(subtitle, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);
        
        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(Color.WHITE);

        emptyLabel = new JLabel("No bookings yet", SwingConstants.CENTER);
        emptyLabel.setForeground(Color.GRAY);
        emptyLabel.setBorder(new EmptyBorder(16, 0, 16, 0));
        
        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);
        
        JPanel actionBar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actionBar.setOpaque(false);
        refreshBtn = new JButton("Refresh History");
        actionBar.add(refreshBtn);
        add(actionBar, BorderLayout.SOUTH);
        
        refreshBtn.addActionListener(e -> loadHistory());
    }
    
    
    @Override
    public void addNotify() {
        super.addNotify();
        loadHistory();
    }

    private void loadHistory() {
        new SwingWorker<List<Booking>, Void>() {
            @Override
            protected List<Booking> doInBackground() {
                return BookingController.getInstance().getBookingsForUser(currentUser.getId());
            }

            @Override
            protected void done() {
                try {
                    List<Booking> list = get();
                    renderBookings(list);
                } catch (Exception e) { e.printStackTrace(); }
            }
        }.execute();
    }

    private void renderBookings(List<Booking> bookings) {
        listPanel.removeAll();

        if (bookings == null || bookings.isEmpty()) {
            listPanel.add(emptyLabel);
        } else {
            for (Booking b : bookings) {
                listPanel.add(createBookingCard(b));
                listPanel.add(Box.createVerticalStrut(10));
            }
        }
        listPanel.revalidate();
        listPanel.repaint();
    }

    private JPanel createBookingCard(Booking booking) {
        JPanel card = new JPanel(new BorderLayout(8, 4));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 225, 232)),
                new EmptyBorder(10, 12, 10, 12)
        ));
        card.setPreferredSize(new Dimension(0, CARD_HEIGHT));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, CARD_HEIGHT));
        card.setMinimumSize(new Dimension(0, CARD_HEIGHT));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);
        JLabel dateLabel = new JLabel(dateFmt.format(booking.getBookingDate()));
        dateLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));

        JLabel statusLabel = new JLabel(booking.getStatus().toUpperCase());
        statusLabel.setOpaque(true);
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
        Color statusColor = "CONFIRMED".equalsIgnoreCase(booking.getStatus())
                ? new Color(46, 160, 67)
                : new Color(200, 80, 60);
        statusLabel.setBackground(statusColor);
        statusLabel.setBorder(new EmptyBorder(4, 8, 4, 8));
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);

        topRow.add(dateLabel, BorderLayout.WEST);
        topRow.add(statusLabel, BorderLayout.EAST);

        JLabel routeLabel = new JLabel(booking.getSchedule().getRoute().getSourceCity() + " → " + booking.getSchedule().getRoute().getDestinationCity());
        routeLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        routeLabel.setForeground(Color.DARK_GRAY);

        JPanel bottomRow = new JPanel(new BorderLayout());
        bottomRow.setOpaque(false);
        JLabel seatPrice = new JLabel("Seat " + booking.getSeatNumber() + " • " + currencyFmt.format(booking.getTotalAmount()));
        seatPrice.setFont(new Font("SansSerif", Font.PLAIN, 12));

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.setFocusPainted(false);
        cancelBtn.setFont(new Font("SansSerif", Font.PLAIN, 12));
        cancelBtn.setVisible("CONFIRMED".equalsIgnoreCase(booking.getStatus()));
        cancelBtn.addActionListener(e -> cancelBooking(booking, cancelBtn));

        bottomRow.add(seatPrice, BorderLayout.WEST);
        bottomRow.add(cancelBtn, BorderLayout.EAST);

        card.add(topRow, BorderLayout.NORTH);
        card.add(routeLabel, BorderLayout.CENTER);
        card.add(bottomRow, BorderLayout.SOUTH);

        return card;
    }

    private void cancelBooking(Booking booking, JButton trigger) {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Cancel this booking?",
                "Confirm",
                JOptionPane.YES_NO_OPTION
        );
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        trigger.setEnabled(false);
        refreshBtn.setEnabled(false);

        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                return BookingController.getInstance().cancelBooking(booking.getId(), currentUser.getId());
            }

            @Override
            protected void done() {
                try {
                    if (get()) {
                        JOptionPane.showMessageDialog(HistoryPanel.this, "Booking cancelled.");
                        loadHistory();
                    } else {
                        JOptionPane.showMessageDialog(HistoryPanel.this, "Unable to cancel booking.");
                        trigger.setEnabled(true);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(HistoryPanel.this, "Error cancelling booking.");
                    trigger.setEnabled(true);
                } finally {
                    refreshBtn.setEnabled(true);
                }
            }
        }.execute();
    }
}
