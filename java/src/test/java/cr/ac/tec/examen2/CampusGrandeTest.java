package cr.ac.tec.examen2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cr.ac.tec.examen2.arbol.ArbolJerarquia;
import cr.ac.tec.examen2.arbol.RegistroSala;
import cr.ac.tec.examen2.datos.CargadorCsv;
import cr.ac.tec.examen2.grafo.Corredor;
import cr.ac.tec.examen2.grafo.GrafoEvacuacion;
import cr.ac.tec.examen2.grafo.RutaEvacuacion;
import cr.ac.tec.examen2.informe.InformeBrigada;
import java.io.IOException;
import org.junit.jupiter.api.Test;

/** El conjunto de datos de prueba manual de 400 salas ({@code datos/campus-400}) es válido y coherente. */
class CampusGrandeTest {
    private static final String CARPETA = "../datos/campus-400/";

    private static ArbolJerarquia arbol() throws IOException {
        ArbolJerarquia arbol = new ArbolJerarquia();
        CargadorCsv.cargarOcupantes(CARPETA + "ocupantes.csv", arbol, CargadorCsv.EDIFICIO_POR_DEFECTO);
        return arbol;
    }

    private static GrafoEvacuacion grafo(ArbolJerarquia arbol) throws IOException {
        GrafoEvacuacion grafo = new GrafoEvacuacion();
        CargadorCsv.cargarPasillos(CARPETA + "pasillos.csv", grafo, CargadorCsv.codigosDeSalas(arbol));
        return grafo;
    }

    @Test
    void tiene400SalasEnVariosEdificiosYAlasSinExcederLaCapacidad() throws IOException {
        ArbolJerarquia arbol = arbol();
        assertEquals(400, arbol.cantidadSalas());
        String[] edificios = arbol.edificios();
        assertEquals(5, edificios.length);
        int alas = 0;
        int salas = 0;
        for (int e = 0; e < edificios.length; e++) {
            String[] nombresAlas = arbol.alas(edificios[e]);
            alas += nombresAlas.length;
            assertTrue(nombresAlas.length >= 3, edificios[e]);
            int deEdificio = 0;
            for (int a = 0; a < nombresAlas.length; a++) {
                int enAla = 0;
                for (int i = 0; i < ArbolJerarquia.CAPACIDAD_ALA; i++) {
                    if (arbol.obtenerSalas(edificios[e], nombresAlas[a])[i].registrada()) {
                        enAla++;
                    }
                }
                assertTrue(enAla >= 1 && enAla <= ArbolJerarquia.CAPACIDAD_ALA);
                deEdificio += enAla;
            }
            salas += deEdificio;
        }
        assertEquals(18, alas);
        assertEquals(400, salas);
    }

    @Test
    void losTotalesPorEdificioSumanLosDeSusAlasYLosDeTodasLasSalas() throws IOException {
        ArbolJerarquia arbol = arbol();
        String[] edificios = arbol.edificios();
        int campus = 0;
        for (int e = 0; e < edificios.length; e++) {
            int deAlas = 0;
            String[] alas = arbol.alas(edificios[e]);
            for (int a = 0; a < alas.length; a++) {
                deAlas += arbol.totalAla(edificios[e], alas[a]);
            }
            assertEquals(arbol.totalEdificio(edificios[e]), deAlas);
            campus += deAlas;
        }
        int deSalas = 0;
        RegistroSala[] todas = arbol.recolectarSalas();
        for (int i = 0; i < todas.length; i++) {
            deSalas += todas[i].ocupantes();
        }
        assertEquals(deSalas, campus);
        assertTrue(campus > 4000, "total: " + campus);
    }

    @Test
    void losCodigosDeSalaSonUnicosEnTodoElCampus() throws IOException {
        String[] codigos = CargadorCsv.codigosDeSalas(arbol());
        for (int i = 0; i < codigos.length; i++) {
            for (int j = i + 1; j < codigos.length; j++) {
                assertTrue(!codigos[i].equals(codigos[j]), "repetido: " + codigos[i]);
            }
        }
    }

    @Test
    void hayEmpatesCerosYMaximosParaProbarElOrdenEstable() throws IOException {
        RegistroSala[] salas = arbol().recolectarSalas();
        int ceros = 0;
        int maximos = 0;
        for (int i = 0; i < salas.length; i++) {
            if (salas[i].ocupantes() == 0) {
                ceros++;
            }
            if (salas[i].ocupantes() == ArbolJerarquia.MAX_OCUPANTES_SALA) {
                maximos++;
            }
        }
        assertTrue(ceros >= 3, "ceros: " + ceros);
        assertTrue(maximos >= 3, "máximos: " + maximos);
    }

    @Test
    void elInformeDeBrigadaEstaOrdenadoDeMayorAMenorYEsEstable() throws IOException {
        ArbolJerarquia arbol = arbol();
        RegistroSala[] original = arbol.recolectarSalas();
        RegistroSala[] informe = InformeBrigada.generar(arbol);
        assertEquals(400, informe.length);
        for (int i = 1; i < informe.length; i++) {
            assertTrue(informe[i - 1].ocupantes() >= informe[i].ocupantes());
            if (informe[i - 1].ocupantes() == informe[i].ocupantes()) {
                assertTrue(posicion(original, informe[i - 1]) < posicion(original, informe[i]), "empate inestable en " + i);
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

    @Test
    void elGrafoTieneVariasSalidasYTodasLasSalasTienenRutaAlCargar() throws IOException {
        ArbolJerarquia arbol = arbol();
        GrafoEvacuacion grafo = grafo(arbol);
        assertEquals(8, grafo.cantidadSalidas());
        RutaEvacuacion[] rutas = grafo.rutasDeSalas();
        assertEquals(400, rutas.length);
        for (int i = 0; i < rutas.length; i++) {
            assertTrue(rutas[i].alcanzable(), "sin ruta: " + rutas[i].origen());
        }
    }

    @Test
    void hayCorredoresBloqueadosDesdeElInicioYBloquearUnaSalidaCambiaRutas() throws IOException {
        GrafoEvacuacion grafo = grafo(arbol());
        Corredor[] corredores = grafo.listarCorredores();
        int bloqueados = 0;
        for (int i = 0; i < corredores.length; i++) {
            if (corredores[i].bloqueado()) {
                bloqueados++;
            }
        }
        assertTrue(bloqueados >= 3, "bloqueados: " + bloqueados);
        assertTrue(corredores.length > 450, "corredores: " + corredores.length);

        String origen = grafo.rutasDeSalas()[0].origen();
        RutaEvacuacion antes = grafo.rutaMasRapida(origen);
        String[] camino = antes.ruta();
        grafo.bloquearCorredor(camino[camino.length - 2], camino[camino.length - 1]);
        RutaEvacuacion despues = grafo.rutaMasRapida(origen);
        assertTrue(despues.alcanzable(), "hay redundancia: debe seguir habiendo salida");
        assertTrue(despues.tiempoSegundos() >= antes.tiempoSegundos());
        assertTrue(!(antes.salida().equals(despues.salida()) && antes.tiempoSegundos() == despues.tiempoSegundos()));
    }
}
