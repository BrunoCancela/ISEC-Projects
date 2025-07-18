package pt.isec.pa.tinypac.model.data.elements;

import pt.isec.pa.tinypac.model.data.IMazeElement;

public class Portal implements IMazeElement {
    public static final char SYMBOL = 'Y';
    @Override
    public char getSymbol() {
        return '-';
    }
}