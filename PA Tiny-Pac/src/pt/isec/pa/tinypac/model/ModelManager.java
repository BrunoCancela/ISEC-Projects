package pt.isec.pa.tinypac.model;

import pt.isec.pa.tinypac.gameengine.GameEngine;
import pt.isec.pa.tinypac.gameengine.IGameEngine;
import pt.isec.pa.tinypac.gameengine.IGameEngineEvolve;
import pt.isec.pa.tinypac.model.fsm.Context;
import pt.isec.pa.tinypac.model.fsm.TinyPacState;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

public class ModelManager implements IGameEngineEvolve {
    public static final String GAME_RUNNING = "game_running";
    public static final String GAME_END = "game_end";
    Context fsm;
    PropertyChangeSupport pcs;
    GameEngine gameEngine;

    public ModelManager(){
        gameEngine = new GameEngine();

        fsm = new Context();
        pcs = new PropertyChangeSupport(this);

        gameEngine.registerClient(this);
        gameEngine.start(1000);
        //pcs.firePropertyChange(GAME_MENU,null,null);
    }

    public void addPropertyChangeListener(String property, PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(property,listener);
    }

    @Override
    public void evolve(IGameEngine gameEngine, long currentTime) {
        if(fsm.evolve()){
            pcs.firePropertyChange(GAME_RUNNING,null,fsm.getState());
        }else{
            gameEngine.stop();
            gameEngine.unregisterClient(this);
            pcs.firePropertyChange(GAME_END,null,null);
        }
        System.out.println(fsm.getState());

    }

    public String getMapLevel() {
        return fsm.getMapLevel();
    }

    public void pause() {
        fsm.pause();
        pcs.firePropertyChange(GAME_RUNNING,null,null);
    }

    public void resume() {
        fsm.resume();
        pcs.firePropertyChange(GAME_RUNNING,null,null);
    }

    public TinyPacState getState() {
        return fsm.getState();
    }

    public void pressDirectionKey(int i) {
        fsm.pressDirectionKey(i);
        pcs.firePropertyChange(GAME_RUNNING,null,null);
    }

    public void exitGame() {
        fsm.exitGame();
        gameEngine.stop();
        gameEngine.unregisterClient(this);
        pcs.firePropertyChange(GAME_END,null,null);
    }

    public int getLevel() {
        return fsm.getLevel();
    }

    public int getScore() {
        return fsm.getScore();
    }

    public int getLives() {
        return fsm.getLives();
    }

    public void saveGame() {
        fsm.saveGame();
    }

    public void loadGame() {
        fsm.loadGame();
        fsm.pause();
        pcs.firePropertyChange(GAME_RUNNING,null,null);
    }
}
