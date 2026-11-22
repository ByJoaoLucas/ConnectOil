Checklist de Finalização — ConnectOil (US1, US2, US3, US4, US5 e US7)

Instrução para o Agente (Claude Code): Antes de qualquer alteração, leia erros_a_corrigir/ e documentacao/connect_oil_doc/Backlog.md para respeitar a arquitetura existente. Implemente uma etapa por vez (Back-end + Migração Flyway se necessário + Front-end + Testes). Ao terminar a etapa, grave o relatório na pasta RELATORIOS/ (formato abaixo), mostre um resumo na resposta e aguarde o meu sinal antes de começar a próxima. Só marque uma etapa como [x] depois de rodar e passar os testes (cd backend && ./mvnw test e cd frontend && npm test -- --watchAll=false). Se não for possível rodar algum comando, diga isso claramente em vez de afirmar que passou.

## Repositórios do projeto
- **Repositório de código (este):** é onde você trabalha. Todas as alterações e os relatórios em `RELATORIOS/` ficam aqui.
- **Repositório de documentação (separado, disciplina):** guarda o backlog e as user stories oficiais. **Você não tem acesso a ele e não deve alterá-lo.**
- A pasta `documentacao/connect_oil_doc/` deste repositório é apenas uma **cópia** e pode estar desatualizada. Se o `Backlog.md` divergir dos critérios de aceite escritos neste checklist, **vale o checklist**. Se achar uma divergência relevante, cite no relatório em "Problemas extras", sem corrigir.
REGRAS GERAIS (valem para todas as etapas)
- 
Controle de versão (INEGOCIÁVEL)
PROIBIDO executar git commit, git push, git merge, git rebase, git reset, git checkout de outra branch ou qualquer comando git que altere histórico, branches ou o repositório remoto. Seu trabalho termina em alterar os arquivos no workspace. Os commits são feitos por mim, somente na minha branch (a branch principal é protegida).
Não crie branches, tags nem stashes. Comandos git somente de leitura (git status, git diff, git log, git branch --show-current) são permitidos para montar o relatório.
Escopo e arquitetura
NÃO mude a arquitetura. Backend: model / dto / repository / service / controller / security (Java 21, Spring Boot 3.2.5, Flyway, JWT). Frontend: Next.js 14 App Router, TypeScript, Tailwind, React Hook Form + Zod, React Query.
NÃO crie arquivos desnecessários. Só código, migração Flyway, testes e o relatório da etapa em RELATORIOS/. Nenhum outro .md, script avulso ou rascunho dentro do repositório. Além do relatório, o único .md que você pode editar é este checklist (para marcar [x] nas etapas concluídas).
Escopo: somente US1, US2, US3, US4, US5 e US7. Não mexer em US6 (venda/troca além do que já existe), US8 (HTTPS, blacklist de JWT), US9 a US15 nem refatorações gerais. Problemas extras que encontrar: não corrija, apenas cite no relatório.
Padrões técnicos
Segurança em duas camadas: toda liberação de acesso exige alterar o SecurityConfig e o @PreAuthorize do método no controller, além do EndpointAuthorizationTest. Papéis existentes: ADMIN, COLLECTOR, ESTABLISHMENT, CLIENT (o papel MANAGER foi removido).
Filtro por dono no service: para ESTABLISHMENT, qualquer leitura ou alteração de agendamentos, registros, notificações e incidentes deve se limitar aos dados dos seus pontos de coleta, validado no service (não só no controller). Acesso a dado de outro estabelecimento deve responder "não encontrado", sem revelar que existe.
Migrações Flyway: ddl-auto está em validate. Toda mudança de entidade exige migração nova. Hoje a última é V5; use a próxima livre (V6, V7...) e confira antes de criar.
Textos ao usuário em português simples, sem termos técnicos. Não deixe a tela vazia em silêncio quando a API devolver 403/404/409: mostre mensagem clara.
Nunca assuma que schedule.getCollector() é não nulo (a partir da Etapa 2 ele pode ser nulo).
Relatório obrigatório ao fim de cada etapa (gravar em RELATORIOS/)

