# Pasta: `frontend/src/lib`

## Visão Geral
Módulos utilitários, configuração de comunicação HTTP e clientes tipados de integração com a API REST do backend.

## Arquivos (1 arquivo)
- **`api.ts`**: Configura a instância base do `axios` (`http://localhost:8080/api/api` no navegador e `NEXT_PUBLIC_API_URL` no SSR), anexa automaticamente o cabeçalho `Authorization: Bearer <accessToken>` via interceptor de requisição, renova tokens expirados em respostas `401` via `/auth/refresh` e exporta os clientes de domínio: `authApi`, `usersApi`, `collectionPointsApi`, `schedulesApi`, `recordsApi`, `productsApi`, `exchangeItemsApi`, `incidentsApi`, `notificationsApi`, `collectorCoverageApi` e `systemConfigApi`.
