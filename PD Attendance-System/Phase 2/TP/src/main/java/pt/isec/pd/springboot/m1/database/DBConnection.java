package pt.isec.pd.springboot.m1.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection{

    public static Connection connect(String dbPath) {
        Connection  conn = null;
        try {
            // Construct the full URL for the SQLite database
            // Establish the connection to the database
            conn = DriverManager.getConnection(dbPath);
            //System.out.println("Connection to SQLite has been established.");
        } catch (SQLException e) {
            // It's generally a good idea to provide more context in error messages.
            System.out.println("Connection to SQLite has failed: " + e.getMessage());
        }
        return conn;
    }
}