Ao terminar cada etapa, crie um arquivo na pasta RELATORIOS/ na raiz do projeto, no mesmo estilo dos relatórios já existentes (RELATORIO_CADASTRO_AUTENTICACAO.md etc.), com o nome:

RELATORIOS/RELATORIO_ETAPA<N>_<US>_<TEMA>.md (ex.: RELATORIO_ETAPA1_US1_EXCLUSAO_CONTA.md)

Estrutura do arquivo:

markdown
# Relatório Técnico - <título da etapa> (<US>)

**Projeto:** Connect Oil - Plataforma Web de Coleta de Óleo
**Branch:** <resultado de `git branch --show-current`>
**Data:** <data de hoje, dd/mm/aaaa>
**Autor:** <o nome que eu informar; se eu não informar, deixe "a preencher">

---

## 1. Visão Geral e Objetivos
### 1.1 Histórias de Usuário
| História | Descrição | Status |
(tabela com a US da etapa e o status: concluída ou parcial, e o que ficou de fora)
### 1.2 Objetivos Alcançados
- [x] item cumprido
- [ ] item NÃO cumprido (explicar o motivo)

## 2. O que foi feito / criado
(resumo claro, separado em Back-end, Banco/Migração, Front-end e Testes)

## 3. Arquivos tocados / modificados
(lista com o caminho de cada arquivo e uma frase do que mudou)

## 4. Testes
(comandos rodados e resultado do backend e do frontend; se algo não pôde ser rodado, dizer claramente)

## 5. Como testar manualmente
(passos curtos no Docker, iguais ao "Teste manual" da etapa)

## 6. Sugestão de mensagem de commit
(Conventional Commits, ex.: `feat(schedules): permitir agendamento sem coletor`)

## 7. Problemas extras encontrados e não corrigidos
(lista; vazio se não houver)

Na resposta do terminal, mostre apenas um resumo curto (o que foi feito, nome do arquivo de relatório criado e a mensagem de commit sugerida). Depois: pare e aguarde meu sinal.

Referência rápida
Status do agendamento (enum ScheduleStatus) → texto na tela (US3)
Enum	Texto em português
PENDING	Agendada
CONFIRMED, ASSIGNED	Atribuída
EN_ROUTE	A caminho
IN_PROGRESS	Em andamento
COMPLETED	Concluída
CANCELLED	Cancelada
RESCHEDULED	Reagendada
Ordem e dependências

Etapa 1 (independente) → Etapa 2 (cria o dono do ponto e o coletor opcional) → Etapa 3 (usa o dono do ponto) → Etapa 4 (usa agendamento sem coletor) → Etapa 5 (abre leitura de registros por dono) → Etapa 6 (usa a leitura e o registro concluído).

Decisões de projeto já tomadas
O dono do ponto é o campo collection_points.establishment_id (FK para users), criado na Etapa 2.
O agendamento nasce sem coletor (collector_id nulo) e PENDING; o coletor passa a ser definido ao aceitar (Etapa 4).
O cancelamento continua em PUT /api/schedules/{id}/cancel?reason=... (o reason é obrigatório para todos).
O aceite é atômico por UPDATE condicional no repository (sem @Version, sem migração extra).
O código de confirmação é gerado ao registrar a pesagem, é visível somente ao estabelecimento dono (e ao ADMIN), e o coletor digita esse código para concluir a coleta.
A exclusão de conta é lógica (isActive = false); e-mail e CPF/CNPJ continuam ocupados depois.
Etapa 1 — US1: Cadastro e Exclusão de Conta

Critérios de aceite (US1): cadastro com nome, CNPJ/CPF, endereço, e-mail e telefone; validação de e-mail e CNPJ/CPF; sem duplicidade; senha forte (8+ caracteres, 1 maiúscula, 1 número, 1 especial); usuário pode excluir a conta. (Cadastro, validações e senha já estão prontos; falta a exclusão na tela.)

