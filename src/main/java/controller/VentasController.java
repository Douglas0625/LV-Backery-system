package controller;

import dao.ClienteDAO;
import dao.PedidoDAO;
import dao.ProductoDAO;
import dao.RecetaDAO;
import dao.VentaDAO;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import model.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class VentasController {

    // ── HEADER ────────────────────────────────────────────────────
    @FXML private ComboBox<String> cbTipoVenta;

    // ── PANEL IZQUIERDO ───────────────────────────────────────────
    @FXML private ComboBox<Cliente> cbCliente;
    @FXML private TextField txtNuevoCliente;
    @FXML private DatePicker dpFechaVenta;

    // Metodo pago
    @FXML private Button btnEfectivo;
    @FXML private Button btnTransferencia;
    @FXML private Button btnTarjeta;
    @FXML private Button btnOtro;
    @FXML private Label lblMetodoPagoSeleccionado;

    // Comprobante
    @FXML private ComboBox<String> cbTipoComprobante;
    @FXML private TextField txtNumeroComprobante;

    // Historial
    @FXML private ComboBox<String> cbFiltroTipo;

    @FXML private TableView<Venta> tablaVentas;
    @FXML private TableColumn<Venta, Integer> colIdVenta;
    @FXML private TableColumn<Venta, String> colClienteVenta;
    @FXML private TableColumn<Venta, String> colFechaVenta;
    @FXML private TableColumn<Venta, String> colTipoVenta2;
    @FXML private TableColumn<Venta, BigDecimal> colTotalVenta2;

    // ── DETALLE ───────────────────────────────────────────────────
    @FXML private TableView<DetalleVenta> tablaDetalleVenta;
    @FXML private TableColumn<DetalleVenta, String> colProductoVenta;
    @FXML private TableColumn<DetalleVenta, Integer> colCantidadVenta;
    @FXML private TableColumn<DetalleVenta, BigDecimal> colPrecioVenta;
    @FXML private TableColumn<DetalleVenta, BigDecimal> colSubtotalVenta;
    @FXML private TableColumn<DetalleVenta, Void> colAccionVenta;

    // Totales
    @FXML private Label lblSubtotal;
    @FXML private Label lblIva;
    @FXML private Label lblTotalVenta;

    // ── DAOS ──────────────────────────────────────────────────────
    private final VentaDAO ventaDAO = new VentaDAO();
    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final ProductoDAO productoDAO = new ProductoDAO();
    private final RecetaDAO recetaDAO = new RecetaDAO();
    private final PedidoDAO pedidoDAO = new PedidoDAO();

    // ── ESTADO ────────────────────────────────────────────────────
    private final ObservableList<DetalleVenta> detalleActual =
            FXCollections.observableArrayList();

    private final ObservableList<Venta> historial =
            FXCollections.observableArrayList();

    private String metodoPagoActual = "Efectivo";

    private Pedido pedidoSeleccionado = null;

    private final Map<Integer, Integer> descontarVitrina =
            new LinkedHashMap<>();

    private static final String ESTILO_METODO_ACTIVO =
            "-fx-background-color: #4B3832; " +
                    "-fx-text-fill: white; " +
                    "-fx-background-radius: 10; " +
                    "-fx-font-weight: bold;";

    private static final String ESTILO_METODO_INACTIVO =
            "-fx-background-color: white; " +
                    "-fx-border-color: #4B3832; " +
                    "-fx-border-radius: 10; " +
                    "-fx-background-radius: 10; " +
                    "-fx-font-weight: bold;";

    // ─────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {

        configurarTablaDetalle();
        configurarTablaHistorial();

        cargarCombos();
        cargarHistorial();

        dpFechaVenta.setValue(LocalDate.now());

        actualizarEstiloMetodoPago(btnEfectivo);

        // FILTRO HISTORIAL
        cbFiltroTipo.valueProperty().addListener((obs, old, val) -> {
            cargarHistorial();
        });

        // CLICK HISTORIAL
        tablaVentas.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, venta) -> {
                    if (venta != null) {
                        mostrarDetalleVenta(venta);
                    }
                });

        // CAMBIO TIPO VENTA
        cbTipoVenta.valueProperty().addListener((obs, old, tipo) -> {

            if (tipo == null) return;

            detalleActual.clear();
            descontarVitrina.clear();
            pedidoSeleccionado = null;

            adaptarUIParaTipo(tipo);

            if ("PEDIDO".equals(tipo)) {
                abrirSelectorPedidoNormal();
            }

            if ("DIRECTA".equals(tipo)) {
                abrirSelectorVitrina();
            }

            actualizarTotales();
        });

        actualizarTotales();
    }

    // ── CONFIGURAR TABLAS ────────────────────────────────────────

    private void configurarTablaDetalle() {

        colProductoVenta.setCellValueFactory(d ->
                new SimpleStringProperty(
                        d.getValue().getNombreProducto()
                ));

        colCantidadVenta.setCellValueFactory(
                new PropertyValueFactory<>("cantidad"));

        colPrecioVenta.setCellValueFactory(
                new PropertyValueFactory<>("precioUnitario"));

        colSubtotalVenta.setCellValueFactory(
                new PropertyValueFactory<>("subtotal"));

        colAccionVenta.setCellFactory(col -> new TableCell<>() {

            private final Button btnEliminar =
                    new Button("✕ Quitar");

            {
                btnEliminar.setStyle(
                        "-fx-background-color: #C0392B;" +
                                "-fx-text-fill: white;" +
                                "-fx-background-radius: 6;"
                );

                btnEliminar.setOnAction(e -> {

                    DetalleVenta item =
                            getTableView()
                                    .getItems()
                                    .get(getIndex());

                    detalleActual.remove(item);

                    actualizarTotales();
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                setGraphic(empty ? null : btnEliminar);
            }
        });

        tablaDetalleVenta.setItems(detalleActual);
    }

    private void configurarTablaHistorial() {

        colIdVenta.setCellValueFactory(
                new PropertyValueFactory<>("idVenta"));

        colClienteVenta.setCellValueFactory(d ->
                new SimpleStringProperty(
                        d.getValue().getNombreCliente()
                ));

        colFechaVenta.setCellValueFactory(d ->
                new SimpleStringProperty(
                        d.getValue().getFechaVenta().toString()
                ));

        colTipoVenta2.setCellValueFactory(
                new PropertyValueFactory<>("tipoVenta"));

        colTotalVenta2.setCellValueFactory(
                new PropertyValueFactory<>("totalVenta"));

        tablaVentas.setItems(historial);
    }

    // ── CARGAR DATOS ─────────────────────────────────────────────

    private void cargarCombos() {

        cbTipoVenta.setItems(FXCollections.observableArrayList(
                "DIRECTA",
                "PEDIDO"
        ));

        cbCliente.setItems(
                FXCollections.observableArrayList(
                        clienteDAO.listarTodos()
                ));

        cbTipoComprobante.setItems(
                FXCollections.observableArrayList(
                        "Factura",
                        "Ticket",
                        "Ninguno"
                ));

        cbFiltroTipo.setItems(
                FXCollections.observableArrayList(
                        "Todas",
                        "DIRECTA",
                        "PEDIDO"
                ));
    }

    private void cargarHistorial() {

        String filtro = cbFiltroTipo.getValue();

        List<Venta> ventas =
                (filtro == null || filtro.equals("Todas"))
                        ? ventaDAO.listarTodas()
                        : ventaDAO.listarPorTipo(filtro);

        historial.setAll(ventas);
    }

    // ── MOSTRAR DETALLE ──────────────────────────────────────────

    private void mostrarDetalleVenta(Venta venta) {

        List<DetalleVenta> detalles =
                ventaDAO.listarDetalles(venta.getIdVenta());

        detalleActual.setAll(detalles);

        actualizarTotales();
    }

    // ── ADAPTAR UI ───────────────────────────────────────────────

    private void adaptarUIParaTipo(String tipo) {

        if ("PEDIDO".equals(tipo)) {
            cbCliente.setDisable(true);
        } else {
            cbCliente.setDisable(false);
        }
    }

    // ── MeTODO PAGO ──────────────────────────────────────────────

    @FXML
    public void seleccionarMetodoPago(ActionEvent e) {

        Button boton = (Button) e.getSource();

        metodoPagoActual = boton.getText();

        actualizarEstiloMetodoPago(boton);

        lblMetodoPagoSeleccionado.setText(
                "Seleccionado: " + metodoPagoActual
        );
    }

    private void actualizarEstiloMetodoPago(Button activo) {

        List<Button> botones = List.of(
                btnEfectivo,
                btnTransferencia,
                btnTarjeta,
                btnOtro
        );

        for (Button b : botones) {

            b.setStyle(
                    b == activo
                            ? ESTILO_METODO_ACTIVO
                            : ESTILO_METODO_INACTIVO
            );
        }
    }

    // ── AGREGAR PRODUCTO ─────────────────────────────────────────

    @FXML
    public void agregarProductoVenta() {

        String tipo = cbTipoVenta.getValue();

        if (tipo == null) {
            alerta("Tipo de venta",
                    "Selecciona el tipo de venta.");
            return;
        }

        if ("PEDIDO".equals(tipo)) {
            abrirSelectorPedidoNormal();
        } else {
            abrirSelectorVitrina();
        }
    }

    // ── PEDIDOS NORMALES ─────────────────────────────────────────

    private void abrirSelectorPedidoNormal() {

        List<Pedido> pedidos =
                pedidoDAO.listarListosPorTipo(false);

        if (pedidos.isEmpty()) {

            alerta("Sin pedidos",
                    "No hay pedidos LISTOS.");

            return;
        }

        seleccionarDeListaDialog(
                "Seleccionar Pedido",
                "Pedidos listos:",
                pedidos
        ).ifPresent(pedido -> {

            pedidoSeleccionado = pedido;

            detalleActual.clear();

            List<DetallePedido> detalles =
                    pedidoDAO.listarDetalles(
                            pedido.getIdPedido());

            for (DetallePedido dp : detalles) {

                DetalleVenta dv = new DetalleVenta();

                dv.setProducto(dp.getProducto());
                dv.setCantidad(dp.getCantidad());
                dv.setPrecioUnitario(dp.getPrecioUnitario());

                dv.calcularSubtotal();

                detalleActual.add(dv);
            }

            cbCliente.setValue(pedido.getCliente());

            actualizarTotales();
        });
    }

    // ── VITRINA ──────────────────────────────────────────────────

    private void abrirSelectorVitrina() {

        List<Pedido> pedidos =
                pedidoDAO.listarListosPorTipo(true);

        if (pedidos.isEmpty()) {

            alerta("Sin stock",
                    "No hay pedidos VITRINA.");

            return;
        }

        seleccionarDeListaDialog(
                "Seleccionar Vitrina",
                "Pedidos vitrinas:",
                pedidos
        ).ifPresent(pedido -> {

            List<DetallePedido> disponibles =
                    pedidoDAO.listarDetallesConRestante(
                            pedido.getIdPedido());

            seleccionarDeListaDialog(
                    "Productos",
                    "Selecciona producto:",
                    disponibles
            ).ifPresent(dp -> {

                TextInputDialog dialog =
                        new TextInputDialog("1");

                dialog.setTitle("Cantidad");

                dialog.setHeaderText(
                        dp.getProducto().getNombreProducto()
                );

                dialog.setContentText("Cantidad:");

                dialog.showAndWait().ifPresent(cantStr -> {

                    try {

                        int cantidad =
                                Integer.parseInt(cantStr);

                        DetalleVenta dv =
                                new DetalleVenta();

                        dv.setProducto(dp.getProducto());
                        dv.setCantidad(cantidad);
                        dv.setPrecioUnitario(
                                dp.getPrecioUnitario());

                        dv.calcularSubtotal();

                        detalleActual.add(dv);

                        descontarVitrina.merge(
                                dp.getIdDetallePedido(),
                                cantidad,
                                Integer::sum
                        );

                        actualizarTotales();

                    } catch (Exception ex) {

                        alerta("Error",
                                "Cantidad inválida.");
                    }
                });
            });
        });
    }

    // ── DIALOG GENÉRICO ──────────────────────────────────────────

    private <T> java.util.Optional<T> seleccionarDeListaDialog(
            String titulo,
            String header,
            List<T> items
    ) {

        Dialog<T> dialog = new Dialog<>();

        dialog.setTitle(titulo);

        dialog.setHeaderText(header);

        dialog.getDialogPane().getButtonTypes().addAll(
                ButtonType.OK,
                ButtonType.CANCEL
        );

        ListView<T> listView =
                new ListView<>(
                        FXCollections.observableArrayList(items)
                );

        listView.getSelectionModel().selectFirst();

        dialog.getDialogPane().setContent(listView);

        dialog.setResultConverter(bt -> {

            if (bt == ButtonType.OK) {
                return listView.getSelectionModel()
                        .getSelectedItem();
            }

            return null;
        });

        return dialog.showAndWait();
    }

    // ── CLIENTE RÁPIDO ───────────────────────────────────────────

    @FXML
    public void crearClienteRapido() {

        String nombre = txtNuevoCliente.getText().trim();

        if (nombre.isEmpty()) {
            alerta("Campo vacío",
                    "Ingresa un nombre.");
            return;
        }

        Cliente cliente = new Cliente();

        cliente.setNombre(nombre);

        if (clienteDAO.insertar(cliente)) {

            cbCliente.setItems(
                    FXCollections.observableArrayList(
                            clienteDAO.listarTodos()
                    ));

            txtNuevoCliente.clear();

            info("Cliente creado",
                    "Cliente agregado.");
        }
    }

    // ── REGISTRAR ────────────────────────────────────────────────

    @FXML
    public void registrarVenta() {

        if (detalleActual.isEmpty()) {

            alerta("Sin productos",
                    "Agrega productos.");

            return;
        }

        BigDecimal total =
                detalleActual.stream()
                        .map(DetalleVenta::getSubtotal)
                        .reduce(BigDecimal.ZERO,
                                BigDecimal::add);

        Venta venta = new Venta();

        venta.setCliente(cbCliente.getValue());

        venta.setFechaVenta(dpFechaVenta.getValue());

        venta.setTipoVenta(cbTipoVenta.getValue());

        venta.setMetodoPago(metodoPagoActual);

        venta.setNumeroComprobante(
                txtNumeroComprobante.getText());

        venta.setTotalVenta(total);

        venta.setDetalles(
                new ArrayList<>(detalleActual));

        if (pedidoSeleccionado != null) {
            venta.setIdPedido(
                    pedidoSeleccionado.getIdPedido());
        }

        int id;

        if ("PEDIDO".equals(cbTipoVenta.getValue())) {

            int estadoEntregado =
                    ventaDAO.obtenerIdEstadoPorNombre(
                            "Entregado");

            id = ventaDAO.registrarVentaPedido(
                    venta,
                    estadoEntregado
            );

        } else {

            id = ventaDAO.registrarVentaDirectaVitrina(
                    venta,
                    descontarVitrina,
                    pedidoDAO
            );
        }

        if (id > 0) {

            info("Venta registrada",
                    "Venta #" + id + " registrada.");

            limpiarFormulario();

            cargarHistorial();

        } else {

            alerta("Error",
                    "No se pudo registrar.");
        }
    }

    // ── LIMPIAR ──────────────────────────────────────────────────

    @FXML
    public void limpiarFormulario() {

        detalleActual.clear();

        descontarVitrina.clear();

        pedidoSeleccionado = null;

        cbCliente.setValue(null);

        cbCliente.setDisable(false);

        dpFechaVenta.setValue(LocalDate.now());

        cbTipoComprobante.setValue(null);

        txtNumeroComprobante.clear();

        txtNuevoCliente.clear();

        metodoPagoActual = "Efectivo";

        actualizarEstiloMetodoPago(btnEfectivo);

        lblMetodoPagoSeleccionado.setText(
                "Seleccionado: Efectivo"
        );

        actualizarTotales();
    }

    // ── TOTALES ──────────────────────────────────────────────────

    private void actualizarTotales() {

        BigDecimal subtotal =
                detalleActual.stream()
                        .map(DetalleVenta::getSubtotal)
                        .reduce(BigDecimal.ZERO,
                                BigDecimal::add);

        BigDecimal iva =
                subtotal.multiply(
                        BigDecimal.valueOf(0.13));

        lblSubtotal.setText(
                "$" + subtotal.setScale(
                        2,
                        RoundingMode.HALF_UP
                ));

        lblIva.setText(
                "$" + iva.setScale(
                        2,
                        RoundingMode.HALF_UP
                ));

        lblTotalVenta.setText(
                "$" + subtotal.add(iva)
                        .setScale(
                                2,
                                RoundingMode.HALF_UP
                        ));
    }

    // ── ALERTAS ──────────────────────────────────────────────────

    private void alerta(String titulo, String msg) {

        Alert alert =
                new Alert(Alert.AlertType.ERROR);

        alert.setTitle(titulo);

        alert.setHeaderText(null);

        alert.setContentText(msg);

        alert.showAndWait();
    }

    private void info(String titulo, String msg) {

        Alert alert =
                new Alert(Alert.AlertType.INFORMATION);

        alert.setTitle(titulo);

        alert.setHeaderText(null);

        alert.setContentText(msg);

        alert.showAndWait();
    }
}