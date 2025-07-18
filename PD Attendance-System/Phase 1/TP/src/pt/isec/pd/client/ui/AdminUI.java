package pt.isec.pd.client.ui;

import pt.isec.pd.client.model.ClientCon;
import pt.isec.pd.commands.ComandoStruct;
import pt.isec.pd.client.model.ClientLogic;
import javax.xml.crypto.Data;
import java.util.Scanner;

public class AdminUI {
    private Scanner scanner;
    private ClientCon clientCon;
    private boolean executando = true;

    private String csv1;
    private String csv2;
    private String email;
    public AdminUI(ClientCon clientCon, String email) {
        this.clientCon = clientCon;
        this.scanner = new Scanner(System.in);
        this.email = email;
    }

    public void iniciar() {
        boolean continuar = true;
        while (continuar) {
            exibirMenuPrincipal();
            int opcao = scanner.nextInt();
            scanner.nextLine();
            switch (opcao) {
                case 1:
                    criarEvento();
                    break;
                case 2:
                    editarEvento();
                    break;
                case 3:
                    eliminarEvento();
                    break;
                case 4:
                    consultarEventos();
                    break;
                case 5:
                    gerarCodigoEvento();
                    break;
                case 6:
                    consultarPresencasEvento();
                    break;
                case 7:
                    gerarCSVPresencas();
                    break;
                case 8:
                    consultarEventosUsuario();
                    break;
                case 9:
                    gerarCSVEventosUsuario();
                    break;
                case 10:
                    eliminarPresencas();
                    break;
                case 11:
                    inserirPresencas();
                    break;
                case 12:
                    logout();
                    continuar = false;
                    break;
                default:
                    System.out.println("Opção inválida.");
            }
        }
    }


    private void exibirMenuPrincipal() {
        System.out.println("\n--- Menu de Administração ---");
        System.out.println("1. Criar evento");
        System.out.println("2. Editar evento");
        System.out.println("3. Eliminar evento");
        System.out.println("4. Consultar eventos");
        System.out.println("5. Gerar código para evento");
        System.out.println("6. Consultar presenças em um evento");
        System.out.println("7. Gerar CSV de presenças em um evento");
        System.out.println("8. Consultar eventos de um usuário");
        System.out.println("9. Gerar CSV de eventos de um usuário");
        System.out.println("10. Eliminar presenças registradas em um evento");
        System.out.println("11. Inserir presenças em um evento");
        System.out.println("12. Logout");
        System.out.print("Escolha uma opção: ");
    }
    private void criarEvento() {
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

        ComandoStruct comando = new ComandoStruct();
        comando.setComando(ComandoStruct.ComandoType.CREATE_EVENT);
        comando.setEvent(nomeEvento);
        comando.setLocal(localEvento);
        comando.setData(dataEvento);
        comando.setBeginHour(horaInicio);
        comando.setEndHour(horaFim);

        System.out.println("Enviando os seguintes dados do evento para o servidor:");
        System.out.println("Nome do Evento: " + nomeEvento);
        System.out.println("Local do Evento: " + localEvento);
        System.out.println("Data do Evento: " + dataEvento);
        System.out.println("Hora de Início: " + horaInicio);
        System.out.println("Hora de Fim: " + horaFim);

        // Enviar o comando para o servidor e receber a resposta
        String resposta = clientCon.enviarComando(comando);
        if (resposta != null && resposta.contains("sucesso")) { // Verifica se a resposta contém a palavra "sucesso"
            System.out.println("Evento criado com sucesso.");
        } else {
            System.out.println("Erro ao criar o evento: " + resposta);
        }
    }


