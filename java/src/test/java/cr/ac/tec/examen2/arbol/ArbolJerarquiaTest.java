package cr.ac.tec.examen2.arbol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cr.ac.tec.examen2.ElementoNoEncontradoException;
import org.junit.jupiter.api.Test;

class ArbolJerarquiaTest {

    private static ArbolJerarquia ejemplo() {
        ArbolJerarquia arbol = new ArbolJerarquia();
        arbol.registrarSala("Principal", "Oeste", 'A', "LAB-101");
        arbol.registrarSala("Principal", "Oeste", 'B', "LAB-102");
        arbol.registrarSala("Principal", "Este", 'A', "OFF-105");
        arbol.registrarSala("Anexo", "Norte", 'A', "AN-1");
        return arbol;
    }

    @Test
    void tieneExactamenteCuatroNiveles() {
        ArbolJerarquia arbol = ejemplo();
        NodoGuia raiz = arbol.raiz();
        assertEquals(0, raiz.nivel());
        NodoArbol edificio = raiz.hijo(0);
        assertEquals(1, edificio.nivel());
        NodoArbol ala = ((NodoGuia) edificio).hijo(0);
        assertEquals(2, ala.nivel());
        NodoArbol hoja = ((NodoGuia) ala).hijo(0);
        assertEquals(3, hoja.nivel());
        assertTrue(hoja instanceof NodoHoja);
        assertEquals(4, ArbolJerarquia.NIVELES);
    }

    @Test
    void laRaizEnlazaLosEdificiosYCadaEdificioSusAlas() {
        ArbolJerarquia arbol = ejemplo();
        assertEquals(2, arbol.raiz().cantidadHijos());
        assertEquals("Principal", ((NodoGuia) arbol.raiz().hijo(0)).nombre());
        assertEquals("Anexo", ((NodoGuia) arbol.raiz().hijo(1)).nombre());
        assertEquals(2, ((NodoGuia) arbol.raiz().hijo(0)).cantidadHijos());
    }

    @Test
    void laHojaTieneUnArregloFijoDe27Salas() {
        ArbolJerarquia arbol = ejemplo();
        Sala[] salas = arbol.obtenerSalas("Principal", "Oeste");
        assertEquals(27, salas.length);
        assertEquals(27, ArbolJerarquia.CAPACIDAD_ALA);
    }

    @Test
    void elArregloSigueElAlfabetoEspanolConEnieEntreNyO() {
        Sala[] salas = ejemplo().obtenerSalas("Principal", "Oeste");
        String esperado = "ABCDEFGHIJKLMNÑOPQRSTUVWXYZ";
        for (int i = 0; i < 27; i++) {
            assertEquals(esperado.charAt(i), salas[i].letra(), "posicion " + i);
        }
        assertEquals('N', salas[13].letra());
        assertEquals('Ñ', salas[14].letra());
        assertEquals('O', salas[15].letra());
    }

    @Test
    void obtenerSalasDevuelveLasSalasRegistradasConSuCodigoYCero() {
        Sala[] salas = ejemplo().obtenerSalas("Principal", "Oeste");
        assertTrue(salas[0].registrada());
        assertEquals("LAB-101", salas[0].codigo());
        assertEquals(0, salas[0].ocupantes());
        assertEquals("LAB-102", salas[1].codigo());
        assertFalse(salas[2].registrada());
        assertNull(salas[2].codigo());
    }

    @Test
    void obtenerSalasDevuelveUnaCopiaDelArreglo() {
        ArbolJerarquia arbol = ejemplo();
        Sala[] salas = arbol.obtenerSalas("Principal", "Oeste");
        salas[0] = null;
        assertTrue(arbol.obtenerSalas("Principal", "Oeste")[0] != null);
    }

