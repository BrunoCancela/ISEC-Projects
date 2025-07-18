package pt.isec.pd.springboot.m1.client.model;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class ClientLogic {
    public static boolean isValidEmail(String email) {
        // Define a regular expression pattern for a valid email address
        String regex = "^[A-Za-z0-9+_.-]+@(.+)$";

        // Compile the pattern
        Pattern pattern = Pattern.compile(regex);

        // Create a Matcher object
        Matcher matcher = pattern.matcher(email);

        // Check if the input string matches the pattern
        return matcher.matches();
    }

    public static String gerarArquivoCSV(String csv, String email,String extra) {
        if (csv == null || csv.isEmpty()) {
            return("Não há dados para gerar o CSV.");
        }

        String nomeArquivo = criarNomeArquivo(email,extra);
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(nomeArquivo), StandardCharsets.UTF_8)) {
            // Write the UTF-8 Byte Order Mark
            writer.write('\ufeff');
            writer.write(csv);
            return("Arquivo CSV gerado com sucesso: " + nomeArquivo);
        } catch (IOException e) {
            return("Erro ao criar o arquivo CSV: " + e.getMessage());
        }
    }

    private static String criarNomeArquivo(String email, String extra) {
        String nomeBase = email.split("@")[0]; // Extrai a parte antes do '@' do email
        return nomeBase + extra + ".csv";    // Cria um nome de arquivo usando o nomeBase
    }

    public static boolean isValidDateFormat(String date) {
        // Pattern for "dd/MM/yyyy"
        String datePattern = "\\d{2}/\\d{2}/\\d{4}";
        if (!date.matches(datePattern)) {
            return false;
        }

        String[] parts = date.split("/");
        try {
            int day = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            int year = Integer.parseInt(parts[2]);

            // Check for valid month
            if (month < 1 || month > 12) {
                return false;
            }

            // Check for valid day
            Calendar calendar = new GregorianCalendar(year, month - 1, day);
            if (day != calendar.get(Calendar.DAY_OF_MONTH)) {
                return false;
            }

            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isValidTimeFormat(String time) {
        String timePattern = "\\d{2}:\\d{2}";
        if (!time.matches(timePattern)) {
            return false;
        }

        String[] parts = time.split(":");
        try {
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);
            // Additional checks for valid hour (0-23) and minute (0-59) values can be added here
            return hour >= 0 && hour < 24 && minute >= 0 && minute < 60;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isStartTimeBeforeEndTime(String startTime, String endTime) {
        int[] start = parseTime(startTime);
        int[] end = parseTime(endTime);

        if(start[0] < end[0]){
            return true;
        }else {
            return start[0] == end[0] && start[1] < end[1];
        }
    }

    private static int[] parseTime(String time) {
        String[] parts = time.split(":");
        int hours = Integer.parseInt(parts[0]);
        int minutes = Integer.parseInt(parts[1]);
        return new int[]{hours, minutes};
    }
}