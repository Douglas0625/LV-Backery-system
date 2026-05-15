package dao;

import config.DatabaseConnection;
import model.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RecetaDAO {

    private final ProductoDAO productoDAO = new ProductoDAO();
    private final IngredienteDAO ingredienteDAO = new IngredienteDAO();

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
        String sql = "SELECT id_receta, id_producto, nombre_receta, rendimiento_total FROM receta WHERE id_producto = ?";
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
            }
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
            // Reemplazar detalles
            eliminarDetalles(conn, r.getIdReceta());
            for (DetalleReceta dr : r.getDetalles()) {
                insertarDetalleReceta(conn, r.getIdReceta(), dr);
            }
            return true;
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

    private Receta mapearReceta(ResultSet rs) throws SQLException {
        Receta r = new Receta();
        r.setIdReceta(rs.getInt("id_receta"));
        r.setNombreReceta(rs.getString("nombre_receta"));
        r.setRendimientoTotal(rs.getInt("rendimiento_total"));
        r.setProducto(productoDAO.buscarPorId(rs.getInt("id_producto")));
        return r;
    }
}
