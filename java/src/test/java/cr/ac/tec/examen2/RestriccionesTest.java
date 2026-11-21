package cr.ac.tec.examen2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import org.junit.jupiter.api.Test;

/** Revisa que el codigo de produccion no use colecciones, ordenamientos de la libreria ni streams. */
class RestriccionesTest {

    private static final String[] PROHIBIDOS = {
        "ArrayList", "LinkedList", "HashMap", "HashSet", "TreeMap", "TreeSet", "PriorityQueue",
        "ArrayDeque", "Stack", "Vector", "Hashtable", "Collections", "Arrays.", "List<", "Map<",
        "Set<", "Queue<", "Deque<", "Iterator", ".sort(", ".sorted(", "java.util.", "java.util.stream",
        "Files.readAllLines", "Collectors", "Comparator"
    };

    private static void recolectar(File carpeta, String[] acumulado, int[] cantidad) {
        File[] hijos = carpeta.listFiles();
        assertTrue(hijos != null, "no se pudo leer " + carpeta);
        for (int i = 0; i < hijos.length; i++) {
            if (hijos[i].isDirectory()) {
                recolectar(hijos[i], acumulado, cantidad);
            } else if (hijos[i].getName().endsWith(".java")) {
                acumulado[cantidad[0]++] = hijos[i].getPath();
            }
        }
    }

    @Test
    void elCodigoDeProduccionNoUsaNadaProhibido() throws IOException {
        String[] archivos = new String[200];
        int[] cantidad = {0};
        recolectar(new File("src/main/java"), archivos, cantidad);
        assertTrue(cantidad[0] >= 10, "se esperaban los fuentes de la solucion");
        StringBuilder hallazgos = new StringBuilder();
        for (int f = 0; f < cantidad[0]; f++) {
            try (BufferedReader lector = new BufferedReader(
                    new InputStreamReader(new FileInputStream(archivos[f]), "UTF-8"))) {
                String linea;
                int numero = 0;
                while ((linea = lector.readLine()) != null) {
                    numero++;
                    for (int p = 0; p < PROHIBIDOS.length; p++) {
                        if (linea.contains(PROHIBIDOS[p])) {
                            hallazgos.append(archivos[f]).append(':').append(numero).append(" -> ")
                                    .append(PROHIBIDOS[p]).append('\n');
                        }
                    }
                }
            }
        }
        assertEquals("", hallazgos.toString());
    }
}