    private void editarEvento() {
        System.out.println("\nEDITAR EVENTO:");

        System.out.print("Nome Atual do Evento: ");
        String nomeAtualEvento = scanner.nextLine();

        System.out.print("Novo Nome do Evento: ");
        String novoNomeEvento = scanner.nextLine();

        System.out.print("Novo Local do Evento: ");
        String novoLocalEvento = scanner.nextLine();

        System.out.print("Nova Data do Evento (dd/MM/AAAA): ");
        String novaDataEvento = scanner.nextLine();
        if(!ClientLogic.isValidDateFormat(novaDataEvento)){
            System.out.println("Data inválida");
            return;
        }
        System.out.print("Nova Hora de Início (HH:MM): ");
        String novaHoraInicio = scanner.nextLine();
        if(!ClientLogic.isValidTimeFormat(novaHoraInicio)){
            System.out.println("Hora inválida");
            return;
        }
        System.out.print("Nova Hora de Fim (HH:MM): ");
        String novaHoraFim = scanner.nextLine();
        if(!ClientLogic.isValidTimeFormat(novaHoraFim)){
            System.out.println("Hora inválida");
            return;
        }
        if (novoNomeEvento.isEmpty() || novoLocalEvento.isEmpty() || novaDataEvento.isEmpty() || novaHoraInicio.isEmpty() || novaHoraFim.isEmpty()) {
            System.out.println("Todos os campos são obrigatórios.");
            return;
        }

        ComandoStruct comando = new ComandoStruct();
        comando.setComando(ComandoStruct.ComandoType.EDIT_EVENT);
        comando.setEvent(nomeAtualEvento); // Nome atual para identificar o evento a ser editado
        comando.setNewEventName(novoNomeEvento); // Novo nome do evento
        comando.setNewEventLocation(novoLocalEvento); // Novo local do evento
        comando.setNewEventDate(novaDataEvento); // Nova data do evento
        comando.setNewEventBeginHour(novaHoraInicio); // Nova hora de início
        comando.setNewEventEndHour(novaHoraFim); // Nova hora de fim

        System.out.println("Enviando os seguintes dados do evento para o servidor:");
        System.out.println("Nome Atual do Evento: " + nomeAtualEvento);
        System.out.println("Novo Nome do Evento: " + novoNomeEvento);
        System.out.println("Novo Local do Evento: " + novoLocalEvento);
        System.out.println("Nova Data do Evento: " + novaDataEvento);
        System.out.println("Nova Hora de Início: " + novaHoraInicio);
        System.out.println("Nova Hora de Fim: " + novaHoraFim);

        // Enviar o comando para o servidor e receber a resposta
        String resposta = clientCon.enviarComando(comando);
        if (resposta != null && resposta.contains("sucesso")) {
            System.out.println("Evento editado com sucesso.");
        } else {
            System.out.println("Erro ao editar o evento: " + resposta);
        }
    }


    private void eliminarEvento() {

        System.out.println("\nELIMINAR EVENTO:");

        System.out.print("Nome do Evento a ser eliminado: ");
        String nomeEvento = scanner.nextLine().trim();

        ComandoStruct comando = new ComandoStruct();
        comando.setComando(ComandoStruct.ComandoType.REMOVE_EVENT);
        comando.setEvent(nomeEvento);

        String resposta = clientCon.enviarComando(comando);
        System.out.println(resposta);
    }

    private void consultarEventos() {
        System.out.println("\nCONSULTAR EVENTOS:");

        // Exemplo de coleta de filtros
        System.out.print("Nome do Evento (deixe em branco para não filtrar): ");
        String nomeEvento = scanner.nextLine().trim();

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

        ComandoStruct comando = new ComandoStruct();
        comando.setComando(ComandoStruct.ComandoType.CHECK_CREATED_EVENTS);
        comando.setEvent(nomeEvento); // Pode ser "" se o usuário não inserir um nome
        comando.setBeginHour(horaInicio);
        comando.setEndHour(horaFim);
        // Enviar os critérios de filtro para o servidor
        String resposta = clientCon.enviarComando(comando);
        System.out.println(resposta);
    }

    private void gerarCodigoEvento() {
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

        ComandoStruct comando = new ComandoStruct();
        comando.setComando(ComandoStruct.ComandoType.GENERATE_NEW_CODE_EVENT);
        comando.setEvent(nomeEvento); // O nome do evento
        comando.setPeriod(String.valueOf(validadeMinutos)); // A validade do código

        // Enviar o comando para o servidor
        String resposta = clientCon.enviarComando(comando);

        // Processar a resposta do servidor
        System.out.println(resposta);
    }

