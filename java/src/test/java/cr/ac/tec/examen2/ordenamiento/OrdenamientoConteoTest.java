package cr.ac.tec.examen2.ordenamiento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import cr.ac.tec.examen2.arbol.RegistroSala;
import org.junit.jupiter.api.Test;

class OrdenamientoConteoTest {

    private static RegistroSala r(String codigo, int ocupantes) {
        return new RegistroSala("E", "A", 'A', codigo, ocupantes);
    }

    private static String codigos(RegistroSala[] salas) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < salas.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(salas[i].codigo());
        }
        return sb.toString();
    }

    @Test
    void ordenaDeMayorAMenor() {
        RegistroSala[] entrada = {r("a", 3), r("b", 10), r("c", 0), r("d", 7)};
        RegistroSala[] salida = new OrdenamientoConteo().ordenarDescendente(entrada, 80);
        assertEquals("b,d,a,c", codigos(salida));
    }

    @Test
    void esEstableConEmpates() {
        RegistroSala[] entrada = {r("a", 5), r("b", 9), r("c", 5), r("d", 9), r("e", 5), r("f", 0), r("g", 0)};
        RegistroSala[] salida = new OrdenamientoConteo().ordenarDescendente(entrada, 80);
        assertEquals("b,d,a,c,e,f,g", codigos(salida));
    }

    @Test
    void todosIgualesConservanElOrdenOriginal() {
        RegistroSala[] entrada = new RegistroSala[50];
        for (int i = 0; i < entrada.length; i++) {
            entrada[i] = r("s" + i, 80);
        }
        RegistroSala[] salida = new OrdenamientoConteo().ordenarDescendente(entrada, 80);
        for (int i = 0; i < entrada.length; i++) {
            assertEquals("s" + i, salida[i].codigo());
        }
    }

    @Test
    void entradaVaciaYDeUnElemento() {
        assertEquals(0, new OrdenamientoConteo().ordenarDescendente(new RegistroSala[0], 80).length);
        RegistroSala[] uno = {r("x", 4)};
        assertEquals("x", codigos(new OrdenamientoConteo().ordenarDescendente(uno, 80)));
    }

    @Test
    void noModificaLaEntrada() {
        RegistroSala[] entrada = {r("a", 1), r("b", 2), r("c", 3)};
        new OrdenamientoConteo().ordenarDescendente(entrada, 80);
        assertEquals("a,b,c", codigos(entrada));
    }

    @Test
    void aceptaLosLimitesDelRango() {
        RegistroSala[] entrada = {r("min", 0), r("max", 80), r("mid", 40)};
        assertEquals("max,mid,min", codigos(new OrdenamientoConteo().ordenarDescendente(entrada, 80)));
    }

    @Test
    void rechazaValoresFueraDelRangoYArgumentosInvalidos() {
        OrdenamientoConteo orden = new OrdenamientoConteo();
        assertThrows(IllegalArgumentException.class,
                () -> orden.ordenarDescendente(new RegistroSala[] {r("a", 81)}, 80));
        assertThrows(IllegalArgumentException.class, () -> orden.ordenarDescendente(null, 80));
        assertThrows(IllegalArgumentException.class, () -> orden.ordenarDescendente(new RegistroSala[0], -1));
        assertThrows(IllegalArgumentException.class,
                () -> orden.ordenarDescendente(new RegistroSala[] {r("a", 1), null}, 80));
    }

    @Test
    void ordenaIgualQueUnaReferenciaSimpleEnUnaEntradaPseudoaleatoria() {
        int n = 400;
        RegistroSala[] entrada = new RegistroSala[n];
        long semilla = 12345;
        for (int i = 0; i < n; i++) {
            semilla = (semilla * 1103515245L + 12345L) & 0x7fffffffL;
            entrada[i] = r("s" + i, (int) (semilla % 81));
        }
        RegistroSala[] salida = new OrdenamientoConteo().ordenarDescendente(entrada, 80);
        assertEquals(n, salida.length);
        for (int i = 1; i < n; i++) {
            int anterior = salida[i - 1].ocupantes();
            int actual = salida[i].ocupantes();
            assertEquals(true, anterior >= actual, "no descendente en " + i);
            if (anterior == actual) {
                int idxAnterior = Integer.parseInt(salida[i - 1].codigo().substring(1));
                int idxActual = Integer.parseInt(salida[i].codigo().substring(1));
                assertEquals(true, idxAnterior < idxActual, "inestable en " + i);
            }
        }
    }

    @Test
    void elConteoDePasosEsLinealEIndependienteDeLosDatos() {
        int n = 400;
        int[][] patrones = new int[4][n];
        for (int i = 0; i < n; i++) {
            patrones[0][i] = 80;                // todos iguales (maximo)
            patrones[1][i] = 0;                 // todos cero
            patrones[2][i] = i % 81;            // ascendente cíclico
            patrones[3][i] = 80 - (i % 81);     // descendente cíclico
        }
        long esperado = 2L * n + (80 + 1);
        for (int p = 0; p < patrones.length; p++) {
            RegistroSala[] entrada = new RegistroSala[n];
            for (int i = 0; i < n; i++) {
                entrada[i] = r("s" + i, patrones[p][i]);
            }
            OrdenamientoConteo orden = new OrdenamientoConteo();
            orden.ordenarDescendente(entrada, 80);
            assertEquals(esperado, orden.operaciones(), "patron " + p);
        }
    }
}
