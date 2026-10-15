/**
 * Hook para perfil de paciente (CU-5).
 */

import { useState, useCallback } from 'react';
import { actualizarPerfil } from '../api/endpoints';
import { isApiError } from '../api/client';
import { validarTelefono, validarDomicilio } from '../utils/validation';

/**
 * @returns {Object}
 */
export function usePerfil() {
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState(null);
  const [exito, setExito] = useState(false);

  /**
   * Actualiza teléfono y/o domicilio.
   * @param {Object} data - { telefono?, domicilio? }
   * @returns {Promise<boolean>}
   */
  const actualizar = useCallback(async (data) => {
    const errores = [];
    if (data.telefono && !validarTelefono(data.telefono)) {
      errores.push('Teléfono muy largo (máx. 30 caracteres)');
    }
    if (data.domicilio && !validarDomicilio(data.domicilio)) {
      errores.push('Domicilio muy largo (máx. 255 caracteres)');
    }
    if (errores.length > 0) {
      const err = new Error(errores.join(', '));
      setError(err);
      return false;
    }

    setCargando(true);
    setError(null);
    setExito(false);
    try {
      await actualizarPerfil(data);
      setExito(true);
      return true;
    } catch (err) {
      setError(isApiError(err) ? err : new Error('No se pudo actualizar el perfil'));
      return false;
    } finally {
      setCargando(false);
    }
  }, []);

  return {
    cargando,
    error,
    exito,
    actualizar,
    setError,
    setExito,
  };
}