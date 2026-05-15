package model;

import java.math.BigDecimal;

public class Producto {

    private int idProducto;
    private String nombreProducto;
    private String descripcion;
    private BigDecimal precioVenta;
    private BigDecimal costoEstimadoUnitario;
    private int unidadesPorPresentacion;

    public Producto() {}

    public Producto(int idProducto, String nombreProducto, String descripcion,
                    BigDecimal precioVenta, BigDecimal costoEstimadoUnitario,
                    int unidadesPorPresentacion) {
        this.idProducto = idProducto;
        this.nombreProducto = nombreProducto;
        this.descripcion = descripcion;
        this.precioVenta = precioVenta;
        this.costoEstimadoUnitario = costoEstimadoUnitario;
        this.unidadesPorPresentacion = unidadesPorPresentacion;
    }

    public int getIdProducto() { return idProducto; }
    public void setIdProducto(int idProducto) { this.idProducto = idProducto; }

    public String getNombreProducto() { return nombreProducto; }
    public void setNombreProducto(String nombreProducto) { this.nombreProducto = nombreProducto; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public BigDecimal getPrecioVenta() { return precioVenta; }
    public void setPrecioVenta(BigDecimal precioVenta) { this.precioVenta = precioVenta; }

    public BigDecimal getCostoEstimadoUnitario() { return costoEstimadoUnitario; }
    public void setCostoEstimadoUnitario(BigDecimal costoEstimadoUnitario) {
        this.costoEstimadoUnitario = costoEstimadoUnitario;
    }

    public int getUnidadesPorPresentacion() { return unidadesPorPresentacion; }
    public void setUnidadesPorPresentacion(int unidadesPorPresentacion) {
        this.unidadesPorPresentacion = unidadesPorPresentacion;
    }

    @Override
    public String toString() { return nombreProducto; }
}