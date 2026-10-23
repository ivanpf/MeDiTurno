// CU-9: consultar las reservas propias, seleccionar una y confirmar su cancelación.
// CU-8 (crear una reserva) está en reservar.js; esta pantalla reutiliza su estilo visual.
// El listado usa el endpoint del CU-10; aquí no se implementan sus filtros de búsqueda.
import React, { useCallback, useRef, useState } from 'react';
import { ActivityIndicator, ScrollView, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { useFocusEffect } from 'expo-router';
import { Button } from '../../shared/src/components/Button';
import { ErrorBanner } from '../../shared/src/components/ErrorBanner';
import { useReservas } from '../../shared/src/hooks/useReservas';
import { COLORS } from '../../shared/src/theme/colors';
import { fechaVisible } from '../../src/features/reservas/reservaConsulta';
import { ESTADOS_RESERVA, motivoNoCancelable } from '../../src/features/reservas/cancelacionReserva';

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
  exito: { backgroundColor: COLORS.exitoClaro, borderRadius: 8, padding: 16, gap: 8 },
  tituloExito: { fontSize: 20, fontWeight: '700', color: COLORS.exito },
});

function AvisoError({ error, onDismiss }) {
  if (!error) return null;
  return error.estado === 501 ? (
    <View style={estilos.aviso} accessibilityLiveRegion="polite">
      <Text style={estilos.fuerte}>Función pendiente</Text>
      <Text style={estilos.texto}>{error.mensaje}</Text>
    </View>
  ) : <ErrorBanner mensaje={error.mensaje} detalles={error.detalles} onDismiss={onDismiss} />;
}

export default function ReservasScreen() {
  const { reservas, cargando, error, cargarReservas, cancelar, cancelando,
    errorCancelacion, requiereActualizar, limpiarErrorCancelacion } = useReservas();
  const [idSeleccionada, setIdSeleccionada] = useState(null);
  const [confirmando, setConfirmando] = useState(false);
  const [cancelada, setCancelada] = useState(null);
  const scroll = useRef(null);
  const ocupado = cargando || cancelando !== null;

  const actualizar = useCallback(() => {
    setIdSeleccionada(null);
    setConfirmando(false);
    setCancelada(null);
    return cargarReservas();
  }, [cargarReservas]);

  useFocusEffect(useCallback(() => { actualizar(); }, [actualizar]));

  function seleccionar(reserva) {
    setIdSeleccionada(reserva.idReserva);
    setConfirmando(false);
    setCancelada(null);
    limpiarErrorCancelacion();
  }

  async function confirmar(reserva) {
    if (await cancelar(reserva.idReserva)) {
      setCancelada(reserva.idReserva);
      setIdSeleccionada(null);
    }
    setConfirmando(false);
    scroll.current?.scrollTo({ y: 0, animated: true });
  }

  return (
    <ScrollView ref={scroll} style={estilos.contenedor} contentContainerStyle={estilos.contenido}>
      <View style={estilos.panel}>
        <Text style={estilos.titulo} accessibilityRole="header">Mis reservas</Text>
        <Text style={estilos.texto}>Seleccione una reserva para ver sus datos y solicitar su cancelación.</Text>
        <Text style={estilos.texto}>Puede cancelar reservas vigentes con al menos 48 horas de anticipación. Los horarios corresponden a Uruguay.</Text>

        {cancelada !== null && (
          <View style={estilos.exito} accessibilityLiveRegion="polite">
            <Text style={estilos.tituloExito}>Reserva cancelada correctamente</Text>
            <Text style={estilos.texto}>Identificador: {cancelada}. La reserva permanece en su historial.</Text>
          </View>
        )}
        <AvisoError error={error} onDismiss={actualizar} />
        <AvisoError error={errorCancelacion} onDismiss={limpiarErrorCancelacion} />
        {requiereActualizar && <Text style={estilos.texto}>Actualice sus reservas antes de solicitar otra cancelación.</Text>}

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
          <View style={estilos.aviso}><Text style={estilos.texto}>No tiene reservas registradas.</Text></View>
        )}

        {/* CU-9: selección y confirmación. El CU-8 permanece en la pantalla Reservar consulta. */}
        {!cargando && !error && (
          <View style={estilos.lista}>
            {reservas.map((reserva) => {
              const seleccionada = idSeleccionada === reserva.idReserva;
              const motivo = motivoNoCancelable(reserva);
              return (
                <View key={reserva.idReserva} style={[estilos.tarjeta, seleccionada && estilos.elegida]}>
                  <TouchableOpacity style={estilos.fila} onPress={() => seleccionar(reserva)} disabled={ocupado}
                    accessibilityRole="button" accessibilityLabel={`Reserva ${reserva.idReserva}, ${reserva.especialidad}`}
                    accessibilityState={{ selected: seleccionada, disabled: ocupado, expanded: seleccionada }}>
                    <Text style={estilos.fuerte}>{reserva.especialidad} · {reserva.nombreMedico}</Text>
                    <Text style={estilos.texto}>{fechaVisible(reserva.fecha)} · ~{reserva.horaEstimada?.slice(0, 5) || reserva.horaInicio?.slice(0, 5)}</Text>
                    <Text style={estilos.texto}>Reserva {reserva.idReserva} · {ESTADOS_RESERVA[reserva.estado] || reserva.estado}</Text>
                    <Text style={estilos.fuerte}>{seleccionada ? 'Datos de la reserva' : 'Ver reserva'}</Text>
                  </TouchableOpacity>
                  {seleccionada && (
                    <View style={estilos.detalle}>
                      <Text style={estilos.texto}>Consultorio: {reserva.consultorio}</Text>
                      <Text style={estilos.texto}>Número de atención: {reserva.numero}</Text>
                      {motivo ? <Text style={estilos.texto}>{motivo}</Text> : (
                        confirmando ? (
                          <>
                            <View style={estilos.aviso}>
                              <Text style={estilos.subtitulo}>¿Desea cancelar esta reserva?</Text>
                              <Text style={estilos.texto}>Se liberará su número de atención. El plazo y el estado se volverán a comprobar al confirmar.</Text>
                            </View>
                            <Button variant="danger" onPress={() => confirmar(reserva)} disabled={ocupado || requiereActualizar} accessibilityRole="button">
                              {cancelando === reserva.idReserva ? 'Cancelando…' : 'Confirmar cancelación'}
                            </Button>
                            <Button variant="secondary" onPress={() => setConfirmando(false)} disabled={ocupado} accessibilityRole="button">Conservar reserva</Button>
                          </>
                        ) : <Button variant="secondary" onPress={() => setConfirmando(true)} disabled={ocupado || requiereActualizar} accessibilityRole="button">Cancelar reserva</Button>
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
