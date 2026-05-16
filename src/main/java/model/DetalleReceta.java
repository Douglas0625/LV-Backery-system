package model;

import java.math.BigDecimal;

public class DetalleReceta {

    private int idDetalleReceta;
    private int idReceta;
    private Ingrediente ingrediente;
    private BigDecimal cantidadGramos;

    public DetalleReceta() {}

    public int getIdDetalleReceta() { return idDetalleReceta; }
    public void setIdDetalleReceta(int idDetalleReceta) { this.idDetalleReceta = idDetalleReceta; }

    public int getIdReceta() { return idReceta; }
    public void setIdReceta(int idReceta) { this.idReceta = idReceta; }

    public Ingrediente getIngrediente() { return ingrediente; }
    public void setIngrediente(Ingrediente ingrediente) { this.ingrediente = ingrediente; }

    public BigDecimal getCantidadGramos() { return cantidadGramos; }
    public void setCantidadGramos(BigDecimal cantidadGramos) { this.cantidadGramos = cantidadGramos; }

    // Costo estimado de este ingrediente en la receta
    public BigDecimal getCostoEstimado() {
        if (ingrediente != null && ingrediente.getCostoPorGramo() != null && cantidadGramos != null) {
            return cantidadGramos.multiply(ingrediente.getCostoPorGramo());
        }
        return BigDecimal.ZERO;
    }
}
