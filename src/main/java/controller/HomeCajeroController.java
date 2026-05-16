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
import java.util.List;

public class HomeCajeroController {

    @FXML private Label lblBienvenida;
    @FXML private Label lblPedidosHoy;
    @FXML private Label lblVentasHoy;
    @FXML private Label lblPendientes;

    @FXML private TableView<Pedido>               tablaUltimosPedidos;
    @FXML private TableColumn<Pedido, Integer>     colId;
    @FXML private TableColumn<Pedido, String>      colCliente;
    @FXML private TableColumn<Pedido, String>      colEntrega;
    @FXML private TableColumn<Pedido, String>      colEstado;
    @FXML private TableColumn<Pedido, BigDecimal>  colTotal;

    private DashboardCajeroController dashboard;

    private final PedidoDAO pedidoDAO = new PedidoDAO();
    private final VentaDAO  ventaDAO  = new VentaDAO();

    public void setDashboard(DashboardCajeroController dashboard) {
        this.dashboard = dashboard;
    }

    @FXML
    public void initialize() {
        if (Sesion.getUsuarioLogueado() != null) {
            lblBienvenida.setText("Bienvenido, " + Sesion.getUsuarioLogueado().getNombre());
        }
        configurarTabla();
        cargarDatos();
    }

    private void configurarTabla() {
        colId.setCellValueFactory(new PropertyValueFactory<>("idPedido"));
        colCliente.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(d.getValue().getNombreCliente()));
        colEntrega.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(
                        d.getValue().getFechaEntrega() != null ? d.getValue().getFechaEntrega().toString() : ""));
        colEstado.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(d.getValue().getNombreEstado()));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("totalPedido"));
    }

    private void cargarDatos() {
        long pendientes = pedidoDAO.contarPendientes();
        lblPendientes.setText(String.valueOf(pendientes));
        lblPedidosHoy.setText(String.valueOf(pendientes)); // aproximado

        BigDecimal ventas = ventaDAO.totalVentasHoy();
        lblVentasHoy.setText("$" + ventas.setScale(2, java.math.RoundingMode.HALF_UP));

        List<Pedido> lista = pedidoDAO.listarPendientes();
        tablaUltimosPedidos.getItems().setAll(lista.subList(0, Math.min(15, lista.size())));
    }

    @FXML
    public void irPedidos(ActionEvent e) {
        if (dashboard != null) dashboard.mostrarPedidos(e);
    }

    @FXML
    public void irVentas(ActionEvent e) {
        if (dashboard != null) dashboard.mostrarVentas(e);
    }
}
