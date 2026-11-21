package cr.ac.tec.examen2.servidor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cr.ac.tec.examen2.Datos;
import cr.ac.tec.examen2.arbol.ArbolJerarquia;
import cr.ac.tec.examen2.datos.CatalogoDatos;
import cr.ac.tec.examen2.grafo.GrafoEvacuacion;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import org.junit.jupiter.api.Test;

/** Selección del conjunto de datos desde la API (/api/datos). */
class ServidorHttpDatosTest {
    private interface Cuerpo {
        void ejecutar(ServidorHttp servidor) throws Exception;
    }

    private static void conCatalogo(Cuerpo cuerpo) throws Exception {
        ArbolJerarquia arbol = Datos.arbolDeCsv();
        GrafoEvacuacion grafo = Datos.grafoDeCsv(arbol);
        ServidorHttp servidor = new ServidorHttp(arbol, grafo, 0, "*", new CatalogoDatos("../datos", "version-b"));
        servidor.iniciar();
        try {
            cuerpo.ejecutar(servidor);
        } finally {
            servidor.detener();
        }
    }

    private static String[] pedir(ServidorHttp servidor, String metodo, String ruta) throws Exception {
        HttpURLConnection con = (HttpURLConnection) new URL("http://127.0.0.1:" + servidor.puerto() + ruta).openConnection();
        con.setRequestMethod(metodo);
        if (metodo.equals("POST")) {
            con.setDoOutput(true);
            con.getOutputStream().close();
        }
        int estado = con.getResponseCode();
        InputStream in = estado >= 400 ? con.getErrorStream() : con.getInputStream();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int n;
        while (in != null && (n = in.read(buffer)) > 0) {
            bytes.write(buffer, 0, n);
        }
        return new String[] {String.valueOf(estado), bytes.toString("UTF-8")};
    }

    @Test
    void getDatosListaLosConjuntosYElActual() throws Exception {
        conCatalogo(servidor -> {
            String[] r = pedir(servidor, "GET", "/api/datos");
            assertEquals("200", r[0]);
            assertEquals("{\"actual\":\"version-b\",\"conjuntos\":[\"campus-400\",\"version-b\"]}", r[1]);
        });
    }

    @Test
    void cargarUnConjuntoReemplazaArbolYGrafoYResponde() throws Exception {
        conCatalogo(servidor -> {
            String[] r = pedir(servidor, "POST", "/api/datos/cargar?conjunto=campus-400");
            assertEquals("200", r[0]);
            assertEquals("{\"actual\":\"campus-400\",\"salas\":400}", r[1]);
            assertTrue(pedir(servidor, "GET", "/api/edificios")[1].contains("\"Ingeniería\""));
            assertTrue(pedir(servidor, "GET", "/api/rutas")[1].contains("Salida Suroeste"));
            assertTrue(pedir(servidor, "GET", "/api/datos")[1].startsWith("{\"actual\":\"campus-400\""));
        });
    }

    @Test
    void recargarDescartaLosCambiosHechosEnMemoria() throws Exception {
        conCatalogo(servidor -> {
            pedir(servidor, "POST", "/api/ocupantes?edificio=Principal&ala=Oeste&letra=A&ocupantes=80");
            assertTrue(pedir(servidor, "GET", "/api/edificios")[1].contains("\"totalOcupantes\":236"));
            pedir(servidor, "POST", "/api/datos/cargar?conjunto=version-b");
            assertTrue(pedir(servidor, "GET", "/api/edificios")[1].contains("\"totalOcupantes\":180"));
        });
    }

    @Test
    void errores() throws Exception {
        conCatalogo(servidor -> {
            assertEquals("404", pedir(servidor, "POST", "/api/datos/cargar?conjunto=no-existe")[0]);
            assertEquals("404", pedir(servidor, "POST", "/api/datos/cargar?conjunto=..%2Fdatos%2Fversion-b")[0]);
            assertEquals("400", pedir(servidor, "POST", "/api/datos/cargar")[0]);
            assertEquals("405", pedir(servidor, "GET", "/api/datos/cargar?conjunto=version-b")[0]);
            assertEquals("405", pedir(servidor, "POST", "/api/datos")[0]);
            // sigue funcionando con los datos anteriores
            assertTrue(pedir(servidor, "GET", "/api/edificios")[1].contains("\"Principal\""));
        });
    }

    @Test
    void sinCatalogoNoHayConjuntosYCargarDa404() throws Exception {
        ArbolJerarquia arbol = Datos.arbolDeCsv();
        ServidorHttp servidor = new ServidorHttp(arbol, Datos.grafoDeCsv(arbol), 0, "*");
        servidor.iniciar();
        try {
            assertEquals("{\"actual\":null,\"conjuntos\":[]}", pedir(servidor, "GET", "/api/datos")[1]);
            assertEquals("404", pedir(servidor, "POST", "/api/datos/cargar?conjunto=version-b")[0]);
        } finally {
            servidor.detener();
        }
    }
}
