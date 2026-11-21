package cr.ac.tec.examen2.grafo;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cr.ac.tec.examen2.Datos;
import cr.ac.tec.examen2.ElementoNoEncontradoException;
import java.io.IOException;
import org.junit.jupiter.api.Test;

/** Estado de los corredores: listado con Abierto/Bloqueado y desbloqueo. */
class CorredoresBloqueadosTest {

    private static GrafoEvacuacion pequeno() {
        GrafoEvacuacion g = new GrafoEvacuacion();
        g.agregarPunto("R", TipoPunto.SALA);
        g.agregarPunto("P", TipoPunto.PASILLO);
        g.agregarPunto("S1", TipoPunto.SALIDA);
        g.agregarPunto("S2", TipoPunto.SALIDA);
        g.agregarCorredor("R", "P", 10);
        g.agregarCorredor("P", "S1", 20);
        g.agregarCorredor("R", "S2", 50);
        return g;
    }

    @Test
    void listarCorredoresDevuelveTodosEnOrdenDeRegistroConSuEstado() {
        GrafoEvacuacion g = pequeno();
        Corredor[] lista = g.listarCorredores();
        assertEquals(3, lista.length);
        assertEquals("R", lista[0].desde());
        assertEquals("P", lista[0].hacia());
        assertEquals(10, lista[0].segundos());
        assertFalse(lista[0].bloqueado());
        assertEquals("S2", lista[2].hacia());
    }

    @Test
    void unCorredorBloqueadoSigueListadoConSuPesoPeroSinAristas() {
        GrafoEvacuacion g = pequeno();
        g.bloquearCorredor("P", "S1");
        Corredor[] lista = g.listarCorredores();
        assertEquals(3, lista.length);
        assertTrue(lista[1].bloqueado());
        assertEquals(20, lista[1].segundos());
        assertFalse(g.existeCorredor("P", "S1"));
        assertFalse(g.existeCorredor("S1", "P"));
        assertEquals(2, g.cantidadCorredores()); // solo los abiertos
    }

    @Test
    void desbloquearRestauraLaAristaEnAmbasDireccionesConElMismoPeso() {
        GrafoEvacuacion g = pequeno();
        g.bloquearCorredor("P", "S1");
        g.desbloquearCorredor("S1", "P"); // los extremos pueden darse en cualquier orden
        assertTrue(g.existeCorredor("P", "S1"));
        assertTrue(g.existeCorredor("S1", "P"));
        assertEquals(20, g.pesoCorredor("P", "S1"));
        assertEquals(20, g.pesoCorredor("S1", "P"));
        assertEquals(3, g.cantidadCorredores());
        assertEquals(1, g.grado("S1"));
        assertFalse(g.listarCorredores()[1].bloqueado());
    }

    @Test
    void desbloquearDevuelveLasRutasAlEstadoAnterior() {
        GrafoEvacuacion g = pequeno();
        assertEquals(30, g.rutaMasRapida("R").tiempoSegundos());
        g.bloquearCorredor("P", "S1");
        assertEquals(50, g.rutaMasRapida("R").tiempoSegundos());
        g.desbloquearCorredor("P", "S1");
        RutaEvacuacion ruta = g.rutaMasRapida("R");
        assertEquals(30, ruta.tiempoSegundos());
        assertArrayEquals(new String[] {"R", "P", "S1"}, ruta.ruta());
    }

    @Test
    void desbloquearErroresSonExplicitos() {
        GrafoEvacuacion g = pequeno();
        assertThrows(IllegalArgumentException.class, () -> g.desbloquearCorredor("R", "P")); // ya abierto
        assertThrows(ElementoNoEncontradoException.class, () -> g.desbloquearCorredor("P", "S2")); // no existe
        assertThrows(ElementoNoEncontradoException.class, () -> g.desbloquearCorredor("R", "Nada"));
        assertThrows(IllegalArgumentException.class, () -> g.desbloquearCorredor(null, "P"));
    }

    @Test
    void bloquearDosVecesSigueSiendoUnErrorExplicito() {
        GrafoEvacuacion g = pequeno();
        g.bloquearCorredor("P", "S1");
        assertThrows(ElementoNoEncontradoException.class, () -> g.bloquearCorredor("P", "S1"));
        assertEquals(3, g.listarCorredores().length);
    }

    @Test
    void volverAAgregarUnCorredorBloqueadoLoReemplazaSinDuplicarElRegistro() {
        GrafoEvacuacion g = pequeno();
        g.bloquearCorredor("P", "S1");
        g.agregarCorredor("P", "S1", 7);
        assertEquals(3, g.listarCorredores().length);
        assertFalse(g.listarCorredores()[1].bloqueado());
        assertEquals(7, g.pesoCorredor("P", "S1"));
    }

    @Test
    void nombresDeSalidasEnOrdenDeRegistro() {
        assertArrayEquals(new String[] {"S1", "S2"}, pequeno().nombresSalidas());
    }

    @Test
    void conLosCsvElListadoTiene15CorredoresYSoloUnoBloqueado() throws IOException {
        GrafoEvacuacion g = Datos.grafoDeCsv(Datos.arbolDeCsv());
        Corredor[] lista = g.listarCorredores();
        assertEquals(15, lista.length);
        int bloqueados = 0;
        for (int i = 0; i < lista.length; i++) {
            if (lista[i].bloqueado()) {
                bloqueados++;
                assertEquals("Pasillo B", lista[i].desde());
                assertEquals("Salida Este", lista[i].hacia());
                assertEquals(20, lista[i].segundos());
            }
        }
        assertEquals(1, bloqueados);
        assertEquals("Salida Norte", lista[0].desde());
    }

    @Test
    void desbloquearElPasilloDelCsvCambiaLasRutasDeLab102YRm103() throws IOException {
        GrafoEvacuacion g = Datos.grafoDeCsv(Datos.arbolDeCsv());
        g.desbloquearCorredor("Pasillo B", "Salida Este");
        RutaEvacuacion[] rutas = g.rutasDeSalas();
        assertEquals("Salida Este", rutas[1].salida());
        assertEquals(32, rutas[1].tiempoSegundos());
        assertArrayEquals(new String[] {"LAB-102", "Pasillo B", "Salida Este"}, rutas[1].ruta());
        assertEquals("Salida Este", rutas[2].salida());
        assertEquals(28, rutas[2].tiempoSegundos());
        assertEquals("Salida Norte", rutas[0].salida());
        assertEquals(30, rutas[0].tiempoSegundos());
        assertEquals("Salida Sur", rutas[3].salida());
        assertEquals(27, rutas[3].tiempoSegundos());
    }
}
