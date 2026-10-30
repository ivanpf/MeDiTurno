// CU-8 completo: especialidad → médico → fecha → número → confirmación.
// CU-9 (cancelar una reserva existente) se implementa por separado en reservas.js.
// La pantalla describe qué mostrar; useReservaConsulta maneja el estado y las peticiones.
import React, { useRef } from 'react';
import { ActivityIndicator, ScrollView, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { Button } from '../../shared/src/components/Button';
import { ErrorBanner } from '../../shared/src/components/ErrorBanner';
import { Select } from '../../shared/src/components/Select';
import { COLORS } from '../../shared/src/theme/colors';
import { useReservaConsulta } from '../../src/features/reservas/useReservaConsulta';
import { cuposLibres, fechaVisible, horaEstimada, numerosDelTurno } from '../../src/features/reservas/reservaConsulta';

const estilos = StyleSheet.create({
  contenedor: { flex: 1, backgroundColor: COLORS.fondo },
  contenido: { padding: 16, paddingBottom: 40 },
  panel: { backgroundColor: COLORS.blanco, borderColor: COLORS.borde, borderWidth: 1,
    borderRadius: 12, padding: 20, gap: 20 },
  paso: { gap: 12, paddingTop: 16, borderTopWidth: 1, borderTopColor: COLORS.borde },
  titulo: { fontSize: 24, fontWeight: '700', color: COLORS.azulOscuro },
  subtitulo: { fontSize: 18, fontWeight: '600', color: COLORS.texto },
  texto: { fontSize: 15, lineHeight: 22, color: COLORS.gris },
  fuerte: { fontSize: 16, fontWeight: '600', color: COLORS.azulOscuro },
  carga: { flexDirection: 'row', alignItems: 'center', gap: 12 },
  semana: { gap: 10 },
  tarjeta: { borderWidth: 1, borderColor: COLORS.borde, borderRadius: 8, padding: 14, gap: 6 },
  elegida: { borderColor: COLORS.azul, backgroundColor: COLORS.azulClaro },
  deshabilitada: { opacity: 0.5 },
  numeros: { gap: 10 },
  numero: { flexDirection: 'row', alignItems: 'center', minHeight: 72, padding: 12,
    borderWidth: 1, borderColor: COLORS.borde, borderRadius: 8, gap: 8 },
  numeroEtiqueta: { flex: 1 },
  numeroHora: { flex: 1, textAlign: 'center' },
  numeroEstado: { flex: 1, textAlign: 'right' },
  resumen: { backgroundColor: COLORS.mostazaClaro, borderRadius: 8, padding: 16, gap: 8 },
  exito: { backgroundColor: COLORS.exitoClaro, borderRadius: 8, padding: 16, gap: 10 },
  tituloExito: { fontSize: 20, fontWeight: '700', color: COLORS.exito },
});

function Cargando({ mensaje }) {
  return (
    <View style={estilos.carga} accessibilityLiveRegion="polite">
      <ActivityIndicator color={COLORS.azul} accessibilityLabel={mensaje} />
      <Text style={estilos.texto}>{mensaje}</Text>
    </View>
  );
}

export default function ReservarScreen() {
  const flujo = useReservaConsulta();
  const scroll = useRef(null);
  const { especialidades, especialidad, codigoEspecialidad, medicos, idMedico, semanas,
    turno, numero, reserva, cargandoEspecialidades, cargandoTurnos, turnosConsultados,
    enviando, errorEspecialidades, errorTurnos, errorReserva } = flujo;
  const ocupado = cargandoEspecialidades || cargandoTurnos || enviando;

  async function confirmar() {
    await flujo.confirmarReserva();
    // La confirmación o el motivo del error aparecen arriba, también en pantallas pequeñas.
    scroll.current?.scrollTo({ y: 0, animated: true });
  }

  return (
    <ScrollView ref={scroll} style={estilos.contenedor} contentContainerStyle={estilos.contenido}>
      <View style={estilos.panel}>
        <Text style={estilos.titulo} accessibilityRole="header">Reservar consulta</Text>

        {reserva ? (
          <>
            <View style={estilos.exito} accessibilityLiveRegion="polite">
              <Text style={estilos.tituloExito}>Reserva realizada correctamente</Text>
              <Text style={estilos.fuerte}>Identificador: {reserva.idReserva}</Text>
              <Text style={estilos.fuerte}>Número de atención: {reserva.numero}</Text>
              <Text style={estilos.texto}>{reserva.especialidad} · {reserva.nombreMedico}</Text>
              <Text style={estilos.texto}>{fechaVisible(reserva.fecha)}</Text>
              <Text style={estilos.texto}>Hora estimada: ~{reserva.horaEstimada.slice(0, 5)}</Text>
              <Text style={estilos.texto}>Consultorio: {reserva.consultorio}</Text>
            </View>
            <Button onPress={flujo.iniciarOtraReserva} accessibilityRole="button">Hacer otra reserva</Button>
          </>
        ) : (
          <>
            <Text style={estilos.texto}>
              Seleccione especialidad, médico, fecha y un número de atención libre para confirmar su reserva.
            </Text>

            {errorReserva && (
              <ErrorBanner mensaje={errorReserva.mensaje} detalles={errorReserva.detalles}
                onDismiss={flujo.limpiarErrorReserva} />
            )}

            <View style={estilos.paso}>
              <Text style={estilos.subtitulo}>1. Especialidad</Text>
              {cargandoEspecialidades && <Cargando mensaje="Cargando especialidades…" />}
              {errorEspecialidades && <Text style={estilos.texto} accessibilityRole="alert">{errorEspecialidades.mensaje}</Text>}
              {!cargandoEspecialidades && !errorEspecialidades && especialidades.length === 0 && (
                <Text style={estilos.texto}>No hay especialidades activas disponibles.</Text>
              )}
              {especialidades.length > 0 && (
                <Select label="Especialidad" placeholder="Seleccione una especialidad" required
                  value={codigoEspecialidad} onValueChange={flujo.seleccionarEspecialidad}
                  options={especialidades.map((item) => ({ value: item.codigo, label: item.nombre }))}
                  disabled={cargandoEspecialidades || enviando} />
              )}
              <Button variant="secondary" onPress={flujo.cargarEspecialidades} disabled={ocupado} accessibilityRole="button">
                {errorEspecialidades ? 'Reintentar' : 'Actualizar especialidades'}
              </Button>
            </View>

            {especialidad && (
              <View style={estilos.paso}>
                <Text style={estilos.subtitulo}>2. Médico</Text>
                {especialidad.requierePase && <Text style={estilos.texto}>Esta especialidad requiere un pase o habilitación de reconsulta vigente.</Text>}
                {cargandoTurnos && <Cargando mensaje="Consultando turnos disponibles…" />}
                {errorTurnos && <Text style={estilos.texto} accessibilityRole="alert">{errorTurnos.mensaje}</Text>}
                {turnosConsultados && !cargandoTurnos && medicos.length === 0 && (
                  <Text style={estilos.texto}>No hay turnos disponibles para esta especialidad en las próximas ocho semanas.</Text>
                )}
                {medicos.length > 0 && (
                  <Select label="Médico" placeholder="Seleccione un médico" required value={idMedico}
                    onValueChange={flujo.seleccionarMedico} options={medicos} disabled={ocupado} />
                )}
                <Button variant="secondary" onPress={flujo.actualizarDisponibilidad} disabled={ocupado} accessibilityRole="button">
                  {errorTurnos ? 'Reintentar disponibilidad' : 'Actualizar disponibilidad'}
                </Button>
              </View>
            )}

            {!!idMedico && !cargandoTurnos && (
              <View style={estilos.paso}>
                <Text style={estilos.subtitulo}>3. Fecha y horario</Text>
                <Text style={estilos.texto}>Turnos disponibles durante las próximas ocho semanas.</Text>
                {semanas.length === 0 && <Text style={estilos.texto}>No hay turnos disponibles para el médico seleccionado.</Text>}
                {semanas.map((semana) => (
                  <View key={semana.inicio} style={estilos.semana}>
                    <Text style={estilos.fuerte}>Semana del {fechaVisible(semana.inicio)}</Text>
                    {semana.turnos.map((item) => {
                      const seleccionado = turno?.idOcurrenciaTurno === item.idOcurrenciaTurno;
                      const libres = cuposLibres(item);
                      const deshabilitado = enviando || libres === 0;
                      return (
                        <TouchableOpacity key={item.idOcurrenciaTurno} accessibilityRole="button"
                          accessibilityState={{ selected: seleccionado, disabled: deshabilitado }}
                          disabled={deshabilitado} onPress={() => flujo.seleccionarTurno(item)}
                          style={[estilos.tarjeta, seleccionado && estilos.elegida, deshabilitado && estilos.deshabilitada]}>
                          <Text style={estilos.fuerte}>{fechaVisible(item.fecha)}</Text>
                          <Text style={estilos.texto}>{item.horaInicio.slice(0, 5)}–{item.horaFin.slice(0, 5)}</Text>
                          <Text style={estilos.texto}>Consultorio: {item.consultorio}</Text>
                          <Text style={estilos.texto}>{libres} cupos libres</Text>
                        </TouchableOpacity>
                      );
                    })}
                  </View>
                ))}
              </View>
            )}

            {turno && (
              <View style={estilos.paso}>
                <Text style={estilos.subtitulo}>4. Número de atención</Text>
                <Text style={estilos.texto}>Los números no disponibles no se pueden elegir. Las horas son estimadas.</Text>
                <View style={estilos.numeros}>
                  {numerosDelTurno(turno).map((item) => {
                    const seleccionado = numero === item.numero;
                    const deshabilitado = enviando || !item.disponible;
                    return (
                      <TouchableOpacity key={item.numero} accessibilityRole="button"
                        accessibilityLabel={`Número ${item.numero}, hora estimada ${item.hora}${item.disponible ? '' : ', no disponible'}`}
                        accessibilityState={{ selected: seleccionado, disabled: deshabilitado }}
                        disabled={deshabilitado} onPress={() => flujo.seleccionarNumero(item.numero)}
                        style={[estilos.numero, seleccionado && estilos.elegida, deshabilitado && estilos.deshabilitada]}>
                        <Text style={[estilos.fuerte, estilos.numeroEtiqueta]}>N.º {item.numero}</Text>
                        <Text style={[estilos.texto, estilos.numeroHora]}>~{item.hora}</Text>
                        <Text style={[estilos.texto, estilos.numeroEstado]}>{!item.disponible ? 'No disponible' : 'Libre'}</Text>
                      </TouchableOpacity>
                    );
                  })}
                </View>

                {numero !== null && (
                  <View style={estilos.resumen}>
                    <Text style={estilos.fuerte}>Revise su reserva</Text>
                    <Text style={estilos.texto}>{especialidad.nombre} · {turno.nombreMedico}</Text>
                    <Text style={estilos.texto}>{fechaVisible(turno.fecha)}</Text>
                    <Text style={estilos.texto}>Número {numero} · Hora estimada: ~{horaEstimada(turno, numero)}</Text>
                    <Text style={estilos.texto}>Consultorio: {turno.consultorio}</Text>
                    <Text style={estilos.texto}>La disponibilidad se vuelve a verificar al confirmar.</Text>
                  </View>
                )}
                <Button onPress={confirmar} disabled={enviando || numero === null} accessibilityRole="button">
                  {enviando ? 'Reservando…' : 'Confirmar reserva'}
                </Button>
                <Button variant="secondary" onPress={flujo.cambiarFecha} disabled={enviando} accessibilityRole="button">Cambiar fecha</Button>
              </View>
            )}
          </>
        )}
      </View>
    </ScrollView>
  );
}
