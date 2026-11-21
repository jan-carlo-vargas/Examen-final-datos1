package cr.ac.tec.examen2.datos;

import cr.ac.tec.examen2.arbol.ArbolJerarquia;
import cr.ac.tec.examen2.grafo.GrafoEvacuacion;
import cr.ac.tec.examen2.grafo.TipoPunto;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Carga de los CSV de {@code datos/version-b}: {@code ocupantes.csv} (codigo,nombre,ala,ocupantes) y
 * {@code pasillos.csv} (desde,hacia,segundos,estado). Formato simple separado por comas, UTF-8, sin
 * campos entre comillas. Ante un error se lanza una excepción con el número de línea; las filas
 * anteriores ya cargadas permanecen.
 */
public final class CargadorCsv {
    /** El CSV de ocupantes no trae edificio: todas sus salas se asignan a este edificio. */
    public static final String EDIFICIO_POR_DEFECTO = "Principal";
    /** Los puntos cuyo nombre empieza así (sin distinguir mayúsculas) son salidas. */
    public static final String PREFIJO_SALIDA = "Salida";

    private static final String ENCABEZADO_OCUPANTES = "codigo,nombre,ala,ocupantes";
    /** Variante con edificio por fila (campus de varios edificios). */
    private static final String ENCABEZADO_OCUPANTES_EDIFICIO = "codigo,nombre,edificio,ala,ocupantes";
    private static final String ENCABEZADO_PASILLOS = "desde,hacia,segundos,estado";

    private CargadorCsv() {
    }

