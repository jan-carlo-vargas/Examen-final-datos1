package cr.ac.tec.examen2.grafo;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cr.ac.tec.examen2.ElementoNoEncontradoException;
import org.junit.jupiter.api.Test;

class GrafoEvacuacionTest {

    /**
     * R --10-- P --20-- S1
     *          |
     *          +---5---- Q --30-- S2
     */
    private static GrafoEvacuacion pequeno() {
        GrafoEvacuacion g = new GrafoEvacuacion();
        g.agregarPunto("R", TipoPunto.SALA);
        g.agregarPunto("P", TipoPunto.PASILLO);
        g.agregarPunto("Q", TipoPunto.PASILLO);
        g.agregarPunto("S1", TipoPunto.SALIDA);
        g.agregarPunto("S2", TipoPunto.SALIDA);
        g.agregarCorredor("R", "P", 10);
        g.agregarCorredor("P", "S1", 20);
        g.agregarCorredor("P", "Q", 5);
        g.agregarCorredor("Q", "S2", 30);
        return g;
    }

    // ---- modelo ----

    @Test
    void cuentaPuntosCorredoresYSalidas() {
        GrafoEvacuacion g = pequeno();
        assertEquals(5, g.cantidadPuntos());
        assertEquals(4, g.cantidadCorredores());
        assertEquals(2, g.cantidadSalidas());
        assertEquals(TipoPunto.SALA, g.tipo("R"));
        assertEquals(TipoPunto.SALIDA, g.tipo("S2"));
    }

    @Test
    void esNoDirigidoYGuardaLaArista_enAmbasListasDeAdyacencia() {
        GrafoEvacuacion g = pequeno();
        assertTrue(g.existeCorredor("R", "P"));
        assertTrue(g.existeCorredor("P", "R"));
        assertEquals(10, g.pesoCorredor("R", "P"));
        assertEquals(10, g.pesoCorredor("P", "R"));
        assertEquals(3, g.grado("P"));
        assertEquals(1, g.grado("R"));
    }

    @Test
    void validaPuntosYCorredoresInvalidos() {
        GrafoEvacuacion g = pequeno();
        assertThrows(IllegalArgumentException.class, () -> g.agregarPunto("R", TipoPunto.SALA));
        assertThrows(IllegalArgumentException.class, () -> g.agregarPunto(null, TipoPunto.SALA));
        assertThrows(IllegalArgumentException.class, () -> g.agregarPunto("  ", TipoPunto.SALA));
        assertThrows(IllegalArgumentException.class, () -> g.agregarPunto("Z", null));
        assertThrows(ElementoNoEncontradoException.class, () -> g.agregarCorredor("R", "NoExiste", 5));
        assertThrows(ElementoNoEncontradoException.class, () -> g.agregarCorredor("NoExiste", "R", 5));
        assertThrows(IllegalArgumentException.class, () -> g.agregarCorredor("R", "P", 7));
        assertThrows(IllegalArgumentException.class, () -> g.agregarCorredor("P", "R", 7));
        assertThrows(IllegalArgumentException.class, () -> g.agregarCorredor("R", "R", 7));
        assertThrows(IllegalArgumentException.class, () -> g.agregarCorredor("R", "Q", -1));
        assertEquals(4, g.cantidadCorredores());
    }

    @Test
    void elGrafoCreceConNuevosEdificiosPuntosYSalidas() {
        GrafoEvacuacion g = new GrafoEvacuacion();
        g.agregarPunto("sala0", TipoPunto.SALA);
        g.agregarPunto("salida", TipoPunto.SALIDA);
        String anterior = "sala0";
        for (int i = 1; i <= 300; i++) {
            String nombre = "p" + i;
            g.agregarPunto(nombre, TipoPunto.PASILLO);
            g.agregarCorredor(anterior, nombre, 2);
            anterior = nombre;
        }
        g.agregarCorredor(anterior, "salida", 3);
        assertEquals(302, g.cantidadPuntos());
        RutaEvacuacion ruta = g.rutaMasRapida("sala0");
        assertTrue(ruta.alcanzable());
        assertEquals(300 * 2 + 3, ruta.tiempoSegundos());
        assertEquals(302, ruta.ruta().length);
    }

    // ---- bloqueo ----

    @Test
    void bloquearEliminaLaAristaEnAmbosSentidos() {
        GrafoEvacuacion g = pequeno();
        g.bloquearCorredor("P", "S1");
        assertFalse(g.existeCorredor("P", "S1"));
        assertFalse(g.existeCorredor("S1", "P"));
        assertEquals(2, g.grado("P"));
        assertEquals(0, g.grado("S1"));
        assertEquals(3, g.cantidadCorredores());
    }

