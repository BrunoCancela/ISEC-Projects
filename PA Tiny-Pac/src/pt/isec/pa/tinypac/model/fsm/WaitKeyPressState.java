package pt.isec.pa.tinypac.model.fsm;

import pt.isec.pa.tinypac.model.data.Data;

import java.io.Serializable;

public class WaitKeyPressState extends StateAdapter implements Serializable {
    protected WaitKeyPressState(Context context, Data data) {
        super(context, data);
    }
    @Override
    public boolean pressDirectionKey(){
        data.setTimer(5);
        changeState(new FiveSecondRuleState(context, data));
        return true;
    }

    @Override
    public boolean pause() {
        changeState(new PausedGameState(context, data, this.getState()));
        return true;
    }

    @Override
    public TinyPacState getState(){ return TinyPacState.WAIT_KEY_PRESS;}

}
