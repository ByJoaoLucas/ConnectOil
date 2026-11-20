# Pasta: `frontend/src/app/incidents`

## Visão Geral
Rota `/incidents` para registro, acompanhamento e resolução de ocorrências e problemas operacionais nos pontos de coleta ou rotas.

## Arquivos (1 arquivo)
- **`page.tsx`**: Lista incidentes (`incidentsApi`), filtra por severidade (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`) e status (`OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`), permite abrir novos chamados vinculados a pontos/agendamentos e registrar notas de resolução (`incidentsApi.resolve`).
