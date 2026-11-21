package cr.ac.tec.examen2.consola;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cr.ac.tec.examen2.Datos;
import cr.ac.tec.examen2.arbol.ArbolJerarquia;
import cr.ac.tec.examen2.grafo.GrafoEvacuacion;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import org.junit.jupiter.api.Test;

class ConsolaTest {

    private static String ejecutar(String entrada) throws IOException {
        ArbolJerarquia arbol = Datos.arbolDeCsv();
        GrafoEvacuacion grafo = Datos.grafoDeCsv(arbol);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintStream salida = new PrintStream(bytes, true, "UTF-8");
        new Consola(arbol, grafo, new BufferedReader(new StringReader(entrada)), salida).ejecutar();
        return bytes.toString("UTF-8");
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

    @Test
    void conteosMuestraTotalesPorAlaYPorEdificio() throws IOException {
        String salida = ejecutar("conteos\nsalir\n");
        assertTrue(salida.contains("Edificio Principal: 180 ocupantes"), salida);
        assertTrue(salida.contains("Ala Oeste: 54 ocupantes"), salida);
        assertTrue(salida.contains("Ala Central: 75 ocupantes"), salida);
        assertTrue(salida.contains("Ala Este: 51 ocupantes"), salida);
    }

    @Test
    void salasMuestraLasSalasDelAlaConSuLetra() throws IOException {
        String salida = ejecutar("salas Principal;Oeste\nsalir\n");
        assertTrue(salida.contains("A: LAB-101 (24 ocupantes)"), salida);
        assertTrue(salida.contains("B: LAB-102 (30 ocupantes)"), salida);
    }

    @Test
    void cadaSenalDeAlarmaMuestraElInformeOrdenado() throws IOException {
        String salida = ejecutar("alarma\nalarma\nsalir\n");
        assertEquals(2, contar(salida, "ALARMA"), salida);
        assertEquals(2, contar(salida, "RM-103"), salida);
        int primera = salida.indexOf("RM-103");
        int segunda = salida.indexOf("LIB-001");
        assertTrue(primera >= 0 && primera < segunda);
    }

    @Test
    void laAlarmaRefleja_lasActualizacionesPrevias() throws IOException {
        String salida = ejecutar("alarma\nactualizar Principal;Este;A;80\nalarma\nsalir\n");
        int ultimaAlarma = salida.lastIndexOf("ALARMA");
        String segundoInforme = salida.substring(ultimaAlarma);
        assertTrue(segundoInforme.indexOf("OFF-105") < segundoInforme.indexOf("RM-103"), salida);
    }

    @Test
    void actualizarConfirmaYValidaEntradas() throws IOException {
        String salida = ejecutar("actualizar Principal;Oeste;A;50\nconteos\n"
                + "actualizar Principal;Oeste;A;-5\n"
                + "actualizar Principal;Oeste;A;abc\n"
                + "actualizar Principal;Oeste;A;81\n"
                + "actualizar Fantasma;Oeste;A;5\n"
                + "actualizar Principal;Sur;A;5\n"
                + "actualizar Principal;Oeste;Z;5\n"
                + "actualizar Principal;Oeste;AB;5\n"
                + "actualizar Principal;Oeste\n"
                + "salir\n");
        assertTrue(salida.contains("Ala Oeste: 80 ocupantes"), salida);
        assertEquals(8, contar(salida, "Error"), salida);
    }

    @Test
    void rutasMuestraSalidaRutaYTiempoDeLasSeisSalas() throws IOException {
        String salida = ejecutar("rutas\nsalir\n");
        assertTrue(salida.contains("LAB-101 -> Salida Norte | 30 s | Ruta: LAB-101 > Pasillo A > Salida Norte"), salida);
        assertTrue(salida.contains("RM-103 -> Salida Sur | 37 s"), salida);
        assertTrue(salida.contains("LIB-001 -> Salida Este | 25 s"), salida);
        assertEquals(6, contar(salida, " s | Ruta:"), salida);
    }

    @Test
    void bloquearCambiaLasRutasPosteriores() throws IOException {
        String salida = ejecutar("bloquear Pasillo C;Salida Sur\nrutas\nsalir\n");
        assertTrue(salida.contains("RM-104 -> Salida Este | 53 s"), salida);
        assertFalse(salida.contains("Salida Sur |"), salida);
    }

    @Test
    void bloquearTodasLasSalidasInformaBloqueado() throws IOException {
        String salida = ejecutar("bloquear Pasillo C;Salida Sur\nbloquear Salida Norte;Pasillo A\n"
                + "bloquear LIB-001;Salida Este\nrutas\nsalir\n");
        assertEquals(6, contar(salida, "Bloqueado"), salida);
    }

    @Test
    void bloquearUnCorredorInexistenteOMalFormadoMuestraError() throws IOException {
        String salida = ejecutar("bloquear LAB-101;Salida Sur\nbloquear Pasillo A\nbloquear ;\nsalir\n");
        assertEquals(3, contar(salida, "Error"), salida);
    }

    @Test
    void comandoDesconocidoYLineasVaciasNoDetienenElPrograma() throws IOException {
        String salida = ejecutar("\nvolar\nconteos\nsalir\n");
        assertTrue(salida.contains("Comando desconocido"), salida);
        assertTrue(salida.contains("Edificio Principal: 180 ocupantes"), salida);
    }

    @Test
    void salirDetieneElProcesamientoYElFinDeEntradaTambien() throws IOException {
        String conSalir = ejecutar("salir\nconteos\n");
        assertFalse(conSalir.contains("Edificio Principal"), conSalir);
        String sinSalir = ejecutar("conteos\n");
        assertTrue(sinSalir.contains("Edificio Principal: 180 ocupantes"), sinSalir);
    }

    @Test
    void ayudaListaLosComandos() throws IOException {
        String salida = ejecutar("ayuda\nsalir\n");
        String[] comandos = {"alarma", "conteos", "salas", "actualizar", "rutas", "bloquear", "salir"};
        for (int i = 0; i < comandos.length; i++) {
            assertTrue(salida.contains(comandos[i]), comandos[i]);
        }
    }
}
