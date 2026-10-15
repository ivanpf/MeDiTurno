import React from 'react';
import { AuthProvider } from '../shared/src/auth/AuthContext';

export function Providers({ children }) {
  return <AuthProvider>{children}</AuthProvider>;
}
