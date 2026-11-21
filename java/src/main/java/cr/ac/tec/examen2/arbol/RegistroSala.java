package cr.ac.tec.examen2.arbol;

/** Copia inmutable de una sala junto con su ubicación (edificio, ala y letra). */
public final class RegistroSala {
    private final String edificio;
    private final String ala;
    private final char letra;
    private final String codigo;
    private final int ocupantes;

    public RegistroSala(String edificio, String ala, char letra, String codigo, int ocupantes) {
        this.edificio = edificio;
        this.ala = ala;
        this.letra = letra;
        this.codigo = codigo;
        this.ocupantes = ocupantes;
    }

    public String edificio() {
        return edificio;
    }

    public String ala() {
        return ala;
    }

    public char letra() {
        return letra;
    }

    public String codigo() {
        return codigo;
    }

    public int ocupantes() {
        return ocupantes;
    }
}
