package pt.isec.pa.tinypac.model.fsm;

import java.io.Serializable;

public interface ITinyPacState extends Serializable {

    boolean pressDirectionKey();
    boolean evolve();
    boolean exitGame();
    boolean pause();
    boolean resume();
    TinyPacState getState();
}
