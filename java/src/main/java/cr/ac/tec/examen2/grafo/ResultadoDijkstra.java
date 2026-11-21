package cr.ac.tec.examen2.grafo;

/** Distancias mínimas (segundos) y predecesores desde un origen; índices según {@link GrafoEvacuacion}. */
public final class ResultadoDijkstra {
    public static final long INFINITO = Long.MAX_VALUE;

    private final long[] distancias;
    private final int[] previos;

    ResultadoDijkstra(long[] distancias, int[] previos) {
        this.distancias = distancias;
        this.previos = previos;
    }

    public long distancia(int punto) {
        return distancias[punto];
    }

    public boolean alcanzable(int punto) {
        return distancias[punto] != INFINITO;
    }

    /** Punto anterior en el camino mínimo, o -1 para el origen y los puntos inalcanzables. */
    public int previo(int punto) {
        return previos[punto];
    }
}
