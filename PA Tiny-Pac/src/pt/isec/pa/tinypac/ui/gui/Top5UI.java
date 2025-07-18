package pt.isec.pa.tinypac.ui.gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import pt.isec.pa.tinypac.model.FileManager;
import pt.isec.pa.tinypac.ui.gui.resources.CSSManager;

public class Top5UI extends BorderPane{
    RootPane rootPane;
    Button goBackButton;
    FileManager fileManager;


    public Top5UI(RootPane rootPane) {
        this.rootPane = rootPane;
        fileManager = new FileManager();
        createViews();
        registerHandlers();
        //update();
    }


    private void createViews() {
        CSSManager.applyCSS(this,"styles.css");

        VBox centerBox = new VBox();
        centerBox.setSpacing(10);
        centerBox.setPadding(new Insets(10));

        Label scoreLabel = new Label("Top 5: ");
        scoreLabel.setStyle("    -fx-font-family: Verdana;-fx-text-fill: yellow; -fx-font-size: 20px;-fx-font-weight: bold;");
        goBackButton = new Button("OK");

        Label scoreBoard = new Label(fileManager.readScoreBoard());
        scoreBoard.setStyle("    -fx-font-family: Verdana;-fx-text-fill: yellow; -fx-font-size: 16px;-fx-font-weight: bold;");

        centerBox.getChildren().addAll(scoreLabel,scoreBoard, goBackButton);
        centerBox.setPrefWidth(300);
        centerBox.setAlignment(Pos.CENTER);

        VBox outerBox = new VBox(centerBox);
        outerBox.setBackground(new Background(new BackgroundFill(Color.BLACK, null, null)));
        centerBox.setBorder(new Border(new BorderStroke(Color.YELLOW, BorderStrokeStyle.SOLID, new CornerRadii(10), BorderWidths.DEFAULT)));
        outerBox.setAlignment(Pos.CENTER);
        this.setCenter(outerBox);
    }

    private void registerHandlers() {
        goBackButton.setOnAction( event -> {
            rootPane.setCenter(new MenuUI(rootPane));
            this.getChildren().clear();
            this.getChildren().removeAll(this);
        });
    }
}