    private void consultarPresencasEvento() {
        System.out.println("\nCONSULTAR PRESENÇAS EM UM EVENTO:");

        System.out.print("Nome do Evento: ");
        String nomeEvento = scanner.nextLine().trim();

        // Criar comando para consultar presenças
        ComandoStruct comando = new ComandoStruct();
        comando.setComando(ComandoStruct.ComandoType.CHECK_EVENT_ATTENDANCE);
        comando.setEvent(nomeEvento);

        // Enviar o comando para o servidor
        String resposta = clientCon.enviarComando(comando);

        if(!resposta.equals("Nenhum evento encontrado.")){
            csv1 = resposta;
        }
        // Processar e exibir a resposta do servidor
        System.out.println(resposta);
    }


    private void gerarCSVPresencas() {
        System.out.println(ClientLogic.gerarArquivoCSV(csv1,email,"_precensas_evento"));
    }
    private void consultarEventosUsuario() {
        System.out.println("\nCONSULTAR EVENTOS DE UM USUÁRIO:");

        System.out.print("Email do Usuário: ");
        String userEmail = scanner.nextLine().trim();

        // Criar comando para consultar eventos
        ComandoStruct comando = new ComandoStruct();
        comando.setComando(ComandoStruct.ComandoType.CHECK_USER_ATTENDANCE_TO_ALL_EVENTS);
        comando.setEmail(userEmail);

        // Enviar o comando para o servidor
        String resposta = clientCon.enviarComando(comando);

        // Processar e exibir a resposta do servidor
        if(!resposta.equals("Usuário não encontrado ou sem eventos.")){
            csv2 = resposta;
        }
        System.out.println(resposta);
    }

    private void gerarCSVEventosUsuario() {
        System.out.println(ClientLogic.gerarArquivoCSV(csv2,email,"_eventos_user"));
    }
    private void eliminarPresencas() {
        System.out.println("\nELIMINAR PRESENÇAS DE UM EVENTO:");

        System.out.print("Nome do Evento: ");
        String nomeEvento = scanner.nextLine().trim();

        System.out.print("Email do Usuário (deixe em branco para eliminar todas as presenças): ");
        String userEmail = scanner.nextLine().trim();

        // Criar comando para eliminar presença
        ComandoStruct comando = new ComandoStruct();
        comando.setComando(ComandoStruct.ComandoType.REMOVE_USER_ATTENDANCE);
        comando.setEvent(nomeEvento);
        comando.setEmail(userEmail); // Isso pode ser uma string vazia se todas as presenças devem ser removidas

        // Enviar o comando para o servidor
        String resposta = clientCon.enviarComando(comando);

        // Processar e exibir a resposta do servidor
        System.out.println(resposta);
    }
    private void inserirPresencas() {
        System.out.println("\nINSERIR PRESENÇAS EM UM EVENTO:");

        System.out.print("Nome do Evento: ");
        String nomeEvento = scanner.nextLine().trim();

        System.out.print("Email do Usuário: ");
        String userEmail = scanner.nextLine().trim();

        // Criar comando para inserir presença
        ComandoStruct comando = new ComandoStruct();
        comando.setComando(ComandoStruct.ComandoType.INSERT_USER_ATTENDANCE);
        comando.setEvent(nomeEvento);
        comando.setEmail(userEmail);

        // Enviar o comando para o servidor
        String resposta = clientCon.enviarComando(comando);

        // Processar e exibir a resposta do servidor
        System.out.println(resposta);
    }

    private void logout() {
        try {
            clientCon.desconectar();
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Erro ao fazer logout: " + e.getMessage());
        }
    }

//    public void logout() {
//        ComandoStruct command = new ComandoStruct();
//        command.setComando(ComandoStruct.ComandoType.USER_LOGOUT);
//        command.setUser("admin@gmail.com");
//
//        // Enviar o comando para o servidor
//        String response = clientCon.enviarComando(command);
//
//        // Processar a resposta do servidor
//        if ("SUCESSO".equalsIgnoreCase(response)) {
//            System.out.println("Logout realizado com sucesso.");
//            // Executar a lógica adicional necessária após o logout
//            // Por exemplo, fechar a conexão com o servidor, terminar o programa, etc.
//            clientCon.desconectar();
//            System.exit(0);
//        } else {
//            System.out.println("Erro ao fazer logout: " + response);
//        }
//    }

}
