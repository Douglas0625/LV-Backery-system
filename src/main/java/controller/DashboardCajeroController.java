package controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import utils.Sesion;

import java.io.IOException;

public class DashboardCajeroController {

    @FXML private Button btnDashboard;
    @FXML private Button btnPedidos;
    @FXML private Button btnVentas;

    @FXML private Label lblTituloModulo;
    @FXML private Label lblNombreUsuario;
    @FXML private StackPane contenedorPrincipal;

    private Button botonActivo;

    private static final String ESTILO_ACTIVO   = "-fx-background-color: #6B4F4F; -fx-background-radius: 8; -fx-padding: 10 15 10 15;";
    private static final String ESTILO_INACTIVO = "-fx-background-color: transparent; -fx-background-radius: 8; -fx-padding: 10 15 10 15;";

    @FXML
    public void initialize() {
        if (Sesion.getUsuarioLogueado() != null) {
            lblNombreUsuario.setText(Sesion.getUsuarioLogueado().getNombre());
        }
        cargarVista("/views/home_cajero.fxml", "Dashboard", btnDashboard);
    }

    @FXML
    public void mostrarDashboardHome(ActionEvent e) {
        cargarVista("/views/home_cajero.fxml", "Dashboard", btnDashboard);
    }

    @FXML
    public void mostrarPedidos(ActionEvent e) {
        cargarVista("/views/pedidos.fxml", "Pedidos", btnPedidos);
    }

    @FXML
    public void mostrarVentas(ActionEvent e) {
        cargarVista("/views/ventas.fxml", "Ventas", btnVentas);
    }

    @FXML
    public void cerrarSesion(ActionEvent e) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Deseas cerrar la sesión?", ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Cerrar sesión");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                Sesion.cerrarSesion();
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/login.fxml"));
                    javafx.scene.Parent root = loader.load();
                    Stage stage = new Stage();
                    stage.setTitle("LV Bakery System");
                    stage.setScene(new javafx.scene.Scene(root));
                    stage.setMaximized(true);
                    stage.show();
                    Stage actual = (Stage) contenedorPrincipal.getScene().getWindow();
                    actual.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    public void cargarVista(String fxmlPath, String titulo, Button boton) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node vista = loader.load();

            Object controller = loader.getController();
            if (controller instanceof HomeCajeroController) {
                ((HomeCajeroController) controller).setDashboard(this);
            }

            contenedorPrincipal.getChildren().setAll(vista);
            lblTituloModulo.setText(titulo);
            actualizarBotonActivo(boton);

        } catch (IOException ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR,
                    "No se pudo cargar: " + fxmlPath, ButtonType.OK).showAndWait();
        }
    }

    private void actualizarBotonActivo(Button nuevo) {
        if (botonActivo != null) botonActivo.setStyle(ESTILO_INACTIVO);
        if (nuevo != null)       nuevo.setStyle(ESTILO_ACTIVO);
        botonActivo = nuevo;
    }
}
