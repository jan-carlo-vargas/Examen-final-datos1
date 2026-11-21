package cr.ac.tec.examen2.servidor;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import cr.ac.tec.examen2.ElementoNoEncontradoException;
import cr.ac.tec.examen2.arbol.ArbolJerarquia;
import cr.ac.tec.examen2.arbol.RegistroSala;
import cr.ac.tec.examen2.arbol.Sala;
import cr.ac.tec.examen2.datos.CatalogoDatos;
import cr.ac.tec.examen2.grafo.Corredor;
import cr.ac.tec.examen2.grafo.GrafoEvacuacion;
import cr.ac.tec.examen2.grafo.RutaEvacuacion;
import cr.ac.tec.examen2.informe.InformeBrigada;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * API HTTP/JSON sobre el backend existente, con el servidor incluido en el JDK (sin dependencias).
 * Solo traduce: toda la lógica (árbol B+, Counting Sort, grafo, Dijkstra) sigue en los módulos
 * del proyecto. Escucha solo en la interfaz local. Las solicitudes se atienden una a una.
 *
 * <pre>
 * GET  /api/edificios                               edificios, alas y totales
 * GET  /api/salas?edificio=&amp;ala=                      salas registradas de un ala
 * POST /api/ocupantes?edificio=&amp;ala=&amp;letra=&amp;ocupantes= actualiza ocupantes
 * POST /api/alarma                                  reporte de brigada ordenado
 * GET  /api/rutas                                   salidas y mejor ruta de cada sala
 * GET  /api/pasillos                                corredores con estado Abierto/Bloqueado
 * POST /api/pasillos/bloquear?desde=&amp;hacia=
 * POST /api/pasillos/desbloquear?desde=&amp;hacia=
 * GET  /api/datos                                  conjuntos de datos disponibles y el actual
 * POST /api/datos/cargar?conjunto=                  reemplaza los datos por otro conjunto (descarta los cambios)
 * </pre>
 */
public final class ServidorHttp {
    private ArbolJerarquia arbol;
    private GrafoEvacuacion grafo;
    private final CatalogoDatos catalogo;
    private final int puertoSolicitado;
    private final String origenCors;
    private HttpServer servidor;

    /** @param puerto 0 para que el sistema elija uno libre */
    public ServidorHttp(ArbolJerarquia arbol, GrafoEvacuacion grafo, int puerto, String origenCors) {
        this(arbol, grafo, puerto, origenCors, null);
    }

    /** Con catálogo, la API puede cambiar de conjunto de datos ({@code /api/datos}). */
    public ServidorHttp(ArbolJerarquia arbol, GrafoEvacuacion grafo, int puerto, String origenCors,
            CatalogoDatos catalogo) {
        this.catalogo = catalogo;
        this.arbol = arbol;
        this.grafo = grafo;
        this.puertoSolicitado = puerto;
        this.origenCors = origenCors;
    }

