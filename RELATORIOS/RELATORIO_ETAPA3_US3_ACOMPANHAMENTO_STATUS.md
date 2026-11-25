# Relatório Técnico - Etapa 3: Acompanhamento de Status e Notificações Confiáveis (US3)

**Projeto:** Connect Oil - Plataforma Web de Coleta de Óleo
**Branch:** feat/completes-us1-us7-fluxo-estabelecimento
**Data:** 22/11/2026
**Autor:** a preencher

---

## 1. Visão Geral e Objetivos
### 1.1 Histórias de Usuário
| História | Descrição | Status |
|----------|-----------|--------|
| US3 | Acompanhamento de Status e Notificações Confiáveis: exibir os status agendada, atribuída, a caminho, concluída e cancelada; notificar o estabelecimento a cada mudança de status. | Concluída |

### 1.2 Objetivos Alcançados
- [x] GET de agendamentos liberados para ESTABLISHMENT com filtro por pontos próprios no service
- [x] Notificações usam `point.getEstablishment()` (dono real) em vez de e-mail de contato
- [x] Notificação de cancelamento adicionada (estabelecimento e coletor, se houver)
- [x] Badges de status em português no frontend (agendada, atribuída, a caminho, em andamento, concluída, cancelada, reagendada)
- [x] Motivo do cancelamento e linha do tempo (timeline) na tela de detalhe do agendamento
- [x] Tratamento de 403/404 com mensagens claras em português
- [x] Testes de service, autorização e frontend passam
- [x] Lista de agendamentos ordenada por data de criação (mais novos primeiro) - `findAllByOrderByCreatedAtDesc`
- [x] Botão "Aceitar Coleta" para COLLECTOR em agendamentos PENDING sem coletor
- [x] Modal de edição: datas passadas editáveis por ADMIN, data formatada corretamente (YYYY-MM-DD)
- [x] Timeline com data de criação formatada localmente (sem conversão UTC)
- [x] Permissões de botões corretas: Editar/Deletar apenas ADMIN; Confirmar/Iniciar/Concluir apenas ADMIN ou coletor dono; Cancelar para todos; Atribuir coletor apenas ADMIN
- [x] **Bugfix**: Formato de hora no modal de edição (HH:mm:ss → HH:mm via .slice(0,5))
- [x] **Bugfix**: Botões de progressão para COLLECTOR atribuído (CONFIRMED/ASSIGNED → Iniciar Deslocamento; IN_PROGRESS → Concluir Coleta)
- [x] **Bugfix**: Notificações persistidas corretamente para estabelecimento (carregar User gerenciado no NotificationService)
- [x] **Bugfix**: Ordenação da listagem para ADMIN/COLLECTOR (mais recentes no topo, sem paginação)
- [x] **Bugfix**: COLLECTOR vê agendamentos atribuídos + disponíveis (PENDING, collector=null) via novo endpoint `/api/schedules/collector/me`
- [x] **Bugfix**: Notificação para ESTABLISHMENT ao criar agendamento (sem coletor atribuído)
- [x] **Bugfix**: Painel "Ações" na tela de detalhe para COLLECTOR com agendamentos atribuídos (status CONFIRMED/ASSIGNED/IN_PROGRESS)

---

## 2. O que foi feito / criado

### Back-end
- **CollectionScheduleController.java**: Adicionados endpoints GET `/api/schedules`, `/list`, `/{id}`, `/date/{date}`, `/point/{pointId}` acessíveis para ESTABLISHMENT com filtro por propriedade no service. Adicionada injeção de `CollectionPointRepository` e verificação de posse do ponto no `getScheduleById`. Adicionado endpoint `PUT /api/schedules/{id}/accept` para COLLECTOR aceitar coleta (self-assignment). **Novo**: Endpoint `GET /api/schedules/collector/me` para COLLECTOR ver seus agendamentos atribuídos + disponíveis (PENDING, collector=null).
- **CollectionScheduleService.java**: 
  - Novo método `getSchedulesByEstablishmentAndDate(UUID establishmentId, LocalDate date)` para filtrar agendamentos por estabelecimento e data.
  - Métodos `getSchedulesByEstablishment` e `getSchedulesByEstablishmentAndDate` validam se o usuário tem role ESTABLISHMENT antes de consultar.
  - Notificações de status (confirmação, atribuição, a caminho, conclusão) alteradas para usar `point.getEstablishment()` em vez de `findByEmail(point.getContactEmail())`.
  - Método `cancelSchedule` agora notifica tanto o estabelecimento (via `point.getEstablishment()`) quanto o coletor (se existir).
  - Adicionado método `acceptSchedule` para COLLECTOR auto-atribuir-se em agendamentos PENDING com collector=null (update condicional).
  - Método `getAllSchedules()` alterado para usar `findAllByOrderByCreatedAtDesc()` (ordenação por mais recentes primeiro).
  - Validações de nulo para `schedule.getCollector()` mantidas em todos os fluxos.
  - **Novo**: Método `getSchedulesForCollector(UUID collectorId)` para retornar agendamentos atribuídos ao coletor + disponíveis (PENDING, collector=null).
  - **Novo**: Notificação para estabelecimento ao criar agendamento (mesmo sem coletor atribuído).
