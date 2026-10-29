// Estructura común de las pantallas con sesión (rol paciente): encabezado, menú
// según la tabla de rutas y el contenido. Equivalente a frontend/src/layout/Layout.jsx
// adaptado a celular (el menú se abre con el botón del encabezado).

import React, { useState } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  ActivityIndicator,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Slot, Redirect, usePathname, useRouter } from 'expo-router';
import { useAuth } from '../../shared/src/auth/useAuth';
import { RUTAS, ETIQUETA_ROL, ROLES } from '../../shared/src/navigation/rutas';
import { COLORS } from '../../shared/src/theme/colors';
import { Modal, Button } from '../../shared/src/components';
import Logo from '../../src/components/Logo';

// Encabezado con el degradado azul de la web (de azul-oscuro a azul).
const BANDAS = 14;
const DEGRADADO = Array.from({ length: BANDAS }, (_, i) => {
  const t = i / (BANDAS - 1);
  const r = Math.round(18 + (29 - 18) * t);
  const g = Math.round(54 + (79 - 54) * t);
  const b = Math.round(107 + (145 - 107) * t);
  return `rgb(${r}, ${g}, ${b})`;
});

const iniciales = (nombre) =>
  (nombre || '')
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((p) => p[0].toUpperCase())
    .join('');

const segmento = (path) => (path || '').split('/').filter(Boolean).pop();

const estilos = StyleSheet.create({
  pantalla: {
    flex: 1,
    backgroundColor: COLORS.fondo,
  },
  encabezado: {
    height: 64,
    justifyContent: 'center',
    overflow: 'hidden',
    backgroundColor: COLORS.azulOscuro,
  },
  gradiente: {
    ...StyleSheet.absoluteFillObject,
    flexDirection: 'row',
  },
  encabezadoContenido: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
  },
  botonMenu: {
    paddingVertical: 6,
    paddingHorizontal: 14,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.6)',
    borderRadius: 8,
  },
  botonMenuTexto: {
    color: COLORS.blanco,
    fontWeight: '600',
    fontSize: 14,
  },
  menu: {
    backgroundColor: COLORS.blanco,
    borderBottomWidth: 1,
    borderBottomColor: COLORS.borde,
    paddingHorizontal: 14,
    paddingVertical: 12,
  },
  usuarioMovil: {
    flexDirection: 'row',
    alignItems: 'center',
    flexWrap: 'wrap',
    gap: 10,
    marginBottom: 12,
    paddingBottom: 14,
    borderBottomWidth: 1,
    borderBottomColor: COLORS.borde,
  },
  persona: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
    flexShrink: 1,
    paddingVertical: 4,
    paddingLeft: 4,
    paddingRight: 14,
    backgroundColor: COLORS.azulClaro,
    borderRadius: 9999,
  },
  avatar: {
    width: 30,
    height: 30,
    borderRadius: 15,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: COLORS.azulOscuro,
  },
  avatarTexto: {
    fontSize: 13,
    fontWeight: '700',
    color: COLORS.blanco,
  },
  personaNombre: {
    fontSize: 15,
    fontWeight: '600',
    color: COLORS.azulOscuro,
    flexShrink: 1,
  },
  rol: {
    paddingVertical: 3,
    paddingHorizontal: 12,
    fontSize: 13,
    fontWeight: '700',
    color: COLORS.azulOscuro,
    backgroundColor: COLORS.mostaza,
    borderRadius: 9999,
    overflow: 'hidden',
  },
  pie: {
    marginTop: 16,
    paddingTop: 16,
    borderTopWidth: 1,
    borderTopColor: COLORS.borde,
  },
  botonSalir: {
    alignItems: 'center',
    paddingVertical: 10,
    paddingHorizontal: 14,
    borderWidth: 1,
    borderColor: COLORS.azul,
    borderRadius: 8,
  },
  botonSalirTexto: {
    color: COLORS.azul,
    fontWeight: '600',
    fontSize: 14,
  },
  grupo: {
    marginTop: 6,
    paddingVertical: 10,
    paddingHorizontal: 12,
    fontSize: 15,
    fontWeight: '700',
    color: COLORS.azulOscuro,
  },
  subitems: {
    marginLeft: 14,
    paddingLeft: 8,
    marginBottom: 6,
    borderLeftWidth: 2,
    borderLeftColor: COLORS.borde,
  },
  item: {
    marginBottom: 2,
    paddingVertical: 10,
    paddingHorizontal: 12,
    borderLeftWidth: 4,
    borderLeftColor: 'transparent',
    borderRadius: 6,
  },
  itemTexto: {
    fontSize: 15,
    color: COLORS.texto,
  },
  activo: {
    backgroundColor: COLORS.azulClaro,
    borderLeftColor: COLORS.mostaza,
  },
  itemTextoActivo: {
    fontWeight: '700',
    color: COLORS.azulOscuro,
  },
  contenido: {
    flex: 1,
  },
  cargando: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: COLORS.fondo,
  },
  modalTexto: {
    fontSize: 15,
    lineHeight: 22,
    color: COLORS.texto,
  },
  modalBotones: {
    flexDirection: 'row',
    justifyContent: 'flex-end',
    gap: 12,
    flex: 1,
  },
});