    @Test
    void lasAlasConLasMismasLetrasSonIndependientes() {
        ArbolJerarquia arbol = ejemplo();
        arbol.actualizarOcupantes("Principal", "Oeste", 'A', 10);
        assertEquals(10, arbol.obtenerSalas("Principal", "Oeste")[0].ocupantes());
        assertEquals(0, arbol.obtenerSalas("Principal", "Este")[0].ocupantes());
        assertEquals(0, arbol.obtenerSalas("Anexo", "Norte")[0].ocupantes());
    }

    @Test
    void actualizarOcupantesCambiaLaSalaIndicada() {
        ArbolJerarquia arbol = ejemplo();
        arbol.actualizarOcupantes("Principal", "Oeste", 'B', 42);
        assertEquals(42, arbol.obtenerSalas("Principal", "Oeste")[1].ocupantes());
        arbol.actualizarOcupantes("Principal", "Oeste", 'B', 0);
        assertEquals(0, arbol.obtenerSalas("Principal", "Oeste")[1].ocupantes());
        arbol.actualizarOcupantes("Principal", "Oeste", 'B', 80);
        assertEquals(80, arbol.obtenerSalas("Principal", "Oeste")[1].ocupantes());
    }

    @Test
    void laEnieSeAceptaEnMayusculaYMinuscula() {
        ArbolJerarquia arbol = ejemplo();
        arbol.registrarSala("Principal", "Oeste", 'ñ', "SALA-NYE");
        assertEquals("SALA-NYE", arbol.obtenerSalas("Principal", "Oeste")[14].codigo());
        arbol.actualizarOcupantes("Principal", "Oeste", 'Ñ', 7);
        assertEquals(7, arbol.obtenerSalas("Principal", "Oeste")[14].ocupantes());
        arbol.actualizarOcupantes("Principal", "Oeste", 'ñ', 9);
        assertEquals(9, arbol.obtenerSalas("Principal", "Oeste")[14].ocupantes());
    }

    @Test
    void totalDeUnaAlaSumaSusSalas() {
        ArbolJerarquia arbol = ejemplo();
        arbol.actualizarOcupantes("Principal", "Oeste", 'A', 24);
        arbol.actualizarOcupantes("Principal", "Oeste", 'B', 30);
        assertEquals(54, arbol.totalAla("Principal", "Oeste"));
        assertEquals(0, arbol.totalAla("Principal", "Este"));
    }

    @Test
    void totalDeUnEdificioSumaTodasSusAlas() {
        ArbolJerarquia arbol = ejemplo();
        arbol.actualizarOcupantes("Principal", "Oeste", 'A', 24);
        arbol.actualizarOcupantes("Principal", "Oeste", 'B', 30);
        arbol.actualizarOcupantes("Principal", "Este", 'A', 6);
        arbol.actualizarOcupantes("Anexo", "Norte", 'A', 11);
        assertEquals(60, arbol.totalEdificio("Principal"));
        assertEquals(11, arbol.totalEdificio("Anexo"));
    }

    @Test
    void losTotalesSeRecalculanTrasActualizar() {
        ArbolJerarquia arbol = ejemplo();
        arbol.actualizarOcupantes("Principal", "Oeste", 'A', 20);
        assertEquals(20, arbol.totalEdificio("Principal"));
        arbol.actualizarOcupantes("Principal", "Oeste", 'A', 5);
        assertEquals(5, arbol.totalEdificio("Principal"));
    }

    @Test
    void edificiosYAlasSeRecorrenEnOrdenDeRegistro() {
        ArbolJerarquia arbol = ejemplo();
        String[] edificios = arbol.edificios();
        assertEquals(2, edificios.length);
        assertEquals("Principal", edificios[0]);
        assertEquals("Anexo", edificios[1]);
        String[] alas = arbol.alas("Principal");
        assertEquals(2, alas.length);
        assertEquals("Oeste", alas[0]);
        assertEquals("Este", alas[1]);
    }

