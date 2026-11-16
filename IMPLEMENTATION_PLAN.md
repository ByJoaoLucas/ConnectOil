# Plano de Implementação - Plataforma Web Connect Oil

## Legenda de Status
- ✅ **Verde** = Concluído
- 🟡 **Amarelo** = Em andamento / Parcialmente implementado
- 🔴 **Vermelho** = Não iniciado

## Excluído: US13 (App Mobile) - Não viável para web-only

## Fase 1: Autenticação e Cadastro Básicos (US01, US08)
### Backend
- ✅ Aprimorar entidade User: adicionar campo CNPJ, validação de complexidade de senha
- ✅ Adicionar bloqueio de conta após 5 tentativas falhas
- ✅ Implementar hash de senha (BCrypt)
- ✅ Adicionar restrições únicas em email e CNPJ/CPF
- 🟡 Criar endpoint de Exclusão de Conta
- 🟡 Adicionar blacklist de token JWT para logout
- 🔴 Implementar configuração de HTTPS obrigatório

### Frontend
- ✅ Atualizar formulário de Cadastro: campo CNPJ/CPF com validação
- 🟡 Adicionar indicador de força da senha
- 🔴 Adicionar Exclusão de Conta na página de Perfil
- ✅ Melhorar mensagens de erro para email/CNPJ duplicado

## Fase 2: Agendamento de Coletas (US02, US03, US11, US12)
### Backend
- ✅ Criar aprimoramentos na entidade CollectionSchedule: janelas de horário, tipo venda/troca
- ✅ Adicionar validações: apenas datas futuras, volume > 0
- ✅ Implementar transições de status: AGENDADO → ATRIBUÍDO → A_CAMINHO → CONCLUÍDO/CANCELADO
- ✅ Adicionar regras de cancelamento: 2h antes para estabelecimento, a qualquer momento com justificativa para coletor
- ✅ Criar endpoint de alerta "quase cheio"
- 🟡 Implementar sistema de notificações (email/push placeholder - entidade existe, serviço parcial)

### Frontend
- ✅ Criar Assistente de Agendamento: seletor de data, janelas de horário (manhã/tarde/flexível), volume, seletor de tipo
- ✅ Adicionar botão de alerta "Quase Cheio" no dashboard
- ✅ Exibir timeline de status com notificações
- ✅ Implementar cancelamento com modal de confirmação

## Fase 3: Dashboard do Coletor e Aceitação (US04, US12)
### Backend
- ✅ Adicionar região/área de cobertura do coletor (entidade CollectorCoverage)
- 🟡 Criar endpoint para coletas disponíveis com filtros (data, região)
- 🟡 Implementar aceitação de coleta (operação atômica para evitar condições de corrida)
- 🔴 Adicionar atualizações em tempo real via WebSocket ou polling

### Frontend
- 🟡 Construir Dashboard do Coletor com visualização mapa/lista (parcial - página do coletor existe)
- ✅ Adicionar filtros: intervalo de datas, região, status
- 🟡 Implementar "Aceitar Coleta" com confirmação
- 🔴 Auto-refresh a cada 3 segundos

## Fase 4: Conclusão de Coleta e Registro de Peso (US05)
### Backend
- ✅ Criar CollectionRecord com peso real, código de confirmação
- ✅ Implementar detecção de discrepância (estimado vs real > limite)
- ✅ Adicionar confirmação do estabelecimento (código ou assinatura placeholder)
- 🟡 Tornar registros imutáveis após conclusão
- 🟡 Disparar notificações para discrepâncias

### Frontend
- 🟡 Construir formulário de Conclusão de Coleta: volume real, entrada de código de confirmação (página existe)
- 🟡 Exibir aviso de discrepância se > limite
- 🟡 Exibir coletas concluídas como somente leitura

## Fase 5: Venda e Troca de Óleo (US06)
### Backend
- ✅ Criar entidade Produto (catálogo de produtos de limpeza)
- ✅ Adicionar configuração de preço base do óleo (SystemConfig)
- 🟡 Implementar lógica de cálculo Venda vs Troca
- ✅ Criar Pedido de Troca com quantidades de produtos (entidade ExchangeItems)
- ✅ Vincular troca ao agendamento de coleta

