package cr.ac.tec.examen2.arbol;

import cr.ac.tec.examen2.ElementoNoEncontradoException;

/**
 * Árbol B+ de cuatro niveles lógicos: raíz (0), edificios (1), alas (2) y hojas (3) con 27 ranuras
 * de sala (alfabeto español, la Ñ entre la N y la O). Edificios y alas se recorren en el orden
 * en que fueron registrados.
 */
public final class ArbolJerarquia {
    public static final int NIVELES = 4;
    public static final int CAPACIDAD_ALA = 27;
    public static final int MAX_OCUPANTES_SALA = 80;
    public static final String ALFABETO = "ABCDEFGHIJKLMNÑOPQRSTUVWXYZ";

    private final NodoGuia raiz = new NodoGuia(0, "Institución");

    public NodoGuia raiz() {
        return raiz;
    }

    // ---------- validaciones ----------

    /** Posición (0 a 26) de la letra en el alfabeto español, sin distinguir mayúsculas; -1 si no pertenece. */
    public static int indiceLetra(char letra) {
        char mayuscula = Character.toUpperCase(letra);
        for (int i = 0; i < ALFABETO.length(); i++) {
            if (ALFABETO.charAt(i) == mayuscula) {
                return i;
            }
        }
        return -1;
    }

    private static int exigirLetra(char letra) {
        int indice = indiceLetra(letra);
        if (indice < 0) {
            throw new IllegalArgumentException("Letra de sala inválida: '" + letra
                    + "'. Debe pertenecer al alfabeto español (A-Z con Ñ).");
        }
        return indice;
    }

    /** Verifica que la cantidad de ocupantes esté entre 0 y {@link #MAX_OCUPANTES_SALA}. */
    public static void validarOcupantes(int ocupantes) {
        if (ocupantes < 0 || ocupantes > MAX_OCUPANTES_SALA) {
            throw new IllegalArgumentException("Cantidad de ocupantes inválida: " + ocupantes
                    + ". Debe estar entre 0 y " + MAX_OCUPANTES_SALA + ".");
        }
    }

    private static String limpiar(String valor, String campo) {
        if (valor == null) {
            throw new IllegalArgumentException("El " + campo + " no puede ser nulo.");
        }
        String limpio = valor.trim();
        if (limpio.isEmpty()) {
            throw new IllegalArgumentException("El " + campo + " no puede estar vacío.");
        }
        return limpio;
    }

    // ---------- búsquedas ----------

    private NodoGuia edificioOpcional(String edificio) {
        return (NodoGuia) raiz.buscarHijo(edificio);
    }

    private NodoGuia edificio(String edificio) {
        NodoGuia nodo = edificioOpcional(edificio);
        if (nodo == null) {
            throw new ElementoNoEncontradoException("Edificio no encontrado: '" + edificio + "'.");
        }
        return nodo;
    }

    private NodoHoja hoja(String edificio, String ala) {
        NodoGuia nodoEdificio = edificio(edificio);
        NodoGuia nodoAla = (NodoGuia) nodoEdificio.buscarHijo(ala);
        if (nodoAla == null) {
            throw new ElementoNoEncontradoException(
                    "Ala no encontrada: '" + ala + "' en el edificio '" + edificio + "'.");
        }
        return (NodoHoja) nodoAla.hijo(0);
    }

    // ---------- registro de salas ----------

    /** Registra una sala en la ranura de la letra indicada, creando el edificio y el ala si no existen. */
    public void registrarSala(String edificio, String ala, char letra, String codigo) {
        String nombreEdificio = limpiar(edificio, "nombre del edificio");
        String nombreAla = limpiar(ala, "nombre del ala");
        String codigoSala = limpiar(codigo, "código de la sala");
        int indice = exigirLetra(letra);

        NodoGuia nodoEdificio = edificioOpcional(nombreEdificio);
        NodoGuia nodoAla = nodoEdificio == null ? null : (NodoGuia) nodoEdificio.buscarHijo(nombreAla);
        if (nodoAla != null) {
            NodoHoja existente = (NodoHoja) nodoAla.hijo(0);
            if (existente.salaEn(indice).registrada()) {
                throw new IllegalArgumentException("La ranura " + ALFABETO.charAt(indice) + " del ala '"
                        + nombreAla + "' ya tiene la sala '" + existente.salaEn(indice).codigo() + "'.");
            }
            for (int i = 0; i < CAPACIDAD_ALA; i++) {
                if (codigoSala.equals(existente.salaEn(i).codigo())) {
                    throw new IllegalArgumentException("El código '" + codigoSala
                            + "' ya está registrado en el ala '" + nombreAla + "'.");
                }
            }
        }

        if (nodoEdificio == null) {
            nodoEdificio = new NodoGuia(1, nombreEdificio);
            raiz.agregarHijo(nodoEdificio);
        }
        if (nodoAla == null) {
            nodoAla = new NodoGuia(2, nombreAla);
            nodoAla.agregarHijo(new NodoHoja(nombreAla));
            nodoEdificio.agregarHijo(nodoAla);
        }
        ((NodoHoja) nodoAla.hijo(0)).salaEn(indice).registrar(codigoSala);
    }