    @Test
    void registrarSalaSiguienteAsignaLetrasConsecutivas() {
        ArbolJerarquia arbol = new ArbolJerarquia();
        assertEquals('A', arbol.registrarSala("P", "X", "S1"));
        assertEquals('B', arbol.registrarSala("P", "X", "S2"));
        assertEquals('C', arbol.registrarSala("P", "X", "S3"));
        assertEquals("S3", arbol.obtenerSalas("P", "X")[2].codigo());
    }

    @Test
    void unAlaLlenaRechazaLaSala28() {
        ArbolJerarquia arbol = new ArbolJerarquia();
        for (int i = 0; i < 27; i++) {
            arbol.registrarSala("P", "X", "S" + i);
        }
        assertEquals('Ñ', arbol.obtenerSalas("P", "X")[14].letra());
        assertThrows(IllegalArgumentException.class, () -> arbol.registrarSala("P", "X", "S27"));
    }

    @Test
    void cantidadDeSalasCuentaSoloLasRegistradas() {
        assertEquals(4, ejemplo().cantidadSalas());
        assertEquals(0, new ArbolJerarquia().cantidadSalas());
    }

    @Test
    void recolectarSalasUsaOrdenDeEdificiosAlasYLetras() {
        RegistroSala[] salas = ejemplo().recolectarSalas();
        assertEquals(4, salas.length);
        assertEquals("LAB-101", salas[0].codigo());
        assertEquals("LAB-102", salas[1].codigo());
        assertEquals("OFF-105", salas[2].codigo());
        assertEquals("AN-1", salas[3].codigo());
        assertEquals("Principal", salas[0].edificio());
        assertEquals("Oeste", salas[0].ala());
        assertEquals('A', salas[0].letra());
    }

    // ---- errores explicitos ----

    @Test
    void edificioInexistenteSeReportaEnTodasLasOperaciones() {
        ArbolJerarquia arbol = ejemplo();
        assertThrows(ElementoNoEncontradoException.class, () -> arbol.obtenerSalas("Fantasma", "Oeste"));
        assertThrows(ElementoNoEncontradoException.class,
                () -> arbol.actualizarOcupantes("Fantasma", "Oeste", 'A', 1));
        assertThrows(ElementoNoEncontradoException.class, () -> arbol.totalAla("Fantasma", "Oeste"));
        assertThrows(ElementoNoEncontradoException.class, () -> arbol.totalEdificio("Fantasma"));
        assertThrows(ElementoNoEncontradoException.class, () -> arbol.alas("Fantasma"));
    }

    @Test
    void alaInexistenteSeReportaEnTodasLasOperaciones() {
        ArbolJerarquia arbol = ejemplo();
        assertThrows(ElementoNoEncontradoException.class, () -> arbol.obtenerSalas("Principal", "Sur"));
        assertThrows(ElementoNoEncontradoException.class,
                () -> arbol.actualizarOcupantes("Principal", "Sur", 'A', 1));
        assertThrows(ElementoNoEncontradoException.class, () -> arbol.totalAla("Principal", "Sur"));
        // El ala existe en otro edificio, pero no en este.
        assertThrows(ElementoNoEncontradoException.class, () -> arbol.totalAla("Anexo", "Oeste"));
    }

    @Test
    void salaNoRegistradaSeReportaAlActualizar() {
        ArbolJerarquia arbol = ejemplo();
        assertThrows(ElementoNoEncontradoException.class,
                () -> arbol.actualizarOcupantes("Principal", "Oeste", 'Z', 3));
        assertEquals(0, arbol.obtenerSalas("Principal", "Oeste")[26].ocupantes());
    }

    @Test
    void ocupantesInvalidosSeRechazanYNoCambianElValor() {
        ArbolJerarquia arbol = ejemplo();
        arbol.actualizarOcupantes("Principal", "Oeste", 'A', 33);
        assertThrows(IllegalArgumentException.class,
                () -> arbol.actualizarOcupantes("Principal", "Oeste", 'A', -1));
        assertThrows(IllegalArgumentException.class,
                () -> arbol.actualizarOcupantes("Principal", "Oeste", 'A', 81));
        assertThrows(IllegalArgumentException.class,
                () -> arbol.actualizarOcupantes("Principal", "Oeste", 'A', Integer.MIN_VALUE));
        assertThrows(IllegalArgumentException.class,
                () -> arbol.actualizarOcupantes("Principal", "Oeste", 'A', Integer.MAX_VALUE));
        assertEquals(33, arbol.obtenerSalas("Principal", "Oeste")[0].ocupantes());
        assertEquals(80, ArbolJerarquia.MAX_OCUPANTES_SALA);
    }

