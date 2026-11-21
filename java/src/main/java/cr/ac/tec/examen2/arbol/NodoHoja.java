package cr.ac.tec.examen2.arbol;

/** Hoja (nivel 3): arreglo de capacidad fija con una ranura por letra del alfabeto español. */
public final class NodoHoja extends NodoArbol {
    private final String nombre;
    private final Sala[] salas = new Sala[ArbolJerarquia.CAPACIDAD_ALA];

    NodoHoja(String nombreAla) {
        this.nombre = nombreAla;
        for (int i = 0; i < salas.length; i++) {
            salas[i] = new Sala(ArbolJerarquia.ALFABETO.charAt(i));
        }
    }

    @Override
    public int nivel() {
        return 3;
    }

    @Override
    public String nombre() {
        return nombre;
    }

    public int capacidad() {
        return salas.length;
    }

    Sala salaEn(int indice) {
        return salas[indice];
    }
}
