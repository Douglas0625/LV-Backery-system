package controller;

import dao.UsuarioDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import model.Rol;
import model.Usuario;
import utils.Sesion;

public class UsuariosController {

    // Tabla
    @FXML private TableView<Usuario> tablaUsuarios;
    @FXML private TableColumn<Usuario, Integer> colId;
    @FXML private TableColumn<Usuario, String>  colNombre;
    @FXML private TableColumn<Usuario, String>  colUsername;
    @FXML private TableColumn<Usuario, String>  colRol;

    // Formulario crear
    @FXML private TextField txtNombreCrear;
    @FXML private TextField txtUsernameCrear;
    @FXML private PasswordField txtPasswordCrear;
    @FXML private ComboBox<Rol> cbRolCrear;

    // Formulario editar
    @FXML private TextField txtNombreEditar;
    @FXML private TextField txtUsernameEditar;
    @FXML private PasswordField txtPasswordEditar;
    @FXML private ComboBox<Rol> cbRolEditar;

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final ObservableList<Usuario> datos = FXCollections.observableArrayList();
    private Usuario seleccionado;

    @FXML
    public void initialize() {
        configurarColumnas();
        cargarRoles();
        cargarUsuarios();

        tablaUsuarios.getSelectionModel().selectedItemProperty().addListener((obs, old, nuevo) -> {
            seleccionado = nuevo;
            if (nuevo != null) poblarFormularioEditar(nuevo);
        });
    }

    private void configurarColumnas() {
        colId.setCellValueFactory(new PropertyValueFactory<>("idUsuario"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colUsername.setCellValueFactory(new PropertyValueFactory<>("usuario"));
        colRol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getRol() != null ? data.getValue().getRol().getNombreRol() : ""
                )
        );
        tablaUsuarios.setItems(datos);
    }

    private void cargarRoles() {
        ObservableList<Rol> roles = FXCollections.observableArrayList(usuarioDAO.listarRoles());
        if (cbRolCrear != null)  cbRolCrear.setItems(roles);
        if (cbRolEditar != null) cbRolEditar.setItems(FXCollections.observableArrayList(usuarioDAO.listarRoles()));
    }

    private void cargarUsuarios() {
        datos.setAll(usuarioDAO.listarTodos());
    }

    @FXML
    public void crearUsuario() {
        String nombre   = txtNombreCrear   != null ? txtNombreCrear.getText().trim()   : "";
        String username = txtUsernameCrear != null ? txtUsernameCrear.getText().trim() : "";
        String password = txtPasswordCrear != null ? txtPasswordCrear.getText().trim() : "";
        Rol rol         = cbRolCrear       != null ? cbRolCrear.getValue()             : null;

        if (nombre.isEmpty() || username.isEmpty() || password.isEmpty() || rol == null) {
            alerta("Campos vacíos", "Completa todos los campos para crear el usuario.");
            return;
        }

        Usuario nuevo = new Usuario(0, nombre, username, password, rol);
        if (usuarioDAO.insertar(nuevo)) {
            info("Éxito", "Usuario creado correctamente.");
            limpiarFormularioCrear();
            cargarUsuarios();
        } else {
            alerta("Error", "No se pudo crear el usuario. El nombre de usuario puede ya existir.");
        }
    }

    @FXML
    public void actualizarUsuario() {
        if (seleccionado == null) { alerta("Sin selección", "Selecciona un usuario de la tabla."); return; }

        String nombre   = txtNombreEditar   != null ? txtNombreEditar.getText().trim()   : "";
        String username = txtUsernameEditar != null ? txtUsernameEditar.getText().trim() : "";
        String password = txtPasswordEditar != null ? txtPasswordEditar.getText().trim() : "";
        Rol rol         = cbRolEditar       != null ? cbRolEditar.getValue()             : null;

        if (nombre.isEmpty() || username.isEmpty() || password.isEmpty() || rol == null) {
            alerta("Campos vacíos", "Completa todos los campos para actualizar.");
            return;
        }

        // Proteger: no eliminar tu propia sesión de administrador
        if (seleccionado.getIdUsuario() == Sesion.getUsuarioLogueado().getIdUsuario()
                && !rol.getNombreRol().equalsIgnoreCase("Administrador")) {
            alerta("Acción no permitida", "No puedes cambiar tu propio rol.");
            return;
        }

        seleccionado.setNombre(nombre);
        seleccionado.setUsuario(username);
        seleccionado.setPassword(password);
        seleccionado.setRol(rol);

        if (usuarioDAO.actualizar(seleccionado)) {
            info("Éxito", "Usuario actualizado.");
            cargarUsuarios();
        } else {
            alerta("Error", "No se pudo actualizar el usuario.");
        }
    }

    @FXML
    public void eliminarUsuario() {
        if (seleccionado == null) { alerta("Sin selección", "Selecciona un usuario de la tabla."); return; }

        if (seleccionado.getIdUsuario() == Sesion.getUsuarioLogueado().getIdUsuario()) {
            alerta("Acción no permitida", "No puedes eliminar tu propio usuario.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Eliminar al usuario \"" + seleccionado.getNombre() + "\"?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirmar eliminación");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                if (usuarioDAO.eliminar(seleccionado.getIdUsuario())) {
                    info("Éxito", "Usuario eliminado.");
                    seleccionado = null;
                    cargarUsuarios();
                } else {
                    alerta("Error", "No se pudo eliminar el usuario.");
                }
            }
        });
    }

    private void poblarFormularioEditar(Usuario u) {
        if (txtNombreEditar   != null) txtNombreEditar.setText(u.getNombre());
        if (txtUsernameEditar != null) txtUsernameEditar.setText(u.getUsuario());
        if (txtPasswordEditar != null) txtPasswordEditar.setText(u.getPassword());
        if (cbRolEditar != null && u.getRol() != null) {
            cbRolEditar.getItems().stream()
                    .filter(r -> r.getIdRol() == u.getRol().getIdRol())
                    .findFirst().ifPresent(cbRolEditar::setValue);
        }
    }

    private void limpiarFormularioCrear() {
        if (txtNombreCrear   != null) txtNombreCrear.clear();
        if (txtUsernameCrear != null) txtUsernameCrear.clear();
        if (txtPasswordCrear != null) txtPasswordCrear.clear();
        if (cbRolCrear       != null) cbRolCrear.setValue(null);
    }

    private void alerta(String titulo, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(titulo); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    private void info(String titulo, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(titulo); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }
}