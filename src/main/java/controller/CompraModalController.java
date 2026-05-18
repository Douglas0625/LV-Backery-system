package controller;

import dao.CompraDAO;
import dao.IngredienteDAO;
import dao.ProveedorDAO;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import model.Compra;
import model.DetalleCompra;
import model.Ingrediente;
import model.Proveedor;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class CompraModalController {

    @FXML private ComboBox<Proveedor>   cbProveedor;
    @FXML private ComboBox<Ingrediente> cbIngrediente;
    @FXML private TextField             txtGramos;
    @FXML private TextField             txtCostoGramo;
    @FXML private Label                 lblSubtotalLinea;

    @FXML private TableView<DetalleCompra>               tablaDetalle;
    @FXML private TableColumn<DetalleCompra, String>     colIngredienteCompra;
    @FXML private TableColumn<DetalleCompra, BigDecimal> colGramosCompra;
    @FXML private TableColumn<DetalleCompra, BigDecimal> colCostoCompra;
    @FXML private TableColumn<DetalleCompra, BigDecimal> colSubtotalCompra;
    @FXML private Label                                  lblTotalCompra;

    private final IngredienteDAO ingredienteDAO = new IngredienteDAO();
    private final ProveedorDAO   proveedorDAO   = new ProveedorDAO();
    private final CompraDAO      compraDAO      = new CompraDAO();

    private final ObservableList<DetalleCompra> detalles = FXCollections.observableArrayList();
    private boolean guardado = false;

    @FXML
    public void initialize() {
        cbProveedor.setItems(FXCollections.observableArrayList(proveedorDAO.listarTodos()));
        cbIngrediente.setItems(FXCollections.observableArrayList(ingredienteDAO.listarTodos()));

        // Columnas tabla
        colIngredienteCompra.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNombreIngrediente()));
        colGramosCompra.setCellValueFactory(new PropertyValueFactory<>("cantidadGramos"));
        colCostoCompra.setCellValueFactory(new PropertyValueFactory<>("costoUnitarioGramo"));
        colSubtotalCompra.setCellValueFactory(new PropertyValueFactory<>("subtotal"));
        tablaDetalle.setItems(detalles);

        // Click derecho eliminar línea
        MenuItem mnuEliminar = new MenuItem("Eliminar línea");
        mnuEliminar.setOnAction(e -> {
            DetalleCompra sel = tablaDetalle.getSelectionModel().getSelectedItem();
            if (sel != null) { detalles.remove(sel); actualizarTotal(); }
        });
        tablaDetalle.setContextMenu(new ContextMenu(mnuEliminar));

        // Subtotal en tiempo real
        Runnable recalc = () -> {
            try {
                BigDecimal g = new BigDecimal(txtGramos.getText().trim());
                BigDecimal c = new BigDecimal(txtCostoGramo.getText().trim());
                lblSubtotalLinea.setText("$" + g.multiply(c).setScale(2, RoundingMode.HALF_UP));
            } catch (Exception ignored) { lblSubtotalLinea.setText("$0.00"); }
        };
        txtGramos.textProperty().addListener((o, old, v) -> recalc.run());
        txtCostoGramo.textProperty().addListener((o, old, v) -> recalc.run());
    }

    @FXML
    public void agregarLineaCompra() {
        Ingrediente ing = cbIngrediente.getValue();
        if (ing == null) { alerta("Selecciona un ingrediente."); return; }
        try {
            BigDecimal gramos = new BigDecimal(txtGramos.getText().trim());
            BigDecimal costo  = new BigDecimal(txtCostoGramo.getText().trim());
            if (gramos.compareTo(BigDecimal.ZERO) <= 0) { alerta("Los gramos deben ser mayores a 0."); return; }

            DetalleCompra d = new DetalleCompra();
            d.setIdIngrediente(ing.getIdIngrediente());
            d.setNombreIngrediente(ing.getNombreIngrediente());
            d.setCantidadGramos(gramos);
            d.setCostoUnitarioGramo(costo);
            d.calcularSubtotal();
            detalles.add(d);
            actualizarTotal();
            limpiarLinea();
        } catch (NumberFormatException e) { alerta("Ingresa valores numéricos válidos."); }
    }

    @FXML
    public void guardarCompra() {
        if (detalles.isEmpty()) { alerta("Agrega al menos un ingrediente a la compra."); return; }

        Compra compra = new Compra();
        Proveedor prov = cbProveedor.getValue();
        if (prov != null) {
            compra.setIdProveedor(prov.getIdProveedor());
            compra.setReferenciaProveedor(prov.getNombreProveedor());
        } else {
            compra.setReferenciaProveedor("Compra directa");
        }
        compra.setTotalCompra(calcularTotal());
        compra.setDetalles(new java.util.ArrayList<>(detalles));

        int id = compraDAO.registrarCompra(compra);
        if (id > 0) { guardado = true; cerrar(); }
        else alerta("No se pudo registrar la compra.");
    }

    @FXML
    public void cancelar() { cerrar(); }

    private void actualizarTotal() {
        lblTotalCompra.setText("$" + calcularTotal().setScale(2, RoundingMode.HALF_UP));
    }

    private BigDecimal calcularTotal() {
        return detalles.stream().map(DetalleCompra::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void limpiarLinea() {
        cbIngrediente.setValue(null);
        txtGramos.clear();
        txtCostoGramo.clear();
        lblSubtotalLinea.setText("$0.00");
    }

    private void cerrar() { ((Stage) cbProveedor.getScene().getWindow()).close(); }
    public boolean isGuardado() { return guardado; }

    private void alerta(String msg) {
        new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK) {{ setHeaderText(null); }}.showAndWait();
    }
}
