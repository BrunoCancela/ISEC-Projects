package pt.isec.pd.springboot.m1.commands;

import pt.isec.pd.springboot.m1.server.ClientHandler;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ProcessAdminCommands {
    public static String createEvent(Connection conn, ComandoStruct comandoStruct) throws SQLException {
        String sql = "INSERT INTO evento (Designation, Local, Date, BeginningHour, EndHour) VALUES (?, ?, ?, ?, ?);";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, comandoStruct.getEvent()); // Designação do evento
            pstmt.setString(2, comandoStruct.getLocal()); // Local do evento
            pstmt.setString(3, comandoStruct.getData()); // Data do evento
            pstmt.setString(4, comandoStruct.getBeginHour()); // Hora de início
            pstmt.setString(5, comandoStruct.getEndHour()); // Hora de fim

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
//                ClientHandler.incrementDatabaseVersion(conn); // Incrementa a versão do banco de dados
                return "Evento criado com sucesso.";
            } else {
                return "Falha ao criar o evento.";
            }
        } catch (SQLException e) {
            System.err.println("Erro ao criar evento: " + e.getMessage());
            return "Erro ao criar evento.";
        }
    }

    public static String editEvent(Connection conn, ComandoStruct comandoStruct) throws SQLException {
        // Verificar se existem presenças para o nome do evento antes de permitir a edição
        if (hasAttendances(conn, comandoStruct.getEvent())) {
            return "Não é possível editar o evento, pois já existem presenças registradas.";
        }
        String sql = "UPDATE evento SET Designation = ?, Local = ?, Date = ?, BeginningHour = ?, EndHour = ? WHERE Designation = ?;";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, comandoStruct.getNewEventName()); // Novo nome do evento
            pstmt.setString(2, comandoStruct.getLocal()); // Novo local do evento
            pstmt.setString(3, comandoStruct.getDate()); // Nova data do evento
            pstmt.setString(4, comandoStruct.getBeginHour()); // Nova hora de início
            pstmt.setString(5, comandoStruct.getEndHour()); // Nova hora de fim
            pstmt.setString(6, comandoStruct.getEvent()); // Nome atual do evento para localizar o registro

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
//                ClientHandler.incrementDatabaseVersion(conn); // Incrementa a versão do banco de dados
                return "Evento atualizado com sucesso.";
            } else {
                return "Falha ao atualizar o evento. A designação do evento pode não existir.";
            }
        } catch (SQLException e) {
            System.err.println("Erro ao atualizar evento: " + e.getMessage());
            return "Erro ao atualizar evento.";
        }
    }

    public static String removeEvent(Connection conn, ComandoStruct comandoStruct) throws SQLException {
        if (hasAttendances(conn, comandoStruct.getEvent())) {
            return "Não é possível remover o evento, pois já existem presenças registradas.";
        }
        String sql = "DELETE FROM evento WHERE Designation = ?;";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, comandoStruct.getEvent()); // Nome (Designation) do evento

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
//                ClientHandler.incrementDatabaseVersion(conn); // Incrementa a versão do banco de dados
                return "Evento removido com sucesso.";
            } else {
                return "Falha ao remover o evento. O nome do evento pode não existir.";
            }
        } catch (SQLException e) {
            System.err.println("Erro ao remover evento: " + e.getMessage());
            return "Erro ao remover evento.";
        }
    }


    public static boolean hasAttendances(Connection conn, String eventName) {
        // Tentativa de obter o EventID com base no nome do evento
        String getEventIdSql = "SELECT EventID FROM evento WHERE Designation = ?";
        try (PreparedStatement pstmtGetEventId = conn.prepareStatement(getEventIdSql)) {
            pstmtGetEventId.setString(1, eventName);
            ResultSet rsEventId = pstmtGetEventId.executeQuery();

            if (rsEventId.next()) {
                int eventId = rsEventId.getInt("EventID");

                // Verifique se existem presenças para esse EventID
                String checkAttendanceSql = "SELECT COUNT(*) AS count FROM assiste WHERE EventID = ?";
                try (PreparedStatement pstmtCheckAttendance = conn.prepareStatement(checkAttendanceSql)) {
                    pstmtCheckAttendance.setInt(1, eventId);
                    ResultSet rsAttendance = pstmtCheckAttendance.executeQuery();

                    if (rsAttendance.next()) {
                        int count = rsAttendance.getInt("count");
                        return count > 0;
                    }
                }
            } else {
                System.out.println("Evento com o nome '" + eventName + "' não foi encontrado.");
            }
        } catch (SQLException e) {
            System.err.println("Erro ao verificar presenças para o evento: " + e.getMessage());
        }
        return false;
    }



    public static String listEvents(Connection conn, ComandoStruct comandoStruct) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT EventID, Designation, Local, Date, BeginningHour, EndHour FROM evento WHERE 1=1");

        // Adicionar filtros à consulta SQL conforme os critérios fornecidos
        List<Object> parameters = new ArrayList<>();
        if (!comandoStruct.getEvent().isEmpty()) {
            sql.append(" AND Designation LIKE ?");
            parameters.add("%" + comandoStruct.getEvent() + "%");
        }
        if (!comandoStruct.getBeginHour().isEmpty()) {
            sql.append(" AND BeginningHour >= ?");
            parameters.add(comandoStruct.getBeginHour());
        }
        if (!comandoStruct.getEndHour().isEmpty()) {
            sql.append(" AND EndHour <= ?");
            parameters.add(comandoStruct.getEndHour());
        }

        try (PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            // Definir os parâmetros na consulta preparada
            for (int i = 0; i < parameters.size(); i++) {
                pstmt.setObject(i + 1, parameters.get(i));
            }

            ResultSet rs = pstmt.executeQuery();
            StringBuilder sb = new StringBuilder();
            while (rs.next()) {
                sb.append("ID: ").append(rs.getInt("EventID"))
                        .append(", Designação: ").append(rs.getString("Designation"))
                        .append(", Local: ").append(rs.getString("Local"))
                        .append(", Data: ").append(rs.getString("Date"))
                        .append(", Início: ").append(rs.getString("BeginningHour"))
                        .append(", Fim: ").append(rs.getString("EndHour"))
                        .append("\n");
            }
            return sb.length() > 0 ? sb.toString() : "Nenhum evento encontrado.";
        }
    }
    public static String generateEventCode(Connection conn, ComandoStruct comandoStruct) throws SQLException {
        // Supondo que 'comandoStruct.getEvent()' retorna o nome do evento
        String eventName = comandoStruct.getEvent();

        // Supondo que 'comandoStruct.getPeriod()' retorna a validade do código em minutos como uma String
        String validityPeriod = comandoStruct.getPeriod();

        // Gerar um código único para o evento
        String uniqueCode = UUID.randomUUID().toString().substring(0, 8);

        // O SQL para inserir o novo código na tabela 'codigo_registo'
        String sqlInsertNewCode = "INSERT INTO codigo_registo (Code, EventID,ValidityDuration) " +
                "VALUES (?, (SELECT EventID FROM evento WHERE Designation = ?),?);";

        try (PreparedStatement pstmt = conn.prepareStatement(sqlInsertNewCode)) {
            // Definindo o código único gerado
            pstmt.setString(1, uniqueCode);

            // Definindo o nome do evento para a subconsulta que busca o ID do evento
            pstmt.setString(2, eventName);

            pstmt.setString(3, String.valueOf(validityPeriod));

            // Executando o comando de inserção
            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                // Se a inserção foi bem-sucedida, retorne o código gerado
                return "Código gerado com sucesso: " + uniqueCode;
            } else {
                // Se nenhuma linha foi afetada, o nome do evento pode não existir na tabela 'evento'
                return "Erro: Evento não encontrado ou código de presença não foi gerado.";
            }
        } catch (SQLException e) {
            // Caso uma exceção SQL ocorra, a capture e retorne uma mensagem de erro
            e.printStackTrace();
            return "Erro ao gerar código de presença: " + e.getMessage();
        }
    }

    public static String checkEventAttendance(Connection conn, ComandoStruct comandoStruct) throws SQLException {
        String eventName = comandoStruct.getEvent();
        // SQL to get event details and attendees using a subquery for the event ID
        String sqlEventDetails = "SELECT e.Designation, e.Local, e.Date, e.BeginningHour, e.EndHour " +
                "FROM evento e " +
                "WHERE e.Designation = ?";

        String sqlAttendees = "SELECT u.Username, u.IdentificationNumber, u.Email FROM utilizador u " +
                "JOIN assiste a ON u.UserID = a.UserID " +
                "JOIN evento e ON a.EventID = e.EventID " +
                "WHERE e.Designation = ?";

        StringBuilder result = new StringBuilder();

        try (PreparedStatement pstmtEvent = conn.prepareStatement(sqlEventDetails)) {
            pstmtEvent.setString(1, eventName);
            ResultSet rsEvent = pstmtEvent.executeQuery();

            if (rsEvent.next()) {
                result.append("\"Designação\";\"").append(rsEvent.getString("Designation")).append("\"\n");
                result.append("\"Local\";\"").append(rsEvent.getString("Local")).append("\"\n");
                result.append("\"Data\";\"").append(rsEvent.getString("Date")).append("\"\n");  // Assuming Date is in correct format
                result.append("\"Hora inicio\";\"").append(rsEvent.getString("BeginningHour")).append("\"\n");  // Time as string
                result.append("\"Hora fim\";\"").append(rsEvent.getString("EndHour")).append("\"\n\n");  // Time as string
            } else {
                return "Evento não encontrado.";
            }
        }

        try (PreparedStatement pstmtAttendees = conn.prepareStatement(sqlAttendees)) {
            pstmtAttendees.setString(1, eventName);
            ResultSet rsAttendees = pstmtAttendees.executeQuery();

            if (rsAttendees.isBeforeFirst()) {
                result.append("\"Nome\";\"Número identificação\";\"Email\"\n");
                while (rsAttendees.next()) {
                    result.append("\"").append(rsAttendees.getString("Username")).append("\";");
                    result.append("\"").append(rsAttendees.getString("IdentificationNumber")).append("\";");
                    result.append("\"").append(rsAttendees.getString("Email")).append("\"\n");
                }
            } else {
                result.append("Nenhuma presença encontrada para o evento.");
            }
        }catch (SQLException e) {
            // Log and handle the SQL exception
            System.err.println("SQL Error: " + e.getMessage());
            return "Erro ao processar o código do evento.";
        }

        return result.toString();
    }


    public static String listUserEvents(Connection conn, ComandoStruct comandoStruct) throws SQLException {
        String userEmail = comandoStruct.getEmail();

        // SQL para consultar eventos que o usuário assistiu
        String sql = "SELECT u.Username, u.IdentificationNumber, u.Email, " +
                "e.Designation, e.Local, e.Date, e.BeginningHour " +
                "FROM evento e " +
                "JOIN assiste a ON e.EventID = a.EventID " +
                "JOIN utilizador u ON a.UserID = u.UserID " +
                "WHERE u.Email = ?";
        StringBuilder result = new StringBuilder();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userEmail);
            ResultSet rs = pstmt.executeQuery();

            // Primeiro, adicione o cabeçalho CSV
            result.append("\"Nome\";\"Número identificação\";\"Email\"\n");

            // Se existir ao menos um resultado, adicione as informações do usuário
            if (rs.next()) {
                result.append(String.format("\"%s\";\"%s\";\"%s\"\n",
                        rs.getString("Username"),
                        rs.getString("IdentificationNumber"),
                        rs.getString("Email")));

                // Depois, adicione o cabeçalho para os eventos
                result.append("\"Designação\";\"Local\";\"Data\";\"Hora início\"\n");

                // Adicione a primeira linha do evento
                result.append(String.format("\"%s\";\"%s\";\"%s\";\"%s\"\n",
                        rs.getString("Designation"),
                        rs.getString("Local"),
                        rs.getString("Date"),
                        rs.getString("BeginningHour")));

                // Continue adicionando eventos se houver mais
                while (rs.next()) {
                    result.append(String.format("\"%s\";\"%s\";\"%s\";\"%s\"\n",
                            rs.getString("Designation"),
                            rs.getString("Local"),
                            rs.getString("Date"),
                            rs.getString("BeginningHour")));
                }
            } else {
                return "Usuário não encontrado ou sem eventos.";
            }
        }

        return result.toString();
    }

    public static String insertAttendance(Connection conn, ComandoStruct comandoStruct) throws SQLException {
        String eventName = comandoStruct.getEvent();
        String userEmail = comandoStruct.getEmail();

        // SQL para inserir a presença
        String sql = "INSERT INTO assiste (UserID, EventID) " +
                "SELECT u.UserID, e.EventID " +
                "FROM utilizador u, evento e " +
                "WHERE u.Email = ? AND e.Designation = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userEmail);
            pstmt.setString(2, eventName);
            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                return "Presença inserida com sucesso.";
            } else {
                return "Erro ao inserir presença. Verifique se o evento e o usuário existem.";
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "Erro ao inserir presença: " + e.getMessage();
        }
    }
    public static String removeAttendance(Connection conn, ComandoStruct comandoStruct) throws SQLException {
        String eventName = comandoStruct.getEvent();
        String userEmail = comandoStruct.getEmail();

        // Preparar a SQL para remover presenças
        String sql = userEmail.isEmpty() ?
                "DELETE FROM assiste WHERE EventID = (SELECT EventID FROM evento WHERE Designation = ?)" :
                "DELETE FROM assiste WHERE UserID = (SELECT UserID FROM utilizador WHERE Email = ?) AND EventID = (SELECT EventID FROM evento WHERE Designation = ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (userEmail.isEmpty()) {
                pstmt.setString(1, eventName);
            } else {
                pstmt.setString(1, userEmail);
                pstmt.setString(2, eventName);
            }
            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                return "Presenças eliminadas com sucesso.";
            } else {
                return "Erro ao eliminar presenças. Verifique se o evento e o usuário existem.";
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "Erro ao eliminar presenças: " + e.getMessage();
        }
    }

    public static String logout(Connection conn, ComandoStruct comandoStruct) {
        try {
            // Suponha que você tenha uma tabela que registra o estado do login
            // e que há uma coluna 'LoggedIn' que você atualiza.
            String sql = "UPDATE administradores SET LoggedIn = FALSE WHERE AdminEmail = ?";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                // Definir o email do administrador a ser deslogado
                pstmt.setString(1, comandoStruct.getEmail());

                // Executar a atualização
                int affectedRows = pstmt.executeUpdate();

                // Verifica se alguma linha foi afetada
                if (affectedRows > 0) {
                    // A linha foi atualizada, o administrador foi deslogado
                    return "SUCESSO";
                } else {
                    // Nenhuma linha afetada, administrador não encontrado ou já deslogado
                    return "Erro ao deslogar: administrador não encontrado ou já deslogado.";
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao deslogar: " + e.getMessage());
            return "Erro ao deslogar: " + e.getMessage();
        }
    }
}