[x] Back-end: DELETE /api/auth/account já existe (AuthController.deleteOwnAccount / AuthService.deleteOwnAccount, exclusão lógica). Apenas confirmar que não gera erro de chave estrangeira e que o usuário desativado não consegue mais logar. Não alterar o backend sem necessidade.
[x] Front-end: em frontend/src/app/profile/page.tsx, seção "Zona de Perigo" (visível só para COLLECTOR e ESTABLISHMENT) com botão "Excluir minha conta" e modal de confirmação em que a pessoa digita EXCLUIR (botão só habilita após digitar). Ao confirmar: DELETE /auth/account, logout pelo AuthContext, limpar tokens e redirecionar para /login, com aviso em português.
[x] Testes: criar frontend/src/app/profile/page.test.tsx (abre o modal, botão desabilitado até digitar EXCLUIR, chama a API, faz logout e redireciona). Garantir que AuthControllerAccountDeletionTest e AuthServiceAccountDeletionTest passam.
[x] Teste manual: cadastrar estabelecimento → entrar → Perfil → excluir → voltar ao login → tentar entrar de novo (deve falhar).
[x] No relatório: informar que e-mail e CPF/CNPJ seguem ocupados após a exclusão.
[x] Relatório: gravar RELATORIOS/RELATORIO_ETAPA1_US1_EXCLUSAO_CONTA.md no formato da seção "Relatório obrigatório" e mostrar o resumo na resposta.
Etapa 2 — US2: Agendamento pelo Estabelecimento e Regras de Cancelamento

Critérios de aceite (US2): informar data, faixa de horário e volume em litros (janelas de 2h, Manhã/Tarde ou horário livre); sem data passada nem volume ≤ 0; criar com status "agendada"; escolher tipo (venda ou troca); estabelecimento cancela até 2h antes; coletor cancela com justificativa em caso de imprevisto.

[ ] Banco (migração V6): (a) collection_schedules.collector_id passa a aceitar NULL; (b) collection_points.establishment_id (FK users, nullable, com índice). A coluna cancellation_reason já existe, não recriar. Atualizar CollectionSchedule (collector nullable), CollectionPoint (@ManyToOne establishment) e DTOs (establishmentId, establishmentName).
[ ] Pontos de coleta: liberar POST/PUT /api/collection-points para ESTABLISHMENT (SecurityConfig + @PreAuthorize). Ao criar, o dono é sempre o usuário logado (ignorar o do corpo); só edita pontos próprios; não exclui. A listagem para ESTABLISHMENT mostra só os próprios. ADMIN continua como hoje e pode informar o dono.
[ ] Agendamento (back-end): remover @NotNull de collectorId em CollectionScheduleDTO. ESTABLISHMENT cria agendamento somente em ponto próprio, com collector = null e status PENDING. Manter as validações atuais (data hoje/futura, volume mínimo, saleType). Corrigir todos os schedule.getCollector() sem checagem de nulo em CollectionScheduleService (criação ~164, confirmação ~264, conclusão ~372, cancelamento ~420, reagendamento ~462): notificar o coletor apenas quando houver coletor.
[ ] Cancelamento (back-end): manter PUT /api/schedules/{id}/cancel?reason=. Abrir para ESTABLISHMENT (só agendamentos dos seus pontos, com a trava de 2h) e COLLECTOR (só agendamentos atribuídos a ele, a qualquer horário, com justificativa obrigatória). ADMIN como hoje.
[ ] Front-end: em schedules/page.tsx, para ESTABLISHMENT: esconder o campo Coletor, listar só os próprios pontos, e liberar "Novo Ponto" em collection-points/page.tsx. O formulário hoje não tem tipo (Venda/Troca) nem janela de horário: adicionar seletor de tipo e de janela (Manhã, Tarde, janela de 2h, horário livre), enviando saleType e timeWindow. Modal de cancelamento: aviso da regra de 2h para estabelecimento; justificativa obrigatória para coletor.
[ ] Testes: criar CollectionScheduleServiceTest (cria sem coletor; recusa ponto de outro estabelecimento; estabelecimento <2h bloqueado; coletor cancela a qualquer hora e exige justificativa; sem NullPointerException com coletor nulo), atualizar EndpointAuthorizationTest e schedules/page.test.tsx.
[ ] Teste manual: entrar como estabelecimento → criar ponto → agendar sem escolher coletor → cancelar com mais de 2h (ok) → tentar cancelar com menos de 2h (bloqueia).
[ ] Relatório: gravar RELATORIOS/RELATORIO_ETAPA2_US2_AGENDAMENTO_ESTABELECIMENTO.md no formato da seção "Relatório obrigatório" e mostrar o resumo na resposta.
Etapa 3 — US3: Acompanhamento de Status e Notificações Confiáveis

