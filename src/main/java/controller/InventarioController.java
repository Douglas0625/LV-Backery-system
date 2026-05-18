package controller;

import dao.IngredienteDAO;
import dao.MovimientoInventarioDAO;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import model.Ingrediente;
import model.MovimientoInventario;
import utils.ModalUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class InventarioController {

    // ── TABLA INGREDIENTES ────────────────────────────────────────
    @FXML private TextField                          txtBuscar;
    @FXML private TableView<Ingrediente>             tablaIngredientes;
    @FXML private TableColumn<Ingrediente, String>     colNombreIng;
    @FXML private TableColumn<Ingrediente, BigDecimal> colStockIng;
    @FXML private TableColumn<Ingrediente, BigDecimal> colCostoIng;
    @FXML private TableColumn<Ingrediente, BigDecimal> colValorIng;
    @FXML private TableColumn<Ingrediente, String>     colEstadoIng;

    // ── TABLA HISTORIAL ───────────────────────────────────────────
    @FXML private TableView<MovimientoInventario>             tablaMovimientos;
    @FXML private TableColumn<MovimientoInventario, String>     colFechaMov;
    @FXML private TableColumn<MovimientoInventario, String>     colIngredienteMov;
    @FXML private TableColumn<MovimientoInventario, String>     colTipoMov;
    @FXML private TableColumn<MovimientoInventario, BigDecimal> colCantidadMov;
    @FXML private TableColumn<MovimientoInventario, String>     colReferencaMov;
    @FXML private TableColumn<MovimientoInventario, String>     colMotivo;

    // ── CARDS ─────────────────────────────────────────────────────
    @FXML private Label lblTotalIngredientes;
    @FXML private Label lblStockBajoCount;
    @FXML private Label lblValorTotal;

    // ── UMBRAL STOCK BAJO (gramos) ────────────────────────────────
    private static final BigDecimal UMBRAL_STOCK_BAJO = BigDecimal.valueOf(500);

    private final IngredienteDAO         ingredienteDAO = new IngredienteDAO();
    private final MovimientoInventarioDAO movDAO        = new MovimientoInventarioDAO();

    private final ObservableList<Ingrediente>          listaIng = FXCollections.observableArrayList();
    private final ObservableList<MovimientoInventario> listaMov = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        configurarTablaIngredientes();
        configurarTablaMovimientos();
        configurarBusqueda();
        cargarDatos();
    }

    // ── CONFIGURACIÓN TABLAS ──────────────────────────────────────
    private void configurarTablaIngredientes() {
        colNombreIng.setCellValueFactory(new PropertyValueFactory<>("nombreIngrediente"));
        colStockIng.setCellValueFactory(new PropertyValueFactory<>("stockActualGramos"));
        colCostoIng.setCellValueFactory(new PropertyValueFactory<>("costoPorGramo"));

        colValorIng.setCellValueFactory(d -> {
            BigDecimal valor = d.getValue().getStockActualGramos()
                    .multiply(d.getValue().getCostoPorGramo())
                    .setScale(2, RoundingMode.HALF_UP);
            return new SimpleObjectProperty<>(valor);
        });

        colEstadoIng.setCellValueFactory(d -> {
            BigDecimal stock = d.getValue().getStockActualGramos();
            return new SimpleStringProperty(
                    stock.compareTo(UMBRAL_STOCK_BAJO) < 0 ? "⚠ Stock Bajo" : "✓ Normal");
        });

        // Color estado
        colEstadoIng.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); return; }
                setText(v);
                setStyle(v.startsWith("⚠")
                        ? "-fx-text-fill:#C0392B; -fx-font-weight:bold;"
                        : "-fx-text-fill:#27AE60; -fx-font-weight:bold;");
            }
        });

        tablaIngredientes.setItems(listaIng);
    }

    private void configurarTablaMovimientos() {
        colFechaMov.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getFechaMovimiento() != null ? d.getValue().getFechaMovimiento().toString() : ""));
        colIngredienteMov.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNombreIngrediente()));
        colTipoMov.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTipoMovimiento()));
        colCantidadMov.setCellValueFactory(new PropertyValueFactory<>("cantidadGramos"));

        if (colReferencaMov != null)
            colReferencaMov.setCellValueFactory(d -> new SimpleStringProperty(
                    d.getValue().getReferencia() != null ? d.getValue().getReferencia() : ""));
        colMotivo.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getDescripcion() != null ? d.getValue().getDescripcion() : ""));

        // Color tipo
        colTipoMov.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); return; }
                setText(v);
                String color = switch (v) {
                    case "Compra"        -> "#27AE60";
                    case "Ajuste entrada"-> "#2980B9";
                    case "Ajuste salida" -> "#E67E22";
                    case "Merma"         -> "#C0392B";
                    case "Producción"    -> "#8E44AD";
                    default              -> "#4B3832";
                };
                setStyle("-fx-text-fill:" + color + "; -fx-font-weight:bold;");
            }
        });

        tablaMovimientos.setItems(listaMov);
    }

    private void configurarBusqueda() {
        if (txtBuscar == null) return;
        txtBuscar.textProperty().addListener((o, old, v) -> {
            if (v == null || v.isBlank()) listaIng.setAll(ingredienteDAO.listarTodos());
            else listaIng.setAll(ingredienteDAO.buscar(v.trim()));
        });
    }

    // ── CARGA DE DATOS ────────────────────────────────────────────
    private void cargarDatos() {
        listaIng.setAll(ingredienteDAO.listarTodos());
        listaMov.setAll(movDAO.listarTodos());
        actualizarCards();
    }

    private void actualizarCards() {
        int total    = listaIng.size();
        long bajo    = listaIng.stream()
                .filter(i -> i.getStockActualGramos().compareTo(UMBRAL_STOCK_BAJO) < 0).count();
        BigDecimal valor = listaIng.stream()
                .map(i -> i.getStockActualGramos().multiply(i.getCostoPorGramo()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        if (lblTotalIngredientes != null) lblTotalIngredientes.setText(String.valueOf(total));
        if (lblStockBajoCount    != null) lblStockBajoCount.setText(String.valueOf(bajo));
        if (lblValorTotal        != null) lblValorTotal.setText("$" + valor);
    }

    // ── ACCIONES BOTONES ──────────────────────────────────────────

    /** Abre modal para crear ingrediente nuevo */
    @FXML
    public void abrirModalNuevoIngrediente() {
        IngredienteModalController ctrl = ModalUtil.abrir(
                "/views/agregar_ingrediente_inventario_modal.fxml",
                "Nuevo Ingrediente",
                tablaIngredientes.getScene().getWindow());
        if (ctrl != null && ctrl.isGuardado()) {
            Ingrediente nuevo = ctrl.getResultado();
            if (ingredienteDAO.insertar(nuevo)) { exito("Ingrediente creado."); cargarDatos(); }
            else alerta("No se pudo crear. El nombre puede ya existir.");
        }
    }

    /** Abre modal para editar ingrediente seleccionado */
    @FXML
    public void editarIngrediente() {
        Ingrediente sel = tablaIngredientes.getSelectionModel().getSelectedItem();
        if (sel == null) { alerta("Selecciona un ingrediente de la tabla."); return; }

        IngredienteModalController ctrl = ModalUtil.abrir(
                "/views/agregar_ingrediente_inventario_modal.fxml",
                "Editar Ingrediente",
                tablaIngredientes.getScene().getWindow(),
                c -> c.setIngrediente(sel));

        if (ctrl != null && ctrl.isGuardado()) {
            if (ingredienteDAO.actualizar(ctrl.getResultado())) { exito("Ingrediente actualizado."); cargarDatos(); }
            else alerta("No se pudo actualizar.");
        }
    }

    /** Eliminar con confirmación */
    @FXML
    public void eliminarIngrediente() {
        Ingrediente sel = tablaIngredientes.getSelectionModel().getSelectedItem();
        if (sel == null) { alerta("Selecciona un ingrediente."); return; }

        Alert conf = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Eliminar \"" + sel.getNombreIngrediente() + "\"?\nEsta acción no se puede deshacer.",
                ButtonType.YES, ButtonType.NO);
        conf.setTitle("Confirmar eliminación"); conf.setHeaderText(null);
        conf.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> {
            if (ingredienteDAO.eliminar(sel.getIdIngrediente())) { exito("Ingrediente eliminado."); cargarDatos(); }
            else alerta("No se pudo eliminar. Puede estar en uso en recetas o movimientos.");
        });
    }

    /** Abre modal de compra */
    @FXML
    public void abrirModalCompra() {
        CompraModalController ctrl = ModalUtil.abrir(
                "/views/modal_compra.fxml",
                "Registrar Compra",
                tablaIngredientes.getScene().getWindow());
        if (ctrl != null && ctrl.isGuardado()) { exito("Compra registrada. Stock actualizado."); cargarDatos(); }
    }

    /** Abre modal de movimiento (ajuste/merma) */
    @FXML
    public void abrirModalMovimiento() {
        MovimientoModalController ctrl = ModalUtil.abrir(
                "/views/modal_movimiento.fxml",
                "Registrar Movimiento",
                tablaIngredientes.getScene().getWindow());
        if (ctrl != null && ctrl.isGuardado()) { exito("Movimiento registrado."); cargarDatos(); }
    }

    /** Filtro rápido stock bajo */
    @FXML
    public void filtrarStockBajo() {
        listaIng.setAll(ingredienteDAO.listarStockBajo(UMBRAL_STOCK_BAJO));
    }

    @FXML
    public void quitarFiltro() {
        if (txtBuscar != null) txtBuscar.clear();
        cargarDatos();
    }

    // ── HELPERS ───────────────────────────────────────────────────
    private void alerta(String msg) {
        new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK)
        {{ setTitle("Atención"); setHeaderText(null); }}.showAndWait();
    }

    private void exito(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK)
        {{ setTitle("Éxito"); setHeaderText(null); }}.showAndWait();
    }
}
