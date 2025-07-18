package pt.isec.pa.tinypac.model.data;

import pt.isec.pa.tinypac.model.data.elements.*;
import pt.isec.pa.tinypac.model.data.ghost.Blinky;
import pt.isec.pa.tinypac.model.data.ghost.Clyde;
import pt.isec.pa.tinypac.model.data.ghost.Inky;
import pt.isec.pa.tinypac.model.data.ghost.Pinky;

import java.io.*;
import java.util.Random;
import java.util.Scanner;

public class Data implements Serializable {
    //Saves the state where it was previous before the pause
    //TODO está mal que doi deves guardar um enum no PausedGameState
    private Maze maze;
    private int currentLvl;
    private String file;
    private int timer;
    private boolean restartLvl;
    private int multiplier;
    private boolean power;
    private int score;
    private int lives;
    private final PacMan pacMan;
    private final Blinky blinky;
    private final Clyde clyde;
    private final Inky inky;
    private final Pinky pinky;
    private int fruteMultiplier;
    private int ballForFrute;
    private int fruteX, fruteY;

    public Data(){
        currentLvl = 1;
        pacMan = new PacMan();
        blinky = new Blinky();
        clyde = new Clyde();
        inky = new Inky();
        pinky = new Pinky();
        file = "Level01.txt";
        readFile();
        timer = 0;
        score = 0;
        multiplier = 1;
        restartLvl = false;
        lives = 3;
        setMovingElements();
        fruteMultiplier = 1;
    }
    public int getLevel() {
        return currentLvl;
    }
    public String getMapLevel() {
        StringBuilder sb = new StringBuilder();
        char[][] lvl = maze.getMaze();
        for (int y = 0; y < lvl.length; y++) {
            for (int x = 0; x < lvl[0].length; x++) {
                if(x == pacMan.getX() && y == pacMan.getY()){
                    sb.append(pacMan.getSymbol());
                }else if(x == blinky.getX() && y == blinky.getY()){
                    sb.append(blinky.getSymbol());
                }else if(x == clyde.getX() && y == clyde.getY()){
                    sb.append(clyde.getSymbol());
                }else if(x == inky.getX() && y == inky.getY()){
                    sb.append(inky.getSymbol());
                }else if(x == pinky.getX() && y == pinky.getY()){
                    sb.append(pinky.getSymbol());
                }else {
                    sb.append(lvl[y][x]);
                }
            }
            sb.append("\n");
        }
        return sb.toString();
    }
    public void changeDiretion(int i) {
        pacMan.changeDirection(i, maze);
    }
    public void setMovingElements(){
        char[][] lvl = maze.getMaze();
        boolean findCave = false;
        int[] topLeftCave = new int[2];
        int[] bottomRightCave= new int[2];
        for (int y = 0; y < lvl.length; y++) {
            for (int x = 0; x < lvl[0].length; x++) {
                if(maze.get(y,x) instanceof PacManSpawn){
                    pacMan.setSpawn(x,y);
                }
                if(maze.get(y,x) instanceof Cave){
                    if(!findCave) {
                        findCave = true;
                        topLeftCave[0] = x;
                        topLeftCave[1] = y;
                    }else {
                        bottomRightCave[0] = x;
                        bottomRightCave[1] = y;
                    }
                }
            }
        }
        spawnGhost(topLeftCave, bottomRightCave);
    }
    private void spawnGhost(int[] topLeftCave, int[] bottomRightCave) {
        Random random = new Random();
        int[][] ghostCoordinates = new int[4][2];

        int ghostIndex = 0;
        while (ghostIndex < 4) {
            int x = random.nextInt(bottomRightCave[0] - topLeftCave[0] + 1) + topLeftCave[0];
            int y = random.nextInt(bottomRightCave[1] - topLeftCave[1] + 1) + topLeftCave[1];

            boolean isSamePosition = false;
            for (int i = 0; i < ghostIndex; i++) {
                if (ghostCoordinates[i][0] == x && ghostCoordinates[i][1] == y) {
                    isSamePosition = true;
                    break;
                }
            }

            if (!isSamePosition) {
                ghostCoordinates[ghostIndex][0] = x;
                ghostCoordinates[ghostIndex][1] = y;
                ghostIndex++;
            }
        }

        blinky.setSpawn(ghostCoordinates[0][0], ghostCoordinates[0][1]);
        clyde.setSpawn(ghostCoordinates[1][0],ghostCoordinates[1][1]);
        inky.setSpawn(ghostCoordinates[2][0],ghostCoordinates[2][1]);
        pinky.setSpawn(ghostCoordinates[3][0],ghostCoordinates[3][1]);
    }

