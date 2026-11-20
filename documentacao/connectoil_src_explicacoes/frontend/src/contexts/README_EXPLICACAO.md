# Pasta: `frontend/src/contexts`

## Visão Geral
Provedores de contexto React para estado global da aplicação.

## Arquivos (1 arquivo)
- **`AuthContext.tsx`**: Gerencia o ciclo de vida da sessão JWT no cliente (`user`, `token`, `refreshToken`, `isLoading`, `isAuthenticated`). Persiste credenciais no `localStorage`, valida a sessão inicial via `/auth/me`, realiza renovação automática de token (`refreshAccessToken`) e expõe `login`, `register`, `logout` e `updateUser`.