Critérios de aceite (US3): exibir os status agendada, atribuída, a caminho, concluída e cancelada; notificar o estabelecimento a cada mudança de status.

[x] Back-end: liberar os GET de agendamentos (/api/schedules, /list, /{id}, /date/{date}, /point/{pointId}) para ESTABLISHMENT, sempre filtrando no service pelos pontos cujo dono é o usuário logado.
[x] Notificações: trocar a busca do estabelecimento por e-mail de contato (findByEmail(point.getContactEmail())) pelo dono real (point.getEstablishment()) em confirmação, atribuição, a caminho e conclusão, e adicionar a notificação no cancelamento (estabelecimento e, se houver, coletor). Nunca assumir coletor não nulo.
[x] Front-end: em schedules/page.tsx e schedules/[id]/page.tsx, badges de status em português (tabela de referência acima), motivo do cancelamento e linha do tempo na tela de detalhe. Tratar 403/404 com mensagem clara.
[x] Testes: CollectionScheduleServiceTest (A não vê agendamento de B; vê os próprios; notificação dispara no cancelamento), EndpointAuthorizationTest e testes de front-end dos schedules.
[x] Teste manual: estabelecimento A agenda; estabelecimento B não vê o agendamento de A; mudar status como admin/coletor e conferir as notificações do A.
[x] Relatório: gravar RELATORIOS/RELATORIO_ETAPA3_US3_ACOMPANHAMENTO_STATUS.md no formato da seção "Relatório obrigatório" e mostrar o resumo na resposta.
Etapa 4 — US4: Coletas Disponíveis e Aceite Atômico

Critérios de aceite (US4): listar endereço, horário, volume estimado e nome do estabelecimento; filtrar por data e região; o coletor aceita a coleta e ela some da lista dos demais.

[ ] Back-end (lista): GET /api/schedules/available (COLLECTOR e ADMIN) com agendamentos PENDING e collector nulo, filtros opcionais por data (ou intervalo) e região (cidade, estado, bairro do ponto). Cada item traz endereço do ponto, horário, volume estimado e nome do estabelecimento (adicionar ao DTO se faltar).
[ ] Back-end (aceite): PUT /api/schedules/{id}/accept (COLLECTOR) com UPDATE condicional no repository (@Modifying @Query: define coletor e status CONFIRMED onde id = :id e status = 'PENDING' e collector nulo), conferindo as linhas afetadas. Se 0 linhas: 409 Conflict ("Esta coleta já foi aceita por outro coletor", confira o mapeamento no GlobalExceptionHandler). Notificar o estabelecimento dono no aceite. Não alterar o /confirm existente; citar no relatório se ficar redundante.
[ ] Front-end: em collector/page.tsx, aba "Coletas Disponíveis" com filtros de data e região, botão "Aceitar Coleta" com confirmação, tratamento do 409 (mensagem clara e recarregar a lista) e remoção imediata da coleta aceita. Se possível, atualização automática da lista a cada poucos segundos (meta da US: até 3s).
[ ] Testes: CollectionScheduleServiceTest (filtros; aceite normal; segundo aceite recebe conflito; só COLLECTOR aceita), EndpointAuthorizationTest e collector/page.test.tsx.
[ ] Teste manual: estabelecimento agenda → dois coletores veem a coleta → o primeiro aceita → o segundo tenta aceitar e recebe a mensagem de conflito, e a coleta some da lista.
[ ] Relatório: gravar RELATORIOS/RELATORIO_ETAPA4_US4_COLETAS_DISPONIVEIS.md no formato da seção "Relatório obrigatório" e mostrar o resumo na resposta.
Etapa 5 — US5: Confirmação Obrigatória na Pesagem e Trava Imediata

Critérios de aceite (US5): exigir peso/volume real; exigir confirmação do estabelecimento (código gerado pelo aplicativo) antes de finalizar; sinalizar discrepância entre estimado e real; não permitir edição após a conclusão.

