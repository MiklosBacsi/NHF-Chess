package view.game.annotation;

/**
 * Record for an arrow annotation (Model coordinates).
 * @param startRow start row of the arrow
 * @param startCol start column of the arrow
 * @param endRow end row of the arrow
 * @param endCol end column of the arrow
 * @param color color of the arrow
 * @author Miklós Bácsi
 */
public record ArrowAnnotation(
        int startRow,
        int startCol,
        int endRow,
        int endCol,
        AnnotationColor color
) {}
