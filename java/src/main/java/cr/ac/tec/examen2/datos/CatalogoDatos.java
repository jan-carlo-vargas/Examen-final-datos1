package cr.ac.tec.examen2.datos;

import cr.ac.tec.examen2.ElementoNoEncontradoException;
import cr.ac.tec.examen2.arbol.ArbolJerarquia;
import cr.ac.tec.examen2.grafo.GrafoEvacuacion;
import java.io.File;
import java.io.IOException;

/**
 * Conjuntos de datos disponibles: subcarpetas de una carpeta base que contienen {@code ocupantes.csv} y
 * {@code pasillos.csv}. Solo se puede cargar un nombre que aparezca en la lista (no se aceptan rutas),
 * y si la carga falla el conjunto actual no cambia.
 */
public final class CatalogoDatos {
    /** Resultado de cargar un conjunto: estructuras nuevas, listas para usar. */
    public static final class ConjuntoDatos {
        private final String nombre;
        private final ArbolJerarquia arbol;
        private final GrafoEvacuacion grafo;

        ConjuntoDatos(String nombre, ArbolJerarquia arbol, GrafoEvacuacion grafo) {
            this.nombre = nombre;
            this.arbol = arbol;
            this.grafo = grafo;
        }

        public String nombre() {
            return nombre;
        }

        public ArbolJerarquia arbol() {
            return arbol;
        }

        public GrafoEvacuacion grafo() {
            return grafo;
        }

        public int salas() {
            return arbol.cantidadSalas();
        }
    }

    private final File base;
    private String actual;

    public CatalogoDatos(String carpetaBase, String actual) {
        this.base = new File(carpetaBase);
        this.actual = actual;
    }

    public String actual() {
        return actual;
    }

    /** Nombres de los conjuntos disponibles, en orden alfabético. */
    public String[] nombres() {
        String[] hijos = base.list();
        if (hijos == null) {
            return new String[0];
        }
        String[] validos = new String[hijos.length];
        int n = 0;
        for (int i = 0; i < hijos.length; i++) {
            File carpeta = new File(base, hijos[i]);
            if (carpeta.isDirectory() && new File(carpeta, "ocupantes.csv").isFile()
                    && new File(carpeta, "pasillos.csv").isFile()) {
                // inserción ordenada
                int posicion = n;
                while (posicion > 0 && validos[posicion - 1].compareTo(hijos[i]) > 0) {
                    validos[posicion] = validos[posicion - 1];
                    posicion--;
                }
                validos[posicion] = hijos[i];
                n++;
            }
        }
        String[] resultado = new String[n];
        for (int i = 0; i < n; i++) {
            resultado[i] = validos[i];
        }
        return resultado;
    }

    /** Carga un conjunto de la lista. Lanza {@link ElementoNoEncontradoException} si no existe. */
    public ConjuntoDatos cargar(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del conjunto de datos está vacío.");
        }
        String[] disponibles = nombres();
        boolean existe = false;
        for (int i = 0; i < disponibles.length; i++) {
            if (disponibles[i].equals(nombre)) {
                existe = true;
            }
        }
        if (!existe) {
            throw new ElementoNoEncontradoException("No existe el conjunto de datos '" + nombre + "'.");
        }
        File carpeta = new File(base, nombre);
        ArbolJerarquia arbol = new ArbolJerarquia();
        GrafoEvacuacion grafo = new GrafoEvacuacion();
        try {
            CargadorCsv.cargarOcupantes(new File(carpeta, "ocupantes.csv").getPath(), arbol,
                    CargadorCsv.EDIFICIO_POR_DEFECTO);
            CargadorCsv.cargarPasillos(new File(carpeta, "pasillos.csv").getPath(), grafo,
                    CargadorCsv.codigosDeSalas(arbol));
        } catch (IOException e) {
            throw new IllegalArgumentException("No se pudo leer el conjunto '" + nombre + "': " + e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("El conjunto '" + nombre + "' no es válido: " + e.getMessage());
        }
        actual = nombre;
        return new ConjuntoDatos(nombre, arbol, grafo);
    }
}
