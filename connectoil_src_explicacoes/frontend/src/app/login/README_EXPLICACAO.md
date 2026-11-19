# Pasta: `frontend/src/app/login`

## Visão Geral
Rota pública `/login` para autenticação de usuários no sistema.

## Arquivos (1 arquivo)
- **`page.tsx`**: Formulário de login validado com `react-hook-form` + `zod` (e-mail válido e senha com mínimo de 6 caracteres), alternância de visibilidade de senha, chamada a `login()` do `AuthContext`, feedback via `toast` e redirecionamento para `/dashboard`.
