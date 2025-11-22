import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class ChessVariantsApplication extends JFrame {

    private final JPanel cards;
    private JLabel gameModeLabel;

    private static final String MENU = "MENU";
    private static final String GAME = "GAME";
    private static final String HISTORY = "HISTORY";

    private static final String[] MODES = {
            "Classical", "Fog of War", "Duck Chess", "Crazyhouse", "Chaturaji"
    };

    public ChessVariantsApplication() {
        setTitle("Chess Variants");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 740);
        setLocationRelativeTo(null);

        cards = new JPanel(new CardLayout());
        cards.add(createMenuPanel(), MENU);
        cards.add(createGamePanel(), GAME);
        cards.add(createHistoryPanel(), HISTORY);

        add(cards);
        setJMenuBar(createMenuBar());

        pack();
        setMinimumSize(getSize());
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // File → Exit
        JMenu fileMenu = new JMenu("File");
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(exitItem);
        menuBar.add(fileMenu);

        // Game → New Game + Main Menu
        JMenu gameMenu = new JMenu("Game");
        JMenu newGameMenu = new JMenu("New Game");
        for (String mode : MODES) {
            JMenuItem item = new JMenuItem(mode);
            item.addActionListener(e -> startNewGame(mode));
            newGameMenu.add(item);
        }
        gameMenu.add(newGameMenu);
        gameMenu.addSeparator();

        JMenuItem mainMenuItem = new JMenuItem("Main Menu");
        mainMenuItem.addActionListener(e -> showScene(MENU));
        gameMenu.add(mainMenuItem);
        menuBar.add(gameMenu);

        // View → Game History
        JMenu viewMenu = new JMenu("View");
        JMenuItem historyItem = new JMenuItem("Game History");
        historyItem.addActionListener(e -> showScene(HISTORY));
        viewMenu.add(historyItem);
        menuBar.add(viewMenu);

        // Help → About + How to Play
        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutItem = new JMenuItem("About Chess Variants");
        aboutItem.setAccelerator(KeyStroke.getKeyStroke("F1"));
        aboutItem.addActionListener(e -> openAboutDialog());
        helpMenu.add(aboutItem);
        helpMenu.addSeparator();

        JMenu howToPlayMenu = new JMenu("How to Play");
        String[] helpTitles = {"Classical Chess", "Fog of War", "Duck Chess", "Crazyhouse", "Chaturaji (4-player)"};
        for (int i = 0; i < helpTitles.length; i++) {
            String title = helpTitles[i];
            JMenuItem item = new JMenuItem(title);
            final int idx = i;
            item.addActionListener(e -> {
                String content = switch (idx) {
                    case 0 -> getClassicalRules();
                    case 1 -> getFogOfWarRules();
                    case 2 -> getDuckChessRules();
                    case 3 -> getCrazyhouseRules();
                    default -> getChaturajiRules();
                };
                openHelpDialog(title + " Rules", content);
            });
            howToPlayMenu.add(item);
        }
        helpMenu.add(howToPlayMenu);
        helpMenu.addSeparator();

        JMenuItem clockItem = new JMenuItem("Chess Clock Guide");
        clockItem.addActionListener(e -> openHelpDialog("Chess Clock Guide", getClockGuide()));
        helpMenu.add(clockItem);
        menuBar.add(helpMenu);

        return menuBar;
    }

    private JPanel createMenuPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(30, 35, 50));

        JLabel title = new JLabel("CHESS VARIANTS", SwingConstants.CENTER);
        title.setFont(new Font("Georgia", Font.BOLD, 64));
        title.setForeground(new Color(200, 220, 255));
        title.setBorder(BorderFactory.createEmptyBorder(60, 0, 70, 0));
        panel.add(title, BorderLayout.NORTH);

        // Main container with vertical BoxLayout
        JPanel mainContainer = new JPanel();
        mainContainer.setLayout(new BoxLayout(mainContainer, BoxLayout.Y_AXIS));
        mainContainer.setBackground(panel.getBackground());

        // === Row 1: Names ===
        JPanel nameRow = new JPanel(new GridLayout(1, 5, 60, 0));
        nameRow.setBackground(panel.getBackground());
        nameRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        // === Row 2: Boxes ===
        JPanel boxRow = new JPanel(new GridLayout(1, 5, 60, 0));
        boxRow.setBackground(panel.getBackground());

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
            box.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    startNewGame(selectedMode);
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
        centerWrapper.setBackground(panel.getBackground());

        // Add generous horizontal padding (left + right)
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.insets = new Insets(0, 50, 50, 50);  // 80px left, 80px right — looks luxurious
        centerWrapper.add(mainContainer, gbc);

        panel.add(centerWrapper, BorderLayout.CENTER);
        return panel;
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

    private JPanel createGamePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(25, 25, 40));

        JLabel title = new JLabel("CHESS GAME", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 48));
        title.setForeground(new Color(100, 200, 255));
        title.setBorder(BorderFactory.createEmptyBorder(40, 0, 20, 0));
        panel.add(title, BorderLayout.NORTH);

        gameModeLabel = new JLabel("Current Mode: No mode selected", SwingConstants.CENTER);
        gameModeLabel.setFont(new Font("Arial", Font.BOLD, 32));
        gameModeLabel.setForeground(Color.WHITE);

        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        center.add(gameModeLabel);
        panel.add(center, BorderLayout.CENTER);

        panel.add(createBackButton(), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(32, 64, 32));

        JLabel title = new JLabel("GAME HISTORY", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 50));
        title.setForeground(new Color(128, 255, 133));
        title.setBorder(BorderFactory.createEmptyBorder(60, 0, 40, 0));
        panel.add(title, BorderLayout.NORTH);

        JTextArea text = new JTextArea("No games played yet.\n\nYour completed games will appear here.");
        text.setEditable(false);
        text.setFont(new Font("Consolas", Font.PLAIN, 20));
        text.setForeground(Color.WHITE);
        text.setBackground(panel.getBackground());
        text.setBorder(BorderFactory.createEmptyBorder(20, 100, 100, 100));
        panel.add(text, BorderLayout.CENTER);

        panel.add(createBackButton(), BorderLayout.SOUTH);
        return panel;
    }

    private JComponent createBackButton() {
        JButton btn = new JButton("Back to Main Menu");
        btn.setFont(new Font("Arial", Font.BOLD, 20));
        btn.setForeground(Color.WHITE);
        btn.setBackground(new Color(100, 100, 150));
        btn.setPreferredSize(new Dimension(300, 60));
        btn.setFocusPainted(false);
        btn.addActionListener(e -> showScene(MENU));

        JPanel wrapper = new JPanel(new FlowLayout());
        wrapper.setBorder(BorderFactory.createEmptyBorder(20, 0, 30, 0));
        wrapper.setBackground(new Color(25, 25, 40));
        wrapper.add(btn);
        return wrapper;
    }

    private void startNewGame(String mode) {
        System.out.println("Starting new game: " + mode);
        gameModeLabel.setText("Current Mode: " + mode);
        showScene(GAME);
    }

    private void showScene(String scene) {
        CardLayout cl = (CardLayout) cards.getLayout();
        cl.show(cards, scene);
    }

    private void openAboutDialog() {
        openHelpDialog("About Chess Variants", """
            <html><div style='text-align:center;padding:30px;font-family:Arial'>
            <h1>Chess Variants</h1>
            <h3>Version 1.0</h3>
            <p>An amazing program that allows you to play chess variants locally with your friends, your cat (or with yourself),
            and you can analyze the matches manually with the help of annotations!</p>
            <br><b>Supported Variants:</b><br>
            Classical • Fog of War • Duck Chess<br>
            Crazyhouse • Chaturaji (4-player)<br><br>
            Made with Java Swing & ❤️<br>
            Made by Miklós Bácsi
            </div></html>
            """);
    }

    private void openHelpDialog(String title, String content) {
        JDialog dialog = new JDialog(this, title, false);
        dialog.setSize(800, 600);
        dialog.setLocationRelativeTo(this);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JTextPane pane = new JTextPane();
        pane.setContentType("text/html");
        pane.setText("<html><body style='font-family:Arial;padding:20px;line-height:1.6'>" + content + "</body></html>");
        pane.setEditable(false);
        pane.setCaretPosition(0);

        dialog.add(new JScrollPane(pane));
        dialog.setVisible(true);
    }

    private String getClassicalRules()   { return "<h2>Classical Chess</h2><p>Standard international chess rules as defined by FIDE.</p>"; }
    private String getFogOfWarRules()    { return "<h2>Fog of War</h2><p>You can only see squares attacked by your pieces.</p>"; }
    private String getDuckChessRules()   { return "<h2>Duck Chess</h2><p>After each move, a neutral duck is placed and blocks that square.</p>"; }
    private String getCrazyhouseRules()  { return "<h2>Crazyhouse</h2><p>Captured pieces can be dropped back onto the board.</p>"; }
    private String getChaturajiRules()   { return "<h2>Chaturaji</h2><p>Ancient 4-player Indian chess variant with dice and triumph rules.</p>"; }
    private String getClockGuide()       { return "<h2>Chess Clock Guide</h2><p>Explains time controls: Bullet, Blitz, Rapid, increment, etc.</p>"; }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            new ChessVariantsApplication().setVisible(true);
        });
    }
}