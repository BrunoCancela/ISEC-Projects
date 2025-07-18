package pt.isec.pa.tinypac.model.data;

import pt.isec.pa.tinypac.model.data.elements.Portal;
import pt.isec.pa.tinypac.model.data.elements.Wall;
import pt.isec.pa.tinypac.model.data.elements.Warp;

public class PacMan implements IMazeElement{

    public static final char SYMBOL = 'M';

    //1-up 2-down 3-left 4-right
    private int direction = 0;
    private int spawnX= 0;
    private int x = 0;
    private int spawnY= 0;
    private int y = 0;
    public PacMan(){

    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getDirection(){
        return direction;
    }


    public void changeDirection(int i,Maze maze) {
        switch (i){
            case 1-> {
                if(!(maze.get(y-1,x) instanceof Wall || maze.get(y-1,x) instanceof Portal)){
                    direction = 1;
                }
            }
            case 2->  {
                if(!(maze.get(y+1,x) instanceof Wall || maze.get(y+1,x) instanceof Portal)){
                    direction = 2;
                }
            }
            case 3->  {
                if(!(maze.get(y,x-1) instanceof Wall || maze.get(y,x-1) instanceof Portal)){
                    direction = 3;
                }
            }
            case 4->  {
                if(!(maze.get(y,x+1) instanceof Wall || maze.get(y,x+1) instanceof Portal)){
                    direction = 4;
                }
            }
            default -> direction = 0;
        }
    }

    @Override
    public char getSymbol() {
        return 'M';
    }

    public void setCoord(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void move(Maze maze) {

        if(direction == 1){
            if(maze.get(y-1,x) instanceof Wall ||  maze.get(y-1,x) instanceof Portal){
                direction = 0;
            }else{
                setCoord(x,y-1);
            }
        }else if(direction == 2){
            if(maze.get(y+1,x) instanceof Wall || maze.get(y+1,x) instanceof Portal){
                direction = 0;
            }else{
                setCoord(x,y+1);
            }
        }else if(direction == 3){
            if(maze.get(y,x-1) instanceof Wall || maze.get(y,x-1) instanceof Portal){
                direction = 0;
            }else{
                setCoord(x-1,y);
            }
        }else{
            if(maze.get(y,x+1) instanceof Wall || maze.get(y,x+1) instanceof Portal){
                direction = 0;
            }else{
                setCoord(x+1,y);
            }
        }
        if(maze.get(y,x) instanceof Warp){
            for(int i = 0; i < maze.getMaze().length; i++){
                for(int j = 0; j < maze.getMaze()[i].length; j++){
                    if(maze.get(i,j) instanceof Warp && !(x ==j && y ==i)){
                        setCoord(j,i);
                        return;
                    }
                }
            }
        }
    }

    public void setSpawn(int coordX, int coordy) {
        spawnX = coordX;
        spawnY = coordy;
        resetPostion();
    }

    public void resetPostion() {
        x = spawnX;
        y = spawnY;
    }
}