[ ] Back-end: em CollectionRecordService.createRecord, não marcar mais o agendamento como COMPLETED ao registrar a pesagem (hoje marca, o que ignora a confirmação); ele permanece IN_PROGRESS.
[ ] Back-end (código): CollectionRecordDTO.confirmationCode só pode ser serializado para o ESTABLISHMENT dono e para ADMIN; para COLLECTOR vai nulo.
[ ] Back-end (conclusão): o endpoint de concluir passa a receber confirmationCode, valida contra o registro do agendamento (INVALID_CONFIRMATION_CODE se errado, reaproveitando a lógica de confirmCollectionByEstablishment), marca o registro como confirmado e só então coloca o agendamento em COMPLETED. Manter a detecção de discrepância e a notificação existentes.
[ ] Back-end (leitura): abrir leitura somente-leitura de registros para ESTABLISHMENT (GET /api/records/{id} e /list), filtrada pelo dono do ponto (SecurityConfig + @PreAuthorize).
[ ] Back-end (imutabilidade): registro/agendamento COMPLETED não pode ser alterado nem excluído (service e API), sem depender da verificação do ADMIN.
[ ] Front-end: collector/complete/[id]/page.tsx exige o código do estabelecimento (campo e erro claro); schedules/[id]/page.tsx e records/[id]/page.tsx mostram o código ao estabelecimento e escondem/desabilitam edição quando COMPLETED.
[ ] Testes: CollectionRecordServiceTest (concluir sem código ou com código errado falha; com código certo conclui; editar COMPLETED falha; coletor não recebe o código no DTO), EndpointAuthorizationTest e testes de front-end.
[ ] Teste manual: coletor registra a pesagem → o estabelecimento vê o código → coletor tenta concluir sem código (bloqueia) → conclui com o código → tenta editar (bloqueia).
[ ] Relatório: gravar RELATORIOS/RELATORIO_ETAPA5_US5_CONFIRMACAO_PESAGEM.md no formato da seção "Relatório obrigatório" e mostrar o resumo na resposta.
Etapa 6 — US7: Histórico, Total de Litros e Comprovante PDF

Critérios de aceite (US7): listar data, litros e coletor de cada coleta concluída; filtrar por período e mostrar o total acumulado de litros; gerar comprovante em PDF com código de verificação.

[ ] Back-end (dependência): adicionar com.github.librepdf:openpdf ao backend/pom.xml (versão estável atual).
[ ] Back-end (histórico): GET /api/records/history?startDate=&endDate= (ESTABLISHMENT: só registros dos seus pontos; ADMIN opcional) retornando a lista (data, litros, coletor) e totalLiters do período, sem alterar os endpoints existentes.
[ ] Back-end (PDF): alterar GET /api/records/{id}/certificate para devolver PDF real (application/pdf, Content-Disposition: attachment) para registro COMPLETED do próprio estabelecimento, sem exigir verificação do ADMIN. O PDF traz estabelecimento, CNPJ, coletor, data, volume real, tipo de óleo e um código de verificação (ex.: hash/HMAC curto de id + volume + data, que permita detectar adulteração), também exibido em tela.
[ ] Front-end: records/page.tsx e records/[id]/page.tsx para ESTABLISHMENT: filtros de data inicial e final, card "Total de litros no período" e botão "Baixar comprovante (PDF)" que baixa o arquivo.
[ ] Testes: CollectionRecordServiceTest (PDF começa com %PDF-; filtro por período; soma de litros; outro estabelecimento não acessa) e records/page.test.tsx.
[ ] Teste manual: após uma coleta concluída, o estabelecimento filtra por período, confere o total e baixa o PDF com o código.
[ ] Relatório: gravar RELATORIOS/RELATORIO_ETAPA6_US7_HISTORICO_PDF.md no formato da seção "Relatório obrigatório" e mostrar o resumo na resposta.
Validação entre uma etapa e outra

Antes de começar a próxima etapa, conferir se o relatório da anterior está em RELATORIOS/ e rodar:

Back-end: cd backend && ./mvnw test
Front-end: cd frontend && npm test -- --watchAll=false

E conferir manualmente no Docker (docker compose up) o fluxo da etapa, com a aba Rede do navegador aberta (F12) para ver se alguma chamada retorna 403, 404 ou 500.