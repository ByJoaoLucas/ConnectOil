# Pasta: `frontend/src/app/collector`

## Visão Geral
Rota `/collector` dedicada ao painel operacional diário do coletor em campo.

## Arquivos (1 arquivo)
- **`page.tsx`**: Exibe os agendamentos atribuídos ao coletor autenticado com atualização automática a cada 30 segundos (`COLLECTOR_DASHBOARD_REFRESH_INTERVAL = 30000`), alternância entre visualização em lista (`list`) e mapa (`map`), filtros por status e atalhos para iniciar rota ou concluir a coleta.
