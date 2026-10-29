// CU-6 Consulta de la cartelera de novedades: carrusel con las fotos de los anuncios y, debajo, cada anuncio.
// La pantalla describe qué mostrar; useCarteleraNovedades maneja el estado y la fuente de datos.
import React from 'react';
import { ActivityIndicator, RefreshControl, ScrollView, StyleSheet, Text, View } from 'react-native';
import { Button } from '../../shared/src/components/Button';
import { ErrorBanner } from '../../shared/src/components/ErrorBanner';
import { COLORS } from '../../shared/src/theme/colors';
import { formatearFecha } from '../../shared/src/utils/date';
import { CarruselFotos } from '../../src/features/anuncios/CarruselFotos';
import { useCarteleraNovedades } from '../../src/features/anuncios/useCartelera';

const estilos = StyleSheet.create({
  contenedor: { flex: 1, backgroundColor: COLORS.fondo },
  contenido: { padding: 16, paddingBottom: 40 },
  panel: { backgroundColor: COLORS.blanco, borderColor: COLORS.borde, borderWidth: 1,
    borderRadius: 12, padding: 20, gap: 16 },
  titulo: { fontSize: 24, fontWeight: '700', color: COLORS.azulOscuro },
  subrayado: { width: 40, height: 3, borderRadius: 2, backgroundColor: COLORS.mostaza, marginTop: -8 },
  texto: { fontSize: 15, lineHeight: 22, color: COLORS.gris },
  carga: { flexDirection: 'row', alignItems: 'center', gap: 12 },
  aviso: { backgroundColor: COLORS.mostazaClaro, borderLeftWidth: 4, borderLeftColor: COLORS.mostaza,
    borderRadius: 8, padding: 16 },
  avisoTexto: { fontSize: 15, lineHeight: 22, color: COLORS.texto },

  // Cada anuncio, con el borde azul a la izquierda.
  anuncio: { backgroundColor: COLORS.fondo, borderWidth: 1, borderColor: COLORS.borde, borderLeftWidth: 4,
    borderLeftColor: COLORS.azul, borderRadius: 12, padding: 16, gap: 6 },
  tituloAnuncio: { fontSize: 17, fontWeight: '700', color: COLORS.azulOscuro },
  datosAnuncio: { fontSize: 13, color: COLORS.gris },
  contenidoAnuncio: { fontSize: 15, lineHeight: 22, color: COLORS.texto, marginTop: 4 },
});

function Anuncio({ anuncio }) {
  return (
    <View style={estilos.anuncio}>
      <Text style={estilos.tituloAnuncio} accessibilityRole="header">{anuncio.titulo}</Text>
      <Text style={estilos.datosAnuncio}>
        Publicado el {formatearFecha(anuncio.fechaPublicacion)}
        {anuncio.fechaFin ? ` · Vigente hasta el ${formatearFecha(`${anuncio.fechaFin}T00:00:00`)}` : ''}
      </Text>
      <Text style={estilos.contenidoAnuncio}>{anuncio.contenido}</Text>
    </View>
  );
}

export default function CarteleraScreen() {
  const { anuncios, fotos, cargando, error, recargar, limpiarError } = useCarteleraNovedades();
  const cargado = !cargando || anuncios.length > 0;

  return (
    <ScrollView style={estilos.contenedor} contentContainerStyle={estilos.contenido}
      refreshControl={<RefreshControl refreshing={cargando && anuncios.length > 0} onRefresh={recargar}
        colors={[COLORS.azul]} tintColor={COLORS.azul} />}>
      <View style={estilos.panel}>
        <Text style={estilos.titulo} accessibilityRole="header">Cartelera de novedades</Text>
        <View style={estilos.subrayado} />

        {error && <ErrorBanner mensaje={error.mensaje} detalles={error.detalles} onDismiss={limpiarError} />}

        {!cargado && (
          <View style={estilos.carga} accessibilityLiveRegion="polite">
            <ActivityIndicator color={COLORS.azul} accessibilityLabel="Cargando la cartelera" />
            <Text style={estilos.texto}>Cargando la cartelera…</Text>
          </View>
        )}

        {cargado && error && anuncios.length === 0 && (
          <Button variant="secondary" onPress={recargar} accessibilityRole="button">Reintentar</Button>
        )}

        {cargado && !error && anuncios.length === 0 && (
          <View style={estilos.aviso} accessibilityLiveRegion="polite">
            <Text style={estilos.avisoTexto}>No hay anuncios publicados en este momento.</Text>
          </View>
        )}

        {fotos.length > 0 && <CarruselFotos fotos={fotos} />}

        {anuncios.map((anuncio) => <Anuncio key={anuncio.id} anuncio={anuncio} />)}
      </View>
    </ScrollView>
  );
}
