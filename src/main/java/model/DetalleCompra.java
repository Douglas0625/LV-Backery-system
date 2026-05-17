package model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class DetalleCompra {

    private int idDetalleCompra;
    private int idCompra;
    private int idIngrediente;
    private String nombreIngrediente;   // para mostrar en tabla
    private BigDecimal cantidadGramos;
    private BigDecimal costoUnitarioGramo;
    private BigDecimal subtotal;

    public int getIdDetalleCompra() { return idDetalleCompra; }
    public void setIdDetalleCompra(int v) { this.idDetalleCompra = v; }

    public int getIdCompra() { return idCompra; }
    public void setIdCompra(int v) { this.idCompra = v; }

    public int getIdIngrediente() { return idIngrediente; }
    public void setIdIngrediente(int v) { this.idIngrediente = v; }

    public String getNombreIngrediente() { return nombreIngrediente; }
    public void setNombreIngrediente(String v) { this.nombreIngrediente = v; }

    public BigDecimal getCantidadGramos() { return cantidadGramos; }
    public void setCantidadGramos(BigDecimal v) { this.cantidadGramos = v; }

    public BigDecimal getCostoUnitarioGramo() { return costoUnitarioGramo; }
    public void setCostoUnitarioGramo(BigDecimal v) { this.costoUnitarioGramo = v; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal v) { this.subtotal = v; }

    public void calcularSubtotal() {
        if (cantidadGramos != null && costoUnitarioGramo != null)
            this.subtotal = cantidadGramos.multiply(costoUnitarioGramo).setScale(2, RoundingMode.HALF_UP);
    }
}
