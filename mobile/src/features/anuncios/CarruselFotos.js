// Carrusel de fotos de la cartelera (CU-6): pasan solas cada pocos segundos, se pueden deslizar con el dedo
// y se elige una con los puntos de abajo. Se hace con ScrollView paginado, sin librerías nuevas.
import React, { useEffect, useRef, useState } from 'react';
import { Image, ScrollView, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { formatearFecha } from '../../../shared/src/utils/date';
import { COLORS } from '../../../shared/src/theme/colors';
import { indiceCircular } from './cartelera';

const INTERVALO_MS = 5000;

const estilos = StyleSheet.create({
  marco: { borderRadius: 12, overflow: 'hidden', backgroundColor: COLORS.azulClaro,
    borderWidth: 1, borderColor: COLORS.borde },
  diapositiva: { aspectRatio: 16 / 9 },
  foto: { width: '100%', height: '100%' },
  sinFoto: { flex: 1, alignItems: 'center', justifyContent: 'center', padding: 24 },
  sinFotoTexto: { fontSize: 15, color: COLORS.gris, textAlign: 'center' },
  pie: { position: 'absolute', left: 0, right: 0, bottom: 0, backgroundColor: COLORS.veloAzul,
    paddingVertical: 10, paddingHorizontal: 14 },
  pieTitulo: { fontSize: 16, fontWeight: '700', color: COLORS.blanco },
  pieFecha: { fontSize: 13, color: COLORS.azulClaro, marginTop: 2 },
  flecha: { position: 'absolute', top: '50%', marginTop: -20, width: 40, height: 40, borderRadius: 20,
    backgroundColor: COLORS.veloAzul, alignItems: 'center', justifyContent: 'center' },
  flechaIzquierda: { left: 8 },
  flechaDerecha: { right: 8 },
  flechaTexto: { fontSize: 26, lineHeight: 28, color: COLORS.blanco, fontWeight: '700' },
  puntos: { flexDirection: 'row', justifyContent: 'center', gap: 4, marginTop: 6 },
  // Área táctil de 24 px alrededor de un punto chico.
  toquePunto: { width: 24, height: 24, alignItems: 'center', justifyContent: 'center' },
  punto: { width: 8, height: 8, borderRadius: 4, backgroundColor: COLORS.borde },
  puntoActual: { width: 20, backgroundColor: COLORS.mostaza },
});

function Diapositiva({ foto, ancho }) {
  const [fallo, setFallo] = useState(false);
  return (
    <View style={[estilos.diapositiva, { width: ancho }]} accessible accessibilityRole="image"
      accessibilityLabel={`${foto.descripcion}. ${foto.titulo}`}>
      {fallo ? (
        <View style={estilos.sinFoto}>
          <Text style={estilos.sinFotoTexto}>No se pudo cargar la imagen.</Text>
        </View>
      ) : (
        <Image source={{ uri: foto.uri, headers: foto.headers }} style={estilos.foto} resizeMode="cover" onError={() => setFallo(true)} />
      )}
      <View style={estilos.pie}>
        <Text style={estilos.pieTitulo} numberOfLines={2}>{foto.titulo}</Text>
        <Text style={estilos.pieFecha}>Publicado el {formatearFecha(foto.fechaPublicacion)}</Text>
      </View>
    </View>
  );
}

export function CarruselFotos({ fotos }) {
  const [ancho, setAncho] = useState(0);
  const [indice, setIndice] = useState(0);
  // Mientras el paciente desliza con el dedo, el carrusel no avanza solo.
  const [tocando, setTocando] = useState(false);
  const scroll = useRef(null);
  const total = fotos.length;

  // Si cambia la cantidad de fotos (al recargar), el índice no puede quedar afuera.
  useEffect(() => { setIndice((i) => indiceCircular(i, total)); }, [total]);

  // Cada cambio de foto reinicia la espera, así una foto elegida a mano se ve el tiempo completo.
  useEffect(() => {
    if (tocando || total < 2) return undefined;
    const espera = setTimeout(() => setIndice((i) => indiceCircular(i + 1, total)), INTERVALO_MS);
    return () => clearTimeout(espera);
  }, [indice, tocando, total]);

  useEffect(() => {
    if (ancho) scroll.current?.scrollTo({ x: indice * ancho, animated: true });
  }, [indice, ancho]);

  const alTerminarDeslizar = (e) => {
    setTocando(false);
    if (ancho) setIndice(indiceCircular(Math.round(e.nativeEvent.contentOffset.x / ancho), total));
  };

  if (total === 0) return null;

  return (
    <View>
      <View style={estilos.marco} onLayout={(e) => setAncho(e.nativeEvent.layout.width - 2)}>
        {ancho > 0 && (
          <ScrollView ref={scroll} horizontal pagingEnabled showsHorizontalScrollIndicator={false}
            onScrollBeginDrag={() => setTocando(true)} onScrollEndDrag={() => setTocando(false)}
            onMomentumScrollEnd={alTerminarDeslizar}>
            {fotos.map((foto) => <Diapositiva key={foto.clave} foto={foto} ancho={ancho} />)}
          </ScrollView>
        )}
        {ancho > 0 && total > 1 && (
          <>
            <TouchableOpacity style={[estilos.flecha, estilos.flechaIzquierda]} accessibilityRole="button"
              accessibilityLabel="Foto anterior" onPress={() => setIndice((i) => indiceCircular(i - 1, total))}>
              <Text style={estilos.flechaTexto}>‹</Text>
            </TouchableOpacity>
            <TouchableOpacity style={[estilos.flecha, estilos.flechaDerecha]} accessibilityRole="button"
              accessibilityLabel="Foto siguiente" onPress={() => setIndice((i) => indiceCircular(i + 1, total))}>
              <Text style={estilos.flechaTexto}>›</Text>
            </TouchableOpacity>
          </>
        )}
      </View>
      {total > 1 && (
        <View style={estilos.puntos}>
          {fotos.map((foto, i) => (
            <TouchableOpacity key={foto.clave} style={estilos.toquePunto} onPress={() => setIndice(i)}
              accessibilityRole="button" accessibilityState={{ selected: i === indice }}
              accessibilityLabel={`Foto ${i + 1} de ${total}: ${foto.titulo}`}>
              <View style={[estilos.punto, i === indice && estilos.puntoActual]} />
            </TouchableOpacity>
          ))}
        </View>
      )}
    </View>
  );
}
