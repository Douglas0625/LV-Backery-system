package utils;

import model.Usuario;

public class Sesion {

    private static Usuario usuarioLogueado;

    public static Usuario getUsuarioLogueado() {
        return usuarioLogueado;
    }

    public static void setUsuarioLogueado(Usuario usuario) {
        usuarioLogueado = usuario;
    }

    public static void cerrarSesion() {
        usuarioLogueado = null;
    }
}