    @Test
    void bloquearFuncionaConLosExtremosEnCualquierOrden() {
        GrafoEvacuacion g = pequeno();
        g.bloquearCorredor("S1", "P");
        assertFalse(g.existeCorredor("P", "S1"));
        assertEquals(0, g.grado("S1"));
    }

    @Test
    void bloquearUnCorredorInexistenteSeReporta() {
        GrafoEvacuacion g = pequeno();
        assertThrows(ElementoNoEncontradoException.class, () -> g.bloquearCorredor("R", "S1"));
        assertThrows(ElementoNoEncontradoException.class, () -> g.bloquearCorredor("R", "Nada"));
        g.bloquearCorredor("R", "P");
        assertThrows(ElementoNoEncontradoException.class, () -> g.bloquearCorredor("R", "P"));
    }

    @Test
    void despuesDeBloquearSePuedeVolverAAgregarElCorredor() {
        GrafoEvacuacion g = pequeno();
        g.bloquearCorredor("R", "P");
        g.agregarCorredor("R", "P", 11);
        assertEquals(11, g.pesoCorredor("P", "R"));
        assertEquals(4, g.cantidadCorredores());
    }

    // ---- Dijkstra ----

    @Test
    void elegiLaSalidaMasRapidaConSuRutaYTiempo() {
        RutaEvacuacion ruta = pequeno().rutaMasRapida("R");
        assertTrue(ruta.alcanzable());
        assertEquals("R", ruta.origen());
        assertEquals("S1", ruta.salida());
        assertEquals(30, ruta.tiempoSegundos());
        assertArrayEquals(new String[] {"R", "P", "S1"}, ruta.ruta());
    }

    @Test
    void dijkstraCalculaDistanciasMinimasANodosIntermedios() {
        GrafoEvacuacion g = pequeno();
        ResultadoDijkstra r = g.dijkstra("R");
        assertEquals(0, r.distancia(g.buscarPunto("R")));
        assertEquals(10, r.distancia(g.buscarPunto("P")));
        assertEquals(15, r.distancia(g.buscarPunto("Q")));
        assertEquals(30, r.distancia(g.buscarPunto("S1")));
        assertEquals(45, r.distancia(g.buscarPunto("S2")));
    }

    @Test
    void prefiereUnCaminoLargoEnAristasPeroMasRapidoEnSegundos() {
        GrafoEvacuacion g = new GrafoEvacuacion();
        g.agregarPunto("R", TipoPunto.SALA);
        g.agregarPunto("a", TipoPunto.PASILLO);
        g.agregarPunto("b", TipoPunto.PASILLO);
        g.agregarPunto("c", TipoPunto.PASILLO);
        g.agregarPunto("S", TipoPunto.SALIDA);
        g.agregarCorredor("R", "S", 100);
        g.agregarCorredor("R", "a", 10);
        g.agregarCorredor("a", "b", 10);
        g.agregarCorredor("b", "c", 10);
        g.agregarCorredor("c", "S", 10);
        RutaEvacuacion ruta = g.rutaMasRapida("R");
        assertEquals(40, ruta.tiempoSegundos());
        assertArrayEquals(new String[] {"R", "a", "b", "c", "S"}, ruta.ruta());
    }

    @Test
    void conBloqueoLaRutaCambiaYNuncaUsaLaAristaBloqueada() {
        GrafoEvacuacion g = pequeno();
        g.bloquearCorredor("P", "S1");
        RutaEvacuacion ruta = g.rutaMasRapida("R");
        assertTrue(ruta.alcanzable());
        assertEquals("S2", ruta.salida());
        assertEquals(45, ruta.tiempoSegundos());
        assertArrayEquals(new String[] {"R", "P", "Q", "S2"}, ruta.ruta());
        String[] puntos = ruta.ruta();
        for (int i = 0; i + 1 < puntos.length; i++) {
            assertTrue(g.existeCorredor(puntos[i], puntos[i + 1]), "usa arista inexistente");
        }
    }

    @Test
    void sinSalidaAlcanzableSeReportaBloqueado() {
        GrafoEvacuacion g = pequeno();
        g.bloquearCorredor("P", "S1");
        g.bloquearCorredor("Q", "S2");
        RutaEvacuacion ruta = g.rutaMasRapida("R");
        assertFalse(ruta.alcanzable());
        assertNull(ruta.salida());
        assertEquals(0, ruta.ruta().length);
        assertTrue(ruta.toString().contains("Bloqueado"));
    }

