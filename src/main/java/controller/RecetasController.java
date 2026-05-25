package controller;

import dao.IngredienteDAO;
import dao.ProductoDAO;
import dao.RecetaDAO;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import model.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Optional;

public class RecetasController {

    // ── TABLA PRODUCTOS / RECETAS ──────────────────────────────
    @FXML private TableView<Receta>                tablaRecetas;
    @FXML private TableColumn<Receta, String>      colProductoRec;
    @FXML private TableColumn<Receta, BigDecimal>  colPrecioRec;
    @FXML private TableColumn<Receta, BigDecimal>  colCostoRec;
    @FXML private TableColumn<Receta, Integer>     colRendimientoRec;
    @FXML private TableColumn<Receta, Void>        colAccionRec;

    // ── PANEL DERECHO — CAMPOS PRODUCTO ───────────────────────
    @FXML private TextField  txtNombreProducto;
    @FXML private TextArea   txtDescripcion;
    @FXML private TextField  txtPrecioVenta;
    @FXML private TextField  txtUnidades;

    // ── PANEL DERECHO — CAMPOS RECETA ─────────────────────────
    @FXML private TextField  txtNombreReceta;
    @FXML private TextField  txtRendimiento;

    // ── TABLA INGREDIENTES (panel derecho) ────────────────────
    @FXML private TableView<DetalleReceta>               tablaDetalleReceta;
    @FXML private TableColumn<DetalleReceta, String>     colIngredienteRec;
    @FXML private TableColumn<DetalleReceta, BigDecimal> colGramosRec;
    @FXML private TableColumn<DetalleReceta, BigDecimal> colCostoIngRec;
    @FXML private TableColumn<DetalleReceta, Void>       colAccionIng;

    // ── COSTO ESTIMADO ─────────────────────────────────────────
    @FXML private Label lblCostoEstimado;

    // ── BÚSQUEDA ───────────────────────────────────────────────
    @FXML private TextField txtBuscar;

    // ── DAOs ───────────────────────────────────────────────────
    private final RecetaDAO     recetaDAO     = new RecetaDAO();
    private final ProductoDAO   productoDAO   = new ProductoDAO();
    private final IngredienteDAO ingredienteDAO = new IngredienteDAO();

    // ── ESTADO ────────────────────────────────────────────────
    private final ObservableList<Receta>        listaRecetas  = FXCollections.observableArrayList();
    private final ObservableList<DetalleReceta> detalleActual = FXCollections.observableArrayList();

    /** null = modo creación; no-null = modo edición */
    private Receta recetaEnEdicion = null;

    // ── INICIALIZACIÓN ─────────────────────────────────────────

    @FXML
    public void initialize() {
        configurarTablaRecetas();
        configurarTablaDetalle();
        configurarBusqueda();
        actualizarCostoEnVivo();
        cargarRecetas();
    }

    // ── CONFIGURACIÓN DE TABLAS ────────────────────────────────

