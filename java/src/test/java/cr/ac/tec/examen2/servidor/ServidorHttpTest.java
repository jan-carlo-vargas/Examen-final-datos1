package cr.ac.tec.examen2.servidor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cr.ac.tec.examen2.Datos;
import cr.ac.tec.examen2.arbol.ArbolJerarquia;
import cr.ac.tec.examen2.grafo.GrafoEvacuacion;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import org.junit.jupiter.api.Test;

/** Pruebas de la API HTTP contra un servidor real en un puerto efímero, con los CSV suministrados. */
class ServidorHttpTest {

    private interface Cuerpo {
        void ejecutar(ServidorHttp servidor) throws Exception;
    }

    private static final class Respuesta {
        int estado;
        String cuerpo;
        String origenCors;
        String metodosCors;
        String tipo;
        String permitidos;
    }

    private static void conServidor(String cors, Cuerpo cuerpo) throws Exception {
        ArbolJerarquia arbol = Datos.arbolDeCsv();
        GrafoEvacuacion grafo = Datos.grafoDeCsv(arbol);
        ServidorHttp servidor = new ServidorHttp(arbol, grafo, 0, cors);
        servidor.iniciar();
        try {
            cuerpo.ejecutar(servidor);
        } finally {
            servidor.detener();
        }
    }

    private static void conServidor(Cuerpo cuerpo) throws Exception {
        conServidor("*", cuerpo);
    }

    private static Respuesta pedir(ServidorHttp servidor, String metodo, String rutaYConsulta) throws IOException {
        URL url = new URL("http://127.0.0.1:" + servidor.puerto() + rutaYConsulta);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod(metodo);
        if (!metodo.equals("GET") && !metodo.equals("OPTIONS")) {
            con.setDoOutput(true);
            con.getOutputStream().close();
        }
        Respuesta r = new Respuesta();
        r.estado = con.getResponseCode();
        InputStream in = r.estado >= 400 ? con.getErrorStream() : con.getInputStream();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        if (in != null) {
            byte[] buffer = new byte[4096];
            int leidos;
            while ((leidos = in.read(buffer)) > 0) {
                bytes.write(buffer, 0, leidos);
            }
            in.close();
        }
        r.cuerpo = bytes.toString("UTF-8");
        r.origenCors = con.getHeaderField("Access-Control-Allow-Origin");
        r.metodosCors = con.getHeaderField("Access-Control-Allow-Methods");
        r.tipo = con.getHeaderField("Content-Type");
        r.permitidos = con.getHeaderField("Allow");
        return r;
    }

    private static int contar(String texto, String patron) {
        int total = 0;
        int desde = 0;
        while ((desde = texto.indexOf(patron, desde)) >= 0) {
            total++;
            desde += patron.length();
        }
        return total;
    }

    // ---------- consulta de edificios, alas, salas y totales ----------

    @Test
    void edificiosDevuelveAlasConTotalesPorAlaYEdificio() throws Exception {
        conServidor(s -> {
            Respuesta r = pedir(s, "GET", "/api/edificios");
            assertEquals(200, r.estado);
            assertTrue(r.tipo.startsWith("application/json"), r.tipo);
            assertTrue(r.tipo.toLowerCase().contains("utf-8"), r.tipo);
            assertEquals("{\"edificios\":[{\"nombre\":\"Principal\",\"totalOcupantes\":180,\"alas\":["
                    + "{\"nombre\":\"Oeste\",\"totalOcupantes\":54},"
                    + "{\"nombre\":\"Central\",\"totalOcupantes\":75},"
                    + "{\"nombre\":\"Este\",\"totalOcupantes\":51}]}]}", r.cuerpo);
        });
    }

    @Test
    void salasDevuelveLasSalasRegistradasDelAla() throws Exception {
        conServidor(s -> {
            Respuesta r = pedir(s, "GET", "/api/salas?edificio=Principal&ala=Oeste");
            assertEquals(200, r.estado);
            assertEquals("{\"edificio\":\"Principal\",\"ala\":\"Oeste\",\"capacidad\":27,\"totalOcupantes\":54,"
                    + "\"salas\":[{\"letra\":\"A\",\"codigo\":\"LAB-101\",\"ocupantes\":24},"
                    + "{\"letra\":\"B\",\"codigo\":\"LAB-102\",\"ocupantes\":30}]}", r.cuerpo);
        });
    }

    @Test
    void salasConEdificioOAlaInexistenteDa404YParametrosFaltantesDa400() throws Exception {
        conServidor(s -> {
            Respuesta r1 = pedir(s, "GET", "/api/salas?edificio=Fantasma&ala=Oeste");
            assertEquals(404, r1.estado);
            assertTrue(r1.cuerpo.startsWith("{\"error\":\""), r1.cuerpo);
            assertEquals(404, pedir(s, "GET", "/api/salas?edificio=Principal&ala=Sur").estado);
            assertEquals(400, pedir(s, "GET", "/api/salas?edificio=Principal").estado);
            assertEquals(400, pedir(s, "GET", "/api/salas").estado);
        });
    }

