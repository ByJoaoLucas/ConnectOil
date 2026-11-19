# Pasta: `frontend/src/app/collector/complete/[id]`

## Visão Geral
Rota dinâmica `/collector/complete/[id]` para o fluxo de conclusão em campo de um agendamento pelo coletor.

## Arquivos (1 arquivo)
- **`page.tsx`**: Carrega o agendamento e o ponto de coleta correspondente, valida o formulário técnico de coleta (volume coletado em litros, tipo de óleo, recipientes, qualidade, temperatura, pH, contaminação, observações e geolocalização) e registra a conclusão integrando `recordsApi` e `schedulesApi`.
