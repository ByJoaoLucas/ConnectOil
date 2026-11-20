# Pasta: `frontend/src/app/records`

## Visão Geral
Rota protegida `/records` para consulta e lançamento manual de registros de coletas efetuadas.

## Arquivos (2 arquivos)
- **`layout.tsx`**: Guarda de autenticação e aplicação do shell `<Layout>`.
- **`page.tsx`**: Lista registros de coleta (`recordsApi`), exibe volume coletado (litros), tipo de óleo, avaliação de qualidade, status de verificação gerencial e oferece formulário para criação/edição de registros vinculando ponto, coletor e agendamento opcional.
