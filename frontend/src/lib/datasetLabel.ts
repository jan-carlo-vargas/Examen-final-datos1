const labels: Record<string, string> = {
  'campus-400': 'Campus grande (400 salas)',
  'version-b': 'Datos del enunciado (6 salas)',
}

/** Etiqueta legible de un conjunto de datos; los nombres desconocidos se muestran tal cual. */
export const datasetLabel = (name: string) => labels[name] ?? name