    @Test
    void losParametrosSeDecodificanComoUtf8() throws Exception {
        conServidor(s -> {
            Respuesta r = pedir(s, "GET", "/api/salas?edificio=Ni%C3%B1os&ala=Oeste");
            assertEquals(404, r.estado);
            assertTrue(r.cuerpo.contains("Ni\u00f1os"), r.cuerpo);
        });
    }

    // ---------- actualizar ocupantes ----------

    @Test
    void actualizarOcupantesCambiaLaSalaYDevuelveLosNuevosTotales() throws Exception {
        conServidor(s -> {
            Respuesta r = pedir(s, "POST", "/api/ocupantes?edificio=Principal&ala=Oeste&letra=a&ocupantes=50");
            assertEquals(200, r.estado);
            assertEquals("{\"edificio\":\"Principal\",\"ala\":\"Oeste\",\"letra\":\"A\",\"codigo\":\"LAB-101\","
                    + "\"ocupantes\":50,\"totalAla\":80,\"totalEdificio\":206}", r.cuerpo);
            assertTrue(pedir(s, "GET", "/api/edificios").cuerpo.contains("\"totalOcupantes\":206"));
        });
    }

    @Test
    void actualizarOcupantesRechazaEntradasInvalidasSinCambiarNada() throws Exception {
        conServidor(s -> {
            String base = "/api/ocupantes?edificio=Principal&ala=Oeste&letra=A&ocupantes=";
            assertEquals(400, pedir(s, "POST", base + "-1").estado);
            assertEquals(400, pedir(s, "POST", base + "81").estado);
            assertEquals(400, pedir(s, "POST", base + "abc").estado);
            assertEquals(400, pedir(s, "POST", base).estado);
            assertEquals(400, pedir(s, "POST", "/api/ocupantes?edificio=Principal&ala=Oeste&letra=AB&ocupantes=3").estado);
            assertEquals(400, pedir(s, "POST", "/api/ocupantes?edificio=Principal&ala=Oeste&letra=1&ocupantes=3").estado);
            assertEquals(400, pedir(s, "POST", "/api/ocupantes?edificio=Principal&ala=Oeste&letra=A").estado);
            assertEquals(404, pedir(s, "POST", "/api/ocupantes?edificio=X&ala=Oeste&letra=A&ocupantes=3").estado);
            assertEquals(404, pedir(s, "POST", "/api/ocupantes?edificio=Principal&ala=Oeste&letra=Z&ocupantes=3").estado);
            assertTrue(pedir(s, "GET", "/api/edificios").cuerpo.contains("\"totalOcupantes\":180"));
        });
    }

    @Test
    void actualizarOcupantesSoloAceptaPost() throws Exception {
        conServidor(s -> {
            Respuesta r = pedir(s, "GET", "/api/ocupantes?edificio=Principal&ala=Oeste&letra=A&ocupantes=5");
            assertEquals(405, r.estado);
            assertEquals("POST", r.permitidos);
            assertTrue(pedir(s, "GET", "/api/edificios").cuerpo.contains("\"totalOcupantes\":180"));
        });
    }

    // ---------- alarma ----------

    @Test
    void alarmaDevuelveElReporteEnElOrdenDelBackend() throws Exception {
        conServidor(s -> {
            Respuesta r = pedir(s, "POST", "/api/alarma");
            assertEquals(200, r.estado);
            assertTrue(r.cuerpo.startsWith("{\"totalSalas\":6,\"salas\":["), r.cuerpo);
            assertTrue(r.cuerpo.contains("{\"posicion\":1,\"edificio\":\"Principal\",\"ala\":\"Central\","
                    + "\"letra\":\"A\",\"codigo\":\"RM-103\",\"ocupantes\":45}"), r.cuerpo);
            String[] orden = {"RM-103", "LIB-001", "LAB-102", "RM-104", "LAB-101", "OFF-105"};
            int anterior = -1;
            for (int i = 0; i < orden.length; i++) {
                int posicion = r.cuerpo.indexOf("\"codigo\":\"" + orden[i] + "\"");
                assertTrue(posicion > anterior, orden[i] + " fuera de orden: " + r.cuerpo);
                assertTrue(r.cuerpo.contains("\"posicion\":" + (i + 1) + ","));
                anterior = posicion;
            }
        });
    }

