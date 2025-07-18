package pt.isec.pd.springboot.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pt.isec.pd.springboot.m1.database.DBConnection;
import pt.isec.pd.springboot.m1.server.ServidorPrincipal;
import pt.isec.pd.springboot.models.UserConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@RestController
@RequestMapping("/register")
public class RegisterController {
    private final ServidorPrincipal servidorPrincipal;
    @Autowired
    public RegisterController(ServidorPrincipal servidorPrincipal) {
        this.servidorPrincipal = servidorPrincipal;
    }
    @PostMapping
    public ResponseEntity<Object> registrarUsuario(@RequestBody UserConfig userConfig) throws SQLException {

        String url = "jdbc:sqlite:" + "database.db";
        Connection dbConnection = DBConnection.connect(url);


        String res = registerUser(dbConnection,userConfig.getNome(),
                userConfig.getNumeroIdentificacao(),
                userConfig.getEmail(),
                userConfig.getPassword(),
                "Client");

        dbConnection.close();

        return ResponseEntity.ok(res);
    }
    private String registerUser(Connection conn, String user, long id, String email, String password, String role) throws SQLException {
        if (!emailExists(conn,email)) {
            String sql = "INSERT INTO utilizador (Username, IdentificationNumber, Email, Password, Role) VALUES (?, ?, ?, ?, ?);";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, user);
                pstmt.setLong(2, id);
                pstmt.setString(3, email);
                pstmt.setString(4, password);
                pstmt.setString(5, role);
                pstmt.executeUpdate();
            }
            servidorPrincipal.onDatabaseUpdate();
            return "Registado com sucesso";
        } else {
            return "Email already exists. Please choose a different one.";
        }
    }
    private static boolean emailExists(Connection conn,String email) throws SQLException {
        String sql = "SELECT COUNT(*) FROM utilizador WHERE Email = ?;";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            return pstmt.executeQuery().getInt(1) > 0;
        }
    }
}