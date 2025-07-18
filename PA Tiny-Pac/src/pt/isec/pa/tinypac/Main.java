package pt.isec.pa.tinypac;

import javafx.application.Application;
import pt.isec.pa.tinypac.gameengine.GameEngine;
import pt.isec.pa.tinypac.model.fsm.Context;
import pt.isec.pa.tinypac.ui.text.TinyPacUI;
import pt.isec.pa.tinypac.ui.gui.MainJFX;

public class Main {
    /*
    public static void main(String[] args){

        Context fsm = new Context();
        TinyPacUI ui = new TinyPacUI(fsm);
        ui.start();
    }*/

    public static Context fsm;
    static {
        fsm = new Context();
    }

    public static void main(String[] args) {
        Application.launch(MainJFX.class,args);
    }
}