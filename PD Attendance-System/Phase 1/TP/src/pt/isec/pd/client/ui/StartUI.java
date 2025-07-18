package pt.isec.pd.client.ui;

import pt.isec.pd.client.model.ClientCon;
import pt.isec.pd.client.model.ClientLogic;
import pt.isec.pd.commands.ComandoStruct;

import java.util.Scanner;

public class StartUI {

    private static Scanner scanner;
    private static ClientCon clientCon;


    public static void main(String[] args){
        if (args.length != 2) {
            System.out.println("Erro: missing (serverAddress serverUdpPort)");
            return;
        }

        scanner = new Scanner(System.in);
        clientCon = new ClientCon((args[0]), Integer.parseInt(args[1]));

        if (!clientCon.conectar()) {
            System.exit(1);
        }

        while (true) {
            mostrarMenuPrincipal();
            int opcao = scanner.nextInt();
            switch (opcao) {
                case 1:
                    registarUtilizador();
                    break;
                case 2:
                    autenticarUsuario();
                    break;
                case 3:
                    System.exit(0);
                    break;
                default:
                    System.out.println("Opção inválida.");
            }
        }
    }

    private static void mostrarMenuPrincipal() {
        System.out.println("\n--- Menu Principal ---");
        System.out.println("1. Registrar novo utilizador");
        System.out.println("2. Autenticar utilizador");
        System.out.println("3. Sair");
        System.out.print("Escolha uma opção: ");
    }

    private static void registarUtilizador() {
        System.out.println("\nREGISTAR:");
        scanner.nextLine(); // Consume the leftover newline from previous input


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

        ComandoStruct comando = new ComandoStruct();
        comando.setComando(ComandoStruct.ComandoType.REGISTER_USER);
        comando.setUser(nome);
        comando.setEmail(email);
        comando.setPassword(password);
        comando.setID(id);

        System.out.println("ComandoStruct: " + comando.getComando() + ", " + comando.getUser() + ", " + comando.getEmail() + ", " + comando.getPassword() + ", " + comando.getID());

        String resposta = clientCon.enviarComando(comando);
        System.out.println(resposta);
    }

    private static void autenticarUsuario() {
        System.out.println("\nAUNTETICAÇÃO:");
        scanner.nextLine();

        System.out.print("Email: ");
        String email = scanner.nextLine();

        System.out.print("Password: ");
        String password = scanner.nextLine();

        ComandoStruct comando = new ComandoStruct();
        comando.setComando(ComandoStruct.ComandoType.USER_LOGIN);
        comando.setEmail(email);
        comando.setPassword(password);

        String resposta = clientCon.enviarComando(comando);

        if(resposta.equals("Admin")){
            AdminUI adminUI = new AdminUI(clientCon,email);
            adminUI.iniciar();
        }else if(resposta.equals("Client")){
            ClienteUI clienteUI = new ClienteUI(clientCon,email);
            clienteUI.iniciar();
        }else{
            System.out.println(resposta);
        }
    }
}