    @Test
    void letrasFueraDelAlfabetoSeRechazan() {
        ArbolJerarquia arbol = ejemplo();
        assertThrows(IllegalArgumentException.class,
                () -> arbol.actualizarOcupantes("Principal", "Oeste", '1', 3));
        assertThrows(IllegalArgumentException.class,
                () -> arbol.actualizarOcupantes("Principal", "Oeste", '*', 3));
        assertThrows(IllegalArgumentException.class,
                () -> arbol.registrarSala("Principal", "Oeste", '#', "X"));
        assertThrows(IllegalArgumentException.class,
                () -> arbol.registrarSala("Principal", "Oeste", 'é', "X"));
    }

    @Test
    void nombresNulosOVaciosSeRechazan() {
        ArbolJerarquia arbol = ejemplo();
        assertThrows(IllegalArgumentException.class, () -> arbol.obtenerSalas(null, "Oeste"));
        assertThrows(IllegalArgumentException.class, () -> arbol.obtenerSalas("Principal", null));
        assertThrows(IllegalArgumentException.class, () -> arbol.obtenerSalas("", "Oeste"));
        assertThrows(IllegalArgumentException.class, () -> arbol.obtenerSalas("Principal", "   "));
        assertThrows(IllegalArgumentException.class, () -> arbol.totalEdificio(null));
        assertThrows(IllegalArgumentException.class, () -> arbol.totalAla(null, null));
        assertThrows(IllegalArgumentException.class, () -> arbol.registrarSala(null, "A", 'A', "X"));
        assertThrows(IllegalArgumentException.class, () -> arbol.registrarSala("P", "", 'A', "X"));
        assertThrows(IllegalArgumentException.class, () -> arbol.registrarSala("P", "A", 'A', null));
        assertThrows(IllegalArgumentException.class, () -> arbol.registrarSala("P", "A", 'A', " "));
    }

    @Test
    void registrarUnaSalaDosVecesSeRechaza() {
        ArbolJerarquia arbol = ejemplo();
        assertThrows(IllegalArgumentException.class,
                () -> arbol.registrarSala("Principal", "Oeste", 'A', "OTRA"));
        assertEquals("LAB-101", arbol.obtenerSalas("Principal", "Oeste")[0].codigo());
    }

    @Test
    void unCodigoRepetidoEnLaMismaAlaSeRechaza() {
        ArbolJerarquia arbol = ejemplo();
        assertThrows(IllegalArgumentException.class,
                () -> arbol.registrarSala("Principal", "Oeste", 'C', "LAB-101"));
        assertThrows(IllegalArgumentException.class,
                () -> arbol.registrarSala("Principal", "Oeste", "LAB-102"));
    }

    @Test
    void elArbolCrecePastaMuchosEdificiosYAlasSinPerderOrden() {
        ArbolJerarquia arbol = new ArbolJerarquia();
        for (int e = 0; e < 40; e++) {
            for (int a = 0; a < 12; a++) {
                arbol.registrarSala("E" + e, "A" + a, "S-" + e + "-" + a);
            }
        }
        assertEquals(40, arbol.edificios().length);
        assertEquals("E39", arbol.edificios()[39]);
        assertEquals(12, arbol.alas("E7").length);
        assertEquals("A11", arbol.alas("E7")[11]);
        assertEquals(480, arbol.cantidadSalas());
        arbol.actualizarOcupantes("E39", "A11", 'A', 70);
        assertEquals(70, arbol.totalEdificio("E39"));
    }
}
