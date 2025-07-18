package pt.isec.pd.springboot.m1.database;

import java.sql.*;

public class SQLiteDBSetup {

    public static void setup(String fileName) throws ClassNotFoundException {
        Class.forName("org.sqlite.JDBC");
        createNewDatabase(fileName);
        createTables(fileName);
    }

    public static void createNewDatabase(String fileName) {
        String url = "jdbc:sqlite:" + fileName;
        try (Connection conn = DriverManager.getConnection(url)) {
            if (conn != null) {
                DatabaseMetaData meta = conn.getMetaData();
                System.out.println("The driver name is " + meta.getDriverName());
                System.out.println("A new database has been created.");
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void createTables(String fileName) {
        // SQL statement for creating the tables
        String sqlUsers = "CREATE TABLE IF NOT EXISTS utilizador  (" +
                "UserID INTEGER PRIMARY KEY AUTOINCREMENT," +
                "Username TEXT," +
                "IdentificationNumber TEXT," +
                "Email TEXT UNIQUE," +
                "Password TEXT," +
                "Role TEXT CHECK(Role IN ('Admin', 'Client'))" +
                ");";

        // SQL statement for creating the Events table
        String sqlEvents = "CREATE TABLE IF NOT EXISTS evento(" +
                "EventID INTEGER PRIMARY KEY AUTOINCREMENT," +
                "Designation TEXT NOT NULL UNIQUE," +
                "Local TEXT," +
                "Date TEXT," +
                "BeginningHour TEXT," +
                "EndHour TEXT" +
                ");";

        String sqlRegistrationCodes = "CREATE TABLE IF NOT EXISTS codigo_registo (" +
                "RegistrationCodeID INTEGER PRIMARY KEY AUTOINCREMENT," +
                "Code TEXT NOT NULL UNIQUE," +
                "EventID INTEGER NOT NULL," +
                "ValidityDuration INTEGER NOT NULL," +
                "Timestamp DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "FOREIGN KEY (EventID) REFERENCES evento(EventID)" +
                ");";

        // SQL statement for creating the Presences table
        String sqlAttendance = "CREATE TABLE IF NOT EXISTS assiste (" +
                "AttendanceID INTEGER PRIMARY KEY AUTOINCREMENT," +
                "UserID INTEGER NOT NULL," +
                "EventID INTEGER NOT NULL," +
                "Timestamp TEXT DEFAULT CURRENT_TIMESTAMP," +
                "FOREIGN KEY (UserID) REFERENCES utilizador(UserID)," +
                "FOREIGN KEY (EventID) REFERENCES evento(EventID)" +
                ");";
        String sqlVersion = "CREATE TABLE IF NOT EXISTS versao (" +
                "VersionID INTEGER PRIMARY KEY AUTOINCREMENT," +
                "VersionNumber INTEGER NOT NULL UNIQUE" +
                ");";

        String sqlAdmin = "INSERT INTO utilizador (Username, Email, Password, Role) VALUES ('admin', 'admin@gmail.com', 'admin', 'Admin');";

        String sqlInsertVersion = "INSERT INTO versao (VersionNumber) SELECT 0 WHERE NOT EXISTS (SELECT 1 FROM versao WHERE VersionID = 1);";

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + fileName);
             Statement stmt = conn.createStatement()) {
            // Create tables
            stmt.execute(sqlUsers);
            stmt.execute(sqlEvents);
            stmt.execute(sqlRegistrationCodes);
            stmt.execute(sqlAttendance);
            stmt.execute(sqlVersion);
            stmt.executeUpdate(sqlAdmin);
            stmt.executeUpdate(sqlInsertVersion);

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
}
