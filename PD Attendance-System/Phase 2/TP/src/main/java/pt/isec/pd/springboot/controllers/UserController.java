package pt.isec.pd.springboot.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pt.isec.pd.springboot.m1.commands.ComandoStruct;
import pt.isec.pd.springboot.m1.commands.ProcessUserCommands;
import pt.isec.pd.springboot.m1.database.DBConnection;
import pt.isec.pd.springboot.m1.server.ServidorPrincipal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@RestController
@RequestMapping("/user")
public class UserController {
    private final ServidorPrincipal servidorPrincipal;

    @Autowired
    public UserController(ServidorPrincipal servidorPrincipal) {
        this.servidorPrincipal = servidorPrincipal;
    }

    @PostMapping("/{code}")
    public ResponseEntity<Object> marcarPresenca(@PathVariable("code") String code, Authentication authentication) throws SQLException {

        String url = "jdbc:sqlite:" + "database.db";
        Connection dbConnection = DBConnection.connect(url);

        String res = processEventCode(dbConnection,code,authentication.getName());
        if(res.contains("sucesso")){
            servidorPrincipal.onDatabaseUpdate();
        }
        dbConnection.close();


        return ResponseEntity.ok(res);
    }

    @GetMapping
    public ResponseEntity<Object> verPresencas( Authentication authentication) throws SQLException {

        String url = "jdbc:sqlite:" + "database.db";
        Connection dbConnection = DBConnection.connect(url);

        ComandoStruct comandoStruct = new ComandoStruct();
        comandoStruct.setEmail(authentication.getName());

        String res = ProcessUserCommands.queryAttendance(dbConnection,authentication.getName(),"","","");

        dbConnection.close();

        return ResponseEntity.ok(res);
    }

    public String processEventCode(Connection conn, String eventCode, String userEmail) throws SQLException {
        if (!doesEventExist(conn, eventCode)) {
            return "Evento não encontrado ou inválido.";
        }

        String sqlCheck = "SELECT cr.EventID " +
                "FROM codigo_registo cr " +
                "WHERE cr.Code = ? " +
                "AND cr.RegistrationCodeID = (SELECT MAX(RegistrationCodeID) FROM codigo_registo WHERE EventID = cr.EventID);";

        try (PreparedStatement pstmtCheck = conn.prepareStatement(sqlCheck)) {
            pstmtCheck.setString(1, eventCode);
            try (ResultSet rs = pstmtCheck.executeQuery()) {
                if (rs.next()) {
                    int eventID = rs.getInt("EventID");

                    if (!isUserAlreadyRegisteredForEvent(conn, userEmail, eventID)) {
                        return insertAttendance(conn, userEmail, eventID);
                    } else {
                        return "Usuário já registrado neste evento.";
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

    private boolean isUserAlreadyRegisteredForEvent(Connection conn, String userEmail, int eventID) throws SQLException {
        String sqlCheckUser = "SELECT COUNT(*) AS count FROM assiste WHERE UserID = (SELECT UserID FROM utilizador WHERE Email = ?) AND EventID = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sqlCheckUser)) {
            pstmt.setString(1, userEmail);
            pstmt.setInt(2, eventID);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("count") > 0;
                }
            }
        }
        return false;
    }

    private String insertAttendance(Connection conn, String userEmail, int eventID) throws SQLException {
        String sqlInsert = "INSERT INTO assiste (UserID, EventID) " +
                "SELECT UserID, ? FROM utilizador WHERE Email = ?";

        try (PreparedStatement pstmtInsert = conn.prepareStatement(sqlInsert)) {
            pstmtInsert.setInt(1, eventID);
            pstmtInsert.setString(2, userEmail);
            int rowsAffected = pstmtInsert.executeUpdate();
            if (rowsAffected > 0) {
                return "Presença registrada com sucesso.";
            } else {
                return "Erro ao registrar a presença.";
            }
        }
    }

    private boolean doesEventExist(Connection conn, String eventCode) throws SQLException {
        String sql = "SELECT COUNT(*) AS count FROM evento WHERE EventID = (SELECT EventID FROM codigo_registo WHERE Code = ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, eventCode);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("count") > 0;
                }
            }
        }
        return false;
    }


}
