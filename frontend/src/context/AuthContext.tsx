"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from "react";
import type { AuthResponse, LoginRequest, RegisterRequest } from "@/lib/types";
import { api } from "@/lib/api";

interface AuthState {
  user: AuthResponse | null;
  loading: boolean;
}

interface AuthContextValue extends AuthState {
  login: (req: LoginRequest) => Promise<void>;
  register: (req: RegisterRequest) => Promise<void>;
  logout: () => void;
  isAdmin: boolean;
}

const AuthContext = createContext<AuthContextValue | null>(null);

const STORAGE_KEY = "retail_auth";

function isTokenExpired(token: string): boolean {
  try {
    const payload = JSON.parse(atob(token.split(".")[1]));
    return Date.now() >= payload.exp * 1000;
  } catch {
    return true;
  }
}

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [state, setState] = useState<AuthState>({ user: null, loading: true });

  useEffect(() => {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored) {
      try {
        const parsed = JSON.parse(stored);
        if (parsed.token && isTokenExpired(parsed.token)) {
          localStorage.removeItem(STORAGE_KEY);
          setState({ user: null, loading: false });
        } else {
          setState({ user: parsed, loading: false });
        }
      } catch {
        localStorage.removeItem(STORAGE_KEY);
        setState({ user: null, loading: false });
      }
    } else {
      setState({ user: null, loading: false });
    }
  }, []);

  const login = useCallback(async (req: LoginRequest) => {
    const res = await api.post<AuthResponse>("/api/auth/login", req);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(res));
    setState({ user: res, loading: false });
  }, []);

  const register = useCallback(async (req: RegisterRequest) => {
    const res = await api.post<AuthResponse>("/api/auth/register", req);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(res));
    setState({ user: res, loading: false });
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem(STORAGE_KEY);
    setState({ user: null, loading: false });
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      ...state,
      login,
      register,
      logout,
      isAdmin: state.user?.role === "ADMIN",
    }),
    [state, login, register, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