    private void configurarTablaRecetas() {
        if (tablaRecetas == null) return;

        colProductoRec.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getProducto() != null
                        ? d.getValue().getProducto().getNombreProducto() : ""));

        colPrecioRec.setCellValueFactory(d -> new SimpleObjectProperty<>(
                d.getValue().getProducto() != null
                        ? d.getValue().getProducto().getPrecioVenta() : BigDecimal.ZERO));

        colCostoRec.setCellValueFactory(d -> new SimpleObjectProperty<>(
                d.getValue().getProducto() != null
                        ? d.getValue().getProducto().getCostoEstimadoUnitario() : BigDecimal.ZERO));

        colRendimientoRec.setCellValueFactory(new PropertyValueFactory<>("rendimientoTotal"));

        // Columna de acciones: Editar + Eliminar
        if (colAccionRec != null) {
            colAccionRec.setCellFactory(col -> new TableCell<>() {
                private final Button btnEditar   = new Button("✏");
                private final Button btnEliminar = new Button("🗑");
                private final HBox   box         = new HBox(6, btnEditar, btnEliminar);

                {
                    box.setAlignment(Pos.CENTER);
                    btnEditar.setStyle("""
                        -fx-background-color: #D8B08C;
                        -fx-text-fill: white;
                        -fx-background-radius: 5;
                        -fx-cursor: hand;
                        -fx-font-size: 12;
                    """);
                    btnEliminar.setStyle("""
                        -fx-background-color: #D64545;
                        -fx-text-fill: white;
                        -fx-background-radius: 5;
                        -fx-cursor: hand;
                        -fx-font-size: 12;
                    """);
                    btnEditar.setOnAction(e -> {
                        Receta r = getTableView().getItems().get(getIndex());
                        cargarEnFormulario(r);
                    });
                    btnEliminar.setOnAction(e -> {
                        Receta r = getTableView().getItems().get(getIndex());
                        confirmarEliminar(r);
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : box);
                }
            });
        }

        tablaRecetas.setItems(listaRecetas);
    }

    private void configurarTablaDetalle() {
        if (tablaDetalleReceta == null) return;

        colIngredienteRec.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getIngrediente() != null
                        ? d.getValue().getIngrediente().getNombreIngrediente() : ""));

        colGramosRec.setCellValueFactory(new PropertyValueFactory<>("cantidadGramos"));

        colCostoIngRec.setCellValueFactory(d ->
                new SimpleObjectProperty<>(d.getValue().getCostoEstimado()));

        // Columna quitar ingrediente
        if (colAccionIng != null) {
            colAccionIng.setCellFactory(col -> new TableCell<>() {
                private final Button btn = new Button("✕");
                {
                    btn.setStyle("""
                        -fx-background-color: #D64545;
                        -fx-text-fill: white;
                        -fx-background-radius: 5;
                        -fx-cursor: hand;
                        -fx-font-size: 11;
                    """);
                    btn.setOnAction(e -> {
                        DetalleReceta dr = getTableView().getItems().get(getIndex());
                        detalleActual.remove(dr);
                        recalcularCosto();
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : btn);
                }
            });
        }

        tablaDetalleReceta.setItems(detalleActual);
    }

    private void configurarBusqueda() {
        if (txtBuscar == null) return;
        txtBuscar.textProperty().addListener((obs, old, val) -> {
            String filtro = val == null ? "" : val.toLowerCase();
            ObservableList<Receta> filtradas = FXCollections.observableArrayList(
                    recetaDAO.listarTodas().stream()
                            .filter(r -> r.getProducto() != null &&
                                    r.getProducto().getNombreProducto().toLowerCase().contains(filtro))
                            .toList());
            listaRecetas.setAll(filtradas);
        });
    }

    // ── CARGA DE DATOS ─────────────────────────────────────────

    private void cargarRecetas() {
        listaRecetas.setAll(recetaDAO.listarTodas());
    }

    // ── FORMULARIO — CARGAR PARA EDITAR ───────────────────────

    private void cargarEnFormulario(Receta r) {
        recetaEnEdicion = r;
        Producto p = r.getProducto();
        if (p != null) {
            if (txtNombreProducto != null) txtNombreProducto.setText(p.getNombreProducto());
            if (txtDescripcion    != null) txtDescripcion.setText(p.getDescripcion() != null ? p.getDescripcion() : "");
            if (txtPrecioVenta    != null) txtPrecioVenta.setText(p.getPrecioVenta().toPlainString());
            if (txtUnidades       != null) txtUnidades.setText(String.valueOf(p.getUnidadesPorPresentacion()));
        }
        if (txtNombreReceta != null) txtNombreReceta.setText(r.getNombreReceta());
        if (txtRendimiento  != null) txtRendimiento.setText(String.valueOf(r.getRendimientoTotal()));
        detalleActual.setAll(r.getDetalles());
        recalcularCosto();
    }

    // ── BOTÓN + INGREDIENTE ────────────────────────────────────

    @FXML
    public void agregarIngredienteReceta() {
        Dialog<DetalleReceta> dialog = new Dialog<>();
        dialog.setTitle("Agregar Ingrediente");
        dialog.setHeaderText(null);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        ComboBox<Ingrediente> cbIng = new ComboBox<>(
                FXCollections.observableArrayList(ingredienteDAO.listarTodos()));
        cbIng.setPromptText("Seleccionar ingrediente");
        cbIng.setPrefWidth(280);

        TextField txtGramos = new TextField();
        txtGramos.setPromptText("Cantidad en gramos");

        Label lblError = new Label();
        lblError.setStyle("-fx-text-fill: #D64545;");

        VBox box = new VBox(10,
                new Label("Ingrediente:"), cbIng,
                new Label("Gramos:"), txtGramos,
                lblError);
        box.setPadding(new Insets(15));
        box.setPrefWidth(320);
        dialog.getDialogPane().setContent(box);

        // Validar al hacer OK
        Button okBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            if (cbIng.getValue() == null) {
                lblError.setText("Selecciona un ingrediente.");
                ev.consume();
                return;
            }
            try {
                new BigDecimal(txtGramos.getText().trim());
            } catch (Exception ex) {
                lblError.setText("Ingresa una cantidad válida.");
                ev.consume();
            }
        });

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    // Verificar que no esté duplicado
                    Ingrediente seleccionado = cbIng.getValue();
                    boolean duplicado = detalleActual.stream()
                            .anyMatch(d -> d.getIngrediente().getIdIngrediente()
                                    == seleccionado.getIdIngrediente());
                    if (duplicado) {
                        alerta("Duplicado", "Este ingrediente ya está en la receta.");
                        return null;
                    }
                    DetalleReceta dr = new DetalleReceta();
                    dr.setIngrediente(seleccionado);
                    dr.setCantidadGramos(new BigDecimal(txtGramos.getText().trim()));
                    return dr;
                } catch (Exception e) { return null; }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(dr -> {
            detalleActual.add(dr);
            recalcularCosto();
        });
    }

    // ── BOTÓN GUARDAR ─────────────────────────────────────────

    @FXML
    public void guardarReceta() {
        // Recoger campos
        String nombre     = get(txtNombreProducto);
        String descripcion = get(txtDescripcion);
        String precioStr  = get(txtPrecioVenta);
        String unidStr    = get(txtUnidades);
        String nomReceta  = get(txtNombreReceta);
        String rendStr    = get(txtRendimiento);

        if (nombre.isEmpty() || precioStr.isEmpty() || unidStr.isEmpty()
                || nomReceta.isEmpty() || rendStr.isEmpty()) {
            alerta("Campos incompletos", "Completa todos los campos antes de guardar.");
            return;
        }
        if (detalleActual.isEmpty()) {
            alerta("Sin ingredientes", "Agrega al menos un ingrediente a la receta.");
            return;
        }

        BigDecimal precio;
        int unidades, rendimiento;
        try {
            precio      = new BigDecimal(precioStr);
            unidades    = Integer.parseInt(unidStr);
            rendimiento = Integer.parseInt(rendStr);
            if (precio.compareTo(BigDecimal.ZERO) < 0 || unidades <= 0 || rendimiento <= 0)
                throw new NumberFormatException();
        } catch (NumberFormatException e) {
            alerta("Formato inválido", "Precio, unidades y rendimiento deben ser números positivos.");
            return;
        }

        // Calcular costo estimado
        BigDecimal costoTotal = detalleActual.stream()
                .map(DetalleReceta::getCostoEstimado)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal costoUnit = costoTotal.divide(
                BigDecimal.valueOf(rendimiento), 4, RoundingMode.HALF_UP);

        if (recetaEnEdicion == null) {
            // ── MODO CREACIÓN ──────────────────────────────────
            Producto producto = new Producto();
            producto.setNombreProducto(nombre);
            producto.setDescripcion(descripcion);
            producto.setPrecioVenta(precio);
            producto.setCostoEstimadoUnitario(costoUnit);
            producto.setUnidadesPorPresentacion(unidades);

            Receta receta = new Receta();
            receta.setNombreReceta(nomReceta);
            receta.setRendimientoTotal(rendimiento);
            receta.setDetalles(new ArrayList<>(detalleActual));

            boolean ok = recetaDAO.insertarProductoYReceta(producto, receta);
            if (ok) {
                info("Guardado", "Producto y receta creados correctamente.");
                limpiarFormulario();
                cargarRecetas();
            } else {
                alerta("Error", "No se pudo guardar. Verifica que el producto no exista ya.");
            }
        } else {
            // ── MODO EDICIÓN ──────────────────────────────────
            Producto producto = recetaEnEdicion.getProducto();
            producto.setNombreProducto(nombre);
            producto.setDescripcion(descripcion);
            producto.setPrecioVenta(precio);
            producto.setUnidadesPorPresentacion(unidades);

            recetaEnEdicion.setNombreReceta(nomReceta);
            recetaEnEdicion.setRendimientoTotal(rendimiento);
            recetaEnEdicion.setDetalles(new ArrayList<>(detalleActual));

            boolean ok = recetaDAO.actualizarProductoYReceta(producto, recetaEnEdicion);
            if (ok) {
                info("Actualizado", "Producto y receta actualizados correctamente.");
                limpiarFormulario();
                cargarRecetas();
            } else {
                alerta("Error", "No se pudo actualizar el registro.");
            }
        }
    }

    // ── CONFIRMAR ELIMINAR (desde botón en tabla) ──────────────

    private void confirmarEliminar(Receta r) {
        String nombre = r.getProducto() != null
                ? r.getProducto().getNombreProducto() : r.getNombreReceta();

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Eliminar el producto \"" + nombre + "\" y su receta? Esta acción no se puede deshacer.",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirmar eliminación");
        confirm.setHeaderText(null);

        Optional<ButtonType> resultado = confirm.showAndWait();
        if (resultado.isPresent() && resultado.get() == ButtonType.YES) {
            boolean ok = (r.getProducto() != null)
                    ? recetaDAO.eliminarProductoYReceta(r.getProducto().getIdProducto())
                    : recetaDAO.eliminarReceta(r.getIdReceta());

            if (ok) {
                info("Eliminado", "Registro eliminado correctamente.");
                if (recetaEnEdicion != null && recetaEnEdicion.getIdReceta() == r.getIdReceta()) {
                    limpiarFormulario();
                }
                cargarRecetas();
            } else {
                alerta("Error", "No se pudo eliminar. Puede que existan ventas asociadas a este producto.");
            }
        }
    }

    // ── BOTÓN LIMPIAR ─────────────────────────────────────────

    @FXML
    public void limpiarFormulario() {
        recetaEnEdicion = null;
        if (txtNombreProducto != null) txtNombreProducto.clear();
        if (txtDescripcion    != null) txtDescripcion.clear();
        if (txtPrecioVenta    != null) txtPrecioVenta.clear();
        if (txtUnidades       != null) txtUnidades.clear();
        if (txtNombreReceta   != null) txtNombreReceta.clear();
        if (txtRendimiento    != null) txtRendimiento.clear();
        detalleActual.clear();
        if (lblCostoEstimado  != null) lblCostoEstimado.setText("$0.00 / unidad");
        tablaRecetas.getSelectionModel().clearSelection();
    }

    // ── COSTO EN VIVO ─────────────────────────────────────────

    private void actualizarCostoEnVivo() {
        if (txtRendimiento == null) return;
        txtRendimiento.textProperty().addListener((obs, old, val) -> recalcularCosto());
    }

    private void recalcularCosto() {
        if (lblCostoEstimado == null) return;

        BigDecimal costoTotal = detalleActual.stream()
                .map(DetalleReceta::getCostoEstimado)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int rendimiento = 1;
        try {
            String r = get(txtRendimiento);
            if (!r.isEmpty()) rendimiento = Math.max(1, Integer.parseInt(r));
        } catch (NumberFormatException ignored) {}

        BigDecimal costoUnit = costoTotal.divide(
                BigDecimal.valueOf(rendimiento), 2, RoundingMode.HALF_UP);

        lblCostoEstimado.setText("$" + costoUnit + " / unidad");

        // Actualizar columna costo en tabla detalle
        tablaDetalleReceta.refresh();
    }

    // ── HELPERS ───────────────────────────────────────────────

    /** Lee el texto de un TextField o TextArea de forma segura. */
    private String get(TextInputControl campo) {
        return campo != null ? campo.getText().trim() : "";
    }

    private void alerta(String titulo, String mensaje) {
        Alert a = new Alert(Alert.AlertType.ERROR, mensaje, ButtonType.OK);
        a.setTitle(titulo);
        a.setHeaderText(null);
        a.showAndWait();
    }

    private void info(String titulo, String mensaje) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, mensaje, ButtonType.OK);
        a.setTitle(titulo);
        a.setHeaderText(null);
        a.showAndWait();
    }
}
