package pt.isec.pa.tinypac.ui.gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import pt.isec.pa.tinypac.model.FileManager;
import pt.isec.pa.tinypac.ui.gui.resources.CSSManager;


public class EnterScoreUI extends BorderPane {
    private final int score;
    RootPane rootPane;
    Button okButton;
    Button saveButton;
    TextField textField;
    FileManager fileManager;


    public EnterScoreUI(RootPane rootPane,int score) {
        fileManager = new FileManager();
        this.rootPane = rootPane;
        this.score = score;
        createViews();
        registerHandlers();
        //update();
    }


    private void createViews() {
        CSSManager.applyCSS(this,"styles.css");

        VBox centerBox = new VBox();
        centerBox.setSpacing(10);
        centerBox.setPadding(new Insets(10));

        Label scoreLabel = new Label("Score: "+score);
        scoreLabel.setStyle("    -fx-font-family: Verdana;-fx-text-fill: yellow; -fx-font-size: 16px;-fx-font-weight: bold;");
        okButton = new Button("OK");

        if(fileManager.isInTop5(score)){
            String pattern = "^[a-zA-Z]{0,20}$";
            textField = new TextField();
            textField.setPromptText("(min 3, max 20 characters)");
            textField.setMaxWidth(200);
            TextFormatter<String> textFormatter = new TextFormatter<>(change -> {
                String newText = change.getControlNewText();
                if (newText.matches(pattern)) {
                    return change;
                }
                return null; // Reject the change
            });

            textField.setTextFormatter(textFormatter);
            saveButton = new Button("Save");
            centerBox.getChildren().addAll(scoreLabel, textField, saveButton, okButton);
        }else{
            centerBox.getChildren().addAll(scoreLabel, okButton);
        }
        centerBox.setMaxWidth(300);
        centerBox.setAlignment(Pos.CENTER);

        VBox outerBox = new VBox(centerBox);
        outerBox.setBackground(new Background(new BackgroundFill(Color.BLACK, null, null)));
        centerBox.setBorder(new Border(new BorderStroke(Color.YELLOW, BorderStrokeStyle.SOLID, new CornerRadii(10), BorderWidths.DEFAULT)));
        outerBox.setAlignment(Pos.CENTER);
        this.setCenter(outerBox);

    }

    private void registerHandlers() {
        okButton.setOnAction( event -> {
            rootPane.setCenter(new MenuUI(rootPane));
            this.getChildren().clear();
            this.getChildren().removeAll(this);
        });

        if(fileManager.isInTop5(score)){
            saveButton.setOnAction(event -> {
                if(textField.getText().length() >= 3) {
                    fileManager.saveInTop5(textField.getText(), score);
                    rootPane.setCenter(new MenuUI(rootPane));
                    this.getChildren().clear();
                    this.getChildren().removeAll(this);
                }else{
                    textField.setStyle("-fx-background-color: #fce886; -fx-border-radius: 4px; -fx-border-color: red");
                }
            });
        }
    }
}
