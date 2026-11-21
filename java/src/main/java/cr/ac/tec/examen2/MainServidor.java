package cr.ac.tec.examen2;

import cr.ac.tec.examen2.arbol.ArbolJerarquia;
import cr.ac.tec.examen2.datos.CargadorCsv;
import cr.ac.tec.examen2.datos.CatalogoDatos;
import cr.ac.tec.examen2.grafo.GrafoEvacuacion;
import cr.ac.tec.examen2.servidor.ServidorHttp;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

/**
 * Arranca la API HTTP del backend. Argumentos opcionales, en orden: carpeta con ocupantes.csv y
 * pasillos.csv (por defecto {@code ../datos/version-b}), puerto (8080; 0 = libre) y origen CORS
 * permitido para el frontend ({@code *}).
 */
public class MainServidor {
    public static void main(String[] args) throws IOException {
        PrintStream salida = new PrintStream(System.out, true, "UTF-8");
        String carpeta = args.length > 0 ? args[0] : "../datos/version-b";
        String origenCors = args.length > 2 ? args[2] : "*";
        int puerto;
        try {
            puerto = args.length > 1 ? Integer.parseInt(args[1]) : 8080;
            if (puerto < 0 || puerto > 65535) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            salida.println("Puerto inválido: '" + args[1] + "'. Debe estar entre 0 y 65535.");
            System.exit(1);
            return;
        }
        ArbolJerarquia arbol = new ArbolJerarquia();
        GrafoEvacuacion grafo = new GrafoEvacuacion();
        try {
            CargadorCsv.cargarOcupantes(new File(carpeta, "ocupantes.csv").getPath(), arbol,
                    CargadorCsv.EDIFICIO_POR_DEFECTO);
            CargadorCsv.cargarPasillos(new File(carpeta, "pasillos.csv").getPath(), grafo,
                    CargadorCsv.codigosDeSalas(arbol));
        } catch (IOException | IllegalArgumentException e) {
            salida.println("No se pudieron cargar los datos: " + e.getMessage());
            System.exit(1);
            return;
        }
        File carpetaDatos = new File(carpeta).getAbsoluteFile();
        CatalogoDatos catalogo = new CatalogoDatos(carpetaDatos.getParent(), carpetaDatos.getName());
        ServidorHttp servidor = new ServidorHttp(arbol, grafo, puerto, origenCors, catalogo);
        try {
            servidor.iniciar();
        } catch (IOException e) {
            salida.println("No se pudo iniciar el servidor: " + e.getMessage());
            System.exit(1);
            return;
        }
        salida.println("API de SalidaSegura lista en http://127.0.0.1:" + servidor.puerto() + " (puerto "
                + servidor.puerto() + "). Origen CORS permitido: " + origenCors);
    }
}
