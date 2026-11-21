package cr.ac.tec.examen2.arbol;

/** Nodo del árbol B+ de la jerarquía institucional (niveles 0 a 3). */
public abstract class NodoArbol {

    /** Nivel lógico del nodo: 0 raíz, 1 edificio, 2 ala, 3 hoja. */
    public abstract int nivel();

    /** Nombre que identifica al nodo (institución, edificio o ala). */
    public abstract String nombre();
}