    public boolean evolve(){
        power = false;
        if(timer > 0){
            timer--;
        }
        if (pacMan.getDirection() > 0) {
            pacMan.move(maze);
        }

        eatOrBeEated();

        blinky.move(maze,pacMan);
        clyde.move(maze,pacMan);
        pinky.move(maze,pacMan);
        inky.move(maze,pacMan);

        eatOrBeEated();

        if(getMapLevel().contains("F")){
            ballForFrute = 0;
        }
        if(maze.get(pacMan.getY(),pacMan.getX()) instanceof Ball){
            if(ballForFrute >= 20){
                maze.set(fruteY,fruteX,new FruteSpawn());
            }
            ballForFrute++;
            score++;
            maze.set(pacMan.getY(), pacMan.getX(), new Empty());

        }else if(maze.get(pacMan.getY(),pacMan.getX()) instanceof PowerBall){
            score+=10;
            maze.set(pacMan.getY(), pacMan.getX(), new Empty());
            power = true;
        }else if(maze.get(pacMan.getY(),pacMan.getX()) instanceof FruteSpawn){
            score+=fruteMultiplier*currentLvl*25;
            fruteMultiplier++;
            maze.set(pacMan.getY(), pacMan.getX(), new Empty());
        }

        return !levelEmpty();
    }
    private void eatOrBeEated() {
        if(pacMan.getX() == blinky.getX() && pacMan.getY() == blinky.getY()){
            if(blinky.isVulnerable()){
                score += 50*multiplier;
                multiplier++;
                blinky.resetPostion();
            }else{
                restartLvl = true;
                lives--;
                return;
            }

        }
        if(pacMan.getX() == clyde.getX() && pacMan.getY() == clyde.getY()){
            if(clyde.isVulnerable()){
                score += 50*multiplier;
                multiplier++;
                clyde.resetPostion();
            }else{
                restartLvl = true;
                lives--;
            }
        }
        if(pacMan.getX() == inky.getX() && pacMan.getY() == inky.getY()){
            if(inky.isVulnerable()){
                inky.resetPostion();
                score += 50*multiplier;
                multiplier++;
            }else{
                restartLvl = true;
                lives--;
                return;
            }
        }
        if(pacMan.getX() == pinky.getX() && pacMan.getY() == pinky.getY()){
            if(pinky.isVulnerable()){
                pinky.resetPostion();
                score += 50*multiplier;
                multiplier++;
            }else{
                restartLvl = true;
                lives--;
            }
        }
    }
    private boolean levelEmpty() {
        char[][] lvl = maze.getMaze();
        for (int y = 0; y < lvl.length; y++) {
            for (int x = 0; x < lvl[0].length; x++) {
                if(maze.get(y,x) instanceof Ball || maze.get(y,x) instanceof PowerBall){
                    return false;
                }
            }
        }
        return true;
    }
    public int getTimer() {
        return timer;
    }
    public void setTimer(int i) {
        timer = i;
    }
    public int getScore() {
        return score;
    }
    public boolean power() {
        return power;
    }
    public void freeGhosts(){
        blinky.canExit(true);
        clyde.canExit(true);
        pinky.canExit(true);
        inky.canExit(true);

//        char[][] lvl = maze.getMaze();
//        for (int y = 0; y < lvl.length; y++) {
//            for (int x = 0; x < lvl[0].length; x++) {
//                if(maze.get(y,x) instanceof Portal){
//                    if(maze.get(y+1,x) instanceof Empty || maze.get(y+1,x) instanceof Ball){
//                        blinky.setCoord(x,y+1);
//                        clyde.setCoord(x,y+1);
//                        inky.setCoord(x,y+1);
//                        pinky.setCoord(x,y+1);
//                        break;
//                    }else if(maze.get(y-1,x) instanceof Empty || maze.get(y-1,x) instanceof Ball){
//                        blinky.setCoord(x,y-1);
//                        clyde.setCoord(x,y-1);
//                        inky.setCoord(x,y-1);
//                        pinky.setCoord(x,y-1);
//                        break;
//                    }else if(maze.get(y,x+1) instanceof Empty || maze.get(y,x+1) instanceof Ball){
//                        blinky.setCoord(x+1,y);
//                        clyde.setCoord(x+1,y);
//                        inky.setCoord(x+1,y);
//                        pinky.setCoord(x+1,y);
//                        break;
//                    }else if(maze.get(y,x-1) instanceof Empty || maze.get(y,x-1) instanceof Ball){
//                        blinky.setCoord(x,y);
//                        clyde.setCoord(x,y);
//                        inky.setCoord(x,y);
//                        pinky.setCoord(x,y);
//                        break;
//                    }
//                }
//            }
//        }
    }
    private void readFile() {
        maze = null;
        File file = new File(this.file);
        if (!file.exists()) {
            return;
        }
        try {
            FileReader fr = new FileReader(file);
            BufferedReader br = new BufferedReader(fr);
            Scanner sc = new Scanner(br);

            sc.useDelimiter("\n");
            int h = 0;
            int w = 0;
            while (sc.hasNextLine()) {
                h++;
                String line = sc.nextLine();
                w = Math.max(w, line.length());
            }
            maze = new Maze(h, w);
            sc.close();
            fr.close();

            fr = new FileReader(file);
            br = new BufferedReader(fr);
            int y = 0;
            int x;
            String line;
            while((line = br.readLine()) != null){
                y++;
                x = 0;
                for (int i = 0; i < line.length(); i++) {
                    char c = line.charAt(i);
                    x++;
                    IMazeElement e = switch (c) {
                        case Ball.SYMBOL -> new Ball();
                        case Cave.SYMBOL -> new Cave();
                        case FruteSpawn.SYMBOL -> new FruteSpawn();
                        case Empty.SYMBOL -> new Empty();
                        case PacManSpawn.SYMBOL -> new PacManSpawn();
                        case Warp.SYMBOL -> new Warp();
                        case Portal.SYMBOL -> new Portal();
                        case PowerBall.SYMBOL -> new PowerBall();
                        case Wall.SYMBOL -> new Wall();
                        default -> null;
                    };
                    maze.set(y-1,x-1,e);

                    if(e instanceof Portal){
                        setCaveExit(x-1,y-1);
                    }

                    if(e instanceof FruteSpawn){
                        fruteY = y-1;
                        fruteX = x-1;
                        fruteMultiplier = 1;
                    }
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void setCaveExit(int x, int y) {
        blinky.setCaveExit(x,y);
        inky.setCaveExit(x,y);
        clyde.setCaveExit(x,y);
        pinky.setCaveExit(x,y);

    }
    public boolean nextLevel() {
        if(currentLvl == 20){
            return false;
        }
        currentLvl++;
        changeFile();
        return true;
    }
    private void changeFile() {
        StringBuilder sb = new StringBuilder();
        sb.append("Level");
        if(currentLvl < 10){
            sb.append('0').append(currentLvl).append(".txt");
        }else{
            sb.append(currentLvl).append(".txt");
        }
        File file = new File(sb.toString());
        if (file.exists()) {
            this.file = sb.toString();
        }
        readFile();
        setMovingElements();
    }
    public void vulnerableGhost(boolean vulnerable) {
        blinky.setVulnerable(vulnerable);
        clyde.setVulnerable(vulnerable);
        inky.setVulnerable(vulnerable);
        pinky.setVulnerable(vulnerable);
    }
    public int getLives() {
        return lives;
    }
    public boolean getRestart() {
        return restartLvl;
    }
    public void setRestart(boolean b) {
        restartLvl = b;
    }
    public void resetMultiplier() {
        multiplier = 1;
    }
    public void resetLevelPositions(){
        pacMan.resetPostion();

        timer=0;

        blinky.canExit(false);
        clyde.canExit(false);
        pinky.canExit(false);
        inky.canExit(false);

        blinky.setVulnerable(false);
        pinky.setVulnerable(false);
        inky.setVulnerable(false);
        clyde.setVulnerable(false);

        blinky.resetPostion();
        pinky.resetPostion();
        inky.resetPostion();
        clyde.resetPostion();
    }
}