- **CollectionScheduleRepository.java**: Adicionado método `findByCollectionPoint_EstablishmentAndScheduledDate(User establishment, LocalDate date)`, `findAllByOrderByCreatedAtDesc()` e **novo** `findByCollectorOrAvailable(User collector, ScheduleStatus pendingStatus)` para buscar agendamentos atribuídos ao coletor + disponíveis.
- **NotificationService.java**: 
  - Injetado `UserRepository` para carregar o usuário gerenciado antes de persistir a notificação.
  - Método `send()` alterado para buscar o `User` via `userRepository.findById(userId)` em vez de criar instância desanexada com builder, garantindo relacionamento JPA correto.

### Banco / Migração
- Nenhuma migração Flyway necessária nesta etapa (alterações apenas de lógica de negócio e consultas).

### Front-end
- **schedules/[id]/page.tsx**: 
  - Adicionado componente `Timeline` que exibe o histórico de mudanças de status do agendamento (criação, atribuição, a caminho, em andamento, conclusão, cancelamento, reagendamento).
  - Função `buildTimeline` que constrói a linha do tempo a partir dos campos de data/hora do DTO (`createdAt`, `assignedAt`, `enRouteAt`, `inProgressAt`, `completedAt`, `cancelledAt`, `rescheduledAt`).
  - Badge de status com cores e textos em português (usando mapeamento: PENDING=Agendada, CONFIRMED/ASSIGNED=Atribuída, EN_ROUTE=A caminho, IN_PROGRESS=Em andamento, COMPLETED=Concluída, CANCELLED=Cancelada, RESCHEDULED=Reagendada).
  - Exibição do motivo do cancelamento (`cancellationReason`) quando o status for CANCELLED.
  - Tratamento de erros 403/404/409 com mensagens amigáveis em português.
  - Adicionado botão "Aceitar Coleta" para COLLECTOR em agendamentos PENDING com collector=null.
  - Botão de edição no header apenas para ADMIN.
  - Modal de edição: dropdown de coletor apenas para ADMIN; datas passadas editáveis por ADMIN (min date removido).
  - Função `formatLocalDate` helper para parsear YYYY-MM-DD sem conversão UTC na timeline.
  - **Bugfix**: Hora no modal de edição - `schedule.startTime.slice(0, 5)` e `schedule.endTime.slice(0, 5)` para converter HH:mm:ss → HH:mm.
  - **Bugfix**: Botões de progressão para COLLECTOR atribuído: status CONFIRMED/ASSIGNED mostra "Iniciar Deslocamento (A caminho)"; status IN_PROGRESS mostra "Concluir Coleta".
- **schedules/page.tsx**: 
  - Filtro de status atualizado para usar os rótulos centralizados de `SCHEDULE_STATUS_LABELS` (Agendada, Atribuída, A caminho, Em andamento, Concluída, Cancelada, Reagendada).
  - Adicionadas variáveis `isCollector`, `isAdmin` e `acceptMutation` com `handleAccept`.
  - Botão "Aceitar Coleta" (CheckCircle2) para COLLECTOR em agendamentos PENDING com collector=null.
  - Permissões de botões: Editar/Deletar apenas ADMIN; Confirmar apenas ADMIN ou coletor dono; Iniciar/Concluir apenas ADMIN ou coletor dono; Cancelar para todos; Dropdown de coletor apenas ADMIN.
  - Modal de edição: `min={isEditing || isAdmin ? undefined : getLocalDateString()}` para permitir ADMIN editar datas passadas.
  - Data no input: `String(initialData.scheduledDate).slice(0, 10)` para formato YYYY-MM-DD correto.
  - **Bugfix**: Hora no modal de edição - `initialData.startTime.slice(0, 5)` e `initialData.endTime.slice(0, 5)` para converter HH:mm:ss → HH:mm.
  - **Bugfix**: Botões de progressão na listagem: status CONFIRMED/ASSIGNED mostra "Iniciar Deslocamento"; status IN_PROGRESS mostra "Concluir Coleta".
