// CU-9: consultar las reservas propias, seleccionar una y confirmar su cancelación.
// CU-8 (crear una reserva) está en reservar.js; esta pantalla reutiliza su estilo visual.
// Se usa el listado de apoyo del CU-9. La búsqueda del historial completo pertenece al CU-10.
import React, { useCallback, useRef } from 'react';
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
  error: { backgroundColor: COLORS.errorClaro, borderLeftColor: COLORS.error, borderLeftWidth: 4,
    borderRadius: 8, padding: 12 },
  textoError: { fontSize: 15, lineHeight: 22, color: COLORS.error },
  exito: { backgroundColor: COLORS.exitoClaro, borderRadius: 8, padding: 16, gap: 8 },
  tituloExito: { fontSize: 20, fontWeight: '700', color: COLORS.exito },
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

export default function ReservasScreen() {
  const { reservas, cargando, error, cargarReservas, cancelar, preparando, cancelando,
    confirmacion, cancelada, idSeleccionada, seleccionarReserva, prepararCancelacion,
    descartarConfirmacion, errorCancelacion, requiereActualizar, limpiarErrorCancelacion } = useReservas();
  const scroll = useRef(null);
  const ocupado = cargando || preparando !== null || cancelando !== null;
  const actualizar = useCallback(() => { cargarReservas(); }, [cargarReservas]);

  useFocusEffect(useCallback(() => { actualizar(); }, [actualizar]));

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
