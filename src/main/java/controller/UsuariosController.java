package controller;

import dao.UsuarioDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import model.Usuario;

import java.net.URL;
import java.util.ResourceBundle;

public class UsuariosController implements Initializable {

    @FXML
    private TableView<Usuario> tablaUsuarios;

    @FXML
    private TableColumn<Usuario, Integer> colId;

    @FXML
    private TableColumn<Usuario, String> colNombre;

    @FXML
    private TableColumn<Usuario, String> colUsername;

    @FXML
    private TableColumn<Usuario, String> colRol;

    private final UsuarioDAO usuarioDAO =
            new UsuarioDAO();

    @Override
    public void initialize(URL url,
                           ResourceBundle resourceBundle) {

        configurarColumnas();

        cargarUsuarios();
    }

    private void configurarColumnas() {

        colId.setCellValueFactory(
                new PropertyValueFactory<>("idUsuario")
        );

        colNombre.setCellValueFactory(
                new PropertyValueFactory<>("nombre")
        );

        colUsername.setCellValueFactory(
                new PropertyValueFactory<>("username")
        );

        colRol.setCellValueFactory(
                new PropertyValueFactory<>("rol")
        );
    }

    private void cargarUsuarios() {

        ObservableList<Usuario> lista =
                FXCollections.observableArrayList(
                        usuarioDAO.listarUsuarios()
                );

        tablaUsuarios.setItems(lista);
    }
}
