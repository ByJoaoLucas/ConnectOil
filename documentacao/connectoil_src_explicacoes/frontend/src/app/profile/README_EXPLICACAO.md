# Pasta: `frontend/src/app/profile`

## Visão Geral
Rota protegida `/profile` para autoatendimento do usuário autenticado (dados cadastrais e segurança da conta).

## Arquivos (2 arquivos)
- **`layout.tsx`**: Guarda de autenticação e aplicação do shell `<Layout>`.
- **`page.tsx`**: Exibe informações do perfil logado e disponibiliza dois formulários validados com Zod: atualização de dados pessoais (`authApi.updateProfile`) e alteração de senha (`authApi.changePassword`).
