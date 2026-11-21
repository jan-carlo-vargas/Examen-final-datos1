package cr.ac.tec.examen2.datos;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cr.ac.tec.examen2.Datos;
import cr.ac.tec.examen2.arbol.ArbolJerarquia;
import cr.ac.tec.examen2.arbol.Sala;
import cr.ac.tec.examen2.grafo.GrafoEvacuacion;
import cr.ac.tec.examen2.grafo.RutaEvacuacion;
import cr.ac.tec.examen2.grafo.TipoPunto;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class CargadorCsvTest {

    private static final String ENC_OCUPANTES = "codigo,nombre,ala,ocupantes\n";
    private static final String ENC_PASILLOS = "desde,hacia,segundos,estado\n";

    // ---- ocupantes.csv real ----

    @Test
    void cargaLasSeisSalasDelCsvEnUnSoloEdificio() throws IOException {
        ArbolJerarquia arbol = new ArbolJerarquia();
        int cargadas = CargadorCsv.cargarOcupantes(Datos.OCUPANTES, arbol, "Principal");
        assertEquals(6, cargadas);
        assertEquals(6, arbol.cantidadSalas());
        assertEquals(1, arbol.edificios().length);
        assertArrayEquals(new String[] {"Oeste", "Central", "Este"}, arbol.alas("Principal"));
    }

    @Test
    void asignaLetrasPorOrdenDeAparicionEnCadaAla() throws IOException {
        ArbolJerarquia arbol = Datos.arbolDeCsv();
        Sala[] oeste = arbol.obtenerSalas("Principal", "Oeste");
        assertEquals("LAB-101", oeste[0].codigo());
        assertEquals(24, oeste[0].ocupantes());
        assertEquals("LAB-102", oeste[1].codigo());
        assertEquals(30, oeste[1].ocupantes());
        Sala[] este = arbol.obtenerSalas("Principal", "Este");
        assertEquals("OFF-105", este[0].codigo());
        assertEquals(6, este[0].ocupantes());
        assertEquals("LIB-001", este[1].codigo());
        assertEquals(45, este[1].ocupantes());
        Sala[] central = arbol.obtenerSalas("Principal", "Central");
        assertEquals("RM-103", central[0].codigo());
        assertEquals("RM-104", central[1].codigo());
    }

    @Test
    void losTotalesPorAlaYEdificioCoincidenConElCsv() throws IOException {
        ArbolJerarquia arbol = Datos.arbolDeCsv();
        assertEquals(54, arbol.totalAla("Principal", "Oeste"));
        assertEquals(75, arbol.totalAla("Principal", "Central"));
        assertEquals(51, arbol.totalAla("Principal", "Este"));
        assertEquals(180, arbol.totalEdificio("Principal"));
    }

    // ---- ocupantes.csv invalido ----

    @Test
    void archivoInexistenteSeReportaComoIOException() {
        assertThrows(FileNotFoundException.class,
                () -> CargadorCsv.cargarOcupantes("no/existe.csv", new ArbolJerarquia(), "P"));
        assertThrows(FileNotFoundException.class,
                () -> CargadorCsv.cargarPasillos("no/existe.csv", new GrafoEvacuacion(), new String[0]));
    }

    @Test
    void encabezadoIncorrectoSeRechaza() throws IOException {
        String ruta = Datos.temporal("codigo,ala,ocupantes\nA,Oeste,3\n");
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> CargadorCsv.cargarOcupantes(ruta, new ArbolJerarquia(), "P"));
        assertTrue(e.getMessage().contains("ncabezado"));
    }

    @Test
    void archivoVacioSeRechaza() throws IOException {
        String ruta = Datos.temporal("");
        assertThrows(IllegalArgumentException.class,
                () -> CargadorCsv.cargarOcupantes(ruta, new ArbolJerarquia(), "P"));
    }

    @Test
    void filaConColumnasIncorrectasIndicaLaLinea() throws IOException {
        String ruta = Datos.temporal(ENC_OCUPANTES + "R1,Sala,Oeste,3\nR2,Sala,Oeste\n");
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> CargadorCsv.cargarOcupantes(ruta, new ArbolJerarquia(), "P"));
        assertTrue(e.getMessage().contains("3"), e.getMessage());
    }

    @Test
    void ocupantesNoNumericosNegativosOFueraDeRangoSeRechazan() throws IOException {
        String[] malos = {"abc", "-4", "81", "3.5", ""};
        for (int i = 0; i < malos.length; i++) {
            String ruta = Datos.temporal(ENC_OCUPANTES + "R1,Sala,Oeste," + malos[i] + "\n");
            assertThrows(IllegalArgumentException.class,
                    () -> CargadorCsv.cargarOcupantes(ruta, new ArbolJerarquia(), "P"),
                    "valor: '" + malos[i] + "'");
        }
    }

    @Test
    void codigoRepetidoEnLaMismaAlaSeRechaza() throws IOException {
        String ruta = Datos.temporal(ENC_OCUPANTES + "R1,Sala,Oeste,3\nR1,Otra,Oeste,4\n");
        assertThrows(IllegalArgumentException.class,
                () -> CargadorCsv.cargarOcupantes(ruta, new ArbolJerarquia(), "P"));
    }

    @Test
    void masDe27SalasEnUnAlaSeRechaza() throws IOException {
        StringBuilder sb = new StringBuilder(ENC_OCUPANTES);
        for (int i = 0; i < 28; i++) {
            sb.append("R").append(i).append(",Sala,Oeste,1\n");
        }
        String ruta = Datos.temporal(sb.toString());
        assertThrows(IllegalArgumentException.class,
                () -> CargadorCsv.cargarOcupantes(ruta, new ArbolJerarquia(), "P"));
    }

    @Test
    void ignoraLineasEnBlancoFinalesYAceptaFinesDeLineaWindows() throws IOException {
        String ruta = Datos.temporal("codigo,nombre,ala,ocupantes\r\nR1,Sala uno,Oeste,3\r\n\r\n");
        ArbolJerarquia arbol = new ArbolJerarquia();
        assertEquals(1, CargadorCsv.cargarOcupantes(ruta, arbol, "P"));
        assertEquals(3, arbol.totalEdificio("P"));
    }

    @Test
    void laCargaConAcentosYEnieSeLeeEnUtf8() throws IOException {
        String ruta = Datos.temporal(ENC_OCUPANTES + "R1,Laboratorio de química,Niños,3\n");
        ArbolJerarquia arbol = new ArbolJerarquia();
        CargadorCsv.cargarOcupantes(ruta, arbol, "P");
        assertEquals("Niños", arbol.alas("P")[0]);
    }

    // ---- pasillos.csv real ----

    @Test
    void cargaElGrafoRealConSalasPasillosYTresSalidas() throws IOException {
        GrafoEvacuacion g = Datos.grafoDeCsv(Datos.arbolDeCsv());
        assertEquals(12, g.cantidadPuntos());
        assertEquals(6, countTipo(g, TipoPunto.SALA));
        assertEquals(3, g.cantidadSalidas());
        assertEquals(TipoPunto.SALIDA, g.tipo("Salida Norte"));
        assertEquals(TipoPunto.SALIDA, g.tipo("Salida Este"));
        assertEquals(TipoPunto.SALIDA, g.tipo("Salida Sur"));
        assertEquals(TipoPunto.PASILLO, g.tipo("Pasillo A"));
        assertEquals(TipoPunto.SALA, g.tipo("LIB-001"));
    }

    private static int countTipo(GrafoEvacuacion g, TipoPunto tipo) {
        int total = 0;
        for (int i = 0; i < g.cantidadPuntos(); i++) {
            if (g.tipo(g.nombre(i)) == tipo) {
                total++;
            }
        }
        return total;
    }

    @Test
    void elCorredorBloqueadoDelCsvSeEliminaEnAmbasDirecciones() throws IOException {
        GrafoEvacuacion g = Datos.grafoDeCsv(Datos.arbolDeCsv());
        assertEquals(14, g.cantidadCorredores()); // 15 filas, 1 bloqueada
        assertFalse(g.existeCorredor("Pasillo B", "Salida Este"));
        assertFalse(g.existeCorredor("Salida Este", "Pasillo B"));
        assertTrue(g.existeCorredor("Pasillo B", "RM-103"));
        assertEquals(25, g.pesoCorredor("LIB-001", "Salida Este"));
    }

    private static void verificar(RutaEvacuacion r, String origen, String salida, long tiempo, String... ruta) {
        assertEquals(origen, r.origen());
        assertTrue(r.alcanzable(), origen);
        assertEquals(salida, r.salida(), origen);
        assertEquals(tiempo, r.tiempoSegundos(), origen);
        assertArrayEquals(ruta, r.ruta(), origen);
    }

    @Test
    void rutasDeLasSeisSalasDeEjemploCoincidenConDijkstra() throws IOException {
        GrafoEvacuacion g = Datos.grafoDeCsv(Datos.arbolDeCsv());
        RutaEvacuacion[] rutas = g.rutasDeSalas();
        assertEquals(6, rutas.length);
        verificar(rutas[0], "LAB-101", "Salida Norte", 30, "LAB-101", "Pasillo A", "Salida Norte");
        verificar(rutas[1], "LAB-102", "Salida Norte", 35, "LAB-102", "Pasillo A", "Salida Norte");
        verificar(rutas[2], "RM-103", "Salida Sur", 37, "RM-103", "RM-104", "Pasillo C", "Salida Sur");
        verificar(rutas[3], "RM-104", "Salida Sur", 27, "RM-104", "Pasillo C", "Salida Sur");
        verificar(rutas[4], "OFF-105", "Salida Sur", 25, "OFF-105", "Pasillo C", "Salida Sur");
        verificar(rutas[5], "LIB-001", "Salida Este", 25, "LIB-001", "Salida Este");
    }

    @Test
    void bloqueandoElCorredorASalidaSurLasRutasSeRedirigen() throws IOException {
        GrafoEvacuacion g = Datos.grafoDeCsv(Datos.arbolDeCsv());
        g.bloquearCorredor("Pasillo C", "Salida Sur");
        RutaEvacuacion[] rutas = g.rutasDeSalas();
        verificar(rutas[0], "LAB-101", "Salida Norte", 30, "LAB-101", "Pasillo A", "Salida Norte");
        verificar(rutas[2], "RM-103", "Salida Este", 49, "RM-103", "Pasillo B", "LIB-001", "Salida Este");
        verificar(rutas[3], "RM-104", "Salida Este", 53,
                "RM-104", "Pasillo C", "OFF-105", "LIB-001", "Salida Este");
        verificar(rutas[4], "OFF-105", "Salida Este", 37, "OFF-105", "LIB-001", "Salida Este");
        verificar(rutas[5], "LIB-001", "Salida Este", 25, "LIB-001", "Salida Este");
    }

    @Test
    void bloqueandoDosSalidasTodasUsanLaUnicaRestante() throws IOException {
        GrafoEvacuacion g = Datos.grafoDeCsv(Datos.arbolDeCsv());
        g.bloquearCorredor("Pasillo C", "Salida Sur");
        g.bloquearCorredor("Salida Norte", "Pasillo A");
        RutaEvacuacion[] rutas = g.rutasDeSalas();
        verificar(rutas[0], "LAB-101", "Salida Este", 59,
                "LAB-101", "LAB-102", "Pasillo B", "LIB-001", "Salida Este");
        verificar(rutas[1], "LAB-102", "Salida Este", 53, "LAB-102", "Pasillo B", "LIB-001", "Salida Este");
    }

    @Test
    void sinNingunaSalidaAlcanzableTodasLasSalasQuedanBloqueadas() throws IOException {
        GrafoEvacuacion g = Datos.grafoDeCsv(Datos.arbolDeCsv());
        g.bloquearCorredor("Pasillo C", "Salida Sur");
        g.bloquearCorredor("Salida Norte", "Pasillo A");
        g.bloquearCorredor("LIB-001", "Salida Este");
        RutaEvacuacion[] rutas = g.rutasDeSalas();
        assertEquals(6, rutas.length);
        for (int i = 0; i < rutas.length; i++) {
            assertFalse(rutas[i].alcanzable(), rutas[i].origen());
            assertTrue(rutas[i].toString().contains("Bloqueado"));
        }
    }

    @Test
    void unaSalaDelArbolSinCorredoresQuedaAisladaYBloqueada() throws IOException {
        String ruta = Datos.temporal(ENC_PASILLOS + "Pasillo X,Salida Norte,5,Abierto\n");
        GrafoEvacuacion g = new GrafoEvacuacion();
        CargadorCsv.cargarPasillos(ruta, g, new String[] {"R1"});
        assertEquals(TipoPunto.SALA, g.tipo("R1"));
        assertFalse(g.rutaMasRapida("R1").alcanzable());
    }

    // ---- pasillos.csv invalido ----

    @Test
    void pasillosConEncabezadoOFilasInvalidasSeRechazan() throws IOException {
        String[] malos = {
            "desde,hacia,segundos\nA,B,3\n",
            ENC_PASILLOS + "A,B,3\n",
            ENC_PASILLOS + "A,B,tres,Abierto\n",
            ENC_PASILLOS + "A,B,-3,Abierto\n",
            ENC_PASILLOS + "A,B,3,Roto\n",
            ENC_PASILLOS + "A,A,3,Abierto\n",
            ENC_PASILLOS + "A,B,3,Abierto\nB,A,4,Abierto\n",
            ENC_PASILLOS + ",B,3,Abierto\n",
        };
        for (int i = 0; i < malos.length; i++) {
            String ruta = Datos.temporal(malos[i]);
            assertThrows(IllegalArgumentException.class,
                    () -> CargadorCsv.cargarPasillos(ruta, new GrafoEvacuacion(), new String[0]),
                    "caso " + i);
        }
    }

    @Test
    void elEstadoSeAceptaSinDistinguirMayusculas() throws IOException {
        String ruta = Datos.temporal(ENC_PASILLOS + "A,B,3,ABIERTO\nB,C,4,bloqueado\n");
        GrafoEvacuacion g = new GrafoEvacuacion();
        assertEquals(2, CargadorCsv.cargarPasillos(ruta, g, new String[0]));
        assertTrue(g.existeCorredor("A", "B"));
        assertFalse(g.existeCorredor("B", "C"));
    }

    // ---- ocupantes.csv con columna de edificio (campus grande) ----

    private static final String ENC_OCUPANTES_EDIFICIO = "codigo,nombre,edificio,ala,ocupantes\n";

    @Test
    void conColumnaEdificioCadaSalaVaASuEdificio() throws IOException {
        String ruta = Datos.temporal(ENC_OCUPANTES_EDIFICIO
                + "A-1,Aula,Ciencias,Norte,10\nA-2,Aula,Ciencias,Sur,20\nB-1,Aula,Artes,Norte,5\n");
        ArbolJerarquia arbol = new ArbolJerarquia();
        assertEquals(3, CargadorCsv.cargarOcupantes(ruta, arbol, "Principal"));
        assertArrayEquals(new String[] {"Ciencias", "Artes"}, arbol.edificios());
        assertArrayEquals(new String[] {"Norte", "Sur"}, arbol.alas("Ciencias"));
        assertEquals(30, arbol.totalEdificio("Ciencias"));
        assertEquals(5, arbol.totalAla("Artes", "Norte"));
    }

    @Test
    void conColumnaEdificioElMismoNombreDeAlaPuedeExistirEnDosEdificios() throws IOException {
        String ruta = Datos.temporal(ENC_OCUPANTES_EDIFICIO
                + "A-1,Aula,Ciencias,Norte,10\nB-1,Aula,Artes,Norte,5\n");
        ArbolJerarquia arbol = new ArbolJerarquia();
        CargadorCsv.cargarOcupantes(ruta, arbol, "Principal");
        assertEquals("A-1", arbol.obtenerSalas("Ciencias", "Norte")[0].codigo());
        assertEquals("B-1", arbol.obtenerSalas("Artes", "Norte")[0].codigo());
    }

    @Test
    void conColumnaEdificioUnaFilaDeCuatroColumnasSeRechazaIndicandoLaLinea() throws IOException {
        String ruta = Datos.temporal(ENC_OCUPANTES_EDIFICIO + "A-1,Aula,Ciencias,Norte,10\nA-2,Aula,Norte,3\n");
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> CargadorCsv.cargarOcupantes(ruta, new ArbolJerarquia(), "P"));
        assertTrue(e.getMessage().contains("3"), e.getMessage());
        assertTrue(e.getMessage().contains("5"), e.getMessage());
    }

    @Test
    void conColumnaEdificioUnEdificioVacioSeRechaza() throws IOException {
        String ruta = Datos.temporal(ENC_OCUPANTES_EDIFICIO + "A-1,Aula, ,Norte,10\n");
        assertThrows(IllegalArgumentException.class,
                () -> CargadorCsv.cargarOcupantes(ruta, new ArbolJerarquia(), "P"));
    }
}
