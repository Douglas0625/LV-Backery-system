package dao;

import config.DatabaseConnection;
import model.*;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PedidoDAO {

    private final ClienteDAO  clienteDAO  = new ClienteDAO();
    private final ProductoDAO productoDAO = new ProductoDAO();

    // ── LISTAR ──────────────────────────────────────────────────

    public List<Pedido> listarTodos() {
        return listarConFiltro("ORDER BY p.id_pedido DESC");
    }

    public List<Pedido> listarPendientes() {
        return listarConFiltro(
                "WHERE e.nombre_estado NOT IN ('Entregado','Cancelado') ORDER BY p.fecha_entrega ASC");
    }


    public List<Pedido> listarListosPorTipo(boolean esVitrina) {
        String condVitrina = esVitrina
                ? "AND UPPER(c.nombre) = 'VITRINA'"
                : "AND UPPER(c.nombre) != 'VITRINA'";
        return listarConFiltro("WHERE e.nombre_estado = 'Listo' " + condVitrina + " ORDER BY p.fecha_entrega ASC");
    }

    /** Detalle de pedido incluyendo cantidad_restante (para venta directa VITRINA). */
    public List<DetallePedido> listarDetallesConRestante(int idPedido) {
        List<DetallePedido> lista = new ArrayList<>();
        String sql = "SELECT id_detalle_pedido, id_pedido, id_producto, cantidad, " +
                "COALESCE(cantidad_restante, cantidad) AS cantidad_restante, precio_unitario, subtotal " +
                "FROM detalle_pedido WHERE id_pedido = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idPedido);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DetallePedido d = new DetallePedido();
                    d.setIdDetallePedido(rs.getInt("id_detalle_pedido"));
                    d.setIdPedido(rs.getInt("id_pedido"));
                    d.setProducto(productoDAO.buscarPorId(rs.getInt("id_producto")));
                    d.setCantidad(rs.getInt("cantidad_restante")); // usamos restante como "disponible"
                    d.setPrecioUnitario(rs.getBigDecimal("precio_unitario"));
                    d.setSubtotal(rs.getBigDecimal("subtotal"));
                    lista.add(d);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lista;
    }

    /**
     * Descuenta cantidad_restante de detalle_pedido. Lanza excepción si insuficiente.
     * Si tras el descuento TODOS los detalles del pedido quedan en cantidad_restante = 0,
     * marca el pedido como Entregado automáticamente.
     */
    public void descontarRestante(Connection conn, int idDetallePedido, int cantidadVendida) throws SQLException {
        // Obtener disponible e id_pedido
        int disponible;
        int idPedido;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id_pedido, COALESCE(cantidad_restante, cantidad) FROM detalle_pedido WHERE id_detalle_pedido=?")) {
            ps.setInt(1, idDetallePedido);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Detalle de pedido no encontrado.");
                idPedido    = rs.getInt(1);
                disponible  = rs.getInt(2);
            }
        }
        if (cantidadVendida > disponible)
            throw new SQLException("Cantidad solicitada (" + cantidadVendida + ") supera la disponible (" + disponible + ").");

        // Descontar
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE detalle_pedido SET cantidad_restante = COALESCE(cantidad_restante, cantidad) - ? WHERE id_detalle_pedido=?")) {
            ps.setInt(1, cantidadVendida);
            ps.setInt(2, idDetallePedido);
            ps.executeUpdate();
        }

        //Si_todo el pedido quedó en 0 → marcar Entregado
        int totalRestante;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COALESCE(SUM(COALESCE(cantidad_restante, cantidad)), 0) FROM detalle_pedido WHERE id_pedido=?")) {
            ps.setInt(1, idPedido);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                totalRestante = rs.getInt(1);
            }
        }
        if (totalRestante == 0) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE pedido SET id_estado_pedido = " +
                            "(SELECT id_estado_pedido FROM estado_pedido WHERE nombre_estado = 'Entregado') " +
                            "WHERE id_pedido = ?")) {
                ps.setInt(1, idPedido);
                ps.executeUpdate();
            }
        }
    }

    private List<Pedido> listarConFiltro(String filtro) {
        List<Pedido> lista = new ArrayList<>();
        String sql = """
            SELECT p.id_pedido, p.fecha_pedido, p.fecha_entrega, p.descripcion_pedido, p.total_pedido,
                   c.id_cliente, c.nombre AS nombre_cliente, c.telefono, c.correo,
                   e.id_estado_pedido, e.nombre_estado
            FROM pedido p
            INNER JOIN cliente c ON p.id_cliente = c.id_cliente
            INNER JOIN estado_pedido e ON p.id_estado_pedido = e.id_estado_pedido
            """ + filtro;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lista;
    }

    public List<Pedido> buscarPorCliente(String nombre) {
        List<Pedido> lista = new ArrayList<>();
        String sql = """
            SELECT p.id_pedido, p.fecha_pedido, p.fecha_entrega, p.descripcion_pedido, p.total_pedido,
                   c.id_cliente, c.nombre AS nombre_cliente, c.telefono, c.correo,
                   e.id_estado_pedido, e.nombre_estado
            FROM pedido p
            INNER JOIN cliente c ON p.id_cliente = c.id_cliente
            INNER JOIN estado_pedido e ON p.id_estado_pedido = e.id_estado_pedido
            WHERE LOWER(c.nombre) LIKE LOWER(?)
            ORDER BY p.id_pedido DESC
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + nombre + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lista;
    }

    // ── DETALLE ─────────────────────────────────────────────────

    public List<DetallePedido> listarDetalles(int idPedido) {
        List<DetallePedido> lista = new ArrayList<>();
        String sql = "SELECT id_detalle_pedido, id_pedido, id_producto, cantidad, precio_unitario, subtotal " +
                "FROM detalle_pedido WHERE id_pedido = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idPedido);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DetallePedido d = new DetallePedido();
                    d.setIdDetallePedido(rs.getInt("id_detalle_pedido"));
                    d.setIdPedido(rs.getInt("id_pedido"));
                    d.setProducto(productoDAO.buscarPorId(rs.getInt("id_producto")));
                    d.setCantidad(rs.getInt("cantidad"));
                    d.setPrecioUnitario(rs.getBigDecimal("precio_unitario"));
                    d.setSubtotal(rs.getBigDecimal("subtotal"));
                    lista.add(d);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lista;
    }

    // ── ESTADOS ─────────────────────────────────────────────────

    public List<EstadoPedido> listarEstados() {
        List<EstadoPedido> lista = new ArrayList<>();
        String sql = "SELECT id_estado_pedido, nombre_estado FROM estado_pedido ORDER BY id_estado_pedido";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next())
                lista.add(new EstadoPedido(rs.getInt("id_estado_pedido"), rs.getString("nombre_estado")));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lista;
    }

    public List<EstadoPedido> listarEstadosFormulario() {
        List<EstadoPedido> lista = new ArrayList<>();
        String sql = "SELECT id_estado_pedido, nombre_estado FROM estado_pedido " +
                "WHERE nombre_estado NOT IN ('Entregado','Cancelado') " +
                "ORDER BY id_estado_pedido";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next())
                lista.add(new EstadoPedido(rs.getInt("id_estado_pedido"), rs.getString("nombre_estado")));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lista;
    }

    // ── INSERTAR ─────────────────────────────────────────────────

    public int insertar(Pedido pedido) {
        String sql = "INSERT INTO pedido (id_cliente, fecha_pedido, fecha_entrega, id_estado_pedido, " +
                "descripcion_pedido, total_pedido) VALUES (?,?,?,?,?,?)";
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            int idGenerado;
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, pedido.getCliente().getIdCliente());
                ps.setDate(2, Date.valueOf(pedido.getFechaPedido()));
                ps.setDate(3, Date.valueOf(pedido.getFechaEntrega()));
                ps.setInt(4, pedido.getEstadoPedido().getIdEstadoPedido());
                ps.setString(5, pedido.getDescripcionPedido());
                ps.setBigDecimal(6, pedido.getTotalPedido());
                ps.executeUpdate();

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) { conn.rollback(); return -1; }
                    idGenerado = keys.getInt(1);
                }
            }

            for (DetallePedido dp : pedido.getDetalles()) {
                insertarDetalle(conn, idGenerado, dp);
            }

            conn.commit();
            return idGenerado;

        } catch (Exception e) {
            e.printStackTrace();
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            return -1;
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ex) { ex.printStackTrace(); }
        }
    }

    // ── ACTUALIZAR ───────────────────────────────────────────────

    public boolean actualizar(Pedido pedido) {
        String sql = "UPDATE pedido SET id_cliente=?, fecha_pedido=?, fecha_entrega=?, " +
                "id_estado_pedido=?, descripcion_pedido=?, total_pedido=? WHERE id_pedido=?";
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            String estadoActual = obtenerNombreEstado(conn, pedido.getIdPedido());
            if (!"Pendiente".equals(estadoActual)) {
                conn.rollback();
                return false;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, pedido.getCliente().getIdCliente());
                ps.setDate(2, Date.valueOf(pedido.getFechaPedido()));
                ps.setDate(3, Date.valueOf(pedido.getFechaEntrega()));
                ps.setInt(4, pedido.getEstadoPedido().getIdEstadoPedido());
                ps.setString(5, pedido.getDescripcionPedido());
                ps.setBigDecimal(6, pedido.getTotalPedido());
                ps.setInt(7, pedido.getIdPedido());
                ps.executeUpdate();
            }

            eliminarDetalles(conn, pedido.getIdPedido());
            for (DetallePedido dp : pedido.getDetalles()) {
                insertarDetalle(conn, pedido.getIdPedido(), dp);
            }

            conn.commit();
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            return false;
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ex) { ex.printStackTrace(); }
        }
    }

    // ── ELIMINAR ─────────────────────────────────────────────────

    public boolean eliminar(int idPedido) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            eliminarDetalles(conn, idPedido);

            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM pedido WHERE id_pedido=?")) {
                ps.setInt(1, idPedido);
                ps.executeUpdate();
            }

            conn.commit();
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            return false;
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ex) { ex.printStackTrace(); }
        }
    }

    // ── CAMBIAR ESTADO ───────────────────────────────────────────

    /**
     * Cambia el estado del pedido.
     * Si el nuevo estado es "En producción" y el anterior NO lo era,
     * valida recetas, valida stock, descuenta ingredientes y registra movimientos.
     * Todo en una única transacción con rollback completo si algo falla.
     *
     * @return null si OK, o mensaje de error si falla.
     */
    public String actualizarEstado(int idPedido, int idNuevoEstado) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // 1. Obtener estado actual
            String estadoActual = obtenerNombreEstado(conn, idPedido);
            if (estadoActual == null) {
                conn.rollback();
                return "No se encontró el pedido #" + idPedido;
            }

            // 2. Obtener nombre del nuevo estado
            String nuevoEstado = obtenerNombreEstadoPorId(conn, idNuevoEstado);
            if (nuevoEstado == null) {
                conn.rollback();
                return "Estado no válido.";
            }

            // 2b. Validar transición
            if (!transicionValida(estadoActual, nuevoEstado)) {
                conn.rollback();
                return "Transición de estado no permitida.";
            }

            // 3. Si nuevo estado es "En producción" y antes NO lo era => procesar producción
            if ("En producción".equals(nuevoEstado) && !"En producción".equals(estadoActual)) {
                String error = procesarProduccionPedido(conn, idPedido);
                if (error != null) {
                    conn.rollback();
                    return error;
                }
            }

            // 4. Actualizar estado
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE pedido SET id_estado_pedido=? WHERE id_pedido=?")) {
                ps.setInt(1, idNuevoEstado);
                ps.setInt(2, idPedido);
                ps.executeUpdate();
            }

            conn.commit();
            return null; // éxito

        } catch (Exception e) {
            e.printStackTrace();
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            return "Error inesperado: " + e.getMessage();
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ex) { ex.printStackTrace(); }
        }
    }

    /**
     * Método centralizado de producción. Valida recetas, valida stock,
     * descuenta ingredientes y registra movimientos de inventario.
     * Usa la conexión con transacción activa del llamador.
     *
     * @return null si todo OK, o mensaje de error si falla.
     */
    private String procesarProduccionPedido(Connection conn, int idPedido) throws SQLException {
        List<DetallePedido> detalles = listarDetallesConConn(conn, idPedido);

        if (detalles.isEmpty()) {
            return "El pedido #" + idPedido + " no tiene productos.";
        }

        RecetaDAO recetaDAO = new RecetaDAO();

        // Paso 1: Validar que todos los productos tengan receta
        for (DetallePedido dp : detalles) {
            int idProducto = dp.getProducto().getIdProducto();
            Receta receta = recetaDAO.buscarPorProducto(idProducto);
            if (receta == null || receta.getDetalles().isEmpty()) {
                return "El producto \"" + dp.getProducto().getNombreProducto() +
                        "\" no tiene receta registrada. No se puede iniciar producción.";
            }
        }

        // Paso 2: Calcular totales de ingredientes requeridos y validar stock
        // Acumular por ingrediente para validar de una vez
        java.util.Map<Integer, BigDecimal> requerido = new java.util.LinkedHashMap<>();
        java.util.Map<Integer, String>     nombresIng = new java.util.LinkedHashMap<>();

        for (DetallePedido dp : detalles) {
            Receta receta = recetaDAO.buscarPorProducto(dp.getProducto().getIdProducto());
            for (DetalleReceta dr : receta.getDetalles()) {
                int idIng = dr.getIngrediente().getIdIngrediente();
                BigDecimal gramos = dr.getCantidadGramos()
                        .multiply(BigDecimal.valueOf(dp.getCantidad()))
                        .divide(BigDecimal.valueOf(receta.getRendimientoTotal()),
                                4, java.math.RoundingMode.HALF_UP);
                requerido.merge(idIng, gramos, BigDecimal::add);
                nombresIng.putIfAbsent(idIng, dr.getIngrediente().getNombreIngrediente());
            }
        }

        // Validar stock de cada ingrediente
        for (java.util.Map.Entry<Integer, BigDecimal> entry : requerido.entrySet()) {
            int idIng = entry.getKey();
            BigDecimal necesario = entry.getValue();
            BigDecimal disponible = obtenerStockIngrediente(conn, idIng);
            if (disponible == null || disponible.compareTo(necesario) < 0) {
                String nombre = nombresIng.get(idIng);
                return "Stock insuficiente para \"" + nombre + "\". " +
                        "Necesario: " + necesario.setScale(2, java.math.RoundingMode.HALF_UP) +
                        "g, Disponible: " + (disponible != null ? disponible.setScale(2, java.math.RoundingMode.HALF_UP) : "0") + "g.";
            }
        }

        // Paso 3: Descontar stock y registrar movimientos
        int idTipoProduccion = obtenerIdTipoMovimiento(conn, "Producción");
        if (idTipoProduccion < 0) {
            return "No se encontró el tipo de movimiento 'Producción' en la base de datos.";
        }

        for (java.util.Map.Entry<Integer, BigDecimal> entry : requerido.entrySet()) {
            int idIng = entry.getKey();
            BigDecimal gramos = entry.getValue();

            // Descontar stock
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE ingrediente SET stock_actual_gramos = stock_actual_gramos - ? WHERE id_ingrediente = ?")) {
                ps.setBigDecimal(1, gramos);
                ps.setInt(2, idIng);
                ps.executeUpdate();
            }

            // Registrar movimiento
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO movimiento_inventario " +
                            "(id_ingrediente, id_tipo_movimiento, fecha_movimiento, cantidad_gramos, descripcion, referencia) " +
                            "VALUES (?, ?, CURRENT_DATE, ?, 'Descuento por producción', ?)")) {
                ps.setInt(1, idIng);
                ps.setInt(2, idTipoProduccion);
                ps.setBigDecimal(3, gramos);
                ps.setString(4, "Pedido #" + idPedido);
                ps.executeUpdate();
            }
        }

        return null; // éxito
    }

    // ── MÉTRICAS ─────────────────────────────────────────────────

    public long contarPendientes() {
        String sql = "SELECT COUNT(*) FROM pedido p " +
                "INNER JOIN estado_pedido e ON p.id_estado_pedido = e.id_estado_pedido " +
                "WHERE e.nombre_estado NOT IN ('Entregado','Cancelado')";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getLong(1);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    // ── PRIVADOS ─────────────────────────────────────────────────

    private void insertarDetalle(Connection conn, int idPedido, DetallePedido d) throws SQLException {
        String sql = "INSERT INTO detalle_pedido (id_pedido, id_producto, cantidad, precio_unitario, subtotal) " +
                "VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idPedido);
            ps.setInt(2, d.getProducto().getIdProducto());
            ps.setInt(3, d.getCantidad());
            ps.setBigDecimal(4, d.getPrecioUnitario());
            ps.setBigDecimal(5, d.getSubtotal());
            ps.executeUpdate();
        }
    }

    private void eliminarDetalles(Connection conn, int idPedido) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM detalle_pedido WHERE id_pedido=?")) {
            ps.setInt(1, idPedido);
            ps.executeUpdate();
        }
    }

    /** Obtiene detalles del pedido usando la conexión activa (para usar dentro de transacciones). */
    private List<DetallePedido> listarDetallesConConn(Connection conn, int idPedido) throws SQLException {
        List<DetallePedido> lista = new ArrayList<>();
        String sql = "SELECT id_detalle_pedido, id_pedido, id_producto, cantidad, precio_unitario, subtotal " +
                "FROM detalle_pedido WHERE id_pedido = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idPedido);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DetallePedido d = new DetallePedido();
                    d.setIdDetallePedido(rs.getInt("id_detalle_pedido"));
                    d.setIdPedido(rs.getInt("id_pedido"));
                    d.setProducto(productoDAO.buscarPorId(rs.getInt("id_producto")));
                    d.setCantidad(rs.getInt("cantidad"));
                    d.setPrecioUnitario(rs.getBigDecimal("precio_unitario"));
                    d.setSubtotal(rs.getBigDecimal("subtotal"));
                    lista.add(d);
                }
            }
        }
        return lista;
    }

    private String obtenerNombreEstado(Connection conn, int idPedido) throws SQLException {
        String sql = "SELECT e.nombre_estado FROM pedido p " +
                "INNER JOIN estado_pedido e ON p.id_estado_pedido = e.id_estado_pedido " +
                "WHERE p.id_pedido = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idPedido);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    private String obtenerNombreEstadoPorId(Connection conn, int idEstado) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT nombre_estado FROM estado_pedido WHERE id_estado_pedido = ?")) {
            ps.setInt(1, idEstado);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    private BigDecimal obtenerStockIngrediente(Connection conn, int idIngrediente) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT stock_actual_gramos FROM ingrediente WHERE id_ingrediente = ?")) {
            ps.setInt(1, idIngrediente);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal(1) : null;
            }
        }
    }

    private int obtenerIdTipoMovimiento(Connection conn, String nombre) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id_tipo_movimiento FROM tipo_movimiento WHERE nombre_tipo = ?")) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        }
    }

    private boolean transicionValida(String actual, String nuevo) {
        return switch (actual) {
            case "Pendiente"     -> nuevo.equals("En producción") || nuevo.equals("Cancelado");
            case "En producción" -> nuevo.equals("Listo") || nuevo.equals("Cancelado");
            case "Listo"         -> nuevo.equals("Entregado");
            default -> false;
        };
    }

    private Pedido mapear(ResultSet rs) throws SQLException {
        Pedido p = new Pedido();
        p.setIdPedido(rs.getInt("id_pedido"));
        p.setFechaPedido(rs.getDate("fecha_pedido").toLocalDate());
        p.setFechaEntrega(rs.getDate("fecha_entrega").toLocalDate());
        p.setDescripcionPedido(rs.getString("descripcion_pedido"));
        p.setTotalPedido(rs.getBigDecimal("total_pedido"));

        Cliente c = new Cliente();
        c.setIdCliente(rs.getInt("id_cliente"));
        c.setNombre(rs.getString("nombre_cliente"));
        c.setTelefono(rs.getString("telefono"));
        c.setCorreo(rs.getString("correo"));
        p.setCliente(c);

        EstadoPedido e = new EstadoPedido();
        e.setIdEstadoPedido(rs.getInt("id_estado_pedido"));
        e.setNombreEstado(rs.getString("nombre_estado"));
        p.setEstadoPedido(e);

        return p;
    }
}