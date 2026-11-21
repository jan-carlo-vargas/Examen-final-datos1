# Campus de 400 salas (prueba manual)

Conjunto de datos grande para probar la aplicación a mano. No reemplaza `datos/version-b` (los datos del enunciado).

- **400 salas**, 5 edificios y 18 alas (20–25 salas por ala; el máximo es 27): Ciencias (4 alas), Ingeniería (4), Humanidades (4), Administración (3) y Servicios (3).
- Ocupantes de 0 a 80, con varias salas en 0, varias en 80 y muchos empates, para ver el orden estable del reporte.
- **Grafo**: 8 salidas, 644 corredores, con rutas alternativas. Cada sala conecta con el pasillo de su ala; cada ala con el vestíbulo de su edificio; los vestíbulos con salidas y explanadas.
- Bloqueados al inicio: `Vestíbulo Ciencias – Salida Norte`, `Pasillo Ingeniería Ala B – Vestíbulo Ingeniería` y `Explanada Norte – Explanada Sur`.
- `ocupantes.csv` usa el encabezado `codigo,nombre,edificio,ala,ocupantes` (con edificio por fila); el cargador también acepta el de 4 columnas.

Cargar: en la página, barra lateral → «Conjunto de datos» → «Campus grande (400 salas)» → «Cargar datos». O al arrancar: `./iniciar.sh campus-400` (Linux/macOS) o `iniciar.bat campus-400` (Windows). Los cambios se hacen en memoria: al reiniciar el backend vuelven los datos originales.

Cosas para probar: totales por edificio y ala; actualizar una sala (0–80) y ver el total; reporte de 400 salas paginado; evacuar desde `ING-B-10` (la ruta evita el pasillo bloqueado); bloquear la última salida de una ruta y ver el cambio; usar «Solo bloqueados» y la búsqueda en la lista de corredores. En campus grandes el esquema dibuja solo la ruta de la sala elegida.