- **dashboard/page.tsx**: 
  - Função `getStatusConfig` atualizada para usar `SCHEDULE_STATUS_LABELS` e `SCHEDULE_STATUS_COLORS` centralizados, garantindo consistência dos rótulos em todas as telas.
- **components/Layout.tsx**: 
  - Adicionado ícone de notificações (sininho) no cabeçalho para usuários ESTABLISHMENT, ADMIN e COLLECTOR.
  - Dropdown de notificações com lista de notificações, contador de não lidas, opção "Marcar todas como lidas" e marcação individual.
  - Integração com `notificationsApi` para buscar notificações, contar não lidas e marcar como lidas.
  - Polling automático a cada 30 segundos para atualizar contador de não lidas.
- **types/index.ts**: 
  - Atualizado `SCHEDULE_STATUS_LABELS` para padronizar conforme tabela de referência do checklist:
    - PENDING → "Agendada"
    - CONFIRMED/ASSIGNED → "Atribuída"
    - EN_ROUTE → "A caminho"
    - IN_PROGRESS → "Em andamento"
    - COMPLETED → "Concluída"
    - CANCELLED → "Cancelada"
    - RESCHEDULED → "Reagendada"
- **lib/api.ts**: Adicionado endpoint `accept: (id) => api.put(\`/schedules/${id}/accept\`)`.

### Testes
- **CollectionScheduleServiceTest.java**: Adicionados 7 novos testes para US3:
  - `getSchedulesByEstablishmentAndDate_ReturnsOnlyOwnSchedules` - estabelecimento vê apenas seus agendamentos
  - `getSchedulesByEstablishmentAndDate_EmptyWhenNoSchedules` - lista vazia quando não há agendamentos
  - `getSchedulesByEstablishmentAndDate_InvalidRole_ThrowsException` - erro se usuário não for ESTABLISHMENT
  - `cancelSchedule_Establishment_NotifiesBothEstablishmentAndCollector` - notifica ambos no cancelamento
  - `cancelSchedule_Establishment_WithoutCollector_NotifiesOnlyEstablishment` - notifica só estabelecimento se não houver coletor
  - `cancelSchedule_Establishment_LessThan2Hours_ThrowsException` - bloqueio de cancelamento < 2h (corrigido fuso horário)
  - `cancelSchedule_Establishment_OtherEstablishmentPoint_ThrowsException` - não pode cancelar ponto de outro estabelecimento
- **EndpointAuthorizationTest.java**: Adicionados 5 testes de autorização para endpoints GET de agendamentos acessíveis por ESTABLISHMENT.

---

