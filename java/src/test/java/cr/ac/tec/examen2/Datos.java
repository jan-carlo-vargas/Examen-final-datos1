package cr.ac.tec.examen2;

import cr.ac.tec.examen2.arbol.ArbolJerarquia;
import cr.ac.tec.examen2.datos.CargadorCsv;
import cr.ac.tec.examen2.grafo.GrafoEvacuacion;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

/** Utilidades compartidas por las pruebas (rutas relativas a la carpeta java/, desde donde corre Maven). */
public final class Datos {
    public static final String OCUPANTES = "../datos/version-b/ocupantes.csv";
    public static final String PASILLOS = "../datos/version-b/pasillos.csv";

    private Datos() {
    }

    public static ArbolJerarquia arbolDeCsv() throws IOException {
        ArbolJerarquia arbol = new ArbolJerarquia();
        CargadorCsv.cargarOcupantes(OCUPANTES, arbol, CargadorCsv.EDIFICIO_POR_DEFECTO);
        return arbol;
    }

    public static String[] codigos(ArbolJerarquia arbol) {
        cr.ac.tec.examen2.arbol.RegistroSala[] salas = arbol.recolectarSalas();
        String[] codigos = new String[salas.length];
        for (int i = 0; i < salas.length; i++) {
            codigos[i] = salas[i].codigo();
        }
        return codigos;
    }

    public static GrafoEvacuacion grafoDeCsv(ArbolJerarquia arbol) throws IOException {
        GrafoEvacuacion grafo = new GrafoEvacuacion();
        CargadorCsv.cargarPasillos(PASILLOS, grafo, codigos(arbol));
        return grafo;
    }

    /** Escribe un archivo temporal UTF-8 que se borra al terminar la JVM y devuelve su ruta. */
    public static String temporal(String contenido) throws IOException {
        File archivo = File.createTempFile("salidasegura", ".csv");
        archivo.deleteOnExit();
        try (Writer escritor = new OutputStreamWriter(new FileOutputStream(archivo), "UTF-8")) {
            escritor.write(contenido);
        }
        return archivo.getAbsolutePath();
    }
}
