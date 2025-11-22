package view.menu;

//import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
//import java.awt.image.BufferedImage;
//import java.io.IOException;
//import java.net.URL;
import java.util.function.Consumer;

public class MainMenuPanel extends JPanel {

    private final String[] MODES;
    private final Consumer<String> onStartGame;

    public MainMenuPanel(String[] modes, Consumer<String> onStartGame) {
        this.MODES = modes;
        this.onStartGame = onStartGame;

        setLayout(new BorderLayout());
        setBackground(new Color(30, 35, 50));

        JLabel title = new JLabel("CHESS VARIANTS", SwingConstants.CENTER);
        title.setFont(new Font("Georgia", Font.BOLD, 64));
        title.setForeground(new Color(200, 220, 255));
        title.setBorder(BorderFactory.createEmptyBorder(60, 0, 70, 0));
        add(title, BorderLayout.NORTH);

        initCenterPanel();
    }

    private void initCenterPanel() {
        // Main container with vertical BoxLayout
        JPanel mainContainer = new JPanel();
        mainContainer.setLayout(new BoxLayout(mainContainer, BoxLayout.Y_AXIS));
        mainContainer.setBackground(getBackground());

        // === Row 1: Names ===
        JPanel nameRow = new JPanel(new GridLayout(1, 5, 60, 0));
        nameRow.setBackground(getBackground());
        nameRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        // === Row 2: Boxes ===
        JPanel boxRow = new JPanel(new GridLayout(1, 5, 60, 0));
        boxRow.setBackground(getBackground());

        for (String mode : MODES) {
            // Name label
            JLabel nameLabel = new JLabel(mode, SwingConstants.CENTER);
            nameLabel.setForeground(Color.WHITE);
            nameLabel.setFont(new Font("Arial", Font.BOLD, 26));
            nameRow.add(nameLabel);

            // Colored box
            JPanel box = new JPanel();
            box.setPreferredSize(new Dimension(200, 200));
            box.setMaximumSize(new Dimension(200, 200));
            box.setBackground(getColorForMode(mode));
            box.setBorder(BorderFactory.createRaisedBevelBorder());
            box.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            box.setToolTipText("Start " + mode);

            String selectedMode = mode;
            box.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    // Call the controller action
                    onStartGame.accept(selectedMode);
                }
            });

            boxRow.add(box);
        }

        // Add rows with controlled vertical gap
        mainContainer.add(nameRow);
        mainContainer.add(Box.createVerticalStrut(25));
        mainContainer.add(boxRow);

        // Center everything with nice side margins
        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setBackground(getBackground());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.insets = new Insets(0, 50, 50, 50);
        centerWrapper.add(mainContainer, gbc);

        add(centerWrapper, BorderLayout.CENTER);
    }

    private Color getColorForMode(String mode) {
        return switch (mode) {
            case "Classical"    -> new Color(100, 149, 237);
            case "Fog of War"   -> new Color(75, 75, 100);
            case "Duck Chess"   -> new Color(255, 193, 7);
            case "Crazyhouse"   -> new Color(220, 20, 60);
            case "Chaturaji"    -> new Color(138, 43, 226);
            default             -> Color.DARK_GRAY;
        };
    }
}