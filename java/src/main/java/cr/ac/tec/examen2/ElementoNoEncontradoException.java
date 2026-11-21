package cr.ac.tec.examen2;

/** Se lanza cuando un edificio, ala, sala o punto del grafo solicitado no existe. */
public class ElementoNoEncontradoException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ElementoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
