package cr.ac.tec.examen2.grafo;

/** Registro de un corredor y su estado (abierto o bloqueado), en el sentido en que fue registrado. */
public final class Corredor {
    private final String desde;
    private final String hacia;
    private final int segundos;
    private final boolean bloqueado;

    Corredor(String desde, String hacia, int segundos, boolean bloqueado) {
        this.desde = desde;
        this.hacia = hacia;
        this.segundos = segundos;
        this.bloqueado = bloqueado;
    }

    public String desde() {
        return desde;
    }

    public String hacia() {
        return hacia;
    }

    public int segundos() {
        return segundos;
    }

    public boolean bloqueado() {
        return bloqueado;
    }
}
