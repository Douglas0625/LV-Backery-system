package controller;

import dao.PedidoDAO;
import dao.VentaDAO;
import dao.IngredienteDAO;
import dao.MovimientoInventarioDAO;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import model.MovimientoInventario;
import model.Pedido;
import utils.Sesion;

import java.math.BigDecimal;
import java.util.List;

public class DashboardAdminController {

    // ── Métricas ──────────────────────────────────────────────────────────────
    @FXML private Label lblTotalVentasHoy;
    @FXML private Label lblCantidadVentasHoy;
    @FXML private Label lblPedidosPendientes;
    @FXML private Label lblStockCritico;
    @FXML private Label lblNombreUsuario;

    // ── Tabla últimos movimientos ─────────────────────────────────────────────
    @FXML private TableView<MovimientoInventario>           tablaMovimientos;
    @FXML private TableColumn<MovimientoInventario, String> colFechaMov;
    @FXML private TableColumn<MovimientoInventario, String> colIngredienteMov;
    @FXML private TableColumn<MovimientoInventario, String> colTipoMov;

    // ── Tabla pedidos recientes ───────────────────────────────────────────────
    @FXML private TableView<Pedido>               tablaPedidosRecientes;
    @FXML private TableColumn<Pedido, Integer>    colIdPedidoD;
    @FXML private TableColumn<Pedido, String>     colClienteD;
    @FXML private TableColumn<Pedido, String>     colEstadoD;
    @FXML private TableColumn<Pedido, BigDecimal> colTotalD;

    private final VentaDAO    ventaDAO    = new VentaDAO();
    private final PedidoDAO   pedidoDAO   = new PedidoDAO();
    private final IngredienteDAO ingredienteDAO = new IngredienteDAO();
    private final MovimientoInventarioDAO movDAO = new MovimientoInventarioDAO();

    @FXML
    public void initialize() {
        cargarMetricas();
        configurarTablas();
        cargarDatos();
    }

    private void cargarMetricas() {
        if (lblNombreUsuario != null && Sesion.getUsuarioLogueado() != null)
            lblNombreUsuario.setText(Sesion.getUsuarioLogueado().getNombre());

        if (lblTotalVentasHoy != null)
            lblTotalVentasHoy.setText("$" + ventaDAO.totalVentasHoy().setScale(2, java.math.RoundingMode.HALF_UP));

        if (lblCantidadVentasHoy != null)
            lblCantidadVentasHoy.setText(String.valueOf(ventaDAO.cantidadVentasHoy()));

        if (lblPedidosPendientes != null)
            lblPedidosPendientes.setText(String.valueOf(pedidoDAO.contarPendientes()));

        if (lblStockCritico != null) {
            long criticos = ingredienteDAO.listarTodos().stream()
                    .filter(i -> i.getStockActualGramos().compareTo(BigDecimal.valueOf(500)) < 0)
                    .count();
            lblStockCritico.setText(criticos + " ingredientes");
        }
    }

    private void configurarTablas() {
        if (tablaMovimientos != null) {
            colFechaMov.setCellValueFactory(d ->
                    new javafx.beans.property.SimpleStringProperty(
                            d.getValue().getFechaMovimiento() != null ? d.getValue().getFechaMovimiento().toString() : ""));
            colIngredienteMov.setCellValueFactory(d ->
                    new javafx.beans.property.SimpleStringProperty(d.getValue().getNombreIngrediente()));
            colTipoMov.setCellValueFactory(d ->
                    new javafx.beans.property.SimpleStringProperty(d.getValue().getTipoMovimiento()));
        }

        if (tablaPedidosRecientes != null) {
            colIdPedidoD.setCellValueFactory(new PropertyValueFactory<>("idPedido"));
            colClienteD.setCellValueFactory(d ->
                    new javafx.beans.property.SimpleStringProperty(d.getValue().getNombreCliente()));
            colEstadoD.setCellValueFactory(d ->
                    new javafx.beans.property.SimpleStringProperty(d.getValue().getNombreEstado()));
            colTotalD.setCellValueFactory(new PropertyValueFactory<>("totalPedido"));
        }
    }

    private void cargarDatos() {
        if (tablaMovimientos != null)
            tablaMovimientos.setItems(FXCollections.observableArrayList(movDAO.listarRecientes(10)));

        if (tablaPedidosRecientes != null) {
            List<Pedido> pendientes = pedidoDAO.listarPendientes();
            tablaPedidosRecientes.setItems(FXCollections.observableArrayList(
                    pendientes.subList(0, Math.min(10, pendientes.size()))));
        }
    }

    // ── Navegación ───────────────────────────────────────────────────────────

    @FXML public void irPedidos()    { abrirVentana("/views/pedidos.fxml",    "Pedidos");    }
    @FXML public void irVentas()     { abrirVentana("/views/ventas.fxml",     "Ventas");     }
    @FXML public void irRecetas()    { abrirVentana("/views/recetas.fxml",    "Recetas");    }
    @FXML public void irReportes()   { abrirVentana("/views/reportes.fxml",   "Reportes");   }
    @FXML public void irInventario() { abrirVentana("/views/inventario.fxml", "Inventario"); }
    @FXML public void irUsuarios()   { abrirVentana("/views/usuarios.fxml",   "Usuarios");   }

    @FXML
    public void cerrarSesion() {
        Sesion.cerrarSesion();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/login.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("LV Bakery System");
            stage.setScene(new Scene(root));
            stage.show();
            // Cerrar la ventana actual
            if (lblTotalVentasHoy != null)
                ((Stage) lblTotalVentasHoy.getScene().getWindow()).close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void abrirVentana(String fxml, String titulo) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("LV Bakery - " + titulo);
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "No se pudo abrir: " + titulo, ButtonType.OK)
            {{ setHeaderText(null); }}.showAndWait();
        }
    }
}