## 3. Arquivos tocados / modificados
| Arquivo | O que mudou |
|---------|-------------|
| `backend/src/main/java/com/oilcollection/controller/CollectionScheduleController.java` | Endpoints GET liberados para ESTABLISHMENT com filtro por posse; injeção de CollectionPointRepository; verificação de posse no getScheduleById; endpoint accept para COLLECTOR; **novo endpoint GET /collector/me para COLLECTOR ver agendamentos atribuídos + disponíveis** |
| `backend/src/main/java/com/oilcollection/service/CollectionScheduleService.java` | Novos métodos getSchedulesByEstablishmentAndDate e acceptSchedule; notificações usam point.getEstablishment(); cancelSchedule notifica estabelecimento e coletor; getAllSchedules usa findAllByOrderByCreatedAtDesc; **novo método getSchedulesForCollector (atribuídos + disponíveis); notificação para estabelecimento ao criar agendamento** |
| `backend/src/main/java/com/oilcollection/service/NotificationService.java` | Injetado UserRepository; método send() carrega User gerenciado para persistir notificação corretamente |
| `backend/src/main/java/com/oilcollection/repository/CollectionScheduleRepository.java` | Adicionado findByCollectionPoint_EstablishmentAndScheduledDate, findAllByOrderByCreatedAtDesc e **findByCollectorOrAvailable (atribuídos + disponíveis PENDING sem coletor)** |
| `backend/src/main/java/com/oilcollection/dto/CollectionScheduleDTO.java` | Adicionado campo collectionPointEstablishmentId e população no fromEntity |
| `frontend/src/app/schedules/[id]/page.tsx` | Componente Timeline, buildTimeline, badges de status em PT-BR, motivo de cancelamento, tratamento de erros, botão Aceitar Coleta, botão editar apenas ADMIN, modal edição restrito, formatLocalDate helper; **bugfix: hora slice(0,5); botões progressão COLLECTOR (CONFIRMED/ASSIGNED→Iniciar, IN_PROGRESS→Concluir); painel Ações visível para COLLECTOR com agendamentos atribuídos** |
| `frontend/src/app/schedules/page.tsx` | Filtro de status com SCHEDULE_STATUS_LABELS; botão Aceitar Coleta para COLLECTOR; permissões de botões corretas; modal edição: min date removido para ADMIN; data formatada com String.slice(0,10); **bugfix: hora slice(0,5); botões progressão listagem (CONFIRMED/ASSIGNED→Iniciar, IN_PROGRESS→Concluir); COLLECTOR usa endpoint /collector/me (atribuídos + disponíveis)** |
| `frontend/src/app/dashboard/page.tsx` | getStatusConfig atualizado para usar labels e colors centralizados |
| `frontend/src/components/Layout.tsx` | Adicionado sininho de notificações no header com dropdown, contador, marcação como lida |
| `frontend/src/types/index.ts` | SCHEDULE_STATUS_LABELS padronizado conforme checklist |
| `frontend/src/lib/api.ts` | Adicionado accept: (id) => api.put(`/schedules/${id}/accept`); **novo getMySchedules para COLLECTOR (endpoint /collector/me); renomeado endpoints establishment para getEstablishmentSchedules** |
| `backend/src/test/java/com/oilcollection/service/CollectionScheduleServiceTest.java` | 7 novos testes US3 (filtragem, notificações, validações de cancelamento) |
| `backend/src/test/java/com/oilcollection/security/EndpointAuthorizationTest.java` | 5 novos testes de autorização para GET de agendamentos por ESTABLISHMENT |

---

## 4. Testes
### Backend (Docker com maven:3.9.6-eclipse-temurin-21-alpine)
```
mvn test
```
**Resultado:** 123 testes executados, 122 passed, 1 skipped, 0 failures, 0 errors. BUILD SUCCESS.

### Frontend
```
npm test
```
**Resultado:** 13 test suites, 82 testes passed. PASS.

### Frontend Build
```
npm run build
```
**Resultado:** Compiled successfully, todas as 16 rotas geradas. BUILD SUCCESS.

---

## 5. Como testar manualmente
1. Subir a aplicação com Docker Compose: `docker compose up -d`
2. Acessar `http://localhost:3000` e fazer login como estabelecimento A
3. Criar um ponto de coleta e um agendamento
4. Fazer logout e login como estabelecimento B
5. Verificar que o agendamento do estabelecimento A **não** aparece na listagem
6. Fazer login como ADMIN ou COLLECTOR
7. Mudar o status do agendamento (atribuir, iniciar rota, concluir)
8. Voltar como estabelecimento A e verificar:
   - Badge de status atualizado em português
   - Timeline na tela de detalhe mostrando as mudanças de status
   - Notificações recebidas (se implementado no frontend)
9. Tentar cancelar com menos de 2h de antecedência → deve bloquear com mensagem clara
10. Cancelar com mais de 2h → deve permitir e mostrar motivo na tela
11. **Como ADMIN**: Editar um agendamento existente → verificar se a data aparece corretamente no modal (formato YYYY-MM-DD)
12. **Como COLLECTOR**: Verificar que o botão "Editar" **não** aparece na listagem de agendamentos
13. **Como ESTABLISHMENT**: Verificar que recebe notificações ao mudar status (atribuir, confirmar, a caminho, concluir, cancelar)
14. **Como COLLECTOR**: Ver agendamentos PENDING sem coletor → clicar "Aceitar Coleta" → agendamento muda para CONFIRMED e assignedAt preenchido
15. **Como segundo COLLECTOR**: Tentar aceitar a mesma coleta → deve receber erro 409 "Esta coleta já foi aceita por outro coletor"

---

