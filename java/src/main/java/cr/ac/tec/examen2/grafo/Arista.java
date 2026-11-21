package cr.ac.tec.examen2.grafo;

/** Nodo de una lista de adyacencia: corredor hacia {@code destino} con su tiempo en segundos. */
final class Arista {
    final int destino;
    final int segundos;
    Arista siguiente;

    Arista(int destino, int segundos) {
        this.destino = destino;
        this.segundos = segundos;
    }
}
