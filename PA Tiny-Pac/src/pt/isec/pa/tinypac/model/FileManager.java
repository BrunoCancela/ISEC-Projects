package pt.isec.pa.tinypac.model;

import javafx.util.Pair;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

public class FileManager {
    boolean savedGame;
    public FileManager(){
        checkFile();
    }
    private void checkFile() {
        File file = new File("savedGame.bin");

        savedGame = file.exists();

        File top5File = new File("top5.txt");

        if (!top5File.exists()) {
            try {
                top5File.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public boolean getSavedGame(){
        return savedGame;
    }

    public boolean isInTop5(int score) {

        if(score == 0 || savedGame){
            return false;
        }
        List<Integer> scores = new ArrayList<>();
        List<String> names = new ArrayList<>();
        readScoresFromFile(scores,names);
        Collections.reverse(scores);

        if (scores.size() < 5) {
            return true; // There is space in the top 5
        }

        int lowestScore = scores.get(0);
        return score > lowestScore;
    }

    private void readScoresFromFile(List<Integer> scores,List<String> names) {
        File top5File = new File("top5.txt");

        try (Scanner reader = new Scanner(new FileReader(top5File))) {
            String line;
            while (reader.hasNextLine()) {
                line = reader.nextLine();
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    String name = parts[0].trim();
                    int score = Integer.parseInt(parts[1].trim());
                    scores.add(score);
                    names.add(name);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String readScoreBoard() {
        StringBuilder stringBuilder = new StringBuilder();
        int position = 0;
        try (Scanner reader = new Scanner(new FileReader("top5.txt"))) {
            String line;
            while (reader.hasNextLine()) {
                line = reader.nextLine();
                position++;
                stringBuilder.append(position).append(". ");
                stringBuilder.append(line);
                stringBuilder.append(System.lineSeparator());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stringBuilder.toString();
    }

    public void newGameDeleteSavedGame(){
        File file = new File("savedGame.bin");
        if (file.exists()) {
            file.delete();
        }
    }

    public void saveInTop5(String name, int score) {
        List<Integer> scores = new ArrayList<>();
        List<String> names = new ArrayList<>();
        readScoresFromFile(scores,names);

        List<Pair<String, Integer>> pairs = new ArrayList<>();
        for (int i = 0; i < names.size(); i++) {
            pairs.add(new Pair<>(names.get(i), scores.get(i)));
        }

        pairs.add(new Pair<>(name, score));

        // Sort the pairs in descending order based on scores
        pairs.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        // Keep only the top 5 pairs
        pairs = pairs.subList(0, Math.min(5, pairs.size()));

        try (FileWriter writer = new FileWriter("top5.txt", false)) {
            for (Pair<String, Integer> pair : pairs) {
                writer.write(pair.getKey() + ":" + pair.getValue() + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