    public void iniciar() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), puertoSolicitado), 0);
        servidor.createContext("/", this::manejar);
        servidor.setExecutor(null);
        servidor.start();
    }

    public void detener() {
        if (servidor != null) {
            servidor.stop(0);
        }
    }

    public int puerto() {
        return servidor.getAddress().getPort();
    }

    // ---------- manejo de solicitudes ----------

    private static final class MetodoNoPermitido extends RuntimeException {
        private static final long serialVersionUID = 1L;
        final String permitido;

        MetodoNoPermitido(String permitido) {
            super("Método no permitido; use " + permitido + ".");
            this.permitido = permitido;
        }
    }

    private void manejar(HttpExchange intercambio) throws IOException {
        try {
            intercambio.getResponseHeaders().set("Access-Control-Allow-Origin", origenCors);
            if (intercambio.getRequestMethod().equals("OPTIONS")) {
                intercambio.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
                intercambio.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
                intercambio.getResponseHeaders().set("Access-Control-Max-Age", "600");
                intercambio.sendResponseHeaders(204, -1);
                return;
            }
            String cuerpo;
            synchronized (this) {
                cuerpo = despachar(intercambio);
            }
            responder(intercambio, 200, cuerpo);
        } catch (MetodoNoPermitido e) {
            intercambio.getResponseHeaders().set("Allow", e.permitido);
            responder(intercambio, 405, error(e.getMessage()));
        } catch (ElementoNoEncontradoException e) {
            responder(intercambio, 404, error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            responder(intercambio, 400, error(e.getMessage()));
        } catch (RuntimeException e) {
            responder(intercambio, 500, error("Error interno del servidor."));
        } finally {
            intercambio.close();
        }
    }

    private static void responder(HttpExchange intercambio, int estado, String cuerpo) throws IOException {
        byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
        intercambio.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        intercambio.sendResponseHeaders(estado, bytes.length);
        try (OutputStream salida = intercambio.getResponseBody()) {
            salida.write(bytes);
        }
    }

    private static String error(String mensaje) {
        return "{\"error\":" + Json.texto(mensaje) + "}";
    }

    private String despachar(HttpExchange intercambio) {
        String ruta = intercambio.getRequestURI().getPath();
        String metodo = intercambio.getRequestMethod();
        Consulta consulta = new Consulta(intercambio.getRequestURI().getRawQuery());
        switch (ruta) {
            case "/api/edificios":
                exigir(metodo, "GET");
                return edificios();
            case "/api/salas":
                exigir(metodo, "GET");
                return salas(consulta.obligatorio("edificio"), consulta.obligatorio("ala"));
            case "/api/ocupantes":
                exigir(metodo, "POST");
                return actualizarOcupantes(consulta);
            case "/api/alarma":
                exigir(metodo, "POST");
                return alarma();
            case "/api/rutas":
                exigir(metodo, "GET");
                return rutas();
            case "/api/pasillos":
                exigir(metodo, "GET");
                return pasillos();
            case "/api/pasillos/bloquear":
                exigir(metodo, "POST");
                grafo.bloquearCorredor(consulta.obligatorio("desde"), consulta.obligatorio("hacia"));
                return corredor(grafo.buscarCorredor(consulta.obligatorio("desde"), consulta.obligatorio("hacia")));
            case "/api/pasillos/desbloquear":
                exigir(metodo, "POST");
                grafo.desbloquearCorredor(consulta.obligatorio("desde"), consulta.obligatorio("hacia"));
                return corredor(grafo.buscarCorredor(consulta.obligatorio("desde"), consulta.obligatorio("hacia")));
            case "/api/datos":
                exigir(metodo, "GET");
                return datos();
            case "/api/datos/cargar":
                exigir(metodo, "POST");
                return cargarDatos(consulta.obligatorio("conjunto"));
            default:
                throw new ElementoNoEncontradoException("Ruta no encontrada: " + ruta);
        }
    }

    private static void exigir(String metodo, String permitido) {
        if (!metodo.equals(permitido)) {
            throw new MetodoNoPermitido(permitido);
        }
    }

    /** Parámetros de la consulta ya decodificados (UTF-8). */
    private static final class Consulta {
        private final String[] claves;
        private final String[] valores;
        private final int cantidad;

        Consulta(String cruda) {
            if (cruda == null || cruda.isEmpty()) {
                claves = new String[0];
                valores = new String[0];
                cantidad = 0;
                return;
            }
            String[] partes = cruda.split("&", -1);
            claves = new String[partes.length];
            valores = new String[partes.length];
            int n = 0;
            for (int i = 0; i < partes.length; i++) {
                if (partes[i].isEmpty()) {
                    continue;
                }
                String[] par = partes[i].split("=", 2);
                claves[n] = URLDecoder.decode(par[0], StandardCharsets.UTF_8);
                valores[n] = par.length == 2 ? URLDecoder.decode(par[1], StandardCharsets.UTF_8) : "";
                n++;
            }
            cantidad = n;
        }

        String obligatorio(String clave) {
            for (int i = 0; i < cantidad; i++) {
                if (claves[i].equals(clave)) {
                    return valores[i];
                }
            }
            throw new IllegalArgumentException("Falta el parámetro '" + clave + "'.");
        }
    }

    // ---------- respuestas ----------

    private String datos() {
        StringBuilder sb = new StringBuilder("{\"actual\":");
        sb.append(catalogo == null ? "null" : Json.texto(catalogo.actual())).append(",\"conjuntos\":[");
        String[] nombres = catalogo == null ? new String[0] : catalogo.nombres();
        for (int i = 0; i < nombres.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(Json.texto(nombres[i]));
        }
        return sb.append("]}").toString();
    }

    private String cargarDatos(String conjunto) {
        if (catalogo == null) {
            throw new ElementoNoEncontradoException("No hay un catálogo de conjuntos de datos configurado.");
        }
        CatalogoDatos.ConjuntoDatos nuevo = catalogo.cargar(conjunto);
        arbol = nuevo.arbol();
        grafo = nuevo.grafo();
        return "{\"actual\":" + Json.texto(nuevo.nombre()) + ",\"salas\":" + nuevo.salas() + "}";
    }

    private String edificios() {
        StringBuilder sb = new StringBuilder("{\"edificios\":[");
        String[] edificios = arbol.edificios();
        for (int e = 0; e < edificios.length; e++) {
            if (e > 0) {
                sb.append(',');
            }
            sb.append("{\"nombre\":").append(Json.texto(edificios[e]))
                    .append(",\"totalOcupantes\":").append(arbol.totalEdificio(edificios[e])).append(",\"alas\":[");
            String[] alas = arbol.alas(edificios[e]);
            for (int a = 0; a < alas.length; a++) {
                if (a > 0) {
                    sb.append(',');
                }
                sb.append("{\"nombre\":").append(Json.texto(alas[a]))
                        .append(",\"totalOcupantes\":").append(arbol.totalAla(edificios[e], alas[a])).append('}');
            }
            sb.append("]}");
        }
        return sb.append("]}").toString();
    }

    private String salas(String edificio, String ala) {
        Sala[] salas = arbol.obtenerSalas(edificio, ala);
        StringBuilder sb = new StringBuilder("{\"edificio\":").append(Json.texto(edificio))
                .append(",\"ala\":").append(Json.texto(ala))
                .append(",\"capacidad\":").append(salas.length)
                .append(",\"totalOcupantes\":").append(arbol.totalAla(edificio, ala)).append(",\"salas\":[");
        boolean primera = true;
        for (int i = 0; i < salas.length; i++) {
            if (!salas[i].registrada()) {
                continue;
            }
            if (!primera) {
                sb.append(',');
            }
            primera = false;
            sb.append("{\"letra\":").append(Json.texto(String.valueOf(salas[i].letra())))
                    .append(",\"codigo\":").append(Json.texto(salas[i].codigo()))
                    .append(",\"ocupantes\":").append(salas[i].ocupantes()).append('}');
        }
        return sb.append("]}").toString();
    }

    private String actualizarOcupantes(Consulta consulta) {
        String edificio = consulta.obligatorio("edificio");
        String ala = consulta.obligatorio("ala");
        String letraTexto = consulta.obligatorio("letra");
        String ocupantesTexto = consulta.obligatorio("ocupantes");
        if (letraTexto.length() != 1) {
            throw new IllegalArgumentException("La letra de la sala debe ser un solo carácter: '" + letraTexto + "'.");
        }
        int ocupantes;
        try {
            ocupantes = Integer.parseInt(ocupantesTexto.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Cantidad de ocupantes inválida: '" + ocupantesTexto + "'.");
        }
        char letra = letraTexto.charAt(0);
        arbol.actualizarOcupantes(edificio, ala, letra, ocupantes);
        String codigo = arbol.obtenerSalas(edificio, ala)[ArbolJerarquia.indiceLetra(letra)].codigo();
        return "{\"edificio\":" + Json.texto(edificio) + ",\"ala\":" + Json.texto(ala)
                + ",\"letra\":" + Json.texto(String.valueOf(Character.toUpperCase(letra)))
                + ",\"codigo\":" + Json.texto(codigo) + ",\"ocupantes\":" + ocupantes
                + ",\"totalAla\":" + arbol.totalAla(edificio, ala)
                + ",\"totalEdificio\":" + arbol.totalEdificio(edificio) + "}";
    }

    private String alarma() {
        RegistroSala[] orden = InformeBrigada.generar(arbol);
        StringBuilder sb = new StringBuilder("{\"totalSalas\":").append(orden.length).append(",\"salas\":[");
        for (int i = 0; i < orden.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append("{\"posicion\":").append(i + 1)
                    .append(",\"edificio\":").append(Json.texto(orden[i].edificio()))
                    .append(",\"ala\":").append(Json.texto(orden[i].ala()))
                    .append(",\"letra\":").append(Json.texto(String.valueOf(orden[i].letra())))
                    .append(",\"codigo\":").append(Json.texto(orden[i].codigo()))
                    .append(",\"ocupantes\":").append(orden[i].ocupantes()).append('}');
        }
        return sb.append("]}").toString();
    }

    private String rutas() {
        StringBuilder sb = new StringBuilder("{\"salidas\":[");
        String[] salidas = grafo.nombresSalidas();
        for (int i = 0; i < salidas.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(Json.texto(salidas[i]));
        }
        sb.append("],\"rutas\":[");
        RutaEvacuacion[] rutas = grafo.rutasDeSalas();
        for (int i = 0; i < rutas.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            RutaEvacuacion r = rutas[i];
            sb.append("{\"sala\":").append(Json.texto(r.origen()))
                    .append(",\"alcanzable\":").append(r.alcanzable())
                    .append(",\"salida\":").append(Json.texto(r.salida()))
                    .append(",\"tiempoSegundos\":").append(r.alcanzable() ? String.valueOf(r.tiempoSegundos()) : "null")
                    .append(",\"ruta\":[");
            String[] puntos = r.ruta();
            for (int p = 0; p < puntos.length; p++) {
                if (p > 0) {
                    sb.append(',');
                }
                sb.append(Json.texto(puntos[p]));
            }
            sb.append("]}");
        }
        return sb.append("]}").toString();
    }

    private String pasillos() {
        Corredor[] lista = grafo.listarCorredores();
        StringBuilder sb = new StringBuilder("{\"pasillos\":[");
        for (int i = 0; i < lista.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(corredor(lista[i]));
        }
        return sb.append("]}").toString();
    }

    private static String corredor(Corredor c) {
        return "{\"desde\":" + Json.texto(c.desde()) + ",\"hacia\":" + Json.texto(c.hacia())
                + ",\"segundos\":" + c.segundos()
                + ",\"estado\":" + Json.texto(c.bloqueado() ? "Bloqueado" : "Abierto") + "}";
    }
}
