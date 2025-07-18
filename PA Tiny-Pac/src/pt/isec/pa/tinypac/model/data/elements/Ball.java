package pt.isec.pa.tinypac.model.data.elements;

import pt.isec.pa.tinypac.model.data.IMazeElement;

public class Ball implements IMazeElement {
    public static final char SYMBOL = 'o';
    @Override
    public char getSymbol() {
        return '.';
    }
}
