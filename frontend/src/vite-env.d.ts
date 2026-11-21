/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** URL base del backend Java de SalidaSegura, p. ej. http://127.0.0.1:8080. Vacía = modo demostración. */
  readonly VITE_SALIDASEGURA_API_URL?: string
}
