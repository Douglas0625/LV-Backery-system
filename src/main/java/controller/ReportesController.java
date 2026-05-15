package controller;

import config.DatabaseConnection;
import dao.IngredienteDAO;
import dao.MovimientoInventarioDAO;
import dao.VentaDAO;
import dao.PedidoDAO;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import model.Ingrediente;
import model.Venta;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

public class ReportesController {

    // ── KPI cards ─────────────────────────────────────────────────────────────
    @FXML private Label lblVentasMes;
    @FXML private Label lblGananciaNeta;
    @FXML private Label lblProductoTop;
    @FXML private Label lblStockCritico;

    // ── Filtros ───────────────────────────────────────────────────────────────
    @FXML private DatePicker dpInicio;
    @FXML private DatePicker dpFin;

    // ── Gráficos ──────────────────────────────────────────────────────────────
    @FXML private LineChart<String, Number>  chartVentasDia;
    @FXML private PieChart                   chartDistribucion;
    @FXML private BarChart<String, Number>   chartProductos;

    // ── Tabla últimas ventas ──────────────────────────────────────────────────
    @FXML private TableView<Venta>               tablaUltimasVentas;
    @FXML private TableColumn<Venta, Integer>    colFactura;
    @FXML private TableColumn<Venta, String>     colClienteRep;
    @FXML private TableColumn<Venta, String>     colFechaRep;
    @FXML private TableColumn<Venta, BigDecimal> colTotalRep;

    // ── Tabla stock crítico ────────────────────────────────────────────────────
    @FXML private TableView<Ingrediente>               tablaStockCritico;
    @FXML private TableColumn<Ingrediente, String>     colIngredienteRep;
    @FXML private TableColumn<Ingrediente, BigDecimal> colStockRep;
    @FXML private TableColumn<Ingrediente, String>     colEstadoRep;

    private final VentaDAO    ventaDAO    = new VentaDAO();
    private final IngredienteDAO ingDAO   = new IngredienteDAO();
    private final PedidoDAO   pedidoDAO  = new PedidoDAO();

    @FXML
    public void initialize() {
        configurarTablas();
        cargarTodo(LocalDate.now().withDayOfMonth(1), LocalDate.now());
    }

    @FXML
    public void filtrar() {
        LocalDate inicio = dpInicio != null ? dpInicio.getValue() : LocalDate.now().withDayOfMonth(1);
        LocalDate fin    = dpFin    != null ? dpFin.getValue()    : LocalDate.now();
        if (inicio == null) inicio = LocalDate.now().withDayOfMonth(1);
        if (fin    == null) fin    = LocalDate.now();
        cargarTodo(inicio, fin);
    }

    private void cargarTodo(LocalDate inicio, LocalDate fin) {
        cargarKpis(inicio, fin);
        cargarChartVentasDia(inicio, fin);
        cargarChartProductos(inicio, fin);
        cargarChartDistribucion(inicio, fin);
        cargarTablaVentas();
        cargarTablaStockCritico();
    }

    // ── KPIs ──────────────────────────────────────────────────────────────────

    private void cargarKpis(LocalDate inicio, LocalDate fin) {
        BigDecimal totalVentas = totalVentasPeriodo(inicio, fin);
        BigDecimal costoTotal  = costoTotalPeriodo(inicio, fin);
        String     productoTop = productoTopPeriodo(inicio, fin);
        long       stockBajo   = ingDAO.listarTodos().stream()
                .filter(i -> i.getStockActualGramos().compareTo(BigDecimal.valueOf(500)) < 0).count();

        if (lblVentasMes    != null) lblVentasMes.setText("$" + totalVentas.setScale(2, java.math.RoundingMode.HALF_UP));
        if (lblGananciaNeta != null) lblGananciaNeta.setText("$" + totalVentas.subtract(costoTotal).setScale(2, java.math.RoundingMode.HALF_UP));
        if (lblProductoTop  != null) lblProductoTop.setText(productoTop.isEmpty() ? "—" : productoTop);
        if (lblStockCritico != null) lblStockCritico.setText(stockBajo + " ingredientes");
    }

