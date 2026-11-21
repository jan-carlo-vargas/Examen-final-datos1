package cr.ac.tec.examen2.grafo;

import cr.ac.tec.examen2.ElementoNoEncontradoException;

/**
 * Grafo no dirigido, ponderado y disperso (lista de adyacencia) con los puntos de un edificio:
 * salas, pasillos y salidas. El peso de cada corredor son los segundos de caminata. Los puntos
 * se numeran en orden de registro; los arreglos internos crecen por duplicación.
 */
public final class GrafoEvacuacion {
    private String[] nombres = new String[16];
    private TipoPunto[] tipos = new TipoPunto[16];
    private Arista[] adyacencia = new Arista[16];
    private int[] grados = new int[16];
    private int cantidadPuntos;
    private int cantidadCorredores;
    private int cantidadSalidas;

    // Registro de todos los corredores (abiertos y bloqueados) para poder listarlos y desbloquearlos.
    private int[] registroDesde = new int[16];
    private int[] registroHacia = new int[16];
    private int[] registroSegundos = new int[16];
    private boolean[] registroBloqueado = new boolean[16];
    private int cantidadRegistros;

    // ---------- puntos ----------

    public int agregarPunto(String nombre, TipoPunto tipo) {
        String limpio = limpiar(nombre);
        if (tipo == null) {
            throw new IllegalArgumentException("El tipo de punto no puede ser nulo.");
        }
        if (buscarPunto(limpio) >= 0) {
            throw new IllegalArgumentException("El punto '" + limpio + "' ya existe.");
        }
        if (cantidadPuntos == nombres.length) {
            int capacidad = nombres.length * 2;
            String[] masNombres = new String[capacidad];
            TipoPunto[] masTipos = new TipoPunto[capacidad];
            Arista[] masAdyacencia = new Arista[capacidad];
            int[] masGrados = new int[capacidad];
            System.arraycopy(nombres, 0, masNombres, 0, cantidadPuntos);
            System.arraycopy(tipos, 0, masTipos, 0, cantidadPuntos);
            System.arraycopy(adyacencia, 0, masAdyacencia, 0, cantidadPuntos);
            System.arraycopy(grados, 0, masGrados, 0, cantidadPuntos);
            nombres = masNombres;
            tipos = masTipos;
            adyacencia = masAdyacencia;
            grados = masGrados;
        }
        nombres[cantidadPuntos] = limpio;
        tipos[cantidadPuntos] = tipo;
        if (tipo == TipoPunto.SALIDA) {
            cantidadSalidas++;
        }
        return cantidadPuntos++;
    }

    /** Índice del punto o -1 si no existe (o el nombre es nulo). */
    public int buscarPunto(String nombre) {
        if (nombre == null) {
            return -1;
        }
        String limpio = nombre.trim();
        for (int i = 0; i < cantidadPuntos; i++) {
            if (nombres[i].equals(limpio)) {
                return i;
            }
        }
        return -1;
    }

    public boolean existePunto(String nombre) {
        return buscarPunto(nombre) >= 0;
    }

    public int cantidadPuntos() {
        return cantidadPuntos;
    }

    public int cantidadCorredores() {
        return cantidadCorredores;
    }

    public int cantidadSalidas() {
        return cantidadSalidas;
    }

    public String nombre(int indice) {
        if (indice < 0 || indice >= cantidadPuntos) {
            throw new IllegalArgumentException("Índice de punto fuera de rango: " + indice);
        }
        return nombres[indice];
    }

    public TipoPunto tipo(String nombre) {
        return tipos[indiceObligatorio(nombre)];
    }

    /** Cantidad de corredores que salen del punto. */
    public int grado(String nombre) {
        return grados[indiceObligatorio(nombre)];
    }

    // ---------- corredores ----------

    public void agregarCorredor(String desde, String hacia, int segundos) {
        int a = indiceObligatorio(desde);
        int b = indiceObligatorio(hacia);
        if (a == b) {
            throw new IllegalArgumentException("Un corredor no puede unir un punto consigo mismo: '"
                    + nombres[a] + "'.");
        }
        if (segundos < 0) {
            throw new IllegalArgumentException("Los segundos de un corredor no pueden ser negativos: " + segundos);
        }
        if (buscarArista(a, b) != null) {
            throw new IllegalArgumentException("Ya existe un corredor entre '" + nombres[a] + "' y '"
                    + nombres[b] + "'.");
        }
        anexar(a, new Arista(b, segundos));
        anexar(b, new Arista(a, segundos));
        cantidadCorredores++;
        registrar(a, b, segundos);
    }

    public boolean existeCorredor(String desde, String hacia) {
        int a = buscarPunto(desde);
        int b = buscarPunto(hacia);
        return a >= 0 && b >= 0 && buscarArista(a, b) != null;
    }

