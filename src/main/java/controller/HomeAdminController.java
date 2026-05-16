package controller;

import dao.IngredienteDAO;
import dao.MovimientoInventarioDAO;
import dao.PedidoDAO;
import dao.VentaDAO;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
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
import java.util.List;

public class HomeAdminController {

    @FXML private Label lblBienvenida;
    @FXML private Label lblPedidosPendientes;
    @FXML private Label lblVentasDia;
    @FXML private Label lblCantVentas;
    @FXML private Label lblStockBajo;
    @FXML private Label lblProductoTop;
    @FXML private Label lblProductoTopVentas;

    @FXML private TableView<Pedido>    tablaUltimosPedidos;
    @FXML private TableColumn<Pedido, Integer> colPedidoId;
    @FXML private TableColumn<Pedido, String>  colPedidoCliente;
    @FXML private TableColumn<Pedido, String>  colPedidoEstado;
    @FXML private TableColumn<Pedido, BigDecimal> colPedidoTotal;

    @FXML private VBox contenedorMovimientos;

    private DashboardAdminController dashboard;

    private final PedidoDAO   pedidoDAO   = new PedidoDAO();
    private final VentaDAO    ventaDAO    = new VentaDAO();
    private final IngredienteDAO ingDAO   = new IngredienteDAO();
    private final MovimientoInventarioDAO movDAO = new MovimientoInventarioDAO();

    public void setDashboard(DashboardAdminController dashboard) {
        this.dashboard = dashboard;
    }

    @FXML
    public void initialize() {
        if (Sesion.getUsuarioLogueado() != null) {
            lblBienvenida.setText("Bienvenido, " + Sesion.getUsuarioLogueado().getNombre());
        }
        configurarTablaPedidos();
        cargarMetricas();
        cargarMovimientos();
    }

    private void configurarTablaPedidos() {
        colPedidoId.setCellValueFactory(new PropertyValueFactory<>("idPedido"));
        colPedidoCliente.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(d.getValue().getNombreCliente()));
        colPedidoEstado.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(d.getValue().getNombreEstado()));
        colPedidoTotal.setCellValueFactory(new PropertyValueFactory<>("totalPedido"));
    }

    private void cargarMetricas() {
        // Pedidos pendientes
        long pendientes = pedidoDAO.contarPendientes();
        lblPedidosPendientes.setText(String.valueOf(pendientes));

        // Ventas del día
        BigDecimal totalVentas = ventaDAO.totalVentasHoy();
        long cantVentas = ventaDAO.cantidadVentasHoy();
        lblVentasDia.setText("$" + totalVentas.setScale(2, java.math.RoundingMode.HALF_UP));
        lblCantVentas.setText(cantVentas + " ventas");

        // Stock bajo (ingredientes con menos de 500g)
        long stockBajo = ingDAO.listarTodos().stream()
                .filter(i -> i.getStockActualGramos().compareTo(BigDecimal.valueOf(500)) < 0)
                .count();
        lblStockBajo.setText(String.valueOf(stockBajo));

        // Últimos pedidos pendientes en tabla
        List<Pedido> pedidos = pedidoDAO.listarPendientes();
        tablaUltimosPedidos.getItems().setAll(pedidos.subList(0, Math.min(10, pedidos.size())));

        // Producto top (placeholder — requeriría query adicional)
        lblProductoTop.setText("—");
        lblProductoTopVentas.setText("Consulta en Reportes");
    }

    private void cargarMovimientos() {
        contenedorMovimientos.getChildren().clear();
        List<MovimientoInventario> movimientos = movDAO.listarRecientes(5);
        for (MovimientoInventario m : movimientos) {
            HBox fila = crearFilaMovimiento(m);
            contenedorMovimientos.getChildren().add(fila);
        }
        if (movimientos.isEmpty()) {
            Label empty = new Label("Sin movimientos recientes");
            empty.setStyle("-fx-text-fill: #7A6F68;");
            contenedorMovimientos.getChildren().add(empty);
        }
    }

    private HBox crearFilaMovimiento(MovimientoInventario m) {
        HBox fila = new HBox(10);
        fila.setStyle("-fx-background-color: #F5F3EF; -fx-background-radius: 8; -fx-padding: 10;");
        fila.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

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

    // Botones de acceso rápido en el home
    @FXML
    public void irPedidos(ActionEvent e) {
        if (dashboard != null) dashboard.mostrarPedidos(e);
    }

    @FXML
    public void irVentas(ActionEvent e) {
        if (dashboard != null) dashboard.mostrarVentas(e);
    }
}
