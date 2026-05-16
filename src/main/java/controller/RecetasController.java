package controller;

import dao.IngredienteDAO;
import dao.ProductoDAO;
import dao.RecetaDAO;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import model.*;

import java.math.BigDecimal;

public class RecetasController {

    // Tabla productos/recetas
    @FXML private TableView<Receta>                  tablaRecetas;
    @FXML private TableColumn<Receta, String>         colProductoRec;
    @FXML private TableColumn<Receta, BigDecimal>     colPrecioRec;
    @FXML private TableColumn<Receta, BigDecimal>     colCostoRec;
    @FXML private TableColumn<Receta, Integer>        colRendimientoRec;

    // Tabla ingredientes de la receta seleccionada
    @FXML private TableView<DetalleReceta>               tablaDetalleReceta;
    @FXML private TableColumn<DetalleReceta, String>     colIngredienteRec;
    @FXML private TableColumn<DetalleReceta, BigDecimal> colGramosRec;
    @FXML private TableColumn<DetalleReceta, BigDecimal> colCostoIngRec;

    // Formulario nueva receta
    @FXML private ComboBox<Producto> cbProductoReceta;
    @FXML private TextField          txtNombreReceta;
    @FXML private TextField          txtRendimiento;

    private final RecetaDAO    recetaDAO    = new RecetaDAO();
    private final ProductoDAO  productoDAO  = new ProductoDAO();
    private final IngredienteDAO ingredienteDAO = new IngredienteDAO();

    private final ObservableList<Receta>         listaRecetas  = FXCollections.observableArrayList();
    private final ObservableList<DetalleReceta>  detalleActual = FXCollections.observableArrayList();
    private Receta recetaSeleccionada;

    @FXML
    public void initialize() {
        configurarTablaRecetas();
        configurarTablaDetalle();
        cargarCombos();
        cargarRecetas();

        if (tablaRecetas != null) {
            tablaRecetas.getSelectionModel().selectedItemProperty().addListener((obs, old, nuevo) -> {
                recetaSeleccionada = nuevo;
                if (nuevo != null) {
                    detalleActual.setAll(nuevo.getDetalles());
                }
            });
        }
    }

    private void configurarTablaRecetas() {
        if (tablaRecetas == null) return;
        colProductoRec.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getProducto() != null ? d.getValue().getProducto().getNombreProducto() : ""));
        colPrecioRec.setCellValueFactory(d -> {
            BigDecimal precio = d.getValue().getProducto() != null ? d.getValue().getProducto().getPrecioVenta() : BigDecimal.ZERO;
            return new javafx.beans.property.SimpleObjectProperty<>(precio);
        });
        colCostoRec.setCellValueFactory(d -> {
            BigDecimal costo = d.getValue().getProducto() != null ? d.getValue().getProducto().getCostoEstimadoUnitario() : BigDecimal.ZERO;
            return new javafx.beans.property.SimpleObjectProperty<>(costo);
        });
        colRendimientoRec.setCellValueFactory(new PropertyValueFactory<>("rendimientoTotal"));
        tablaRecetas.setItems(listaRecetas);
    }

    private void configurarTablaDetalle() {
        if (tablaDetalleReceta == null) return;
        colIngredienteRec.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getIngrediente() != null ? d.getValue().getIngrediente().getNombreIngrediente() : ""));
        colGramosRec.setCellValueFactory(new PropertyValueFactory<>("cantidadGramos"));
        colCostoIngRec.setCellValueFactory(d ->
                new javafx.beans.property.SimpleObjectProperty<>(d.getValue().getCostoEstimado()));
        tablaDetalleReceta.setItems(detalleActual);
    }

    private void cargarCombos() {
        if (cbProductoReceta != null)
            cbProductoReceta.setItems(FXCollections.observableArrayList(productoDAO.listarTodos()));
    }

    private void cargarRecetas() {
        listaRecetas.setAll(recetaDAO.listarTodas());
    }

    @FXML
    public void agregarIngredienteReceta() {
        Dialog<DetalleReceta> dialog = new Dialog<>();
        dialog.setTitle("Agregar Ingrediente a la Receta");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        ComboBox<Ingrediente> cbIng = new ComboBox<>(FXCollections.observableArrayList(ingredienteDAO.listarTodos()));
        cbIng.setPromptText("Seleccionar ingrediente");
        TextField txtGramos = new TextField(); txtGramos.setPromptText("Gramos");

        javafx.scene.layout.VBox box = new javafx.scene.layout.VBox(10,
                new Label("Ingrediente:"), cbIng, new Label("Gramos:"), txtGramos);
        box.setPadding(new javafx.geometry.Insets(10));
        dialog.getDialogPane().setContent(box);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK && cbIng.getValue() != null) {
                try {
                    DetalleReceta dr = new DetalleReceta();
                    dr.setIngrediente(cbIng.getValue());
                    dr.setCantidadGramos(new BigDecimal(txtGramos.getText().trim()));
                    return dr;
                } catch (Exception e) { return null; }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(dr -> detalleActual.add(dr));
    }

    @FXML
    public void guardarReceta() {
        Producto producto   = cbProductoReceta != null ? cbProductoReceta.getValue() : null;
        String   nombre     = txtNombreReceta  != null ? txtNombreReceta.getText().trim() : "";
        String   rendStr    = txtRendimiento   != null ? txtRendimiento.getText().trim()  : "";

        if (producto == null || nombre.isEmpty() || rendStr.isEmpty() || detalleActual.isEmpty()) {
            alerta("Incompleto", "Completa producto, nombre, rendimiento y agrega al menos un ingrediente.");
            return;
        }

        try {
            int rendimiento = Integer.parseInt(rendStr);
            Receta receta = new Receta();
            receta.setProducto(producto);
            receta.setNombreReceta(nombre);
            receta.setRendimientoTotal(rendimiento);
            receta.setDetalles(new java.util.ArrayList<>(detalleActual));

            boolean ok = recetaDAO.insertarReceta(receta);
            if (ok) {
                info("Éxito", "Receta guardada correctamente.");
                limpiarFormulario();
                cargarRecetas();
            } else {
                alerta("Error", "No se pudo guardar. Este producto ya puede tener una receta.");
            }
        } catch (NumberFormatException e) {
            alerta("Formato inválido", "El rendimiento debe ser un número entero.");
        }
    }

    @FXML
    public void eliminarReceta() {
        if (recetaSeleccionada == null) { alerta("Sin selección", "Selecciona una receta de la tabla."); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Eliminar la receta \"" + recetaSeleccionada.getNombreReceta() + "\"?",
                ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                if (recetaDAO.eliminarReceta(recetaSeleccionada.getIdReceta())) {
                    info("Éxito", "Receta eliminada.");
                    detalleActual.clear();
                    cargarRecetas();
                } else {
                    alerta("Error", "No se pudo eliminar la receta.");
                }
            }
        });
    }

    private void limpiarFormulario() {
        if (cbProductoReceta != null) cbProductoReceta.setValue(null);
        if (txtNombreReceta  != null) txtNombreReceta.clear();
        if (txtRendimiento   != null) txtRendimiento.clear();
        detalleActual.clear();
    }

    private void alerta(String t, String m) {
        new Alert(Alert.AlertType.ERROR, m, ButtonType.OK) {{ setTitle(t); setHeaderText(null); }}.showAndWait();
    }

    private void info(String t, String m) {
        new Alert(Alert.AlertType.INFORMATION, m, ButtonType.OK) {{ setTitle(t); setHeaderText(null); }}.showAndWait();
    }
}