    @Test
    void cadaAlarmaRecalculaConLosOcupantesActuales() throws Exception {
        conServidor(s -> {
            pedir(s, "POST", "/api/ocupantes?edificio=Principal&ala=Este&letra=A&ocupantes=80");
            Respuesta r = pedir(s, "POST", "/api/alarma");
            assertTrue(r.cuerpo.contains("{\"posicion\":1,\"edificio\":\"Principal\",\"ala\":\"Este\","
                    + "\"letra\":\"A\",\"codigo\":\"OFF-105\",\"ocupantes\":80}"), r.cuerpo);
        });
    }

    @Test
    void alarmaSoloAceptaPost() throws Exception {
        conServidor(s -> {
            Respuesta r = pedir(s, "GET", "/api/alarma");
            assertEquals(405, r.estado);
            assertEquals("POST", r.permitidos);
        });
    }

    // ---------- rutas y pasillos ----------

    @Test
    void rutasDevuelveSalidaRutaYTiempoDeCadaSala() throws Exception {
        conServidor(s -> {
            Respuesta r = pedir(s, "GET", "/api/rutas");
            assertEquals(200, r.estado);
            assertTrue(r.cuerpo.contains("\"salidas\":[\"Salida Norte\",\"Salida Este\",\"Salida Sur\"]"), r.cuerpo);
            assertTrue(r.cuerpo.contains("{\"sala\":\"LAB-101\",\"alcanzable\":true,\"salida\":\"Salida Norte\","
                    + "\"tiempoSegundos\":30,\"ruta\":[\"LAB-101\",\"Pasillo A\",\"Salida Norte\"]}"), r.cuerpo);
            assertTrue(r.cuerpo.contains("{\"sala\":\"RM-103\",\"alcanzable\":true,\"salida\":\"Salida Sur\","
                    + "\"tiempoSegundos\":37,\"ruta\":[\"RM-103\",\"RM-104\",\"Pasillo C\",\"Salida Sur\"]}"), r.cuerpo);
            assertTrue(r.cuerpo.contains("{\"sala\":\"LIB-001\",\"alcanzable\":true,\"salida\":\"Salida Este\","
                    + "\"tiempoSegundos\":25,\"ruta\":[\"LIB-001\",\"Salida Este\"]}"), r.cuerpo);
            assertEquals(6, contar(r.cuerpo, "\"sala\":"));
        });
    }

    @Test
    void pasillosDevuelveElEstadoDeLos15Corredores() throws Exception {
        conServidor(s -> {
            Respuesta r = pedir(s, "GET", "/api/pasillos");
            assertEquals(200, r.estado);
            assertEquals(15, contar(r.cuerpo, "\"desde\":"));
            assertTrue(r.cuerpo.startsWith("{\"pasillos\":[{\"desde\":\"Salida Norte\",\"hacia\":\"Pasillo A\","
                    + "\"segundos\":20,\"estado\":\"Abierto\"}"), r.cuerpo);
            assertTrue(r.cuerpo.contains("{\"desde\":\"Pasillo B\",\"hacia\":\"Salida Este\",\"segundos\":20,"
                    + "\"estado\":\"Bloqueado\"}"), r.cuerpo);
            assertEquals(1, contar(r.cuerpo, "\"estado\":\"Bloqueado\""));
        });
    }

    @Test
    void bloquearUnPasilloLoMarcaYLasRutasNuncaLoUsan() throws Exception {
        conServidor(s -> {
            Respuesta r = pedir(s, "POST", "/api/pasillos/bloquear?desde=Pasillo%20C&hacia=Salida%20Sur");
            assertEquals(200, r.estado);
            assertEquals("{\"desde\":\"Pasillo C\",\"hacia\":\"Salida Sur\",\"segundos\":18,\"estado\":\"Bloqueado\"}",
                    r.cuerpo);
            assertTrue(pedir(s, "GET", "/api/pasillos").cuerpo.contains(
                    "{\"desde\":\"Pasillo C\",\"hacia\":\"Salida Sur\",\"segundos\":18,\"estado\":\"Bloqueado\"}"));
            Respuesta rutas = pedir(s, "GET", "/api/rutas");
            assertFalse(rutas.cuerpo.contains("\"Pasillo C\",\"Salida Sur\""), rutas.cuerpo);
            assertFalse(rutas.cuerpo.contains("\"Salida Sur\",\"Pasillo C\""), rutas.cuerpo);
            assertTrue(rutas.cuerpo.contains("{\"sala\":\"RM-104\",\"alcanzable\":true,\"salida\":\"Salida Este\","
                    + "\"tiempoSegundos\":53,\"ruta\":[\"RM-104\",\"Pasillo C\",\"OFF-105\",\"LIB-001\",\"Salida Este\"]}"),
                    rutas.cuerpo);
        });
    }

