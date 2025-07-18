package pt.isec.pa.tinypac.ui.gui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import pt.isec.pa.tinypac.Main;
import pt.isec.pa.tinypac.model.fsm.Context;


public class MainJFX extends Application {
    Context fsm;

    @Override
    public void init() throws Exception {
        super.init();
        fsm = Main.fsm;
    }

    @Override
    public void start(Stage stage) throws Exception {
        newStageForTesting(stage,"PacMan");
        //newStageForTesting(new Stage(),"PacMan#clone");
    }

    private void newStageForTesting(Stage stage, String title){
        RootPane root = new RootPane();
        Scene scene = new Scene(root,700,500, Color.BLACK);
        stage.setScene(scene);
        stage.setTitle(title);
        stage.setMinWidth(700);
        stage.setMinHeight(450);
        stage.show();
    }
}
