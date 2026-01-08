package views.admin;

import controllers.RouteController;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import models.Route;

public class AddRoutePanel extends JPanel {
    private JTextField srcField, dstField, distField, durField;

    public AddRoutePanel() {
        setLayout(new GridBagLayout());
        setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        add(new JLabel("Source City:"), gbc);
        srcField = new JTextField();
        gbc.gridx = 1;
        add(srcField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        add(new JLabel("Destination City:"), gbc);
        dstField = new JTextField();
        gbc.gridx = 1;
        add(dstField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        add(new JLabel("Distance (km):"), gbc);
        distField = new JTextField();
        gbc.gridx = 1;
        add(distField, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        add(new JLabel("Duration (e.g., 4h 30m):"), gbc);
        durField = new JTextField();
        gbc.gridx = 1;
        add(durField, gbc);

        JButton saveBtn = new JButton("Save Route");
        saveBtn.setBackground(new Color(0, 153, 102));
        saveBtn.setForeground(Color.WHITE);
        gbc.gridx = 1; gbc.gridy = 4;
        add(saveBtn, gbc);

        saveBtn.addActionListener(e -> {
            try {
                String src = srcField.getText();
                String dst = dstField.getText();
                double dist = Double.parseDouble(distField.getText());
                String dur = durField.getText();
                Route r = new Route(null, src, dst, dist, dur);

                new SwingWorker<Void, Void>() {
                    @Override protected Void doInBackground() {
                        RouteController.getInstance().addRoute(r);
                        return null;
                    }
                    @Override protected void done() {
                        JOptionPane.showMessageDialog(AddRoutePanel.this, "Route Added!");
                        srcField.setText("");
                        dstField.setText("");
                        distField.setText("");
                        durField.setText("");
                    }
                }.execute();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Invalid inputs.");
            }
        });
    }
}
