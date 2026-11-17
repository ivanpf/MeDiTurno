/** CU-5: carga y edición del contacto del paciente autenticado. */
import { useState, useEffect, useCallback } from 'react';
import { consultarPerfil, actualizarPerfil } from '../api/endpoints';
import { isApiError } from '../api/client';

const contacto = (p) => ({ correo: p.correo, telefono: p.telefono ?? '', domicilio: p.domicilio ?? '' });
export function usePerfil() {
  const [perfil, setPerfil] = useState(null);
  const [datos, setDatos] = useState({ correo: '', telefono: '', domicilio: '' });
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState(null);
  const [exito, setExito] = useState(false);
  const [intento, setIntento] = useState(0);
  useEffect(() => {
    let vigente = true;
    setCargando(true);
    setError(null);
    consultarPerfil().then((p) => { if (vigente) { setPerfil(p); setDatos(contacto(p)); } })
      .catch((err) => { if (vigente) setError(isApiError(err) ? err : new Error('No se pudo consultar el perfil')); })
      .finally(() => { if (vigente) setCargando(false); });
    return () => { vigente = false; };
  }, [intento]);
  const actualizar = useCallback(async () => {
    setCargando(true);
    setError(null);
    setExito(false);
    try {
      const nuevos = { correo: datos.correo.trim(), telefono: datos.telefono.trim() || null, domicilio: datos.domicilio.trim() || null };
      await actualizarPerfil(nuevos);
      const guardado = { ...perfil, ...nuevos };
      setPerfil(guardado);
      setDatos(contacto(guardado));
      setExito(true);
      return true;
    } catch (err) {
      setError(isApiError(err) ? err : new Error('No se pudo actualizar el perfil'));
      return false;
    } finally { setCargando(false); }
  }, [datos, perfil]);
  const descartar = () => { if (perfil) setDatos(contacto(perfil)); setError(null); setExito(false); };
  return { perfil, datos, setDatos, cargando, error, exito, actualizar, descartar, setError, reintentar: () => setIntento((i) => i + 1) };
}
