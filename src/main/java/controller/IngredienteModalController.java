package controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import model.Ingrediente;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class IngredienteModalController {

    @FXML private Label     lblTitulo;
    @FXML private TextField txtNombre;
    @FXML private TextField txtStock;
    @FXML private TextField txtCostoGramo;
    @FXML private Label     lblValorInventario;
    @FXML private Button    btnGuardar;

    private Ingrediente ingredienteEditando = null;
    private boolean guardado = false;
    private Ingrediente resultado = null;

    @FXML
    public void initialize() {
        // Recalcular valor inventario en tiempo real
        Runnable recalc = () -> {
            try {
                BigDecimal stock = new BigDecimal(txtStock.getText().trim());
                BigDecimal costo = new BigDecimal(txtCostoGramo.getText().trim());
                BigDecimal valor = stock.multiply(costo).setScale(2, RoundingMode.HALF_UP);
                lblValorInventario.setText("$" + valor);
            } catch (Exception ignored) {
                lblValorInventario.setText("$0.00");
            }
        };
        txtStock.textProperty().addListener((o, old, v) -> recalc.run());
        txtCostoGramo.textProperty().addListener((o, old, v) -> recalc.run());
    }

    /** Llamar desde el controller padre para modo editar. */
    public void setIngrediente(Ingrediente ing) {
        this.ingredienteEditando = ing;
        lblTitulo.setText("Editar Ingrediente");
        txtNombre.setText(ing.getNombreIngrediente());
        txtStock.setText(ing.getStockActualGramos().toPlainString());
        txtCostoGramo.setText(ing.getCostoPorGramo().toPlainString());
        // En edición el stock no se puede cambiar directamente (usar movimientos)
        txtStock.setEditable(false);
        txtStock.setStyle("-fx-background-color:#E8E8E8; -fx-border-color:#E5E1D8;");
    }

    @FXML
    public void guardar() {
        String nombre   = txtNombre.getText().trim();
        String stockStr = txtStock.getText().trim();
        String costoStr = txtCostoGramo.getText().trim();

        if (nombre.isEmpty()) { alerta("El nombre del ingrediente es obligatorio."); return; }

        BigDecimal stock, costo;
        try {
            stock = new BigDecimal(stockStr.isEmpty() ? "0" : stockStr);
            costo = new BigDecimal(costoStr.isEmpty() ? "0" : costoStr);
        } catch (NumberFormatException e) {
            alerta("Los valores de stock y costo deben ser numéricos.");
            return;
        }

        resultado = new Ingrediente();
        resultado.setNombreIngrediente(nombre);
        resultado.setStockActualGramos(stock);
        resultado.setCostoPorGramo(costo);
        if (ingredienteEditando != null) resultado.setIdIngrediente(ingredienteEditando.getIdIngrediente());

        guardado = true;
        cerrar();
    }

    @FXML
    public void cancelar() {
        guardado = false;
        cerrar();
    }

    private void cerrar() {
        ((Stage) txtNombre.getScene().getWindow()).close();
    }

    public boolean isGuardado() { return guardado; }
    public Ingrediente getResultado() { return resultado; }

    private void alerta(String msg) {
        new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK)
        {{ setHeaderText(null); }}.showAndWait();
    }
}
