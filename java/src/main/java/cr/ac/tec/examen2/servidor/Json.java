package cr.ac.tec.examen2.servidor;

/** Escritura mínima de JSON (solo cadenas); el resto de la estructura se arma con StringBuilder. */
final class Json {
    private static final String HEX = "0123456789abcdef";

    private Json() {
    }

    /** Cadena JSON entre comillas con escapes; {@code null} se escribe como {@code null}. */
    static String texto(String valor) {
        if (valor == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder(valor.length() + 2);
        sb.append('"');
        for (int i = 0; i < valor.length(); i++) {
            char c = valor.charAt(i);
            if (c == '"') {
                sb.append("\\\"");
            } else if (c == '\\') {
                sb.append("\\\\");
            } else if (c == '\n') {
                sb.append("\\n");
            } else if (c == '\r') {
                sb.append("\\r");
            } else if (c == '\t') {
                sb.append("\\t");
            } else if (c < 0x20) {
                sb.append("\\u00").append(HEX.charAt(c >> 4)).append(HEX.charAt(c & 0xF));
            } else {
                sb.append(c);
            }
        }
        return sb.append('"').toString();
    }
}
