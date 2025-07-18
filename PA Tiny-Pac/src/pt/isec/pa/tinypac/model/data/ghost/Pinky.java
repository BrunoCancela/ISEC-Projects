package pt.isec.pa.tinypac.model.data.ghost;

import pt.isec.pa.tinypac.model.data.IMazeElement;
import pt.isec.pa.tinypac.model.data.Maze;
import pt.isec.pa.tinypac.model.data.PacMan;
import pt.isec.pa.tinypac.model.data.elements.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/* Este fantasma desloca-se na direção de um dos cantos do labirinto de acordo com a
seguinte ordem: canto superior direito, canto inferior direito, canto superior esquerdo,
canto inferior esquerdo, retomando novamente a sequência. Ao chegar a um cruzamento,
sorteia uma direção que o faça, no imediato, aproximar-se mais do canto pretendido do
labirinto. Quando encontra um obstáculo, tendo de sortear uma nova direção, se já está a
menos de uma determinada distância do canto pretendido, então altera o objetivo para o
próximo canto do labirinto. A distância referida deve ser definida pelo aluno - por exemplo
10-15% da largura do tabuleiro;*/
public class Pinky implements IMazeElement {
    public static final char SYMBOL = 'P';
    private int x;
    private int y;
    private int spawnX;
    private int spawnY;
    private int exitX;
    private int exitY;
    private boolean canExit;
    private int direction;
    private boolean vulnerable;
    private boolean coordsEnded;

    private List<Coordinates> coordinatesList;
    public Pinky(){
        x= 0;
        y= 0;
        spawnX= 0;
        spawnY= 0;
        exitX = 0;
        exitY = 0;
        canExit = false;
        direction = 0;
        vulnerable = false;
        coordinatesList = new ArrayList<>();
        coordsEnded = false;
    }

    public void setCaveExit(int auxX, int auxY) {
        exitX = auxX;
        exitY = auxY;
    }

