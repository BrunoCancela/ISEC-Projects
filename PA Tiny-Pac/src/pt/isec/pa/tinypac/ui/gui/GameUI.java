package pt.isec.pa.tinypac.ui.gui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import pt.isec.pa.tinypac.model.ModelManager;
import pt.isec.pa.tinypac.model.fsm.TinyPacState;
import pt.isec.pa.tinypac.ui.gui.resources.CSSManager;

import java.util.Objects;

public class GameUI  extends BorderPane {


    MazeUI mazeUI;

    Button btnPause;
    Button btnResume;
    Button btnSaveGame;
    Button btnExitGame;
    RootPane rootPane;

    ModelManager modelManager;


    public GameUI(RootPane rootPane, boolean savedGame) {
        this.rootPane = rootPane;
        modelManager = new ModelManager();
        if(savedGame){
            modelManager.loadGame();
        }

        createViews();
        registerHandlers();
        update();

        Platform.runLater(this::requestFocus);
    }
    private void createViews() {

        CSSManager.applyCSS(this,"styles.css");

        btnPause = new Button("Pause");
        btnResume = new Button("Resume");
        btnSaveGame = new Button("Save Game");
        btnExitGame = new Button("Exit Game");

        btnResume.setDisable(true);
        btnSaveGame.setDisable(true);
        btnExitGame.setDisable(true);

        VBox buttonPane = new VBox(btnPause, btnResume, btnSaveGame, btnExitGame);
        buttonPane.setSpacing(10);
        buttonPane.setPadding(new Insets(10));

        buttonPane.setBorder(new Border(new BorderStroke(Color.YELLOW, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));


        mazeUI = new MazeUI(modelManager);

        this.setBackground(new Background(new BackgroundFill(Color.BLACK, null,null)));
        this.setLeft(buttonPane);
        this.setCenter(mazeUI);
    }

    private void registerHandlers() {

        modelManager.addPropertyChangeListener(modelManager.GAME_RUNNING,  evt -> Platform.runLater(()->update()));
        modelManager.addPropertyChangeListener(modelManager.GAME_END,  evt -> Platform.runLater(()->{
            rootPane.setCenter(new EnterScoreUI(rootPane, modelManager.getScore()));
            modelManager = null;
            this.getChildren().removeAll(this);
        }));

        btnPause.setOnAction(actionEvent -> modelManager.pause());

        btnSaveGame.setOnAction(actionEvent -> modelManager.saveGame());

        btnResume.setOnAction(actionEvent -> modelManager.resume());

        btnExitGame.setOnAction(actionEvent -> {
            modelManager.exitGame();
        });

        btnSaveGame.setFocusTraversable(false);
        btnPause.setFocusTraversable(false);
        btnResume.setFocusTraversable(false);

        rootPane.requestFocus();
        this.setOnKeyPressed(keyEvent -> {
            switch (keyEvent.getCode()) {
                case UP -> {
                    if (!btnPause.isDisabled()) {
                        System.out.println("UP");
                        modelManager.pressDirectionKey(1);
                    }
                }
                case DOWN -> {
                    if (!btnPause.isDisabled()) {
                        System.out.println("DOWN");
                        modelManager.pressDirectionKey(2);
                    }
                }
                case LEFT -> {
                    if (!btnPause.isDisabled()) {
                        System.out.println("LEFT");
                        modelManager.pressDirectionKey(3);
                    }
                }
                case RIGHT -> {
                    if (!btnPause.isDisabled()) {
                        System.out.println("RIGHT");
                        modelManager.pressDirectionKey(4);
                    }
                }
                case ESCAPE -> {
                    if (modelManager.getState() == TinyPacState.PAUSED_GAME) {
                        modelManager.resume();

                    } else {
                        modelManager.pause();
                    }
                }
                default -> {
                }
            }
        });
    }
    private void update() {

        //this.setVisible(true);

        if (Objects.requireNonNull(modelManager.getState()) == TinyPacState.PAUSED_GAME) {
            btnResume.setDisable(false);
            btnSaveGame.setDisable(false);
            btnExitGame.setDisable(false);
            btnPause.setDisable(true);
        } else {
            btnResume.setDisable(true);
            btnSaveGame.setDisable(true);
            btnExitGame.setDisable(true);
            btnPause.setDisable(false);
        }
        updateGridPane();
    }

    private void updateGridPane() {
        //mazeUI = (MazeUI) this.getCenter();
        mazeUI.getChildren().clear(); // Clear existing children
        mazeUI.addImagesToGrid(mazeUI);
    }
}
