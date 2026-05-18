package dao;

import config.DatabaseConnection;
import model.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RecetaDAO {

    private final ProductoDAO    productoDAO    = new ProductoDAO();
    private final IngredienteDAO ingredienteDAO = new IngredienteDAO();

    // ── CONSULTAS ────────────────────────────────────────────────────────────

    public List<Receta> listarTodas() {
        List<Receta> lista = new ArrayList<>();
        String sql = "SELECT r.id_receta, r.id_producto, r.nombre_receta, r.rendimiento_total " +
                "FROM receta r ORDER BY r.nombre_receta";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Receta receta = mapearReceta(rs);
                receta.setDetalles(listarDetalles(receta.getIdReceta()));
                lista.add(receta);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return lista;
    }

    public Receta buscarPorProducto(int idProducto) {
        String sql = "SELECT id_receta, id_producto, nombre_receta, rendimiento_total " +
                "FROM receta WHERE id_producto = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idProducto);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Receta receta = mapearReceta(rs);
                receta.setDetalles(listarDetalles(receta.getIdReceta()));
                return receta;
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    public List<DetalleReceta> listarDetalles(int idReceta) {
        List<DetalleReceta> detalles = new ArrayList<>();
        String sql = "SELECT dr.id_detalle_receta, dr.id_receta, dr.id_ingrediente, dr.cantidad_gramos " +
                "FROM detalle_receta dr WHERE dr.id_receta = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idReceta);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                DetalleReceta dr = new DetalleReceta();
                dr.setIdDetalleReceta(rs.getInt("id_detalle_receta"));
                dr.setIdReceta(rs.getInt("id_receta"));
                dr.setCantidadGramos(rs.getBigDecimal("cantidad_gramos"));
                dr.setIngrediente(ingredienteDAO.buscarPorId(rs.getInt("id_ingrediente")));
                detalles.add(dr);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return detalles;
    }

    /** Devuelve true si el producto ya tiene una receta registrada. */
    public boolean existeRecetaParaProducto(int idProducto) {
        String sql = "SELECT 1 FROM receta WHERE id_producto = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idProducto);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }

    // ── INSERCIÓN ────────────────────────────────────────────────────────────

    /**
     * Inserta un producto nuevo y su receta en una sola transacción.
     * Actualiza el costo_estimado_unitario del producto según los ingredientes.
     */
    public boolean insertarProductoYReceta(Producto producto, Receta receta) {
        String sqlProd = "INSERT INTO producto (nombre_producto, descripcion, precio_venta, " +
                "costo_estimado_unitario, unidades_por_presentacion) VALUES (?,?,?,?,?)";
        String sqlRec  = "INSERT INTO receta (id_producto, nombre_receta, rendimiento_total) VALUES (?,?,?)";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            // 1. Insertar producto
            int idProducto;
            try (PreparedStatement ps = conn.prepareStatement(sqlProd, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, producto.getNombreProducto());
                ps.setString(2, producto.getDescripcion());
                ps.setBigDecimal(3, producto.getPrecioVenta());
                ps.setBigDecimal(4, producto.getCostoEstimadoUnitario());
                ps.setInt(5, producto.getUnidadesPorPresentacion());
                ps.executeUpdate();
                ResultSet keys = ps.getGeneratedKeys();
                keys.next();
                idProducto = keys.getInt(1);
            }

            // 2. Insertar receta
            int idReceta;
            try (PreparedStatement ps = conn.prepareStatement(sqlRec, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, idProducto);
                ps.setString(2, receta.getNombreReceta());
                ps.setInt(3, receta.getRendimientoTotal());
                ps.executeUpdate();
                ResultSet keys = ps.getGeneratedKeys();
                keys.next();
                idReceta = keys.getInt(1);
            }

            // 3. Insertar detalles
            for (DetalleReceta dr : receta.getDetalles()) {
                insertarDetalleReceta(conn, idReceta, dr);
            }

            // 4. Actualizar costo del producto
            actualizarCostoProducto(conn, idProducto, idReceta, receta.getRendimientoTotal());

            conn.commit();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /** Inserta solo la receta para un producto ya existente. */
    public boolean insertarReceta(Receta r) {
        String sql = "INSERT INTO receta (id_producto, nombre_receta, rendimiento_total) VALUES (?,?,?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, r.getProducto().getIdProducto());
            ps.setString(2, r.getNombreReceta());
            ps.setInt(3, r.getRendimientoTotal());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                int idReceta = keys.getInt(1);
                r.setIdReceta(idReceta);
                for (DetalleReceta dr : r.getDetalles()) {
                    insertarDetalleReceta(conn, idReceta, dr);
                }
                actualizarCostoProducto(conn, r.getProducto().getIdProducto(),
                        idReceta, r.getRendimientoTotal());
            }
            return true;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    // ── ACTUALIZACIÓN ─────────────────────────────────────────────────────────

    /**
     * Actualiza datos del producto y su receta (nombre, rendimiento e ingredientes).
     * Reemplaza todos los detalles y recalcula el costo estimado.
     */
    public boolean actualizarProductoYReceta(Producto producto, Receta receta) {
        String sqlProd = "UPDATE producto SET nombre_producto=?, descripcion=?, precio_venta=?, " +
                "unidades_por_presentacion=? WHERE id_producto=?";
        String sqlRec  = "UPDATE receta SET nombre_receta=?, rendimiento_total=? WHERE id_receta=?";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            // Actualizar producto (sin costo, se recalcula después)
            try (PreparedStatement ps = conn.prepareStatement(sqlProd)) {
                ps.setString(1, producto.getNombreProducto());
                ps.setString(2, producto.getDescripcion());
                ps.setBigDecimal(3, producto.getPrecioVenta());
                ps.setInt(4, producto.getUnidadesPorPresentacion());
                ps.setInt(5, producto.getIdProducto());
                ps.executeUpdate();
            }

            // Actualizar receta
            try (PreparedStatement ps = conn.prepareStatement(sqlRec)) {
                ps.setString(1, receta.getNombreReceta());
                ps.setInt(2, receta.getRendimientoTotal());
                ps.setInt(3, receta.getIdReceta());
                ps.executeUpdate();
            }

            // Reemplazar detalles
            eliminarDetalles(conn, receta.getIdReceta());
            for (DetalleReceta dr : receta.getDetalles()) {
                insertarDetalleReceta(conn, receta.getIdReceta(), dr);
            }

            // Recalcular costo del producto
            actualizarCostoProducto(conn, producto.getIdProducto(),
                    receta.getIdReceta(), receta.getRendimientoTotal());

            conn.commit();
            return true;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    public boolean actualizarReceta(Receta r) {
        String sql = "UPDATE receta SET nombre_receta=?, rendimiento_total=? WHERE id_receta=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, r.getNombreReceta());
            ps.setInt(2, r.getRendimientoTotal());
            ps.setInt(3, r.getIdReceta());
            ps.executeUpdate();
            eliminarDetalles(conn, r.getIdReceta());
            for (DetalleReceta dr : r.getDetalles()) {
                insertarDetalleReceta(conn, r.getIdReceta(), dr);
            }
            actualizarCostoProducto(conn, r.getProducto().getIdProducto(),
                    r.getIdReceta(), r.getRendimientoTotal());
            return true;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    // ── ELIMINACIÓN ───────────────────────────────────────────────────────────

    /** Elimina el producto y en cascada su receta y detalles. */
    public boolean eliminarProductoYReceta(int idProducto) {
        String sql = "DELETE FROM producto WHERE id_producto = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idProducto);
            return ps.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    public boolean eliminarReceta(int idReceta) {
        String sql = "DELETE FROM receta WHERE id_receta = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idReceta);
            return ps.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    // ── HELPERS PRIVADOS ──────────────────────────────────────────────────────

    private void insertarDetalleReceta(Connection conn, int idReceta, DetalleReceta dr) throws SQLException {
        String sql = "INSERT INTO detalle_receta (id_receta, id_ingrediente, cantidad_gramos) VALUES (?,?,?)";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, idReceta);
        ps.setInt(2, dr.getIngrediente().getIdIngrediente());
        ps.setBigDecimal(3, dr.getCantidadGramos());
        ps.executeUpdate();
    }

    private void eliminarDetalles(Connection conn, int idReceta) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("DELETE FROM detalle_receta WHERE id_receta = ?");
        ps.setInt(1, idReceta);
        ps.executeUpdate();
    }

    /**
     * Recalcula costo_estimado_unitario del producto:
     * costo = SUM(cantidad_gramos * costo_por_gramo) / rendimiento_total
     */
    private void actualizarCostoProducto(Connection conn, int idProducto, int idReceta, int rendimiento)
            throws SQLException {
        String sqlCosto = """
            SELECT COALESCE(SUM(dr.cantidad_gramos * i.costo_por_gramo), 0)
            FROM detalle_receta dr
            INNER JOIN ingrediente i ON dr.id_ingrediente = i.id_ingrediente
            WHERE dr.id_receta = ?
            """;
        try (PreparedStatement ps = conn.prepareStatement(sqlCosto)) {
            ps.setInt(1, idReceta);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                java.math.BigDecimal costoTotal = rs.getBigDecimal(1);
                java.math.BigDecimal costoUnit  = costoTotal.divide(
                        java.math.BigDecimal.valueOf(rendimiento), 4, java.math.RoundingMode.HALF_UP);
                try (PreparedStatement pu = conn.prepareStatement(
                        "UPDATE producto SET costo_estimado_unitario = ? WHERE id_producto = ?")) {
                    pu.setBigDecimal(1, costoUnit);
                    pu.setInt(2, idProducto);
                    pu.executeUpdate();
                }
            }
        }
    }

    private Receta mapearReceta(ResultSet rs) throws SQLException {
        Receta r = new Receta();
        r.setIdReceta(rs.getInt("id_receta"));
        r.setNombreReceta(rs.getString("nombre_receta"));
        r.setRendimientoTotal(rs.getInt("rendimiento_total"));
        r.setProducto(productoDAO.buscarPorId(rs.getInt("id_producto")));
        return r;
    }
}
