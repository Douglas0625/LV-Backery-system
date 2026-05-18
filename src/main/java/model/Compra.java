package model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class Compra {

    private int idCompra;
    private Integer idProveedor;
    private BigDecimal totalCompra;
    private String referenciaProveedor;
    private List<DetalleCompra> detalles = new ArrayList<>();

    public int getIdCompra() { return idCompra; }
    public void setIdCompra(int idCompra) { this.idCompra = idCompra; }

    public Integer getIdProveedor() { return idProveedor; }
    public void setIdProveedor(Integer idProveedor) { this.idProveedor = idProveedor; }

    public BigDecimal getTotalCompra() { return totalCompra; }
    public void setTotalCompra(BigDecimal totalCompra) { this.totalCompra = totalCompra; }

    public String getReferenciaProveedor() { return referenciaProveedor; }
    public void setReferenciaProveedor(String referenciaProveedor) { this.referenciaProveedor = referenciaProveedor; }

    public List<DetalleCompra> getDetalles() { return detalles; }
    public void setDetalles(List<DetalleCompra> detalles) { this.detalles = detalles; }
}
