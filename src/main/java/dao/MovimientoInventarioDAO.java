package dao;

import config.DatabaseConnection;
import model.Ingrediente;
import model.MovimientoInventario;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MovimientoInventarioDAO {

    private final IngredienteDAO ingredienteDAO = new IngredienteDAO();

    public List<MovimientoInventario> listarRecientes(int limite) {
        List<MovimientoInventario> lista = new ArrayList<>();
        String sql = """
            SELECT m.id_movimiento, m.fecha_movimiento, m.cantidad_gramos,
                   m.descripcion, m.referencia, m.observacion,
                   i.id_ingrediente, i.nombre_ingrediente,
                   i.stock_actual_gramos, i.costo_por_gramo,
                   t.nombre_tipo
            FROM movimiento_inventario m
            INNER JOIN ingrediente i ON m.id_ingrediente = i.id_ingrediente
            INNER JOIN tipo_movimiento t ON m.id_tipo_movimiento = t.id_tipo_movimiento
            ORDER BY m.id_movimiento DESC
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

    public List<MovimientoInventario> listarTodos() {
        List<MovimientoInventario> lista = new ArrayList<>();
        String sql = """
            SELECT m.id_movimiento, m.fecha_movimiento, m.cantidad_gramos,
                   m.descripcion, m.referencia, m.observacion,
                   i.id_ingrediente, i.nombre_ingrediente,
                   i.stock_actual_gramos, i.costo_por_gramo,
                   t.nombre_tipo
            FROM movimiento_inventario m
            INNER JOIN ingrediente i ON m.id_ingrediente = i.id_ingrediente
            INNER JOIN tipo_movimiento t ON m.id_tipo_movimiento = t.id_tipo_movimiento
            ORDER BY m.id_movimiento DESC
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        } catch (Exception e) { e.printStackTrace(); }
        return lista;
    }

    /** Registra un movimiento y actualiza stock en una sola transacción. */
    public boolean registrarMovimiento(int idIngrediente, String tipoNombre,
                                       BigDecimal cantidadGramos, String descripcion,
                                       String referencia, BigDecimal deltaStock) {
        String sqlTipo = "SELECT id_tipo_movimiento FROM tipo_movimiento WHERE nombre_tipo = ?";
        String sqlMov  = "INSERT INTO movimiento_inventario " +
                         "(id_ingrediente, id_tipo_movimiento, fecha_movimiento, cantidad_gramos, descripcion, referencia) " +
                         "VALUES (?,?,CURRENT_DATE,?,?,?)";
        String sqlStk  = "UPDATE ingrediente SET stock_actual_gramos = stock_actual_gramos + ? WHERE id_ingrediente = ?";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            // Obtener id del tipo
            int idTipo;
            try (PreparedStatement ps = conn.prepareStatement(sqlTipo)) {
                ps.setString(1, tipoNombre);
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) throw new RuntimeException("Tipo de movimiento no encontrado: " + tipoNombre);
                idTipo = rs.getInt(1);
            }

            // Insertar movimiento
            try (PreparedStatement ps = conn.prepareStatement(sqlMov)) {
                ps.setInt(1, idIngrediente);
                ps.setInt(2, idTipo);
                ps.setBigDecimal(3, cantidadGramos);
                ps.setString(4, descripcion);
                ps.setString(5, referencia);
                ps.executeUpdate();
            }

            // Actualizar stock
            try (PreparedStatement ps = conn.prepareStatement(sqlStk)) {
                ps.setBigDecimal(1, deltaStock);
                ps.setInt(2, idIngrediente);
                ps.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private MovimientoInventario mapear(ResultSet rs) throws SQLException {
        MovimientoInventario m = new MovimientoInventario();
        m.setIdMovimiento(rs.getInt("id_movimiento"));
        m.setFechaMovimiento(rs.getDate("fecha_movimiento").toLocalDate());
        m.setCantidadGramos(rs.getBigDecimal("cantidad_gramos"));
        m.setDescripcion(rs.getString("descripcion"));
        m.setReferencia(rs.getString("referencia"));
        m.setObservacion(rs.getString("observacion"));
        m.setTipoMovimiento(rs.getString("nombre_tipo"));

        Ingrediente i = new Ingrediente();
        i.setIdIngrediente(rs.getInt("id_ingrediente"));
        i.setNombreIngrediente(rs.getString("nombre_ingrediente"));
        i.setStockActualGramos(rs.getBigDecimal("stock_actual_gramos"));
        i.setCostoPorGramo(rs.getBigDecimal("costo_por_gramo"));
        m.setIngrediente(i);

        return m;
    }
}
