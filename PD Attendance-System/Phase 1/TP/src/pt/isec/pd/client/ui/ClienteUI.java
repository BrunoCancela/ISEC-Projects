package pt.isec.pd.client.ui;

import com.mysql.cj.xdevapi.Client;
import pt.isec.pd.client.model.ClientCon;
import pt.isec.pd.client.model.ClientLogic;
import pt.isec.pd.commands.ComandoStruct;

import java.util.Scanner;

public class ClienteUI {

    private static Scanner scanner;

    private static ClientCon clientCon;

    private static String email;
    private static String csv;

    ClienteUI(ClientCon con, String email){
        clientCon = con;
        this.email = email;
        scanner = new Scanner(System.in);
    }

    public void iniciar() {
        boolean continuar = true;
        while (continuar) {
            exibirMenuPrincipal();
            int opcao = scanner.nextInt();
            switch (opcao) {
                case 1:
                    editarDadosRegistro();
                    break;
                case 2:
                    submeterCodigoEvento();
                    break;
                case 3:
                    consultarPresencas();
                    break;
                case 4:
                    gerarCSV();
                    break;
                case 5:
                    logout();
                    continuar = false;
                    break;
                default:
                    System.out.println("Opção inválida.");
            }
        }
    }

    private static void exibirMenuPrincipal() {
        System.out.println("\n--- Menu Cliente ---");
        System.out.println("1. Editar dados de registro");
        System.out.println("2. Submeter código de evento");
        System.out.println("3. Consultar presenças");
        System.out.println("4. Gerar arquivo CSV");
        System.out.println("5. Logout");
        System.out.print("Escolha uma opção: ");
    }
    private void editarDadosRegistro() {
        try {
            // Fetch current user information from the server
            ComandoStruct comando = new ComandoStruct();
            comando.setComando(ComandoStruct.ComandoType.GET_USER_INFO);
            comando.setEmail(email);
            String userInfo = clientCon.enviarComando(comando);
            System.out.println("Dados atuais: " + userInfo);

            // User decides what to edit
            System.out.println("Editar Dados de Registro");
            scanner.nextLine();
            System.out.print("Novo nome de usuário (deixe em branco para não alterar): ");
            String newUser = scanner.nextLine();
            System.out.print("Nova senha (deixe em branco para não alterar): ");
            String newPassword = scanner.nextLine();

            // Check if the user wants to update each field
            if (!newUser.trim().isEmpty() || !newPassword.trim().isEmpty()) {

                ComandoStruct comando2 = new ComandoStruct();
                comando2.setComando(ComandoStruct.ComandoType.UPDATE_USER_INFO);
                comando2.setEmail(email);
                comando2.setUser(newUser);
                comando2.setPassword(newPassword);

                String response = clientCon.enviarComando(comando2);
                System.out.println(response);

            } else {
                System.out.println("Nenhuma alteração realizada.");
            }
        } catch (Exception e) {
            System.err.println("Erro ao obter ou atualizar informações do usuário: " + e.getMessage());
        }
    }

    private static void submeterCodigoEvento() {
        System.out.print("Digite o código do evento: ");
        scanner.nextLine();
        String eventCode = scanner.nextLine();

        // Criar um comando para enviar ao servidor
        ComandoStruct comando = new ComandoStruct();
        comando.setComando(ComandoStruct.ComandoType.REGISTER_ATTENDANCE);
        comando.setEventCode(eventCode);
        comando.setEmail(email); // Supondo que 'email' seja o identificador do usuário

        // Enviar o comando ao servidor e receber a resposta
        String response = clientCon.enviarComando(comando);

        // Processar e exibir a resposta
        System.out.println(response);
    }

    private void consultarPresencas() {
        try {
            System.out.println("Consultar Presenças");
            System.out.print("Filtrar por período (p), nome do evento (n) ou sem filtro (deixe em branco)? (p/n): ");
            scanner.nextLine(); // Limpar buffer do scanner
            String filterType = scanner.nextLine();

            String period = "";
            String eventName = "";
            ComandoStruct comando = new ComandoStruct();
            comando.setComando(ComandoStruct.ComandoType.CHECK_ATTENDANCES);
            comando.setFilter(filterType);
            comando.setEmail(email);

            if (filterType.equalsIgnoreCase("p")) {
                System.out.print("Data do Evento (dd/MM/AAAA): ");
                period = scanner.nextLine();
                if(!ClientLogic.isValidDateFormat(period)){
                    System.out.println("Data inválida");
                    return;
                }
                comando.setPeriod(period);
            } else if (filterType.equalsIgnoreCase("n")) {
                System.out.print("Digite o nome do evento: ");
                eventName = scanner.nextLine();
                comando.setEvent(eventName);
            }

            // Enviar a consulta e receber os resultados
            String response = clientCon.enviarComando(comando);
            if(!response.equals("Vazio")){
                csv = response;
            }
            System.out.println("Resultados da consulta de presenças:\n" + response);

        } catch (Exception e) {
            System.err.println("Erro ao consultar presenças: " + e.getMessage());
        }
    }

    private void gerarCSV() {
        System.out.println(ClientLogic.gerarArquivoCSV(csv,email,"_presencas")); // Exibe o conteúdo CSV
    }

    private static void logout() {
        try {
            clientCon.desconectar();
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Erro ao fazer logout: " + e.getMessage());
        }
    }
}