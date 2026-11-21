package cr.ac.tec.examen2.ordenamiento;

import cr.ac.tec.examen2.arbol.RegistroSala;

/**
 * Counting Sort estable en orden descendente por cantidad de ocupantes. Con n salas y valores en
 * [0, k], cuesta O(n + k) en tiempo y O(n + k) en espacio, sin importar cómo vengan los datos.
 */
public final class OrdenamientoConteo {
    private long operaciones;

    /**
     * Devuelve una copia ordenada de mayor a menor ocupación; las salas con igual cantidad conservan
     * el orden de la entrada.
     *
     * @param maximo mayor cantidad de ocupantes permitida (k)
     */
    public RegistroSala[] ordenarDescendente(RegistroSala[] entrada, int maximo) {
        if (entrada == null) {
            throw new IllegalArgumentException("La entrada no puede ser nula.");
        }
        if (maximo < 0 || maximo == Integer.MAX_VALUE) {
            throw new IllegalArgumentException("El máximo de ocupantes debe estar entre 0 y "
                    + (Integer.MAX_VALUE - 1) + ": " + maximo);
        }
        operaciones = 0;
        int n = entrada.length;
        int[] posicion = new int[maximo + 1];

        // Paso 1 (n iteraciones): validar y contar cada valor.
        for (int i = 0; i < n; i++) {
            if (entrada[i] == null) {
                throw new IllegalArgumentException("Sala nula en la posición " + i + ".");
            }
            int valor = entrada[i].ocupantes();
            if (valor < 0 || valor > maximo) {
                throw new IllegalArgumentException("Ocupantes fuera del rango [0, " + maximo + "] en la posición "
                        + i + ": " + valor);
            }
            posicion[valor]++;
            operaciones++;
        }

        // Paso 2 (k + 1 iteraciones): posición inicial de cada valor, de mayor a menor.
        int acumulado = 0;
        for (int valor = maximo; valor >= 0; valor--) {
            int cantidad = posicion[valor];
            posicion[valor] = acumulado;
            acumulado += cantidad;
            operaciones++;
        }

        // Paso 3 (n iteraciones): colocar recorriendo la entrada de izquierda a derecha (estable).
        RegistroSala[] salida = new RegistroSala[n];
        for (int i = 0; i < n; i++) {
            salida[posicion[entrada[i].ocupantes()]++] = entrada[i];
            operaciones++;
        }
        return salida;
    }

    /** Iteraciones de la última ejecución: siempre 2n + (k + 1). */
    public long operaciones() {
        return operaciones;
    }
}
