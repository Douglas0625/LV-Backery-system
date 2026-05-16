package model;

import java.math.BigDecimal;

public class DetallePedido {

    private int idDetallePedido;
    private int idPedido;
    private Producto producto;
    private int cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal subtotal;

    public DetallePedido() {}

    public int getIdDetallePedido() { return idDetallePedido; }
    public void setIdDetallePedido(int idDetallePedido) { this.idDetallePedido = idDetallePedido; }

    public int getIdPedido() { return idPedido; }
    public void setIdPedido(int idPedido) { this.idPedido = idPedido; }

    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }

    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }

    public BigDecimal getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(BigDecimal precioUnitario) { this.precioUnitario = precioUnitario; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    public void calcularSubtotal() {
        if (precioUnitario != null) {
            this.subtotal = precioUnitario.multiply(BigDecimal.valueOf(cantidad));
        }
    }
}
