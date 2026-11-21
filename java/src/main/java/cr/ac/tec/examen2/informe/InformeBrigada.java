package cr.ac.tec.examen2.informe;

import cr.ac.tec.examen2.arbol.ArbolJerarquia;
import cr.ac.tec.examen2.arbol.RegistroSala;
import cr.ac.tec.examen2.ordenamiento.OrdenamientoConteo;

/** Reporte de brigada: todas las salas de la universidad de mayor a menor ocupación. */
public final class InformeBrigada {

    private InformeBrigada() {
    }

    /** Reúne todas las salas (orden de recorrido) y las ordena con Counting Sort estable. */
    public static RegistroSala[] generar(ArbolJerarquia arbol) {
        return new OrdenamientoConteo().ordenarDescendente(arbol.recolectarSalas(),
                ArbolJerarquia.MAX_OCUPANTES_SALA);
    }

    public static String ocupantes(int cantidad) {
        return cantidad + (cantidad == 1 ? " ocupante" : " ocupantes");
    }

    public static String formatear(RegistroSala[] ordenadas) {
        if (ordenadas.length == 0) {
            return "No hay salas registradas.";
        }
        StringBuilder texto = new StringBuilder();
        texto.append("Reporte de brigada: ").append(ordenadas.length)
                .append(" salas (de mayor a menor ocupación)");
        for (int i = 0; i < ordenadas.length; i++) {
            RegistroSala sala = ordenadas[i];
            texto.append('\n').append(String.valueOf(i + 1)).append(". ")
                    .append(sala.edificio()).append('/').append(sala.ala()).append('/').append(sala.letra())
                    .append(' ').append(sala.codigo()).append(": ").append(ocupantes(sala.ocupantes()));
        }
        return texto.toString();
    }
}
