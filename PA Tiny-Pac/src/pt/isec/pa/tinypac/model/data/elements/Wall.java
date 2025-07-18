package pt.isec.pa.tinypac.model.data.elements;

import pt.isec.pa.tinypac.model.data.IMazeElement;

public class Wall implements IMazeElement {
    public static final char SYMBOL = 'x';
    @Override
    public char getSymbol() {
        return SYMBOL;
    }
}
