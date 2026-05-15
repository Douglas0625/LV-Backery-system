package model;

import java.math.BigDecimal;

public class Ingrediente {

    private int idIngrediente;
    private String nombreIngrediente;
    private BigDecimal stockActualGramos;
    private BigDecimal costoPorGramo;

    public Ingrediente() {}

    public Ingrediente(int idIngrediente, String nombreIngrediente,
                       BigDecimal stockActualGramos, BigDecimal costoPorGramo) {
        this.idIngrediente = idIngrediente;
        this.nombreIngrediente = nombreIngrediente;
        this.stockActualGramos = stockActualGramos;
        this.costoPorGramo = costoPorGramo;
    }

    public int getIdIngrediente() { return idIngrediente; }
    public void setIdIngrediente(int idIngrediente) { this.idIngrediente = idIngrediente; }

    public String getNombreIngrediente() { return nombreIngrediente; }
    public void setNombreIngrediente(String nombreIngrediente) { this.nombreIngrediente = nombreIngrediente; }

    public BigDecimal getStockActualGramos() { return stockActualGramos; }
    public void setStockActualGramos(BigDecimal stockActualGramos) {
        this.stockActualGramos = stockActualGramos;
    }

    public BigDecimal getCostoPorGramo() { return costoPorGramo; }
    public void setCostoPorGramo(BigDecimal costoPorGramo) { this.costoPorGramo = costoPorGramo; }

    @Override
    public String toString() { return nombreIngrediente; }
}
