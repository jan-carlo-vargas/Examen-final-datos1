package cr.ac.tec.examen2;

import cr.ac.tec.examen2.arbol.ArbolJerarquia;
import cr.ac.tec.examen2.arbol.RegistroSala;
import cr.ac.tec.examen2.consola.Consola;
import cr.ac.tec.examen2.datos.CargadorCsv;
import cr.ac.tec.examen2.grafo.GrafoEvacuacion;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;

/**
 * Punto de entrada de SalidaSegura. Argumento opcional: carpeta con ocupantes.csv y pasillos.csv
 * (por defecto {@code ../datos/version-b}, pensada para ejecutarse desde la carpeta java/).
 */
public class Main {
    public static void main(String[] args) throws IOException {
        PrintStream salida = new PrintStream(System.out, true, "UTF-8");
        String carpeta = args.length > 0 ? args[0] : "../datos/version-b";
        ArbolJerarquia arbol = new ArbolJerarquia();
        GrafoEvacuacion grafo = new GrafoEvacuacion();
        try {
            int salas = CargadorCsv.cargarOcupantes(new File(carpeta, "ocupantes.csv").getPath(), arbol,
                    CargadorCsv.EDIFICIO_POR_DEFECTO);
            int corredores = CargadorCsv.cargarPasillos(new File(carpeta, "pasillos.csv").getPath(), grafo,
                    codigosDeSalas(arbol));
            salida.println("Datos cargados desde " + carpeta + ": " + salas + " salas y " + corredores
                    + " corredores.");
        } catch (IOException | IllegalArgumentException e) {
            salida.println("No se pudieron cargar los datos: " + e.getMessage());
            System.exit(1);
            return;
        }
        BufferedReader entrada = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));
        new Consola(arbol, grafo, entrada, salida).ejecutar();
    }

    /** Códigos de todas las salas del árbol, en orden de recorrido. */
    private static String[] codigosDeSalas(ArbolJerarquia arbol) {
        RegistroSala[] salas = arbol.recolectarSalas();
        String[] codigos = new String[salas.length];
        for (int i = 0; i < salas.length; i++) {
            codigos[i] = salas[i].codigo();
        }
        return codigos;
    }
}
