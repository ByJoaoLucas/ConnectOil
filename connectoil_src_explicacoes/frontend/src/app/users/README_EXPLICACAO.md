# Pasta: `frontend/src/app/users`

## Visão Geral
Rota administrativa `/users` para gestão de contas, papéis (roles) e ativação/desativação de usuários do sistema.

## Arquivos (2 arquivos)
- **`layout.tsx`**: Guarda de autenticação e autorização restrita ao perfil `ADMIN`.
- **`page.tsx`**: Lista usuários (`usersApi`), permite filtrar/buscar, criar e editar contas (`ADMIN`, `COLLECTOR`, `CLIENT`, `ESTABLISHMENT`), validar CPF e ativar ou remover/desativar usuários.
