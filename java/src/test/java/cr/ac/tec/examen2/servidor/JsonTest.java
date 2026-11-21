package cr.ac.tec.examen2.servidor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class JsonTest {

    @Test
    void textoAgregaComillasYEscapaCaracteresEspeciales() {
        assertEquals("\"hola\"", Json.texto("hola"));
        assertEquals("\"a\\\"b\"", Json.texto("a\"b"));
        assertEquals("\"a\\\\b\"", Json.texto("a\\b"));
        assertEquals("\"l1\\nl2\\tx\"", Json.texto("l1\nl2\tx"));
        assertEquals("\"\\u0001\"", Json.texto("\u0001"));
    }

    @Test
    void textoConservaAcentosYEnie() {
        assertEquals("\"Ni\u00f1os qu\u00edmica\"", Json.texto("Ni\u00f1os qu\u00edmica"));
    }

    @Test
    void nuloSeEscribeComoNull() {
        assertEquals("null", Json.texto(null));
    }
}
