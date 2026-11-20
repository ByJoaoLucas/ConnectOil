# Pasta: `frontend/src/app/dashboard`

## Visão Geral
Rota protegida `/dashboard` que exibe a visão geral operacional do sistema logo após o login.

## Arquivos (2 arquivos)
- **`layout.tsx`**: Guarda de autenticação (`useAuth`). Redireciona usuários não autenticados para `/login` e envolve o conteúdo no shell principal `<Layout>`.
- **`page.tsx`**: Consulta dados resumidos via `collectionPointsApi`, `schedulesApi` e `recordsApi` e renderiza cartões de métricas (`StatsCard`) com totais de pontos de coleta, agendamentos, coletas realizadas e volume acumulado.