    public void setCoord(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    @Override
    public char getSymbol() {
        if(vulnerable){
            return 'p';
        }
        return SYMBOL;
    }

    public void setVulnerable(boolean vulnerable){
        this.vulnerable = vulnerable;
    }

    public boolean isVulnerable() {
        return vulnerable;
    }

    public void setSpawn(int coordX, int coordy) {
        spawnX = coordX;
        spawnY = coordy;
        resetPostion();
    }

    public void resetPostion() {
        vulnerable = false;
        Random random = new Random();
        direction = random.nextInt(4) + 1;
        x = spawnX;
        y = spawnY;
        coordinatesList = new ArrayList<>();
        coordsEnded = false;
    }

    public void canExit(boolean b) {
        canExit = b;
    }

    public void move(Maze maze, PacMan pacMan){
        if(vulnerable){
            if(coordinatesList.isEmpty() || coordsEnded){
                normalMovement(maze);
                coordsEnded = true;
            }else{
                setCoord(coordinatesList.get(0).getX(),coordinatesList.get(0).getY());
                coordinatesList.remove(0);
            }
        }else{
            coordsEnded = false;
            if(maze.get(y,x) instanceof Portal || maze.get(y,x) instanceof Cave){
                if(canExit){
                    exitCave(maze);
                }
            }else{
                normalMovement(maze);
            }
        }
    }

    private void exitCave(Maze maze) {
        if(maze.get(y,x) instanceof Portal){
            if(maze.get(y-1,x) instanceof Empty || maze.get(y-1,x) instanceof Ball)
                setCoord(x,y-1);
            else if(maze.get(y+1,x) instanceof Empty || maze.get(y+1,x) instanceof Ball)
                setCoord(x,y+1);
            else if(maze.get(y,x-1) instanceof Empty || maze.get(y,x-1) instanceof Ball)
                setCoord(x-1,y);
            else
                setCoord(x+1,y);
        }else{
            if(x < exitX){
                x++;
            }else if(x > exitX){
                x--;
            }else{
                if(y < exitY){
                    y++;
                }else if(y > exitY){
                    y--;
                }
            }
        }
    }

    public void normalMovement(Maze maze){
        if(coordinatesList.size() > 20){
            coordinatesList.remove(20);
        }
        coordinatesList.add(0,new Coordinates(x,y));

        Random random = new Random();
        IMazeElement front = new Wall();
        IMazeElement left = new Wall();
        IMazeElement right = new Wall();
        int oldX = x, oldY = y;
        int[][] newCoord = new int [4][2];
        switch (direction){
            case 1 -> {
                front = maze.get(y-1,x);
                newCoord[0][0] = y-1; newCoord[0][1] = x;
                left = maze.get(y,x-1);
                newCoord[1][0] = y; newCoord[1][1] = x-1;
                right = maze.get(y,x+1);
                newCoord[2][0] = y; newCoord[2][1] = x+1;
                //back
                newCoord[3][0] = y+1; newCoord[3][1] = x;
            }
            case 2 -> {
                front = maze.get(y+1,x);
                newCoord[0][0] = y+1; newCoord[0][1] = x;
                left = maze.get(y,x+1);
                newCoord[1][0] = y; newCoord[1][1] = x+1;
                right = maze.get(y,x-1);
                newCoord[2][0] = y; newCoord[2][1] = x-1;
                //back
                newCoord[3][0] = y-1; newCoord[3][1] = x;
            }
            case 3 -> {
                front = maze.get(y,x-1);
                newCoord[0][0] = y; newCoord[0][1] = x-1;
                left = maze.get(y+1,x);
                newCoord[1][0] = y+1; newCoord[1][1] = x;
                right = maze.get(y-1,x);
                newCoord[2][0] = y-1; newCoord[2][1] = x;
                //back
                newCoord[3][0] = y; newCoord[3][1] = x+1;
            }
            case 4 -> {
                front = maze.get(y,x+1);
                newCoord[0][0] = y; newCoord[0][1] = x+1;
                left = maze.get(y-1,x);
                newCoord[1][0] = y-1; newCoord[1][1] = x;
                right = maze.get(y+1,x);
                newCoord[2][0] = y+1; newCoord[2][1] = x;
                //back
                newCoord[3][0] = y; newCoord[3][1] = x-1;
            }
        }

        if(!(front instanceof Wall || front instanceof Warp || front instanceof Portal)){
            if(!(left instanceof Wall || left instanceof Warp || left instanceof Portal)){
                if(!(right instanceof Wall || right instanceof Warp || right instanceof Portal)){
                    switch (random.nextInt(3) + 1){
                        case 1 -> setCoord(newCoord[0][1],newCoord[0][0]);
                        case 2-> setCoord(newCoord[1][1],newCoord[1][0]);
                        default -> setCoord(newCoord[2][1],newCoord[2][0]);
                    }
                }else{
                    if (random.nextInt(2) + 1 == 1) {
                        setCoord(newCoord[0][1], newCoord[0][0]);
                    } else {
                        setCoord(newCoord[1][1], newCoord[1][0]);
                    }
                }
            }else{
                if(!(right instanceof Wall || right instanceof Warp || right instanceof Portal)){
                    if (random.nextInt(2) + 1 == 1) {
                        setCoord(newCoord[0][1], newCoord[0][0]);
                    } else {
                        setCoord(newCoord[2][1], newCoord[2][0]);
                    }
                }else{
                    setCoord(newCoord[0][1],newCoord[0][0]);
                }
            }
        }else{
            if(!(right instanceof Wall || right instanceof Warp || right instanceof Portal)){
                if(!(left instanceof Wall || left instanceof Warp || left instanceof Portal)){
                    if (random.nextInt(3) + 1 == 2) {
                        setCoord(newCoord[1][1], newCoord[1][0]);
                    } else {
                        setCoord(newCoord[2][1], newCoord[2][0]);
                    }
                }else{
                    setCoord(newCoord[2][1],newCoord[2][0]);
                }
            }else{
                if(!(left instanceof Wall || left instanceof Warp || left instanceof Portal)){
                    setCoord(newCoord[1][1],newCoord[1][0]);
                }else{
                    setCoord(newCoord[3][1],newCoord[3][0]);
                }
            }
        }

        if(oldX == x){
            if((oldY-1) == y){
                direction = 1;
            }else{
                direction = 2;
            }
        }else{
            if((oldX-1) == x){
                direction = 3;
            }else{
                direction = 4;
            }
        }
    }
}

//    public void normalMovement(Maze maze){
//        if(coordinatesList.size() > 20){
//            coordinatesList.remove(20);
//        }
//        coordinatesList.add(0,new Coordinates(x,y));
//
//        boolean didntMove = true;
//        int cornerX = 0;
//        int cornerY = 0;
//        int oldX = x;
//        int oldY = y;
//        switch (corner){
//            case 1 -> {
//                System.out.println("a1");
//                cornerY = 0;
//                cornerX = maze.getMaze().length - 1;
//            }
//            case 2 -> {
//                cornerY = maze.getMaze()[0].length - 1;
//                cornerX = maze.getMaze().length - 1;
//            }
//            case 3 -> {
//                System.out.println("a2");
//                cornerY = 0;
//                cornerX = 0;
//            }
//            case 4 -> {
//                System.out.println("a3");
//                cornerY = maze.getMaze()[0].length - 1;
//                cornerX = 0;
//            }
//        }
//        if(goInWrongDirection){
//            if(direction == 1){
//
//                if(!(maze.get(y-1,x) instanceof Wall ||  maze.get(y-1,x) instanceof Portal ||  maze.get(y-1,x) instanceof Warp)&& didntMove){
//                    setCoord(x,y-1);
//                    didntMove = false;
//                }else if(!(maze.get(y,x+1) instanceof Wall ||  maze.get(y,x+1) instanceof Portal ||  maze.get(y,x+1) instanceof Warp) && didntMove){
//                    setCoord(x+1,y);
//                    didntMove = false;
//                    direction = 3;
//                }else if(!(maze.get(y,x-1) instanceof Wall ||  maze.get(y,x-1) instanceof Portal ||  maze.get(y,x-1) instanceof Warp) && didntMove){
//                    setCoord(x-1,y-1);
//                    didntMove = false;
//                    direction = 2;
//                }
//            }else if(direction == 2){
//
//                if(!(maze.get(y,x-1) instanceof Wall ||  maze.get(y,x-1) instanceof Portal ||  maze.get(y,x-1) instanceof Warp) && didntMove){
//                    setCoord(x-1,y);
//                    didntMove = false;
//                }else if(!(maze.get(y-1,x) instanceof Wall ||  maze.get(y-1,x) instanceof Portal ||  maze.get(y-1,x) instanceof Warp)&& didntMove){
//                    setCoord(x,y-1);
//                    direction = 1;
//                    didntMove = false;
//                }else if(!(maze.get(y+1,x) instanceof Wall ||  maze.get(y+1,x) instanceof Portal ||  maze.get(y+1,x) instanceof Warp)&& didntMove){
//                    setCoord(x,y-1);
//                    direction = 4;
//                    didntMove = false;
//                }
//            }else if(direction == 3){
//
//                if(!(maze.get(y,x+1) instanceof Wall ||  maze.get(y,x+1) instanceof Portal ||  maze.get(y,x+1) instanceof Warp) && didntMove){
//                    setCoord(x+1,y);
//                    didntMove = false;
//                }if(!(maze.get(y-1,x) instanceof Wall ||  maze.get(y-1,x) instanceof Portal ||  maze.get(y-1,x) instanceof Warp)&& didntMove){
//                    setCoord(x,y-1);
//                    direction = 1;
//                    didntMove = false;
//                }else if(!(maze.get(y+1,x) instanceof Wall ||  maze.get(y+1,x) instanceof Portal ||  maze.get(y+1,x) instanceof Warp)&& didntMove){
//                    setCoord(x,y+1);
//                    direction = 4;
//                    didntMove = false;
//                }
//            }else{
//
//                if(!(maze.get(y+1,x) instanceof Wall ||  maze.get(y+1,x) instanceof Portal ||  maze.get(y+1,x) instanceof Warp)){
//                    setCoord(x,y+1);
//                    didntMove = false;
//                }else if(!(maze.get(y,x+1) instanceof Wall ||  maze.get(y,x+1) instanceof Portal ||  maze.get(y,x+1) instanceof Warp) && didntMove){
//                    setCoord(x+1,y);
//                    didntMove = false;
//                    direction = 3;
//                }else if(!(maze.get(y,x-1) instanceof Wall ||  maze.get(y,x-1) instanceof Portal ||  maze.get(y,x-1) instanceof Warp) && didntMove){
//                    setCoord(x-1,y);
//                    didntMove = false;
//                    direction = 2;
//                }
//            }
//        }
//
//
//
//        if(!(maze.get(y-1,x) instanceof Wall ||  maze.get(y-1,x) instanceof Portal ||  maze.get(y-1,x) instanceof Warp && didntMove)){
//                if(Math.abs(cornerY - y-1) > Math.abs(cornerY-y)){
//                    didntMove = false;
//                    setCoord(x,y-1);
//                }
//        }
//
//        if(!(maze.get(y+1,x) instanceof Wall || maze.get(y+1,x) instanceof Portal || maze.get(y+1,x) instanceof Warp) && didntMove){
//            if(Math.abs(cornerY - y+1) > Math.abs(cornerY-y)){
//                didntMove = false;
//                setCoord(x,y+1);
//            }
//        }
//        if(!(maze.get(y,x-1) instanceof Wall || maze.get(y,x-1) instanceof Portal || maze.get(y,x-1) instanceof Warp) && didntMove){
//            if(Math.abs(cornerX - x-1) > Math.abs(cornerX-x)){
//                didntMove = false;
//                setCoord(x-1,y);
//            }
//        }
//
//        if(!(maze.get(y,x+1) instanceof Wall || maze.get(y,x+1) instanceof Portal || maze.get(y,x+1) instanceof Warp) && didntMove){
//            if(Math.abs(cornerX - x+1) > Math.abs(cornerX-x)){
//                didntMove = false;
//                setCoord(x+1,y);
//            }
//        }
//
//        if(didntMove){
//            goInWrongDirection = true;
//        }
//
//        int dx = cornerX - x;
//        int dy = cornerY - y;
//
//        int mazeWidth = maze.getMaze().length;
//        int mazeHeight =maze.getMaze()[0].length;
//
//        int distanceX = (int) (mazeWidth * 0.1); // 10% of maze width
//        int distanceY = (int) (mazeHeight * 0.1); // 10% of maze height
//
//        boolean isWithinThreshold = (Math.abs(dx) <= distanceX) && (Math.abs(dy) <= distanceY);
//
//        if (isWithinThreshold) {
//            if(corner == 1){
//                corner = 2;
//            }else if(corner == 2) {
//                corner = 3;
//            }else if(corner == 3) {
//                corner = 4;
//            }else{
//                corner = 1;
//            }
//        }
//
//        if(!goInWrongDirection) {
//            if (oldX == x) {
//                if ((oldY - 1) == y) {
//                    direction = 1;
//                } else {
//                    direction = 2;
//                }
//            } else {
//                if ((oldX - 1) == x) {
//                    direction = 3;
//                } else {
//                    direction = 4;
//                }
//            }
//        }
//    }