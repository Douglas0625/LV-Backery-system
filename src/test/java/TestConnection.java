import database.DatabaseConnection;

import java.sql.Connection;
import java.sql.SQLException;

public class TestConnection {

    public static void main(String[] args) {
        try {
            Connection connection = DatabaseConnection.getConnection();

            if (connection != null) {
                System.out.println("Conexión exitosa a PostgreSQL");
            }

            connection.close();

        } catch (SQLException e) {
            System.out.println("Error al conectar con PostgreSQL");
            e.printStackTrace();
        }
    }
}