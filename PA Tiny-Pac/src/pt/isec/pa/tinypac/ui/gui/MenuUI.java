package pt.isec.pa.tinypac.ui.gui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import pt.isec.pa.tinypac.model.FileManager;
import pt.isec.pa.tinypac.ui.gui.resources.CSSManager;
import pt.isec.pa.tinypac.ui.gui.resources.ImageManager;

public class MenuUI extends BorderPane {
    Button btnStart, btnTop5, btnExit, btnContinue;
    RootPane rootPane;

    FileManager fileManager;
    public MenuUI(RootPane rootPane){
        this.rootPane = rootPane;
        fileManager = new FileManager();
        createViews();
        registerHandlers();
        update();
    }
    private void createViews(){

        CSSManager.applyCSS(this,"styles.css");

        this.setStyle("-fx-background-color: black;");
        VBox menu = new VBox(20);
        Image titleImage = ImageManager.getImage("title.png");
        ImageView titleImageView = new ImageView();
        titleImageView.setImage(titleImage);

        menu.setAlignment(Pos.CENTER);

        // Create the image at the top of the menu
        Image icon = ImageManager.getImage("pacman.png");

        btnStart = createButton("START",icon);
        btnTop5 = createButton("TOP 5",icon);
        btnExit = createButton("EXIT GAME",icon);

        VBox buttonBox = new VBox(10);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(10));

        if(fileManager.getSavedGame()){
            btnContinue = createButton("CONTINUE GAME",icon);
            buttonBox.getChildren().addAll(btnStart,btnContinue,btnTop5,btnExit);
        }else {
            buttonBox.getChildren().addAll(btnStart, btnTop5, btnExit);
        }

        Label footer = new Label("DEIS-ISEC-IPC LEI PA 2022-2023 \n     Bruno Cancela 2020131288\n           Trabalho Académico");
        footer.getStyleClass().add("footer-label");
        Image image = ImageManager.getImage("isec_logo.png");
        ImageView imageFooter = new ImageView(image);

        assert image != null;
            imageFooter.setFitWidth(image.getWidth() / 3);
            imageFooter.setFitHeight(image.getHeight() / 3);

        HBox footerBox = new HBox(10, imageFooter, footer);
        footerBox.setAlignment(Pos.CENTER);
        footerBox.getStyleClass().add("footer-box");

        menu.getChildren().addAll(titleImageView,buttonBox, footerBox);

        this.setCenter(menu);
    }

    private Button createButton(String text, Image icon) {
        Button button = new Button(text);
        ImageView imageView = new ImageView(icon);
        imageView.setFitWidth(14);  // Adjust the desired width
        imageView.setFitHeight(14); // Adjust the desired height

        StackPane graphicContainer = new StackPane(imageView);
        graphicContainer.setVisible(false);

        button.setGraphic(graphicContainer);

        button.setOnMouseEntered(event -> graphicContainer.setVisible(true));

        button.setOnMouseExited(event -> graphicContainer.setVisible(false));

        button.setOnMousePressed(event -> {
            Image newIcon = ImageManager.getImage("blinky.png"); // Replace with the path to your new icon
            imageView.setImage(newIcon);
        });
        button.setOnMouseReleased(event -> imageView.setImage(icon));

        return button;
    }
    private void registerHandlers() {
        btnStart.setOnAction( event -> {
            fileManager.newGameDeleteSavedGame();
            rootPane.setCenter(new GameUI(rootPane, false));
            this.getChildren().clear();
            this.getChildren().removeAll(this);
        });

        btnTop5.setOnAction( event -> {
            rootPane.setCenter(new Top5UI(rootPane));
            this.getChildren().clear();
            this.getChildren().removeAll(this);
        });

        btnExit.setOnAction( event -> Platform.exit());

        if(fileManager.getSavedGame()) {
            btnContinue.setOnAction(event -> {
                rootPane.setCenter(new GameUI(rootPane, true));
                this.getChildren().clear();
                this.getChildren().removeAll(this);
            });
        }
    }
    private void update() {
        setVisible(true);
    }
}
