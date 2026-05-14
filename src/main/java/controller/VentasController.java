package controller;

import dao.ClienteDAO;
import dao.ProductoDAO;
import dao.RecetaDAO;
import dao.VentaDAO;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import model.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public class VentasController {

    // Panel izquierdo: historial
    @FXML private ComboBox<String> cbFiltroTipo;

    // Panel central: formulario nueva venta
    @FXML private ComboBox<String>   cbTipoVenta;
    @FXML private ComboBox<Cliente>  cbCliente;
    @FXML private ComboBox<String>   cbMetodoPago;
    @FXML private ComboBox<String>   cbTipoComprobante;
    @FXML private TextField          txtNumeroComprobante;
    @FXML private Label              lblTotalVenta;

    // Tabla detalle venta (formulario)
    @FXML private TableView<DetalleVenta>               tablaDetalleVenta;
    @FXML private TableColumn<DetalleVenta, String>     colProductoVenta;
    @FXML private TableColumn<DetalleVenta, Integer>    colCantidadVenta;
    @FXML private TableColumn<DetalleVenta, BigDecimal> colPrecioVenta;
    @FXML private TableColumn<DetalleVenta, BigDecimal> colSubtotalVenta;

    // Tabla historial ventas
    @FXML private TableView<Venta>               tablaVentas;
    @FXML private TableColumn<Venta, Integer>    colIdVenta;
    @FXML private TableColumn<Venta, String>     colClienteVenta;
    @FXML private TableColumn<Venta, String>     colFechaVenta;
    @FXML private TableColumn<Venta, String>     colTipoVenta2;
    @FXML private TableColumn<Venta, BigDecimal> colTotalVenta2;

    private final VentaDAO    ventaDAO    = new VentaDAO();
    private final ClienteDAO  clienteDAO  = new ClienteDAO();
    private final ProductoDAO productoDAO = new ProductoDAO();
    private final RecetaDAO   recetaDAO   = new RecetaDAO();

    private final ObservableList<DetalleVenta> detalleActual = FXCollections.observableArrayList();
    private final ObservableList<Venta>        historial     = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        configurarTablaDetalle();
        configurarTablaHistorial();
        cargarCombos();
        cargarHistorial();
        if (lblTotalVenta != null) lblTotalVenta.setText("$0.00");
    }

    private void configurarTablaDetalle() {
        if (tablaDetalleVenta == null) return;
        colProductoVenta.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNombreProducto()));
        colCantidadVenta.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        colPrecioVenta.setCellValueFactory(new PropertyValueFactory<>("precioUnitario"));
        colSubtotalVenta.setCellValueFactory(new PropertyValueFactory<>("subtotal"));
        tablaDetalleVenta.setItems(detalleActual);
    }

    private void configurarTablaHistorial() {
        if (tablaVentas == null) return;
        if (colIdVenta     != null) colIdVenta.setCellValueFactory(new PropertyValueFactory<>("idVenta"));
        if (colClienteVenta!= null) colClienteVenta.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNombreCliente()));
        if (colFechaVenta  != null) colFechaVenta.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getFechaVenta() != null ? d.getValue().getFechaVenta().toString() : ""));
        if (colTipoVenta2  != null) colTipoVenta2.setCellValueFactory(new PropertyValueFactory<>("tipoVenta"));
        if (colTotalVenta2 != null) colTotalVenta2.setCellValueFactory(new PropertyValueFactory<>("totalVenta"));
        tablaVentas.setItems(historial);
    }

    private void cargarCombos() {
        if (cbTipoVenta != null)
            cbTipoVenta.setItems(FXCollections.observableArrayList("DIRECTA", "PEDIDO"));
        if (cbCliente != null)
            cbCliente.setItems(FXCollections.observableArrayList(clienteDAO.listarTodos()));
        if (cbMetodoPago != null)
            cbMetodoPago.setItems(FXCollections.observableArrayList("Efectivo", "Tarjeta", "Transferencia", "Otro"));
        if (cbTipoComprobante != null)
            cbTipoComprobante.setItems(FXCollections.observableArrayList("Factura", "Ticket", "Ninguno"));
    }

    private void cargarHistorial() {
        historial.setAll(ventaDAO.listarTodas());
    }

    @FXML
    public void agregarProductoVenta() {
        Dialog<DetalleVenta> dialog = new Dialog<>();
        dialog.setTitle("Agregar Producto");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        ComboBox<Producto> cbProd = new ComboBox<>(FXCollections.observableArrayList(productoDAO.listarTodos()));
        cbProd.setPromptText("Seleccionar producto");
        TextField txtCant = new TextField("1"); txtCant.setPromptText("Cantidad");

        javafx.scene.layout.VBox box = new javafx.scene.layout.VBox(10,
                new Label("Producto:"), cbProd, new Label("Cantidad:"), txtCant);
        box.setPadding(new javafx.geometry.Insets(10));
        dialog.getDialogPane().setContent(box);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK && cbProd.getValue() != null) {
                Producto p = cbProd.getValue();
                int cant;
                try { cant = Integer.parseInt(txtCant.getText().trim()); } catch (Exception e) { cant = 1; }
                DetalleVenta dv = new DetalleVenta();
                dv.setProducto(p);
                dv.setCantidad(cant);
                dv.setPrecioUnitario(p.getPrecioVenta());
                dv.calcularSubtotal();
                return dv;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(dv -> {
            detalleActual.add(dv);
            recalcularTotal();
        });
    }

    @FXML
    public void registrarVenta() {
        String tipo  = cbTipoVenta  != null ? cbTipoVenta.getValue()  : null;
        String metodo = cbMetodoPago != null ? cbMetodoPago.getValue() : null;

        if (tipo == null || metodo == null || detalleActual.isEmpty()) {
            alerta("Incompleto", "Selecciona tipo de venta, método de pago y agrega al menos un producto.");
            return;
        }

        Venta venta = new Venta();
        venta.setTipoVenta(tipo);
        venta.setMetodoPago(metodo);
        venta.setFechaVenta(LocalDate.now());
        venta.setCliente(cbCliente != null ? cbCliente.getValue() : null);
        venta.setNumeroComprobante(txtNumeroComprobante != null ? txtNumeroComprobante.getText().trim() : null);
        venta.setDetalles(new java.util.ArrayList<>(detalleActual));
        venta.setTotalVenta(detalleActual.stream().map(DetalleVenta::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        int id = ventaDAO.guardarVenta(venta, recetaDAO);
        if (id > 0) {
            info("Éxito", "Venta #" + id + " registrada. Inventario actualizado automáticamente.");
            limpiarFormulario();
            cargarHistorial();
        } else {
            alerta("Error", "No se pudo registrar la venta.");
        }
    }

    @FXML
    public void eliminarItemVenta() {
        DetalleVenta sel = tablaDetalleVenta != null ? tablaDetalleVenta.getSelectionModel().getSelectedItem() : null;
        if (sel != null) {
            detalleActual.remove(sel);
            recalcularTotal();
        }
    }

    private void recalcularTotal() {
        BigDecimal total = detalleActual.stream().map(DetalleVenta::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (lblTotalVenta != null) lblTotalVenta.setText("$" + total.setScale(2, java.math.RoundingMode.HALF_UP));
    }

    private void limpiarFormulario() {
        detalleActual.clear();
        if (cbTipoVenta    != null) cbTipoVenta.setValue(null);
        if (cbCliente      != null) cbCliente.setValue(null);
        if (cbMetodoPago   != null) cbMetodoPago.setValue(null);
        if (lblTotalVenta  != null) lblTotalVenta.setText("$0.00");
        if (txtNumeroComprobante != null) txtNumeroComprobante.clear();
    }

    private void alerta(String t, String m) {
        new Alert(Alert.AlertType.ERROR, m, ButtonType.OK) {{ setTitle(t); setHeaderText(null); }}.showAndWait();
    }

    private void info(String t, String m) {
        new Alert(Alert.AlertType.INFORMATION, m, ButtonType.OK) {{ setTitle(t); setHeaderText(null); }}.showAndWait();
    }
}
