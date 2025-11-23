package controller;

import view.MainFrame;

import javax.swing.*;

public class GameController {

    public static final String MENU = "MENU";
    public static final String GAME = "GAME";
    public static final String HISTORY = "HISTORY";
    public static final String[] MODES = {
            "Classical", "Fog of War", "Duck Chess", "Crazyhouse", "Chaturaji"
    };

    private MainFrame view;

    public GameController() {
        // View is initialized lazily or via setView to avoid circular constructor issues
        // but here we can instantiate the view directly
    }

    public void start() {
        this.view = new MainFrame(this);
        this.view.setVisible(true);
    }

    public void startNewGame(String mode) {
        System.out.println("Starting new game: " + mode);
        view.updateGameModeLabel(mode);
        showScene(GAME);
    }

    public void showScene(String scene) {
        view.showCard(scene);
    }

    // --- Dialog Logic ---

    public void openAboutDialog() {
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
            by Miklós Bácsi
            </div></html>
            """);
    }

    public void openRulesDialog(int index, String title) {
        String content = switch (index) {
            case 0 -> getClassicalRules();
            case 1 -> getFogOfWarRules();
            case 2 -> getDuckChessRules();
            case 3 -> getCrazyhouseRules();
            default -> getChaturajiRules();
        };
        openHelpDialog(title + " Rules", content);
    }

    public void openHelpDialog(String title, String content) {
        // Pass the view as the parent component
        JDialog dialog = new JDialog(view, title, false);
        dialog.setSize(800, 600);
        dialog.setLocationRelativeTo(view);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JTextPane pane = new JTextPane();
        pane.setContentType("text/html");
        pane.setText("<html><body style='font-family:Arial;padding:20px;line-height:1.6'>" + content + "</body></html>");
        pane.setEditable(false);
        pane.setCaretPosition(0);

        dialog.add(new JScrollPane(pane));
        dialog.setVisible(true);
    }

    // --- Rules Content (Model Data) ---

    public String getClassicalRules()   { return "<h2>Classical Chess</h2><p>Standard international chess rules as defined by FIDE.</p>"; }
    public String getFogOfWarRules()    { return "<h2>Fog of War</h2><p>You can only see squares attacked by your pieces.</p>"; }
    public String getDuckChessRules()   { return "<h2>Duck Chess</h2><p>After each move, a neutral duck is placed and blocks that square.</p>"; }
    public String getCrazyhouseRules()  { return "<h2>Crazyhouse</h2><p>Captured pieces can be dropped back onto the board.</p>"; }
    public String getChaturajiRules()   { return "<h2>Chaturaji</h2><p>Ancient 4-player Indian chess variant with dice and triumph rules.</p>"; }
    public String getClockGuide()       { return "<h2>Chess Clock Guide</h2><p>Explains time controls: Bullet, Blitz, Rapid, increment, etc.</p>"; }
}