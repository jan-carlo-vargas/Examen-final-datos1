package cr.ac.tec.examen2.grafo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ColaPrioridadMinimaTest {

    @Test
    void extraeEnOrdenDePrioridadAscendente() {
        ColaPrioridadMinima cola = new ColaPrioridadMinima();
        cola.insertar(1, 50);
        cola.insertar(2, 10);
        cola.insertar(3, 30);
        cola.insertar(4, 20);
        assertEquals(10, cola.prioridadMinima());
        assertEquals(2, cola.extraerMinimo());
        assertEquals(4, cola.extraerMinimo());
        assertEquals(3, cola.extraerMinimo());
        assertEquals(1, cola.extraerMinimo());
        assertTrue(cola.estaVacia());
    }

    @Test
    void losEmpatesSeDesempatanPorElementoMenor() {
        ColaPrioridadMinima cola = new ColaPrioridadMinima();
        cola.insertar(9, 5);
        cola.insertar(3, 5);
        cola.insertar(7, 5);
        assertEquals(3, cola.extraerMinimo());
        assertEquals(7, cola.extraerMinimo());
        assertEquals(9, cola.extraerMinimo());
    }

    @Test
    void permiteElementosRepetidosConDistintaPrioridad() {
        ColaPrioridadMinima cola = new ColaPrioridadMinima();
        cola.insertar(1, 20);
        cola.insertar(1, 5);
        assertEquals(2, cola.tamano());
        assertEquals(5, cola.prioridadMinima());
        assertEquals(1, cola.extraerMinimo());
        assertEquals(20, cola.prioridadMinima());
    }

    @Test
    void creceMasAllaDeLaCapacidadInicial() {
        ColaPrioridadMinima cola = new ColaPrioridadMinima();
        int n = 5000;
        for (int i = 0; i < n; i++) {
            cola.insertar(i, (i * 7919L) % 1009);
        }
        assertEquals(n, cola.tamano());
        long anterior = -1;
        for (int i = 0; i < n; i++) {
            long prioridad = cola.prioridadMinima();
            assertTrue(prioridad >= anterior);
            anterior = prioridad;
            cola.extraerMinimo();
        }
        assertFalse(!cola.estaVacia());
    }

    @Test
    void colaVaciaLanzaExcepcionExplicita() {
        ColaPrioridadMinima cola = new ColaPrioridadMinima();
        assertTrue(cola.estaVacia());
        assertThrows(IllegalStateException.class, cola::extraerMinimo);
        assertThrows(IllegalStateException.class, cola::prioridadMinima);
        assertThrows(IllegalStateException.class, cola::elementoMinimo);
    }
}
