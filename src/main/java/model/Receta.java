package model;

import java.util.ArrayList;
import java.util.List;

public class Receta {

    private int idReceta;
    private Producto producto;
    private String nombreReceta;
    private int rendimientoTotal;
    private List<DetalleReceta> detalles = new ArrayList<>();

    public Receta() {}

    public int getIdReceta() { return idReceta; }
    public void setIdReceta(int idReceta) { this.idReceta = idReceta; }

    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { this.producto = producto; }

    public String getNombreReceta() { return nombreReceta; }
    public void setNombreReceta(String nombreReceta) { this.nombreReceta = nombreReceta; }

    public int getRendimientoTotal() { return rendimientoTotal; }
    public void setRendimientoTotal(int rendimientoTotal) { this.rendimientoTotal = rendimientoTotal; }

    public List<DetalleReceta> getDetalles() { return detalles; }
    public void setDetalles(List<DetalleReceta> detalles) { this.detalles = detalles; }

    @Override
    public String toString() { return nombreReceta; }
}