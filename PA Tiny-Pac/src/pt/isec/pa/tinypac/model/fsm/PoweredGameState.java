package pt.isec.pa.tinypac.model.fsm;

import pt.isec.pa.tinypac.model.data.Data;

import java.io.Serializable;

public class PoweredGameState extends StateAdapter implements Serializable {
    protected PoweredGameState(Context context, Data data) {
        super(context, data);
    }

    @Override
    public boolean evolve() {
        if(data.evolve()) {
            if (data.getTimer() > 0) {
                data.resetMultiplier();
                changeState(new PoweredGameState(context, data));
            } else {
                data.vulnerableGhost(false);
                changeState(new NormalGameState(context, data));
            }

            if(data.getRestart()) {
                if (data.getLives() <= 0)
                    return false;
                data.changeDiretion(0);
                data.setRestart(false);
                data.resetLevelPositions();
                changeState(new WaitKeyPressState(context, data));
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
        changeState(new PoweredGameState(context, data));
        return true;
    }

    @Override
    public boolean pause() {
        changeState(new PausedGameState(context, data, this.getState()));
        return true;
    }

    @Override
    public TinyPacState getState() {
        return TinyPacState.POWERED_GAME;
    }
}
