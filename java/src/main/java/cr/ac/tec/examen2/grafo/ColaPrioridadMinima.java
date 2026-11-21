package cr.ac.tec.examen2.grafo;

/**
 * Cola de prioridad mínima con montículo binario sobre arreglos. Acepta elementos repetidos; los
 * empates de prioridad se resuelven por el elemento menor. Insertar y extraer cuestan O(log n).
 */
public final class ColaPrioridadMinima {
    private int[] elementos = new int[16];
    private long[] prioridades = new long[16];
    private int tamano;

    public boolean estaVacia() {
        return tamano == 0;
    }

    public int tamano() {
        return tamano;
    }

    public void insertar(int elemento, long prioridad) {
        if (tamano == elementos.length) {
            int[] masElementos = new int[tamano * 2];
            long[] masPrioridades = new long[tamano * 2];
            System.arraycopy(elementos, 0, masElementos, 0, tamano);
            System.arraycopy(prioridades, 0, masPrioridades, 0, tamano);
            elementos = masElementos;
            prioridades = masPrioridades;
        }
        int i = tamano++;
        while (i > 0) {
            int padre = (i - 1) / 2;
            if (!menor(elemento, prioridad, elementos[padre], prioridades[padre])) {
                break;
            }
            elementos[i] = elementos[padre];
            prioridades[i] = prioridades[padre];
            i = padre;
        }
        elementos[i] = elemento;
        prioridades[i] = prioridad;
    }

    public int elementoMinimo() {
        exigirNoVacia();
        return elementos[0];
    }

    public long prioridadMinima() {
        exigirNoVacia();
        return prioridades[0];
    }

    public int extraerMinimo() {
        exigirNoVacia();
        int resultado = elementos[0];
        tamano--;
        if (tamano > 0) {
            int elemento = elementos[tamano];
            long prioridad = prioridades[tamano];
            int i = 0;
            while (true) {
                int hijo = 2 * i + 1;
                if (hijo >= tamano) {
                    break;
                }
                if (hijo + 1 < tamano
                        && menor(elementos[hijo + 1], prioridades[hijo + 1], elementos[hijo], prioridades[hijo])) {
                    hijo++;
                }
                if (!menor(elementos[hijo], prioridades[hijo], elemento, prioridad)) {
                    break;
                }
                elementos[i] = elementos[hijo];
                prioridades[i] = prioridades[hijo];
                i = hijo;
            }
            elementos[i] = elemento;
            prioridades[i] = prioridad;
        }
        return resultado;
    }

    private void exigirNoVacia() {
        if (tamano == 0) {
            throw new IllegalStateException("La cola de prioridad está vacía.");
        }
    }

    private static boolean menor(int elementoA, long prioridadA, int elementoB, long prioridadB) {
        return prioridadA < prioridadB || (prioridadA == prioridadB && elementoA < elementoB);
    }
}
