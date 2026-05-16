package controller;

import dao.IngredienteDAO;
import dao.MovimientoInventarioDAO;
import dao.ProveedorDAO;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import model.Ingrediente;
import model.MovimientoInventario;
import model.Proveedor;

import java.math.BigDecimal;

public class InventarioController {

    // Tabla ingredientes
    @FXML private TableView<Ingrediente> tablaIngredientes;
    @FXML private TableColumn<Ingrediente, String>     colNombreIng;
    @FXML private TableColumn<Ingrediente, BigDecimal> colStockIng;
    @FXML private TableColumn<Ingrediente, BigDecimal> colCostoIng;
    @FXML private TableColumn<Ingrediente, String>     colEstadoIng;

    // Tabla movimientos
    @FXML private TableView<MovimientoInventario> tablaMovimientos;
    @FXML private TableColumn<MovimientoInventario, String>     colFechaMov;
    @FXML private TableColumn<MovimientoInventario, String>     colIngredienteMov;
    @FXML private TableColumn<MovimientoInventario, String>     colTipoMov;
    @FXML private TableColumn<MovimientoInventario, BigDecimal> colCantidadMov;
    @FXML private TableColumn<MovimientoInventario, String>     colMotivo;

    // Formulario compra
    @FXML private ComboBox<Proveedor>   cbProveedor;
    @FXML private ComboBox<Ingrediente> cbIngredienteCompra;
    @FXML private TextField             txtCantidadCompra;
    @FXML private TextField             txtCostoGramo;

    // Formulario ajuste
    @FXML private ComboBox<Ingrediente> cbIngredienteAjuste;
    @FXML private ComboBox<String>      cbTipoAjuste;
    @FXML private TextField             txtCantidadAjuste;
    @FXML private TextArea              txtMotivoAjuste;

    private final IngredienteDAO ingredienteDAO         = new IngredienteDAO();
    private final MovimientoInventarioDAO movimientoDAO = new MovimientoInventarioDAO();
    private final ProveedorDAO proveedorDAO             = new ProveedorDAO();

