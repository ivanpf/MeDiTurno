import { Platform } from 'react-native';

/**
 * A que direccion le habla la app para llegar al backend.
 *
 * El emulador de Android es una maquina virtual con su propia red: si la app
 * pide "localhost" busca un servidor DENTRO del emulador, no en tu PC. Android
 * reserva la direccion 10.0.2.2 como alias del anfitrion, y ese es el unico
 * camino que funciona desde el emulador.
 *
 * En un celular fisico ni siquiera eso sirve, porque el telefono esta en otra
 * maquina: ahi hay que apuntar a la IP de tu PC en la red WiFi. Para ese caso
 * cree un archivo .env con:
 *
 *   EXPO_PUBLIC_API_URL=http://192.168.0.15:8080
 *
 * (reemplazando por tu IP real, que sacas con `ipconfig`). Expo reemplaza esa
 * variable en el bundle al arrancar, asi que hay que reiniciar el servidor de
 * Expo despues de editarla.
 */
function resolveBaseUrl(): string {
  // Referencia estatica a proposito: Expo solo reemplaza process.env.EXPO_PUBLIC_*
  // escrito con punto. Con corchetes (process.env['...']) no lo reemplaza.
  const fromEnv = process.env.EXPO_PUBLIC_API_URL;
  if (fromEnv) {
    return fromEnv.replace(/\/+$/, '');
  }

  if (Platform.OS === 'android') {
    return 'http://10.0.2.2:8080';
  }

  return 'http://localhost:8080';
}

export const API_BASE_URL = resolveBaseUrl();

/**
 * Resultado de consultar el backend. En vez de lanzar excepciones que la
 * pantalla tendria que atrapar, devolvemos siempre un objeto que dice si salio
 * bien o mal. La UI solo mira `ok` y sabe que campos tiene disponibles.
 */
export type PingResult =
  | { ok: true; message: string; ms: number }
  | { ok: false; error: string; ms: number };

/**
 * Llama a GET / del backend, que responde "Backend andando correctamente".
 *
 * Sin timeout, un backend apagado deja el fetch colgado casi un minuto y la
 * pantalla parece trabada. AbortController lo corta a los 5 segundos.
 */
export async function pingBackend(timeoutMs = 5000): Promise<PingResult> {
  const startedAt = Date.now();
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);

  try {
    const response = await fetch(`${API_BASE_URL}/`, {
      signal: controller.signal,
    });
    const body = (await response.text()).trim();
    const ms = Date.now() - startedAt;

    if (!response.ok) {
      return { ok: false, error: `El backend respondio HTTP ${response.status}`, ms };
    }

    return { ok: true, message: body, ms };
  } catch (error) {
    const ms = Date.now() - startedAt;

    if (error instanceof Error && error.name === 'AbortError') {
      return { ok: false, error: `Sin respuesta en ${timeoutMs / 1000} segundos`, ms };
    }

    return {
      ok: false,
      error: error instanceof Error ? error.message : String(error),
      ms,
    };
  } finally {
    clearTimeout(timer);
  }
}

/** Los datos del usuario que devuelve el backend al ingresar. */
export type Usuario = {
  id: number;
  email: string;
  nombre: string;
};

export type LoginResult =
  | { ok: true; usuario: Usuario }
  | { ok: false; error: string };

/**
 * Llama a POST /api/auth/login.
 *
 * Devuelve siempre un objeto con `ok`, igual que pingBackend, para que la
 * pantalla no tenga que atrapar excepciones. Los tres finales posibles son:
 * credenciales validas (200), credenciales invalidas (401) y backend caido.
 */
export async function login(
  email: string,
  password: string,
  timeoutMs = 8000,
): Promise<LoginResult> {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);

  try {
    const response = await fetch(`${API_BASE_URL}/api/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password }),
      signal: controller.signal,
    });

    if (response.status === 401) {
      return { ok: false, error: 'Email o clave incorrectos' };
    }

    if (!response.ok) {
      return { ok: false, error: `El backend respondio HTTP ${response.status}` };
    }

    return { ok: true, usuario: (await response.json()) as Usuario };
  } catch (error) {
    if (error instanceof Error && error.name === 'AbortError') {
      return { ok: false, error: `Sin respuesta en ${timeoutMs / 1000} segundos` };
    }

    return {
      ok: false,
      error: 'No se pudo conectar con el servidor',
    };
  } finally {
    clearTimeout(timer);
  }
}

/** Un turno medico, tal como lo devuelve el backend. */
export type Turno = {
  id: number;
  paciente: string;
  especialidad: string;
  profesional: string;
  /** Fecha en formato ISO, por ejemplo "2026-09-08T08:00:00". */
  fechaHora: string;
  estado: string;
};

/**
 * Una pagina de turnos.
 *
 * `ultima` es el dato clave del scroll infinito: mientras sea false hay mas
 * paginas para pedir, y cuando llega a true la pantalla deja de pedir.
 */
export type PaginaTurnos = {
  contenido: Turno[];
  pagina: number;
  totalPaginas: number;
  totalElementos: number;
  ultima: boolean;
};

export type TurnosResult =
  | { ok: true; pagina: PaginaTurnos }
  | { ok: false; error: string };

/**
 * Pide una pagina de turnos a GET /api/turnos.
 *
 * El scroll infinito no puede traer los turnos de una sola vez: se pide de a
 * `size` y se van sumando a los que ya se mostraban.
 */
export async function getTurnos(
  page = 0,
  size = 20,
  timeoutMs = 8000,
): Promise<TurnosResult> {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);

  try {
    const response = await fetch(`${API_BASE_URL}/api/turnos?page=${page}&size=${size}`, {
      signal: controller.signal,
    });

    if (!response.ok) {
      return { ok: false, error: `El backend respondio HTTP ${response.status}` };
    }

    return { ok: true, pagina: (await response.json()) as PaginaTurnos };
  } catch (error) {
    if (error instanceof Error && error.name === 'AbortError') {
      return { ok: false, error: `Sin respuesta en ${timeoutMs / 1000} segundos` };
    }

    return { ok: false, error: 'No se pudo conectar con el servidor' };
  } finally {
    clearTimeout(timer);
  }
}
