package controller;

import dao.ClienteDAO;
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
import java.util.List;

public class VentasController {

    // ── HEADER ────────────────────────────────────────────────────
    @FXML private ComboBox<String> cbTipoVenta;

    // ── PANEL IZQUIERDO: info general ────────────────────────────
    @FXML private ComboBox<Cliente> cbCliente;
    @FXML private TextField         txtNuevoCliente;
    @FXML private DatePicker        dpFechaVenta;

    // Botones de método de pago
    @FXML private Button btnEfectivo;
    @FXML private Button btnTransferencia;
    @FXML private Button btnTarjeta;
    @FXML private Button btnOtro;
    @FXML private Label  lblMetodoPagoSeleccionado;

    // Comprobante
    @FXML private ComboBox<String> cbTipoComprobante;
    @FXML private TextField        txtNumeroComprobante;

    // Filtro + historial
    @FXML private ComboBox<String> cbFiltroTipo;
    @FXML private TableView<Venta>               tablaVentas;
    @FXML private TableColumn<Venta, Integer>    colIdVenta;
    @FXML private TableColumn<Venta, String>     colClienteVenta;
    @FXML private TableColumn<Venta, String>     colFechaVenta;
    @FXML private TableColumn<Venta, String>     colTipoVenta2;
    @FXML private TableColumn<Venta, BigDecimal> colTotalVenta2;

    // ── PANEL DERECHO: detalle de venta ──────────────────────────
    @FXML private TableView<DetalleVenta>               tablaDetalleVenta;
    @FXML private TableColumn<DetalleVenta, String>     colProductoVenta;
    @FXML private TableColumn<DetalleVenta, Integer>    colCantidadVenta;
    @FXML private TableColumn<DetalleVenta, BigDecimal> colPrecioVenta;
    @FXML private TableColumn<DetalleVenta, BigDecimal> colSubtotalVenta;
    @FXML private TableColumn<DetalleVenta, Void>       colAccionVenta;

    // Totales
    @FXML private Label lblSubtotal;
    @FXML private Label lblIva;
    @FXML private Label lblTotalVenta;

    // ── DAOs ──────────────────────────────────────────────────────
    private final VentaDAO    ventaDAO    = new VentaDAO();
    private final ClienteDAO  clienteDAO  = new ClienteDAO();
    private final ProductoDAO productoDAO = new ProductoDAO();
    private final RecetaDAO   recetaDAO   = new RecetaDAO();

    // ── ESTADO INTERNO ────────────────────────────────────────────
    private final ObservableList<DetalleVenta> detalleActual = FXCollections.observableArrayList();
    private final ObservableList<Venta>        historial     = FXCollections.observableArrayList();

    // Método de pago seleccionado actualmente (default: Efectivo)
    private String metodoPagoActual = "Efectivo";

    // Estilos para botones de método de pago
    private static final String ESTILO_METODO_ACTIVO   =
            "-fx-background-color: #4B3832; -fx-text-fill: white; -fx-background-radius: 10; -fx-font-weight: bold;";
    private static final String ESTILO_METODO_INACTIVO =
            "-fx-background-color: white; -fx-border-color: #4B3832; -fx-border-radius: 10; -fx-background-radius: 10; -fx-font-weight: bold;";

    // ─────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        configurarTablaDetalle();
        configurarTablaHistorial();
        cargarCombos();
        cargarHistorial();

        // Fecha por defecto: hoy
        if (dpFechaVenta != null) dpFechaVenta.setValue(LocalDate.now());

        // Método de pago: Efectivo activo por defecto
        actualizarEstiloMetodoPago(btnEfectivo);

        // Filtro historial — recarga al cambiar
        if (cbFiltroTipo != null) {
            cbFiltroTipo.valueProperty().addListener((obs, old, val) -> cargarHistorial());
        }

        // Al seleccionar una venta del historial → mostrar su detalle en la tabla
        if (tablaVentas != null) {
            tablaVentas.getSelectionModel().selectedItemProperty()
                    .addListener((obs, old, venta) -> {
                        if (venta != null) mostrarDetalleVenta(venta);
                    });
        }