    private final ObservableList<Ingrediente>         listaIngredientes  = FXCollections.observableArrayList();
    private final ObservableList<MovimientoInventario> listaMovimientos  = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        configurarTablaIngredientes();
        configurarTablaMovimientos();
        cargarCombos();
        cargarDatos();
    }

    private void configurarTablaIngredientes() {
        if (tablaIngredientes == null) return;
        colNombreIng.setCellValueFactory(new PropertyValueFactory<>("nombreIngrediente"));
        colStockIng.setCellValueFactory(new PropertyValueFactory<>("stockActualGramos"));
        colCostoIng.setCellValueFactory(new PropertyValueFactory<>("costoPorGramo"));
        colEstadoIng.setCellValueFactory(data -> {
            BigDecimal stock = data.getValue().getStockActualGramos();
            String estado = stock.compareTo(BigDecimal.valueOf(500)) < 0 ? "⚠ Stock Bajo" : "✓ Normal";
            return new SimpleStringProperty(estado);
        });
        tablaIngredientes.setItems(listaIngredientes);
    }

    private void configurarTablaMovimientos() {
        if (tablaMovimientos == null) return;
        colFechaMov.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getFechaMovimiento() != null
                        ? data.getValue().getFechaMovimiento().toString() : ""));
        colIngredienteMov.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getNombreIngrediente()));
        colTipoMov.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getTipoMovimiento()));
        colCantidadMov.setCellValueFactory(new PropertyValueFactory<>("cantidadGramos"));
        colMotivo.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getDescripcion() != null
                        ? data.getValue().getDescripcion() : ""));
        tablaMovimientos.setItems(listaMovimientos);
    }

    private void cargarCombos() {
        ObservableList<Ingrediente> ingredientes = FXCollections.observableArrayList(ingredienteDAO.listarTodos());
        ObservableList<Proveedor>   proveedores  = FXCollections.observableArrayList(proveedorDAO.listarTodos());

        if (cbProveedor != null)          cbProveedor.setItems(proveedores);
        if (cbIngredienteCompra != null)  cbIngredienteCompra.setItems(ingredientes);
        if (cbIngredienteAjuste != null)  cbIngredienteAjuste.setItems(FXCollections.observableArrayList(ingredienteDAO.listarTodos()));
        if (cbTipoAjuste != null)
            cbTipoAjuste.setItems(FXCollections.observableArrayList("Ajuste entrada", "Ajuste salida", "Merma"));
    }

    private void cargarDatos() {
        listaIngredientes.setAll(ingredienteDAO.listarTodos());
        listaMovimientos.setAll(movimientoDAO.listarTodos());
    }

    @FXML
    public void registrarCompra() {
        Ingrediente ingrediente = cbIngredienteCompra != null ? cbIngredienteCompra.getValue() : null;
        String cantStr  = txtCantidadCompra != null ? txtCantidadCompra.getText().trim() : "";
        String costoStr = txtCostoGramo     != null ? txtCostoGramo.getText().trim()     : "";

        if (ingrediente == null || cantStr.isEmpty() || costoStr.isEmpty()) {
            alerta("Campos vacíos", "Selecciona ingrediente, cantidad y costo por gramo.");
            return;
        }

        try {
            BigDecimal cantidad = new BigDecimal(cantStr);
            BigDecimal costo    = new BigDecimal(costoStr);

            Proveedor prov = cbProveedor != null ? cbProveedor.getValue() : null;
            String ref = prov != null ? "Proveedor: " + prov.getNombreProveedor() : "Compra directa";

            boolean ok = movimientoDAO.registrarMovimiento(
                    ingrediente.getIdIngrediente(), "Compra",
                    cantidad, "Compra de ingrediente", ref, cantidad);

            if (ok) {
                // Actualizar también el costo por gramo
                ingrediente.setCostoPorGramo(costo);
                ingredienteDAO.actualizar(ingrediente);
                info("Éxito", "Compra registrada. Stock actualizado.");
                limpiarCompra();
                cargarDatos();
            } else {
                alerta("Error", "No se pudo registrar la compra.");
            }
        } catch (NumberFormatException e) {
            alerta("Formato inválido", "Ingresa valores numéricos válidos.");
        }
    }

    @FXML
    public void registrarAjuste() {
        Ingrediente ingrediente = cbIngredienteAjuste != null ? cbIngredienteAjuste.getValue() : null;
        String tipo    = cbTipoAjuste       != null ? cbTipoAjuste.getValue()              : null;
        String cantStr = txtCantidadAjuste  != null ? txtCantidadAjuste.getText().trim()   : "";
        String motivo  = txtMotivoAjuste    != null ? txtMotivoAjuste.getText().trim()      : "";

        if (ingrediente == null || tipo == null || cantStr.isEmpty()) {
            alerta("Campos vacíos", "Completa ingrediente, tipo y cantidad.");
            return;
        }

        try {
            BigDecimal cantidad = new BigDecimal(cantStr);
            BigDecimal delta = tipo.equals("Ajuste entrada") ? cantidad : cantidad.negate();

            boolean ok = movimientoDAO.registrarMovimiento(
                    ingrediente.getIdIngrediente(), tipo, cantidad, motivo, null, delta);

            if (ok) {
                info("Éxito", "Ajuste registrado correctamente.");
                limpiarAjuste();
                cargarDatos();
            } else {
                alerta("Error", "No se pudo registrar el ajuste.");
            }
        } catch (NumberFormatException e) {
            alerta("Formato inválido", "Ingresa una cantidad numérica válida.");
        }
    }

    @FXML
    public void agregarIngrediente() {
        // Diálogo simple para añadir ingrediente nuevo
        Dialog<Ingrediente> dialog = new Dialog<>();
        dialog.setTitle("Nuevo Ingrediente");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField nombre = new TextField(); nombre.setPromptText("Nombre del ingrediente");
        TextField costo  = new TextField(); costo.setPromptText("Costo por gramo (ej: 0.0025)");

        javafx.scene.layout.VBox box = new javafx.scene.layout.VBox(10, new Label("Nombre:"), nombre, new Label("Costo/gramo:"), costo);
        box.setPadding(new javafx.geometry.Insets(10));
        dialog.getDialogPane().setContent(box);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                Ingrediente i = new Ingrediente();
                i.setNombreIngrediente(nombre.getText().trim());
                i.setStockActualGramos(BigDecimal.ZERO);
                try { i.setCostoPorGramo(new BigDecimal(costo.getText().trim())); }
                catch (Exception ex) { i.setCostoPorGramo(BigDecimal.ZERO); }
                return i;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(i -> {
            if (!i.getNombreIngrediente().isEmpty()) {
                if (ingredienteDAO.insertar(i)) {
                    info("Éxito", "Ingrediente agregado.");
                    cargarDatos();
                    cargarCombos();
                } else {
                    alerta("Error", "No se pudo agregar el ingrediente. Puede que ya exista.");
                }
            }
        });
    }

    private void limpiarCompra() {
        if (cbIngredienteCompra != null) cbIngredienteCompra.setValue(null);
        if (txtCantidadCompra   != null) txtCantidadCompra.clear();
        if (txtCostoGramo       != null) txtCostoGramo.clear();
        if (cbProveedor         != null) cbProveedor.setValue(null);
    }

    private void limpiarAjuste() {
        if (cbIngredienteAjuste != null) cbIngredienteAjuste.setValue(null);
        if (cbTipoAjuste        != null) cbTipoAjuste.setValue(null);
        if (txtCantidadAjuste   != null) txtCantidadAjuste.clear();
        if (txtMotivoAjuste     != null) txtMotivoAjuste.clear();
    }

    private void alerta(String titulo, String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK) {{ setTitle(titulo); setHeaderText(null); }}.showAndWait();
    }

    private void info(String titulo, String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK) {{ setTitle(titulo); setHeaderText(null); }}.showAndWait();
    }
}