    @Test
    void desbloquearUnPasilloLoReabreYActualizaLasRutas() throws Exception {
        conServidor(s -> {
            Respuesta r = pedir(s, "POST", "/api/pasillos/desbloquear?desde=Salida%20Este&hacia=Pasillo%20B");
            assertEquals(200, r.estado);
            assertTrue(r.cuerpo.contains("\"estado\":\"Abierto\""), r.cuerpo);
            assertEquals(0, contar(pedir(s, "GET", "/api/pasillos").cuerpo, "\"estado\":\"Bloqueado\""));
            Respuesta rutas = pedir(s, "GET", "/api/rutas");
            assertTrue(rutas.cuerpo.contains("{\"sala\":\"LAB-102\",\"alcanzable\":true,\"salida\":\"Salida Este\","
                    + "\"tiempoSegundos\":32,\"ruta\":[\"LAB-102\",\"Pasillo B\",\"Salida Este\"]}"), rutas.cuerpo);
        });
    }

    @Test
    void bloquearYDesbloquearValidanEntradasYMetodos() throws Exception {
        conServidor(s -> {
            assertEquals(404, pedir(s, "POST", "/api/pasillos/bloquear?desde=LAB-101&hacia=Salida%20Sur").estado);
            assertEquals(404, pedir(s, "POST", "/api/pasillos/bloquear?desde=Nada&hacia=Salida%20Sur").estado);
            assertEquals(400, pedir(s, "POST", "/api/pasillos/bloquear?desde=Pasillo%20A").estado);
            assertEquals(400, pedir(s, "POST", "/api/pasillos/desbloquear?desde=Pasillo%20A&hacia=LAB-101").estado); // ya abierto
            assertEquals(404, pedir(s, "POST", "/api/pasillos/desbloquear?desde=LAB-101&hacia=Salida%20Sur").estado);
            assertEquals(405, pedir(s, "GET", "/api/pasillos/bloquear?desde=Pasillo%20A&hacia=LAB-101").estado);
            assertEquals(405, pedir(s, "POST", "/api/pasillos").estado);
            assertEquals(405, pedir(s, "POST", "/api/rutas").estado);
            assertEquals(405, pedir(s, "POST", "/api/edificios").estado);
        });
    }

    @Test
    void sinNingunaSalidaAlcanzableTodasLasSalasSeReportanBloqueadas() throws Exception {
        conServidor(s -> {
            pedir(s, "POST", "/api/pasillos/bloquear?desde=Pasillo%20C&hacia=Salida%20Sur");
            pedir(s, "POST", "/api/pasillos/bloquear?desde=Salida%20Norte&hacia=Pasillo%20A");
            pedir(s, "POST", "/api/pasillos/bloquear?desde=LIB-001&hacia=Salida%20Este");
            Respuesta r = pedir(s, "GET", "/api/rutas");
            assertEquals(6, contar(r.cuerpo, "\"alcanzable\":false,\"salida\":null,\"tiempoSegundos\":null,\"ruta\":[]"),
                    r.cuerpo);
            assertEquals(0, contar(r.cuerpo, "\"alcanzable\":true"));
        });
    }

    // ---------- CORS, rutas desconocidas ----------

    @Test
    void lasRespuestasLlevanElOrigenCorsConfigurado() throws Exception {
        conServidor("http://localhost:5173", s -> {
            assertEquals("http://localhost:5173", pedir(s, "GET", "/api/edificios").origenCors);
            assertEquals("http://localhost:5173", pedir(s, "GET", "/api/nada").origenCors); // tambien en errores
        });
        conServidor("*", s -> assertEquals("*", pedir(s, "GET", "/api/edificios").origenCors));
    }

    @Test
    void laPreparacionCorsOptionsResponde204ConLosMetodosPermitidos() throws Exception {
        conServidor("http://localhost:5173", s -> {
            Respuesta r = pedir(s, "OPTIONS", "/api/alarma");
            assertEquals(204, r.estado);
            assertEquals("http://localhost:5173", r.origenCors);
            assertTrue(r.metodosCors.contains("POST") && r.metodosCors.contains("GET"), r.metodosCors);
            assertEquals("", r.cuerpo);
        });
    }

    @Test
    void rutaDesconocidaDa404ConErrorJson() throws Exception {
        conServidor(s -> {
            Respuesta r = pedir(s, "GET", "/api/xyz");
            assertEquals(404, r.estado);
            assertTrue(r.cuerpo.startsWith("{\"error\":\""), r.cuerpo);
            assertEquals(404, pedir(s, "GET", "/").estado);
            assertNull(null);
        });
    }

    @Test
    void elPuertoEfimeroSeResuelveAlIniciar() throws Exception {
        conServidor(s -> assertTrue(s.puerto() > 0));
    }
}
