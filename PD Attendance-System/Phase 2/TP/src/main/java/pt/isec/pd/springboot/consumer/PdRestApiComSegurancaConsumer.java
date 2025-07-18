package pt.isec.pd.springboot.consumer;

import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import pt.isec.pd.springboot.m1.client.model.ClientLogic;

import java.io.IOException;
import java.util.Base64;
import java.util.Scanner;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class PdRestApiComSegurancaConsumer {


    public static String sendRequestAndShowResponse(String uri, String verb, String authorizationValue, String body) throws MalformedURLException, IOException {

        String responseBody = null;
        URL url = new URL(uri);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod(verb);
        connection.setRequestProperty("Accept", "application/xml, */*");

        if(authorizationValue!=null) {
            connection.setRequestProperty("Authorization", authorizationValue);
        }

        if(body!=null){
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "Application/Json");
            connection.getOutputStream().write(body.getBytes());
        }

        connection.connect();

        int responseCode = connection.getResponseCode();
        //System.out.println("Response code: " +  responseCode + " (" + connection.getResponseMessage() + ")");

        Scanner s;

        if(connection.getErrorStream()!=null) {
            s = new Scanner(connection.getErrorStream()).useDelimiter("\\A");
            responseBody = s.hasNext() ? s.next() : null;
        }

        try {
            s = new Scanner(connection.getInputStream()).useDelimiter("\\A");
            responseBody = s.hasNext() ? s.next() : null;
        } catch (IOException e){}

        connection.disconnect();

        //System.out.println(verb + " " + uri + (body==null?"":" with body: "+body) + " ==> " + responseBody);
        //System.out.println();

        return responseBody;
    }

    public static void main(String[] args) throws MalformedURLException, IOException {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("Escolha uma opção:");
            System.out.println("1. Registar");
            System.out.println("2. Login");
            System.out.println("3. Sair");

            System.out.print("Escolha uma opção: ");
            int choice = scanner.nextInt();
            scanner.nextLine(); // Consume the newline

            switch (choice) {
                case 1:
                    registerUser(scanner);
                    break;
                case 2:
                    String token = loginUser(scanner);
                    if (token != null) {
                        try {
                            if(checkIfUserIsAdmin(token))
                                adminMenu(scanner,token);
                            else
                                userMenu(scanner, token);
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    break;
                case 3:
                    System.out.println("Saindo...");
                    return;
                default:
                    System.out.println("Opção inválida.");
                    break;
            }
        }
    }
    public static boolean checkIfUserIsAdmin(String token) throws JSONException, IOException {
        String res = sendRequestAndShowResponse("http://localhost:8080/authorization", "GET", "bearer " + token, null);
        //System.out.println(res);
        return res.contains("ADMIN");
    }
    private static void registerUser(Scanner scanner) throws IOException {
        System.out.println("\nREGISTAR:");

        System.out.print("Nome: ");
        String nome = scanner.nextLine();

        System.out.print("Número de Identificação: ");
        String newId = scanner.nextLine();
        long id = 0;
        try {
            id = Long.parseLong(newId.trim());
        } catch (NumberFormatException e) {
            System.out.println("O Número de Identificação fornecido não é um número inteiro válido.");
            return;
        }

        System.out.print("Email: ");
        String email = scanner.nextLine();
        if(!ClientLogic.isValidEmail(email)){
            System.out.println("Email fornecido não é válido");
            return;
        }

        System.out.print("Password: ");
        String password = scanner.nextLine();

        String jsonBody = String.format("{\"nome\":\"%s\",\"numeroIdentificacao\":\"%s\",\"email\":\"%s\", \"password\":\"%s\"}", nome, id, email, password);
        String res = sendRequestAndShowResponse("http://localhost:8080/register", "POST", null, jsonBody);
        System.out.println(res);
    }
    private static String loginUser(Scanner scanner) throws IOException {
        System.out.println("Digite o email:");
        String email = scanner.nextLine();
        System.out.println("Digite a senha:");
        String password = scanner.nextLine();

        String credentials = Base64.getEncoder().encodeToString((email + ":" + password).getBytes());
        return sendRequestAndShowResponse("http://localhost:8080/login", "POST", "basic " + credentials, null);
    }
    private static void userMenu(Scanner scanner, String token) throws IOException {
        while (true) {
            System.out.println("\nMenu do Usuário:");
            System.out.println("1. Submeter código de evento");
            System.out.println("2. Visualizar presenças");
            System.out.println("3. Sair");

            System.out.print("Escolha uma opção: ");
            int choice = scanner.nextInt();
            scanner.nextLine(); // Consumir a nova linha

            switch (choice) {
                case 1:
                    submitEventCode(scanner, token);
                    break;
                case 2:
                    viewAttendance(token);
                    break;
                case 3:
                    System.out.println("Saindo do menu do usuário...");
                    return;
                default:
                    System.out.println("Opção inválida.");
                    break;
            }
        }
    }
    private static void viewAttendance(String token) throws IOException {
        String res = sendRequestAndShowResponse("http://localhost:8080/user", "GET", "bearer " + token, null);
        System.out.println();
        System.out.println(res);
    }
    private static void submitEventCode(Scanner scanner, String token) throws IOException {
        System.out.print("Digite o código do evento: ");
        String eventCode = scanner.nextLine();
        String res = sendRequestAndShowResponse("http://localhost:8080/user/"+eventCode, "POST", "bearer " + token, null);
        System.out.println();
        System.out.println(res);
    }
    private static void adminMenu(Scanner scanner, String token) throws IOException {
        while (true) {
            System.out.println("\nMenu do Administrador:");
            System.out.println("1. Criar evento");
            System.out.println("2. Eliminar evento");
            System.out.println("3. Consultar os eventos criados");
            System.out.println("4. Gerar código de presença");
            System.out.println("5. Consultar as presenças registadas");
            System.out.println("6. Sair");

            System.out.print("Escolha uma opção: ");
            int choice = scanner.nextInt();
            scanner.nextLine(); // Consumir a nova linha

            switch (choice) {
                case 1:
                    createEvent(scanner, token);
                    break;
                case 2:
                    deleteEvent(scanner, token);
                    break;
                case 3:
                    viewEvents(token);
                    break;
                case 4:
                    generateRegistrationCode(scanner,token);
                    break;
                case 5:
                    viewAttendancesAdmin(scanner, token);
                    break;
                case 6:
                    System.out.println("Saindo do menu do administrador...");
                    return;
                default:
                    System.out.println("Opção inválida.");
                    break;
            }
        }
    }
    private static void viewAttendancesAdmin(Scanner scanner, String token) throws IOException {
        System.out.println("\nCONSULTAR PRESENÇAS DE UM EVENTO:");

        System.out.print("Nome do Evento: ");
        String nomeEvento = scanner.nextLine().trim();

        String url = "http://localhost:8080/eventos/" + nomeEvento + "/presencas";
        String response = sendRequestAndShowResponse(url, "GET", "Bearer " + token, null);
        System.out.println(response);
    }
    private static void generateRegistrationCode(Scanner scanner, String token) throws IOException {
        System.out.println("\nGERAR CÓDIGO PARA EVENTO:");

        System.out.print("Nome do Evento: ");
        String nomeEvento = scanner.nextLine().trim();

        System.out.print("Validade do Código (em minutos): ");
        int validadeMinutos;
        try {
            validadeMinutos = Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Erro: A validade precisa ser um número inteiro.");
            return;
        }


        String url = "http://localhost:8080/eventos/" + nomeEvento + "/gerarcodigo?min=" + validadeMinutos;;
        String jsonBody = String.format("{\"min\":\"%d\"}", validadeMinutos);
        String response = sendRequestAndShowResponse(url, "POST", "bearer " + token, null);
        System.out.println(response);
    }
    private static void viewEvents(String token) throws IOException {
        String response = sendRequestAndShowResponse("http://localhost:8080/eventos", "GET", "bearer " + token, null);
        System.out.println(response);
    }
    private static void deleteEvent(Scanner scanner, String token) throws IOException {
        System.out.println("\nELIMINAR EVENTO:");

        System.out.print("Nome do Evento a ser eliminado: ");
        String nomeEvento = scanner.nextLine().trim();

        String response = sendRequestAndShowResponse("http://localhost:8080/eventos/" + nomeEvento, "DELETE", "bearer " + token, null);

        System.out.println(response);
    }
    private static void createEvent(Scanner scanner, String token) throws IOException {
        System.out.println("\nCRIAR EVENTO:");

        System.out.print("Nome do Evento: ");
        String nomeEvento = scanner.nextLine();

        System.out.print("Local do Evento: ");
        String localEvento = scanner.nextLine();

        System.out.print("Data do Evento (dd/MM/AAAA): ");
        String dataEvento = scanner.nextLine();
        if(!ClientLogic.isValidDateFormat(dataEvento)){
            System.out.println("Data inválida");
            return;
        }

        System.out.print("Hora de Início (HH:MM): ");
        String horaInicio = scanner.nextLine();
        if(!ClientLogic.isValidTimeFormat(horaInicio)){
            System.out.println("Hora inválida");
            return;
        }

        System.out.print("Hora de Fim (HH:MM): ");
        String horaFim = scanner.nextLine();
        if(!ClientLogic.isValidTimeFormat(horaFim)){
            System.out.println("Hora inválida");
            return;
        }

        if (!ClientLogic.isStartTimeBeforeEndTime(horaInicio, horaFim)) {
            System.out.println("A hora de início deve ser anterior à hora de fim.");
            return;
        }

        if (nomeEvento.isEmpty() || localEvento.isEmpty() || dataEvento.isEmpty() || horaInicio.isEmpty() || horaFim.isEmpty()) {
            System.out.println("Todos os campos são obrigatórios.");
            return;
        }

        String jsonBody = String.format(
                "{\"name\":\"%s\",\"location\":\"%s\",\"date\":\"%s\",\"startTime\":\"%s\",\"endTime\":\"%s\"}",
                nomeEvento, localEvento, dataEvento, horaInicio, horaFim);

        // Enviar a requisição e processar a resposta
        String response = sendRequestAndShowResponse(
                "http://localhost:8080/eventos", "POST", "bearer " + token, jsonBody
        );
        System.out.println(response);
    }
}
