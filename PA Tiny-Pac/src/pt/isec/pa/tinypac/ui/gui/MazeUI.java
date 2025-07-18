package pt.isec.pa.tinypac.ui.gui;


import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import pt.isec.pa.tinypac.model.ModelManager;
import pt.isec.pa.tinypac.ui.gui.resources.ImageManager;

public class MazeUI extends GridPane {

    private static final int SQUARE_SIZE = 10; // Size of each square in pixels

    private final Image wallImage;
    private final Image emptyImage;
    private final Image ballImage;
    private final Image powerImage;
    private final Image blinkyImage;
    private final Image clydeImage;
    private final Image inkyImage;
    private final Image pinkyImage;
    private final Image scaredGhost;
    private final Image pacManImage;
    private final Image warpImage;
    private final Image portalImage;
    private final Image fruteImage;
    ModelManager modelManager;
    public MazeUI(ModelManager modelManager){

        wallImage = ImageManager.getImage("wall.png");
        emptyImage = ImageManager.getImage("empty.png");
        ballImage = ImageManager.getImage("ball.png");
        powerImage = ImageManager.getImage("power.png");
        blinkyImage = ImageManager.getImage("blinky.png");
        inkyImage = ImageManager.getImage("inky.png");
        clydeImage = ImageManager.getImage("clyde.png");
        pinkyImage = ImageManager.getImage("pinky.png");
        scaredGhost = ImageManager.getImage("scaredGhost.png");
        pacManImage = ImageManager.getImage("pacman.png");
        warpImage = ImageManager.getImage("warp.png");
        portalImage = ImageManager.getImage("portal.png");
        fruteImage = ImageManager.getImage("frute.png");


        this.modelManager = modelManager;
        this.setAlignment(Pos.CENTER);
        this.setPadding(new Insets(10));
        this.setHgap(2);
        this.setVgap(2);
        addImagesToGrid(this);
    }
    public void addImagesToGrid(GridPane gridPane) {
        String[] lines = modelManager.getMapLevel().split("\n");

        GridPane labelPane = new GridPane();
        labelPane.setHgap(10);

// Create labels for level, score, and lives
        Label levelLabel = new Label("Lvl: "+ modelManager.getLevel());
        Label scoreLabel = new Label("Score: "+ modelManager.getScore());
        Label livesLabel = new Label("Lives:"+ modelManager.getLives());

// Create labels for level value, score value, and lives value

// Add labels and their values to the labelPane
        labelPane.add(levelLabel, 0, 0);
        labelPane.add(scoreLabel, 2, 0);
        labelPane.add(livesLabel, 4, 0);
        labelPane.setAlignment(Pos.CENTER);
        labelPane.setBackground(
                new Background(
                        new BackgroundFill(Color.YELLOW,null,null)
                )
        );

        gridPane.addRow(0, labelPane);

        GridPane map = new GridPane();
        map.setAlignment(Pos.CENTER);
        map.setPadding(new Insets(10));
        map.setHgap(2);
        map.setVgap(2);

        gridPane.addRow(1, map);

        for (int row = 1; row <= lines.length; row++) {
            String line = lines[row-1];

            for (int col = 0; col < line.length(); col++) {
                char ch = line.charAt(col);
                ImageView imageView = createImageViewForChar(ch);
                map.add(imageView, col, row);
            }
        }
    }

    private ImageView createImageViewForChar(char ch) {
        ImageView imageView = new ImageView();

        if (ch == 'x') {
            imageView.setImage(wallImage);
        } else if (ch == '.') {
            imageView.setImage(ballImage);
        } else if (ch == 'O') {
            imageView.setImage(powerImage);
        } else if (ch == 'B') {
            imageView.setImage(blinkyImage);
        }else if (ch == 'C') {
            imageView.setImage(clydeImage);
        }else if (ch == 'I') {
            imageView.setImage(inkyImage);
        }else if (ch == 'P') {
            imageView.setImage(pinkyImage);
        } else if (ch == 'M') {
            imageView.setImage(pacManImage);
        } else if (ch == 'W') {
            imageView.setImage(warpImage);
        }else if (ch == 'b' || ch == 'c' || ch == 'i' || ch == 'p') {
            imageView.setImage(scaredGhost);
        }else if (ch == '-') {
            imageView.setImage(portalImage);
        }else if (ch == 'F') {
            imageView.setImage(fruteImage);
        } else {
            imageView.setImage(emptyImage);
        }

        imageView.setFitWidth(SQUARE_SIZE);
        imageView.setFitHeight(SQUARE_SIZE);
        return imageView;
    }
}
