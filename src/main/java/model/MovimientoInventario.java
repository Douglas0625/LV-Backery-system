package model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class MovimientoInventario {

    private int idMovimiento;
    private Ingrediente ingrediente;
    private String tipoMovimiento;
    private LocalDate fechaMovimiento;
    private BigDecimal cantidadGramos;
    private String descripcion;
    private String referencia;
    private String observacion;

    public MovimientoInventario() {}

    public int getIdMovimiento() { return idMovimiento; }
    public void setIdMovimiento(int idMovimiento) { this.idMovimiento = idMovimiento; }

    public Ingrediente getIngrediente() { return ingrediente; }
    public void setIngrediente(Ingrediente ingrediente) { this.ingrediente = ingrediente; }

    public String getTipoMovimiento() { return tipoMovimiento; }
    public void setTipoMovimiento(String tipoMovimiento) { this.tipoMovimiento = tipoMovimiento; }

    public LocalDate getFechaMovimiento() { return fechaMovimiento; }
    public void setFechaMovimiento(LocalDate fechaMovimiento) { this.fechaMovimiento = fechaMovimiento; }

    public BigDecimal getCantidadGramos() { return cantidadGramos; }
    public void setCantidadGramos(BigDecimal cantidadGramos) { this.cantidadGramos = cantidadGramos; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getReferencia() { return referencia; }
    public void setReferencia(String referencia) { this.referencia = referencia; }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }

    // Para mostrar en tabla
    public String getNombreIngrediente() { return ingrediente != null ? ingrediente.getNombreIngrediente() : ""; }
}