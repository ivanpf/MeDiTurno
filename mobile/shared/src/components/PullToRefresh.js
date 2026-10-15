/**
 * PullToRefresh - Wrapper para FlatList/SectionList con pull to refresh.
 */

import React from 'react';
import { RefreshControl, View } from 'react-native';

/**
 * @param {Object} props
 * @param {boolean} props.refreshing
 * @param {() => void} props.onRefresh
 * @param {React.ReactElement} props.children - FlatList o SectionList
 * @param {string} [props.tintColor='#1d4f91']
 * @param {string} [props.title='Actualizando...']
 */
export function PullToRefresh({ refreshing, onRefresh, children, tintColor = '#1d4f91', title = 'Actualizando...' }) {
  // Clonar el child y agregar refreshControl
  return React.cloneElement(children, {
    refreshControl: (
      <RefreshControl
        refreshing={refreshing}
        onRefresh={onRefresh}
        tintColor={tintColor}
        title={title}
        colors={[tintColor]}
        progressBackgroundColor="#fff"
      />
    ),
  });
}