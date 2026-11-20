# Pasta: `frontend/src/app/collection-points/[id]`

## Visão Geral
Rota dinâmica protegida `/collection-points/[id]` com o detalhamento completo de um ponto de coleta específico.

## Arquivos (1 arquivo)
- **`page.tsx`**: Carrega os dados do ponto (`collectionPointsApi.getById`), seu histórico de agendamentos (`schedulesApi.getByPoint`) e registros de coleta (`recordsApi.getByPoint`), exibindo endereço, coordenadas, contatos, capacidade atual e ações de gerenciamento/agendamento.
