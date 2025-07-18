package pt.isec.pa.tinypac.model.fsm;

import pt.isec.pa.tinypac.model.data.Data;

import java.io.Serializable;

public enum TinyPacState implements Serializable {
    WAIT_KEY_PRESS, FIVE_SECOND_RULE, NORMAL_GAME, POWERED_GAME, PAUSED_GAME, WAIT_NAME;

/*    static ITinyPacState createState(TinyPacState type,Context context, Data data) {
        return switch (type) {
            case WAIT_KEY_PRESS -> new WaitKeyPressState(context,data);
            case FIVE_SECOND_RULE -> new FiveSecondRuleState(context,data);
            case NORMAL_GAME -> new NormalGameState(context,data);
            case POWERED_GAME -> new PoweredGameState(context,data);
            case PAUSED_GAME -> new PausedGameState(context,data);
            case WAIT_NAME -> new WaitNameState(context,data);
        };
    }*/
}
