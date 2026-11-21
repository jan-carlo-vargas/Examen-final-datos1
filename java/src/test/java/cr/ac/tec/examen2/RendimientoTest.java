package cr.ac.tec.examen2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cr.ac.tec.examen2.arbol.ArbolJerarquia;
import cr.ac.tec.examen2.arbol.RegistroSala;
import cr.ac.tec.examen2.informe.InformeBrigada;
import cr.ac.tec.examen2.ordenamiento.OrdenamientoConteo;
import org.junit.jupiter.api.Test;

/**
 * Comprobacion enfocada del reporte de brigada: 400 salas, maximo 80 ocupantes por sala.
 * Los patrones incluyen entradas "peores" para otros algoritmos (todo igual, ya ordenado,
 * inverso). El conteo de pasos es independiente del hardware; el tiempo de reloj es solo
 * una verificacion empirica en la maquina que ejecuta la prueba.
 */
class RendimientoTest {

    private static final int EDIFICIOS = 8;
    private static final int ALAS_POR_EDIFICIO = 2;
    private static final int SALAS_POR_ALA = 25;
    private static final int TOTAL_SALAS = EDIFICIOS * ALAS_POR_EDIFICIO * SALAS_POR_ALA; // 400
    private static final long LIMITE_NANOS = 1_000_000_000L;

    private static int valor(int patron, int i, long[] semilla) {
        switch (patron) {
            case 0:
                return 80;
            case 1:
                return 0;
            case 2:
                return i % 81;
            case 3:
                return 80 - (i % 81);
            case 4:
                return (i * 37) % 81;
            default:
                semilla[0] = (semilla[0] * 1103515245L + 12345L) & 0x7fffffffL;
                return (int) (semilla[0] % 81);
        }
    }

    private static ArbolJerarquia construir(int patron) {
        ArbolJerarquia arbol = new ArbolJerarquia();
        long[] semilla = {2024};
        int i = 0;
        for (int e = 0; e < EDIFICIOS; e++) {
            for (int a = 0; a < ALAS_POR_EDIFICIO; a++) {
                for (int s = 0; s < SALAS_POR_ALA; s++) {
                    char letra = arbol.registrarSala("E" + e, "A" + a, "S-" + e + "-" + a + "-" + s);
                    arbol.actualizarOcupantes("E" + e, "A" + a, letra, valor(patron, i, semilla));
                    i++;
                }
            }
        }
        return arbol;
    }

    @Test
    void elEscenarioTiene400SalasConMaximoDe80Ocupantes() {
        ArbolJerarquia arbol = construir(5);
        assertEquals(400, TOTAL_SALAS);
        assertEquals(400, arbol.cantidadSalas());
        RegistroSala[] salas = arbol.recolectarSalas();
        for (int i = 0; i < salas.length; i++) {
            assertTrue(salas[i].ocupantes() >= 0 && salas[i].ocupantes() <= 80);
        }
    }

    @Test
    void elReporteDe400SalasTerminaEnMenosDeUnSegundoParaCadaPatron() {
        long peor = 0;
        for (int patron = 0; patron < 6; patron++) {
            ArbolJerarquia arbol = construir(patron);
            long inicio = System.nanoTime();
            RegistroSala[] orden = InformeBrigada.generar(arbol);
            String texto = InformeBrigada.formatear(orden);
            long duracion = System.nanoTime() - inicio;
            assertEquals(400, orden.length);
            assertTrue(texto.length() > 0);
            assertTrue(duracion < LIMITE_NANOS, "patron " + patron + ": " + duracion + " ns");
            if (duracion > peor) {
                peor = duracion;
            }
        }
        assertTrue(peor < LIMITE_NANOS);
    }

    @Test
    void elTiempoSeRepiteSinSuperarElLimiteEnMultiplesEjecuciones() {
        ArbolJerarquia arbol = construir(2);
        for (int repeticion = 0; repeticion < 50; repeticion++) {
            long inicio = System.nanoTime();
            InformeBrigada.formatear(InformeBrigada.generar(arbol));
            assertTrue(System.nanoTime() - inicio < LIMITE_NANOS, "repeticion " + repeticion);
        }
    }

    @Test
    void elNumeroDePasosEsExactamenteLinealEnElPeorCasoDeCualquierPatron() {
        long esperado = 2L * 400 + (80 + 1); // n + (k+1) + n, con n=400 y k=80
        for (int patron = 0; patron < 6; patron++) {
            RegistroSala[] salas = construir(patron).recolectarSalas();
            OrdenamientoConteo orden = new OrdenamientoConteo();
            orden.ordenarDescendente(salas, ArbolJerarquia.MAX_OCUPANTES_SALA);
            assertEquals(esperado, orden.operaciones(), "patron " + patron);
        }
    }

    @Test
    void elResultadoEsDescendenteYEstableParaCadaPatron() {
        for (int patron = 0; patron < 6; patron++) {
            RegistroSala[] original = construir(patron).recolectarSalas();
            RegistroSala[] orden = InformeBrigada.generar(construir(patron));
            for (int i = 1; i < orden.length; i++) {
                assertTrue(orden[i - 1].ocupantes() >= orden[i].ocupantes(), "patron " + patron);
            }
            // Estabilidad: en cada grupo de igual valor, el orden relativo es el del recorrido.
            for (int i = 1; i < orden.length; i++) {
                if (orden[i - 1].ocupantes() == orden[i].ocupantes()) {
                    assertTrue(posicion(original, orden[i - 1]) < posicion(original, orden[i]),
                            "patron " + patron + " pos " + i);
                }
            }
        }
    }

    private static int posicion(RegistroSala[] original, RegistroSala sala) {
        for (int i = 0; i < original.length; i++) {
            if (original[i].codigo().equals(sala.codigo())) {
                return i;
            }
        }
        return -1;
    }
}
