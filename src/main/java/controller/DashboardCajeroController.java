package controller;

import dao.PedidoDAO;
import dao.VentaDAO;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import utils.Sesion;

import java.math.BigDecimal;

public class DashboardCajeroController {

    @FXML private Label lblTotalVentasHoy;
    @FXML private Label lblCantidadVentasHoy;
    @FXML private Label lblPedidosPendientes;
    @FXML private Label lblNombreUsuario;

    private final VentaDAO  ventaDAO  = new VentaDAO();
    private final PedidoDAO pedidoDAO = new PedidoDAO();

    @FXML
    public void initialize() {
        if (lblNombreUsuario != null && Sesion.getUsuarioLogueado() != null)
            lblNombreUsuario.setText(Sesion.getUsuarioLogueado().getNombre());

        if (lblTotalVentasHoy != null)
            lblTotalVentasHoy.setText("$" + ventaDAO.totalVentasHoy().setScale(2, java.math.RoundingMode.HALF_UP));

        if (lblCantidadVentasHoy != null)
            lblCantidadVentasHoy.setText(String.valueOf(ventaDAO.cantidadVentasHoy()));

        if (lblPedidosPendientes != null)
            lblPedidosPendientes.setText(String.valueOf(pedidoDAO.contarPendientes()));
    }

    @FXML public void irPedidos() { abrirVentana("/views/pedidos.fxml", "Pedidos"); }
    @FXML public void irVentas()  { abrirVentana("/views/ventas.fxml",  "Ventas");  }

    @FXML
    public void cerrarSesion() {
        Sesion.cerrarSesion();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/login.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("LV Bakery System");
            stage.setScene(new Scene(root));
            stage.show();
            if (lblNombreUsuario != null)
                ((Stage) lblNombreUsuario.getScene().getWindow()).close();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void abrirVentana(String fxml, String titulo) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("LV Bakery - " + titulo);
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "No se pudo abrir: " + titulo, ButtonType.OK)
            {{ setHeaderText(null); }}.showAndWait();
        }
    }
}
