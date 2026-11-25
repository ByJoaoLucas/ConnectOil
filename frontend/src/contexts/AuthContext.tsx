'use client';

import React, {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useState,
  ReactNode,
} from 'react';
import { api, authApi } from '@/lib/api';
import { User, AuthResponse, RegisterData } from '@/types';

interface AuthContextType {
  user: User | null;
  token: string | null;
  refreshToken: string | null;
  isLoading: boolean;
  isAuthenticated: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (data: RegisterData) => Promise<void>;
  logout: () => void;
  refreshAccessToken: () => Promise<void>;
  updateUser: (user: Partial<User>) => void;
  deleteAccount: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [refreshToken, setRefreshToken] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  const isAuthenticated = !!token && !!user;

  const logout = useCallback(() => {
    setUser(null);
    setToken(null);
    setRefreshToken(null);
    if (typeof window !== 'undefined') {
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('user');
    }
    delete api.defaults.headers.common['Authorization'];
  }, []);

  const deleteAccount = useCallback(async () => {
    await authApi.deleteAccount();
    logout();
  }, [logout]);

  const refreshAccessToken = useCallback(async () => {
    const currentRefreshToken = typeof window !== 'undefined'
      ? localStorage.getItem('refreshToken') || refreshToken
      : refreshToken;

    if (!currentRefreshToken) {
      throw new Error('No refresh token');
    }

    const response = await api.post<AuthResponse>('/auth/refresh', null, {
      params: { refreshToken: currentRefreshToken },
    });

    const { accessToken, refreshToken: newRefreshToken, user: userData } = response.data;

    setToken(accessToken);
    setRefreshToken(newRefreshToken);
    setUser(userData);

    if (typeof window !== 'undefined') {
      localStorage.setItem('accessToken', accessToken);
      localStorage.setItem('refreshToken', newRefreshToken);
      localStorage.setItem('user', JSON.stringify(userData));
    }

    api.defaults.headers.common['Authorization'] = `Bearer ${accessToken}`;
  }, [refreshToken]);

  useEffect(() => {
    const initAuth = async () => {
      if (typeof window === 'undefined') {
        setIsLoading(false);
        return;
      }

      const storedToken = localStorage.getItem('accessToken');
      const storedRefreshToken = localStorage.getItem('refreshToken');
      const storedUser = localStorage.getItem('user');

      if (storedToken && storedRefreshToken && storedUser) {
        try {
          setToken(storedToken);
          setRefreshToken(storedRefreshToken);
          setUser(JSON.parse(storedUser));

          api.defaults.headers.common['Authorization'] = `Bearer ${storedToken}`;
          await api.get('/auth/me');
        } catch {
          try {
            await refreshAccessToken();
          } catch {
            logout();
          }
        }
      }

      setIsLoading(false);
    };

    initAuth();
  }, [logout, refreshAccessToken]);

  const login = async (email: string, password: string) => {
    const response = await api.post<AuthResponse>('/auth/login', { email, password });
    const { accessToken, refreshToken: newRefreshToken, user: userData } = response.data;

    setToken(accessToken);
    setRefreshToken(newRefreshToken);
    setUser(userData);

    localStorage.setItem('accessToken', accessToken);
    localStorage.setItem('refreshToken', newRefreshToken);
    localStorage.setItem('user', JSON.stringify(userData));

    api.defaults.headers.common['Authorization'] = `Bearer ${accessToken}`;
  };

  const register = async (data: RegisterData) => {
    const response = await api.post<AuthResponse>('/auth/register', data);
    const { accessToken, refreshToken: newRefreshToken, user: userData } = response.data;

    setToken(accessToken);
    setRefreshToken(newRefreshToken);
    setUser(userData);

    localStorage.setItem('accessToken', accessToken);
    localStorage.setItem('refreshToken', newRefreshToken);
    localStorage.setItem('user', JSON.stringify(userData));

    api.defaults.headers.common['Authorization'] = `Bearer ${accessToken}`;
  };

  const updateUser = (userData: Partial<User>) => {
    if (user) {
      const updatedUser = { ...user, ...userData };
      setUser(updatedUser);
      if (typeof window !== 'undefined') {
        localStorage.setItem('user', JSON.stringify(updatedUser));
      }
    }
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        refreshToken,
        isLoading,
        isAuthenticated,
        login,
        register,
        logout,
        refreshAccessToken,
        updateUser,
        deleteAccount,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
