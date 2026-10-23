// CU-11 Consulta de la historia clínica propia: filtros, listado de atenciones finalizadas y detalle de la elegida.
// La pantalla describe qué mostrar; useHistoriaClinica maneja el estado y las peticiones.
import React, { useEffect, useRef } from 'react';
import { ActivityIndicator, BackHandler, ScrollView, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { Button } from '../../shared/src/components/Button';
import { ErrorBanner } from '../../shared/src/components/ErrorBanner';
import { Input } from '../../shared/src/components/Input';
import { Select } from '../../shared/src/components/Select';
import { COLORS } from '../../shared/src/theme/colors';
import { useHistoriaClinica } from '../../src/features/consultas/useHistoriaClinica';
import {
  ORDENES, fechaVisible, formatearEntradaFecha, seccionesDelDetalle,
} from '../../src/features/consultas/historiaClinica';

const estilos = StyleSheet.create({
  contenedor: { flex: 1, backgroundColor: COLORS.fondo },
  contenido: { padding: 16, paddingBottom: 40 },
  panel: { backgroundColor: COLORS.blanco, borderColor: COLORS.borde, borderWidth: 1,
    borderRadius: 12, padding: 20, gap: 16 },
  titulo: { fontSize: 24, fontWeight: '700', color: COLORS.azulOscuro },
  texto: { fontSize: 15, lineHeight: 22, color: COLORS.gris },
  carga: { flexDirection: 'row', alignItems: 'center', gap: 12 },
  aviso: { backgroundColor: COLORS.mostazaClaro, borderLeftWidth: 4, borderLeftColor: COLORS.mostaza,
    borderRadius: 8, padding: 16 },
  avisoTexto: { fontSize: 15, lineHeight: 22, color: COLORS.texto },
  errorCampo: { marginTop: -8, fontSize: 13, color: COLORS.error },

  // Filtros: de a dos por fila, como en la interfaz acordada.
  fila: { flexDirection: 'row', gap: 12 },
  columna: { flex: 1 },

  // Listado en forma de tabla: encabezado celeste y una fila por atención.
  tabla: { borderWidth: 1, borderColor: COLORS.borde, borderRadius: 8, overflow: 'hidden' },
  filaTabla: { flexDirection: 'row', alignItems: 'center', gap: 8, paddingVertical: 10, paddingHorizontal: 10,
    borderTopWidth: 1, borderTopColor: COLORS.borde },
  encabezado: { backgroundColor: COLORS.azulClaro, borderTopWidth: 0 },
  filaElegida: { backgroundColor: COLORS.azulClaro },
  celdaFecha: { width: 86 },
  celda: { flex: 1 },
  celdaAccion: { width: 74, alignItems: 'flex-end' },
  textoEncabezado: { fontSize: 13, fontWeight: '700', color: COLORS.azulOscuro },
  textoCelda: { fontSize: 14, lineHeight: 20, color: COLORS.texto },
  botonDetalle: { paddingVertical: 6, paddingHorizontal: 8, borderWidth: 1, borderColor: COLORS.azul,
    borderRadius: 8, backgroundColor: COLORS.blanco },
  botonDetalleElegido: { backgroundColor: COLORS.azul },
  textoBotonDetalle: { fontSize: 13, fontWeight: '600', color: COLORS.azul, textAlign: 'center' },
  textoBotonDetalleElegido: { color: COLORS.blanco },

  // Detalle de la atención, debajo del listado.
  detalle: { backgroundColor: COLORS.fondo, borderWidth: 1, borderColor: COLORS.borde, borderRadius: 12,
    padding: 16, gap: 14 },
  tituloDetalle: { fontSize: 18, fontWeight: '700', color: COLORS.azulOscuro },
  seccion: { gap: 4 },
  etiqueta: { fontSize: 15, fontWeight: '700', color: COLORS.azulOscuro },
  textoClinico: { fontSize: 15, lineHeight: 22, color: COLORS.texto },
  cerrar: { alignSelf: 'flex-start', minWidth: 120 },
});

function Cargando({ mensaje }) {
  return (
    <View style={estilos.carga} accessibilityLiveRegion="polite">
      <ActivityIndicator color={COLORS.azul} accessibilityLabel={mensaje} />
      <Text style={estilos.texto}>{mensaje}</Text>
    </View>
  );
}

function Aviso({ children }) {
  return (
    <View style={estilos.aviso} accessibilityLiveRegion="polite">
      <Text style={estilos.avisoTexto}>{children}</Text>
    </View>
  );
}

function Detalle({ atencion, onCerrar }) {
  // En Android, el botón «atrás» cierra el detalle en lugar de salir de la pantalla.
  useEffect(() => {
    const suscripcion = BackHandler.addEventListener('hardwareBackPress', () => {
      onCerrar();
      return true;
    });
    return () => suscripcion.remove();
  }, [onCerrar]);

  return (
    <View style={estilos.detalle} accessibilityLiveRegion="polite">
      <Text style={estilos.tituloDetalle} accessibilityRole="header">Atención del {fechaVisible(atencion.fecha)}</Text>
      <Text style={estilos.texto}>{atencion.especialidad} · {atencion.nombreMedico}</Text>
      {seccionesDelDetalle(atencion).map((seccion) => (
        <View key={seccion.titulo} style={estilos.seccion}>
          <Text style={estilos.etiqueta}>{seccion.titulo}:</Text>
          <Text style={estilos.textoClinico}>{seccion.texto}</Text>
        </View>
      ))}
      <Button variant="secondary" onPress={onCerrar} style={estilos.cerrar} accessibilityRole="button">Cerrar</Button>
    </View>
  );
}

export default function HistoriaClinicaScreen() {
  const historia = useHistoriaClinica();
  const scroll = useRef(null);
  const posicionDetalle = useRef(0);
  const { atenciones, tieneAtenciones, opciones, filtros, hayFiltros, cargando, error, errorPeriodo,
    seleccionada } = historia;

  // Al elegir una atención se baja hasta su detalle, que queda debajo del listado.
  const mostrarDetalle = () => scroll.current?.scrollTo({ y: posicionDetalle.current, animated: true });
  // onLayout no se repite si se elige otra atención y el detalle no cambia de tamaño.
  useEffect(() => {
    if (seleccionada && posicionDetalle.current) mostrarDetalle();
  }, [seleccionada?.idConsulta]);

  return (
    <ScrollView ref={scroll} style={estilos.contenedor} contentContainerStyle={estilos.contenido}
      keyboardShouldPersistTaps="handled">
      <View style={estilos.panel}>
        <Text style={estilos.titulo} accessibilityRole="header">Mi historia clínica</Text>

        {error && <ErrorBanner mensaje={error.mensaje} detalles={error.detalles} onDismiss={historia.limpiarError} />}

        {tieneAtenciones === null && cargando && <Cargando mensaje="Cargando su historia clínica…" />}
        {tieneAtenciones === null && !cargando && error && (
          <Button variant="secondary" onPress={historia.cargarHistoria} accessibilityRole="button">Reintentar</Button>
        )}

        {/* Flujo alternativo 3.A: no existen atenciones finalizadas. */}
        {tieneAtenciones === false && (
          <>
            <Aviso>No existen atenciones finalizadas para visualizar en su historia clínica.</Aviso>
            <Button variant="secondary" onPress={historia.cargarHistoria} disabled={cargando} accessibilityRole="button">
              Actualizar
            </Button>
          </>
        )}

        {tieneAtenciones && (
          <>
            <View>
              <View style={estilos.fila}>
                <Input label="Desde" placeholder="DD/MM/AAAA" keyboardType="number-pad" maxLength={10}
                  value={filtros.fechaDesde} style={estilos.columna}
                  onChangeText={(valor) => historia.cambiarFiltro('fechaDesde', formatearEntradaFecha(valor))} />
                <Input label="Hasta" placeholder="DD/MM/AAAA" keyboardType="number-pad" maxLength={10}
                  value={filtros.fechaHasta} style={estilos.columna}
                  onChangeText={(valor) => historia.cambiarFiltro('fechaHasta', formatearEntradaFecha(valor))} />
              </View>
              {errorPeriodo && <Text style={estilos.errorCampo} accessibilityRole="alert">{errorPeriodo}</Text>}
              <View style={estilos.fila}>
                <View style={estilos.columna}>
                  <Select label="Especialidad" value={filtros.codigoEspecialidad}
                    onValueChange={(valor) => historia.cambiarFiltro('codigoEspecialidad', valor)}
                    options={[{ value: '', label: 'Todas' }, ...opciones.especialidades]} />
                </View>
                <View style={estilos.columna}>
                  <Select label="Médico" value={filtros.idMedico}
                    onValueChange={(valor) => historia.cambiarFiltro('idMedico', valor)}
                    options={[{ value: '', label: 'Todos' }, ...opciones.medicos]} />
                </View>
              </View>
              <Select label="Ordenar por fecha" value={String(filtros.descendente)}
                onValueChange={(valor) => historia.cambiarFiltro('descendente', valor === 'true')} options={ORDENES} />
              {hayFiltros && (
                <Button variant="secondary" onPress={historia.limpiarFiltros} disabled={cargando} accessibilityRole="button">
                  Limpiar filtros
                </Button>
              )}
            </View>

            {cargando && <Cargando mensaje="Buscando atenciones…" />}
            {!cargando && atenciones.length === 0 && (
              <Aviso>No se encontraron atenciones con los criterios seleccionados.</Aviso>
            )}

            {!cargando && atenciones.length > 0 && (
              <View style={estilos.tabla}>
                <View style={[estilos.filaTabla, estilos.encabezado]}>
                  <Text style={[estilos.textoEncabezado, estilos.celdaFecha]}>Fecha</Text>
                  <Text style={[estilos.textoEncabezado, estilos.celda]}>Especialidad</Text>
                  <Text style={[estilos.textoEncabezado, estilos.celda]}>Médico</Text>
                  <View style={estilos.celdaAccion} />
                </View>
                {atenciones.map((atencion) => {
                  const elegida = seleccionada?.idConsulta === atencion.idConsulta;
                  return (
                    <View key={atencion.idConsulta} style={[estilos.filaTabla, elegida && estilos.filaElegida]}>
                      <Text style={[estilos.textoCelda, estilos.celdaFecha]}>{fechaVisible(atencion.fecha)}</Text>
                      <Text style={[estilos.textoCelda, estilos.celda]}>{atencion.especialidad}</Text>
                      <Text style={[estilos.textoCelda, estilos.celda]}>{atencion.nombreMedico}</Text>
                      <View style={estilos.celdaAccion}>
                        <TouchableOpacity accessibilityRole="button" accessibilityState={{ selected: elegida }}
                          accessibilityLabel={`Ver detalle de la atención del ${fechaVisible(atencion.fecha)}, ${atencion.especialidad}`}
                          onPress={() => historia.verDetalle(atencion)}
                          style={[estilos.botonDetalle, elegida && estilos.botonDetalleElegido]}>
                          <Text style={[estilos.textoBotonDetalle, elegida && estilos.textoBotonDetalleElegido]}>
                            Ver detalle
                          </Text>
                        </TouchableOpacity>
                      </View>
                    </View>
                  );
                })}
              </View>
            )}

            {seleccionada && (
              // La posición es relativa al panel: se suma el margen del contenido (16).
              <View onLayout={(e) => { posicionDetalle.current = e.nativeEvent.layout.y + 16; mostrarDetalle(); }}>
                <Detalle atencion={seleccionada} onCerrar={historia.cerrarDetalle} />
              </View>
            )}
          </>
        )}
      </View>
    </ScrollView>
  );
}
