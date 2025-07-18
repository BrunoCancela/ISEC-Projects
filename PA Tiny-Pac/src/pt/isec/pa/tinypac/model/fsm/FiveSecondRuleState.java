package pt.isec.pa.tinypac.model.fsm;

import pt.isec.pa.tinypac.model.data.Data;

import java.io.Serializable;

public class FiveSecondRuleState extends StateAdapter implements Serializable {

    protected FiveSecondRuleState(Context context, Data data) {
        super(context, data);
    }

    @Override
    public boolean evolve() {
        data.evolve();
        if(data.getTimer() > 0){
            changeState(new FiveSecondRuleState(context, data));
        }else{
            data.freeGhosts();
            changeState(new NormalGameState(context, data));
        }
        return true;
    }

    @Override
    public boolean pressDirectionKey() {
        changeState(new FiveSecondRuleState(context, data));
        return true;
    }

    @Override
    public boolean pause() {
        changeState(new PausedGameState(context, data, this.getState()));
        return true;
    }

    @Override
    public TinyPacState getState(){ return TinyPacState.FIVE_SECOND_RULE;}
}
