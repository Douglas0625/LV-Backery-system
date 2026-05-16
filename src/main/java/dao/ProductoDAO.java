package dao;

import config.DatabaseConnection;
import model.Producto;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductoDAO {

    public List<Producto> listarTodos() {
        List<Producto> lista = new ArrayList<>();
        String sql = "SELECT id_producto, nombre_producto, descripcion, precio_venta, " +
                     "costo_estimado_unitario, unidades_por_presentacion FROM producto ORDER BY nombre_producto";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return lista;
    }

    public Producto buscarPorId(int id) {
        String sql = "SELECT id_producto, nombre_producto, descripcion, precio_venta, " +
                     "costo_estimado_unitario, unidades_por_presentacion FROM producto WHERE id_producto = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapear(rs);
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    public boolean insertar(Producto p) {
        String sql = "INSERT INTO producto (nombre_producto, descripcion, precio_venta, " +
                     "costo_estimado_unitario, unidades_por_presentacion) VALUES (?,?,?,?,?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getNombreProducto());
            ps.setString(2, p.getDescripcion());
            ps.setBigDecimal(3, p.getPrecioVenta());
            ps.setBigDecimal(4, p.getCostoEstimadoUnitario());
            ps.setInt(5, p.getUnidadesPorPresentacion());
            return ps.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    public boolean actualizar(Producto p) {
        String sql = "UPDATE producto SET nombre_producto=?, descripcion=?, precio_venta=?, " +
                     "costo_estimado_unitario=?, unidades_por_presentacion=? WHERE id_producto=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getNombreProducto());
            ps.setString(2, p.getDescripcion());
            ps.setBigDecimal(3, p.getPrecioVenta());
            ps.setBigDecimal(4, p.getCostoEstimadoUnitario());
            ps.setInt(5, p.getUnidadesPorPresentacion());
            ps.setInt(6, p.getIdProducto());
            return ps.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    public boolean eliminar(int id) {
        String sql = "DELETE FROM producto WHERE id_producto = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    private Producto mapear(ResultSet rs) throws SQLException {
        Producto p = new Producto();
        p.setIdProducto(rs.getInt("id_producto"));
        p.setNombreProducto(rs.getString("nombre_producto"));
        p.setDescripcion(rs.getString("descripcion"));
        p.setPrecioVenta(rs.getBigDecimal("precio_venta"));
        p.setCostoEstimadoUnitario(rs.getBigDecimal("costo_estimado_unitario"));
        p.setUnidadesPorPresentacion(rs.getInt("unidades_por_presentacion"));
        return p;
    }
}
