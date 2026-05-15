package dao;

import config.DatabaseConnection;
import model.Ingrediente;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class IngredienteDAO {

    public List<Ingrediente> listarTodos() {
        List<Ingrediente> lista = new ArrayList<>();
        String sql = "SELECT id_ingrediente, nombre_ingrediente, stock_actual_gramos, costo_por_gramo " +
                "FROM ingrediente ORDER BY nombre_ingrediente";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        } catch (Exception e) { e.printStackTrace(); }
        return lista;
    }

    public Ingrediente buscarPorId(int id) {
        String sql = "SELECT id_ingrediente, nombre_ingrediente, stock_actual_gramos, costo_por_gramo " +
                "FROM ingrediente WHERE id_ingrediente = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapear(rs);
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    public boolean insertar(Ingrediente i) {
        String sql = "INSERT INTO ingrediente (nombre_ingrediente, stock_actual_gramos, costo_por_gramo) VALUES (?,?,?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, i.getNombreIngrediente());
            ps.setBigDecimal(2, i.getStockActualGramos());
            ps.setBigDecimal(3, i.getCostoPorGramo());
            return ps.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    public boolean actualizar(Ingrediente i) {
        String sql = "UPDATE ingrediente SET nombre_ingrediente=?, costo_por_gramo=? WHERE id_ingrediente=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, i.getNombreIngrediente());
            ps.setBigDecimal(2, i.getCostoPorGramo());
            ps.setInt(3, i.getIdIngrediente());
            return ps.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    /**
     * Actualiza el stock sumando o restando gramos.
     * delta positivo = entrada, negativo = salida.
     */
    public boolean actualizarStock(Connection conn, int idIngrediente, BigDecimal delta) throws SQLException {
        String sql = "UPDATE ingrediente SET stock_actual_gramos = stock_actual_gramos + ? WHERE id_ingrediente = ?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setBigDecimal(1, delta);
        ps.setInt(2, idIngrediente);
        return ps.executeUpdate() > 0;
    }

    public boolean eliminar(int id) {
        String sql = "DELETE FROM ingrediente WHERE id_ingrediente = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    private Ingrediente mapear(ResultSet rs) throws SQLException {
        Ingrediente i = new Ingrediente();
        i.setIdIngrediente(rs.getInt("id_ingrediente"));
        i.setNombreIngrediente(rs.getString("nombre_ingrediente"));
        i.setStockActualGramos(rs.getBigDecimal("stock_actual_gramos"));
        i.setCostoPorGramo(rs.getBigDecimal("costo_por_gramo"));
        return i;
    }
}