### Frontend
- ✅ Adicionar toggle Venda/Troca no agendamento
- 🟡 Exibir valor calculado baseado no volume
- 🟡 Modal de seleção de produtos para troca
- 🟡 Exibir produtos a levar na visão do coletor

## Fase 6: Histórico e Certificados PDF (US07, US14)
### Backend
- 🟡 Criar endpoint de histórico com filtros e totais
- 🔴 Gerar certificado PDF com código de verificação (QR/hash)
- ✅ Implementar log de auditoria para todas as alterações de coleta (entidade AuditLog + service)
- 🔴 Adicionar endpoint de verificação de códigos de certificado

### Frontend
- 🟡 Construir página de Histórico com filtros, totais (página de registros existe)
- 🔴 Adicionar botão "Baixar Certificado"
- 🔴 Exibir código de verificação no certificado

## Fase 7: Funcionalidades Ambientais (US15)
### Backend
- 🔴 Adicionar conteúdo de diretrizes de armazenamento
- ✅ Criar entidade Incidente para vazamentos/acidentes
- 🔴 Adicionar validação do coletor (documentos, veículo)
- 🟡 Priorizar "quase cheio" na lista do coletor

### Frontend
- 🔴 Exibir modal de diretrizes de armazenamento durante agendamento
- 🟡 Adicionar formulário de Relatório de Incidente (página de incidentes existe)
- 🟡 Destacar coletas prioritárias para coletores

## Fase 8: Não-Funcionais (US08, US09, US10, US14)
- 🟡 Auditoria de segurança: hash de senha ✅, RBAC ✅, HTTPS 🔴, bloqueio de conta ✅
- 🟡 Performance: adicionar índices ✅, otimizar queries 🟡, adicionar cache 🔴
- 🟡 Usabilidade: simplificar linguagem 🟡, adicionar tooltips 🟡, reduzir cliques 🟡
- 🟡 Integridade de dados: logs de auditoria ✅, códigos de verificação 🔴, configuração de backup diário 🔴

---

## Dívidas Técnicas a Resolver
1. ✅ Atualizar entidade User: CNPJ, nome do estabelecimento, campos de endereço
2. ✅ Adicionar status ALMOST_FULL em CollectionPoint
3. ✅ Adicionar saleType (SALE/EXCHANGE), timeWindow em CollectionSchedule
4. ✅ Adicionar actualVolume, confirmationCode, discrepancyFlag em CollectionRecord
5. ✅ Adicionar catálogo de Produtos
6. ✅ Adicionar entidade Incidente
7. ✅ Adicionar entidade AuditLog
8. ✅ Adicionar entidade Notification

---

## Resumo

| Fase | Total | ✅ Concluído | 🟡 Em Andamento | 🔴 Não Iniciado |
|------|-------|--------------|-----------------|-----------------|
| Fase 1 | 11 | 6 | 3 | 2 |
| Fase 2 | 10 | 8 | 1 | 1 |
| Fase 3 | 8 | 2 | 3 | 3 |
| Fase 4 | 8 | 3 | 3 | 2 |
| Fase 5 | 9 | 4 | 4 | 1 |
| Fase 6 | 7 | 1 | 2 | 4 |
| Fase 7 | 7 | 1 | 3 | 3 |
| Fase 8 | 4 | 0 | 4 | 0 |
| **Total** | **64** | **25** | **23** | **16** |

**Progresso Geral: ~39% concluído, ~36% em andamento, ~25% não iniciado**

---

## Próximos Passos Prioritários

1. **Finalizar Fase 3** - Dashboard do Coletor completo (alta prioridade para US04)
2. **Finalizar Fase 4** - Conclusão de Coleta com validação de discrepância
3. **Implementar Fase 6** - Certificados PDF (requer biblioteca PDF como iText ou Flying Saucer)
4. **Completar Fase 5** - Lógica de cálculo Venda vs Troca no frontend
5. **Configurar HTTPS** - Certificados SSL para produção
6. **Implementar WebSocket** - Para atualizações em tempo real (Fase 3)
7. **Backup automatizado** - Configurar pg_dump agendado