package controller;

import dao.IngredienteDAO;
import dao.MovimientoInventarioDAO;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import model.Ingrediente;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MovimientoModalController {

    @FXML private ComboBox<Ingrediente> cbIngrediente;
    @FXML private ComboBox<String>      cbTipoMovimiento;
    @FXML private TextField             txtCantidad;
    @FXML private Label                 lblStockActual;
    @FXML private Label                 lblStockResultante;
    @FXML private TextArea              txtDescripcion;

    private final IngredienteDAO         ingredienteDAO = new IngredienteDAO();
    private final MovimientoInventarioDAO movDAO        = new MovimientoInventarioDAO();

    private boolean guardado = false;

    // Tipos que AUMENTAN stock
    private static final java.util.Set<String> ENTRADAS = java.util.Set.of("Compra", "Ajuste entrada");

    @FXML
    public void initialize() {
        cbIngrediente.setItems(FXCollections.observableArrayList(ingredienteDAO.listarTodos()));
        cbTipoMovimiento.setItems(FXCollections.observableArrayList(
                "Ajuste entrada", "Ajuste salida", "Merma", "Producción"));

        // Actualizar info de stock al cambiar ingrediente o cantidad/tipo
        cbIngrediente.valueProperty().addListener((o, old, v) -> actualizarInfoStock());
        cbTipoMovimiento.valueProperty().addListener((o, old, v) -> actualizarInfoStock());
        txtCantidad.textProperty().addListener((o, old, v) -> actualizarInfoStock());
    }

    private void actualizarInfoStock() {
        Ingrediente ing = cbIngrediente.getValue();
        if (ing == null) { lblStockActual.setText("—"); lblStockResultante.setText("—"); return; }

        BigDecimal actual = ing.getStockActualGramos();
        lblStockActual.setText(actual.setScale(2, RoundingMode.HALF_UP) + " g");

        try {
            BigDecimal cant = new BigDecimal(txtCantidad.getText().trim());
            String tipo = cbTipoMovimiento.getValue();
            if (tipo == null) return;
            BigDecimal resultado = ENTRADAS.contains(tipo) ? actual.add(cant) : actual.subtract(cant);
            lblStockResultante.setText(resultado.setScale(2, RoundingMode.HALF_UP) + " g");
            lblStockResultante.setStyle(resultado.compareTo(BigDecimal.ZERO) < 0
                    ? "-fx-text-fill:#C0392B; -fx-font-size:14px; -fx-font-weight:bold;"
                    : "-fx-text-fill:#9A5B39; -fx-font-size:14px; -fx-font-weight:bold;");
        } catch (Exception ignored) { lblStockResultante.setText("—"); }
    }

    @FXML
    public void registrar() {
        Ingrediente ing = cbIngrediente.getValue();
        String tipo     = cbTipoMovimiento.getValue();
        String cantStr  = txtCantidad.getText().trim();
        String desc     = txtDescripcion.getText().trim();

        if (ing == null || tipo == null || cantStr.isEmpty()) {
            alerta("Completa ingrediente, tipo y cantidad.");
            return;
        }

        BigDecimal cantidad;
        try { cantidad = new BigDecimal(cantStr); }
        catch (NumberFormatException e) { alerta("La cantidad debe ser un número."); return; }

        if (cantidad.compareTo(BigDecimal.ZERO) <= 0) { alerta("La cantidad debe ser mayor a 0."); return; }

        BigDecimal delta = ENTRADAS.contains(tipo) ? cantidad : cantidad.negate();

        // Validar que no quede negativo
        if (delta.compareTo(BigDecimal.ZERO) < 0) {
            BigDecimal resultante = ing.getStockActualGramos().add(delta);
            if (resultante.compareTo(BigDecimal.ZERO) < 0) {
                alerta("Stock insuficiente. Stock actual: " + ing.getStockActualGramos() + "g");
                return;
            }
        }

        boolean ok = movDAO.registrarMovimiento(
                ing.getIdIngrediente(), tipo, cantidad,
                desc.isEmpty() ? tipo : desc, null, delta);

        if (ok) { guardado = true; cerrar(); }
        else alerta("No se pudo registrar el movimiento.");
    }

    @FXML
    public void cancelar() { cerrar(); }

    private void cerrar() { ((Stage) cbIngrediente.getScene().getWindow()).close(); }
    public boolean isGuardado() { return guardado; }

    private void alerta(String msg) {
        new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK) {{ setHeaderText(null); }}.showAndWait();
    }
}
