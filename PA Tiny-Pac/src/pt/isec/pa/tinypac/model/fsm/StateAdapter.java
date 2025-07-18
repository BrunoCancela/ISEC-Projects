package pt.isec.pa.tinypac.model.fsm;

import pt.isec.pa.tinypac.model.data.Data;

abstract class StateAdapter implements ITinyPacState{
    protected Data data;
    protected Context context;
    protected  StateAdapter(Context context, Data data){
        this.context = context;
        this.data = data;
    }

    protected void changeState(ITinyPacState newState){
        context.changeState(newState);
    }

    @Override
    public boolean evolve() {
        return true;
    }
    @Override
    public boolean pressDirectionKey() {
        return false;
    }
    @Override
    public boolean exitGame(){
        return false;
    }
    @Override
    public boolean pause(){return false;}
    @Override
    public boolean resume(){return false;}
    @Override
    public TinyPacState getState(){return null;}
}
