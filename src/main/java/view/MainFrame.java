package view;

import controller.GameController;
import view.game.GamePanel;
import view.history.HistoryPanel;
import view.menu.MainMenuPanel;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {

    private final GameController controller;
    private final JPanel cards;
    private GamePanel gamePanel;

    public MainFrame(GameController controller) {
        this.controller = controller;

        setTitle("Chess Variants");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 740);
        setLocationRelativeTo(null);

        // Initialize Card Layout
        cards = new JPanel(new CardLayout());

        // Create Screens
        // 1. Menu: Passes the "Start Game" action to controller
        MainMenuPanel menuPanel = new MainMenuPanel(GameController.MODES, controller::startNewGame);

        // 2. Game: Passes the "Back" action to controller
        gamePanel = new GamePanel(() -> controller.showScene(GameController.MENU));

        // 3. History: Passes the "Back" action to controller
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

    // Updates the text on the game screen
    public void updateGameModeLabel(String mode) {
        gamePanel.setGameModeText("Current Mode: " + mode);
    }

    // Switches the view
    public void showCard(String cardName) {
        CardLayout cl = (CardLayout) cards.getLayout();
        cl.show(cards, cardName);
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

        // View → Game History
        JMenu viewMenu = new JMenu("View");
        JMenuItem historyItem = new JMenuItem("Game History");
        historyItem.addActionListener(e -> controller.showScene(GameController.HISTORY));
        viewMenu.add(historyItem);
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
}