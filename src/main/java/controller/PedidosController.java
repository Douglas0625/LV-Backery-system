package controller;

import dao.ClienteDAO;
import dao.PedidoDAO;
import dao.ProductoDAO;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import model.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PedidosController {

    // ── FILTROS ──────────────────────────────────────────────────
    @FXML private TextField              txtBuscarCliente;
    @FXML private DatePicker             dpFechaFiltro;
    @FXML private ComboBox<EstadoPedido> cbEstadoFiltro;
    @FXML private Button                 btnNuevoPedido;
    @FXML private Button                 btnLimpiar;

    // ── TABLA PRINCIPAL ───────────────────────────────────────────
    @FXML private TableView<Pedido>               tablePedidos;
    @FXML private TableColumn<Pedido, Integer>    colIdPedido;
    @FXML private TableColumn<Pedido, String>     colCliente;
    @FXML private TableColumn<Pedido, String>     colFechaPedido;
    @FXML private TableColumn<Pedido, String>     colFechaEntrega;
    @FXML private TableColumn<Pedido, BigDecimal> colTotalPedido;
    @FXML private TableColumn<Pedido, String>     colEstadoPedido;

    // ── FORMULARIO DERECHO ────────────────────────────────────────
    @FXML private ComboBox<Cliente>      cbCliente;
    @FXML private DatePicker             dpFechaPedido;
    @FXML private DatePicker             dpFechaEntrega;
    @FXML private ComboBox<EstadoPedido> cbEstadoPedido;
    @FXML private TextArea               txtDescripcionPedido;
    @FXML private Button                 btnAgregarItem;
    @FXML private Label                  lblTotalPedido;
    @FXML private Button                 btnGuardarPedido;

    // ── TABLA DETALLE (FORMULARIO) ────────────────────────────────
    @FXML private TableView<DetallePedido>               tableDetallePedido;
    @FXML private TableColumn<DetallePedido, String>     colProductoDetalle;
    @FXML private TableColumn<DetallePedido, Integer>    colCantidadDetalle;
    @FXML private TableColumn<DetallePedido, BigDecimal> colPrecioDetalle;
    @FXML private TableColumn<DetallePedido, BigDecimal> colSubtotalDetalle;

    // ── PANEL DETALLE SELECCIONADO ────────────────────────────────
    @FXML private Label                              lblDetalleIdPedido;
    @FXML private Label                              lblDetalleFecha;
    @FXML private Label                              lblNotasProduccion;
    @FXML private Button                             btnImprimir;
    @FXML private Button                             btnModificar;
    @FXML private TableView<DetallePedido>           tableDetalleSeleccionado;
    @FXML private TableColumn<DetallePedido, String>     colDetalleProducto;
    @FXML private TableColumn<DetallePedido, Integer>    colDetalleCantidad;
    @FXML private TableColumn<DetallePedido, BigDecimal> colDetallePrecio;
    @FXML private TableColumn<DetallePedido, BigDecimal> colDetalleSubtotal;

    // ── DAOs ──────────────────────────────────────────────────────
    private final PedidoDAO   pedidoDAO   = new PedidoDAO();
    private final ClienteDAO  clienteDAO  = new ClienteDAO();
    private final ProductoDAO productoDAO = new ProductoDAO();

    // ── ESTADO INTERNO ────────────────────────────────────────────
    private final ObservableList<Pedido>        listaPedidos = FXCollections.observableArrayList();
    private final ObservableList<DetallePedido> detalleForm  = FXCollections.observableArrayList();
    /** null = modo crear, != null = modo editar */
    private Pedido pedidoEditando = null;

    // ─────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        configurarTablaPrincipal();
        configurarTablaDetalle();
        configurarTablaDetalleSeleccionado();
        cargarCombos();
        cargarPedidos();
        configurarFiltros();
        configurarSeleccionTabla();
        configurarBotones();

        dpFechaPedido.setValue(LocalDate.now());
        actualizarTotal();
    }

    // ── CONFIGURACIÓN ─────────────────────────────────────────────

    private void configurarTablaPrincipal() {
        colIdPedido.setCellValueFactory(new PropertyValueFactory<>("idPedido"));
        colCliente.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getNombreCliente()));
        colFechaPedido.setCellValueFactory(d ->
                new SimpleStringProperty(fmt(d.getValue().getFechaPedido())));
        colFechaEntrega.setCellValueFactory(d ->
                new SimpleStringProperty(fmt(d.getValue().getFechaEntrega())));
        colTotalPedido.setCellValueFactory(new PropertyValueFactory<>("totalPedido"));
        colEstadoPedido.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getNombreEstado()));

        // Color por estado
        colEstadoPedido.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); return; }
                setText(v);
                String color = switch (v) {
                    case "Pendiente"     -> "#E67E22";
                    case "En producción" -> "#2980B9";
                    case "Listo"         -> "#27AE60";
                    case "Entregado"     -> "#7F8C8D";
                    case "Cancelado"     -> "#C0392B";
                    default              -> "#4B3832";
                };
                setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
            }
        });

        tablePedidos.setItems(listaPedidos);
    }

    private void configurarTablaDetalle() {
        colProductoDetalle.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getProducto() != null
                        ? d.getValue().getProducto().getNombreProducto() : ""));
        colCantidadDetalle.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        colPrecioDetalle.setCellValueFactory(new PropertyValueFactory<>("precioUnitario"));
        colSubtotalDetalle.setCellValueFactory(new PropertyValueFactory<>("subtotal"));
        tableDetallePedido.setItems(detalleForm);

        // Click derecho para eliminar item
        MenuItem mnuEliminar = new MenuItem("Eliminar item");
        mnuEliminar.setOnAction(e -> eliminarItemDetalle());
        tableDetallePedido.setContextMenu(new ContextMenu(mnuEliminar));
    }

    private void configurarTablaDetalleSeleccionado() {
        if (colDetalleProducto != null)
            colDetalleProducto.setCellValueFactory(d -> new SimpleStringProperty(
                    d.getValue().getProducto() != null
                            ? d.getValue().getProducto().getNombreProducto() : ""));
        if (colDetalleCantidad != null)
            colDetalleCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        if (colDetallePrecio != null)
            colDetallePrecio.setCellValueFactory(new PropertyValueFactory<>("precioUnitario"));
        if (colDetalleSubtotal != null)
            colDetalleSubtotal.setCellValueFactory(new PropertyValueFactory<>("subtotal"));
    }

    private void cargarCombos() {
        // Clientes
        cbCliente.setItems(FXCollections.observableArrayList(clienteDAO.listarTodos()));

        // Estado formulario: excluye Entregado y Cancelado
        List<EstadoPedido> estadosForm = pedidoDAO.listarEstadosFormulario();
        cbEstadoPedido.setItems(FXCollections.observableArrayList(estadosForm));

        // Estado filtro: todos los estados
        cbEstadoFiltro.setItems(FXCollections.observableArrayList(pedidoDAO.listarEstados()));
        cbEstadoFiltro.setPromptText("Todos los estados");

        // Estado por defecto en formulario = Pendiente
        estadosForm.stream()
                .filter(e -> e.getNombreEstado().equalsIgnoreCase("Pendiente"))
                .findFirst()
                .ifPresent(cbEstadoPedido::setValue);
    }

    private void cargarPedidos() {
        listaPedidos.setAll(pedidoDAO.listarTodos());
    }

    private void configurarFiltros() {
        txtBuscarCliente.textProperty().addListener((o, old, v) -> aplicarFiltros());
        cbEstadoFiltro.valueProperty().addListener((o, old, v) -> aplicarFiltros());
        dpFechaFiltro.valueProperty().addListener((o, old, v) -> aplicarFiltros());
    }

    private void configurarSeleccionTabla() {
        tablePedidos.getSelectionModel().selectedItemProperty()
                .addListener((o, old, sel) -> {
                    if (sel != null) mostrarDetallePedido(sel);
                });
    }

    private void configurarBotones() {
        btnNuevoPedido.setOnAction(e -> prepararNuevoPedido());
        btnLimpiar.setOnAction(e -> limpiarFiltros());
        btnAgregarItem.setOnAction(e -> abrirDialogoAgregarItem());
        btnGuardarPedido.setOnAction(e -> guardarPedido());
        if (btnModificar != null) btnModificar.setOnAction(e -> cargarPedidoEnFormulario());
    }

    // ── FILTROS ───────────────────────────────────────────────────

    private void aplicarFiltros() {
        String texto = txtBuscarCliente.getText().trim().toLowerCase();
        EstadoPedido estadoFil = cbEstadoFiltro.getValue();
        LocalDate    fechaFil  = dpFechaFiltro.getValue();

        List<Pedido> todos    = pedidoDAO.listarTodos();
        List<Pedido> filtrados = todos.stream().filter(p -> {
            boolean ok = true;
            if (!texto.isEmpty())
                ok = p.getNombreCliente().toLowerCase().contains(texto);
            if (estadoFil != null)
                ok = ok && p.getEstadoPedido().getIdEstadoPedido() == estadoFil.getIdEstadoPedido();
            if (fechaFil != null)
                ok = ok && p.getFechaPedido().equals(fechaFil);
            return ok;
        }).toList();
        listaPedidos.setAll(filtrados);
    }

    @FXML
    public void limpiarFiltros() {
        txtBuscarCliente.clear();
        cbEstadoFiltro.setValue(null);
        dpFechaFiltro.setValue(null);
        cargarPedidos();
    }

    // ── CREAR / EDITAR ────────────────────────────────────────────

    @FXML
    public void prepararNuevoPedido() {
        pedidoEditando = null;
        limpiarFormulario();
        habilitarFormulario(true);
        cbEstadoPedido.setDisable(false);
        btnGuardarPedido.setText("Crear Nuevo Pedido");
    }

    private void cargarPedidoEnFormulario() {
        Pedido sel = tablePedidos.getSelectionModel().getSelectedItem();
        if (sel == null) {
            alerta("Selecciona un pedido de la tabla primero.");
            return;
        }

        // Bloquear edición de pedidos terminados
        String estado = sel.getNombreEstado();
        if (!"Pendiente".equals(estado)) {
            alerta("Solo se pueden modificar pedidos en estado 'Pendiente'.");
            return;
        }

        pedidoEditando = sel;

        cbCliente.getItems().stream()
                .filter(c -> c.getIdCliente() == sel.getCliente().getIdCliente())
                .findFirst().ifPresent(cbCliente::setValue);

        dpFechaPedido.setValue(sel.getFechaPedido());
        dpFechaEntrega.setValue(sel.getFechaEntrega());

        cbEstadoPedido.getItems().stream()
                .filter(e -> e.getIdEstadoPedido() == sel.getEstadoPedido().getIdEstadoPedido())
                .findFirst().ifPresent(cbEstadoPedido::setValue);

        txtDescripcionPedido.setText(sel.getDescripcionPedido());
        detalleForm.setAll(pedidoDAO.listarDetalles(sel.getIdPedido()));
        actualizarTotal();
        habilitarFormulario(true);
        cbEstadoPedido.setDisable(true);
        btnGuardarPedido.setText("Guardar Cambios");
    }

    @FXML
    public void guardarPedido() {
        if (!validarFormulario()) return;

        Pedido p = new Pedido();
        p.setCliente(cbCliente.getValue());
        p.setFechaPedido(dpFechaPedido.getValue());
        p.setFechaEntrega(dpFechaEntrega.getValue());
        p.setEstadoPedido(cbEstadoPedido.getValue());
        p.setDescripcionPedido(txtDescripcionPedido.getText().trim());
        p.setDetalles(new ArrayList<>(detalleForm));
        p.setTotalPedido(calcularTotal());

        if (pedidoEditando == null) {
            int id = pedidoDAO.insertar(p);
            if (id > 0) {
                exito("Pedido #" + id + " creado correctamente.");
                limpiarFormulario();
            } else {
                alerta("No se pudo crear el pedido. Revise la conexión con la base de datos.");
            }
        } else {
            p.setIdPedido(pedidoEditando.getIdPedido());
            if (pedidoDAO.actualizar(p)) {
                exito("Pedido actualizado correctamente.");
                limpiarFormulario();
                pedidoEditando = null;
                btnGuardarPedido.setText("Crear Nuevo Pedido");
            } else {
                alerta("No se pudo actualizar el pedido. Revise la conexión con la base de datos.");
            }
        }
        cargarPedidos();
    }

    // ── ELIMINAR ──────────────────────────────────────────────────

    @FXML
    public void eliminarPedido() {
        Pedido sel = tablePedidos.getSelectionModel().getSelectedItem();
        if (sel == null) {
            alerta("Selecciona un pedido para eliminar.");
            return;
        }

        String estadoSel = sel.getNombreEstado();
        if (!"Pendiente".equals(estadoSel)) {
            alerta("No se pueden eliminar pedidos que no estén en estado 'Pendiente'.");
            return;
        }

        Alert conf = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Eliminar el pedido #" + sel.getIdPedido() +
                        " de " + sel.getNombreCliente() + "?\n\nEsta acción no se puede deshacer.",
                ButtonType.YES, ButtonType.NO);
        conf.setTitle("Confirmar eliminación");
        conf.setHeaderText(null);
        conf.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> {
            if (pedidoDAO.eliminar(sel.getIdPedido())) {
                exito("Pedido eliminado.");
                limpiarDetallePanelInferior();
                if (pedidoEditando != null && pedidoEditando.getIdPedido() == sel.getIdPedido()) {
                    limpiarFormulario();
                }
                cargarPedidos();
            } else {
                alerta("No se pudo eliminar el pedido.");
            }
        });
    }

    // ── CAMBIAR ESTADO ────────────────────────────────────────────

    /**
     * Permite cambiar a cualquier estado.
     * Si el nuevo estado es "En producción" y el anterior no lo era,
     * PedidoDAO.actualizarEstado() validará recetas, stock y descontará inventario.
     * Si algo falla, se muestra el error y NO se cambia el estado.
     */
    @FXML
    public void cambiarEstado() {
        Pedido sel = tablePedidos.getSelectionModel().getSelectedItem();
        if (sel == null) {
            alerta("Selecciona un pedido primero.");
            return;
        }

        List<EstadoPedido> estados = pedidoDAO.listarEstados();
        EstadoPedido actual = estados.stream()
                .filter(e -> e.getIdEstadoPedido() == sel.getEstadoPedido().getIdEstadoPedido())
                .findFirst().orElse(estados.isEmpty() ? null : estados.get(0));

        ChoiceDialog<EstadoPedido> dlg = new ChoiceDialog<>(actual, estados);
        dlg.setTitle("Cambiar Estado");
        dlg.setHeaderText("Pedido #" + sel.getIdPedido() + " — " + sel.getNombreCliente());
        dlg.setContentText("Nuevo estado:");
        dlg.showAndWait().ifPresent(nuevo -> {
            if (nuevo.getIdEstadoPedido() == sel.getEstadoPedido().getIdEstadoPedido()) return;

            String error = pedidoDAO.actualizarEstado(sel.getIdPedido(), nuevo.getIdEstadoPedido());
            if (error == null) {
                exito("Estado actualizado a: " + nuevo.getNombreEstado());
                cargarPedidos();
                // Refrescar panel inferior
                pedidoDAO.listarTodos().stream()
                        .filter(p -> p.getIdPedido() == sel.getIdPedido())
                        .findFirst()
                        .ifPresent(this::mostrarDetallePedido);
            } else {
                alerta("No se pudo cambiar el estado:\n\n" + error);
            }
        });
    }

    // ── DETALLE INFERIOR ─────────────────────────────────────────

    private void mostrarDetallePedido(Pedido p) {
        if (lblDetalleIdPedido != null)
            lblDetalleIdPedido.setText("ORD-" + String.format("%04d", p.getIdPedido()));
        if (lblDetalleFecha != null)
            lblDetalleFecha.setText(fmt(p.getFechaEntrega()));
        if (lblNotasProduccion != null)
            lblNotasProduccion.setText(
                    p.getDescripcionPedido() != null && !p.getDescripcionPedido().isBlank()
                            ? p.getDescripcionPedido() : "Sin notas");

        List<DetallePedido> det = pedidoDAO.listarDetalles(p.getIdPedido());
        if (tableDetalleSeleccionado != null)
            tableDetalleSeleccionado.setItems(FXCollections.observableArrayList(det));
    }

    private void limpiarDetallePanelInferior() {
        if (lblDetalleIdPedido  != null) lblDetalleIdPedido.setText("ORD-0000");
        if (lblDetalleFecha     != null) lblDetalleFecha.setText("—");
        if (lblNotasProduccion  != null) lblNotasProduccion.setText("Sin notas");
        if (tableDetalleSeleccionado != null) tableDetalleSeleccionado.getItems().clear();
    }

    // ── AGREGAR ITEM ─────────────────────────────────────────────

    private void abrirDialogoAgregarItem() {
        Dialog<DetallePedido> dlg = new Dialog<>();
        dlg.setTitle("Añadir Producto al Pedido");
        dlg.setHeaderText(null);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        ComboBox<Producto> cbProd = new ComboBox<>(
                FXCollections.observableArrayList(productoDAO.listarTodos()));
        cbProd.setPromptText("Seleccionar producto...");
        cbProd.setMaxWidth(Double.MAX_VALUE);

        TextField txtCant = new TextField("1");
        Label lblPrecio   = new Label("Precio: $0.00");
        Label lblSub      = new Label("Subtotal: $0.00");

        Runnable recalc = () -> {
            Producto prod = cbProd.getValue();
            if (prod == null) return;
            try {
                int c = Integer.parseInt(txtCant.getText().trim());
                if (c < 1) c = 1;
                lblPrecio.setText("Precio: $" + prod.getPrecioVenta().setScale(2, RoundingMode.HALF_UP));
                BigDecimal sub = prod.getPrecioVenta().multiply(BigDecimal.valueOf(c))
                        .setScale(2, RoundingMode.HALF_UP);
                lblSub.setText("Subtotal: $" + sub);
            } catch (NumberFormatException ignored) {}
        };

        cbProd.valueProperty().addListener((o, old, v) -> recalc.run());
        txtCant.textProperty().addListener((o, old, v) -> recalc.run());

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(15));
        grid.addRow(0, new Label("Producto:"), cbProd);
        grid.addRow(1, new Label("Cantidad:"), txtCant);
        grid.addRow(2, new Label(""), lblPrecio);
        grid.addRow(3, new Label(""), lblSub);
        GridPane.setHgrow(cbProd, Priority.ALWAYS);

        dlg.getDialogPane().setContent(grid);
        dlg.getDialogPane().setPrefWidth(420);

        Button okBtn = (Button) dlg.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.setDisable(true);
        cbProd.valueProperty().addListener((o, old, v) -> okBtn.setDisable(v == null));

        dlg.setResultConverter(bt -> {
            if (bt != ButtonType.OK || cbProd.getValue() == null) return null;
            try {
                int cant = Math.max(1, Integer.parseInt(txtCant.getText().trim()));
                DetallePedido dp = new DetallePedido();
                dp.setProducto(cbProd.getValue());
                dp.setCantidad(cant);
                dp.setPrecioUnitario(cbProd.getValue().getPrecioVenta());
                dp.calcularSubtotal();
                return dp;
            } catch (NumberFormatException ex) {
                return null;
            }
        });

        dlg.showAndWait().ifPresent(dp -> {
            detalleForm.stream()
                    .filter(d -> d.getProducto().getIdProducto() == dp.getProducto().getIdProducto())
                    .findFirst()
                    .ifPresentOrElse(existente -> {
                        existente.setCantidad(existente.getCantidad() + dp.getCantidad());
                        existente.calcularSubtotal();
                        tableDetallePedido.refresh();
                    }, () -> detalleForm.add(dp));
            actualizarTotal();
        });
    }

    private void eliminarItemDetalle() {
        DetallePedido sel = tableDetallePedido.getSelectionModel().getSelectedItem();
        if (sel != null) {
            detalleForm.remove(sel);
            actualizarTotal();
        }
    }

    // ── TOTAL ─────────────────────────────────────────────────────

    private void actualizarTotal() {
        BigDecimal total = calcularTotal();
        lblTotalPedido.setText("$" + total.setScale(2, RoundingMode.HALF_UP));
    }

    private BigDecimal calcularTotal() {
        return detalleForm.stream()
                .map(DetallePedido::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ── FORMULARIO ────────────────────────────────────────────────

    private void limpiarFormulario() {
        cbCliente.setValue(null);
        dpFechaPedido.setValue(LocalDate.now());
        dpFechaEntrega.setValue(null);
        cbEstadoPedido.getItems().stream()
                .filter(e -> e.getNombreEstado().equalsIgnoreCase("Pendiente"))
                .findFirst()
                .ifPresent(cbEstadoPedido::setValue);
        txtDescripcionPedido.clear();
        detalleForm.clear();
        actualizarTotal();
        pedidoEditando = null;
        btnGuardarPedido.setText("Crear Nuevo Pedido");
    }

    private void habilitarFormulario(boolean habilitar) {
        cbCliente.setDisable(!habilitar);
        dpFechaPedido.setDisable(!habilitar);
        dpFechaEntrega.setDisable(!habilitar);
        cbEstadoPedido.setDisable(!habilitar);
        txtDescripcionPedido.setDisable(!habilitar);
        btnAgregarItem.setDisable(!habilitar);
        btnGuardarPedido.setDisable(!habilitar);
        tableDetallePedido.setDisable(!habilitar);
    }

    // ── VALIDACIÓN ────────────────────────────────────────────────

    private boolean validarFormulario() {
        List<String> errores = new ArrayList<>();

        if (cbCliente.getValue()      == null) errores.add("• Selecciona un cliente.");
        if (dpFechaPedido.getValue()  == null) errores.add("• Ingresa la fecha del pedido.");
        if (dpFechaEntrega.getValue() == null) errores.add("• Ingresa la fecha de entrega.");
        if (cbEstadoPedido.getValue() == null) errores.add("• Selecciona un estado.");
        if (detalleForm.isEmpty())             errores.add("• Agrega al menos un producto.");

        if (dpFechaPedido.getValue() != null && dpFechaEntrega.getValue() != null
                && dpFechaEntrega.getValue().isBefore(dpFechaPedido.getValue()))
            errores.add("• La fecha de entrega no puede ser anterior a la fecha del pedido.");

        if (!errores.isEmpty()) {
            alerta(String.join("\n", errores));
            return false;
        }
        return true;
    }

    // ── HELPERS ───────────────────────────────────────────────────

    private String fmt(LocalDate d) {
        return d != null ? d.toString() : "";
    }

    private void alerta(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle("Atención");
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void exito(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Éxito");
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}