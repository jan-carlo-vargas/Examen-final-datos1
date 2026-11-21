package cr.ac.tec.examen2.arbol;

/** Ranura de una hoja: guarda la letra de su posición, el código de la sala y su cantidad de ocupantes. */
public final class Sala {
    private final char letra;
    private String codigo;
    private int ocupantes;

    Sala(char letra) {
        this.letra = letra;
    }

    public char letra() {
        return letra;
    }

    /** Código de la sala o {@code null} si la ranura no tiene sala registrada. */
    public String codigo() {
        return codigo;
    }

    public int ocupantes() {
        return ocupantes;
    }

    public boolean registrada() {
        return codigo != null;
    }

    void registrar(String codigoSala) {
        this.codigo = codigoSala;
        this.ocupantes = 0;
    }

    void fijarOcupantes(int cantidad) {
        this.ocupantes = cantidad;
    }
}
