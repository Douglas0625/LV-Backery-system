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

    // ── GUARDAR ───────────────────────────────────────────────────

    /**
     * Guarda la venta con todos sus detalles.
     *
     * REGLA: Si la venta viene de un pedido (idPedido != null), NO se descuentan
     * ingredientes del inventario, ya que el descuento ocurrió al pasar el pedido
     * a estado "En producción".
     *
     * Si es venta directa (idPedido == null), descuenta ingredientes según receta.
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

        boolean esDesdePedido = venta.getIdPedido() != null;

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Insertar venta
                int idVenta;
                try (PreparedStatement ps = conn.prepareStatement(sqlVenta, Statement.RETURN_GENERATED_KEYS)) {
                    if (esDesdePedido)
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

                // 2. Insertar detalles
                for (DetalleVenta dv : venta.getDetalles()) {
                    try (PreparedStatement ps = conn.prepareStatement(sqlDetalle)) {
                        ps.setInt(1, idVenta);
                        ps.setInt(2, dv.getProducto().getIdProducto());
                        ps.setInt(3, dv.getCantidad());
                        ps.setBigDecimal(4, dv.getPrecioUnitario());
                        ps.setBigDecimal(5, dv.getSubtotal());
                        ps.executeUpdate();
                    }
                }

                // 3. Descontar inventario SOLO si es venta directa (sin pedido)
                if (!esDesdePedido) {
                    descontarIngredientes(conn, venta.getDetalles(), idVenta, recetaDAO);
                }

                conn.commit();
                return idVenta;

            } catch (Exception ex) {
                conn.rollback();
                ex.printStackTrace();
                return -1;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * Venta por PEDIDO (cliente normal, estado Listo → Entregado).
     * NO descuenta ingredientes.
     * @return id de venta generado, o -1 si falla.
     */
    public int registrarVentaPedido(Venta venta, int idEstadoEntregado) {
        String sqlVenta   = "INSERT INTO venta (id_pedido, id_cliente, fecha_venta, total_venta, tipo_venta, metodo_pago, numero_comprobante) VALUES (?,?,?,?,?,?,?)";
        String sqlDetalle = "INSERT INTO detalle_venta (id_venta, id_producto, cantidad, precio_unitario, subtotal) VALUES (?,?,?,?,?)";
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int idVenta;
                try (PreparedStatement ps = conn.prepareStatement(sqlVenta, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, venta.getIdPedido());
                    if (venta.getCliente() != null) ps.setInt(2, venta.getCliente().getIdCliente());
                    else ps.setNull(2, Types.INTEGER);
                    ps.setDate(3, Date.valueOf(venta.getFechaVenta()));
                    ps.setBigDecimal(4, venta.getTotalVenta());
                    ps.setString(5, "PEDIDO");
                    ps.setString(6, venta.getMetodoPago());
                    ps.setString(7, venta.getNumeroComprobante());
                    ps.executeUpdate();
                    ResultSet keys = ps.getGeneratedKeys(); keys.next();
                    idVenta = keys.getInt(1);
                }
                for (DetalleVenta dv : venta.getDetalles()) {
                    try (PreparedStatement ps = conn.prepareStatement(sqlDetalle)) {
                        ps.setInt(1, idVenta);
                        ps.setInt(2, dv.getProducto().getIdProducto());
                        ps.setInt(3, dv.getCantidad());
                        ps.setBigDecimal(4, dv.getPrecioUnitario());
                        ps.setBigDecimal(5, dv.getSubtotal());
                        ps.executeUpdate();
                    }
                }
                // Marcar pedido como Entregado
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE pedido SET id_estado_pedido=? WHERE id_pedido=?")) {
                    ps.setInt(1, idEstadoEntregado);
                    ps.setInt(2, venta.getIdPedido());
                    ps.executeUpdate();
                }
                conn.commit();
                return idVenta;
            } catch (Exception ex) {
                conn.rollback(); ex.printStackTrace(); return -1;
            }
        } catch (Exception e) {
            e.printStackTrace(); return -1;
        }
    }

    /**
     * Venta DIRECTA desde pedido VITRINA (estado Listo).
     * Descuenta cantidad_restante en detalle_pedido. NO descuenta ingredientes.
     * detallesVitrina: pares {idDetallePedido, cantidadVendida} para descontar restante.
     */
    public int registrarVentaDirectaVitrina(Venta venta,
                                            java.util.Map<Integer, Integer> detallesVitrina,
                                            PedidoDAO pedidoDAO) {
        String sqlVenta   = "INSERT INTO venta (id_pedido, id_cliente, fecha_venta, total_venta, tipo_venta, metodo_pago, numero_comprobante) VALUES (?,?,?,?,?,?,?)";
        String sqlDetalle = "INSERT INTO detalle_venta (id_venta, id_producto, cantidad, precio_unitario, subtotal) VALUES (?,?,?,?,?)";
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int idVenta;
                try (PreparedStatement ps = conn.prepareStatement(sqlVenta, Statement.RETURN_GENERATED_KEYS)) {
                    if (venta.getIdPedido() != null) ps.setInt(1, venta.getIdPedido());
                    else ps.setNull(1, Types.INTEGER);
                    if (venta.getCliente() != null) ps.setInt(2, venta.getCliente().getIdCliente());
                    else ps.setNull(2, Types.INTEGER);
                    ps.setDate(3, Date.valueOf(venta.getFechaVenta()));
                    ps.setBigDecimal(4, venta.getTotalVenta());
                    ps.setString(5, "DIRECTA");
                    ps.setString(6, venta.getMetodoPago());
                    ps.setString(7, venta.getNumeroComprobante());
                    ps.executeUpdate();
                    ResultSet keys = ps.getGeneratedKeys(); keys.next();
                    idVenta = keys.getInt(1);
                }
                for (DetalleVenta dv : venta.getDetalles()) {
                    try (PreparedStatement ps = conn.prepareStatement(sqlDetalle)) {
                        ps.setInt(1, idVenta);
                        ps.setInt(2, dv.getProducto().getIdProducto());
                        ps.setInt(3, dv.getCantidad());
                        ps.setBigDecimal(4, dv.getPrecioUnitario());
                        ps.setBigDecimal(5, dv.getSubtotal());
                        ps.executeUpdate();
                    }
                }
                // Descontar cantidad_restante por cada detalle de pedido VITRINA
                for (java.util.Map.Entry<Integer, Integer> entry : detallesVitrina.entrySet()) {
                    pedidoDAO.descontarRestante(conn, entry.getKey(), entry.getValue());
                }
                conn.commit();
                return idVenta;
            } catch (Exception ex) {
                conn.rollback(); ex.printStackTrace(); return -1;
            }
        } catch (Exception e) {
            e.printStackTrace(); return -1;
        }
    }
    private void descontarIngredientes(Connection conn, List<DetalleVenta> detalles,
                                       int idVenta, RecetaDAO recetaDAO) throws SQLException {
        // Paso 1: acumular ingredientes requeridos
        java.util.Map<Integer, BigDecimal> requerido  = new java.util.LinkedHashMap<>();
        java.util.Map<Integer, String>     nombres    = new java.util.LinkedHashMap<>();

        for (DetalleVenta dv : detalles) {
            Receta receta = recetaDAO.buscarPorProducto(dv.getProducto().getIdProducto());
            if (receta == null || receta.getDetalles().isEmpty()) continue;
            for (DetalleReceta dr : receta.getDetalles()) {
                int id = dr.getIngrediente().getIdIngrediente();
                BigDecimal gramos = dr.getCantidadGramos()
                        .multiply(BigDecimal.valueOf(dv.getCantidad()))
                        .divide(BigDecimal.valueOf(receta.getRendimientoTotal()),
                                4, java.math.RoundingMode.HALF_UP);
                requerido.merge(id, gramos, BigDecimal::add);
                nombres.putIfAbsent(id, dr.getIngrediente().getNombreIngrediente());
            }
        }

        // Paso 2: validar stock acumulado
        for (java.util.Map.Entry<Integer, BigDecimal> e : requerido.entrySet()) {
            BigDecimal stock = obtenerStockIngrediente(conn, e.getKey());
            if (stock.compareTo(e.getValue()) < 0)
                throw new SQLException("Stock insuficiente para: " + nombres.get(e.getKey()));
        }

        String sqlStock = "UPDATE ingrediente SET stock_actual_gramos = stock_actual_gramos - ? WHERE id_ingrediente = ?";
        String sqlMov   = "INSERT INTO movimiento_inventario " +
                "(id_ingrediente, id_tipo_movimiento, fecha_movimiento, cantidad_gramos, descripcion, referencia) " +
                "VALUES (?, (SELECT id_tipo_movimiento FROM tipo_movimiento WHERE nombre_tipo='Producción'), " +
                "CURRENT_DATE, ?, 'Descuento por venta directa', ?)";

        // Paso 3: descontar
        for (java.util.Map.Entry<Integer, BigDecimal> e : requerido.entrySet()) {
            try (PreparedStatement ps = conn.prepareStatement(sqlStock)) {
                ps.setBigDecimal(1, e.getValue());
                ps.setInt(2, e.getKey());
                ps.executeUpdate();
            }
        }

        // Paso 4: registrar movimientos
        for (java.util.Map.Entry<Integer, BigDecimal> e : requerido.entrySet()) {
            try (PreparedStatement ps = conn.prepareStatement(sqlMov)) {
                ps.setInt(1, e.getKey());
                ps.setBigDecimal(2, e.getValue());
                ps.setString(3, "Venta #" + idVenta);
                ps.executeUpdate();
            }
        }
    }

    // ── REPORTES ──────────────────────────────────────────────────

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
            while (rs.next()) lista.add(new String[]{ rs.getString(1), rs.getString(2) });
        } catch (Exception e) { e.printStackTrace(); }
        return lista;
    }

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
            while (rs.next()) lista.add(new String[]{ rs.getString(1), rs.getString(2) });
        } catch (Exception e) { e.printStackTrace(); }
        return lista;
    }

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
            while (rs.next()) lista.add(new String[]{ rs.getString(1), rs.getString(2) });
        } catch (Exception e) { e.printStackTrace(); }
        return lista;
    }

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
            while (rs.next()) lista.add(new String[]{ rs.getString(1), rs.getString(2) });
        } catch (Exception e) { e.printStackTrace(); }
        return lista;
    }

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
            while (rs.next()) lista.add(new String[]{ rs.getString(1), rs.getString(2) });
        } catch (Exception e) { e.printStackTrace(); }
        return lista;
    }

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
            while (rs.next()) lista.add(new String[]{ rs.getString(1), rs.getString(2) });
        } catch (Exception e) { e.printStackTrace(); }
        return lista;
    }

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

    public long contarTodas() {
        String sql = "SELECT COUNT(*) FROM venta";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getLong(1);
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }

    public BigDecimal totalIngresos() {
        String sql = "SELECT COALESCE(SUM(total_venta), 0) FROM venta";
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

    private BigDecimal obtenerStockIngrediente(Connection conn, int idIngrediente) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT stock_actual_gramos FROM ingrediente WHERE id_ingrediente=?")) {
            ps.setInt(1, idIngrediente);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
            }
        }
    }

    public int obtenerIdEstadoPorNombre(String nombre) {
        String sql = "SELECT id_estado_pedido FROM estado_pedido WHERE nombre_estado = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        } catch (Exception e) { e.printStackTrace(); return -1; }
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