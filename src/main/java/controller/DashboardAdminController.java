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

public class DashboardAdminController {

    // Sidebar buttons
    @FXML private Button btnDashboard;
    @FXML private Button btnPedidos;
    @FXML private Button btnVentas;
    @FXML private Button btnRecetas;
    @FXML private Button btnReportes;
    @FXML private Button btnInventario;
    @FXML private Button btnUsuarios;
    @FXML private Button btnClientes;

    // Navbar
    @FXML private Label lblTituloModulo;
    @FXML private Label lblNombreUsuario;

    // Contenedor dinámico
    @FXML private StackPane contenedorPrincipal;

    // Botón activo actual
    private Button botonActivo;

    // Estilos sidebar
    private static final String ESTILO_ACTIVO      = "-fx-background-color: #6B4F4F; -fx-background-radius: 8; -fx-padding: 10 15 10 15;";
    private static final String ESTILO_INACTIVO    = "-fx-background-color: transparent; -fx-background-radius: 8; -fx-padding: 10 15 10 15;";

    @FXML
    public void initialize() {
        // Mostrar nombre del usuario logueado
        if (Sesion.getUsuarioLogueado() != null) {
            lblNombreUsuario.setText(Sesion.getUsuarioLogueado().getNombre());
        }

        // Cargar home por defecto
        cargarVista("/views/home_admin.fxml", "Dashboard", btnDashboard);
    }

    // ===== ACCIONES DEL SIDEBAR =====

    @FXML
    public void mostrarDashboardHome(ActionEvent e) {
        cargarVista("/views/home_admin.fxml", "Dashboard", btnDashboard);
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
    public void mostrarRecetas(ActionEvent e) {
        cargarVista("/views/recetas.fxml", "Recetas", btnRecetas);
    }

    @FXML
    public void mostrarReportes(ActionEvent e) {
        cargarVista("/views/reportes.fxml", "Reportes", btnReportes);
    }

    @FXML
    public void mostrarInventario(ActionEvent e) {
        cargarVista("/views/inventario.fxml", "Inventario", btnInventario);
    }

    @FXML
    public void mostrarUsuarios(ActionEvent e) {
        cargarVista("/views/usuarios.fxml", "Usuarios", btnUsuarios);
    }

    @FXML
    public void mostrarClientes(ActionEvent e) {
        cargarVista("/views/clientes.fxml", "Clientes", btnClientes);
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
                    javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                            getClass().getResource("/views/login.fxml"));
                    javafx.scene.Parent root = loader.load();
                    Stage stage = new Stage();
                    stage.setTitle("LV Bakery System");
                    stage.setScene(new javafx.scene.Scene(root));
                    stage.setMaximized(true);
                    stage.show();
                    // Cerrar ventana actual
                    Stage actual = (Stage) contenedorPrincipal.getScene().getWindow();
                    actual.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    // ===== METODO CENTRAL DE NAVEGACIÓN =====

    /**
     * Carga un FXML dentro del contenedorPrincipal.
     * Actualiza título navbar y estado visual del botón.
     */
    public void cargarVista(String fxmlPath, String titulo, Button boton) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node vista = loader.load();

            // Inyectar referencia al dashboard en controllers que lo necesiten
            Object controller = loader.getController();
            if (controller instanceof HomeAdminController) {
                ((HomeAdminController) controller).setDashboard(this);
            }

            // Reemplazar contenido
            contenedorPrincipal.getChildren().setAll(vista);

            // Actualizar navbar
            lblTituloModulo.setText(titulo);

            // Actualizar estado visual del sidebar
            actualizarBotonActivo(boton);

        } catch (IOException ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR,
                    "No se pudo cargar: " + fxmlPath, ButtonType.OK)
                    .showAndWait();
        }
    }

    private void actualizarBotonActivo(Button nuevo) {
        // Desactivar botón anterior
        if (botonActivo != null) {
            botonActivo.setStyle(ESTILO_INACTIVO);
        }
        // Activar nuevo
        if (nuevo != null) {
            nuevo.setStyle(ESTILO_ACTIVO);
        }
        botonActivo = nuevo;
    }
}
