package pt.isec.pa.tinypac.model.data.elements;

import pt.isec.pa.tinypac.model.data.IMazeElement;

public class Cave implements IMazeElement {
    public static final char SYMBOL = 'y';
    @Override
    public char getSymbol() {
        return ' ';
    }
}
