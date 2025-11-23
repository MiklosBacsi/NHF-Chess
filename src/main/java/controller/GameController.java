package controller;

import view.MainFrame;
import view.game.BoardTheme;

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

    public void setTheme(BoardTheme theme) {
        view.setBoardTheme(theme);
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
        dialog.setSize(800, 460);
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

    public String getClassicalRules() {
        return "<h2>Classical Chess</h2><p>Standard international chess rules as defined by FIDE (International Chess Federation).</p>";
    }

    public String getFogOfWarRules() {
        return "<h2>Fog of War</h2><p>You can only see squares occupied or attacked by your pieces. " +
            "There is no chess, so kings can also be captured. In this case, the game ends.</p>";
    }

    public String getDuckChessRules() {
        return "<h2>Duck Chess</h2><p>After each move, a neutral duck is placed on an empty square, which blocks that square. " +
            "The duck cannot be captured or moved through, and it has to be placed on a different square, than it previously has been.</p>";
    }
    public String getCrazyhouseRules() {
        return "<h2>Crazyhouse</h2><p>When capturing a piece of the enemy, it moves into our container. " +
            "Instead of making a move, you can choose to take a piece out of the container, and drop it back onto the board, in an empty square, but now as your piece.</p>";
    }

    public String getChaturajiRules() {
        return "<h2>Chaturaji</h2><p>Ancient 4-player Indian chess variant in which the goal is to collect to most points.</p>" +
            "<p>Each player has 4 pawns (do not have initial double-step), a king, a bishop, a knight, and a sail boat. The sail boat moves the same way as a rock, but cannot castle.<br>" +
            "Adjacent players move in perpendicular directions to each other, which introduces a twist in strategy. The order of moving is clockwise.</p>" +
            "<p>Kings can step into checks, and they are not obliged to block them, thus they can be captured.<br>" +
            "If a player's king is captured, he resigns or his time runs out, his rounds will be automatically skipped, but his pieces will remain on the board with grey colour, " +
            "which can be captured for +0 points. If the king stays on the board, its colour will be unchanged, and it can be captured for +3 points.<br>" +
            "The final player to stay alive will receive +3 points for every uncaptured enemy king remaining on the board.</p>" +
            "<p>Points:" +
            "<ul>" +
                "<li>1 point: capturing a pawn; simultaneously checking 2 kings</li>" +
                "<li>3 points: capturing a king or a knight</li>" +
                "<li>5 points: capturing a bishop or a sail boat; simultaneously checking 3 kings</li>" +
            "</ul></p>" +
            "<p>The game (automatically) ends, if 3 kings are captured, or if it is possible for only one player to win (calculated from points).<br>" +
            "The player with the most points wins, even if his king has been captured, or he resigned.</p>";
    }

    public String getClockGuide() {
        return "<h2>Chess Clock Guide</h2><p>Each player is given the equal amount of time at the start of the game, and only the active player's time is decreasing. " +
            "Before beginning the match, you must choose from a chess clock preset or you set a custom one yourself.</p>" +
            "<p><strong>minutes | seconds</strong> or just <strong>minutes</strong><br>" +
            "Minutes: integer between 1 and 30, the initial time.<br>" +
            "Seconds: integer between 0 and 30, that many seconds are added to your time after making a move.</p>" +
            "<p>If your time runs out, you lose/die.</p>";
    }
}