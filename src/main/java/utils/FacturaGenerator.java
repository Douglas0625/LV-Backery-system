package utils;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import model.DetalleVenta;
import model.Venta;

import java.awt.Desktop;
import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Genera facturas PDF usando src/main/resources/factura_template.html
 * Edita ese archivo para cambiar el diseño.
 */
public class FacturaGenerator {

    // ── Datos de empresa ─────────────────────────────────────────────
    private static final String EMPRESA_NOMBRE   = "LV Bakery";
    private static final String EMPRESA_SLOGAN   = "Panaderia y Reposteria Artesanal";
    private static final String EMPRESA_TELEFONO = "Tel: (503) 0000-0000";
    private static final String EMPRESA_EMAIL    = "contacto@lvbakery.com";
    private static final float  IVA_TASA         = 0.13f;
    // ─────────────────────────────────────────────────────────────────

    /** Genera el PDF y lo abre con el visor del sistema. */
    public static void generarYAbrir(int idVenta, Venta venta, List<DetalleVenta> detalles) {
        try {
            File pdf = Files.createTempFile("factura_" + idVenta + "_", ".pdf").toFile();
            pdf.deleteOnExit();
            generar(idVenta, venta, detalles, pdf.getAbsolutePath());
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN))
                Desktop.getDesktop().open(pdf);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error al generar la factura: " + e.getMessage(), e);
        }
    }

    // ── GENERACIÓN ───────────────────────────────────────────────────

    private static void generar(
            int idVenta,
            Venta venta,
            List<DetalleVenta> detalles,
            String rutaSalida
    ) throws Exception {

        String html = cargarPlantilla();

        html = reemplazarVariables(
                html,
                idVenta,
                venta,
                detalles
        );

        // DEBUG
        System.out.println(html);

        try (OutputStream os = new FileOutputStream(rutaSalida)) {

            PdfRendererBuilder builder =
                    new PdfRendererBuilder();

            builder.useFastMode();

            builder.withHtmlContent(html, null);

            builder.toStream(os);

            builder.run();
        }
    }

    // ── SUSTITUCIÓN ──────────────────────────────────────────────────

    private static String reemplazarVariables(String html, int idVenta,
                                               Venta venta, List<DetalleVenta> detalles) {
        BigDecimal subtotal = detalles.stream()
                .map(DetalleVenta::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal iva   = subtotal.multiply(BigDecimal.valueOf(IVA_TASA)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(iva).setScale(2, RoundingMode.HALF_UP);

        String fecha   = venta.getFechaVenta() != null
                ? venta.getFechaVenta().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "-";
        String cliente = (venta.getCliente() != null && venta.getCliente().getNombre() != null)
                ? venta.getCliente().getNombre() : "Consumidor Final";
        String tipo    = "PEDIDO".equals(venta.getTipoVenta()) ? "Venta por Pedido" : "Venta Directa";
        String pedRef  = venta.getIdPedido() != null ? "Pedido ref: #" + venta.getIdPedido() : "";
        String comp    = (venta.getNumeroComprobante() != null && !venta.getNumeroComprobante().isBlank())
                ? venta.getNumeroComprobante() : "-";

        html = html
                .replace("{{empresa_nombre}}",   EMPRESA_NOMBRE)
                .replace("{{empresa_slogan}}",   EMPRESA_SLOGAN)
                .replace("{{empresa_telefono}}", EMPRESA_TELEFONO)
                .replace("{{empresa_email}}",    EMPRESA_EMAIL)
                .replace("{{id_venta}}",         String.format("%06d", idVenta))
                .replace("{{tipo_venta}}",       tipo)
                .replace("{{fecha}}",            fecha)
                .replace("{{metodo_pago}}",      safe(venta.getMetodoPago()))
                .replace("{{comprobante}}",      comp)
                .replace("{{id_pedido}}",        pedRef)
                .replace("{{cliente_nombre}}",   cliente)
                .replace("{{subtotal}}",         fmt(subtotal))
                .replace("{{iva}}",              fmt(iva))
                .replace("{{total}}",            fmt(total));

        html = expandirBloqueProductos(html, detalles);
        return html;
    }

    private static String expandirBloqueProductos(String html, List<DetalleVenta> detalles) {
        int inicio = html.indexOf("{{#productos}}");
        int fin    = html.indexOf("{{/productos}}");
        if (inicio < 0 || fin < 0) return html;

        String bloque = html.substring(inicio + "{{#productos}}".length(), fin);
        StringBuilder filas = new StringBuilder();
        int n = 1;
        for (DetalleVenta dv : detalles) {
            filas.append(bloque
                    .replace("{{clase_fila}}",      n % 2 == 0 ? "par" : "")
                    .replace("{{numero}}",          String.valueOf(n++))
                    .replace("{{producto}}",         safe(dv.getNombreProducto()))
                    .replace("{{cantidad}}",         String.valueOf(dv.getCantidad()))
                    .replace("{{precio_unitario}}", fmt(dv.getPrecioUnitario()))
                    .replace("{{subtotal_item}}",   fmt(dv.getSubtotal())));
        }
        return html.substring(0, inicio) + filas + html.substring(fin + "{{/productos}}".length());
    }

    // ── CARGA DESDE CLASSPATH ────────────────────────────────────────

    private static String cargarPlantilla() throws IOException {
        try (InputStream in = FacturaGenerator.class
                .getClassLoader().getResourceAsStream("factura_template.html")) {
            if (in == null)
                throw new FileNotFoundException(
                        "No se encontro factura_template.html en src/main/resources/");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    // ── HELPERS ──────────────────────────────────────────────────────

    private static String fmt(BigDecimal v) {
        return v != null ? v.setScale(2, RoundingMode.HALF_UP).toPlainString() : "0.00";
    }
    private static String safe(String s) { return s != null ? s : ""; }
}
