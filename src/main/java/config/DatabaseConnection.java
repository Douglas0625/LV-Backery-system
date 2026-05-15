package config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static final String URL =
            EnvConfig.BD_URL;

    private static final String USER =
            EnvConfig.BD_USER;

    private static final String PASSWORD =
            EnvConfig.DB_PASSWORD;

    public static Connection getConnection() {

        try {

            return DriverManager.getConnection(
                    URL,
                    USER,
                    PASSWORD
            );

        } catch (SQLException e) {

            e.printStackTrace();

            return null;
        }
    }
}