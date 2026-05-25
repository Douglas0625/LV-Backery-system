package controller;

import dao.IngredienteDAO;
import dao.PedidoDAO;
import dao.VentaDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import model.Ingrediente;
import model.Venta;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

public class ReportesController {

    // ── CARDS ──────────────────────────────────────────────────
    @FXML private Label lblTotalVentasMes;
    @FXML private Label lblGananciaNeta;
    @FXML private Label lblProductoTop;
    @FXML private Label lblStockCritico;

    // ── FILTROS ────────────────────────────────────────────────
    @FXML private DatePicker dpFechaInicio;
    @FXML private DatePicker dpFechaFin;

    // ── GRÁFICOS ───────────────────────────────────────────────
    @FXML private LineChart<String, Number>  graficoVentasDia;
    @FXML private PieChart                   graficoDistribucion;
    @FXML private BarChart<String, Number>   graficoProductos;

    // ── TABLA VENTAS ───────────────────────────────────────────
    @FXML private TableView<Venta>               tablaUltimasVentas;
    @FXML private TableColumn<Venta, Integer>    colVentaId;
    @FXML private TableColumn<Venta, String>     colVentaCliente;
    @FXML private TableColumn<Venta, String>     colVentaFecha;
    @FXML private TableColumn<Venta, BigDecimal> colVentaTotal;

    // ── TABLA STOCK ────────────────────────────────────────────
    @FXML private TableView<Ingrediente>               tablaStockCritico;
    @FXML private TableColumn<Ingrediente, String>     colIngNombre;
    @FXML private TableColumn<Ingrediente, BigDecimal> colIngStock;
    @FXML private TableColumn<Ingrediente, String>     colIngEstado;

    private final VentaDAO       ventaDAO  = new VentaDAO();
    private final PedidoDAO      pedidoDAO = new PedidoDAO();
    private final IngredienteDAO ingDAO    = new IngredienteDAO();

    @FXML
    public void initialize() {
        configurarTablaVentas();
        configurarTablaStock();
        // Carga inicial: mes actual sin filtro
        cargarTodo(null, null);
    }

    // ── BOTÓN FILTRAR ──────────────────────────────────────────

    @FXML
    private void onFiltrar() {
        LocalDate desde = (dpFechaInicio != null) ? dpFechaInicio.getValue() : null;
        LocalDate hasta = (dpFechaFin   != null) ? dpFechaFin.getValue()    : null;

        // Si solo se pone una fecha, la otra toma el mismo valor
        if (desde != null && hasta == null) hasta = desde;
        if (hasta != null && desde == null) desde = hasta;

        cargarTodo(desde, hasta);
    }

    // ── CARGA CENTRALIZADA ─────────────────────────────────────

    /**
     * Carga todas las secciones usando el rango dado.
     * Si desde/hasta son null se usa el mes actual.
     */
    private void cargarTodo(LocalDate desde, LocalDate hasta) {
        cargarMetricas(desde, hasta);
        cargarGraficos(desde, hasta);
        cargarTablas(desde, hasta);
    }

    // ── CONFIGURACIÓN DE TABLAS ────────────────────────────────

    private void configurarTablaVentas() {
        if (tablaUltimasVentas == null) return;

        if (colVentaId != null)
            colVentaId.setCellValueFactory(new PropertyValueFactory<>("idVenta"));

        if (colVentaCliente != null)
            colVentaCliente.setCellValueFactory(d ->
                    new javafx.beans.property.SimpleStringProperty(
                            d.getValue().getNombreCliente() != null
                                    ? d.getValue().getNombreCliente()
                                    : "—"));

        if (colVentaFecha != null)
            colVentaFecha.setCellValueFactory(d ->
                    new javafx.beans.property.SimpleStringProperty(
                            d.getValue().getFechaVenta() != null
                                    ? d.getValue().getFechaVenta().toString()
                                    : ""));

        if (colVentaTotal != null)
            colVentaTotal.setCellValueFactory(new PropertyValueFactory<>("totalVenta"));
    }

    private void configurarTablaStock() {
        if (tablaStockCritico == null) return;

        if (colIngNombre != null)
            colIngNombre.setCellValueFactory(new PropertyValueFactory<>("nombreIngrediente"));

        if (colIngStock != null)
            colIngStock.setCellValueFactory(new PropertyValueFactory<>("stockActualGramos"));

        if (colIngEstado != null)
            colIngEstado.setCellValueFactory(d -> {
                boolean bajo = d.getValue().getStockActualGramos()
                        .compareTo(BigDecimal.valueOf(500)) < 0;
                return new javafx.beans.property.SimpleStringProperty(
                        bajo ? "⚠ Crítico" : "✓ OK");
            });
    }

