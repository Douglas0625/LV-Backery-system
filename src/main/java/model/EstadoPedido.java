package model;

public class EstadoPedido {

    private int idEstadoPedido;
    private String nombreEstado;

    public EstadoPedido() {}

    public EstadoPedido(int idEstadoPedido, String nombreEstado) {
        this.idEstadoPedido = idEstadoPedido;
        this.nombreEstado = nombreEstado;
    }

    public int getIdEstadoPedido() { return idEstadoPedido; }
    public void setIdEstadoPedido(int idEstadoPedido) { this.idEstadoPedido = idEstadoPedido; }

    public String getNombreEstado() { return nombreEstado; }
    public void setNombreEstado(String nombreEstado) { this.nombreEstado = nombreEstado; }

    @Override
    public String toString() { return nombreEstado; }
}