    /** Registra una sala en la primera ranura libre del ala (A, B, C...) y devuelve su letra. */
    public char registrarSala(String edificio, String ala, String codigo) {
        String nombreEdificio = limpiar(edificio, "nombre del edificio");
        String nombreAla = limpiar(ala, "nombre del ala");
        limpiar(codigo, "código de la sala");
        int indice = 0;
        NodoGuia nodoEdificio = edificioOpcional(nombreEdificio);
        NodoGuia nodoAla = nodoEdificio == null ? null : (NodoGuia) nodoEdificio.buscarHijo(nombreAla);
        if (nodoAla != null) {
            NodoHoja existente = (NodoHoja) nodoAla.hijo(0);
            while (indice < CAPACIDAD_ALA && existente.salaEn(indice).registrada()) {
                indice++;
            }
            if (indice == CAPACIDAD_ALA) {
                throw new IllegalArgumentException("El ala '" + nombreAla + "' ya tiene sus "
                        + CAPACIDAD_ALA + " salas.");
            }
        }
        char letra = ALFABETO.charAt(indice);
        registrarSala(nombreEdificio, nombreAla, letra, codigo);
        return letra;
    }

    // ---------- operaciones requeridas ----------

    /** Operación 1: devuelve una copia del arreglo de 27 ranuras del ala. */
    public Sala[] obtenerSalas(String edificio, String ala) {
        NodoHoja hoja = hoja(limpiar(edificio, "nombre del edificio"), limpiar(ala, "nombre del ala"));
        Sala[] copia = new Sala[CAPACIDAD_ALA];
        for (int i = 0; i < CAPACIDAD_ALA; i++) {
            copia[i] = hoja.salaEn(i);
        }
        return copia;
    }

    /** Operación 2: actualiza los ocupantes de una sala registrada. */
    public void actualizarOcupantes(String edificio, String ala, char letra, int ocupantes) {
        String nombreEdificio = limpiar(edificio, "nombre del edificio");
        String nombreAla = limpiar(ala, "nombre del ala");
        int indice = exigirLetra(letra);
        validarOcupantes(ocupantes);
        Sala sala = hoja(nombreEdificio, nombreAla).salaEn(indice);
        if (!sala.registrada()) {
            throw new ElementoNoEncontradoException("Sala no encontrada: la ranura " + ALFABETO.charAt(indice)
                    + " del ala '" + nombreAla + "' no tiene sala registrada.");
        }
        sala.fijarOcupantes(ocupantes);
    }

    private static int sumar(NodoHoja hoja) {
        int total = 0;
        for (int i = 0; i < CAPACIDAD_ALA; i++) {
            total += hoja.salaEn(i).ocupantes();
        }
        return total;
    }

    /** Operación 3a: total de ocupantes de un ala. */
    public int totalAla(String edificio, String ala) {
        return sumar(hoja(limpiar(edificio, "nombre del edificio"), limpiar(ala, "nombre del ala")));
    }

    /** Operación 3b: total de ocupantes de un edificio (suma de todas sus alas). */
    public int totalEdificio(String edificio) {
        NodoGuia nodoEdificio = edificio(limpiar(edificio, "nombre del edificio"));
        int total = 0;
        for (int i = 0; i < nodoEdificio.cantidadHijos(); i++) {
            total += sumar((NodoHoja) ((NodoGuia) nodoEdificio.hijo(i)).hijo(0));
        }
        return total;
    }

    // ---------- recorrido ----------

    public String[] edificios() {
        String[] nombres = new String[raiz.cantidadHijos()];
        for (int i = 0; i < nombres.length; i++) {
            nombres[i] = raiz.hijo(i).nombre();
        }
        return nombres;
    }

    public String[] alas(String edificio) {
        NodoGuia nodoEdificio = edificio(limpiar(edificio, "nombre del edificio"));
        String[] nombres = new String[nodoEdificio.cantidadHijos()];
        for (int i = 0; i < nombres.length; i++) {
            nombres[i] = nodoEdificio.hijo(i).nombre();
        }
        return nombres;
    }

    public int cantidadSalas() {
        int total = 0;
        for (int e = 0; e < raiz.cantidadHijos(); e++) {
            NodoGuia edificio = (NodoGuia) raiz.hijo(e);
            for (int a = 0; a < edificio.cantidadHijos(); a++) {
                NodoHoja hoja = (NodoHoja) ((NodoGuia) edificio.hijo(a)).hijo(0);
                for (int s = 0; s < CAPACIDAD_ALA; s++) {
                    if (hoja.salaEn(s).registrada()) {
                        total++;
                    }
                }
            }
        }
        return total;
    }

    /**
     * Reúne todas las salas registradas en el orden determinista de recorrido: edificios y alas en
     * orden de registro y, dentro de cada ala, letras en orden alfabético español.
     */
    public RegistroSala[] recolectarSalas() {
        RegistroSala[] resultado = new RegistroSala[cantidadSalas()];
        int posicion = 0;
        for (int e = 0; e < raiz.cantidadHijos(); e++) {
            NodoGuia edificio = (NodoGuia) raiz.hijo(e);
            for (int a = 0; a < edificio.cantidadHijos(); a++) {
                NodoGuia ala = (NodoGuia) edificio.hijo(a);
                NodoHoja hoja = (NodoHoja) ala.hijo(0);
                for (int s = 0; s < CAPACIDAD_ALA; s++) {
                    Sala sala = hoja.salaEn(s);
                    if (sala.registrada()) {
                        resultado[posicion++] = new RegistroSala(edificio.nombre(), ala.nombre(),
                                sala.letra(), sala.codigo(), sala.ocupantes());
                    }
                }
            }
        }
        return resultado;
    }
}
