package pt.isec.pa.tinypac.model.fsm;

import pt.isec.pa.tinypac.model.data.Data;

import java.io.Serializable;

public class PausedGameState extends StateAdapter implements Serializable {
    private final TinyPacState previousToPausedState;
    protected PausedGameState(Context context, Data data, TinyPacState state) {
        super(context, data);
        previousToPausedState = state;
    }

    @Override
    public boolean resume() {
        switch(previousToPausedState){
            case WAIT_KEY_PRESS -> changeState(new WaitKeyPressState(context, data));
            case FIVE_SECOND_RULE -> changeState(new FiveSecondRuleState(context, data));
            case NORMAL_GAME -> changeState(new NormalGameState(context, data));
            case POWERED_GAME -> changeState(new PoweredGameState(context, data));
        }
        return true;
    }

    @Override
    public boolean exitGame() {
        return true;
    }

    @Override
    public TinyPacState getState() {
        return TinyPacState.PAUSED_GAME;
    }
}
