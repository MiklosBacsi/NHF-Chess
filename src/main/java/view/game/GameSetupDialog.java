package view.game;

import model.TimeSettings;

import javax.swing.*;
import java.awt.*;

/**
 * Pop-up window that forces the user to choose a chess clock.
 * @author Miklós Bácsi
 */
public class GameSetupDialog extends JDialog {
    private TimeSettings selectedSettings = null; // null means cancelled

    /**
     * Constructor that creates dialog for choosing check clock settings.
     * @param parent Main frame
     */
    public GameSetupDialog(Frame parent) {
        super(parent, "Game Setup", true);
        setLayout(new BorderLayout());
        setSize(400, 320); // Slightly taller to fit drop-downs comfortably
        setLocationRelativeTo(parent);

        // --- PRESETS ---
        JPanel presetsPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        presetsPanel.setBorder(BorderFactory.createTitledBorder("Presets"));

        addPresetButton(presetsPanel, "10 | 15", 10, 15);
        addPresetButton(presetsPanel, "10", 10, 0);
        addPresetButton(presetsPanel, "5", 5, 0);
        addPresetButton(presetsPanel, "3 | 2", 3, 2);
        addPresetButton(presetsPanel, "1 | 5", 1, 5);
        addPresetButton(presetsPanel, "1", 1, 0);

        // --- CUSTOM ---
        JPanel customPanel = new JPanel(new FlowLayout());
        customPanel.setBorder(BorderFactory.createTitledBorder("Custom"));

        // Generate Data for Drop-downs
        Integer[] minuteOptions = new Integer[30];
        for (int i = 0; i < 30; i++) minuteOptions[i] = i + 1; // 1 to 30

        Integer[] secondOptions = new Integer[31];
        for (int i = 0; i < 31; i++) secondOptions[i] = i;     // 0 to 30

        // Create Drop-downs (JComboBox)
        JComboBox<Integer> minCombo = new JComboBox<>(minuteOptions);
        minCombo.setSelectedItem(10); // Default 10 min

        JComboBox<Integer> secCombo = new JComboBox<>(secondOptions);
        secCombo.setSelectedItem(0);  // Default 0 sec incr

        // Add to UI
        customPanel.add(new JLabel("Minutes:"));
        customPanel.add(minCombo);
        customPanel.add(new JLabel("Increment (sec):"));
        customPanel.add(secCombo);

        JButton customBtn = new JButton("Start Custom");
        customBtn.addActionListener(e -> {
            int m = (Integer) minCombo.getSelectedItem();
            int s = (Integer) secCombo.getSelectedItem();
            selectedSettings = new TimeSettings(m, s);
            dispose();
        });
        customPanel.add(customBtn);

        // Layout
        JPanel center = new JPanel(new GridLayout(2, 1));
        center.add(presetsPanel);
        center.add(customPanel);
        add(center, BorderLayout.CENTER);
    }

    /**
     * Helper to add button.
     * @param panel presets panel
     * @param label text on button
     * @param min initial minutes
     * @param inc incrementing seconds
     */
    private void addPresetButton(JPanel panel, String label, int min, int inc) {
        JButton btn = new JButton(label);
        btn.addActionListener(e -> {
            selectedSettings = new TimeSettings(min, inc);
            dispose();
        });
        panel.add(btn);
    }

    /**
     * @return user's selected settings (null if aborted)
     */
    public TimeSettings getSelectedSettings() {
        return selectedSettings;
    }
}
