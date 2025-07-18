package pt.isec.pa.tinypac.model.fsm;

import pt.isec.pa.tinypac.model.data.Data;

import java.io.Serializable;

public class NormalGameState extends StateAdapter implements Serializable {
    protected NormalGameState(Context context, Data data) {
        super(context, data);
    }

    @Override
    public boolean evolve() {
        if(data.evolve()){
            if (data.power()) {
                data.setTimer(15);
                data.vulnerableGhost(true);
                data.resetMultiplier();
                changeState(new PoweredGameState(context, data));
            } else {
                if(data.getRestart()) {
                    if (data.getLives() <= 0) {
                        return false;
                    }
                    System.out.println("asd");
                    data.changeDiretion(0);
                    data.setRestart(false);
                    data.resetLevelPositions();
                    changeState(new WaitKeyPressState(context, data));
                }else {
                    changeState(new NormalGameState(context, data));
                }
            }
        }else{
            if(data.nextLevel()) {
                changeState(new WaitKeyPressState(context, data));
            }else{
                return false;
            }
        }
        return true;
    }
    @Override
    public boolean pressDirectionKey() {
        changeState(new NormalGameState(context, data));
        return true;
    }
    @Override
    public boolean pause() {
        changeState(new PausedGameState(context, data, this.getState()));
        return true;
    }
    @Override
    public TinyPacState getState() {
        return TinyPacState.NORMAL_GAME;
    }
}
