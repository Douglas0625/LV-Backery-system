package dao;

import config.DatabaseConnection;
import model.Rol;
import model.Usuario;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UsuarioDAO {

    public Usuario iniciarSesion(String username, String password) {
        Usuario usuario = null;
        String sql = """
                SELECT u.id_usuario, u.nombre, u.usuario, u.password,
                       r.id_rol, r.nombre_rol
                FROM usuario u
                INNER JOIN rol r ON u.id_rol = r.id_rol
                WHERE u.usuario = ? AND u.password = ?
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Rol rol = new Rol(rs.getInt("id_rol"), rs.getString("nombre_rol"));
                usuario = new Usuario(rs.getInt("id_usuario"), rs.getString("nombre"),
                        rs.getString("usuario"), rs.getString("password"), rol);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return usuario;
    }

    public List<Usuario> listarTodos() {
        List<Usuario> lista = new ArrayList<>();
        String sql = """
            SELECT u.id_usuario, u.nombre, u.usuario, u.password,
                   r.id_rol, r.nombre_rol
            FROM usuario u
            INNER JOIN rol r ON u.id_rol = r.id_rol
            ORDER BY u.nombre
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Rol rol = new Rol(rs.getInt("id_rol"), rs.getString("nombre_rol"));
                lista.add(new Usuario(rs.getInt("id_usuario"), rs.getString("nombre"),
                        rs.getString("usuario"), rs.getString("password"), rol));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return lista;
    }

    public List<Rol> listarRoles() {
        List<Rol> lista = new ArrayList<>();
        String sql = "SELECT id_rol, nombre_rol FROM rol ORDER BY id_rol";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(new Rol(rs.getInt("id_rol"), rs.getString("nombre_rol")));
        } catch (Exception e) { e.printStackTrace(); }
        return lista;
    }

    public boolean insertar(Usuario u) {
        String sql = "INSERT INTO usuario (nombre, usuario, password, id_rol) VALUES (?,?,?,?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, u.getNombre());
            ps.setString(2, u.getUsuario());
            ps.setString(3, u.getPassword());
            ps.setInt(4, u.getRol().getIdRol());
            return ps.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    public boolean actualizar(Usuario u) {
        String sql = "UPDATE usuario SET nombre=?, usuario=?, password=?, id_rol=? WHERE id_usuario=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, u.getNombre());
            ps.setString(2, u.getUsuario());
            ps.setString(3, u.getPassword());
            ps.setInt(4, u.getRol().getIdRol());
            ps.setInt(5, u.getIdUsuario());
            return ps.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    // ============================================================
//  AGREGAR este metodo dentro de la clase UsuarioDAO existente
//  (antes del último metodo al final del archivo)
// ============================================================

    /** Cuenta el total de usuarios registrados en el sistema. */
    public long contarTodos() {
        String sql = "SELECT COUNT(*) FROM usuario";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getLong(1);
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }

    public boolean eliminar(int idUsuario) {
        String sql = "DELETE FROM usuario WHERE id_usuario = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            return ps.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }
}
