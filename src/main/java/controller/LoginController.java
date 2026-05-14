package controller;

import dao.UsuarioDAO;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import model.Usuario;
import utils.Sesion;

public class LoginController {

    @FXML
    private TextField txtUsuario;

    @FXML
    private PasswordField txtPassword;

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    @FXML
    public void iniciarSesion(ActionEvent event) {

        String usuario = txtUsuario.getText().trim();
        String password = txtPassword.getText().trim();

        if (usuario.isEmpty() || password.isEmpty()) {

            mostrarAlerta(
                    "Campos vacíos",
                    "Debe completar usuario y contraseña."
            );

            return;
        }

        Usuario usuarioLogueado =
                usuarioDAO.iniciarSesion(usuario, password);

        if (usuarioLogueado == null) {

            mostrarAlerta(
                    "Credenciales incorrectas",
                    "Usuario o contraseña inválidos."
            );

            return;
        }

        Sesion.setUsuarioLogueado(usuarioLogueado);

        abrirDashboard();

        cerrarVentana();
    }

    private void abrirDashboard() {

        try {

            String vistaDashboard;

            String rol =
                    Sesion.getUsuarioLogueado()
                            .getRol()
                            .getNombreRol();

            if (rol.equalsIgnoreCase("Administrador")) {

                vistaDashboard = "/views/dashboard_admin.fxml";

            } else {

                vistaDashboard = "/views/dashboard_cajero.fxml";
            }

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(vistaDashboard)
            );

            Parent root = loader.load();

            Stage stage = new Stage();

            stage.setTitle("LV Bakery System");

            stage.setScene(new Scene(root));

            stage.setMaximized(true);

            stage.show();

        } catch (Exception e) {

            e.printStackTrace();

            mostrarAlerta(
                    "Error",
                    "No se pudo abrir el dashboard."
            );
        }
    }

    private void cerrarVentana() {

        Stage stage = (Stage) txtUsuario
                .getScene()
                .getWindow();

        stage.close();
    }

    private void mostrarAlerta(String titulo, String mensaje) {

        Alert alert = new Alert(Alert.AlertType.ERROR);

        alert.setTitle(titulo);

        alert.setHeaderText(null);

        alert.setContentText(mensaje);

        alert.showAndWait();
    }
}