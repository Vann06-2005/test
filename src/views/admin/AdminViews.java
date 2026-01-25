package views.admin;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import views.Login;

public class AdminViews extends JPanel {
    private Login loginApp;

    public AdminViews(Login app) {
        this.loginApp = app;

        setLayout(new BorderLayout(15, 15));
        setBackground(new Color(245, 247, 250));
        setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));

        // --- Header section ---
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(40, 53, 82));
        header.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        JLabel title = new JLabel("EazyBus Admin Console");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 18));
        header.add(title, BorderLayout.WEST);

        JPanel headerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        headerRight.setOpaque(false);
        JLabel welcome = new JLabel("Welcome, Admin");
        welcome.setForeground(Color.WHITE);
        JButton logoutBtn = new JButton("Logout");
        logoutBtn.setFocusPainted(false);
        logoutBtn.setBackground(Color.WHITE);
        logoutBtn.addActionListener(e -> loginApp.logout());
        headerRight.add(welcome);
        headerRight.add(logoutBtn);
        header.add(headerRight, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);

        // --- Tabs section ---
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tabs.addTab("Manage Buses", new ManageBusesPanel());
        tabs.addTab("Add Route", new AddRoutePanel());
        tabs.addTab("Add Schedule", new AddSchedulePanel());
        tabs.addTab("Manage Bookings", new ManageBookingsPanel());

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(Color.WHITE);
        contentPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 215, 225), 1),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)));
        contentPanel.add(tabs, BorderLayout.CENTER);

        add(contentPanel, BorderLayout.CENTER);
    }
}
