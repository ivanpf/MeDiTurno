/**
 * Modal component con backdrop y animación.
 */

import React, { useEffect, useRef } from 'react';
import { View, Text, StyleSheet, Modal as RNModal, TouchableWithoutFeedback, Animated, Platform } from 'react-native';

const estilos = StyleSheet.create({
  overlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.5)',
    justifyContent: 'center',
    alignItems: 'center',
    padding: 16,
  },
  contenido: {
    backgroundColor: '#fff',
    borderRadius: 14,
    width: '100%',
    maxWidth: 400,
    maxHeight: '90%',
    overflow: 'hidden',
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: 20,
    borderBottomWidth: 1,
    borderBottomColor: '#d3dce8',
  },
  titulo: {
    fontSize: 18,
    fontWeight: '600',
    color: '#12366b',
  },
  cerrar: {
    padding: 4,
  },
  cuerpo: {
    padding: 20,
    maxHeight: 300,
  },
  footer: {
    flexDirection: 'row',
    justifyContent: 'flex-end',
    gap: 12,
    padding: 20,
    borderTopWidth: 1,
    borderTopColor: '#d3dce8',
  },
});

/**
 * @param {Object} props
 * @param {boolean} props.visible
 * @param {() => void} props.onClose
 * @param {string} props.titulo
 * @param {React.ReactNode} props.children
 * @param {React.ReactNode} [props.footer]
 * @param {boolean} [props.cerrarAlTocarFondo=true]
 */
export function Modal({ visible, onClose, titulo, children, footer, cerrarAlTocarFondo = true }) {
  const fadeAnim = useRef(new Animated.Value(0)).current;
  const slideAnim = useRef(new Animated.Value(50)).current;

  useEffect(() => {
    if (visible) {
      Animated.parallel([
        Animated.timing(fadeAnim, { toValue: 1, duration: 200, useNativeDriver: true }),
        Animated.timing(slideAnim, { toValue: 0, duration: 200, useNativeDriver: true }),
      ]).start();
    } else {
      Animated.parallel([
        Animated.timing(fadeAnim, { toValue: 0, duration: 150, useNativeDriver: true }),
        Animated.timing(slideAnim, { toValue: 50, duration: 150, useNativeDriver: true }),
      ]).start(() => {});
    }
  }, [visible, fadeAnim, slideAnim]);

  if (!visible) return null;

  const animStyles = {
    opacity: fadeAnim,
    transform: [{ translateY: slideAnim }],
  };

  return (
    <RNModal visible={visible} transparent animationType="none" onRequestClose={onClose}>
      <Animated.View style={[estilos.overlay, { opacity: fadeAnim }]}>
        <TouchableWithoutFeedback onPress={cerrarAlTocarFondo ? onClose : undefined}>
          <View style={{ flex: 1 }} />
        </TouchableWithoutFeedback>
        <Animated.View style={[estilos.contenido, animStyles]}>
          <View style={estilos.header}>
            <Text style={estilos.titulo}>{titulo}</Text>
            <TouchableWithoutFeedback onPress={onClose}>
              <View style={estilos.cerrar}>
                <Text style={{ fontSize: 24, color: '#5b6b7c', lineHeight: 24 }}>✕</Text>
              </View>
            </TouchableWithoutFeedback>
          </View>
          <View style={estilos.cuerpo}>{children}</View>
          {footer && <View style={estilos.footer}>{footer}</View>}
        </Animated.View>
      </Animated.View>
    </RNModal>
  );
}