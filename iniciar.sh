#!/usr/bin/env bash
# Inicia backend (Java) y frontend (Vite) de SalidaSegura. Ctrl+C detiene ambos.
# Uso: ./iniciar.sh [conjunto-de-datos]   (carpeta dentro de datos/; por defecto version-b; "campus-400" = 400 salas)
# Requisitos: JDK 17+ (java y javac), Node 22+ y pnpm (o npm).
set -e
RAIZ="$(cd "$(dirname "$0")" && pwd)"
PUERTO_API=8080
PUERTO_WEB=8443
DATOS="${1:-version-b}"
[ -f "$RAIZ/datos/$DATOS/ocupantes.csv" ] || { echo "No existe datos/$DATOS/ocupantes.csv"; exit 1; }

for cmd in java javac node; do
  command -v "$cmd" >/dev/null || { echo "Falta '$cmd'. Instálalo e intenta de nuevo."; exit 1; }
done
if command -v pnpm >/dev/null; then GESTOR=pnpm; elif command -v npm >/dev/null; then GESTOR=npm; else echo "Falta pnpm o npm."; exit 1; fi

echo "==> Compilando backend..."
mkdir -p "$RAIZ/java/target/classes"
javac -encoding UTF-8 --release 17 -d "$RAIZ/java/target/classes" $(find "$RAIZ/java/src/main" -name '*.java')

if [ ! -d "$RAIZ/frontend/node_modules" ]; then
  echo "==> Instalando dependencias del frontend ($GESTOR)..."
  (cd "$RAIZ/frontend" && $GESTOR install)
fi

echo "==> Iniciando backend en http://127.0.0.1:$PUERTO_API (datos: $DATOS)"
(cd "$RAIZ/java" && exec java -cp target/classes cr.ac.tec.examen2.MainServidor ../datos/$DATOS $PUERTO_API "http://localhost:$PUERTO_WEB") &
PID_API=$!
trap 'kill $PID_API 2>/dev/null' EXIT INT TERM

for _ in $(seq 1 30); do
  (exec 3<>/dev/tcp/127.0.0.1/$PUERTO_API) 2>/dev/null && break
  sleep 0.5
done

URL="http://localhost:$PUERTO_WEB"
echo "==> Frontend en $URL (Ctrl+C para detener todo)"
( sleep 3; (xdg-open "$URL" || open "$URL") >/dev/null 2>&1 || true ) &
cd "$RAIZ/frontend"
VITE_SALIDASEGURA_API_URL="http://127.0.0.1:$PUERTO_API" npx vite --host localhost --port $PUERTO_WEB --strictPort &
PID_WEB=$!
trap 'kill $PID_API $PID_WEB 2>/dev/null' EXIT INT TERM
wait $PID_WEB
