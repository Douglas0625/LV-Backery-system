package application;

import config.DatabaseConnection;

import java.sql.Connection;

public class TestConnection {

    public static void main(String[] args) {

        Connection connection =
                DatabaseConnection.getConnection();

        if(connection != null){

            System.out.println("Conexion exitosa");

        }else{

            System.out.println("Error de conexion");
        }
    }
}