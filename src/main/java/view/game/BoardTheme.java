package view.game;

import java.awt.Color;

/**
 * This class implements the various visual themes of the board.
 * @author Miklós Bácsi
 */
public class BoardTheme {

    private final String name;
    private final Color light;
    private final Color dark;

    /**
     * Constructor that initializes values.
     * @param name name of the theme
     * @param light color of the light squares
     * @param dark color of the dar squares
     */
    public BoardTheme(String name, Color light, Color dark) {
        this.name = name;
        this.light = light;
        this.dark = dark;
    }

    /**
     * @return the name of the theme
     */
    public String getName() {
        return name;
    }

    /**
     * @return the color of the light squares
     */
    public Color getLight() {
        return light;
    }

    /**
     * @return the color of the dark squares
     */
    public Color getDark() {
        return dark;
    }

    public static final BoardTheme BROWN = new BoardTheme("Brown", new Color(240, 217, 181), new Color(181, 136, 99));
    public static final BoardTheme GREEN = new BoardTheme("Green", new Color(235, 236, 208), new Color(115, 149, 82));
    public static final BoardTheme GREY  = new BoardTheme("Grey",  new Color(218, 218, 218), new Color(173, 173, 173));

    public static final BoardTheme[] ALL_THEMES = {BROWN, GREEN, GREY};
}