    public int pesoCorredor(String desde, String hacia) {
        int a = indiceObligatorio(desde);
        int b = indiceObligatorio(hacia);
        Arista arista = buscarArista(a, b);
        if (arista == null) {
            throw new ElementoNoEncontradoException("No existe un corredor entre '" + nombres[a] + "' y '"
                    + nombres[b] + "'.");
        }
        return arista.segundos;
    }

    /** Bloquea un corredor eliminando la arista en ambas direcciones de la lista de adyacencia. */
    public void bloquearCorredor(String desde, String hacia) {
        int a = indiceObligatorio(desde);
        int b = indiceObligatorio(hacia);
        if (buscarArista(a, b) == null) {
            throw new ElementoNoEncontradoException("No existe un corredor entre '" + nombres[a] + "' y '"
                    + nombres[b] + "'.");
        }
        quitar(a, b);
        quitar(b, a);
        cantidadCorredores--;
        registroBloqueado[buscarRegistro(a, b)] = true;
    }

    /** Reabre un corredor bloqueado: restaura la arista en ambas direcciones con su peso original. */
    public void desbloquearCorredor(String desde, String hacia) {
        int a = indiceObligatorio(desde);
        int b = indiceObligatorio(hacia);
        int registro = buscarRegistro(a, b);
        if (registro < 0) {
            throw new ElementoNoEncontradoException("No existe un corredor entre '" + nombres[a] + "' y '"
                    + nombres[b] + "'.");
        }
        if (!registroBloqueado[registro]) {
            throw new IllegalArgumentException("El corredor entre '" + nombres[a] + "' y '" + nombres[b]
                    + "' no está bloqueado.");
        }
        int segundos = registroSegundos[registro];
        anexar(a, new Arista(b, segundos));
        anexar(b, new Arista(a, segundos));
        cantidadCorredores++;
        registroBloqueado[registro] = false;
    }

    /** Todos los corredores, abiertos y bloqueados, en orden de registro. */
    public Corredor[] listarCorredores() {
        Corredor[] lista = new Corredor[cantidadRegistros];
        for (int i = 0; i < cantidadRegistros; i++) {
            lista[i] = new Corredor(nombres[registroDesde[i]], nombres[registroHacia[i]], registroSegundos[i],
                    registroBloqueado[i]);
        }
        return lista;
    }

    /** El corredor entre dos puntos (en cualquier orden), esté abierto o bloqueado. */
    public Corredor buscarCorredor(String desde, String hacia) {
        int a = indiceObligatorio(desde);
        int b = indiceObligatorio(hacia);
        int registro = buscarRegistro(a, b);
        if (registro < 0) {
            throw new ElementoNoEncontradoException("No existe un corredor entre '" + nombres[a] + "' y '"
                    + nombres[b] + "'.");
        }
        return new Corredor(nombres[registroDesde[registro]], nombres[registroHacia[registro]],
                registroSegundos[registro], registroBloqueado[registro]);
    }

    /** Nombres de las salidas en orden de registro. */
    public String[] nombresSalidas() {
        String[] salidas = new String[cantidadSalidas];
        int posicion = 0;
        for (int i = 0; i < cantidadPuntos; i++) {
            if (tipos[i] == TipoPunto.SALIDA) {
                salidas[posicion++] = nombres[i];
            }
        }
        return salidas;
    }

    // ---------- Dijkstra y rutas ----------

    /** Dijkstra desde un punto sobre las aristas que existen en este momento. */
    public ResultadoDijkstra dijkstra(String origen) {
        return dijkstra(indiceObligatorio(origen));
    }

    private ResultadoDijkstra dijkstra(int origen) {
        long[] distancias = new long[cantidadPuntos];
        int[] previos = new int[cantidadPuntos];
        for (int i = 0; i < cantidadPuntos; i++) {
            distancias[i] = ResultadoDijkstra.INFINITO;
            previos[i] = -1;
        }
        distancias[origen] = 0;
        ColaPrioridadMinima cola = new ColaPrioridadMinima();
        cola.insertar(origen, 0);
        while (!cola.estaVacia()) {
            long distancia = cola.prioridadMinima();
            int actual = cola.extraerMinimo();
            if (distancia > distancias[actual]) {
                continue; // entrada obsoleta
            }
            for (Arista arista = adyacencia[actual]; arista != null; arista = arista.siguiente) {
                long candidata = distancia + arista.segundos;
                if (candidata < distancias[arista.destino]) {
                    distancias[arista.destino] = candidata;
                    previos[arista.destino] = actual;
                    cola.insertar(arista.destino, candidata);
                }
            }
        }
        return new ResultadoDijkstra(distancias, previos);
    }

