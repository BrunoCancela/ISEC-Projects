package pt.isec.pd.springboot.m1.server;

import java.sql.SQLException;

public interface DatabaseVersionUpdateListener {
    void onDatabaseUpdate() throws SQLException;
}