# Pasta: `frontend/src/app/schedules`

## Visão Geral
Rota protegida `/schedules` para gestão da agenda de coletas entre estabelecimentos/pontos de coleta e coletores.

## Arquivos (2 arquivos)
- **`layout.tsx`**: Guarda de autenticação e encapsulamento no componente `<Layout>`.
- **`page.tsx`**: Lista agendamentos (`schedulesApi`), filtra por status (`PENDING`, `CONFIRMED`, `ASSIGNED`, `EN_ROUTE`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`, `RESCHEDULED`), permite criar novos agendamentos (`scheduleSchema`) vinculando ponto de coleta, coletor, data, janela de horário e volume estimado, além de executar ações rápidas de confirmação/cancelamento.
