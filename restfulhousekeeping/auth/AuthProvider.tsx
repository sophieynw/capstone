import React, { useCallback, useEffect, useRef, useState } from 'react';
import * as SecureStore from 'expo-secure-store';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

import { AuthContext } from './AuthContext';
import { setUnauthorizedHandler } from '@/api/apiClient';
import { User } from '@/types/entityTypes';
import { getUserById } from '@/api/usersApi';

interface Props {
  children: React.ReactNode;
}

const TOKEN_KEY = 'token';
const USER_KEY = 'user';
const MAX_TIMEOUT_MS = 2_147_483_647;

function getTokenExpiration(token: string): number | null {
  try {
    const payload = token.split('.')[1];
    if (!payload) return null;

    const normalized = payload.replace(/-/g, '+').replace(/_/g, '/');
    const padded = normalized.padEnd(
      normalized.length + ((4 - (normalized.length % 4)) % 4),
      '=',
    );
    const decoded = JSON.parse(atob(padded)) as { exp?: unknown };

    return typeof decoded.exp === 'number' ? decoded.exp * 1000 : null;
  } catch {
    // If this is an opaque or malformed token, let the server validate it.
    return null;
  }
}

export function AuthProvider({ children }: Props) {
  const [token, setToken] = useState<string | null>(null);
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [queryClient, setQueryClient] = useState(() => new QueryClient());
  const activeQueryClient = useRef(queryClient);
  const logoutInFlight = useRef<Promise<void> | null>(null);

  const logout = useCallback(() => {
    if (logoutInFlight.current) return logoutInFlight.current;

    // Update React state first so navigation immediately switches to login.
    setToken(null);
    setUser(null);

    const previousClient = activeQueryClient.current;
    const freshClient = new QueryClient();
    activeQueryClient.current = freshClient;
    setQueryClient(freshClient);
    // Late mutation callbacks still reference the previous client's cache.
    previousClient.clear();

    const cleanup = Promise.all([
      SecureStore.deleteItemAsync(TOKEN_KEY),
      SecureStore.deleteItemAsync(USER_KEY),
    ])
      .then(() => undefined)
      .finally(() => {
        logoutInFlight.current = null;
      });
    logoutInFlight.current = cleanup;
    return cleanup;
  }, []);

  // Restore login when app starts
  useEffect(() => {
    const restoreSession = async () => {
      try {
        const [savedToken, savedUser] = await Promise.all([
          SecureStore.getItemAsync(TOKEN_KEY),
          SecureStore.getItemAsync(USER_KEY),
        ]);

        const expiration = savedToken ? getTokenExpiration(savedToken) : null;

        if (
          !savedToken ||
          !savedUser ||
          (expiration !== null && expiration <= Date.now())
        ) {
          await logout();
          return;
        }

        setToken(savedToken);
        setUser(JSON.parse(savedUser) as User);
      } catch (error) {
        console.log('Failed restoring session', error);
        await logout();
      } finally {
        setIsLoading(false);
      }
    };

    void restoreSession();
  }, [logout]);

  // Any protected endpoint can tell us that a token is invalid or revoked.
  useEffect(() => {
    setUnauthorizedHandler(logout);
    return () => setUnauthorizedHandler(null);
  }, [logout]);

  // Log out at the JWT expiry time even if the user makes no API request.
  useEffect(() => {
    if (!token) return;

    const expiration = getTokenExpiration(token);
    if (expiration === null) return;

    let timeout: ReturnType<typeof setTimeout> | undefined;
    const scheduleExpirationCheck = () => {
      const remaining = expiration - Date.now();
      if (remaining <= 0) {
        void logout();
        return;
      }

      timeout = setTimeout(
        scheduleExpirationCheck,
        Math.min(remaining, MAX_TIMEOUT_MS),
      );
    };

    scheduleExpirationCheck();
    return () => {
      if (timeout) clearTimeout(timeout);
    };
  }, [token, logout]);

  const login = async (token: string, user: User) => {
    await logoutInFlight.current;
    await Promise.all([
      SecureStore.setItemAsync(TOKEN_KEY, token),
      SecureStore.setItemAsync(USER_KEY, JSON.stringify(user)),
    ]);

    // Only mount authenticated screens once requests can read the saved token.
    setToken(token);
    setUser(user);
  };

  const update = async (updatedUser: User) => {
    if (activeQueryClient.current !== queryClient) return;
    await SecureStore.setItemAsync(USER_KEY, JSON.stringify(updatedUser));
    if (activeQueryClient.current === queryClient) setUser(updatedUser);
  };

  const userId = user?.id;

  const refreshUser = useCallback(async () => {
    if (!userId) return;
    try {
      const fresh = await getUserById(userId);
      await update(fresh);
    } catch (error) {
      console.log('Failed refreshing user', error);
    }
  }, [userId, update]);

  return (
    <AuthContext.Provider
      value={{
        token,
        user,
        isLoading,
        login,
        logout,
        update,
        refreshUser
      }}
    >
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    </AuthContext.Provider>
  );
}
