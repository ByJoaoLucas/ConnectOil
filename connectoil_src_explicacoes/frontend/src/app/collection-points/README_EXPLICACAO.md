# Pasta: `frontend/src/app/collection-points`

## Visão Geral
Rota protegida `/collection-points` para listagem, filtragem e cadastro/edição de pontos de coleta de óleo.

## Arquivos (2 arquivos)
- **`layout.tsx`**: Verifica autenticação (`isAuthenticated`) e aplica o shell `<Layout>`.
- **`page.tsx`**: Lista pontos de coleta (`collectionPointsApi`), permite busca/filtros por status (`ACTIVE`, `INACTIVE`, `MAINTENANCE`, `FULL`, `PENDING_APPROVAL`), exibe percentual de preenchimento/capacidade e disponibiliza modal/formulário validado com Zod (`pointSchema`) para criação, edição e exclusão de pontos.
