# Pasta: `frontend/src/app/schedules/[id]`

## Visão Geral
Rota dinâmica protegida `/schedules/[id]` para acompanhamento, edição e transição de ciclo de vida de um agendamento específico.

## Arquivos (1 arquivo)
- **`page.tsx`**: Exibe detalhes do agendamento (`schedulesApi.getById`), histórico de timestamps (`assignedAt`, `enRouteAt`, `completedAt`, `cancelledAt`), registros vinculados e ações de fluxo (confirmar, iniciar rota, concluir, reagendar ou cancelar com motivo).
