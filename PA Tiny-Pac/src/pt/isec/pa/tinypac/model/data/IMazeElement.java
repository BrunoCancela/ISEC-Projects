package pt.isec.pa.tinypac.model.data;

import java.io.Serializable;

public interface IMazeElement extends Serializable{
    char getSymbol(); // returns the symbol of this element
    // The list of symbols is available
    // in the description of this work
}
