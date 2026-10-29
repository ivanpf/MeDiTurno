// CU-9: próximas reservas y cancelación. CU-10: historial completo, búsqueda y detalle.
// CU-8 (crear una reserva) está en reservar.js; esta pantalla reutiliza su estilo visual.
import React, { useCallback, useEffect, useRef, useState } from 'react';
import { ActivityIndicator, BackHandler, ScrollView, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { useFocusEffect } from 'expo-router';
import { Button } from '../../shared/src/components/Button';
import { ErrorBanner } from '../../shared/src/components/ErrorBanner';
import { Input } from '../../shared/src/components/Input';
import { Select } from '../../shared/src/components/Select';
import { useReservas } from '../../shared/src/hooks/useReservas';
import { COLORS } from '../../shared/src/theme/colors';
import { fechaVisible } from '../../src/features/reservas/reservaConsulta';
import { ESTADOS_RESERVA, motivoNoCancelable } from '../../src/features/reservas/cancelacionReserva';
import { useHistorialReservas } from '../../src/features/reservas/useHistorialReservas';
import {
  DIRECCIONES_HISTORIAL, ESTADOS_HISTORIAL, ORDENES_HISTORIAL, formatearFechaHistorial, horaDelHistorial,
  ESTADOS_ORDEN_HISTORIAL,
} from '../../src/features/reservas/historialReservas';

const estilos = StyleSheet.create({
  contenedor: { flex: 1, backgroundColor: COLORS.fondo },
  contenido: { padding: 16, paddingBottom: 40 },
  panel: { backgroundColor: COLORS.blanco, borderColor: COLORS.borde, borderWidth: 1,
    borderRadius: 12, padding: 20, gap: 20 },
  titulo: { fontSize: 24, fontWeight: '700', color: COLORS.azulOscuro },
  subtitulo: { fontSize: 18, fontWeight: '600', color: COLORS.texto },
  texto: { fontSize: 15, lineHeight: 22, color: COLORS.gris },
  fuerte: { fontSize: 16, fontWeight: '600', color: COLORS.azulOscuro },
  carga: { flexDirection: 'row', alignItems: 'center', gap: 12 },
  lista: { gap: 12 },
  tarjeta: { borderWidth: 1, borderColor: COLORS.borde, borderRadius: 8, overflow: 'hidden' },
  elegida: { borderColor: COLORS.azul, backgroundColor: COLORS.azulClaro },
  fila: { padding: 14, gap: 6, minHeight: 48 },
  detalle: { padding: 14, gap: 12, borderTopWidth: 1, borderTopColor: COLORS.borde },
  aviso: { backgroundColor: COLORS.mostazaClaro, borderRadius: 8, padding: 16, gap: 8 },
  error: { backgroundColor: COLORS.errorClaro, borderLeftColor: COLORS.error, borderLeftWidth: 4,
    borderRadius: 8, padding: 12 },
  textoError: { fontSize: 15, lineHeight: 22, color: COLORS.error },
  exito: { backgroundColor: COLORS.exitoClaro, borderRadius: 8, padding: 16, gap: 8 },
  tituloExito: { fontSize: 20, fontWeight: '700', color: COLORS.exito },
  pestanas: { flexDirection: 'row', gap: 8, paddingHorizontal: 16, paddingTop: 12 },
  pestana: { flex: 1, minHeight: 48, justifyContent: 'center', alignItems: 'center',
    borderWidth: 1, borderColor: COLORS.azul, borderRadius: 8, backgroundColor: COLORS.blanco, padding: 10 },
  pestanaActiva: { backgroundColor: COLORS.azulClaro, borderWidth: 2 },
  textoPestana: { fontSize: 15, fontWeight: '600', color: COLORS.azulOscuro },
  deshabilitada: { opacity: 0.5 },
  filtros: { gap: 4 },
  filaFiltros: { flexDirection: 'row', gap: 12 },
  columnaFiltro: { flex: 1, minWidth: 0, width: 0 },
  acciones: { flexDirection: 'row', flexWrap: 'wrap', gap: 12 },
  accion: { flex: 1, minWidth: 120 },
  errorCampo: { color: COLORS.error, fontSize: 14, lineHeight: 20, marginBottom: 12 },
  contextoError: { gap: 12 },
  etiqueta: { fontSize: 15, fontWeight: '600', color: COLORS.azulOscuro },
});

function AvisoError({ error, onDismiss }) {
  if (!error) return null;
  if (error.estado === 501) return (
    <View style={estilos.aviso} accessibilityLiveRegion="polite">
      <Text style={estilos.fuerte}>Función no disponible</Text>
      <Text style={estilos.texto}>{error.mensaje}</Text>
    </View>
  );
  return onDismiss ? <ErrorBanner mensaje={error.mensaje} detalles={error.detalles} onDismiss={onDismiss} /> : (
    <View style={estilos.error} accessibilityRole="alert">
      <Text style={estilos.textoError}>{error.mensaje}</Text>
      {error.detalles?.map((detalle, indice) => <Text key={indice} style={estilos.textoError}>{detalle}</Text>)}
    </View>
  );
}

// Datos de la vista previa que se está confirmando, recibidos del servidor.
function DatosConfirmacion({ reserva }) {
  return (
    <View style={estilos.aviso}>
      <Text style={estilos.subtitulo}>¿Desea cancelar esta reserva?</Text>
      <Text style={estilos.fuerte}>Reserva {reserva.idReserva} · Número {reserva.numero}</Text>
      <Text style={estilos.texto}>{fechaVisible(reserva.fecha)}</Text>
      <Text style={estilos.texto}>Turno: {reserva.horaInicio.slice(0, 5)}–{reserva.horaFin.slice(0, 5)}</Text>
      <Text style={estilos.texto}>{reserva.especialidad} · {reserva.nombreMedico}</Text>
      <Text style={estilos.texto}>Consultorio: {reserva.consultorio}</Text>
      <Text style={estilos.texto}>Se liberará su número de atención. El plazo y el estado se volverán a comprobar al confirmar.</Text>
    </View>
  );
}

// ---------------- CU-9: próximas reservas, vista previa y cancelación ----------------
function ReservasProximas({ onOperacion }) {
  const { reservas, cargando, error, cargarReservas, cancelar, preparando, cancelando,
    confirmacion, cancelada, idSeleccionada, seleccionarReserva, prepararCancelacion,
    descartarConfirmacion, errorCancelacion, requiereActualizar, limpiarErrorCancelacion } = useReservas();
  const scroll = useRef(null);
  const ocupado = cargando || preparando !== null || cancelando !== null;
  const actualizar = useCallback(() => { cargarReservas(); }, [cargarReservas]);

  useFocusEffect(useCallback(() => { actualizar(); }, [actualizar]));
  // Durante una cancelación no se cambia de sección para poder mostrar su resultado.
  useEffect(() => { onOperacion(preparando !== null || cancelando !== null); },
    [preparando, cancelando, onOperacion]);

  async function solicitarCancelacion(idReserva) {
    if (!await prepararCancelacion(idReserva)) scroll.current?.scrollTo({ y: 0, animated: true });
  }

  async function confirmar() {
    await cancelar();
    scroll.current?.scrollTo({ y: 0, animated: true });
  }

  return (
    <ScrollView ref={scroll} style={estilos.contenedor} contentContainerStyle={estilos.contenido}>
      <View style={estilos.panel}>
        <Text style={estilos.titulo} accessibilityRole="header">Mis reservas</Text>
        <Text style={estilos.texto}>Consulte sus reservas de hoy y de fechas próximas. Seleccione una para ver sus datos y solicitar su cancelación.</Text>
        <Text style={estilos.texto}>Puede cancelar reservas vigentes hasta 48 horas antes del inicio del turno, inclusive. Los horarios corresponden a Uruguay.</Text>

        {cancelada !== null && (
          <View style={estilos.exito} accessibilityLiveRegion="polite">
            <Text style={estilos.tituloExito}>Reserva cancelada correctamente</Text>
            <Text style={estilos.texto}>Identificador: {cancelada}. Su número quedó disponible y la cancelación se conserva registrada.</Text>
          </View>
        )}
        <AvisoError error={error} />
        <AvisoError error={errorCancelacion} onDismiss={limpiarErrorCancelacion} />
        {requiereActualizar && !cargando && <Text style={estilos.texto}>Actualice sus reservas antes de solicitar otra cancelación.</Text>}

        <Button variant="secondary" onPress={actualizar} disabled={ocupado} accessibilityRole="button">
          {error ? 'Reintentar carga' : 'Actualizar reservas'}
        </Button>
        {cargando && (
          <View style={estilos.carga} accessibilityLiveRegion="polite">
            <ActivityIndicator color={COLORS.azul} accessibilityLabel="Cargando sus reservas" />
            <Text style={estilos.texto}>Cargando sus reservas…</Text>
          </View>
        )}
        {!cargando && !error && reservas.length === 0 && (
          <View style={estilos.aviso}><Text style={estilos.texto}>No tiene reservas para hoy ni para fechas próximas.</Text></View>
        )}

        {/* CU-9: selección y confirmación. El CU-8 permanece en Reservar consulta. */}
        {!cargando && !error && (
          <View style={estilos.lista}>
            {reservas.map((item) => {
              const reserva = item.reserva;
              const seleccionada = idSeleccionada === reserva.idReserva;
              const motivo = motivoNoCancelable(item);
              const preparada = confirmacion?.reserva.idReserva === reserva.idReserva;
              return (
                <View key={reserva.idReserva} style={[estilos.tarjeta, seleccionada && estilos.elegida]}>
                  <TouchableOpacity style={estilos.fila} onPress={() => seleccionarReserva(reserva.idReserva)} disabled={ocupado}
                    accessibilityRole="button" accessibilityLabel={`Reserva ${reserva.idReserva}, ${reserva.especialidad}`}
                    accessibilityState={{ selected: seleccionada, disabled: ocupado, expanded: seleccionada }}>
                    <Text style={estilos.fuerte}>{reserva.especialidad} · {reserva.nombreMedico}</Text>
                    <Text style={estilos.texto}>{fechaVisible(reserva.fecha)}</Text>
                    <Text style={estilos.texto}>Turno: {reserva.horaInicio.slice(0, 5)}–{reserva.horaFin.slice(0, 5)}</Text>
                    <Text style={estilos.texto}>Reserva {reserva.idReserva} · {ESTADOS_RESERVA[reserva.estado] || reserva.estado}</Text>
                    {item.fechaCancelacion && (
                      <Text style={estilos.texto}>Cancelada el {fechaVisible(item.fechaCancelacion.slice(0, 10))} a las {item.fechaCancelacion.slice(11, 16)}</Text>
                    )}
                    <Text style={estilos.fuerte}>{seleccionada ? 'Datos de la reserva' : 'Ver reserva'}</Text>
                  </TouchableOpacity>
                  {seleccionada && (
                    <View style={estilos.detalle}>
                      <Text style={estilos.texto}>Consultorio: {reserva.consultorio}</Text>
                      <Text style={estilos.texto}>Número de atención: {reserva.numero}</Text>
                      <Text style={estilos.texto}>Su hora estimada: ~{reserva.horaEstimada?.slice(0, 5) || reserva.horaInicio.slice(0, 5)}</Text>
                      {motivo ? <Text style={estilos.texto}>{motivo}</Text> : (
                        preparada ? (
                          <>
                            <DatosConfirmacion reserva={confirmacion.reserva} />
                            <Button variant="danger" onPress={confirmar} disabled={ocupado || requiereActualizar} accessibilityRole="button">
                              {cancelando === reserva.idReserva ? 'Cancelando…' : 'Confirmar cancelación'}
                            </Button>
                            <Button variant="secondary" onPress={descartarConfirmacion} disabled={ocupado} accessibilityRole="button">Conservar reserva</Button>
                          </>
                        ) : (
                          <>
                            {preparando === reserva.idReserva && (
                              <View style={estilos.carga} accessibilityLiveRegion="polite">
                                <ActivityIndicator color={COLORS.azul} accessibilityLabel="Verificando la reserva" />
                                <Text style={estilos.texto}>Verificando si la reserva puede cancelarse…</Text>
                              </View>
                            )}
                            <Button variant="secondary" onPress={() => solicitarCancelacion(reserva.idReserva)} disabled={ocupado || requiereActualizar} accessibilityRole="button">
                              Cancelar reserva
                            </Button>
                          </>
                        )
                      )}
                    </View>
                  )}
                </View>
              );
            })}
          </View>
        )}
      </View>
    </ScrollView>
  );
}

// ---------------- CU-10: filtros, historial completo y detalle ----------------
function DetalleHistorial({ reserva }) {
  return (
    <View style={estilos.detalle} accessibilityLiveRegion="polite">
      <Text style={estilos.subtitulo} accessibilityRole="header">Detalle de la reserva {reserva.idReserva}</Text>
      <Text style={estilos.texto}>Especialidad: {reserva.especialidad || 'Sin especialidad'}</Text>
      <Text style={estilos.texto}>Médico: {reserva.nombreMedico || 'Sin médico'}</Text>
      <Text style={estilos.texto}>Fecha: {reserva.fecha ? fechaVisible(reserva.fecha) : 'Sin fecha'}</Text>
      <Text style={estilos.texto}>Turno: {horaDelHistorial(reserva.horaInicio)}–{horaDelHistorial(reserva.horaFin)}</Text>
      <Text style={estilos.texto}>Consultorio: {reserva.consultorio || 'Sin consultorio'}</Text>
      <Text style={estilos.texto}>Número de atención: {reserva.numero ?? 'Sin número'}</Text>
      <Text style={estilos.texto}>Hora estimada: {reserva.horaEstimada ? `~${horaDelHistorial(reserva.horaEstimada)}` : 'No informada'}</Text>
      <Text style={estilos.texto}>Estado de la reserva: {ESTADOS_RESERVA[reserva.estado] || reserva.estado || 'No informado'}</Text>
      {reserva.numeroOrden != null ? (
        <>
          <Text style={estilos.texto}>Número de orden: {reserva.numeroOrden}</Text>
          <Text style={estilos.texto}>Estado de la orden: {ESTADOS_ORDEN_HISTORIAL[reserva.estadoOrden] || reserva.estadoOrden || 'No informado'}</Text>
        </>
      ) : <Text style={estilos.texto}>Sin orden emitida.</Text>}
    </View>
  );
}

function HistorialReservas() {
  const historial = useHistorialReservas();
  const { actualizar, descartarConsultas, cerrarDetalle, idSeleccionada } = historial;
  const { filtros, cargando, error, errorPeriodo } = historial;
  const especialidades = [{ value: '', label: 'Todas las especialidades' }, ...historial.especialidades];
  // Conserva visible un código escrito mientras el catálogo todavía no estaba disponible.
  if (filtros.codigoEspecialidad && !especialidades.some((opcion) => opcion.value === filtros.codigoEspecialidad)) {
    especialidades.push({ value: filtros.codigoEspecialidad, label: filtros.codigoEspecialidad });
  }

  useFocusEffect(useCallback(() => {
    actualizar();
    return descartarConsultas;
  }, [actualizar, descartarConsultas]));

  useEffect(() => {
    if (idSeleccionada === null) return;
    const suscripcion = BackHandler.addEventListener('hardwareBackPress', () => {
      cerrarDetalle();
      return true;
    });
    return () => suscripcion.remove();
  }, [idSeleccionada, cerrarDetalle]);

  return (
    <ScrollView style={estilos.contenedor} contentContainerStyle={estilos.contenido} keyboardShouldPersistTaps="handled">
      <View style={estilos.panel}>
        <Text style={estilos.titulo} accessibilityRole="header">Historial de reservas</Text>
        <Text style={estilos.texto}>Consulte sus reservas anteriores y futuras, incluidas las canceladas. Los horarios corresponden a Uruguay.</Text>

        <View style={estilos.filtros}>
          <Text style={estilos.subtitulo} accessibilityRole="header">Buscar reservas</Text>
          <View style={estilos.filaFiltros}>
            <Input label="Desde" placeholder="DD/MM/AAAA" keyboardType="number-pad" maxLength={10}
              value={filtros.fechaDesde} style={estilos.columnaFiltro} accessibilityLabel="Fecha desde"
              onChangeText={(valor) => historial.cambiarFiltro('fechaDesde', formatearFechaHistorial(valor))} />
            <Input label="Hasta" placeholder="DD/MM/AAAA" keyboardType="number-pad" maxLength={10}
              value={filtros.fechaHasta} style={estilos.columnaFiltro} accessibilityLabel="Fecha hasta"
              onChangeText={(valor) => historial.cambiarFiltro('fechaHasta', formatearFechaHistorial(valor))} />
          </View>
          {errorPeriodo && <Text style={estilos.errorCampo} accessibilityRole="alert">{errorPeriodo}</Text>}

          {historial.especialidades.length > 0 ? (
            <Select label="Especialidad" placeholder="Todas las especialidades" value={filtros.codigoEspecialidad}
              options={especialidades} onValueChange={(valor) => historial.cambiarFiltro('codigoEspecialidad', valor)} />
          ) : (
            <Input label="Código de especialidad" placeholder="Todas las especialidades" value={filtros.codigoEspecialidad}
              autoCapitalize="none" autoCorrect={false} maxLength={50}
              onChangeText={(valor) => historial.cambiarFiltro('codigoEspecialidad', valor)} />
          )}
          {historial.cargandoEspecialidades && <Text style={estilos.texto}>Cargando especialidades…</Text>}
          {historial.errorEspecialidades && (
            <View style={estilos.contextoError}>
              <Text style={estilos.etiqueta}>No se pudo actualizar el listado de especialidades.</Text>
              <AvisoError error={historial.errorEspecialidades} />
              <Button variant="secondary" onPress={historial.cargarEspecialidades} disabled={historial.cargandoEspecialidades}>
                Reintentar especialidades
              </Button>
            </View>
          )}

          {/* Contrato actual: ReservaResponseDTO no trae idMedico y GET /medicos no admite pacientes.
              Se mantiene el filtro habilitado por ID, sin inventar identificadores desde el nombre.
              Cuando backend entregue los identificadores, se puede ofrecer un selector por nombre. */}
          <Input label="Médico (identificador)" placeholder="Todos los médicos" value={filtros.idMedico}
            keyboardType="number-pad" maxLength={19} autoCorrect={false} accessibilityLabel="Identificador del médico"
            onChangeText={(valor) => historial.cambiarFiltro('idMedico', valor)} />
          <Select label="Estado de la reserva" placeholder="Todos los estados" value={filtros.estado}
            options={ESTADOS_HISTORIAL} onValueChange={(valor) => historial.cambiarFiltro('estado', valor)} />
          <View style={estilos.filaFiltros}>
            <Select label="Ordenar por" value={filtros.ordenarPor} options={ORDENES_HISTORIAL} style={estilos.columnaFiltro}
              onValueChange={(valor) => historial.cambiarFiltro('ordenarPor', valor)} />
            <Select label="Dirección" value={filtros.descendente} options={DIRECCIONES_HISTORIAL} style={estilos.columnaFiltro}
              onValueChange={(valor) => historial.cambiarFiltro('descendente', valor)} />
          </View>
          <View style={estilos.acciones}>
            <Button onPress={historial.buscar} disabled={cargando} style={estilos.accion} accessibilityRole="button">
              {cargando ? 'Buscando…' : 'Buscar'}
            </Button>
            <Button variant="secondary" onPress={historial.limpiarFiltros} disabled={cargando || !historial.hayFiltros}
              style={estilos.accion} accessibilityRole="button">Limpiar filtros</Button>
          </View>
        </View>

        {historial.hayCambios && (
          <View style={estilos.aviso} accessibilityLiveRegion="polite">
            <Text style={estilos.texto}>Hay filtros sin aplicar. Presione Buscar para actualizar los resultados.</Text>
          </View>
        )}
        {/* Se conserva el mensaje real del backend, también para 400, 403 y 501. */}
        <AvisoError error={error} />
        {cargando && (
          <View style={estilos.carga} accessibilityLiveRegion="polite">
            <ActivityIndicator color={COLORS.azul} accessibilityLabel="Cargando el historial de reservas" />
            <Text style={estilos.texto}>Cargando su historial de reservas…</Text>
          </View>
        )}
        {!cargando && !error && historial.consultado && (
          <>
            <Text style={estilos.subtitulo} accessibilityRole="header">Resultados: {historial.reservas.length}</Text>
            {historial.reservas.length === 0 ? (
              <View style={estilos.aviso} accessibilityLiveRegion="polite">
                <Text style={estilos.texto}>No se encontraron reservas para esta búsqueda.</Text>
              </View>
            ) : (
              <View style={estilos.lista}>
                {historial.reservas.map((reserva) => {
                  const seleccionada = idSeleccionada === reserva.idReserva;
                  return (
                    <View key={reserva.idReserva} style={[estilos.tarjeta, seleccionada && estilos.elegida]}>
                      <TouchableOpacity style={estilos.fila} onPress={() => historial.seleccionar(reserva.idReserva)}
                        accessibilityRole="button" accessibilityLabel={`Reserva ${reserva.idReserva}, ${reserva.especialidad || 'sin especialidad'}`}
                        accessibilityState={{ selected: seleccionada, expanded: seleccionada }}>
                        <Text style={estilos.fuerte}>{reserva.especialidad || 'Sin especialidad'} · {reserva.nombreMedico || 'Sin médico'}</Text>
                        <Text style={estilos.texto}>{reserva.fecha ? fechaVisible(reserva.fecha) : 'Sin fecha'}</Text>
                        <Text style={estilos.texto}>Turno: {horaDelHistorial(reserva.horaInicio)}–{horaDelHistorial(reserva.horaFin)}</Text>
                        <Text style={estilos.texto}>Consultorio: {reserva.consultorio || 'Sin consultorio'} · Número: {reserva.numero ?? 'Sin número'}</Text>
                        <Text style={estilos.texto}>Reserva {reserva.idReserva} · {ESTADOS_RESERVA[reserva.estado] || reserva.estado || 'Sin estado'}</Text>
                        {reserva.numeroOrden != null && (
                          <Text style={estilos.texto}>Orden {reserva.numeroOrden} · {ESTADOS_ORDEN_HISTORIAL[reserva.estadoOrden] || reserva.estadoOrden || 'Sin estado'}</Text>
                        )}
                        <Text style={estilos.fuerte}>{seleccionada ? 'Cerrar detalle' : 'Ver detalle'}</Text>
                      </TouchableOpacity>
                      {seleccionada && <DetalleHistorial reserva={reserva} />}
                    </View>
                  );
                })}
              </View>
            )}
          </>
        )}
      </View>
    </ScrollView>
  );
}

export default function ReservasScreen() {
  const [seccion, setSeccion] = useState('proximas');
  const [operando, setOperando] = useState(false);
  return (
    <View style={estilos.contenedor}>
      <View style={estilos.pestanas}>
        {[{ value: 'proximas', label: 'Próximas' }, { value: 'historial', label: 'Historial' }].map((opcion) => (
          <TouchableOpacity key={opcion.value} disabled={operando}
            style={[estilos.pestana, seccion === opcion.value && estilos.pestanaActiva, operando && estilos.deshabilitada]}
            onPress={() => setSeccion(opcion.value)} accessibilityRole="button"
            accessibilityState={{ selected: seccion === opcion.value, disabled: operando }}>
            <Text style={estilos.textoPestana}>{opcion.label}</Text>
          </TouchableOpacity>
        ))}
      </View>
      {seccion === 'proximas' ? <ReservasProximas onOperacion={setOperando} /> : <HistorialReservas />}
    </View>
  );
}
