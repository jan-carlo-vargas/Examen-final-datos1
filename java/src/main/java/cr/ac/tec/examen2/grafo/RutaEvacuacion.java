package cr.ac.tec.examen2.grafo;

/** Mejor ruta de una sala a una salida; si ninguna salida es alcanzable, {@link #alcanzable()} es falso. */
public final class RutaEvacuacion {
    private final String origen;
    private final String salida;
    private final String[] ruta;
    private final long tiempoSegundos;

    private RutaEvacuacion(String origen, String salida, String[] ruta, long tiempoSegundos) {
        this.origen = origen;
        this.salida = salida;
        this.ruta = ruta;
        this.tiempoSegundos = tiempoSegundos;
    }

    static RutaEvacuacion bloqueada(String origen) {
        return new RutaEvacuacion(origen, null, new String[0], 0);
    }

    static RutaEvacuacion hallada(String origen, String salida, String[] ruta, long tiempoSegundos) {
        return new RutaEvacuacion(origen, salida, ruta, tiempoSegundos);
    }

    public String origen() {
        return origen;
    }

    public boolean alcanzable() {
        return salida != null;
    }

    /** Salida elegida, o {@code null} si no hay salida alcanzable. */
    public String salida() {
        return salida;
    }

    /** Puntos del recorrido desde el origen hasta la salida; vacío si está bloqueado. */
    public String[] ruta() {
        String[] copia = new String[ruta.length];
        System.arraycopy(ruta, 0, copia, 0, ruta.length);
        return copia;
    }

    public long tiempoSegundos() {
        return tiempoSegundos;
    }

    @Override
    public String toString() {
        if (!alcanzable()) {
            return origen + " -> Bloqueado (sin salida alcanzable)";
        }
        StringBuilder texto = new StringBuilder();
        texto.append(origen).append(" -> ").append(salida).append(" | ").append(tiempoSegundos)
                .append(" s | Ruta: ");
        for (int i = 0; i < ruta.length; i++) {
            if (i > 0) {
                texto.append(" > ");
            }
            texto.append(ruta[i]);
        }
        return texto.toString();
    }
}
