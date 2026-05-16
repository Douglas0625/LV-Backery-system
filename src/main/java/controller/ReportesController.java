package controller;

import dao.IngredienteDAO;
import dao.PedidoDAO;
import dao.VentaDAO;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import model.Ingrediente;

import java.math.BigDecimal;
import java.util.List;

public class ReportesController {

    @FXML private Label lblTotalVentasMes;
    @FXML private Label lblPedidosMes;
    @FXML private Label lblIngredienteCritico;
    @FXML private Label lblTotalVentasHoy;

    private final VentaDAO    ventaDAO  = new VentaDAO();
    private final PedidoDAO   pedidoDAO = new PedidoDAO();
    private final IngredienteDAO ingDAO = new IngredienteDAO();

    @FXML
    public void initialize() {
        cargarMetricas();
    }

    private void cargarMetricas() {
        // Ventas del día
        BigDecimal hoy = ventaDAO.totalVentasHoy();
        if (lblTotalVentasHoy != null)
            lblTotalVentasHoy.setText("$" + hoy.setScale(2, java.math.RoundingMode.HALF_UP));

        // Pedidos pendientes
        long pendientes = pedidoDAO.contarPendientes();
        if (lblPedidosMes != null)
            lblPedidosMes.setText(String.valueOf(pendientes) + " pendientes");

        // Ingrediente con menor stock
        List<Ingrediente> ings = ingDAO.listarTodos();
        ings.stream()
            .min((a, b) -> a.getStockActualGramos().compareTo(b.getStockActualGramos()))
            .ifPresent(i -> {
                if (lblIngredienteCritico != null)
                    lblIngredienteCritico.setText(i.getNombreIngrediente()
                            + " (" + i.getStockActualGramos() + "g)");
            });
    }
}