    @Test
    void unaSalaAisladaSeReportaBloqueado() {
        GrafoEvacuacion g = pequeno();
        g.agregarPunto("isla", TipoPunto.SALA);
        RutaEvacuacion ruta = g.rutaMasRapida("isla");
        assertFalse(ruta.alcanzable());
        assertTrue(ruta.toString().contains("Bloqueado"));
    }

    @Test
    void sinSalidasRegistradasTodoEstaBloqueado() {
        GrafoEvacuacion g = new GrafoEvacuacion();
        g.agregarPunto("R", TipoPunto.SALA);
        g.agregarPunto("P", TipoPunto.PASILLO);
        g.agregarCorredor("R", "P", 4);
        assertFalse(g.rutaMasRapida("R").alcanzable());
    }

    @Test
    void soportaNSalidasYEligeLaMasCercana() {
        GrafoEvacuacion g = new GrafoEvacuacion();
        g.agregarPunto("R", TipoPunto.SALA);
        int n = 7;
        for (int i = 0; i < n; i++) {
            g.agregarPunto("S" + i, TipoPunto.SALIDA);
            g.agregarCorredor("R", "S" + i, 100 - 10 * i);
        }
        RutaEvacuacion ruta = g.rutaMasRapida("R");
        assertEquals("S6", ruta.salida());
        assertEquals(40, ruta.tiempoSegundos());
        assertEquals(7, g.cantidadSalidas());
    }

    @Test
    void conEmpateEntreSalidasGanaLaPrimeraRegistrada() {
        GrafoEvacuacion g = new GrafoEvacuacion();
        g.agregarPunto("R", TipoPunto.SALA);
        g.agregarPunto("SB", TipoPunto.SALIDA);
        g.agregarPunto("SA", TipoPunto.SALIDA);
        g.agregarCorredor("R", "SA", 10);
        g.agregarCorredor("R", "SB", 10);
        assertEquals("SB", g.rutaMasRapida("R").salida());
    }

    @Test
    void desdeUnaSalidaLaRutaEsElMismoPuntoConCeroSegundos() {
        RutaEvacuacion ruta = pequeno().rutaMasRapida("S1");
        assertTrue(ruta.alcanzable());
        assertEquals("S1", ruta.salida());
        assertEquals(0, ruta.tiempoSegundos());
        assertArrayEquals(new String[] {"S1"}, ruta.ruta());
    }

    @Test
    void origenInexistenteSeReporta() {
        GrafoEvacuacion g = pequeno();
        assertThrows(ElementoNoEncontradoException.class, () -> g.rutaMasRapida("Nada"));
        assertThrows(IllegalArgumentException.class, () -> g.rutaMasRapida(null));
        assertThrows(ElementoNoEncontradoException.class, () -> g.dijkstra("Nada"));
    }

    @Test
    void rutasDeSalasSoloIncluyeSalasEnOrdenDeRegistro() {
        GrafoEvacuacion g = new GrafoEvacuacion();
        g.agregarPunto("P", TipoPunto.PASILLO);
        g.agregarPunto("R2", TipoPunto.SALA);
        g.agregarPunto("S", TipoPunto.SALIDA);
        g.agregarPunto("R1", TipoPunto.SALA);
        g.agregarCorredor("R2", "P", 1);
        g.agregarCorredor("P", "S", 1);
        g.agregarCorredor("R1", "S", 5);
        RutaEvacuacion[] rutas = g.rutasDeSalas();
        assertEquals(2, rutas.length);
        assertEquals("R2", rutas[0].origen());
        assertEquals("R1", rutas[1].origen());
        assertEquals(2, rutas[0].tiempoSegundos());
        assertEquals(5, rutas[1].tiempoSegundos());
    }

    @Test
    void elTextoDeLaRutaIncluyeSalidaTiempoYRecorrido() {
        String texto = pequeno().rutaMasRapida("R").toString();
        assertTrue(texto.contains("S1"));
        assertTrue(texto.contains("30 s"));
        assertTrue(texto.contains("R > P > S1"));
    }

    @Test
    void dijkstraNoDependeDeAristasBloqueadasAnteriores() {
        GrafoEvacuacion g = pequeno();
        assertEquals(30, g.rutaMasRapida("R").tiempoSegundos());
        g.bloquearCorredor("P", "S1");
        assertEquals(45, g.rutaMasRapida("R").tiempoSegundos());
    }
}