        actualizarTotales();
    }

    // ── CONFIGURACIÓN TABLAS ──────────────────────────────────────

    private void configurarTablaDetalle() {
        if (tablaDetalleVenta == null) return;

        colProductoVenta.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getNombreProducto()));
        colCantidadVenta.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        colPrecioVenta.setCellValueFactory(new PropertyValueFactory<>("precioUnitario"));
        colSubtotalVenta.setCellValueFactory(new PropertyValueFactory<>("subtotal"));

        // Columna de acción: botón eliminar por fila
        colAccionVenta.setCellFactory(col -> new TableCell<>() {
            private final Button btnEliminar = new Button("✕ Quitar");
            {
                btnEliminar.setStyle(
                        "-fx-background-color: #C0392B; -fx-text-fill: white; " +
                                "-fx-background-radius: 6; -fx-font-size: 11;");
                btnEliminar.setOnAction(e -> {
                    DetalleVenta item = getTableView().getItems().get(getIndex());
                    detalleActual.remove(item);
                    actualizarTotales();
                });
            }
            @Override
            protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : btnEliminar);
            }
        });

        tablaDetalleVenta.setItems(detalleActual);
    }

    private void configurarTablaHistorial() {
        if (tablaVentas == null) return;

        if (colIdVenta      != null) colIdVenta.setCellValueFactory(new PropertyValueFactory<>("idVenta"));
        if (colClienteVenta != null) colClienteVenta.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getNombreCliente()));
        if (colFechaVenta   != null) colFechaVenta.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getFechaVenta() != null
                        ? d.getValue().getFechaVenta().toString() : ""));
        if (colTipoVenta2   != null) colTipoVenta2.setCellValueFactory(new PropertyValueFactory<>("tipoVenta"));
        if (colTotalVenta2  != null) colTotalVenta2.setCellValueFactory(new PropertyValueFactory<>("totalVenta"));

        tablaVentas.setItems(historial);
    }

    // ── CARGAR DATOS ──────────────────────────────────────────────

    private void cargarCombos() {
        if (cbTipoVenta != null)
            cbTipoVenta.setItems(FXCollections.observableArrayList("DIRECTA", "PEDIDO"));

        if (cbCliente != null)
            cbCliente.setItems(FXCollections.observableArrayList(clienteDAO.listarTodos()));

        if (cbTipoComprobante != null)
            cbTipoComprobante.setItems(FXCollections.observableArrayList("Factura", "Ticket", "Ninguno"));

        if (cbFiltroTipo != null)
            cbFiltroTipo.setItems(FXCollections.observableArrayList("Todas", "DIRECTA", "PEDIDO"));
    }

    private void cargarHistorial() {
        String filtro = cbFiltroTipo != null ? cbFiltroTipo.getValue() : null;

        List<Venta> ventas;
        if (filtro == null || filtro.equals("Todas")) {
            ventas = ventaDAO.listarTodas();
        } else {
            ventas = ventaDAO.listarPorTipo(filtro);
        }
        historial.setAll(ventas);
    }

    /**
     * Carga el detalle de la venta seleccionada en la tabla de detalle.
     * Solo es lectura — no modifica el formulario activo.
     */
    private void mostrarDetalleVenta(Venta venta) {
        List<DetalleVenta> detalles = ventaDAO.listarDetalles(venta.getIdVenta());
        // Si hay una venta en proceso, no pisarla — solo mostramos en una alerta informativa
        if (!detalleActual.isEmpty()) {
            // La tabla de detalle está en uso (nueva venta en curso): solo mostramos en alerta
            mostrarDetalleEnAlerta(venta, detalles);
        } else {
            // No hay venta en curso: podemos mostrar el detalle en la tabla como referencia
            detalleActual.setAll(detalles);
            actualizarTotales();
        }
    }

    private void mostrarDetalleEnAlerta(Venta venta, List<DetalleVenta> detalles) {
        StringBuilder sb = new StringBuilder();
        sb.append("Venta #").append(venta.getIdVenta())
                .append(" — ").append(venta.getFechaVenta())
                .append("\nCliente: ").append(venta.getNombreCliente())
                .append("\nMétodo: ").append(venta.getMetodoPago())
                .append("\n\nProductos:\n");

        for (DetalleVenta d : detalles) {
            sb.append("  • ").append(d.getNombreProducto())
                    .append(" x").append(d.getCantidad())
                    .append(" = $").append(d.getSubtotal().setScale(2, RoundingMode.HALF_UP))
                    .append("\n");
        }
        sb.append("\nTOTAL: $").append(venta.getTotalVenta().setScale(2, RoundingMode.HALF_UP));

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Detalle de Venta #" + venta.getIdVenta());
        alert.setHeaderText(null);
        alert.setContentText(sb.toString());
        alert.showAndWait();
    }

    // ── MÉTODO DE PAGO (botones toggle) ───────────────────────────

    @FXML
    public void seleccionarMetodoPago(ActionEvent e) {
        Button botonPresionado = (Button) e.getSource();
        metodoPagoActual = botonPresionado.getText();
        actualizarEstiloMetodoPago(botonPresionado);
        if (lblMetodoPagoSeleccionado != null)
            lblMetodoPagoSeleccionado.setText("Seleccionado: " + metodoPagoActual);
    }

    private void actualizarEstiloMetodoPago(Button activo) {
        List<Button> botones = new ArrayList<>();
        if (btnEfectivo      != null) botones.add(btnEfectivo);
        if (btnTransferencia != null) botones.add(btnTransferencia);
        if (btnTarjeta       != null) botones.add(btnTarjeta);
        if (btnOtro          != null) botones.add(btnOtro);

        for (Button b : botones) {
            b.setStyle(b == activo ? ESTILO_METODO_ACTIVO : ESTILO_METODO_INACTIVO);
        }
    }

    // ── AGREGAR PRODUCTO ──────────────────────────────────────────

    @FXML
    public void agregarProductoVenta() {
        Dialog<DetalleVenta> dialog = new Dialog<>();
        dialog.setTitle("Agregar Producto a la Venta");
        dialog.setHeaderText(null);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        ComboBox<Producto> cbProd = new ComboBox<>(
                FXCollections.observableArrayList(productoDAO.listarTodos()));
        cbProd.setPromptText("Seleccionar producto");
        cbProd.setMaxWidth(Double.MAX_VALUE);

        TextField txtCant = new TextField("1");
        txtCant.setPromptText("Cantidad");

        Label lblPrecio = new Label("Precio: —");

        // Mostrar precio al seleccionar producto
        cbProd.valueProperty().addListener((obs, old, p) -> {
            if (p != null)
                lblPrecio.setText("Precio unitario: $" + p.getPrecioVenta().setScale(2, RoundingMode.HALF_UP));
        });

        javafx.scene.layout.VBox box = new javafx.scene.layout.VBox(10,
                new Label("Producto:"), cbProd,
                new Label("Cantidad:"), txtCant,
                lblPrecio);
        box.setPadding(new javafx.geometry.Insets(15));
        box.setPrefWidth(320);
        dialog.getDialogPane().setContent(box);

        // Validar y construir el detalle
        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK && cbProd.getValue() != null) {
                Producto p = cbProd.getValue();
                int cant;
                try {
                    cant = Integer.parseInt(txtCant.getText().trim());
                    if (cant <= 0) throw new NumberFormatException();
                } catch (NumberFormatException ex) {
                    alerta("Cantidad inválida", "La cantidad debe ser un número entero positivo.");
                    return null;
                }
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
            // Si el producto ya está en la lista, aumentar cantidad
            boolean encontrado = false;
            for (DetalleVenta existente : detalleActual) {
                if (existente.getProducto().getIdProducto() == dv.getProducto().getIdProducto()) {
                    existente.setCantidad(existente.getCantidad() + dv.getCantidad());
                    existente.calcularSubtotal();
                    tablaDetalleVenta.refresh();
                    encontrado = true;
                    break;
                }
            }
            if (!encontrado) {
                detalleActual.add(dv);
            }
            actualizarTotales();
        });
    }

    // ── CREAR CLIENTE RÁPIDO ──────────────────────────────────────

    @FXML
    public void crearClienteRapido() {
        if (txtNuevoCliente == null) return;
        String nombre = txtNuevoCliente.getText().trim();
        if (nombre.isEmpty()) {
            alerta("Campo vacío", "Ingresa el nombre del cliente.");
            return;
        }
        Cliente nuevo = new Cliente();
        nuevo.setNombre(nombre);
        if (clienteDAO.insertar(nuevo)) {
            // Recargar combo y seleccionar el recién creado
            List<Cliente> todos = clienteDAO.listarTodos();
            cbCliente.setItems(FXCollections.observableArrayList(todos));
            todos.stream()
                    .filter(c -> c.getNombre().equalsIgnoreCase(nombre))
                    .findFirst()
                    .ifPresent(cbCliente::setValue);
            txtNuevoCliente.clear();
            info("Cliente creado", "\"" + nombre + "\" agregado correctamente.");
        } else {
            alerta("Error", "No se pudo crear el cliente.");
        }
    }

    // ── REGISTRAR VENTA ───────────────────────────────────────────

    @FXML
    public void registrarVenta() {
        // Validaciones
        String tipo = cbTipoVenta != null ? cbTipoVenta.getValue() : null;
        if (tipo == null) {
            alerta("Tipo de venta", "Selecciona el tipo de venta (DIRECTA o PEDIDO).");
            return;
        }
        if (detalleActual.isEmpty()) {
            alerta("Sin productos", "Agrega al menos un producto antes de registrar la venta.");
            return;
        }
        if (metodoPagoActual == null || metodoPagoActual.isEmpty()) {
            alerta("Método de pago", "Selecciona el método de pago.");
            return;
        }

        // Calcular total
        BigDecimal total = detalleActual.stream()
                .map(DetalleVenta::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Construir objeto Venta
        Venta venta = new Venta();
        venta.setTipoVenta(tipo);
        venta.setMetodoPago(metodoPagoActual);
        venta.setFechaVenta(dpFechaVenta != null && dpFechaVenta.getValue() != null
                ? dpFechaVenta.getValue() : LocalDate.now());
        venta.setCliente(cbCliente != null ? cbCliente.getValue() : null);
        venta.setNumeroComprobante(
                txtNumeroComprobante != null ? txtNumeroComprobante.getText().trim() : null);
        venta.setDetalles(new ArrayList<>(detalleActual));
        venta.setTotalVenta(total);

        // Guardar (con descuento de inventario)
        int id = ventaDAO.guardarVenta(venta, recetaDAO);
        if (id > 0) {
            info("Venta registrada",
                    "Venta #" + id + " registrada exitosamente.\n" +
                            "Total: $" + total.setScale(2, RoundingMode.HALF_UP) + "\n" +
                            "Inventario actualizado automáticamente.");
            limpiarFormulario();
            cargarHistorial();
        } else {
            alerta("Error al guardar",
                    "No se pudo registrar la venta. Verifica que haya stock suficiente " +
                            "de ingredientes y vuelve a intentarlo.");
        }
    }

    // ── LIMPIAR ───────────────────────────────────────────────────

    @FXML
    public void limpiarFormulario() {
        detalleActual.clear();
        if (cbTipoVenta          != null) cbTipoVenta.setValue(null);
        if (cbCliente            != null) cbCliente.setValue(null);
        if (dpFechaVenta         != null) dpFechaVenta.setValue(LocalDate.now());
        if (cbTipoComprobante    != null) cbTipoComprobante.setValue(null);
        if (txtNumeroComprobante != null) txtNumeroComprobante.clear();
        if (txtNuevoCliente      != null) txtNuevoCliente.clear();

        // Resetear método de pago
        metodoPagoActual = "Efectivo";
        actualizarEstiloMetodoPago(btnEfectivo);
        if (lblMetodoPagoSeleccionado != null)
            lblMetodoPagoSeleccionado.setText("Seleccionado: Efectivo");

        actualizarTotales();
    }

    // ── TOTALES ───────────────────────────────────────────────────

    private void actualizarTotales() {
        BigDecimal subtotal = detalleActual.stream()
                .map(DetalleVenta::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // IVA 13% — solo informativo, el precio de venta ya lo incluye
        BigDecimal iva = subtotal.multiply(BigDecimal.valueOf(0.13)).setScale(2, RoundingMode.HALF_UP);

        if (lblSubtotal  != null) lblSubtotal.setText("$" + subtotal.setScale(2, RoundingMode.HALF_UP));
        if (lblIva       != null) lblIva.setText("$" + iva);
        BigDecimal total = subtotal.add(iva);
        if (lblTotalVenta != null)
            lblTotalVenta.setText("$" + total.setScale(2, RoundingMode.HALF_UP));    }

    // ── ALERTAS ───────────────────────────────────────────────────

    private void alerta(String titulo, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(titulo);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void info(String titulo, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(titulo);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}