package pt.isec.pa.tinypac.model.data.elements;

import pt.isec.pa.tinypac.model.data.IMazeElement;

public class Empty implements IMazeElement {

    public static final char SYMBOL = ' ';

    @Override
    public char getSymbol() {
        return SYMBOL;
    }
}
