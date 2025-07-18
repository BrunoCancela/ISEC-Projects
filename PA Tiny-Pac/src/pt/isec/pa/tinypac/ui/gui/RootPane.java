package pt.isec.pa.tinypac.ui.gui;

import javafx.scene.layout.*;
import javafx.scene.paint.Color;



public class RootPane extends BorderPane{

    public RootPane(){
        createViews();
        registerHandlers();
        update();
    }
    private void createViews(){

        MenuUI menuUI = new MenuUI(this);

        this.setCenter(menuUI);

        menuUI.setBackground(new Background(new BackgroundFill(Color.BLACK,null,null)));
    }

    private void registerHandlers() {
    }
    private void update() {
    }
}
