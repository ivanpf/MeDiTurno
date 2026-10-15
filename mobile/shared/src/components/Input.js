/**
 * Input component con label, error y secureTextEntry.
 */

import React, { forwardRef, useImperativeHandle } from 'react';
import { TextInput, View, Text, StyleSheet } from 'react-native';

const estilos = StyleSheet.create({
  contenedor: {
    width: '100%',
    marginBottom: 16,
  },
  label: {
    fontSize: 13,
    fontWeight: '600',
    color: '#5b6b7c',
    marginBottom: 8,
  },
  inputWrapper: {
    position: 'relative',
    flexDirection: 'row',
    alignItems: 'center',
  },
  input: {
    flex: 1,
    height: 48,
    backgroundColor: '#fff',
    borderWidth: 1,
    borderColor: '#d3dce8',
    borderRadius: 8,
    paddingHorizontal: 16,
    fontSize: 15,
    color: '#1c2733',
  },
  inputFocused: {
    borderColor: '#1d4f91',
    borderWidth: 2,
  },
  inputError: {
    borderColor: '#b42318',
  },
  icon: {
    position: 'absolute',
    right: 16,
    zIndex: 1,
    fontSize: 13,
    fontWeight: '600',
    color: '#1d4f91',
  },
  errorTexto: {
    marginTop: 6,
    fontSize: 13,
    color: '#b42318',
  },
});

/**
 * @param {Object} props
 * @param {string} [props.label]
 * @param {string} [props.placeholder]
 * @param {string} [props.value]
 * @param {(value: string) => void} [props.onChangeText]
 * @param {string} [props.error]
 * @param {boolean} [props.secureTextEntry=false]
 * @param {boolean} [props.disabled=false]
 * @param {'email'|'tel'|'text'|'password'} [props.keyboardType='default']
 * @param {string} [props.autoComplete]
 * @param {number} [props.maxLength]
 * @param {boolean} [props.required=false]
 * @param {string} [props.className]
 * @param {Object} [props.style]
 */
export const Input = forwardRef((props, ref) => {
  const {
    label,
    placeholder,
    value = '',
    onChangeText,
    error,
    secureTextEntry = false,
    disabled = false,
    keyboardType = 'default',
    autoComplete,
    maxLength,
    required = false,
    className = '',
    style,
    ...rest
  } = props;

  const [focused, setFocused] = React.useState(false);
  const [mostrarPassword, setMostrarPassword] = React.useState(false);

  useImperativeHandle(ref, () => ({
    focus: () => textInputRef?.current?.focus(),
    blur: () => textInputRef?.current?.blur(),
  }));

  const textInputRef = React.useRef(null);

  const handleChange = (text) => {
    if (maxLength && text.length > maxLength) return;
    onChangeText?.(text);
  };

  return (
    <View style={[estilos.contenedor, style]} className={className}>
      {label && (
        <Text style={estilos.label}>
          {label} {required && <Text style={{ color: '#b42318' }}>*</Text>}
        </Text>
      )}
      <View style={estilos.inputWrapper}>
        <TextInput
          ref={textInputRef}
          style={[
            estilos.input,
            focused && estilos.inputFocused,
            error && estilos.inputError,
            disabled && { backgroundColor: '#f4f7fb' },
          ]}
          placeholder={placeholder}
          value={value}
          onChangeText={handleChange}
          onFocus={() => setFocused(true)}
          onBlur={() => setFocused(false)}
          secureTextEntry={secureTextEntry && !mostrarPassword}
          disabled={disabled}
          keyboardType={keyboardType}
          autoComplete={autoComplete}
          maxLength={maxLength}
          {...rest}
        />
        {secureTextEntry && (
          <Text
            style={estilos.icon}
            onPress={() => setMostrarPassword((prev) => !prev)}
            accessible
            accessibilityLabel={mostrarPassword ? 'Ocultar contraseña' : 'Mostrar contraseña'}
          >
            {mostrarPassword ? 'Ocultar' : 'Ver'}
          </Text>
        )}
      </View>
      {error && <Text style={estilos.errorTexto}>{error}</Text>}
    </View>
  );
});

Input.displayName = 'Input';