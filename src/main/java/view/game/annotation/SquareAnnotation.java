package view.game.annotation;

/**
 * Record for a highlighted square (annotation) (Model coordinates).
 * @param row row of the highlighted square
 * @param col column of the highlighted square
 * @param color color of the highlighted square
 * @see AnnotationColor
 * @see AnnotationLayer
 * @see ArrowAnnotation
 * @author Miklós Bácsi
 */
public record SquareAnnotation(
        int row,
        int col,
        AnnotationColor color
) {}
