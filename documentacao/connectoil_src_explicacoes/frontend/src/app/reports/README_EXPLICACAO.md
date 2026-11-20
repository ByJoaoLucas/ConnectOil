# Pasta: `frontend/src/app/reports`

## Visão Geral
Rota administrativa `/reports` voltada à análise gerencial e indicadores de desempenho.

## Arquivos (2 arquivos)
- **`layout.tsx`**: Guarda de autenticação e autorização restrita a `user?.role === 'ADMIN'` (redireciona outros perfis para `/dashboard`).
- **`page.tsx`**: Utiliza `recharts` (`BarChart`, `LineChart`, `PieChart`) e `recordsApi` para exibir relatórios filtráveis por intervalo de datas em quatro abas: volume diário (`volume`), desempenho por coletor (`collectors`), distribuição por tipo de óleo (`oil-types`) e volume por ponto de coleta (`points`).
