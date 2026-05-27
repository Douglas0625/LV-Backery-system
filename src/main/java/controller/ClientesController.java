package controller;

import dao.ClienteDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import model.Cliente;

public class ClientesController {

    // ── Tabla ────────────────────────────────────────────────────
    @FXML private TableView<Cliente>            tablaClientes;
    @FXML private TableColumn<Cliente, Integer> colId;
    @FXML private TableColumn<Cliente, String>  colNombre;
    @FXML private TableColumn<Cliente, String>  colTelefono;
    @FXML private TableColumn<Cliente, String>  colCorreo;

    // ── Búsqueda y contador ──────────────────────────────────────
    @FXML private TextField txtBuscar;
    @FXML private Label     lblTotal;

    // ── Formulario Crear ─────────────────────────────────────────
    @FXML private TextField txtNombreCrear;
    @FXML private TextField txtTelefonoCrear;
    @FXML private TextField txtCorreoCrear;

    // ── Formulario Editar ────────────────────────────────────────
    @FXML private TextField txtNombreEditar;
    @FXML private TextField txtTelefonoEditar;
    @FXML private TextField txtCorreoEditar;

    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final ObservableList<Cliente> datos = FXCollections.observableArrayList();
    private Cliente seleccionado;

    @FXML
    public void initialize() {
        configurarColumnas();
        cargarClientes();

        tablaClientes.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, nuevo) -> {
                    seleccionado = nuevo;
                    if (nuevo != null) poblarFormularioEditar(nuevo);
                });

        if (txtBuscar != null)
            txtBuscar.textProperty().addListener((obs, old, v) -> buscar(v.trim()));
    }

    private void configurarColumnas() {
        colId.setCellValueFactory(new PropertyValueFactory<>("idCliente"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colTelefono.setCellValueFactory(new PropertyValueFactory<>("telefono"));
        colCorreo.setCellValueFactory(new PropertyValueFactory<>("correo"));
        tablaClientes.setItems(datos);
    }

    private void cargarClientes() {
        datos.setAll(clienteDAO.listarTodos());
        actualizarContador();
    }

    private void buscar(String texto) {
        datos.setAll(texto.isEmpty() ? clienteDAO.listarTodos() : clienteDAO.buscar(texto));
        actualizarContador();
    }

    private void actualizarContador() {
        if (lblTotal != null) lblTotal.setText(String.valueOf(clienteDAO.contarTodos()));
    }

    @FXML
    public void crearCliente() {
        String nombre   = val(txtNombreCrear);
        String telefono = val(txtTelefonoCrear);
        String correo   = val(txtCorreoCrear);

        if (nombre.isEmpty()) { alerta("Campo requerido", "El nombre es obligatorio."); return; }
        if (!correo.isEmpty() && !correo.contains("@")) { alerta("Correo inválido", "Ingresa un correo válido."); return; }

        if (clienteDAO.insertar(new Cliente(0, nombre, telefono, correo))) {
            info("Éxito", "Cliente creado correctamente.");
            limpiarCrear();
            cargarClientes();
        } else {
            alerta("Error", "No se pudo crear el cliente.");
        }
    }

    @FXML
    public void actualizarCliente() {
        if (seleccionado == null) { alerta("Sin selección", "Selecciona un cliente de la tabla."); return; }

        String nombre   = val(txtNombreEditar);
        String telefono = val(txtTelefonoEditar);
        String correo   = val(txtCorreoEditar);

        if (nombre.isEmpty()) { alerta("Campo requerido", "El nombre es obligatorio."); return; }
        if (!correo.isEmpty() && !correo.contains("@")) { alerta("Correo inválido", "Ingresa un correo válido."); return; }

        seleccionado.setNombre(nombre);
        seleccionado.setTelefono(telefono);
        seleccionado.setCorreo(correo);

        if (clienteDAO.actualizar(seleccionado)) {
            info("Éxito", "Cliente actualizado.");
            cargarClientes();
        } else {
            alerta("Error", "No se pudo actualizar el cliente.");
        }
    }

    @FXML
    public void eliminarCliente() {
        if (seleccionado == null) { alerta("Sin selección", "Selecciona un cliente de la tabla."); return; }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Eliminar al cliente \"" + seleccionado.getNombre() + "\"?\nEsta acción no se puede deshacer.",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirmar eliminación");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                if (clienteDAO.eliminar(seleccionado.getIdCliente())) {
                    info("Éxito", "Cliente eliminado.");
                    seleccionado = null;
                    limpiarEditar();
                    cargarClientes();
                } else {
                    alerta("Error", "No se pudo eliminar. El cliente puede tener pedidos asociados.");
                }
            }
        });
    }

    private void poblarFormularioEditar(Cliente c) {
        if (txtNombreEditar   != null) txtNombreEditar.setText(nvl(c.getNombre()));
        if (txtTelefonoEditar != null) txtTelefonoEditar.setText(nvl(c.getTelefono()));
        if (txtCorreoEditar   != null) txtCorreoEditar.setText(nvl(c.getCorreo()));
    }

    private void limpiarCrear() {
        if (txtNombreCrear   != null) txtNombreCrear.clear();
        if (txtTelefonoCrear != null) txtTelefonoCrear.clear();
        if (txtCorreoCrear   != null) txtCorreoCrear.clear();
    }

    private void limpiarEditar() {
        if (txtNombreEditar   != null) txtNombreEditar.clear();
        if (txtTelefonoEditar != null) txtTelefonoEditar.clear();
        if (txtCorreoEditar   != null) txtCorreoEditar.clear();
    }

    private String val(TextField f) { return f != null ? f.getText().trim() : ""; }
    private String nvl(String s)    { return s != null ? s : ""; }

    private void alerta(String titulo, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(titulo); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    private void info(String titulo, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(titulo); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }
}