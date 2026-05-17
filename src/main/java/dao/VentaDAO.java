package dao;

import config.DatabaseConnection;
import model.*;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class VentaDAO {

    private final ProductoDAO productoDAO = new ProductoDAO();

    // ── LISTAR ────────────────────────────────────────────────────

    public List<Venta> listarHoy() {
        List<Venta> lista = new ArrayList<>();
        String sql = """
            SELECT v.id_venta, v.id_pedido, v.fecha_venta, v.total_venta,
                   v.tipo_venta, v.metodo_pago, v.numero_comprobante,
                   c.id_cliente, c.nombre as nombre_cliente
            FROM venta v
            LEFT JOIN cliente c ON v.id_cliente = c.id_cliente
            WHERE v.fecha_venta = CURRENT_DATE
            ORDER BY v.id_venta DESC
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        } catch (Exception e) { e.printStackTrace(); }
        return lista;
    }

    public List<Venta> listarTodas() {
        List<Venta> lista = new ArrayList<>();
        String sql = """
            SELECT v.id_venta, v.id_pedido, v.fecha_venta, v.total_venta,
                   v.tipo_venta, v.metodo_pago, v.numero_comprobante,
                   c.id_cliente, c.nombre as nombre_cliente
            FROM venta v
            LEFT JOIN cliente c ON v.id_cliente = c.id_cliente
            ORDER BY v.id_venta DESC
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        } catch (Exception e) { e.printStackTrace(); }
        return lista;
    }

    public List<Venta> listarPorTipo(String tipo) {
        List<Venta> lista = new ArrayList<>();
        String sql = """
            SELECT v.id_venta, v.id_pedido, v.fecha_venta, v.total_venta,
                   v.tipo_venta, v.metodo_pago, v.numero_comprobante,
                   c.id_cliente, c.nombre as nombre_cliente
            FROM venta v
            LEFT JOIN cliente c ON v.id_cliente = c.id_cliente
            WHERE v.tipo_venta = ?
            ORDER BY v.id_venta DESC
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tipo);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) lista.add(mapear(rs));
        } catch (Exception e) { e.printStackTrace(); }
        return lista;
    }

    /** Retorna los N productos más vendidos (nombre, cantidad total vendida). */
    public List<String[]> productosMasVendidos(int limite) {
        List<String[]> resultado = new ArrayList<>();
        String sql = """
        SELECT p.nombre_producto, SUM(dv.cantidad) as total_vendido
        FROM detalle_venta dv
        INNER JOIN producto p ON dv.id_producto = p.id_producto
        GROUP BY p.nombre_producto
        ORDER BY total_vendido DESC
        LIMIT ?
        """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limite);
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                resultado.add(new String[]{rs.getString("nombre_producto"), rs.getString("total_vendido")});
        } catch (Exception e) { e.printStackTrace(); }
        return resultado;
    }

    /** Retorna el total de ventas del mes actual. */
    public BigDecimal totalVentasMes() {
        String sql = """
        SELECT COALESCE(SUM(total_venta), 0)
        FROM venta
        WHERE DATE_TRUNC('month', fecha_venta) = DATE_TRUNC('month', CURRENT_DATE)
        """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getBigDecimal(1);
        } catch (Exception e) { e.printStackTrace(); }
        return BigDecimal.ZERO;
    }

    /** Ganancia neta del mes (total ventas - costo estimado de productos vendidos). */
    public BigDecimal gananciaNeta() {
        String sql = """
        SELECT COALESCE(SUM(dv.cantidad * p.precio_venta)
                        - SUM(dv.cantidad * p.costo_estimado_unitario), 0)
        FROM detalle_venta dv
        INNER JOIN producto p ON dv.id_producto = p.id_producto
        INNER JOIN venta v ON dv.id_venta = v.id_venta
        WHERE DATE_TRUNC('month', v.fecha_venta) = DATE_TRUNC('month', CURRENT_DATE)
        """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getBigDecimal(1);
        } catch (Exception e) { e.printStackTrace(); }
        return BigDecimal.ZERO;
    }

    /** Retorna las ventas totales por día del mes actual para el gráfico de línea. */
    public List<String[]> ventasPorDiaMesActual() {
        List<String[]> resultado = new ArrayList<>();
        String sql = """
        SELECT TO_CHAR(fecha_venta, 'DD/MM') as dia,
               COALESCE(SUM(total_venta), 0) as total
        FROM venta
        WHERE DATE_TRUNC('month', fecha_venta) = DATE_TRUNC('month', CURRENT_DATE)
        GROUP BY fecha_venta
        ORDER BY fecha_venta
        """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next())
                resultado.add(new String[]{rs.getString("dia"), rs.getString("total")});
        } catch (Exception e) { e.printStackTrace(); }
        return resultado;
    }

    /** Ventas por tipo para el PieChart (DIRECTA / PEDIDO). */
    public List<String[]> ventasPorTipo() {
        List<String[]> resultado = new ArrayList<>();
        String sql = """
        SELECT tipo_venta, COUNT(*) as cantidad
        FROM venta
        GROUP BY tipo_venta
        """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next())
                resultado.add(new String[]{rs.getString("tipo_venta"), rs.getString("cantidad")});
        } catch (Exception e) { e.printStackTrace(); }
        return resultado;
    }

    /** Últimas N ventas para tabla de reportes. */
    public List<Venta> listarUltimas(int limite) {
        List<Venta> lista = new ArrayList<>();
        String sql = """
        SELECT v.id_venta, v.id_pedido, v.fecha_venta, v.total_venta,
               v.tipo_venta, v.metodo_pago, v.numero_comprobante,
               c.id_cliente, c.nombre as nombre_cliente
        FROM venta v
        LEFT JOIN cliente c ON v.id_cliente = c.id_cliente
        ORDER BY v.id_venta DESC
        LIMIT ?
        """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limite);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) lista.add(mapear(rs));
        } catch (Exception e) { e.printStackTrace(); }
        return lista;
    }

    /**
     * Carga el detalle de una venta específica (productos, cantidades, precios).
     * Se usa al seleccionar una venta del historial para mostrar su contenido.
     */
    public List<DetalleVenta> listarDetalles(int idVenta) {
        List<DetalleVenta> lista = new ArrayList<>();
        String sql = """
            SELECT dv.id_detalle_venta, dv.id_venta, dv.id_producto,
                   dv.cantidad, dv.precio_unitario, dv.subtotal,
                   p.nombre_producto
            FROM detalle_venta dv
            INNER JOIN producto p ON dv.id_producto = p.id_producto
            WHERE dv.id_venta = ?
            ORDER BY dv.id_detalle_venta
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idVenta);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                DetalleVenta dv = new DetalleVenta();
                dv.setIdDetalleVenta(rs.getInt("id_detalle_venta"));
                dv.setIdVenta(rs.getInt("id_venta"));
                dv.setCantidad(rs.getInt("cantidad"));
                dv.setPrecioUnitario(rs.getBigDecimal("precio_unitario"));
                dv.setSubtotal(rs.getBigDecimal("subtotal"));
                // Construir producto mínimo (solo lo que necesita la tabla)
                Producto p = new Producto();
                p.setIdProducto(rs.getInt("id_producto"));
                p.setNombreProducto(rs.getString("nombre_producto"));
                p.setPrecioVenta(rs.getBigDecimal("precio_unitario"));
                dv.setProducto(p);
                lista.add(dv);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return lista;
    }

    // ── MÉTRICAS ──────────────────────────────────────────────────

    public BigDecimal totalVentasHoy() {
        String sql = "SELECT COALESCE(SUM(total_venta),0) FROM venta WHERE fecha_venta = CURRENT_DATE";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getBigDecimal(1);
        } catch (Exception e) { e.printStackTrace(); }
        return BigDecimal.ZERO;
    }

    public long cantidadVentasHoy() {
        String sql = "SELECT COUNT(*) FROM venta WHERE fecha_venta = CURRENT_DATE";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getLong(1);
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }

    // ── GUARDAR ───────────────────────────────────────────────────

    /**
     * Guarda la venta con todos sus detalles y descuenta ingredientes
     * del inventario según las recetas de cada producto vendido.
     * Todo ocurre en una sola transacción: o todo se guarda o nada.
     */
    public int guardarVenta(Venta venta, RecetaDAO recetaDAO) {

        String sqlVenta = """
            INSERT INTO venta (id_pedido, id_cliente, fecha_venta, total_venta,
                               tipo_venta, metodo_pago, numero_comprobante)
            VALUES (?,?,?,?,?,?,?)
            """;
        String sqlDetalle = """
            INSERT INTO detalle_venta (id_venta, id_producto, cantidad, precio_unitario, subtotal)
            VALUES (?,?,?,?,?)
            """;
        String sqlMov = """
            INSERT INTO movimiento_inventario
                (id_ingrediente, id_tipo_movimiento, fecha_movimiento,
                 cantidad_gramos, descripcion, referencia)
            VALUES (?,
                    (SELECT id_tipo_movimiento FROM tipo_movimiento WHERE nombre_tipo='Producción'),
                    CURRENT_DATE, ?, 'Descuento por venta', ?)
            """;
        String sqlStock = """
            UPDATE ingrediente
            SET stock_actual_gramos = stock_actual_gramos - ?
            WHERE id_ingrediente = ?
            """;

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            // 1. Insertar venta y obtener ID generado
            int idVenta;
            try (PreparedStatement ps = conn.prepareStatement(sqlVenta, Statement.RETURN_GENERATED_KEYS)) {
                if (venta.getIdPedido() != null)
                    ps.setInt(1, venta.getIdPedido());
                else
                    ps.setNull(1, Types.INTEGER);

                if (venta.getCliente() != null)
                    ps.setInt(2, venta.getCliente().getIdCliente());
                else
                    ps.setNull(2, Types.INTEGER);

                ps.setDate(3, Date.valueOf(venta.getFechaVenta()));
                ps.setBigDecimal(4, venta.getTotalVenta());
                ps.setString(5, venta.getTipoVenta());
                ps.setString(6, venta.getMetodoPago());
                ps.setString(7, venta.getNumeroComprobante());
                ps.executeUpdate();

                ResultSet keys = ps.getGeneratedKeys();
                keys.next();
                idVenta = keys.getInt(1);
            }

            // 2. Insertar cada línea de detalle y descontar inventario
            for (DetalleVenta dv : venta.getDetalles()) {

                // Insertar detalle
                try (PreparedStatement ps = conn.prepareStatement(sqlDetalle)) {
                    ps.setInt(1, idVenta);
                    ps.setInt(2, dv.getProducto().getIdProducto());
                    ps.setInt(3, dv.getCantidad());
                    ps.setBigDecimal(4, dv.getPrecioUnitario());
                    ps.setBigDecimal(5, dv.getSubtotal());
                    ps.executeUpdate();
                }

                // Descontar ingredientes según receta
                Receta receta = recetaDAO.buscarPorProducto(dv.getProducto().getIdProducto());
                if (receta != null && !receta.getDetalles().isEmpty()) {
                    for (DetalleReceta dr : receta.getDetalles()) {
                        // Gramos a descontar proporcional a la cantidad vendida
                        BigDecimal gramos = dr.getCantidadGramos()
                                .multiply(BigDecimal.valueOf(dv.getCantidad()))
                                .divide(BigDecimal.valueOf(receta.getRendimientoTotal()),
                                        4, java.math.RoundingMode.HALF_UP);

                        try (PreparedStatement ps = conn.prepareStatement(sqlStock)) {
                            ps.setBigDecimal(1, gramos);
                            ps.setInt(2, dr.getIngrediente().getIdIngrediente());
                            ps.executeUpdate();
                        }
                        try (PreparedStatement ps = conn.prepareStatement(sqlMov)) {
                            ps.setInt(1, dr.getIngrediente().getIdIngrediente());
                            ps.setBigDecimal(2, gramos);
                            ps.setString(3, "Venta #" + idVenta);
                            ps.executeUpdate();
                        }
                    }
                }
            }

            conn.commit();
            return idVenta;

        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * Total de ventas del mes en curso (o en el rango dado).
     * Si fechaInicio y fechaFin son null, usa el mes actual.
     */
    public BigDecimal totalVentasMesReporte() {
        String sql = """
            SELECT COALESCE(SUM(total_venta), 0)
            FROM venta
            WHERE DATE_TRUNC('month', fecha_venta) = DATE_TRUNC('month', CURRENT_DATE)
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getBigDecimal(1);
        } catch (Exception e) { e.printStackTrace(); }
        return BigDecimal.ZERO;
    }

    /**
     * Total de ventas en un rango de fechas.
     */
    public BigDecimal totalVentasEnRangoReporte(LocalDate desde, LocalDate hasta) {
        String sql = "SELECT COALESCE(SUM(total_venta), 0) FROM venta WHERE fecha_venta BETWEEN ? AND ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(desde));
            ps.setDate(2, Date.valueOf(hasta));
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getBigDecimal(1);
        } catch (Exception e) { e.printStackTrace(); }
        return BigDecimal.ZERO;
    }

    /**
     * Ganancia neta del mes = total ventas del mes - costo estimado de productos vendidos.
     * Costo = SUM(dv.cantidad * p.costo_estimado_unitario) para ventas del mes.
     */
//    public BigDecimal gananciaNetaReporte() {
//        String sql = """
//            SELECT
//                COALESCE(SUM(v.total_venta), 0) -
//                COALESCE(SUM(dv.cantidad * p.costo_estimado_unitario), 0)
//            FROM venta v
//            INNER JOIN detalle_venta dv ON v.id_venta = dv.id_venta
//            INNER JOIN producto p ON dv.id_producto = p.id_producto
//            WHERE DATE_TRUNC('month', v.fecha_venta) = DATE_TRUNC('month', CURRENT_DATE)
//            """;
//        try (Connection conn = DatabaseConnection.getConnection();
//             PreparedStatement ps = conn.prepareStatement(sql);
//             ResultSet rs = ps.executeQuery()) {
//            if (rs.next()) return rs.getBigDecimal(1);
//        } catch (Exception e) { e.printStackTrace(); }
//        return BigDecimal.ZERO;
//    }

    /**
     * Ganancia neta en un rango de fechas.
     */
    public BigDecimal gananciaNetaReporte(LocalDate desde, LocalDate hasta) {
        String sql = """
            SELECT
                COALESCE(SUM(v.total_venta), 0) -
                COALESCE(SUM(dv.cantidad * p.costo_estimado_unitario), 0)
            FROM venta v
            INNER JOIN detalle_venta dv ON v.id_venta = dv.id_venta
            INNER JOIN producto p ON dv.id_producto = p.id_producto
            WHERE v.fecha_venta BETWEEN ? AND ?
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(desde));
            ps.setDate(2, Date.valueOf(hasta));
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getBigDecimal(1);
        } catch (Exception e) { e.printStackTrace(); }
        return BigDecimal.ZERO;
    }

    /**
     * Productos más vendidos del mes actual.
     * Retorna List<String[]> donde [0]=nombre_producto, [1]=cantidad_total.
     * @param limit cuántos productos devolver (ej: 1 para el top, 8 para el gráfico)
     */
    public List<String[]> productosMasVendidosReporte(int limit) {
        List<String[]> lista = new ArrayList<>();
        String sql = """
            SELECT p.nombre_producto, SUM(dv.cantidad) AS total_vendido
            FROM detalle_venta dv
            INNER JOIN producto p ON dv.id_producto = p.id_producto
            INNER JOIN venta v ON dv.id_venta = v.id_venta
            WHERE DATE_TRUNC('month', v.fecha_venta) = DATE_TRUNC('month', CURRENT_DATE)
            GROUP BY p.nombre_producto
            ORDER BY total_vendido DESC
            LIMIT ?
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                lista.add(new String[]{ rs.getString(1), rs.getString(2) });
            }
        } catch (Exception e) { e.printStackTrace(); }
        return lista;
    }

    /**
     * Productos más vendidos en un rango de fechas.
     */
    public List<String[]> productosMasVendidosReporte(int limit, LocalDate desde, LocalDate hasta) {
        List<String[]> lista = new ArrayList<>();
        String sql = """
            SELECT p.nombre_producto, SUM(dv.cantidad) AS total_vendido
            FROM detalle_venta dv
            INNER JOIN producto p ON dv.id_producto = p.id_producto
            INNER JOIN venta v ON dv.id_venta = v.id_venta
            WHERE v.fecha_venta BETWEEN ? AND ?
            GROUP BY p.nombre_producto
            ORDER BY total_vendido DESC
            LIMIT ?
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(desde));
            ps.setDate(2, Date.valueOf(hasta));
            ps.setInt(3, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                lista.add(new String[]{ rs.getString(1), rs.getString(2) });
            }
        } catch (Exception e) { e.printStackTrace(); }
        return lista;
    }

    /**
     * Ventas agrupadas por día en el mes actual.
     * Retorna List<String[]> donde [0]=día (yyyy-MM-dd), [1]=total del día.
     */
    public List<String[]> ventasPorDiaMesActualReporte() {
        List<String[]> lista = new ArrayList<>();
        String sql = """
            SELECT TO_CHAR(fecha_venta, 'DD/MM') AS dia,
                   COALESCE(SUM(total_venta), 0) AS total
            FROM venta
            WHERE DATE_TRUNC('month', fecha_venta) = DATE_TRUNC('month', CURRENT_DATE)
            GROUP BY fecha_venta
            ORDER BY fecha_venta ASC
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(new String[]{ rs.getString(1), rs.getString(2) });
            }
        } catch (Exception e) { e.printStackTrace(); }
        return lista;
    }

    /**
     * Ventas agrupadas por día en un rango de fechas.
     */
    public List<String[]> ventasPorDiaEnRangoReporte(LocalDate desde, LocalDate hasta) {
        List<String[]> lista = new ArrayList<>();
        String sql = """
            SELECT TO_CHAR(fecha_venta, 'DD/MM') AS dia,
                   COALESCE(SUM(total_venta), 0) AS total
            FROM venta
            WHERE fecha_venta BETWEEN ? AND ?
            GROUP BY fecha_venta
            ORDER BY fecha_venta ASC
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(desde));
            ps.setDate(2, Date.valueOf(hasta));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                lista.add(new String[]{ rs.getString(1), rs.getString(2) });
            }
        } catch (Exception e) { e.printStackTrace(); }
        return lista;
    }

    /**
     * Distribución de ventas por tipo (DIRECTA / PEDIDO) del mes actual.
     * Retorna List<String[]> donde [0]=tipo_venta, [1]=total.
     */
    public List<String[]> ventasPorTipoReporte() {
        List<String[]> lista = new ArrayList<>();
        String sql = """
            SELECT tipo_venta, COALESCE(SUM(total_venta), 0) AS total
            FROM venta
            WHERE DATE_TRUNC('month', fecha_venta) = DATE_TRUNC('month', CURRENT_DATE)
            GROUP BY tipo_venta
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(new String[]{ rs.getString(1), rs.getString(2) });
            }
        } catch (Exception e) { e.printStackTrace(); }
        return lista;
    }

    /**
     * Distribución de ventas por tipo en un rango de fechas.
     */
    public List<String[]> ventasPorTipoReporte(LocalDate desde, LocalDate hasta) {
        List<String[]> lista = new ArrayList<>();
        String sql = """
            SELECT tipo_venta, COALESCE(SUM(total_venta), 0) AS total
            FROM venta
            WHERE fecha_venta BETWEEN ? AND ?
            GROUP BY tipo_venta
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(desde));
            ps.setDate(2, Date.valueOf(hasta));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                lista.add(new String[]{ rs.getString(1), rs.getString(2) });
            }
        } catch (Exception e) { e.printStackTrace(); }
        return lista;
    }

    /**
     * Últimas N ventas registradas (para la tabla del reporte).
     */
    public List<Venta> listarUltimasReporte(int limit) {
        List<Venta> lista = new ArrayList<>();
        String sql = """
            SELECT v.id_venta, v.id_pedido, v.fecha_venta, v.total_venta,
                   v.tipo_venta, v.metodo_pago, v.numero_comprobante,
                   c.id_cliente, c.nombre as nombre_cliente
            FROM venta v
            LEFT JOIN cliente c ON v.id_cliente = c.id_cliente
            ORDER BY v.id_venta DESC
            LIMIT ?
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) lista.add(mapear(rs));
        } catch (Exception e) { e.printStackTrace(); }
        return lista;
    }

    /**
     * Últimas N ventas en un rango de fechas.
     */
    public List<Venta> listarUltimasEnRangoReporte(int limit, LocalDate desde, LocalDate hasta) {
        List<Venta> lista = new ArrayList<>();
        String sql = """
            SELECT v.id_venta, v.id_pedido, v.fecha_venta, v.total_venta,
                   v.tipo_venta, v.metodo_pago, v.numero_comprobante,
                   c.id_cliente, c.nombre as nombre_cliente
            FROM venta v
            LEFT JOIN cliente c ON v.id_cliente = c.id_cliente
            WHERE v.fecha_venta BETWEEN ? AND ?
            ORDER BY v.id_venta DESC
            LIMIT ?
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(desde));
            ps.setDate(2, Date.valueOf(hasta));
            ps.setInt(3, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) lista.add(mapear(rs));
        } catch (Exception e) { e.printStackTrace(); }
        return lista;
    }

    // ── MAPEO PRIVADO ─────────────────────────────────────────────

    private Venta mapear(ResultSet rs) throws SQLException {
        Venta v = new Venta();
        v.setIdVenta(rs.getInt("id_venta"));

        int idPedido = rs.getInt("id_pedido");
        if (!rs.wasNull()) v.setIdPedido(idPedido);

        v.setFechaVenta(rs.getDate("fecha_venta").toLocalDate());
        v.setTotalVenta(rs.getBigDecimal("total_venta"));
        v.setTipoVenta(rs.getString("tipo_venta"));
        v.setMetodoPago(rs.getString("metodo_pago"));
        v.setNumeroComprobante(rs.getString("numero_comprobante"));

        int idCliente = rs.getInt("id_cliente");
        if (!rs.wasNull()) {
            Cliente c = new Cliente();
            c.setIdCliente(idCliente);
            c.setNombre(rs.getString("nombre_cliente"));
            v.setCliente(c);
        }
        return v;
    }
}