package dao;

import config.DatabaseConnection;
import model.*;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class VentaDAO {

    private final ProductoDAO productoDAO = new ProductoDAO();

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

    /** Guarda venta con detalles y descuenta ingredientes por receta. */
    public int guardarVenta(Venta venta, RecetaDAO recetaDAO) {
        String sqlVenta = "INSERT INTO venta (id_pedido, id_cliente, fecha_venta, total_venta, " +
                "tipo_venta, metodo_pago, numero_comprobante) VALUES (?,?,CURRENT_DATE,?,?,?,?)";
        String sqlDetalle = "INSERT INTO detalle_venta (id_venta, id_producto, cantidad, precio_unitario, subtotal) " +
                "VALUES (?,?,?,?,?)";
        String sqlMov = "INSERT INTO movimiento_inventario " +
                "(id_ingrediente, id_tipo_movimiento, fecha_movimiento, cantidad_gramos, descripcion, referencia) " +
                "VALUES (?, (SELECT id_tipo_movimiento FROM tipo_movimiento WHERE nombre_tipo='Producción'), " +
                "CURRENT_DATE, ?, 'Descuento por venta', ?)";
        String sqlStock = "UPDATE ingrediente SET stock_actual_gramos = stock_actual_gramos - ? WHERE id_ingrediente = ?";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            // Insertar venta
            int idVenta;
            try (PreparedStatement ps = conn.prepareStatement(sqlVenta, Statement.RETURN_GENERATED_KEYS)) {
                if (venta.getIdPedido() != null) ps.setInt(1, venta.getIdPedido()); else ps.setNull(1, Types.INTEGER);
                if (venta.getCliente() != null)  ps.setInt(2, venta.getCliente().getIdCliente()); else ps.setNull(2, Types.INTEGER);
                ps.setBigDecimal(3, venta.getTotalVenta());
                ps.setString(4, venta.getTipoVenta());
                ps.setString(5, venta.getMetodoPago());
                ps.setString(6, venta.getNumeroComprobante());
                ps.executeUpdate();
                ResultSet keys = ps.getGeneratedKeys();
                keys.next();
                idVenta = keys.getInt(1);
            }

            // Insertar detalles y descontar inventario
            for (DetalleVenta dv : venta.getDetalles()) {
                try (PreparedStatement ps = conn.prepareStatement(sqlDetalle)) {
                    ps.setInt(1, idVenta);
                    ps.setInt(2, dv.getProducto().getIdProducto());
                    ps.setInt(3, dv.getCantidad());
                    ps.setBigDecimal(4, dv.getPrecioUnitario());
                    ps.setBigDecimal(5, dv.getSubtotal());
                    ps.executeUpdate();
                }

                // Descontar por receta
                Receta receta = recetaDAO.buscarPorProducto(dv.getProducto().getIdProducto());
                if (receta != null) {
                    for (DetalleReceta dr : receta.getDetalles()) {
                        BigDecimal gramos = dr.getCantidadGramos()
                                .multiply(BigDecimal.valueOf(dv.getCantidad()))
                                .divide(BigDecimal.valueOf(receta.getRendimientoTotal()), 4, java.math.RoundingMode.HALF_UP);

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
