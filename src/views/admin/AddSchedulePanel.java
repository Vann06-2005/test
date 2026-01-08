package views.admin;

import controllers.BusController;
import controllers.RouteController;
import controllers.ScheduleController;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import models.Bus;
import models.Route;
import models.Schedule;

public class AddSchedulePanel extends JPanel {
    private JComboBox<String> busCombo, routeCombo;
    private JTextField dateField, priceField;
    private JButton loadDataBtn, saveBtn;

    private List<Bus> buses;
    private List<Route> routes;

    public AddSchedulePanel() {
        setLayout(new GridBagLayout());
        setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        add(new JLabel("Load Data:"), gbc);
        loadDataBtn = new JButton("Refresh Lists");
        gbc.gridx = 1;
        add(loadDataBtn, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        add(new JLabel("Select Bus:"), gbc);
        busCombo = new JComboBox<>();
        gbc.gridx = 1;
        add(busCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        add(new JLabel("Select Route:"), gbc);
        routeCombo = new JComboBox<>();
        gbc.gridx = 1;
        add(routeCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        add(new JLabel("Departure (yyyy-MM-dd HH:mm):"), gbc);
        dateField = new JTextField(LocalDateTime.now().plusDays(1)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        gbc.gridx = 1;
        add(dateField, gbc);

        gbc.gridx = 0; gbc.gridy = 4;
        add(new JLabel("Price ($):"), gbc);
        priceField = new JTextField();
        gbc.gridx = 1;
        add(priceField, gbc);

        saveBtn = new JButton("Create Schedule");
        saveBtn.setBackground(new Color(255, 140, 0));
        saveBtn.setForeground(Color.WHITE);
        gbc.gridx = 1; gbc.gridy = 5;
        add(saveBtn, gbc);

        loadDataBtn.addActionListener(e -> loadLists());
        saveBtn.addActionListener(e -> createSchedule());
    }

    private void loadLists() {
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
                if (buses != null) for (Bus b : buses) busCombo.addItem(b.getBusNumber());
                if (routes != null) for (Route r : routes) routeCombo.addItem(r.getSourceCity() + " ƒ+' " + r.getDestinationCity());
            }
        }.execute();
    }

    private void createSchedule() {
        try {
            if (busCombo.getSelectedIndex() < 0 || routeCombo.getSelectedIndex() < 0) return;
            Bus b = buses.get(busCombo.getSelectedIndex());
            Route r = routes.get(routeCombo.getSelectedIndex());
            LocalDateTime dep = LocalDateTime.parse(dateField.getText(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            LocalDateTime arr = dep.plusHours(4);
            BigDecimal price = new BigDecimal(priceField.getText());
            Schedule s = new Schedule(null, b, r, dep, arr, price);

            new SwingWorker<Void, Void>() {
                @Override protected Void doInBackground() {
                    ScheduleController.getInstance().addSchedule(s);
                    return null;
                }
                @Override protected void done() {
                    JOptionPane.showMessageDialog(AddSchedulePanel.this, "Schedule Created!");
                }
            }.execute();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: Check date/price format.");
        }
    }
}
