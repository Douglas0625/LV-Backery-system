package dao;

import config.DatabaseConnection;
import model.Compra;
import model.DetalleCompra;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CompraDAO {

    /** Guarda compra + detalles + actualiza stock + registra movimiento odo en 1 transacción. */
    public int registrarCompra(Compra compra) {
        String sqlCompra  = "INSERT INTO compra (fecha_compra, id_proveedor, total_compra) VALUES (CURRENT_DATE,?,?)";
        String sqlDetalle = "INSERT INTO detalle_compra (id_compra, id_ingrediente, cantidad_gramos, costo_unitario_gramo, subtotal) VALUES (?,?,?,?,?)";
        String sqlStock   = "UPDATE ingrediente SET stock_actual_gramos = stock_actual_gramos + ?, costo_por_gramo = ? WHERE id_ingrediente = ?";
        String sqlTipo    = "SELECT id_tipo_movimiento FROM tipo_movimiento WHERE nombre_tipo = 'Compra'";
        String sqlMov     = "INSERT INTO movimiento_inventario (id_ingrediente, id_tipo_movimiento, fecha_movimiento, cantidad_gramos, descripcion, referencia) VALUES (?,?,CURRENT_DATE,?,?,?)";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            // 1. Insertar cabecera compra
            int idCompra;
            try (PreparedStatement ps = conn.prepareStatement(sqlCompra, Statement.RETURN_GENERATED_KEYS)) {
                if (compra.getIdProveedor() != null) ps.setInt(1, compra.getIdProveedor());
                else ps.setNull(1, Types.INTEGER);
                ps.setBigDecimal(2, compra.getTotalCompra());
                ps.executeUpdate();
                ResultSet keys = ps.getGeneratedKeys();
                keys.next();
                idCompra = keys.getInt(1);
            }

            // 2. Obtener id tipo movimiento Compra
            int idTipoCompra;
            try (PreparedStatement ps = conn.prepareStatement(sqlTipo);
                 ResultSet rs = ps.executeQuery()) {
                rs.next();
                idTipoCompra = rs.getInt(1);
            }

            // 3. Por cada detalle: insertar, actualizar stock, registrar movimiento
            for (DetalleCompra d : compra.getDetalles()) {
                // Detalle compra
                try (PreparedStatement ps = conn.prepareStatement(sqlDetalle)) {
                    ps.setInt(1, idCompra);
                    ps.setInt(2, d.getIdIngrediente());
                    ps.setBigDecimal(3, d.getCantidadGramos());
                    ps.setBigDecimal(4, d.getCostoUnitarioGramo());
                    ps.setBigDecimal(5, d.getSubtotal());
                    ps.executeUpdate();
                }
                // Stock + costo por gramo
                try (PreparedStatement ps = conn.prepareStatement(sqlStock)) {
                    ps.setBigDecimal(1, d.getCantidadGramos());
                    ps.setBigDecimal(2, d.getCostoUnitarioGramo());
                    ps.setInt(3, d.getIdIngrediente());
                    ps.executeUpdate();
                }
                // Movimiento inventario
                try (PreparedStatement ps = conn.prepareStatement(sqlMov)) {
                    ps.setInt(1, d.getIdIngrediente());
                    ps.setInt(2, idTipoCompra);
                    ps.setBigDecimal(3, d.getCantidadGramos());
                    ps.setString(4, "Compra #" + idCompra);
                    ps.setString(5, compra.getReferenciaProveedor());
                    ps.executeUpdate();
                }
            }

            conn.commit();
            return idCompra;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }
}
