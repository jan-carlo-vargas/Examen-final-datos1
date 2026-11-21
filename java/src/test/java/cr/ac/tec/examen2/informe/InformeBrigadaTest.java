package cr.ac.tec.examen2.informe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cr.ac.tec.examen2.Datos;
import cr.ac.tec.examen2.arbol.ArbolJerarquia;
import cr.ac.tec.examen2.arbol.RegistroSala;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class InformeBrigadaTest {

    @Test
    void ordenaLasSalasDelCsvDeMayorAMenorConEmpatesEnOrdenDeRecorrido() throws IOException {
        RegistroSala[] orden = InformeBrigada.generar(Datos.arbolDeCsv());
        String[] esperado = {"RM-103", "LIB-001", "LAB-102", "RM-104", "LAB-101", "OFF-105"};
        assertEquals(esperado.length, orden.length);
        for (int i = 0; i < esperado.length; i++) {
            assertEquals(esperado[i], orden[i].codigo(), "posicion " + i);
        }
        assertEquals(45, orden[0].ocupantes());
        assertEquals(6, orden[5].ocupantes());
    }

    @Test
    void elRecorridoVaPorEdificiosLuegoAlasLuegoLetras() {
        ArbolJerarquia arbol = new ArbolJerarquia();
        arbol.registrarSala("Norte", "Z", 'B', "n-z-b");
        arbol.registrarSala("Norte", "Z", 'A', "n-z-a");
        arbol.registrarSala("Norte", "Y", 'A', "n-y-a");
        arbol.registrarSala("Sur", "X", 'A', "s-x-a");
        // Todas con 0 ocupantes: el resultado es exactamente el orden de recorrido.
        RegistroSala[] orden = InformeBrigada.generar(arbol);
        assertEquals("n-z-a", orden[0].codigo());
        assertEquals("n-z-b", orden[1].codigo());
        assertEquals("n-y-a", orden[2].codigo());
        assertEquals("s-x-a", orden[3].codigo());
    }

    @Test
    void cadaAlarmaVuelveALeerLosOcupantesActuales() throws IOException {
        ArbolJerarquia arbol = Datos.arbolDeCsv();
        assertEquals("RM-103", InformeBrigada.generar(arbol)[0].codigo());
        arbol.actualizarOcupantes("Principal", "Oeste", 'A', 80);
        assertEquals("LAB-101", InformeBrigada.generar(arbol)[0].codigo());
    }

    @Test
    void arbolSinSalasGeneraInformeVacio() {
        assertEquals(0, InformeBrigada.generar(new ArbolJerarquia()).length);
        String texto = InformeBrigada.formatear(InformeBrigada.generar(new ArbolJerarquia()));
        assertTrue(texto.contains("No hay salas"));
    }

    @Test
    void elTextoListaPosicionCodigoYOcupantesEnOrden() throws IOException {
        String texto = InformeBrigada.formatear(InformeBrigada.generar(Datos.arbolDeCsv()));
        int a = texto.indexOf("RM-103");
        int b = texto.indexOf("LIB-001");
        int c = texto.indexOf("LAB-102");
        int d = texto.indexOf("RM-104");
        int e = texto.indexOf("LAB-101");
        int f = texto.indexOf("OFF-105");
        assertTrue(a >= 0 && a < b && b < c && c < d && d < e && e < f, texto);
        assertTrue(texto.contains("45 ocupantes"));
        assertTrue(texto.contains("1."));
        assertTrue(texto.contains("6."));
        assertTrue(texto.contains("Principal"));
    }
}
