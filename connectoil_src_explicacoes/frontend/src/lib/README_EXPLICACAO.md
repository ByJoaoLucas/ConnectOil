# Pasta: `frontend/src/lib`

## Visão Geral
Módulos utilitários, configuração de comunicação HTTP e clientes tipados de integração com a API REST do backend.

## Arquivos (1 arquivo)
- **`api.ts`**: Configura a instância base do `axios` com `NEXT_PUBLIC_API_URL` no navegador e `NEXT_PUBLIC_INTERNAL_API_URL` no SSR dentro do Docker, sempre usando um único prefixo `/api`. O cliente anexa automaticamente o cabeçalho `Authorization`, renova tokens expirados em respostas `401` via `/auth/refresh` e exporta os clientes de domínio: `authApi`, `usersApi`, `collectionPointsApi`, `schedulesApi`, `recordsApi`, `productsApi`, `exchangeItemsApi`, `incidentsApi`, `notificationsApi`, `collectorCoverageApi` e `systemConfigApi`.
