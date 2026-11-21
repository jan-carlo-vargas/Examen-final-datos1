package cr.ac.tec.examen2.datos;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import cr.ac.tec.examen2.ElementoNoEncontradoException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.jupiter.api.Test;

class CatalogoDatosTest {
    private static final String BASE = "../datos";

    private static void escribir(File archivo, String texto) throws IOException {
        try (FileOutputStream f = new FileOutputStream(archivo)) {
            f.write(texto.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static File base() throws IOException {
        File base = Files.createTempDirectory("catalogo").toFile();
        base.deleteOnExit();
        return base;
    }

    private static File conjunto(File base, String nombre, String ocupantes, String pasillos) throws IOException {
        File carpeta = new File(base, nombre);
        carpeta.mkdir();
        carpeta.deleteOnExit();
        if (ocupantes != null) {
            File o = new File(carpeta, "ocupantes.csv");
            escribir(o, ocupantes);
            o.deleteOnExit();
        }
        if (pasillos != null) {
            File p = new File(carpeta, "pasillos.csv");
            escribir(p, pasillos);
            p.deleteOnExit();
        }
        return carpeta;
    }

    @Test
    void listaSoloLasCarpetasConLosDosCsvEnOrdenAlfabetico() {
        CatalogoDatos catalogo = new CatalogoDatos(BASE, "version-b");
        assertArrayEquals(new String[] {"campus-400", "version-b"}, catalogo.nombres());
        assertEquals("version-b", catalogo.actual());
    }

    @Test
    void ignoraCarpetasIncompletas() throws IOException {
        File base = base();
        conjunto(base, "zeta", "codigo,nombre,ala,ocupantes\n", "desde,hacia,segundos,estado\n");
        conjunto(base, "alfa", "codigo,nombre,ala,ocupantes\n", "desde,hacia,segundos,estado\n");
        conjunto(base, "incompleta", "codigo,nombre,ala,ocupantes\n", null);
        assertArrayEquals(new String[] {"alfa", "zeta"}, new CatalogoDatos(base.getPath(), "alfa").nombres());
    }

    @Test
    void cargaUnConjuntoYLoMarcaComoActual() throws IOException {
        CatalogoDatos catalogo = new CatalogoDatos(BASE, "version-b");
        CatalogoDatos.ConjuntoDatos datos = catalogo.cargar("campus-400");
        assertEquals("campus-400", datos.nombre());
        assertEquals(400, datos.salas());
        assertEquals(400, datos.arbol().cantidadSalas());
        assertEquals(8, datos.grafo().cantidadSalidas());
        assertEquals("campus-400", catalogo.actual());
    }

    @Test
    void cargarElConjuntoPequenoDaLasSeisSalasDelEnunciado() throws IOException {
        CatalogoDatos.ConjuntoDatos datos = new CatalogoDatos(BASE, "campus-400").cargar("version-b");
        assertEquals(6, datos.salas());
        assertEquals(180, datos.arbol().totalEdificio("Principal"));
    }

    @Test
    void rechazaNombresQueNoSonCarpetasDelCatalogo() {
        CatalogoDatos catalogo = new CatalogoDatos(BASE, "version-b");
        assertThrows(ElementoNoEncontradoException.class, () -> catalogo.cargar("no-existe"));
        assertThrows(ElementoNoEncontradoException.class, () -> catalogo.cargar("../datos/version-b"));
        assertThrows(ElementoNoEncontradoException.class, () -> catalogo.cargar("version-a"));
        assertThrows(IllegalArgumentException.class, () -> catalogo.cargar(""));
        assertEquals("version-b", catalogo.actual());
    }

    @Test
    void siUnConjuntoEstaCorruptoSeReportaYElActualNoCambia() throws IOException {
        File base = base();
        conjunto(base, "bueno", "codigo,nombre,ala,ocupantes\nR1,Sala,Oeste,3\n", "desde,hacia,segundos,estado\nSalida A,R1,5,Abierto\n");
        conjunto(base, "malo", "codigo,nombre,ala,ocupantes\nR1,Sala,Oeste,999\n", "desde,hacia,segundos,estado\n");
        CatalogoDatos catalogo = new CatalogoDatos(base.getPath(), "bueno");
        assertThrows(IllegalArgumentException.class, () -> catalogo.cargar("malo"));
        assertEquals("bueno", catalogo.actual());
    }
}
