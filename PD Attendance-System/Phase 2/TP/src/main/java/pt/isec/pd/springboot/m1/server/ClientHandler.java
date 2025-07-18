package pt.isec.pd.springboot.m1.server;

import pt.isec.pd.springboot.m1.commands.ComandoStruct;
import pt.isec.pd.springboot.m1.commands.ProcessUserCommands;
import pt.isec.pd.springboot.m1.commands.ProcessAdminCommands;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class ClientHandler extends Thread {
    private static Map<String, ClientHandler> loggedInUsers = new HashMap<>();
    private Socket clientSocket;
    private ObjectInputStream oin;
    private ObjectOutputStream oout;
    private String userEmail;
    private static Connection conn;

    private static DatabaseVersionUpdateListener updateListener;
    public ClientHandler(Socket socket, Connection dbConnection, DatabaseVersionUpdateListener updateListener) {
        this.clientSocket = socket;
        conn = dbConnection;
        this.updateListener = updateListener;
    }

    public void run() {
        try {
            oout = new ObjectOutputStream(clientSocket.getOutputStream());
            oin = new ObjectInputStream(clientSocket.getInputStream());

            while (!clientSocket.isClosed()) {
                ComandoStruct request = (ComandoStruct) oin.readObject();
                String response = processarComando(request);
                oout.writeObject(response);
                oout.flush();
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Erro ao comunicar com o cliente: " + e.getMessage());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            closeResources();
        }
    }

    private String processarComando(ComandoStruct comandoStruct) throws SQLException {
        switch (comandoStruct.getComando()) {
            case USER_LOGIN:
                return userLogin(comandoStruct.getEmail(), comandoStruct.getPassword());
            case REGISTER_USER:
                return registerUser(comandoStruct.getUser(),
                        comandoStruct.getID(),
                        comandoStruct.getEmail(),
                        comandoStruct.getPassword(),
                        "Client");

            case GET_USER_INFO:
                return ProcessUserCommands.getUserInfo(conn,comandoStruct.getEmail());

            case UPDATE_USER_INFO:
                return ProcessUserCommands.updateUserInfo(conn,
                        comandoStruct.getEmail(),
                        comandoStruct.getUser(),
                        comandoStruct.getPassword());

            case REGISTER_ATTENDANCE:
                return ProcessUserCommands.submitEventCode(conn,comandoStruct.getEmail(),comandoStruct.getEventCode());

            case CHECK_ATTENDANCES:
                return ProcessUserCommands.queryAttendance(conn,comandoStruct.getEmail(),comandoStruct.getFilter(),comandoStruct.getPeriod(),comandoStruct.getEvent());

            case CREATE_EVENT:
                return ProcessAdminCommands.createEvent(conn, comandoStruct);

            case EDIT_EVENT:
                return ProcessAdminCommands.editEvent(conn, comandoStruct) ;

            case REMOVE_EVENT:
                return ProcessAdminCommands.removeEvent(conn, comandoStruct);

            case CHECK_CREATED_EVENTS:
                // Implement logic to check created events with filters
                return ProcessAdminCommands.listEvents(conn, comandoStruct);

            case GENERATE_NEW_CODE_EVENT:
                return ProcessAdminCommands.generateEventCode(conn, comandoStruct);

            case CHECK_EVENT_ATTENDANCE:
                return ProcessAdminCommands.checkEventAttendance(conn, comandoStruct);

            case CHECK_USER_ATTENDANCE_TO_ALL_EVENTS:
                // Implement logic to check all attendances of a certain user
                return ProcessAdminCommands.listUserEvents(conn,comandoStruct);

            case REMOVE_USER_ATTENDANCE:
                // Implement logic to remove a certain user's attendance in an event
                return ProcessAdminCommands.removeAttendance(conn, comandoStruct);

            case INSERT_USER_ATTENDANCE:
                // Implement logic to insert attendance for a certain user in an event
                return ProcessAdminCommands.insertAttendance(conn, comandoStruct);

            case USER_LOGOUT:
                // Implement logic to logout
                return ProcessAdminCommands.logout(conn,comandoStruct);
            default:
                return "Comando desconhecido";
        }
    }

    private void logout() {
        closeResources();
        interrupt(); // Interrupt the current thread
    }

    private void closeResources() {
        try {
            if (oin != null) {
                oin.close();
            }
            if (oout != null) {
                oout.close();
            }
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Erro ao fechar recursos: " + e.getMessage());
        } finally {
            synchronized (loggedInUsers) {
                loggedInUsers.remove(userEmail);
            }
        }
    }

    private static String registerUser(String user, long id, String email, String password, String role) throws SQLException {

        if (!emailExists(email)) {
            String sql = "INSERT INTO utilizador (Username, IdentificationNumber, Email, Password, Role) VALUES (?, ?, ?, ?, ?);";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, user);
                pstmt.setLong(2, id);
                pstmt.setString(3, email);
                pstmt.setString(4, password);
                pstmt.setString(5, role);
                pstmt.executeUpdate();
            }
           // incrementDatabaseVersion(conn);
            return "Registado com sucesso";
        } else {
            return "Email already exists. Please choose a different one.";
        }
    }
    private static boolean emailExists(String email) throws SQLException {
        String sql = "SELECT COUNT(*) FROM utilizador WHERE Email = ?;";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            return pstmt.executeQuery().getInt(1) > 0;
        }
    }
    private String userLogin(String email, String password) {
        synchronized (loggedInUsers) {
            if (loggedInUsers.containsKey(email)) {
                return "User already logged in with this email";
            }

            String sql = "SELECT Role FROM utilizador WHERE Email = ? AND Password = ?;";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, email);
                pstmt.setString(2, password);
                ResultSet resultSet = pstmt.executeQuery();

                // Check if the query returned any rows
                if (resultSet.next()) {
                    this.userEmail = email;
                    loggedInUsers.put(email, this);
                    return resultSet.getString("Role"); // Return the role
                }
            } catch (SQLException e) {
                System.err.println("Erro ao verificar usuário: " + e.getMessage());
            }
        }
        return "Nao foi possiver efetuar o login verifique o seu email e a sua password";
    }

    public static void incrementDatabaseVersion(Connection connAux) throws SQLException {
        int currentVersion = getCurrentVersion(connAux);
        int newVersion = currentVersion + 1;
        insertNewVersion(connAux,newVersion);
        updateListener.onDatabaseUpdate();
    }
    static int getCurrentVersion(Connection connAux) throws SQLException {
        String sql = "SELECT MAX(VersionNumber) FROM versao;";
        try (PreparedStatement pstmt = connAux.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            } else {
                return 0; // Default to 0 if no version found
            }
        }
    }
    private static void insertNewVersion(Connection connAux, int newVersion) throws SQLException {
        String sql = "INSERT INTO versao (VersionNumber) VALUES (?);";
        try (PreparedStatement pstmt = connAux.prepareStatement(sql)) {
            pstmt.setInt(1, newVersion);
            pstmt.executeUpdate();
        }
    }

}
