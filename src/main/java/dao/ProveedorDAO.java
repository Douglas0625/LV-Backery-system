package dao;

import config.DatabaseConnection;
import model.Proveedor;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProveedorDAO {

    public List<Proveedor> listarTodos() {
        List<Proveedor> lista = new ArrayList<>();
        String sql = "SELECT id_proveedor, nombre_proveedor, telefono FROM proveedor ORDER BY nombre_proveedor";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Proveedor p = new Proveedor();
                p.setIdProveedor(rs.getInt("id_proveedor"));
                p.setNombreProveedor(rs.getString("nombre_proveedor"));
                p.setTelefono(rs.getString("telefono"));
                lista.add(p);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return lista;
    }
}
