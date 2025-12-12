package view.game.annotation;

import java.awt.Color;

/**
 * This enum stores the colors for the annotations.
 * @see SquareAnnotation
 * @see ArrowAnnotation
 * @see AnnotationLayer
 * @author Miklós Bácsi
 */
public enum AnnotationColor {
    RED(new Color(235, 97, 80, 200)),
    GREEN(new Color(157, 188, 0, 200)),
    BLUE(new Color(82, 176, 220, 200)),
    ORANGE(new Color(255, 170, 0, 200));

    private final Color color;

    /**
     * Constructor that initializes its color.
     * @param color color of the annotation
     */
    AnnotationColor(Color color) {
        this.color = color;
    }

    /**
     * Helper to get awt color of annotation.
     * @return color of the annotation
     */
    public Color getAwtColor() {
        return color;
    }
}