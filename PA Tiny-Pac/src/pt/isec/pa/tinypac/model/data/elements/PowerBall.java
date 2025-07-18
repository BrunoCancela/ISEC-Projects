package pt.isec.pa.tinypac.model.data.elements;

import pt.isec.pa.tinypac.model.data.IMazeElement;

public class PowerBall implements IMazeElement {
    public static final char SYMBOL = 'O';
    @Override
    public char getSymbol() {
        return SYMBOL;
    }
}