    // ── MÉTRICAS (CARDS) ───────────────────────────────────────

    private void cargarMetricas(LocalDate desde, LocalDate hasta) {

        // Ventas del mes / rango
        BigDecimal mes = (desde != null)
                ? ventaDAO.totalVentasEnRangoReporte(desde, hasta)
                : ventaDAO.totalVentasMes();
        if (lblTotalVentasMes != null)
            lblTotalVentasMes.setText("$ " + mes.setScale(2, RoundingMode.HALF_UP));

        // Ganancia neta del mes / rango
        BigDecimal ganancia = (desde != null)
                ? ventaDAO.gananciaNetaReporte(desde, hasta)
                : ventaDAO.gananciaNeta();
        if (lblGananciaNeta != null)
            lblGananciaNeta.setText("$ " + ganancia.setScale(2, RoundingMode.HALF_UP));

        // Producto top del mes / rango
        List<String[]> tops = (desde != null)
                ? ventaDAO.productosMasVendidosReporte(1, desde, hasta)
                : ventaDAO.productosMasVendidosReporte(1);
        if (lblProductoTop != null)
            lblProductoTop.setText(tops.isEmpty() ? "Sin datos" : tops.get(0)[0]);

        // Stock crítico (ingredientes < 500g) — no depende del rango
        long criticos = ingDAO.listarTodos().stream()
                .filter(i -> i.getStockActualGramos().compareTo(BigDecimal.valueOf(500)) < 0)
                .count();
        if (lblStockCritico != null)
            lblStockCritico.setText(criticos + " ingredientes");
    }

    // ── GRÁFICOS ───────────────────────────────────────────────

    private void cargarGraficos(LocalDate desde, LocalDate hasta) {
        cargarGraficoLinea(desde, hasta);
        cargarGraficoPie(desde, hasta);
        cargarGraficoBarras(desde, hasta);
    }

    private void cargarGraficoLinea(LocalDate desde, LocalDate hasta) {
        if (graficoVentasDia == null) return;

        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        serie.setName("Ventas ($)");

        List<String[]> datos = (desde != null)
                ? ventaDAO.ventasPorDiaEnRangoReporte(desde, hasta)
                : ventaDAO.ventasPorDiaMesActual();

        for (String[] fila : datos) {
            try {
                serie.getData().add(new XYChart.Data<>(fila[0], new BigDecimal(fila[1])));
            } catch (Exception ignored) {}
        }

        graficoVentasDia.setAnimated(false);
        graficoVentasDia.getData().setAll(serie);
    }

    private void cargarGraficoPie(LocalDate desde, LocalDate hasta) {
        if (graficoDistribucion == null) return;

        ObservableList<PieChart.Data> porciones = FXCollections.observableArrayList();

        List<String[]> datos = (desde != null)
                ? ventaDAO.ventasPorTipoReporte(desde, hasta)
                : ventaDAO.ventasPorTipo();

        for (String[] fila : datos) {
            try {
                porciones.add(new PieChart.Data(fila[0], Double.parseDouble(fila[1])));
            } catch (Exception ignored) {}
        }

        if (porciones.isEmpty()) {
            porciones.add(new PieChart.Data("Sin datos", 1));
        }

        graficoDistribucion.setAnimated(false);
        graficoDistribucion.setData(porciones);
    }

    private void cargarGraficoBarras(LocalDate desde, LocalDate hasta) {
        if (graficoProductos == null) return;

        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        serie.setName("Unidades vendidas");

        List<String[]> datos = (desde != null)
                ? ventaDAO.productosMasVendidosReporte(8, desde, hasta)
                : ventaDAO.productosMasVendidosReporte(8);

        for (String[] fila : datos) {
            try {
                serie.getData().add(new XYChart.Data<>(fila[0], Long.parseLong(fila[1])));
            } catch (Exception ignored) {}
        }

        graficoProductos.setAnimated(false);
        graficoProductos.getData().setAll(serie);
    }

    // ── TABLAS ─────────────────────────────────────────────────

    private void cargarTablas(LocalDate desde, LocalDate hasta) {
        // Últimas ventas
        if (tablaUltimasVentas != null) {
            List<Venta> ventas = (desde != null)
                    ? ventaDAO.listarUltimasEnRangoReporte(20, desde, hasta)
                    : ventaDAO.listarUltimas(20);
            tablaUltimasVentas.getItems().setAll(ventas);
        }

        // Stock crítico — siempre muestra el estado actual del inventario
        if (tablaStockCritico != null) {
            List<Ingrediente> todos = ingDAO.listarTodos();
            todos.sort((a, b) -> a.getStockActualGramos().compareTo(b.getStockActualGramos()));
            tablaStockCritico.getItems().setAll(todos);
        }
    }
}