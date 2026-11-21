package cr.ac.tec.examen2.consola;

import cr.ac.tec.examen2.ElementoNoEncontradoException;
import cr.ac.tec.examen2.arbol.ArbolJerarquia;
import cr.ac.tec.examen2.arbol.RegistroSala;
import cr.ac.tec.examen2.arbol.Sala;
import cr.ac.tec.examen2.grafo.GrafoEvacuacion;
import cr.ac.tec.examen2.grafo.RutaEvacuacion;
import cr.ac.tec.examen2.informe.InformeBrigada;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;

/** Consola interactiva: el comando {@code alarma} simula la recepción de la señal de alarma. */
public final class Consola {
    private final ArbolJerarquia arbol;
    private final GrafoEvacuacion grafo;
    private final BufferedReader entrada;
    private final PrintStream salida;

    public Consola(ArbolJerarquia arbol, GrafoEvacuacion grafo, BufferedReader entrada, PrintStream salida) {
        this.arbol = arbol;
        this.grafo = grafo;
        this.entrada = entrada;
        this.salida = salida;
    }

    /** Procesa comandos hasta {@code salir} o el fin de la entrada. */
    public void ejecutar() throws IOException {
        salida.println("SalidaSegura - sistema de emergencia universitaria. Escriba 'ayuda' para ver los comandos.");
        while (true) {
            salida.print("> ");
            salida.flush();
            String linea = entrada.readLine();
            if (linea == null) {
                salida.println();
                return;
            }
            if (!procesar(linea.trim())) {
                return;
            }
        }
    }

    /** Devuelve falso cuando se pidió terminar. */
    private boolean procesar(String linea) {
        if (linea.isEmpty()) {
            return true;
        }
        int corte = 0;
        while (corte < linea.length() && !Character.isWhitespace(linea.charAt(corte))) {
            corte++;
        }
        String comando = linea.substring(0, corte);
        String resto = linea.substring(corte).trim();
        try {
            if (comando.equalsIgnoreCase("salir")) {
                salida.println("Hasta luego.");
                return false;
            } else if (comando.equalsIgnoreCase("ayuda")) {
                ayuda();
            } else if (comando.equalsIgnoreCase("conteos")) {
                conteos();
            } else if (comando.equalsIgnoreCase("salas")) {
                salas(argumentos(resto, 2, "salas <edificio>;<ala>"));
            } else if (comando.equalsIgnoreCase("actualizar")) {
                actualizar(argumentos(resto, 4, "actualizar <edificio>;<ala>;<letra>;<ocupantes>"));
            } else if (comando.equalsIgnoreCase("alarma")) {
                alarma();
            } else if (comando.equalsIgnoreCase("rutas")) {
                rutas();
            } else if (comando.equalsIgnoreCase("bloquear")) {
                bloquear(argumentos(resto, 2, "bloquear <punto A>;<punto B>"));
            } else {
                salida.println("Comando desconocido: '" + comando + "'. Escriba 'ayuda'.");
            }
        } catch (IllegalArgumentException | ElementoNoEncontradoException e) {
            salida.println("Error: " + e.getMessage());
        }
        return true;
    }

    private static String[] argumentos(String resto, int esperados, String uso) {
        String[] partes = resto.split(";", -1);
        if (partes.length != esperados) {
            throw new IllegalArgumentException("Uso: " + uso);
        }
        for (int i = 0; i < partes.length; i++) {
            partes[i] = partes[i].trim();
        }
        return partes;
    }

    private void ayuda() {
        salida.println("Comandos (los argumentos se separan con ';'):");
        salida.println("  alarma                                          simula la señal de alarma y muestra el reporte de brigada");
        salida.println("  conteos                                         ocupantes por ala y por edificio");
        salida.println("  salas <edificio>;<ala>                          salas registradas de un ala");
        salida.println("  actualizar <edificio>;<ala>;<letra>;<ocupantes> cambia los ocupantes de una sala");
        salida.println("  rutas                                           mejor salida, ruta y tiempo de cada sala");
        salida.println("  bloquear <punto A>;<punto B>                    bloquea un corredor");
        salida.println("  ayuda                                           muestra esta ayuda");
        salida.println("  salir                                           termina el programa");
    }

    private void conteos() {
        String[] edificios = arbol.edificios();
        if (edificios.length == 0) {
            salida.println("No hay edificios registrados.");
            return;
        }
        for (int e = 0; e < edificios.length; e++) {
            salida.println("Edificio " + edificios[e] + ": "
                    + InformeBrigada.ocupantes(arbol.totalEdificio(edificios[e])));
            String[] alas = arbol.alas(edificios[e]);
            for (int a = 0; a < alas.length; a++) {
                salida.println("  Ala " + alas[a] + ": "
                        + InformeBrigada.ocupantes(arbol.totalAla(edificios[e], alas[a])));
            }
        }
    }

    private void salas(String[] argumentos) {
        Sala[] salas = arbol.obtenerSalas(argumentos[0], argumentos[1]);
        salida.println("Salas del ala " + argumentos[1] + " del edificio " + argumentos[0] + ":");
        int mostradas = 0;
        for (int i = 0; i < salas.length; i++) {
            if (salas[i].registrada()) {
                salida.println("  " + salas[i].letra() + ": " + salas[i].codigo() + " ("
                        + InformeBrigada.ocupantes(salas[i].ocupantes()) + ")");
                mostradas++;
            }
        }
        if (mostradas == 0) {
            salida.println("  (sin salas registradas)");
        }
    }

    private void actualizar(String[] argumentos) {
        if (argumentos[2].length() != 1) {
            throw new IllegalArgumentException("La letra de la sala debe ser un solo carácter: '"
                    + argumentos[2] + "'.");
        }
        int ocupantes;
        try {
            ocupantes = Integer.parseInt(argumentos[3]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Cantidad de ocupantes inválida: '" + argumentos[3] + "'.");
        }
        arbol.actualizarOcupantes(argumentos[0], argumentos[1], argumentos[2].charAt(0), ocupantes);
        salida.println("Actualizado: " + argumentos[0] + "/" + argumentos[1] + "/"
                + Character.toUpperCase(argumentos[2].charAt(0)) + " = " + InformeBrigada.ocupantes(ocupantes));
    }

    private void alarma() {
        salida.println("*** ALARMA RECIBIDA ***");
        RegistroSala[] ordenadas = InformeBrigada.generar(arbol);
        salida.println(InformeBrigada.formatear(ordenadas));
    }

    private void rutas() {
        RutaEvacuacion[] rutas = grafo.rutasDeSalas();
        salida.println("Rutas de evacuación (tiempo de caminata en segundos):");
        for (int i = 0; i < rutas.length; i++) {
            salida.println("  " + rutas[i]);
        }
    }

    private void bloquear(String[] argumentos) {
        grafo.bloquearCorredor(argumentos[0], argumentos[1]);
        salida.println("Corredor bloqueado: " + argumentos[0] + " <-> " + argumentos[1]);
    }
}
