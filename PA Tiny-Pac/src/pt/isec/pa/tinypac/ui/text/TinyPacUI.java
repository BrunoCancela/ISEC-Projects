package pt.isec.pa.tinypac.ui.text;

import pt.isec.pa.tinypac.model.fsm.*;
import pt.isec.pa.tinypac.utils.PAInput;


public class TinyPacUI {
    Context fsm;
    boolean finish;
    public TinyPacUI(Context fsm){ this.fsm = fsm;}
    public void start(){
        boolean exit = false;
        while(!exit) {
            System.out.print("\nDEIS-ISEC-IPC\nLEI PA 2022\\2023\nBruno Cancela 2020131288\nTrabalho Académico\n");
            switch (PAInput.chooseOption("Menu:","Iniciar Jogo", "Consultar Top 5", "Sair")) {
                case 1 -> {
                    finish = false;
                    while (!finish){
                        if(fsm.getState() == null)System.exit(-1);
                        switch (fsm.getState()) {
                            case WAIT_KEY_PRESS -> waitKeyPress();
                            case FIVE_SECOND_RULE -> fiveSecondRule();
                            case NORMAL_GAME -> normalGame();
                            case POWERED_GAME -> poweredGame();
                            case PAUSED_GAME -> pausedGame();
                            case WAIT_NAME -> waitName();
                        }
                    } {
                    System.out.printf("\nCurrent state: %s\n\n", fsm.getState());
                }}
                case 2 -> System.out.println("\nTOP 5:\n 1-> ¯\\_(ツ)_/¯ | SCORE: ¯\\_(ツ)_/¯");
                case 3 -> exit = true;
            }
        }
    }
    private boolean waitKeyPress() {
        System.out.printf("\nSTART GAME - LVL %d     SCORE: %d\n", fsm.getLevel(), fsm.getScore());
        System.out.print(fsm.getMapLevel());
        switch (PAInput.chooseOption("Start Game:","Pause", "Press Direction Key")) {
            case 1 -> fsm.pause();
            case 2 -> chooseDiretion();
            default -> System.out.println("Not an option");
        }
        return true;
    }
    private boolean fiveSecondRule() {
        System.out.printf("\nFIVE SECOND RULE - LVL %d     SCORE: %d\n", fsm.getLevel(), fsm.getScore());
        System.out.print(fsm.getMapLevel());
        switch (PAInput.chooseOption("FIVE SECONDS RULE:","Pause","Evolve", "Press Direction Key")) {
            case 1 -> fsm.pause();
            case 2 -> fsm.evolve();
            case 3 -> chooseDiretion();
            default -> System.out.println("Not an option");
        }
        return true;
    }
    private boolean normalGame() {
        System.out.printf("\nNORMAL GAME - LVL %d     SCORE: %d\n", fsm.getLevel(), fsm.getScore());
        System.out.print(fsm.getMapLevel());
        switch (PAInput.chooseOption("Normal Game:","Pause","Evolve", "Press Direction Key")) {
            case 1 -> fsm.pause();
            case 2 -> {if(!fsm.evolve()){
                finish=true;
            }
            }
            case 3 -> chooseDiretion();
            default -> System.out.println("Not an option");
        }
        return true;
    }
    private boolean poweredGame() {
        System.out.printf("\nPOWER BALL GAME - LVL %d     SCORE: %d\n", fsm.getLevel(), fsm.getScore());
        System.out.print(fsm.getMapLevel());
        switch (PAInput.chooseOption("Power Ball Game:","Pause","Evolve", "Press Direction Key")) {
            case 1 -> fsm.pause();
            case 2 -> {if(!fsm.evolve()){
                finish=true;
            }
            }
            case 3 -> chooseDiretion();
            default -> System.out.println("Not an option");
        }
        return true;
    }
    private boolean pausedGame() {
        System.out.printf("\nPAUSED GAME - LVL %d     SCORE: %d\n", fsm.getLevel(), fsm.getScore());
        switch (PAInput.chooseOption("Pause:","Resume","Save Game","Exit")) {
            case 1 -> fsm.resume();
            case 2 -> System.out.println("GAME SAVED\n");
            case 3 -> fsm.exitGame();
            default -> System.out.println("Not an option");
        }
        return true;
    }
    private boolean waitName() {
        System.out.printf("\nWAIT NAME GAME - LVL %d     SCORE: %d\n", fsm.getLevel(), fsm.getScore());
        String name;
        name = PAInput.readString("Insira o nome: ", true);
        finish=true;
        return true;
    }
    private void chooseDiretion() {
        switch (PAInput.chooseOption("Change Diretion:", "Up", "Down", "Left", "Right")) {
            case 1 -> fsm.pressDirectionKey(1);
            case 2 -> fsm.pressDirectionKey(2);
            case 3 -> fsm.pressDirectionKey(3);
            case 4 -> fsm.pressDirectionKey(4);
        }
    }
}
