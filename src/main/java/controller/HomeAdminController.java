package controller;

import dao.IngredienteDAO;
import dao.MovimientoInventarioDAO;
import dao.PedidoDAO;
import dao.ProductoDAO;
import dao.UsuarioDAO;
import dao.VentaDAO;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import model.Ingrediente;
import model.MovimientoInventario;
import model.Pedido;
import utils.Sesion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class HomeAdminController {

    // ── Labels de bienvenida y métricas ──────────────────────────────────────
    @FXML private Label lblBienvenida;
    @FXML private Label lblPedidosPendientes;
    @FXML private Label lblVentasDia;
    @FXML private Label lblCantVentas;
    @FXML private Label lblStockBajo;
    @FXML private Label lblProductoTop;
    @FXML private Label lblProductoTopVentas;

    // ── Tabla últimos pedidos ─────────────────────────────────────────────────
    @FXML private TableView<Pedido>               tablaUltimosPedidos;
    @FXML private TableColumn<Pedido, Integer>    colPedidoId;
    @FXML private TableColumn<Pedido, String>     colPedidoCliente;
    @FXML private TableColumn<Pedido, String>     colPedidoEstado;
    @FXML private TableColumn<Pedido, BigDecimal> colPedidoTotal;

    // ── Contenedor de movimientos ─────────────────────────────────────────────
    @FXML private VBox contenedorMovimientos;

    // ── Referencia al shell de navegación ────────────────────────────────────
    private DashboardAdminController dashboard;

    // ── DAOs ──────────────────────────────────────────────────────────────────
    private final PedidoDAO              pedidoDAO = new PedidoDAO();
    private final VentaDAO               ventaDAO  = new VentaDAO();
    private final ProductoDAO            productoDAO = new ProductoDAO();
    private final UsuarioDAO             usuarioDAO  = new UsuarioDAO();
    private final IngredienteDAO         ingDAO    = new IngredienteDAO();
    private final MovimientoInventarioDAO movDAO   = new MovimientoInventarioDAO();

    // ── Setter inyectado por DashboardAdminController ─────────────────────────
    public void setDashboard(DashboardAdminController dashboard) {
        this.dashboard = dashboard;
    }

    // ── Inicialización ────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        try {
            if (Sesion.getUsuarioLogueado() != null) {
                lblBienvenida.setText("Bienvenido, " + Sesion.getUsuarioLogueado().getNombre());
            }
            configurarTablaPedidos();
            cargarMetricas();
            cargarMovimientos();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Configuración de columnas de la tabla ─────────────────────────────────
    private void configurarTablaPedidos() {
        colPedidoId.setCellValueFactory(new PropertyValueFactory<>("idPedido"));
        colPedidoCliente.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(d.getValue().getNombreCliente()));
        colPedidoEstado.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(d.getValue().getNombreEstado()));
        colPedidoTotal.setCellValueFactory(new PropertyValueFactory<>("totalPedido"));
    }

    // ── Carga de métricas desde DAOs ─────────────────────────────────────────
    private void cargarMetricas() {
        // --- Pedidos pendientes ---
        try {
            long pendientes = pedidoDAO.contarPendientes();
            lblPedidosPendientes.setText(String.valueOf(pendientes));
        } catch (Exception e) {
            e.printStackTrace();
            lblPedidosPendientes.setText("—");
        }

        // --- Ventas del día (total $ y cantidad) ---
        try {
            BigDecimal totalHoy = ventaDAO.totalVentasHoy();
            long cantHoy = ventaDAO.cantidadVentasHoy();
            lblVentasDia.setText("$" + totalHoy.setScale(2, RoundingMode.HALF_UP));
            lblCantVentas.setText(cantHoy + " venta" + (cantHoy != 1 ? "s" : ""));
        } catch (Exception e) {
            e.printStackTrace();
            lblVentasDia.setText("$0.00");
            lblCantVentas.setText("0 ventas");
        }

        // --- Stock bajo (ingredientes < 500 g) ---
        try {
            long stockBajo = ingDAO.listarTodos().stream()
                    .filter(i -> i.getStockActualGramos().compareTo(BigDecimal.valueOf(500)) < 0)
                    .count();
            lblStockBajo.setText(String.valueOf(stockBajo));
        } catch (Exception e) {
            e.printStackTrace();
            lblStockBajo.setText("—");
        }

        // --- Producto más vendido ---
        try {
            List<String[]> tops = ventaDAO.productosMasVendidos(1);
            if (!tops.isEmpty()) {
                lblProductoTop.setText(tops.get(0)[0]);
                lblProductoTopVentas.setText(tops.get(0)[1] + " unidades vendidas");
            } else {
                lblProductoTop.setText("Sin ventas aún");
                lblProductoTopVentas.setText("");
            }
        } catch (Exception e) {
            e.printStackTrace();
            lblProductoTop.setText("—");
            lblProductoTopVentas.setText("");
        }

        // --- Tabla: últimos 10 pedidos pendientes ---
        try {
            List<Pedido> pedidos = pedidoDAO.listarPendientes();
            int limite = Math.min(10, pedidos.size());
            tablaUltimosPedidos.getItems().setAll(pedidos.subList(0, limite));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Carga de movimientos recientes ────────────────────────────────────────
    private void cargarMovimientos() {
        try {
            contenedorMovimientos.getChildren().clear();
            List<MovimientoInventario> movimientos = movDAO.listarRecientes(5);
            for (MovimientoInventario m : movimientos) {
                contenedorMovimientos.getChildren().add(crearFilaMovimiento(m));
            }
            if (movimientos.isEmpty()) {
                Label empty = new Label("Sin movimientos recientes");
                empty.setStyle("-fx-text-fill: #7A6F68;");
                contenedorMovimientos.getChildren().add(empty);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Fila visual para cada movimiento ──────────────────────────────────────
    private HBox crearFilaMovimiento(MovimientoInventario m) {
        HBox fila = new HBox(10);
        fila.setStyle("-fx-background-color: #F5F3EF; -fx-background-radius: 8; -fx-padding: 10;");
        fila.setAlignment(Pos.CENTER_LEFT);

        Region barra = new Region();
        barra.setPrefWidth(4);
        barra.setPrefHeight(40);
        barra.setStyle("-fx-background-color: #8B5E3C; -fx-background-radius: 10;");

        VBox info = new VBox(2);
        Label nombre = new Label(m.getNombreIngrediente());
        nombre.setStyle("-fx-font-family: 'Segoe UI Bold'; -fx-font-size: 13; -fx-text-fill: #4B3832;");
        Label detalle = new Label(m.getTipoMovimiento() + " · " + m.getCantidadGramos() + "g");
        detalle.setStyle("-fx-font-size: 11; -fx-text-fill: #7A6F68;");
        info.getChildren().addAll(nombre, detalle);

        fila.getChildren().addAll(barra, info);
        return fila;
    }

    // ── Accesos rápidos desde botones del home ────────────────────────────────
    @FXML
    public void irPedidos(ActionEvent e) {
        if (dashboard != null) dashboard.mostrarPedidos(e);
    }

    @FXML
    public void irVentas(ActionEvent e) {
        if (dashboard != null) dashboard.mostrarVentas(e);
    }
}