    /**
     * Carga las salas en el árbol (letras A, B, C... por orden de aparición en cada ala); devuelve las filas leídas.
     * Acepta dos encabezados: {@code codigo,nombre,ala,ocupantes} (todas las salas van al edificio dado) o
     * {@code codigo,nombre,edificio,ala,ocupantes} (cada fila indica su edificio).
     */
    public static int cargarOcupantes(String ruta, ArbolJerarquia arbol, String edificio) throws IOException {
        int cargadas = 0;
        try (BufferedReader lector = abrir(ruta)) {
            boolean conEdificio = leerEncabezadoOcupantes(lector);
            int columnas = conEdificio ? 5 : 4;
            String linea;
            int numero = 1;
            while ((linea = lector.readLine()) != null) {
                numero++;
                if (linea.trim().isEmpty()) {
                    continue;
                }
                String[] campos = campos(linea, columnas, numero);
                String nombreEdificio = conEdificio ? campos[2] : edificio;
                String ala = campos[columnas - 2];
                try {
                    int ocupantes = Integer.parseInt(campos[columnas - 1]);
                    ArbolJerarquia.validarOcupantes(ocupantes);
                    char letra = arbol.registrarSala(nombreEdificio, ala, campos[0]);
                    arbol.actualizarOcupantes(nombreEdificio, ala, letra, ocupantes);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Línea " + numero + ": ocupantes no numéricos: '"
                            + campos[columnas - 1] + "'.");
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Línea " + numero + ": " + e.getMessage());
                }
                cargadas++;
            }
        }
        return cargadas;
    }

    /**
     * Carga el grafo: primero registra las salas dadas (aunque no tengan corredores) y luego los
     * puntos y corredores del archivo. Los corredores "Bloqueado" se agregan y se eliminan de
     * inmediato, de modo que el grafo queda sin esa arista. Devuelve las filas leídas.
     */
    public static int cargarPasillos(String ruta, GrafoEvacuacion grafo, String[] codigosSalas) throws IOException {
        for (int i = 0; i < codigosSalas.length; i++) {
            grafo.agregarPunto(codigosSalas[i], TipoPunto.SALA);
        }
        int cargadas = 0;
        try (BufferedReader lector = abrir(ruta)) {
            leerEncabezado(lector, ENCABEZADO_PASILLOS);
            String linea;
            int numero = 1;
            while ((linea = lector.readLine()) != null) {
                numero++;
                if (linea.trim().isEmpty()) {
                    continue;
                }
                String[] campos = campos(linea, 4, numero);
                try {
                    int segundos = Integer.parseInt(campos[2]);
                    boolean bloqueado;
                    if (campos[3].equalsIgnoreCase("Abierto")) {
                        bloqueado = false;
                    } else if (campos[3].equalsIgnoreCase("Bloqueado")) {
                        bloqueado = true;
                    } else {
                        throw new IllegalArgumentException("estado desconocido: '" + campos[3]
                                + "' (se esperaba Abierto o Bloqueado).");
                    }
                    asegurarPunto(grafo, campos[0]);
                    asegurarPunto(grafo, campos[1]);
                    grafo.agregarCorredor(campos[0], campos[1], segundos);
                    if (bloqueado) {
                        grafo.bloquearCorredor(campos[0], campos[1]);
                    }
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Línea " + numero + ": segundos no numéricos: '"
                            + campos[2] + "'.");
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Línea " + numero + ": " + e.getMessage());
                }
                cargadas++;
            }
        }
        return cargadas;
    }

    /** Códigos de todas las salas del árbol, en orden de recorrido (las que se registran como salas del grafo). */
    public static String[] codigosDeSalas(ArbolJerarquia arbol) {
        cr.ac.tec.examen2.arbol.RegistroSala[] salas = arbol.recolectarSalas();
        String[] codigos = new String[salas.length];
        for (int i = 0; i < salas.length; i++) {
            codigos[i] = salas[i].codigo();
        }
        return codigos;
    }

    private static void asegurarPunto(GrafoEvacuacion grafo, String nombre) {
        if (nombre.isEmpty()) {
            throw new IllegalArgumentException("el nombre del punto está vacío.");
        }
        if (!grafo.existePunto(nombre)) {
            boolean salida = nombre.regionMatches(true, 0, PREFIJO_SALIDA, 0, PREFIJO_SALIDA.length());
            grafo.agregarPunto(nombre, salida ? TipoPunto.SALIDA : TipoPunto.PASILLO);
        }
    }

    private static BufferedReader abrir(String ruta) throws IOException {
        return new BufferedReader(new InputStreamReader(new FileInputStream(ruta), "UTF-8"));
    }

    /** Lee el encabezado de ocupantes; devuelve true si trae la columna de edificio. */
    private static boolean leerEncabezadoOcupantes(BufferedReader lector) throws IOException {
        String linea = lector.readLine();
        if (linea != null && linea.length() > 0 && linea.charAt(0) == '﻿') {
            linea = linea.substring(1);
        }
        if (linea != null && linea.trim().equals(ENCABEZADO_OCUPANTES_EDIFICIO)) {
            return true;
        }
        if (linea == null || !linea.trim().equals(ENCABEZADO_OCUPANTES)) {
            throw new IllegalArgumentException("Encabezado inválido: se esperaba '" + ENCABEZADO_OCUPANTES + "' o '"
                    + ENCABEZADO_OCUPANTES_EDIFICIO + "' pero se leyó '"
                    + (linea == null ? "(archivo vacío)" : linea.trim()) + "'.");
        }
        return false;
    }

    private static void leerEncabezado(BufferedReader lector, String esperado) throws IOException {
        String linea = lector.readLine();
        if (linea != null && linea.length() > 0 && linea.charAt(0) == '﻿') {
            linea = linea.substring(1);
        }
        if (linea == null || !linea.trim().equals(esperado)) {
            throw new IllegalArgumentException("Encabezado inválido: se esperaba '" + esperado + "' pero se leyó '"
                    + (linea == null ? "(archivo vacío)" : linea.trim()) + "'.");
        }
    }

    private static String[] campos(String linea, int esperados, int numero) {
        String[] campos = linea.split(",", -1);
        if (campos.length != esperados) {
            throw new IllegalArgumentException("Línea " + numero + ": se esperaban " + esperados
                    + " columnas pero hay " + campos.length + ".");
        }
        for (int i = 0; i < campos.length; i++) {
            campos[i] = campos[i].trim();
        }
        return campos;
    }
}
