@echo off
rem Inicia backend (Java) y frontend (Vite) de SalidaSegura en dos ventanas.
rem Uso: iniciar.bat [conjunto-de-datos]  (carpeta dentro de datos\; por defecto version-b; campus-400 = 400 salas)
rem Requisitos: JDK 17+ (java y javac), Node 22+ y pnpm (o npm).
setlocal
set RAIZ=%~dp0
set PUERTO_API=8080
set PUERTO_WEB=8443
set DATOS=%~1
if "%DATOS%"=="" set DATOS=version-b
if not exist "%RAIZ%datos\%DATOS%\ocupantes.csv" (echo No existe datos\%DATOS%\ocupantes.csv & pause & exit /b 1)

for %%C in (java javac node) do (
  where %%C >nul 2>nul || (echo Falta %%C. Instalalo e intenta de nuevo. & pause & exit /b 1)
)
set GESTOR=npm
where pnpm >nul 2>nul && set GESTOR=pnpm

echo ==^> Compilando backend...
if not exist "%RAIZ%java\target\classes" mkdir "%RAIZ%java\target\classes"
dir /s /b "%RAIZ%java\src\main\*.java" > "%RAIZ%java\target\fuentes.txt"
javac -encoding UTF-8 --release 17 -d "%RAIZ%java\target\classes" @"%RAIZ%java\target\fuentes.txt" || (pause & exit /b 1)

if not exist "%RAIZ%frontend\node_modules" (
  echo ==^> Instalando dependencias del frontend...
  pushd "%RAIZ%frontend"
  call %GESTOR% install || (popd & pause & exit /b 1)
  popd
)

echo ==^> Iniciando backend y frontend en ventanas separadas (ciérralas para detener)
start "SalidaSegura - backend" /D "%RAIZ%java" java -cp target/classes cr.ac.tec.examen2.MainServidor ../datos/%DATOS% %PUERTO_API% http://localhost:%PUERTO_WEB%
set VITE_SALIDASEGURA_API_URL=http://127.0.0.1:%PUERTO_API%
start "SalidaSegura - frontend" /D "%RAIZ%frontend" cmd /k npx vite --host localhost --port %PUERTO_WEB% --strictPort
timeout /t 5 >nul
start "" http://localhost:%PUERTO_WEB%