## 6. Correções adicionais aplicadas (pós-validação)
| Item | Arquivo | Correção |
|------|---------|----------|
| 1. Timezone bug no modal de edição | `frontend/src/app/schedules/page.tsx` (linha 637) | Alterado `setValue('scheduledDate', initialData.scheduledDate)` para `setValue('scheduledDate', String(initialData.scheduledDate).slice(0, 10))` para extrair apenas a data no formato YYYY-MM-DD |
| 2. Botão Editar restrito a ADMIN | `frontend/src/app/schedules/page.tsx` (linhas 432-442) | Alterado `!isEstablishment` para `user?.role === 'ADMIN'` - apenas ADMIN pode editar agendamentos |
| 3. Notificações em mudanças de status | `backend/src/main/java/com/oilcollection/service/CollectionScheduleService.java` | Verificado: todos os métodos (assignSchedule, confirmSchedule, startSchedule, completeSchedule, cancelSchedule, rescheduleSchedule) usam `point.getEstablishment()` para notificar o estabelecimento |
| 4. Ordenação de agendamentos (mais novos primeiro) | `backend/src/main/java/com/oilcollection/service/CollectionScheduleService.java` + `Repository` | `getAllSchedules()` usa `findAllByOrderByCreatedAtDesc()`; adicionado método no repository |
| 5. Botão "Aceitar Coleta" para COLLECTOR | `backend` (Controller/Service/Repository) + `frontend` (page/page.tsx, api.ts) | Endpoint PUT /accept, update condicional no service, botão com CheckCircle2 no frontend |
| 6. Modal de edição: datas passadas para ADMIN | `frontend/src/app/schedules/page.tsx` | `min={isEditing || isAdmin ? undefined : getLocalDateString()}` |
| 7. Timeline data de criação sem UTC | `frontend/src/app/schedules/[id]/page.tsx` | Função `formatLocalDate` para parsear YYYY-MM-DD localmente |
| 8. Permissões de botões refinadas | `frontend/src/app/schedules/page.tsx`, `schedules/[id]/page.tsx` | Edit/Delete apenas ADMIN; Confirm/Start/Complete apenas ADMIN ou coletor dono; Collector dropdown apenas ADMIN |
| **9. Formato de hora no modal de edição (HH:mm:ss → HH:mm)** | `frontend/src/app/schedules/page.tsx` (linha 686) + `schedules/[id]/page.tsx` (linha 619) | Adicionado `.slice(0, 5)` em `startTime` e `endTime` ao preencher o formulário de edição, pois o backend retorna HH:mm:ss |
| **10. Botões de progressão para COLLECTOR atribuído** | `frontend/src/app/schedules/page.tsx` + `schedules/[id]/page.tsx` | Status CONFIRMED/ASSIGNED ("Atribuída"): botão "Iniciar Deslocamento (A caminho)"; Status IN_PROGRESS ("A caminho"): botão "Concluir Coleta" |
| **11. Notificações persistidas para estabelecimento** | `backend/src/main/java/com/oilcollection/service/NotificationService.java` | Injetado `UserRepository`; método `send()` carrega `User` via `findById()` antes de salvar, garantindo relacionamento JPA correto (antes criava instância desanexada com builder) |
| **12. Ordenação listagem ADMIN/COLLECTOR (mais recentes no topo)** | `backend/src/main/java/com/oilcollection/service/CollectionScheduleService.java` + `Repository` | `getAllSchedules()` usa `findAllByOrderByCreatedAtDesc()`; endpoint `/api/schedules/list` retorna lista completa sem paginação |
| **13. COLLECTOR vê agendamentos atribuídos + disponíveis** | `backend` (Controller/Service/Repository) + `frontend` (page.tsx, api.ts) | Novo endpoint `GET /api/schedules/collector/me`; novo método `getSchedulesForCollector`; query `findByCollectorOrAvailable`; frontend usa endpoint para role COLLECTOR |
| **14. Notificação para ESTABLISHMENT ao criar agendamento** | `backend/src/main/java/com/oilcollection/service/CollectionScheduleService.java` | No método `createSchedule`, adicionada notificação para o estabelecimento (via `point.getEstablishment()`) mesmo quando não há coletor atribuído |
| **15. Painel "Ações" na tela de detalhe para COLLECTOR** | `frontend/src/app/schedules/[id]/page.tsx` | Verificado: botões "Iniciar Deslocamento" (CONFIRMED/ASSIGNED) e "Concluir Coleta" (IN_PROGRESS) já estavam implementados corretamente para COLLECTOR dono do agendamento |

---

