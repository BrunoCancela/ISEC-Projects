package pt.isec.pd.commands;

import pt.isec.pd.server.ClientHandler;

import java.sql.*;

public class ProcessUserCommands {
    public static String getUserInfo(Connection conn, String email) throws SQLException {
        String sql = "SELECT Username, IdentificationNumber, Email, Role FROM utilizador WHERE Email = ?;";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String userInfo = "Username: " + rs.getString("Username") +
                        ", ID: " + rs.getString("IdentificationNumber") +
                        ", Email: " + rs.getString("Email") +
                        ", Role: " + rs.getString("Role");
                return userInfo;
            } else {
                return "User not found";
            }
        }
    }

    public static String updateUserInfo(Connection conn, String oldEmail, String newUsername, String newPassword) throws SQLException {
        String oldName = null;
        String oldPass = null;

        String sqlSelect = "SELECT Username, Password FROM utilizador WHERE Email = ?;";
        try (PreparedStatement pstmt = conn.prepareStatement(sqlSelect)) {
            pstmt.setString(1, oldEmail);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                oldName = rs.getString("Username");
                oldPass = rs.getString("Password");
            } else {
                return "User not found.";
            }
        }

        String sql = "UPDATE utilizador SET Username = ?, Password = ? WHERE Email = ?;";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newUsername.isEmpty() ? oldName : newUsername);
            pstmt.setString(2, newPassword.isEmpty() ? oldPass : newPassword);
            pstmt.setString(3, oldEmail);
            int updatedRows = pstmt.executeUpdate();
            if (updatedRows > 0) {
                ClientHandler.incrementDatabaseVersion(conn);
                return "User info updated successfully";
            } else {
                return "No update performed";
            }
        }
    }
    public static String submitEventCode(Connection conn, String userEmail, String eventCode) throws SQLException {
        // Query to check if the registration code is the most recent and still valid
        String sqlCheck = "SELECT cr.EventID, cr.ValidityDuration, cr.Timestamp, e.Date, e.BeginningHour, e.EndHour " +
                "FROM codigo_registo cr " +
                "JOIN evento e ON cr.EventID = e.EventID " +
                "WHERE cr.Code = ? " +
                "AND cr.RegistrationCodeID = (SELECT MAX(RegistrationCodeID) FROM codigo_registo WHERE EventID = cr.EventID);";

        try (PreparedStatement pstmtCheck = conn.prepareStatement(sqlCheck)) {
            pstmtCheck.setString(1, eventCode);
            try (ResultSet rs = pstmtCheck.executeQuery()) {
                if (rs.next()) {
                    int eventID = rs.getInt("EventID");
                    long validityDuration = rs.getLong("ValidityDuration");
                    Timestamp codeTimestamp = rs.getTimestamp("Timestamp");

                    long codeValidUntilMillis = codeTimestamp.getTime() + validityDuration * 60 * 1000;
                    if (System.currentTimeMillis() <= codeValidUntilMillis) {
                        if (isUserAlreadyRegisteredForEvent(conn, userEmail, eventID)) {
                            return "Não é possível registrar a assistência: já registrado em outro evento ou horário incompatível.";
                        }

                        String sqlInsert = "INSERT INTO assiste (UserID, EventID) " +
                                "SELECT UserID, ? FROM utilizador " +
                                "WHERE Email = ?";

                        try (PreparedStatement pstmtInsert = conn.prepareStatement(sqlInsert)) {
                            pstmtInsert.setInt(1, eventID);
                            pstmtInsert.setString(2, userEmail);
                            int rowsAffected = pstmtInsert.executeUpdate();
                            if (rowsAffected > 0) {
                                ClientHandler.incrementDatabaseVersion(conn);
                                return "Event code submitted successfully.";
                            } else {
                                return "Erro desconhecido ao registrar assistência.";
                            }
                        }
                    } else {
                        return "Código de evento expirou.";
                    }
                } else {
                    return "Código de evento inválido ou não é o mais recente.";
                }
            }
        } catch (SQLException e) {
            System.err.println("SQL Error: " + e.getMessage());
            return "Erro ao processar o código do evento.";
        }
    }

    public static boolean isUserRegisteredForOverlappingEvent(Connection conn, String userEmail, int eventID) throws SQLException {
        String sqlAlreadyRegistered = "SELECT COUNT(*) FROM assiste a " +
                "JOIN evento e ON a.EventID = e.EventID " +
                "JOIN utilizador u ON a.UserID = u.UserID " +
                "WHERE u.Email = ? AND e.EventID != ? " +
                "AND e.Date = CURRENT_DATE " +
                "AND ((e.BeginningHour < (SELECT EndHour FROM evento WHERE EventID = ?)) " +
                "AND (e.EndHour > (SELECT BeginningHour FROM evento WHERE EventID = ?)))";

        try (PreparedStatement pstmt = conn.prepareStatement(sqlAlreadyRegistered)) {
            pstmt.setString(1, userEmail);
            pstmt.setInt(2, eventID);
            pstmt.setInt(3, eventID);
            pstmt.setInt(4, eventID);
            System.out.println(eventID);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    System.out.println(rs.getString(1));
                    int count = 1;

                    return count > 0; // If count is more than 0, the user is registered for an overlapping event
                }
            }
        }

        return false;
    }

    public static boolean isUserAlreadyRegisteredForEvent(Connection conn, String userEmail, int eventID) throws SQLException {
        // This SQL query checks if the user is already registered for the specific event
        String sqlCheckRegistration = "SELECT COUNT(*) FROM assiste a " +
                "JOIN utilizador u ON a.UserID = u.UserID " +
                "WHERE u.Email = ? AND a.EventID = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sqlCheckRegistration)) {
            pstmt.setString(1, userEmail);
            pstmt.setInt(2, eventID);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt(1);
                    return count > 0;
                }
            }
        }
        return isUserRegisteredForOverlappingEvent(conn,userEmail,eventID); // If the user is not registered for the event, return false
    }


    public static String queryAttendance(Connection conn, String userEmail, String filterType, String period, String eventName) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT u.Username, u.IdentificationNumber, u.Email, e.Designation, e.Local, e.Date, e.BeginningHour " +
                "FROM assiste a " +
                "JOIN utilizador u ON a.UserID = u.UserID " +
                "JOIN evento e ON a.EventID = e.EventID " +
                "WHERE u.Email = ?");

        // Adiciona condição ao SQL baseado no tipo de filtro, se fornecido
        if (filterType.equalsIgnoreCase("p") && !period.isEmpty()) {
            sql.append(" AND e.Date = ?");
        } else if (filterType.equalsIgnoreCase("n") && !eventName.isEmpty()) {
            sql.append(" AND e.Designation = ?");
        }

        try (PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            pstmt.setString(1, userEmail);

            // Seta o parâmetro adicional se um filtro foi aplicado
            if ((filterType.equalsIgnoreCase("p") && !period.isEmpty()) ||
                    (filterType.equalsIgnoreCase("n") && !eventName.isEmpty())) {
                pstmt.setString(2, filterType.equalsIgnoreCase("p") ? period : eventName);
            }

            ResultSet rs = pstmt.executeQuery();

            if (!rs.isBeforeFirst()) { // Verifica se o ResultSet está vazio
                return "Vazio";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("\"Nome\";\"Número identificação\";\"Email\"\n");
            boolean firstRow = false;
            while (rs.next()) {
                if (!firstRow) {
                    // Adiciona as informações do usuário e do evento
                    sb.append("\"").append(rs.getString("Username")).append("\";");
                    sb.append("\"").append(rs.getString("IdentificationNumber")).append("\";");
                    sb.append("\"").append(rs.getString("Email")).append("\"\n");
                    firstRow=true;
                }
                sb.append("\"Designação\";\"Local\";\"Data\";\"Hora início\"\n");
                sb.append("\"").append(rs.getString("Designation")).append("\";");
                sb.append("\"").append(rs.getString("Local")).append("\";");
                sb.append("\"").append(rs.getString("Date")).append("\";");
                sb.append("\"").append(rs.getString("BeginningHour")).append("\"\n");
            }
            return sb.toString();
        }
    }
}