    private BigDecimal totalVentasPeriodo(LocalDate inicio, LocalDate fin) {
        String sql = "SELECT COALESCE(SUM(total_venta),0) FROM venta WHERE fecha_venta BETWEEN ? AND ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, java.sql.Date.valueOf(inicio));
            ps.setDate(2, java.sql.Date.valueOf(fin));
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getBigDecimal(1);
        } catch (Exception e) { e.printStackTrace(); }
        return BigDecimal.ZERO;
    }

    private BigDecimal costoTotalPeriodo(LocalDate inicio, LocalDate fin) {
        String sql = """
            SELECT COALESCE(SUM(dv.cantidad * p.costo_estimado_unitario), 0)
            FROM detalle_venta dv
            JOIN venta v ON dv.id_venta = v.id_venta
            JOIN producto p ON dv.id_producto = p.id_producto
            WHERE v.fecha_venta BETWEEN ? AND ?
            """;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, java.sql.Date.valueOf(inicio));
            ps.setDate(2, java.sql.Date.valueOf(fin));
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getBigDecimal(1);
        } catch (Exception e) { e.printStackTrace(); }
        return BigDecimal.ZERO;
    }

    private String productoTopPeriodo(LocalDate inicio, LocalDate fin) {
        String sql = """
            SELECT p.nombre_producto
            FROM detalle_venta dv
            JOIN venta v ON dv.id_venta = v.id_venta
            JOIN producto p ON dv.id_producto = p.id_producto
            WHERE v.fecha_venta BETWEEN ? AND ?
            GROUP BY p.id_producto, p.nombre_producto
            ORDER BY SUM(dv.cantidad) DESC
            LIMIT 1
            """;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, java.sql.Date.valueOf(inicio));
            ps.setDate(2, java.sql.Date.valueOf(fin));
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString(1);
        } catch (Exception e) { e.printStackTrace(); }
        return "";
    }

    // ── Chart: Ventas por día ─────────────────────────────────────────────────

    private void cargarChartVentasDia(LocalDate inicio, LocalDate fin) {
        if (chartVentasDia == null) return;
        String sql = """
            SELECT fecha_venta, COALESCE(SUM(total_venta),0)
            FROM venta
            WHERE fecha_venta BETWEEN ? AND ?
            GROUP BY fecha_venta
            ORDER BY fecha_venta
            """;
        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        serie.setName("Ventas");
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, java.sql.Date.valueOf(inicio));
            ps.setDate(2, java.sql.Date.valueOf(fin));
            ResultSet rs = ps.executeQuery();
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM");
            while (rs.next()) {
                String fecha = rs.getDate(1).toLocalDate().format(fmt);
                double total = rs.getBigDecimal(2).doubleValue();
                serie.getData().add(new XYChart.Data<>(fecha, total));
            }
        } catch (Exception e) { e.printStackTrace(); }
        chartVentasDia.getData().clear();
        chartVentasDia.getData().add(serie);
    }

    // ── Chart: Productos más vendidos ─────────────────────────────────────────

    private void cargarChartProductos(LocalDate inicio, LocalDate fin) {
        if (chartProductos == null) return;
        String sql = """
            SELECT p.nombre_producto, SUM(dv.cantidad) as total_vendido
            FROM detalle_venta dv
            JOIN venta v ON dv.id_venta = v.id_venta
            JOIN producto p ON dv.id_producto = p.id_producto
            WHERE v.fecha_venta BETWEEN ? AND ?
            GROUP BY p.id_producto, p.nombre_producto
            ORDER BY total_vendido DESC
            LIMIT 8
            """;
        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        serie.setName("Unidades");
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, java.sql.Date.valueOf(inicio));
            ps.setDate(2, java.sql.Date.valueOf(fin));
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                serie.getData().add(new XYChart.Data<>(rs.getString(1), rs.getLong(2)));
        } catch (Exception e) { e.printStackTrace(); }
        chartProductos.getData().clear();
        chartProductos.getData().add(serie);
    }

    // ── Chart: Distribución por tipo ─────────────────────────────────────────

    private void cargarChartDistribucion(LocalDate inicio, LocalDate fin) {
        if (chartDistribucion == null) return;
        String sql = """
            SELECT tipo_venta, COUNT(*) FROM venta
            WHERE fecha_venta BETWEEN ? AND ?
            GROUP BY tipo_venta
            """;
        ObservableList<PieChart.Data> data = FXCollections.observableArrayList();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, java.sql.Date.valueOf(inicio));
            ps.setDate(2, java.sql.Date.valueOf(fin));
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                data.add(new PieChart.Data(rs.getString(1), rs.getLong(2)));
        } catch (Exception e) { e.printStackTrace(); }
        chartDistribucion.setData(data);
    }

    // ── Tablas ────────────────────────────────────────────────────────────────

    private void configurarTablas() {
        if (tablaUltimasVentas != null) {
            if (colFactura    != null) colFactura.setCellValueFactory(new PropertyValueFactory<>("idVenta"));
            if (colClienteRep != null) colClienteRep.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNombreCliente()));
            if (colFechaRep   != null) colFechaRep.setCellValueFactory(d -> new SimpleStringProperty(
                    d.getValue().getFechaVenta() != null ? d.getValue().getFechaVenta().toString() : ""));
            if (colTotalRep   != null) colTotalRep.setCellValueFactory(new PropertyValueFactory<>("totalVenta"));
        }

        if (tablaStockCritico != null) {
            if (colIngredienteRep != null)
                colIngredienteRep.setCellValueFactory(new PropertyValueFactory<>("nombreIngrediente"));
            if (colStockRep != null)
                colStockRep.setCellValueFactory(new PropertyValueFactory<>("stockActualGramos"));
            if (colEstadoRep != null)
                colEstadoRep.setCellValueFactory(d -> new SimpleStringProperty(
                        d.getValue().getStockActualGramos().compareTo(BigDecimal.valueOf(500)) < 0
                                ? "⚠ Crítico" : "✓ Normal"));
        }
    }

    private void cargarTablaVentas() {
        if (tablaUltimasVentas == null) return;
        tablaUltimasVentas.setItems(FXCollections.observableArrayList(ventaDAO.listarTodas()));
    }

    private void cargarTablaStockCritico() {
        if (tablaStockCritico == null) return;
        ObservableList<Ingrediente> criticos = FXCollections.observableArrayList(
                ingDAO.listarTodos().stream()
                        .filter(i -> i.getStockActualGramos().compareTo(BigDecimal.valueOf(500)) < 0)
                        .toList());
        tablaStockCritico.setItems(criticos);
    }
}