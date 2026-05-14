package dao;

import config.DatabaseConnection;
import model.Rol;
import model.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class UsuarioDAO {

    public Usuario iniciarSesion(String username, String password) {

        Usuario usuario = null;

        String sql = """
                SELECT u.id_usuario,
                       u.nombre,
                       u.usuario,
                       u.password,
                       r.id_rol,
                       r.nombre_rol
                FROM usuario u
                INNER JOIN rol r
                    ON u.id_rol = r.id_rol
                WHERE u.usuario = ?
                AND u.password = ?
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {

            ps.setString(1, username);
            ps.setString(2, password);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {

                Rol rol = new Rol();
                rol.setIdRol(rs.getInt("id_rol"));
                rol.setNombreRol(rs.getString("nombre_rol"));

                usuario = new Usuario();
                usuario.setIdUsuario(rs.getInt("id_usuario"));
                usuario.setNombre(rs.getString("nombre"));
                usuario.setUsuario(rs.getString("usuario"));
                usuario.setPassword(rs.getString("password"));
                usuario.setRol(rol);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return usuario;
    }
}
