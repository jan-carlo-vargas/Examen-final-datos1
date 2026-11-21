package cr.ac.tec.examen2.arbol;

/**
 * Nodo guía: la raíz (nivel 0), un edificio (nivel 1) o un ala (nivel 2). Mantiene un arreglo
 * de hijos que crece por duplicación y conserva el orden de registro.
 */
public final class NodoGuia extends NodoArbol {
    private final int nivel;
    private final String nombre;
    private NodoArbol[] hijos = new NodoArbol[4];
    private int cantidadHijos;

    NodoGuia(int nivel, String nombre) {
        if (nivel < 0 || nivel > 2) {
            throw new IllegalArgumentException("Un nodo guía solo puede estar en los niveles 0, 1 o 2.");
        }
        this.nivel = nivel;
        this.nombre = nombre;
    }

    @Override
    public int nivel() {
        return nivel;
    }

    @Override
    public String nombre() {
        return nombre;
    }

    public int cantidadHijos() {
        return cantidadHijos;
    }

    public NodoArbol hijo(int posicion) {
        if (posicion < 0 || posicion >= cantidadHijos) {
            throw new IllegalArgumentException("Posición de hijo fuera de rango: " + posicion);
        }
        return hijos[posicion];
    }

    /** Devuelve el hijo con ese nombre o {@code null} si no existe. */
    NodoArbol buscarHijo(String nombreHijo) {
        for (int i = 0; i < cantidadHijos; i++) {
            if (hijos[i].nombre().equals(nombreHijo)) {
                return hijos[i];
            }
        }
        return null;
    }

    void agregarHijo(NodoArbol hijo) {
        if (hijo.nivel() != nivel + 1) {
            throw new IllegalArgumentException("El hijo debe estar en el nivel " + (nivel + 1) + ".");
        }
        if (cantidadHijos == hijos.length) {
            NodoArbol[] mayor = new NodoArbol[hijos.length * 2];
            System.arraycopy(hijos, 0, mayor, 0, cantidadHijos);
            hijos = mayor;
        }
        hijos[cantidadHijos++] = hijo;
    }
}
