package pt.isec.pd.springboot.security;


import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import pt.isec.pd.springboot.m1.database.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Component
public class UserAuthenticationProvider implements AuthenticationProvider
{

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException
    {
        String username = authentication.getName();
        String password = authentication.getCredentials().toString();

//        System.out.println(username);
//        System.out.println(password);

        String role = null;
        String url = "jdbc:sqlite:" + "database.db";
        Connection dbConnection = DBConnection.connect(url);
        String sql = "SELECT Role FROM utilizador WHERE Email = ? AND Password = ?;";


        try (PreparedStatement pstmt = dbConnection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            ResultSet resultSet = pstmt.executeQuery();

            // Check if the query returned any rows
            if (resultSet.next()) {
                role = resultSet.getString("Role"); // Return the role
            }
        } catch (SQLException e) {
            System.err.println("Erro ao verificar usuário: " + e.getMessage());
        } finally {
            try {
                if (dbConnection != null) {
                    dbConnection.close();
                }
            } catch (SQLException ex) {
                System.err.println("Erro ao fechar a conexão com o banco de dados: " + ex.getMessage());
            }
        }

        if(role == null){
            return null;
        }

        if(role.equals("Admin")){
            List<GrantedAuthority> authorities = new ArrayList<>();
            authorities.add(new SimpleGrantedAuthority("ADMIN"));

            return new UsernamePasswordAuthenticationToken(username, password, authorities);
        }else if(role.equals("Client")){
            List<GrantedAuthority> authorities = new ArrayList<>();
            authorities.add(new SimpleGrantedAuthority("CLIENT"));

            return new UsernamePasswordAuthenticationToken(username, password, authorities);
        }

        return null;
    }

    @Override
    public boolean supports(Class<?> authentication)
    {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }
}
