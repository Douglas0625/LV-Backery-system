package dao;

import config.DatabaseConnection;
import model.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PedidoDAO {

    private final ClienteDAO clienteDAO   = new ClienteDAO();
    private final ProductoDAO productoDAO = new ProductoDAO();

    // ── LISTAR ──────────────────────────────────────────────────
    public List<Pedido> listarTodos() {
        return listarConFiltro("ORDER BY p.id_pedido DESC");
    }

    public List<Pedido> listarPendientes() {
        return listarConFiltro(
            "WHERE e.nombre_estado NOT IN ('Entregado','Cancelado') ORDER BY p.fecha_entrega ASC");
    }

    private List<Pedido> listarConFiltro(String filtro) {
        List<Pedido> lista = new ArrayList<>();
        String sql = """
            SELECT p.id_pedido, p.fecha_pedido, p.fecha_entrega, p.descripcion_pedido, p.total_pedido,
                   c.id_cliente, c.nombre as nombre_cliente, c.telefono, c.correo,
                   e.id_estado_pedido, e.nombre_estado
            FROM pedido p
            INNER JOIN cliente c ON p.id_cliente = c.id_cliente
            INNER JOIN estado_pedido e ON p.id_estado_pedido = e.id_estado_pedido
            """ + filtro;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        } catch (Exception e) { e.printStackTrace(); }
        return lista;
    }

    public List<Pedido> buscarPorCliente(String nombre) {
        List<Pedido> lista = new ArrayList<>();
        String sql = """
            SELECT p.id_pedido, p.fecha_pedido, p.fecha_entrega, p.descripcion_pedido, p.total_pedido,
                   c.id_cliente, c.nombre as nombre_cliente, c.telefono, c.correo,
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
            ResultSet rs = ps.executeQuery();
            while (rs.next()) lista.add(mapear(rs));
        } catch (Exception e) { e.printStackTrace(); }
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
            ResultSet rs = ps.executeQuery();
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
        } catch (Exception e) { e.printStackTrace(); }
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
        } catch (Exception e) { e.printStackTrace(); }
        return lista;
    }

    // ── INSERTAR ─────────────────────────────────────────────────
    public int insertar(Pedido pedido) {
        String sql = "INSERT INTO pedido (id_cliente, fecha_pedido, fecha_entrega, id_estado_pedido, " +
                     "descripcion_pedido, total_pedido) VALUES (?,?,?,?,?,?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            conn.setAutoCommit(false);
            ps.setInt(1, pedido.getCliente().getIdCliente());
            ps.setDate(2, Date.valueOf(pedido.getFechaPedido()));
            ps.setDate(3, Date.valueOf(pedido.getFechaEntrega()));
            ps.setInt(4, pedido.getEstadoPedido().getIdEstadoPedido());
            ps.setString(5, pedido.getDescripcionPedido());
            ps.setBigDecimal(6, pedido.getTotalPedido());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                int id = keys.getInt(1);
                for (DetallePedido dp : pedido.getDetalles())
                    insertarDetalle(conn, id, dp);
                conn.commit();
                return id;
            }
            conn.rollback();
        } catch (Exception e) { e.printStackTrace(); }
        return -1;
    }

    // ── ACTUALIZAR ───────────────────────────────────────────────
    public boolean actualizar(Pedido pedido) {
        String sql = "UPDATE pedido SET id_cliente=?, fecha_pedido=?, fecha_entrega=?, " +
                     "id_estado_pedido=?, descripcion_pedido=?, total_pedido=? WHERE id_pedido=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            ps.setInt(1, pedido.getCliente().getIdCliente());
            ps.setDate(2, Date.valueOf(pedido.getFechaPedido()));
            ps.setDate(3, Date.valueOf(pedido.getFechaEntrega()));
            ps.setInt(4, pedido.getEstadoPedido().getIdEstadoPedido());
            ps.setString(5, pedido.getDescripcionPedido());
            ps.setBigDecimal(6, pedido.getTotalPedido());
            ps.setInt(7, pedido.getIdPedido());
            ps.executeUpdate();
            // Reemplazar detalle
            eliminarDetalles(conn, pedido.getIdPedido());
            for (DetallePedido dp : pedido.getDetalles())
                insertarDetalle(conn, pedido.getIdPedido(), dp);
            conn.commit();
            return true;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    // ── ELIMINAR ─────────────────────────────────────────────────
    public boolean eliminar(int idPedido) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            eliminarDetalles(conn, idPedido);
            PreparedStatement ps = conn.prepareStatement("DELETE FROM pedido WHERE id_pedido=?");
            ps.setInt(1, idPedido);
            ps.executeUpdate();
            conn.commit();
            return true;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    // ── CAMBIAR ESTADO ───────────────────────────────────────────
    public boolean actualizarEstado(int idPedido, int idEstado) {
        String sql = "UPDATE pedido SET id_estado_pedido=? WHERE id_pedido=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idEstado); ps.setInt(2, idPedido);
            return ps.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    // ── MÉTRICAS ─────────────────────────────────────────────────
    public long contarPendientes() {
        String sql = "SELECT COUNT(*) FROM pedido p " +
                     "INNER JOIN estado_pedido e ON p.id_estado_pedido=e.id_estado_pedido " +
                     "WHERE e.nombre_estado NOT IN ('Entregado','Cancelado')";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getLong(1);
        } catch (Exception e) { e.printStackTrace(); }
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
