package controller;

import dao.PedidoDAO;
import dao.VentaDAO;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import model.Pedido;
import utils.Sesion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class HomeCajeroController {

    // ── Labels ────────────────────────────────────────────────────────────────
    @FXML private Label lblBienvenida;

    /** Cantidad de ventas cerradas hoy. */
    @FXML private Label lblPedidosHoy;

    /** Total en dinero vendido hoy ($). */
    @FXML private Label lblVentasHoy;

    /** Pedidos aún pendientes (sin despachar). */
    @FXML private Label lblPendientes;

    // ── Tabla ─────────────────────────────────────────────────────────────────
    @FXML private TableView<Pedido>               tablaUltimosPedidos;
    @FXML private TableColumn<Pedido, Integer>    colId;
    @FXML private TableColumn<Pedido, String>     colCliente;
    @FXML private TableColumn<Pedido, String>     colEntrega;
    @FXML private TableColumn<Pedido, String>     colEstado;
    @FXML private TableColumn<Pedido, BigDecimal> colTotal;

    // ── Referencia al shell de navegación ────────────────────────────────────
    private DashboardCajeroController dashboard;

    // ── DAOs ──────────────────────────────────────────────────────────────────
    private final PedidoDAO pedidoDAO = new PedidoDAO();
    private final VentaDAO  ventaDAO  = new VentaDAO();

    // ── Setter inyectado por DashboardCajeroController ────────────────────────
    public void setDashboard(DashboardCajeroController dashboard) {
        this.dashboard = dashboard;
    }

    // ── Inicialización ────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        try {
            if (Sesion.getUsuarioLogueado() != null) {
                lblBienvenida.setText("Bienvenido, " + Sesion.getUsuarioLogueado().getNombre());
            }
            configurarTabla();
            cargarDatos();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Columnas de la tabla ──────────────────────────────────────────────────
    private void configurarTabla() {
        colId.setCellValueFactory(new PropertyValueFactory<>("idPedido"));
        colCliente.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(d.getValue().getNombreCliente()));
        colEntrega.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(
                        d.getValue().getFechaEntrega() != null
                                ? d.getValue().getFechaEntrega().toString()
                                : "—"));
        colEstado.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(d.getValue().getNombreEstado()));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("totalPedido"));
    }

    // ── Carga de métricas del día ─────────────────────────────────────────────
    private void cargarDatos() {
        // --- Ventas del día: cantidad de ventas cerradas hoy ---
        try {
            long cantVentasHoy = ventaDAO.cantidadVentasHoy();
            lblPedidosHoy.setText(String.valueOf(cantVentasHoy));
        } catch (Exception e) {
            e.printStackTrace();
            lblPedidosHoy.setText("0");
        }

        // --- Total vendido hoy en dinero ---
        try {
            BigDecimal totalHoy = ventaDAO.totalVentasHoy();
            lblVentasHoy.setText("$" + totalHoy.setScale(2, RoundingMode.HALF_UP));
        } catch (Exception e) {
            e.printStackTrace();
            lblVentasHoy.setText("$0.00");
        }

        // --- Pedidos pendientes (sin despachar) ---
        try {
            long pendientes = pedidoDAO.contarPendientes();
            lblPendientes.setText(String.valueOf(pendientes));
        } catch (Exception e) {
            e.printStackTrace();
            lblPendientes.setText("0");
        }

        // --- Tabla: últimos 15 pedidos pendientes ---
        try {
            List<Pedido> lista = pedidoDAO.listarPendientes();
            int limite = Math.min(15, lista.size());
            tablaUltimosPedidos.getItems().setAll(lista.subList(0, limite));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Accesos rápidos ───────────────────────────────────────────────────────
    @FXML
    public void irPedidos(ActionEvent e) {
        if (dashboard != null) dashboard.mostrarPedidos(e);
    }

    @FXML
    public void irVentas(ActionEvent e) {
        if (dashboard != null) dashboard.mostrarVentas(e);
    }
}
