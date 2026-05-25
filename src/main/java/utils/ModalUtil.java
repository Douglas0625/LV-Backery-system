package utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.net.URL;
import java.util.function.Consumer;

public class ModalUtil {

    /**
     * Abre un FXML como ventana modal.
     * Usa el ClassLoader de la aplicación para resolver rutas absolutas (/views/...).
     */
    public static <T> T abrir(String fxmlPath, String titulo, Window owner, Consumer<T> setup) {
        try {
            // Quitar la / inicial para usar getResourceAsStream del classloader de la app
            String path = fxmlPath.startsWith("/") ? fxmlPath.substring(1) : fxmlPath;
            URL url = Thread.currentThread().getContextClassLoader().getResource(path);

            if (url == null) {
                System.err.println("[ModalUtil] FXML no encontrado: " + path);
                error("FXML no encontrado: " + path);
                return null;
            }

            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();
            T controller = loader.getController();

            if (setup != null) setup.accept(controller);

            Stage stage = new Stage();
            stage.setTitle(titulo);
            stage.initModality(Modality.WINDOW_MODAL);
            if (owner != null) stage.initOwner(owner);
            stage.setResizable(false);
            stage.setScene(new Scene(root));
            stage.showAndWait();

            return controller;

        } catch (Exception e) {
            e.printStackTrace();
            error(e.getClass().getSimpleName() + ":\n" + e.getMessage());
            return null;
        }
    }

    public static <T> T abrir(String fxmlPath, String titulo, Window owner) {
        return abrir(fxmlPath, titulo, owner, null);
    }

    private static void error(String msg) {
        javafx.scene.control.Alert a = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.ERROR);
        a.setTitle("Error al abrir modal");
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}