    /**
     * Ruta más rápida desde el punto a cualquier salida alcanzable. Si dos salidas empatan en tiempo
     * gana la registrada primero. Sin salida alcanzable, la ruta queda marcada como bloqueada.
     */
    public RutaEvacuacion rutaMasRapida(String origen) {
        int inicio = indiceObligatorio(origen);
        ResultadoDijkstra resultado = dijkstra(inicio);
        int mejor = -1;
        for (int i = 0; i < cantidadPuntos; i++) {
            if (tipos[i] == TipoPunto.SALIDA && resultado.alcanzable(i)
                    && (mejor < 0 || resultado.distancia(i) < resultado.distancia(mejor))) {
                mejor = i;
            }
        }
        if (mejor < 0) {
            return RutaEvacuacion.bloqueada(nombres[inicio]);
        }
        int largo = 0;
        for (int p = mejor; p != -1; p = resultado.previo(p)) {
            largo++;
        }
        String[] ruta = new String[largo];
        int posicion = largo - 1;
        for (int p = mejor; p != -1; p = resultado.previo(p)) {
            ruta[posicion--] = nombres[p];
        }
        return RutaEvacuacion.hallada(nombres[inicio], nombres[mejor], ruta, resultado.distancia(mejor));
    }

    /** Ruta más rápida de cada sala, en el orden en que se registraron. */
    public RutaEvacuacion[] rutasDeSalas() {
        int total = 0;
        for (int i = 0; i < cantidadPuntos; i++) {
            if (tipos[i] == TipoPunto.SALA) {
                total++;
            }
        }
        RutaEvacuacion[] rutas = new RutaEvacuacion[total];
        int posicion = 0;
        for (int i = 0; i < cantidadPuntos; i++) {
            if (tipos[i] == TipoPunto.SALA) {
                rutas[posicion++] = rutaMasRapida(nombres[i]);
            }
        }
        return rutas;
    }

    // ---------- auxiliares ----------

    private static String limpiar(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del punto no puede ser nulo ni vacío.");
        }
        return nombre.trim();
    }

    private int indiceObligatorio(String nombre) {
        String limpio = limpiar(nombre);
        int indice = buscarPunto(limpio);
        if (indice < 0) {
            throw new ElementoNoEncontradoException("Punto no encontrado: '" + limpio + "'.");
        }
        return indice;
    }

    private int buscarRegistro(int a, int b) {
        for (int i = 0; i < cantidadRegistros; i++) {
            if ((registroDesde[i] == a && registroHacia[i] == b) || (registroDesde[i] == b && registroHacia[i] == a)) {
                return i;
            }
        }
        return -1;
    }

    private void registrar(int a, int b, int segundos) {
        int existente = buscarRegistro(a, b);
        if (existente >= 0) {
            registroDesde[existente] = a;
            registroHacia[existente] = b;
            registroSegundos[existente] = segundos;
            registroBloqueado[existente] = false;
            return;
        }
        if (cantidadRegistros == registroDesde.length) {
            int capacidad = registroDesde.length * 2;
            int[] masDesde = new int[capacidad];
            int[] masHacia = new int[capacidad];
            int[] masSegundos = new int[capacidad];
            boolean[] masBloqueado = new boolean[capacidad];
            System.arraycopy(registroDesde, 0, masDesde, 0, cantidadRegistros);
            System.arraycopy(registroHacia, 0, masHacia, 0, cantidadRegistros);
            System.arraycopy(registroSegundos, 0, masSegundos, 0, cantidadRegistros);
            System.arraycopy(registroBloqueado, 0, masBloqueado, 0, cantidadRegistros);
            registroDesde = masDesde;
            registroHacia = masHacia;
            registroSegundos = masSegundos;
            registroBloqueado = masBloqueado;
        }
        registroDesde[cantidadRegistros] = a;
        registroHacia[cantidadRegistros] = b;
        registroSegundos[cantidadRegistros] = segundos;
        registroBloqueado[cantidadRegistros] = false;
        cantidadRegistros++;
    }

    private Arista buscarArista(int origen, int destino) {
        for (Arista arista = adyacencia[origen]; arista != null; arista = arista.siguiente) {
            if (arista.destino == destino) {
                return arista;
            }
        }
        return null;
    }

    private void anexar(int origen, Arista nueva) {
        if (adyacencia[origen] == null) {
            adyacencia[origen] = nueva;
        } else {
            Arista ultima = adyacencia[origen];
            while (ultima.siguiente != null) {
                ultima = ultima.siguiente;
            }
            ultima.siguiente = nueva;
        }
        grados[origen]++;
    }

    private void quitar(int origen, int destino) {
        Arista anterior = null;
        for (Arista actual = adyacencia[origen]; actual != null; actual = actual.siguiente) {
            if (actual.destino == destino) {
                if (anterior == null) {
                    adyacencia[origen] = actual.siguiente;
                } else {
                    anterior.siguiente = actual.siguiente;
                }
                grados[origen]--;
                return;
            }
            anterior = actual;
        }
    }
}