## 7. Sugestão de mensagem de commit
```
feat(schedules): implementar US3 - acompanhamento de status e notificações

- Liberar GET de agendamentos para ESTABLISHMENT com filtro por pontos próprios
- Notificações usam point.getEstablishment() (dono real) em vez de e-mail
- Adicionar notificação de cancelamento para estabelecimento e coletor
- Frontend: badges de status PT-BR padronizados (Agendada, Atribuída, A caminho, Em andamento, Concluída, Cancelada, Reagendada)
- Timeline na tela de detalhe com motivo de cancelamento
- Sininho de notificações no header para ESTABLISHMENT/ADMIN/COLLECTOR
- Tratamento de erros 403/404 com mensagens claras
- Lista ordenada por mais recentes (findAllByOrderByCreatedAtDesc)
- Aceite próprio pelo coletor (PUT /accept, update condicional)
- Botão "Aceitar Coleta" no frontend para COLLECTOR
- Modal de edição: datas passadas editáveis por ADMIN, data formatada YYYY-MM-DD
- Permissões: Editar/Deletar apenas ADMIN; Confirm/Start/Complete apenas ADMIN ou dono; Collector dropdown apenas ADMIN
- 7 testes de service + 5 testes de autorização adicionados

fix(schedules): corrigir bugs da validação manual Etapa 3

- Modal edição: converter hora HH:mm:ss → HH:mm com .slice(0,5)
- Botões COLLECTOR: progressão CONFIRMED/ASSIGNED→Iniciar Deslocamento, IN_PROGRESS→Concluir Coleta
- Notificações: carregar User gerenciado no NotificationService para persistir corretamente
- Ordenação: listagem ADMIN/COLLECTOR ordenada por mais recentes (findAllByOrderByCreatedAtDesc)
- COLLECTOR vê agendamentos atribuídos + disponíveis (endpoint /collector/me + findByCollectorOrAvailable)
- Notificação para ESTABLISHMENT ao criar agendamento (mesmo sem coletor)
- Painel Ações na tela de detalhe para COLLECTOR já funcionava corretamente
```

---

## 8. Problemas extras encontrados e não corrigidos
- Nenhum problema extra relevante encontrado durante a implementação da Etapa 3.

---

## 9. Melhorias pós-US3 (entregues após validação da Etapa 3)

As seguintes melhorias foram implementadas após a conclusão dos critérios de aceite do US3, a pedido do usuário:

### 9.1 Exclusão de Notificações Lidas
- **Backend**: Adicionado endpoint `DELETE /api/notifications/{id}` em `NotificationController.java`
- **Service**: `NotificationService.deleteNotification()` valida que a notificação pertence ao usuário e que já foi lida (`isRead = true`), lançando `BusinessException` (HTTP 400) caso contrário
- **Frontend**: Ícone de lixeira (`Trash2` do lucide-react) exibido apenas para notificações já marcadas como lidas no dropdown do header
- **API**: `notificationsApi.deleteNotification(id)` em `lib/api.ts`

### 9.2 Perfil de Teste Dedicado (`application-test.yml`)
- Criado `backend/src/main/resources/application-test.yml` com configurações corretas para testes:
  - Credenciais de banco alinhadas com `.env` (connectoil/admin123/admin123)
  - JWT secret válido em base64 (sem underscores inválidos)
  - Flyway desabilitado para testes unitários rápidos
  - Hibernate `ddl-auto: create-drop` para schema limpo a cada execução
- Permite rodar `mvn test` sem depender de Docker Compose subido

### 9.3 Correções de Integração Frontend-Backend
- **Layout.tsx**: Corrigido parsing de notificações - Spring Boot retorna `Page` direto em `response.data` (não `response.data.data`), ajustado para `data.content || data`
- **Contador não lidas**: Endpoint correto `/count/unread` (era `/unread-count`)
- **CORS**: Validado para `localhost:3000` funcionando corretamente

### 9.4 Validação Completa do Fluxo E2E
Testado e confirmado funcionando:
1. Estabelecimento cria agendamento → recebe `SCHEDULE_CREATED`
2. Coletor aceita agendamento (`PUT /accept`) → estabelecimento recebe `SCHEDULE_ASSIGNED`, coletor recebe `SCHEDULE_ASSIGNED`
3. Coletor inicia deslocamento (`PUT /start`) → estabelecimento recebe `SCHEDULE_EN_ROUTE`
4. Coletor conclui coleta (`PUT /complete`) → estabelecimento recebe `SCHEDULE_COMPLETED` ✅
5. Notificações aparecem no sininho do header com badge de contador
6. Notificações lidas mostram ícone de lixeira para exclusão