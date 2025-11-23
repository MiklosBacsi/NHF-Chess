package view.game;

import java.awt.Color;

public class BoardTheme {

    private final String name;
    private final Color light;
    private final Color dark;

    public BoardTheme(String name, Color light, Color dark) {
        this.name = name;
        this.light = light;
        this.dark = dark;
    }

    public String getName() {
        return name;
    }

    public Color getLight() {
        return light;
    }

    public Color getDark() {
        return dark;
    }

    public static final BoardTheme BROWN = new BoardTheme("Brown", new Color(240, 217, 181), new Color(181, 136, 99));
    public static final BoardTheme GREEN = new BoardTheme("Green", new Color(235, 236, 208), new Color(115, 149, 82));
    public static final BoardTheme GREY  = new BoardTheme("Grey",  new Color(218, 218, 218), new Color(173, 173, 173));

    public static final BoardTheme[] ALL_THEMES = {BROWN, GREEN, GREY};
}
