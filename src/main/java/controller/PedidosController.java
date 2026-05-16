package controller;

import dao.ClienteDAO;
import dao.PedidoDAO;
import dao.ProductoDAO;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import model.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PedidosController {

    // Filtros
    @FXML private TextField    txtBuscarCliente;
    @FXML private DatePicker   dpFechaFiltro;
    @FXML private ComboBox<EstadoPedido> cbEstadoFiltro;
    @FXML private Button       btnNuevoPedido;
    @FXML private Button       btnLimpiar;

    // Tabla principal
    @FXML private TableView<Pedido>                  tablePedidos;
    @FXML private TableColumn<Pedido, Integer>        colIdPedido;
    @FXML private TableColumn<Pedido, String>         colCliente;
    @FXML private TableColumn<Pedido, String>         colFechaPedido;
    @FXML private TableColumn<Pedido, String>         colFechaEntrega;
    @FXML private TableColumn<Pedido, BigDecimal>     colTotalPedido;
    @FXML private TableColumn<Pedido, String>         colEstadoPedido;

    // Formulario nuevo pedido
    @FXML private ComboBox<Cliente>      cbCliente;
    @FXML private DatePicker             dpFechaPedido;
    @FXML private DatePicker             dpFechaEntrega;
    @FXML private ComboBox<EstadoPedido> cbEstadoPedido;
    @FXML private TextArea               txtDescripcionPedido;
    @FXML private Button                 btnAgregarItem;
    @FXML private Label                  lblTotalPedido;
    @FXML private Button                 btnGuardarPedido;

    // Tabla detalle (formulario)
    @FXML private TableView<DetallePedido>              tableDetallePedido;
    @FXML private TableColumn<DetallePedido, String>    colProductoDetalle;
    @FXML private TableColumn<DetallePedido, Integer>   colCantidadDetalle;
    @FXML private TableColumn<DetallePedido, BigDecimal> colPrecioDetalle;
    @FXML private TableColumn<DetallePedido, BigDecimal> colSubtotalDetalle;

    // Panel detalle seleccionado
    @FXML private Label lblDetalleIdPedido;
    @FXML private Label lblDetalleFecha;
    @FXML private Label lblNotasProduccion;
    @FXML private Button btnImprimir;
    @FXML private Button btnModificar;
    @FXML private TableView<DetallePedido>               tableDetalleSeleccionado;
    @FXML private TableColumn<DetallePedido, String>     colDetalleProducto;
    @FXML private TableColumn<DetallePedido, Integer>    colDetalleCantidad;
    @FXML private TableColumn<DetallePedido, BigDecimal> colDetallePrecio;

    private final PedidoDAO  pedidoDAO  = new PedidoDAO();
    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final ProductoDAO productoDAO = new ProductoDAO();

    private final ObservableList<Pedido>       listaPedidos  = FXCollections.observableArrayList();
    private final ObservableList<DetallePedido> detalleForm  = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        configurarTablaPrincipal();
        configurarTablaDetalle();
        cargarCombos();
        cargarPedidos();

        tablePedidos.getSelectionModel().selectedItemProperty().addListener((obs, old, nuevo) -> {
            if (nuevo != null) mostrarDetallePedido(nuevo);
        });

        if (lblTotalPedido != null) lblTotalPedido.setText("$0.00");
        if (dpFechaPedido  != null) dpFechaPedido.setValue(LocalDate.now());
    }

    private void configurarTablaPrincipal() {
        if (tablePedidos == null) return;
        colIdPedido.setCellValueFactory(new PropertyValueFactory<>("idPedido"));
        colCliente.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNombreCliente()));
        colFechaPedido.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getFechaPedido() != null ? d.getValue().getFechaPedido().toString() : ""));
        colFechaEntrega.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getFechaEntrega() != null ? d.getValue().getFechaEntrega().toString() : ""));
        colTotalPedido.setCellValueFactory(new PropertyValueFactory<>("totalPedido"));
        colEstadoPedido.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNombreEstado()));
        tablePedidos.setItems(listaPedidos);
    }

    private void configurarTablaDetalle() {
        if (tableDetallePedido == null) return;
        colProductoDetalle.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getProducto() != null ? d.getValue().getProducto().getNombreProducto() : ""));
        colCantidadDetalle.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        colPrecioDetalle.setCellValueFactory(new PropertyValueFactory<>("precioUnitario"));
        colSubtotalDetalle.setCellValueFactory(new PropertyValueFactory<>("subtotal"));
        tableDetallePedido.setItems(detalleForm);
    }

    private void cargarCombos() {
        ObservableList<Cliente> clientes       = FXCollections.observableArrayList(clienteDAO.listarTodos());
        ObservableList<EstadoPedido> estados   = FXCollections.observableArrayList(pedidoDAO.listarEstados());

        if (cbCliente      != null) cbCliente.setItems(clientes);
        if (cbEstadoPedido != null) cbEstadoPedido.setItems(estados);
        if (cbEstadoFiltro != null) {
            ObservableList<EstadoPedido> estadosFiltro = FXCollections.observableArrayList(pedidoDAO.listarEstados());
            cbEstadoFiltro.setItems(estadosFiltro);
        }
    }

    private void cargarPedidos() {
        listaPedidos.setAll(pedidoDAO.listarTodos());
    }

    @FXML
    public void filtrarPedidos() {
        String textoBusqueda   = txtBuscarCliente != null ? txtBuscarCliente.getText().trim().toLowerCase() : "";
        EstadoPedido estadoFil = cbEstadoFiltro   != null ? cbEstadoFiltro.getValue() : null;

        ObservableList<Pedido> filtrados = FXCollections.observableArrayList(pedidoDAO.listarTodos());
        filtrados.removeIf(p -> {
            boolean nombre = !textoBusqueda.isEmpty() && !p.getNombreCliente().toLowerCase().contains(textoBusqueda);
            boolean estado = estadoFil != null && p.getEstadoPedido().getIdEstadoPedido() != estadoFil.getIdEstadoPedido();
            return nombre || estado;
        });
        listaPedidos.setAll(filtrados);
    }

    @FXML
    public void limpiarFiltros() {
        if (txtBuscarCliente != null) txtBuscarCliente.clear();
        if (cbEstadoFiltro   != null) cbEstadoFiltro.setValue(null);
        if (dpFechaFiltro    != null) dpFechaFiltro.setValue(null);
        cargarPedidos();
    }

    @FXML
    public void agregarItemPedido() {
        Dialog<DetallePedido> dialog = new Dialog<>();
        dialog.setTitle("Añadir Producto al Pedido");
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
                Producto prod = cbProd.getValue();
                int cant;
                try { cant = Integer.parseInt(txtCant.getText().trim()); } catch (Exception ex) { cant = 1; }
                DetallePedido dp = new DetallePedido();
                dp.setProducto(prod);
                dp.setCantidad(cant);
                dp.setPrecioUnitario(prod.getPrecioVenta());
                dp.calcularSubtotal();
                return dp;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(dp -> {
            detalleForm.add(dp);
            recalcularTotal();
        });
    }

    @FXML
    public void guardarPedido() {
        Cliente cliente   = cbCliente      != null ? cbCliente.getValue()      : null;
        LocalDate entrega = dpFechaEntrega != null ? dpFechaEntrega.getValue() : null;
        EstadoPedido est  = cbEstadoPedido != null ? cbEstadoPedido.getValue() : null;

        if (cliente == null || entrega == null || est == null || detalleForm.isEmpty()) {
            alerta("Incompleto", "Selecciona cliente, fecha de entrega, estado y al menos un producto.");
            return;
        }

        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setFechaPedido(LocalDate.now());
        pedido.setFechaEntrega(entrega);
        pedido.setEstadoPedido(est);
        pedido.setDescripcionPedido(txtDescripcionPedido != null ? txtDescripcionPedido.getText() : "");
        pedido.setDetalles(new java.util.ArrayList<>(detalleForm));
        pedido.setTotalPedido(detalleForm.stream().map(DetallePedido::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        int id = pedidoDAO.insertar(pedido);
        if (id > 0) {
            info("Éxito", "Pedido #" + id + " creado correctamente.");
            limpiarFormularioPedido();
            cargarPedidos();
        } else {
            alerta("Error", "No se pudo guardar el pedido.");
        }
    }

    private void mostrarDetallePedido(Pedido p) {
        if (lblDetalleIdPedido != null) lblDetalleIdPedido.setText("ORD-" + String.format("%04d", p.getIdPedido()));
        if (lblDetalleFecha    != null) lblDetalleFecha.setText(p.getFechaEntrega() != null ? p.getFechaEntrega().toString() : "");
        if (lblNotasProduccion != null) lblNotasProduccion.setText(p.getDescripcionPedido() != null ? p.getDescripcionPedido() : "Sin notas");

        // Cargar detalles en tabla inferior
        if (tableDetalleSeleccionado != null) {
            if (colDetalleProducto != null)
                colDetalleProducto.setCellValueFactory(d -> new SimpleStringProperty(
                        d.getValue().getProducto() != null ? d.getValue().getProducto().getNombreProducto() : ""));
            if (colDetalleCantidad != null)
                colDetalleCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
            if (colDetallePrecio != null)
                colDetallePrecio.setCellValueFactory(new PropertyValueFactory<>("precioUnitario"));
            tableDetalleSeleccionado.setItems(
                    FXCollections.observableArrayList(pedidoDAO.listarDetalles(p.getIdPedido())));
        }
    }

    private void recalcularTotal() {
        BigDecimal total = detalleForm.stream().map(DetallePedido::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (lblTotalPedido != null) lblTotalPedido.setText("$" + total.setScale(2, java.math.RoundingMode.HALF_UP));
    }

    private void limpiarFormularioPedido() {
        if (cbCliente          != null) cbCliente.setValue(null);
        if (dpFechaEntrega     != null) dpFechaEntrega.setValue(null);
        if (cbEstadoPedido     != null) cbEstadoPedido.setValue(null);
        if (txtDescripcionPedido != null) txtDescripcionPedido.clear();
        if (lblTotalPedido     != null) lblTotalPedido.setText("$0.00");
        detalleForm.clear();
    }

    private void alerta(String t, String m) {
        new Alert(Alert.AlertType.ERROR, m, ButtonType.OK) {{ setTitle(t); setHeaderText(null); }}.showAndWait();
    }

    private void info(String t, String m) {
        new Alert(Alert.AlertType.INFORMATION, m, ButtonType.OK) {{ setTitle(t); setHeaderText(null); }}.showAndWait();
    }
}
