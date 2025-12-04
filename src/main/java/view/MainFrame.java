package view;

import controller.GameController;
import view.game.BoardTheme;
import view.game.GamePanel;
import view.history.HistoryPanel;
import view.menu.MainMenuPanel;

import javax.swing.*;
import java.awt.*;

/**
 * This class is responsible for the main frame (window) of the program.
 * @author Miklós Bácsi
 */
public class MainFrame extends JFrame {

    private final GameController controller;
    private final JPanel cards;
    private final GamePanel gamePanel;

    /**
     * Constructor that creates the various items.
     * @param controller controller of the program
     */
    public MainFrame(GameController controller) {
        this.controller = controller;

        setTitle("Chess Variants");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 740);
        setLocationRelativeTo(null);

        // Initialize Card Layout
        cards = new JPanel(new CardLayout());

        // ==== Create Screens ====
        // Menu: Passes the "Start Game" action to controller
        MainMenuPanel menuPanel = new MainMenuPanel(GameController.MODES, controller::startNewGame);

        // Game: Passes the "Back" action to controller
        gamePanel = new GamePanel(() -> controller.showScene(GameController.MENU));

        // History: Passes the "Back" action to controller
        HistoryPanel historyPanel = new HistoryPanel(() -> controller.showScene(GameController.MENU));

        // Add to Cards
        cards.add(menuPanel, GameController.MENU);
        cards.add(gamePanel, GameController.GAME);
        cards.add(historyPanel, GameController.HISTORY);

        add(cards);
        setJMenuBar(createMenuBar());

        pack();
        setMinimumSize(getSize());
    }

    /**
     * Starts a new game.
     * @param mode name of the chosen chess variant
     */
    public void startNewGame(String mode) {
        gamePanel.startNewGame(mode);
    }

    /**
     * Switches the view to the card.
     * @param cardName name of the card to show
     */
    public void showCard(String cardName) {
        CardLayout cl = (CardLayout) cards.getLayout();
        cl.show(cards, cardName);
    }

    /**
     * Creates the menu bar.
     * @return the created menu bar
     */
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
        for (String mode : GameController.MODES) {
            JMenuItem item = new JMenuItem(mode);
            item.addActionListener(e -> controller.startNewGame(mode));
            newGameMenu.add(item);
        }
        gameMenu.add(newGameMenu);
        gameMenu.addSeparator();

        JMenuItem mainMenuItem = new JMenuItem("Main Menu");
        mainMenuItem.addActionListener(e -> controller.showScene(GameController.MENU));
        gameMenu.add(mainMenuItem);
        menuBar.add(gameMenu);

        // View → Theme + Game History
        JMenu viewMenu = new JMenu("View");

        JMenuItem historyItem = new JMenuItem("Game History");
        historyItem.addActionListener(e -> controller.showScene(GameController.HISTORY));
        viewMenu.add(historyItem);

        JMenu themeMenu = new JMenu("Theme");
        ButtonGroup themeGroup = new ButtonGroup();
        for (BoardTheme theme : BoardTheme.ALL_THEMES) {
            JRadioButtonMenuItem themeItem = new JRadioButtonMenuItem(theme.getName());
            // Select Brown by default
            if (theme == BoardTheme.BROWN) themeItem.setSelected(true);

            themeItem.addActionListener(e -> controller.setTheme(theme));

            themeGroup.add(themeItem);
            themeMenu.add(themeItem);
        }
        viewMenu.add(themeMenu);
        viewMenu.addSeparator();

        menuBar.add(viewMenu);

        // Help → About + How to Play
        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutItem = new JMenuItem("About Chess Variants");
        aboutItem.setAccelerator(KeyStroke.getKeyStroke("F1"));
        aboutItem.addActionListener(e -> controller.openAboutDialog());
        helpMenu.add(aboutItem);
        helpMenu.addSeparator();

        JMenu howToPlayMenu = new JMenu("How to Play");
        String[] helpTitles = {"Classical Chess", "Fog of War", "Duck Chess", "Crazyhouse", "Chaturaji (4-player)"};
        for (int i = 0; i < helpTitles.length; i++) {
            String title = helpTitles[i];
            final int idx = i; // Capture for lambda
            JMenuItem item = new JMenuItem(title);
            item.addActionListener(e -> controller.openRulesDialog(idx, title));
            howToPlayMenu.add(item);
        }
        helpMenu.add(howToPlayMenu);
        helpMenu.addSeparator();

        JMenuItem clockItem = new JMenuItem("Chess Clock Guide");
        clockItem.addActionListener(e -> controller.openHelpDialog("Chess Clock Guide", controller.getClockGuide()));
        helpMenu.add(clockItem);
        menuBar.add(helpMenu);

        return menuBar;
    }

    /**
     * Sets the theme of the board to the given one.
     * @param theme the theme or the board is set to this
     */
    public void setBoardTheme(BoardTheme theme) {
        gamePanel.setBoardTheme(theme);
    }
}