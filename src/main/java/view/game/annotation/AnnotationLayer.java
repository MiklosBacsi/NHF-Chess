package view.game.annotation;

import java.util.HashSet;
import java.util.Set;

/**
 * This class stores the square and arrow annotations for a single move (for a single player).
 * @see AnnotationColor
 * @see SquareAnnotation
 * @see ArrowAnnotation
 * @see view.game.BoardPanel
 * @author Miklós Bácsi
 */
public class AnnotationLayer {
    private final Set<SquareAnnotation> squares = new HashSet<>();
    private final Set<ArrowAnnotation> arrows = new HashSet<>();

    /**
     * Getter
     * @return square annotations of a move
     */
    public Set<SquareAnnotation> getSquares() {
        return squares;
    }

    /**
     * Getter
     * @return arrow annotations of a move
     */
    public Set<ArrowAnnotation> getArrows() {
        return arrows;
    }

    /**
     * Clears both the square and arrow annotations.
     */
    public void clear() {
        squares.clear();
        arrows.clear();
    }

    /**
     * Toggles the square annotation.
     * It draws a new square annotation to the given place with the given color.
     * If an identical annotation already exists, then it gets removed (and the new one is not added either).
     * @param row row of the square
     * @param col column of the square
     * @param color color of the annotation
     */
    public void toggleSquare(int row, int col, AnnotationColor color) {
        SquareAnnotation newAnnot = new SquareAnnotation(row, col, color);

        // Check if exact match exists
        boolean exactMatch = squares.contains(newAnnot);

        // Remove any on this square
        squares.removeIf(s -> s.row() == row && s.col() == col);

        // If it wasn't an exact match, add the new one
        if (!exactMatch) {
            squares.add(newAnnot);
        }
    }

    /**
     * Toggles the arrow annotation.
     * It draws a new arrow annotation to the given start and end place with the given color.
     * If an identical annotation already exists, then it gets removed (and the new one is not added either).
     * @param sr start row
     * @param sc start column
     * @param er end row
     * @param ec end column
     * @param color annotation color
     */
    public void toggleArrow(int sr, int sc, int er, int ec, AnnotationColor color) {
        ArrowAnnotation newArrow = new ArrowAnnotation(sr, sc, er, ec, color);

        // Check if exact match exists
        boolean exactMatch = arrows.contains(newArrow);

        // Remove any on this place
        arrows.removeIf(
                a -> a.startRow() == sr && a.startCol() == sc && a.endRow() == er && a.endCol() == ec
        );

        // If it wasn't an exact match, add the new one
        if (!exactMatch) {
            arrows.add(newArrow);
        }
    }
}