export default function AppLayout() {
  const { rol, nombre, cargando, cerrarSesion } = useAuth();
  const pathname = usePathname();
  const router = useRouter();
  const [menuAbierto, setMenuAbierto] = useState(false);
  const [confirmando, setConfirmando] = useState(false);
  const [saliendo, setSaliendo] = useState(false);

  if (cargando) {
    return (
      <View style={estilos.cargando}>
        <ActivityIndicator size="large" color={COLORS.azul} />
      </View>
    );
  }

  if (!rol || rol !== ROLES.PACIENTE) return <Redirect href="/(auth)/login" />;

  const menu = RUTAS.filter((r) => r.roles.includes(rol));

  // Agrupa los ítems consecutivos con el mismo grupo; los ítems sin grupo quedan sueltos.
  const secciones = [];
  menu.forEach((r) => {
    const ultima = secciones[secciones.length - 1];
    if (r.grupo && ultima?.grupo === r.grupo) ultima.items.push(r);
    else secciones.push({ grupo: r.grupo, items: [r] });
  });

  const actual = segmento(pathname);
  const cerrarMenu = () => setMenuAbierto(false);

  const irA = (path) => {
    cerrarMenu();
    router.push(path);
  };

  const pedirConfirmacion = () => {
    cerrarMenu();
    setConfirmando(true);
  };

  const salir = async () => {
    setSaliendo(true);
    await cerrarSesion();
    setSaliendo(false);
    setConfirmando(false);
    router.replace('/(auth)/login');
  };

  return (
    <SafeAreaView style={estilos.pantalla}>
      <View style={estilos.encabezado}>
        <View style={estilos.gradiente} pointerEvents="none">
          {DEGRADADO.map((color, i) => (
            <View key={i} style={{ flex: 1, backgroundColor: color }} />
          ))}
        </View>
        <View style={estilos.encabezadoContenido}>
          <Logo variante="claro" />
          <TouchableOpacity style={estilos.botonMenu} onPress={() => setMenuAbierto(!menuAbierto)}>
            <Text style={estilos.botonMenuTexto}>{menuAbierto ? 'Cerrar menú' : 'Menú'}</Text>
          </TouchableOpacity>
        </View>
      </View>

      {menuAbierto && (
        <View style={estilos.menu}>
          <View style={estilos.usuarioMovil}>
            <View style={estilos.persona}>
              <View style={estilos.avatar}>
                <Text style={estilos.avatarTexto}>{iniciales(nombre)}</Text>
              </View>
              <Text style={estilos.personaNombre}>{nombre}</Text>
            </View>
            <Text style={estilos.rol}>{ETIQUETA_ROL[rol]}</Text>
          </View>

          {secciones.map((sec) =>
            sec.grupo ? (
              <View key={sec.grupo}>
                <Text style={estilos.grupo}>{sec.grupo}</Text>
                <View style={estilos.subitems}>
                  {sec.items.map((r) => {
                    const activo = segmento(r.path) === actual;
                    return (
                      <TouchableOpacity
                        key={r.path}
                        style={[estilos.item, activo && estilos.activo]}
                        onPress={() => irA(r.path)}
                      >
                        <Text style={[estilos.itemTexto, activo && estilos.itemTextoActivo]}>{r.etiqueta}</Text>
                      </TouchableOpacity>
                    );
                  })}
                </View>
              </View>
            ) : (
              (() => {
                const r = sec.items[0];
                const activo = segmento(r.path) === actual;
                return (
                  <TouchableOpacity
                    key={r.path}
                    style={[estilos.item, activo && estilos.activo]}
                    onPress={() => irA(r.path)}
                  >
                    <Text style={[estilos.itemTexto, activo && estilos.itemTextoActivo]}>{r.etiqueta}</Text>
                  </TouchableOpacity>
                );
              })()
            ),
          )}

          <View style={estilos.pie}>
            <TouchableOpacity style={estilos.botonSalir} onPress={pedirConfirmacion}>
              <Text style={estilos.botonSalirTexto}>Cerrar sesión</Text>
            </TouchableOpacity>
          </View>
        </View>
      )}

      <View style={estilos.contenido}>
        <Slot />
      </View>

      <Modal
        visible={confirmando}
        onClose={() => !saliendo && setConfirmando(false)}
        titulo="Cerrar sesión"
        footer={
          <View style={estilos.modalBotones}>
            <Button variant="secondary" disabled={saliendo} onPress={() => setConfirmando(false)}>
              Cancelar
            </Button>
            <Button className="principal" disabled={saliendo} onPress={salir}>
              {saliendo ? 'Cerrando...' : 'Cerrar sesión'}
            </Button>
          </View>
        }
      >
        <Text style={estilos.modalTexto}>¿Desea cerrar su sesión?</Text>
      </Modal>
    </SafeAreaView>
  );
}
