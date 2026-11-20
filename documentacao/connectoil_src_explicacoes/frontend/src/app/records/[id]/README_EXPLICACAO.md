# Pasta: `frontend/src/app/records/[id]`

## Visão Geral
Rota dinâmica protegida `/records/[id]` para auditoria, verificação e detalhamento técnico de um registro de coleta.

## Arquivos (1 arquivo)
- **`page.tsx`**: Apresenta parâmetros físico-químicos e operacionais da coleta (volume em litros, quantidade/tipo de recipientes, nota de qualidade, temperatura °C, pH, percentual de contaminação, coordenadas GPS, código de confirmação/certificado) e permite verificação pelo gestor (`recordsApi.verify`) ou edição.
