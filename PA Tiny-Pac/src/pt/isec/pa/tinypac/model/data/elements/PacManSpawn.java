package pt.isec.pa.tinypac.model.data.elements;

import pt.isec.pa.tinypac.model.data.IMazeElement;

public class PacManSpawn implements IMazeElement {
    public static final char SYMBOL = 'M';
    @Override
    public char getSymbol() {
        return ' ';
    }
}