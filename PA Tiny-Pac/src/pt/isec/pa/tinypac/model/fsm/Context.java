package pt.isec.pa.tinypac.model.fsm;

import pt.isec.pa.tinypac.model.data.Data;

import java.io.*;

public class Context implements Serializable {
    private Data data;
    private ITinyPacState tinyPacState;

    public Context(){
        data = new Data();
        tinyPacState = new WaitKeyPressState(this, data);
    }
    public void changeState(ITinyPacState newState) {
        this.tinyPacState = newState;
    }

    public TinyPacState getState() {
        return tinyPacState.getState();
    }


    public boolean pause(){return tinyPacState.pause();}

    public boolean resume(){return tinyPacState.resume();}

    public void saveGame() {
        try {
            // Create the output stream for the binary file
            FileOutputStream fileOut = new FileOutputStream("savedGame.bin", false);
            ObjectOutputStream objectOut = new ObjectOutputStream(fileOut);

            // Write the data object to the file
            objectOut.writeObject(data);

            // Write the state object to the file
            tinyPacState.resume();
            objectOut.writeObject(tinyPacState);
            tinyPacState.pause();

            // Close the streams
            objectOut.close();
            fileOut.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadGame() {
        try {
            // Create the input stream for the binary file
            FileInputStream fileIn = new FileInputStream("savedGame.bin");
            ObjectInputStream objectIn = new ObjectInputStream(fileIn);

            // Read the data object from the file
            Data savedData = (Data) objectIn.readObject();

            // Read the state object from the file
            ITinyPacState savedState = (ITinyPacState) objectIn.readObject();

            // Assign the saved data and state to the current context
            this.data = savedData;
            this.tinyPacState = savedState;

            switch (tinyPacState.getState()){
                case WAIT_KEY_PRESS -> changeState(new WaitKeyPressState(this, data));
                case FIVE_SECOND_RULE  -> changeState(new FiveSecondRuleState(this, data));
                case NORMAL_GAME -> changeState(new NormalGameState(this, data));
                case POWERED_GAME  -> changeState(new PoweredGameState(this, data));
            }
            // Close the streams
            objectIn.close();
            fileIn.close();

            File file = new File("savedGame.bin");
            if (file.exists()) {
                if (file.delete()) {
                    System.out.println("Binary file deleted successfully.");
                } else {
                    System.out.println("Failed to delete the binary file.");
                }
            } else {
                System.out.println("Binary file does not exist.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public int getLevel() {
        return data.getLevel();
    }

    public boolean evolve() {
        return tinyPacState.evolve();

    }
    public void pressDirectionKey(int i) {
        data.changeDiretion(i);
        tinyPacState.pressDirectionKey();
    }
    public void exitGame() {
        tinyPacState.exitGame();
    }
    public String getMapLevel() {
        return data.getMapLevel();
    }
    public int getScore() {
        return data.getScore();
    }

    public int getLives() {
        return data.getLives();
    }
}
