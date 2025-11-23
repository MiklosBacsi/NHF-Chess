package view.menu;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.function.Consumer;

public class MainMenuPanel extends JPanel {

    private final String[] MODES;
    private final Consumer<String> onStartGame;
    private BufferedImage backgroundImage;

    public MainMenuPanel(String[] modes, Consumer<String> onStartGame) {
        this.MODES = modes;
        this.onStartGame = onStartGame;

        // Load the image
        try {
            URL imgUrl = getClass().getResource("/title-screen.png");
            if (imgUrl != null) {
                backgroundImage = ImageIO.read(imgUrl);
            } else {
                System.err.println("Could not find image: /title-screen.png");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        setLayout(new BorderLayout());
        // Fallback color if image fails to load
        setBackground(new Color(30, 35, 50));

        JLabel titleLabel = new JLabel("CHESS VARIANTS", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Georgia", Font.BOLD, 64));
        titleLabel.setForeground(Color.WHITE);

        // For dark background of title
        JPanel titleBox = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Same color as center panel for consistency
                g2d.setColor(new Color(0, 0, 0, 120));

                // Rounded rectangle
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30);

                super.paintComponent(g);
            }
        };
        titleBox.setOpaque(false);

        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        titleBox.add(titleLabel);

        // 3. The Wrapper to position the box on the screen (External spacing)
        // We use this to keep the "60px top, 70px bottom" spacing you had originally
        JPanel titleWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
        titleWrapper.setOpaque(false);
        titleWrapper.setBorder(BorderFactory.createEmptyBorder(60, 0, 70, 0));
        titleWrapper.add(titleBox);

        add(titleWrapper, BorderLayout.NORTH);

        initCenterPanel();
    }

    // Painting backgroun picture
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (backgroundImage != null) {
            Graphics2D g2d = (Graphics2D) g;

            // Enable smooth scaling
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            int panelWidth = getWidth();
            int panelHeight = getHeight();
            int imgWidth = backgroundImage.getWidth();
            int imgHeight = backgroundImage.getHeight();

            // Calculate the scale to COVER the panel (Math.max)
            double scaleX = (double) panelWidth / imgWidth;
            double scaleY = (double) panelHeight / imgHeight;
            double scale = Math.max(scaleX, scaleY);

            int newWidth = (int) (imgWidth * scale);
            int newHeight = (int) (imgHeight * scale);

            // Center the image (crop excess from sides/top/bottom)
            int x = (panelWidth - newWidth) / 2;
            int y = (panelHeight - newHeight) / 2;

            g2d.drawImage(backgroundImage, x, y, newWidth, newHeight, this);
        }
    }

    private void initCenterPanel() {
        JPanel mainContainer = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                // Enable antialiasing for smooth corners
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // 1. Set the semi-transparent dark color (120/255 opacity)
                g2d.setColor(new Color(0, 0, 0, 120));

                // 2. Draw a rounded rectangle behind the content
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 60, 60);

                super.paintComponent(g);
            }
        };

        mainContainer.setLayout(new BoxLayout(mainContainer, BoxLayout.Y_AXIS));
        mainContainer.setOpaque(false);

        // Add padding so the black box is bigger than the content inside
        mainContainer.setBorder(BorderFactory.createEmptyBorder(20, 40, 40, 40));

        // === Row 1: Names ===
        JPanel nameRow = new JPanel(new GridLayout(1, 5, 60, 0));
        nameRow.setOpaque(false);
        nameRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        // === Row 2: Boxes ===
        JPanel boxRow = new JPanel(new GridLayout(1, 5, 60, 0));
        boxRow.setOpaque(false);

        for (String mode : MODES) {
            // Create the Name Label
            JLabel nameLabel = new JLabel(mode, SwingConstants.CENTER);
            nameLabel.setForeground(Color.WHITE);
            nameLabel.setFont(new Font("Arial", Font.BOLD, 26));
            nameRow.add(nameLabel);

            // Load the Texture Image
            String filename = "/game-mode/" + mode.replace(" ", "-") + ".png";
            BufferedImage textureImg = null;
            try {
                URL url = getClass().getResource(filename);
                if (url != null) {
                    textureImg = ImageIO.read(url);
                } else {
                    System.err.println("Texture not found: " + filename);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            final BufferedImage finalTexture = textureImg;

            // Create the Box with Custom Image Painting
            JPanel box = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    if (finalTexture != null) {
                        Graphics2D g2d = (Graphics2D) g;
                        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                        // Draw image stretched to fit the 200x200 box
                        g2d.drawImage(finalTexture, 0, 0, getWidth(), getHeight(), this);
                    }
                }
            };

            // Set standard properties
            box.setPreferredSize(new Dimension(200, 200));
            box.setMaximumSize(new Dimension(200, 200));
            box.setBorder(BorderFactory.createRaisedBevelBorder());
            box.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            box.setToolTipText("Start " + mode);

            box.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    onStartGame.accept(mode);
                }
            });
            boxRow.add(box);
        }

        mainContainer.add(nameRow);
        mainContainer.add(Box.createVerticalStrut(25));
        mainContainer.add(boxRow);

        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        // Reduced the margins since the black box now has its own internal padding
        gbc.insets = new Insets(0, 20, 20, 20);
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