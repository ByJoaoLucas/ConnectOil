# 📋 Backlog

## V1 — Produto mínimo viável (MVP)

**Objetivo**

- Permitir que o cliente visualize o site e consiga cadastrar suas empresa para a coleta.
- Cadastro de clientes.
- Login e logout.
- Criação para coleta.
- Visualização de preços e funcionalidades.
- Armazenamento dos locais no banco de dados.

<details>
    <summary>📝 User Stories e Critérios de Aceitação</summary>

### **User Story 1 (Funcional)**

Eu, **como estabelecimento** do Connect Oil, **quero** poder cadastrar meus dados e informações de contato, **para que** eu possa criar uma conta e acessar os serviços oferecidos pela plataforma.

### **Critérios de Aceitação:**

- O sistema deve permitir que o estabelecimento insira seu nome, CNPJ/CPF, endereço, email e telefone.
- O sistema deve validar o formato do endereço de e-mail e do CNPJ/CPF antes de permitir o cadastro.
- O sistema não deve permitir cadastro com e-mail ou CNPJ/CPF ja cadastrado.
- O sistema deve permitir que o estabelecimento crie uma senha segura, com requisitos mínimos de complexidade (por exemplo, comprimento mínimo, inclusão de caracteres especiais).

### **Fonte: Time**

### **User Story 2 (Funcional)**

Eu, **como Estabelecimento** do Connect Oil, **quero** agendar uma coleta informando a quantidade aproximada de óleo, **para que** eu não precise procurar por conta própria quem recolha.

### **Critérios de Aceitação:**

- O sistema deve permitir informar data, faixa de horário, endereço e volume estimado em litros.
- O sistema não deve permitir agendamento em data passada nem volume menor ou igual a zero.
- O sistema deve criar a coleta com status "agendada".
- O sistema deve permitir cancelar a coleta até 2 horas antes do horário agendado.

### **Fonte: Time**

### **User Story 3 (Funcional)**

Eu, **como Estabelecimento**, **quero** acompanhar o status da minha coleta, **para que** eu possa me organizar e saber quando o óleo será recolhido.

### **Critérios de Aceitação:**

- O sistema deve exibir os status: agendada, atribuída, a caminho, concluída e cancelada.
- O sistema deve notificar o Estabelecimento a cada mudança de status.

### **Fonte: Time e IA**

### **User Story 4 (Funcional)**

Eu, **como Coletor**, **quero** visualizar as coletas agendadas na minha região, **para que** eu possa planejar minha rota.

### **Critérios de Aceitação:**

- O sistema deve listar endereço, horário, volume estimado e nome do Estabelecimento.
- O sistema deve permitir filtrar por data e região.
- O sistema deve permitir que o Coletor aceite uma coleta, removendo-a da lista dos demais Coletores.

### **Fonte: IA**

### **User Story 5 (Funcional)**

Eu, **como Coletor**, **quero** registrar a pesagem do óleo no momento da coleta, **para que** o volume descartado seja confiável e a pontuação seja justa.

### **Critérios de Aceitação:**

- O sistema deve exigir o peso/volume real para concluir a coleta.
- O sistema deve exigir a confirmação do Estabelecimento (código ou assinatura) antes de finalizar.
- O sistema deve sinalizar para revisão coletas em que a diferença entre o volume estimado e o real ultrapasse um limite definido.
- O sistema não deve permitir a edição da coleta após sua conclusão.

### **Fonte: IA**

### **User Story 6 (Funcional)**

Eu, **como Estabelecimento**, **quero** acumular pontos a cada descarte e trocá-los por vouchers, **para que** eu seja recompensado por descartar o óleo corretamente.

### **Critérios de Aceitação:**

- O sistema deve creditar os pontos automaticamente após a confirmação da coleta, uma única vez por coleta.
- O sistema deve calcular os pontos com base nos litros pesados e na regra de pontuação vigente.
- O sistema deve gerar um código único ao resgatar um voucher e debitar os pontos correspondentes.
- O sistema não deve permitir resgate com saldo de pontos insuficiente.

### **Fonte: Time**

### **User Story 7 (Funcional)**

Eu, **como Estabelecimento**, **quero** consultar meu histórico de descartes e meu impacto ambiental, **para que** eu acompanhe minha contribuição e possa comprovar boas práticas.

### **Critérios de Aceitação:**

- O sistema deve listar data, litros, pontos ganhos e Coletor de cada coleta concluída.
- O sistema deve permitir filtrar por período e exibir o total acumulado de litros.
- O sistema deve gerar comprovante de descarte em PDF com código de verificação.

### **Fonte: IA**

### **User Story 8 (Não Funcional - Segurança)**

Eu, **como usuário** do Connect Oil, **quero** que meus dados e acessos estejam protegidos, **para que** eu use a plataforma com confiança.

### **Critérios de Aceitação:**

- O sistema deve armazenar senhas com hash, nunca em texto puro.
- O sistema deve exigir token de autenticação válido em todas as rotas protegidas.
- O sistema deve restringir cada perfil apenas às telas e ações do seu papel.
- O sistema deve ser acessível somente via HTTPS.
- O sistema deve bloquear temporariamente a conta após 5 tentativas de login inválidas.

### **Fonte: Time e IA**

### **User Story 9 (Não Funcional - Usabilidade)**

Eu, **como Estabelecimento com pouca familiaridade com tecnologia**, **quero** uma interface simples e adaptada ao celular, **para que** eu consiga agendar uma coleta sem ajuda.

### **Critérios de Aceitação:**

- Um usuário novo deve conseguir agendar a primeira coleta em até 3 minutos, em teste com usuários reais.
- O sistema deve funcionar em telas de celular e nos navegadores atuais.

### **Fonte: Time**

### **User Story 10 (Não Funcional - Desempenho e Disponibilidade)**

Eu, **como usuário** do Connect Oil, **quero** que o sistema responda rápido e esteja disponível, **para que** eu não perca tempo nem coletas.

### **Critérios de Aceitação:**

- As telas principais devem carregar em até 4 segundos com 100 usuários simultâneos.
- O sistema deve ter disponibilidade mínima de 93% ao mês durante o piloto.

### **Fonte: Time**
</details>

## V2 —  Reformulação através de Persona

**Objetivo**

- Reformular os User Stories pela Persona, adicionando, removendo ou alterando.
- Cadastro dos dias  coletados nos restaurantes.
- Anotar a quantidade em cada dia coletado.
- Recebe um alerta quando estiver perto de encher o reservatório do restaurante.
- Notificar o uso do óleo, após a coleta, seus produtos derivados dele.


<details>
  <summary>📝 User Stories e Critérios de Aceitação</summary>

### **User Story 1 (Funcional)**

Eu, **como estabelecimento** do Connect Oil, **quero** poder cadastrar meus dados e informações de contato, **para que** eu possa criar uma conta e acessar os serviços oferecidos pela plataforma.

### **Critérios de Aceitação:**

- O sistema deve permitir que o estabelecimento insira seu nome, CNPJ/CPF, endereço, email e telefone.
- O sistema deve validar o formato do endereço de e-mail e do CNPJ/CPF antes de permitir o cadastro.
- O sistema não deve permitir cadastro com e-mail ou CNPJ/CPF ja cadastrado.
- O sistema deve permitir que o estabelecimento crie uma senha segura, com requisitos mínimos de complexidade (por exemplo, comprimento mínimo, inclusão de caracteres especiais).

### **Fonte: Time**

### **User Story 2 (Funcional)**

Eu, **como Estabelecimento** do Connect Oil, **quero** agendar uma coleta informando a quantidade aproximada de óleo, **para que** eu não precise procurar por conta própria quem recolha.

### **Critérios de Aceitação:**

- O sistema deve permitir informar data, faixa de horário, endereço e volume estimado em litros.
- O sistema não deve permitir agendamento em data passada nem volume menor ou igual a zero.
- O sistema deve criar a coleta com status "agendada".
- O sistema deve permitir cancelar a coleta até 2 horas antes do horário agendado.

### **Fonte: Time**

### **User Story 3 (Funcional)**

Eu, **como Estabelecimento**, **quero** acompanhar o status da minha coleta, **para que** eu possa me organizar e saber quando o óleo será recolhido.

### **Critérios de Aceitação:**

- O sistema deve exibir os status: agendada, atribuída, a caminho, concluída e cancelada.
- O sistema deve notificar o Estabelecimento a cada mudança de status.

### **Fonte: Time e IA**

### **User Story 4 (Funcional)**

Eu, **como Coletor**, **quero** visualizar as coletas agendadas na minha região, **para que** eu possa planejar minha rota.

### **Critérios de Aceitação:**

- O sistema deve listar endereço, horário, volume estimado e nome do Estabelecimento.
- O sistema deve permitir filtrar por data e região.
- O sistema deve permitir que o Coletor aceite uma coleta, removendo-a da lista dos demais Coletores.

### **Fonte: IA**

### **User Story 5 (Funcional)**

Eu, **como Coletor**, **quero** registrar a pesagem do óleo no momento da coleta, **para que** o volume descartado seja confiável e a pontuação seja justa.

### **Critérios de Aceitação:**

- O sistema deve exigir o peso/volume real para concluir a coleta.
- O sistema deve exigir a confirmação do Estabelecimento (código ou assinatura) antes de finalizar.
- O sistema deve sinalizar para revisão coletas em que a diferença entre o volume estimado e o real ultrapasse um limite definido.
- O sistema não deve permitir a edição da coleta após sua conclusão.

### **Fonte: IA**

### **User Story 6 (Funcional)**

Eu, **como Estabelecimento**, **quero** acumular pontos a cada descarte e trocá-los por vouchers, **para que** eu seja recompensado por descartar o óleo corretamente.

### **Critérios de Aceitação:**

- O sistema deve creditar os pontos automaticamente após a confirmação da coleta, uma única vez por coleta.
- O sistema deve calcular os pontos com base nos litros pesados e na regra de pontuação vigente.
- O sistema deve gerar um código único ao resgatar um voucher e debitar os pontos correspondentes.
- O sistema não deve permitir resgate com saldo de pontos insuficiente.

### **Fonte: Time**

### **User Story 7 (Funcional)**

Eu, **como Estabelecimento**, **quero** consultar meu histórico de descartes e meu impacto ambiental, **para que** eu acompanhe minha contribuição e possa comprovar boas práticas.

### **Critérios de Aceitação:**

- O sistema deve listar data, litros, pontos ganhos e Coletor de cada coleta concluída.
- O sistema deve permitir filtrar por período e exibir o total acumulado de litros.
- O sistema deve gerar comprovante de descarte em PDF com código de verificação.

### **Fonte: IA**

### **User Story 8 (Não Funcional - Segurança)**

Eu, **como usuário** do Connect Oil, **quero** que meus dados e acessos estejam protegidos, **para que** eu use a plataforma com confiança.

### **Critérios de Aceitação:**

- O sistema deve armazenar senhas com hash, nunca em texto puro.
- O sistema deve exigir token de autenticação válido em todas as rotas protegidas.
- O sistema deve restringir cada perfil apenas às telas e ações do seu papel.
- O sistema deve ser acessível somente via HTTPS.
- O sistema deve bloquear temporariamente a conta após 5 tentativas de login inválidas.

### **Fonte: Time e IA**

### **User Story 9 (Não Funcional - Usabilidade)**

Eu, **como Estabelecimento com pouca familiaridade com tecnologia**, **quero** uma interface simples e adaptada ao celular, **para que** eu consiga agendar uma coleta sem ajuda.

### **Critérios de Aceitação:**

- Um usuário novo deve conseguir agendar a primeira coleta em até 3 minutos, em teste com usuários reais.
- O sistema deve funcionar em telas de celular e nos navegadores atuais.

### **Fonte: Time**

### **User Story 10 (Não Funcional - Desempenho e Disponibilidade)**

Eu, **como usuário** do Connect Oil, **quero** que o sistema responda rápido e esteja disponível, **para que** eu não perca tempo nem coletas.

### **Critérios de Aceitação:**

- As telas principais devem carregar em até 4 segundos com 100 usuários simultâneos.
- O sistema deve ter disponibilidade mínima de 93% ao mês durante o piloto.

### **Fonte: Time**

### **User Story 11 (Funcional - Desempenho e Disponibilidade)**

Eu, **como Estabelecimento,** **quero** alertar o sistema quando o armazenamento de óleo estiver quase cheio, para dar tempo hábil para a coleta ser realizada.

### **Critérios de Aceitação:**

- O sistema recebe um alerta após o estabelecimento notificar sobre o quase preenchimento.
- A empresa vai dar prioridade para essas coletas.

### **Fonte: Time**
</details>

## V3  —  Revisão/Adição de requisitos não-funcionais do sistema

**Objetivo**

- Melhorar a qualidade do software.
- Reformular ou adicionar User Story Não Funcional.
- Melhorar o desenvolvimento do projeto.

<details>
  <summary>📝 User Stories e Critérios de Aceitação</summary>

### **User Story 1 (Funcional)**

Eu, **como estabelecimento** do Connect Oil, **quero** poder cadastrar meus dados e informações de contato, **para que** eu possa criar uma conta e acessar os serviços oferecidos pela plataforma.

### **Critérios de Aceitação:**

- O sistema deve permitir que o estabelecimento insira seu nome, CNPJ/CPF, endereço, email e telefone.
- O sistema deve validar o formato do endereço de e-mail e do CNPJ/CPF antes de permitir o cadastro.
- O sistema não deve permitir cadastro com e-mail ou CNPJ/CPF ja cadastrado.
- O sistema deve permitir que o estabelecimento crie uma senha segura, com requisitos mínimos de complexidade (por exemplo, comprimento mínimo, inclusão de caracteres especiais).

### **Fonte: Time**

### **User Story 2 (Funcional)**

Eu, **como Estabelecimento** do Connect Oil, **quero** agendar uma coleta informando a quantidade aproximada de óleo, **para que** eu não precise procurar por conta própria quem recolha.

### **Critérios de Aceitação:**

- O sistema deve permitir informar data, faixa de horário e volume estimado em litros.
- O sistema não deve permitir agendamento em data passada nem volume menor ou igual a zero.
- O sistema deve criar a coleta com status "agendada".
- O sistema deve permitir cancelar a coleta até 2 horas antes do horário agendado.

### **Fonte: Time**

### **User Story 3 (Funcional)**

Eu, **como Estabelecimento**, **quero** acompanhar o status da minha coleta, **para que** eu possa me organizar e saber quando o óleo será recolhido.

### **Critérios de Aceitação:**

- O sistema deve exibir os status: agendada, atribuída, a caminho, concluída e cancelada.
- O sistema deve notificar o Estabelecimento a cada mudança de status.

### **Fonte: Time e IA**

### **User Story 4 (Funcional)**

Eu, **como Coletor**, **quero** visualizar as coletas agendadas na minha região, **para que** eu possa planejar minha rota.

### **Critérios de Aceitação:**

- O sistema deve listar endereço, horário, volume estimado e nome do Estabelecimento.
- O sistema deve permitir filtrar por data e região.
- O sistema deve permitir que o Coletor aceite uma coleta, removendo-a da lista dos demais Coletores.

### **Fonte: IA**

### **User Story 5 (Funcional)**

Eu, **como Coletor**, **quero** registrar a pesagem do óleo no momento da coleta, **para que** o volume descartado seja confiável e a pontuação seja justa.

### **Critérios de Aceitação:**

- O sistema deve exigir o peso/volume real para concluir a coleta.
- O sistema deve exigir a confirmação do Estabelecimento (código ou assinatura) antes de finalizar.
- O sistema deve sinalizar para revisão coletas em que a diferença entre o volume estimado e o real ultrapasse um limite definido.
- O sistema não deve permitir a edição da coleta após sua conclusão.

### **Fonte: IA**

### **User Story 6 (Funcional)**

Eu, **como Estabelecimento**, **quero** vender o óleo usado para o coletor, ou troca-lo por produtos de limpeza, **para que** eu seja recompensado por descartar o óleo corretamente.

### **Critérios de Aceitação:**

- O sistema deve dar um valor base para a venda do litro do óleo (1L = R$ [Valor do litro de óleo]).
- O sistema deve calcular o valor da venda com base nos litros pesados.
- Em caso de troca por produtos, o sistema irá fornecer um valor com base no cálculo (Qta. de óleo x Valor do litro) e mostrará a lista de produtos e quantidades disponíveis para troca.
- O estabelecimento escolhe os produtos e a quantidade e solicita a coleta.
- O coletor deve trazer os produtos solicitados no momento da coleta.

### **Fonte: Time**

### **User Story 7 (Funcional)**

Eu, **como Estabelecimento**, **quero** consultar meu histórico de descartes e meu impacto ambiental, **para que** eu acompanhe minha contribuição e possa comprovar boas práticas.

### **Critérios de Aceitação:**

- O sistema deve listar data, litros e o coletor de cada coleta concluída.
- O sistema deve permitir filtrar por período e exibir o total acumulado de litros.
- O sistema deve gerar comprovante de descarte em PDF com código de verificação.

### **Fonte: IA**

### **User Story 8 (Não Funcional - Segurança)**

Eu, **como usuário** do Connect Oil, **quero** que meus dados e acessos estejam protegidos, **para que** eu use a plataforma com confiança.

### **Critérios de Aceitação:**

- O sistema deve armazenar senhas com hash, nunca em texto puro.
- O sistema deve exigir token de autenticação válido em todas as rotas protegidas.
- O sistema deve restringir cada perfil apenas às telas e ações do seu papel.
- O sistema deve ser acessível somente via HTTPS.
- O sistema deve bloquear temporariamente a conta após 5 tentativas de login inválidas.

### **Fonte: Time e IA**

### **User Story 9 (Não Funcional - Usabilidade)**

Eu, **como Estabelecimento** com pouca familiaridade com tecnologia, **quero** uma interface simples e fácil de usar, para que eu consiga agendar uma coleta sem ajuda.

### **Critérios de Aceitação:**

- Um usuário novo deve conseguir agendar a primeira coleta em até 3 minutos, em teste com usuários reais.
- Os textos, botões e mensagens de erro devem usar linguagem simples, sem termos técnicos.

### **Fonte: Time**

### **User Story 10 (Não Funcional - Desempenho e Disponibilidade)**

Eu, **como usuário** do Connect Oil, **quero** que o sistema responda rápido e esteja disponível, **para que** eu não perca tempo nem coletas.

### **Critérios de Aceitação:**

- As telas principais devem carregar em até 4 segundos com 100 usuários simultâneos.
- O sistema deve ter disponibilidade mínima de 93% ao mês durante o piloto.

### **Fonte: Time**

### **User Story 11 (Funcional - Desempenho e Disponibilidade)**

Eu, **como Estabelecimento,** **quero** alertar o sistema quando o armazenamento de óleo estiver quase cheio, para dar tempo hábil para a coleta ser realizada.

### **Critérios de Aceitação:**

- O sistema recebe um alerta após o estabelecimento notificar sobre o quase preenchimento.
- A empresa vai dar prioridade para essas coletas.

### **Fonte: Time**

### **User Story 12 (Não Funcional - Desempenho)**

Eu, **como Estabelecimento,** **quero** que as ações principais do sistema sejam concluídas rapidamente, para que eu consiga agendar e acompanhar coletas sem espera.

### **Critérios de Aceitação:**

- O sistema deve enviar a notificação de mudança de status em até 1 minuto após a mudança.
- O sistema deve gerar o comprovante de descarte em PDF em até 5 segundos.
- O sistema deve atualizar a lista de coletas disponíveis para o Coletor em até 5 segundos após uma coleta ser aceita por outro Coletor.

### **Fonte: Time**

### **User Story 13 (Não Funcional - Compatibilidade)**

Eu, **como Estabelecimento,** **quero** acessar o Connect Oil tanto pelo aplicativo quanto por uma versão web no computador, para que eu possa gerenciar minhas coletas pelo dispositivo que for mais conveniente.

### **Critérios de Aceitação:**

- O sistema deve funcionar nas duas últimas versões dos navegadores Chrome, Firefox, Edge e Safari.
- O aplicativo deve funcionar nas versões do Android e do iOS definidas pela equipe como mínimas.
- A versão web deve se adaptar a telas de celular, tablet e computador sem perda de funcionalidade.
- A mesma conta deve acessar os mesmos dados no aplicativo e na versão web, de forma sincronizada.

### **Fonte: Time**

### **User Story 14 (Não Funcional - Integridade)**

Eu, **como usuário do Connect Oil,** **quero** que os dados de coletas, pesagens e pontos sejam sempre corretos e não sofram alterações indevidas, para que eu possa confiar nos registros e comprovantes da plataforma.

### **Critérios de Aceitação:**

- O sistema deve garantir que a conclusão da coleta e a atribuição de pontos/valores ocorram juntas.
- O sistema deve impedir que a mesma coleta gere pontos ou pagamentos mais de uma vez.
- O sistema deve registrar um log de auditoria (quem, o quê e quando) de todas as alterações em coletas, pesagens e pontuações.
- O comprovante de descarte deve conter um código de verificação que permita confirmar sua autenticidade e detectar adulteração.
- O sistema deve realizar backup automático dos dados ao menos uma vez por dia, com possibilidade de restauração.
- O sistema deve validar os dados no servidor, mesmo que já tenham sido validados na interface.

### **Fonte: Time e IA**

### **User Story 15 (Não Funcional - Segurança ambiental)**

Eu, **como Estabelecimento, quero** que o sistema ajude a evitar vazamentos e descarte incorreto do óleo, para que minha atividade não contamine o solo e a água.

### **Critérios de Aceitação:**

- O sistema deve exibir orientações de armazenamento seguro do óleo durante o agendamento.
- O sistema deve permitir que o Estabelecimento ou o Coletor registre ocorrências de vazamento ou derramamento, vinculadas à coleta.
- O sistema deve permitir que somente Coletores com cadastro validado (documentos e dados do veículo) aceitem coletas.
- O sistema deve priorizar a notificação do Coletor quando houver alerta de armazenamento quase cheio (US11).

### **Fonte: Time**

</details>

## V4  —  Princípios, Estilos e Design

**Objetivo**

- Compreender o conceito de arquitetura de software, a importância de decisões de alto nível e os princípios que evitam a degradação do código.
- Analisar os principais padrões de organização de sistemas, comparando suas vantagens, desvantagens e cenários de aplicação.
- Aplicar técnicas de IA Generativa e Modelos de Linguagem (LLMs) para mapear requisitos funcionais (User Stories) em diagramas e especificações arquiteturais.


- Garantir que o backlog de User Stories + critérios de aceitação esteja bem-formado e sem ambiguidades antes do design.

<details>

 <summary>🗺️ Mapeamento de Componentes e Decisão de Estilo Arquitetural </summary>


**Objetivo**
- Atuar como um Architect Agent para mapear os grupos funcionais em componentes de software e sugerir o estilo arquitetural mais adequado.


**Arquitetura do Sistema — Connect Oil**


## 1. Mapeamento de Domínios para Componentes Arquiteturais

A estrutura funcional da aplicação foi dividida em componentes bem delimitados com responsabilidades claras:

* **Auth & Identity Service (`IAM`):** Serviço Core (REST API) responsável pela autenticação (JWT/OAuth2), controle de acesso baseado em papéis (RBAC), cadastro de usuários, validações de CPF/CNPJ e e-mail no backend, além da gestão do ciclo de vida da conta (incluindo exclusão e compliance com a LGPD).
* **Coleta & Schedule Service (`Agendamento & Coletas`):** Serviço Core (REST API / Event-Driven) encarregado da máquina de estados das coletas, aplicação das regras de agendamento e cancelamento (validação de janelas temporais), tratamento de alertas de reservatório cheio e ordenação da fila de prioridades.
* **Logistics & Dispatch Service (`Logística & Campo`):** Serviço Operacional (REST API / WebSocket) voltado às operações de campo dos coletores. Gerencia a listagem e filtros regionais/temporais de coletas disponíveis, o aceite concorrente com remoção imediata da visão de outros coletores e a validação documental/veicular do operador.
* **Valuation & Exchange Engine (`Benefícios & Venda`):** Motor de Regras (REST API) que mantém a tabela de preços de mercado do litro do óleo, calcula o valor de venda a partir da pesagem real e gerencia o catálogo e saldo de conversão para troca por produtos de limpeza.
* **ESG Audit & Certificate Service (`Histórico & Audit`):** Serviço de Suporte / Batch dedicado à consolidação de dados de descarte, geração assíncrona de comprovantes em PDF com código de verificação por hash e manutenção do log imutável de auditoria.
* **Notification Worker (`Transversal`):** Processador Assíncrono responsável por consumir eventos do sistema e disparar notificações (Push/E-mail) para estabelecimentos e coletores dentro do SLA estipulado (< 1 min).
* **API Gateway & Edge (`Transversal`):** Componente de Infraestrutura que provê ponto único de entrada, terminação TLS/HTTPS, roteamento de requisições, limitação de taxa (Rate Limiting) e proteção contra ataques de força bruta.

---

## 2. Estilo Arquitetural Recomendado

O estilo arquitetural recomendado para o Connect Oil é o **Monólito Modular Orientado a Eventos (Modular Monolith Event-Driven)** para as fases iniciais e de piloto, evoluindo de forma natural para **Microsserviços Event-Driven** conforme o volume de operações cresça.

### Visão Geral do Fluxo

As aplicações Web (React) e Mobile (React Native) comunicam-se exclusivamente via HTTPS/REST com o **API Gateway**. Requisições síncronas de cadastro, consulta logística e valoração de trocas são roteadas para os respectivos serviços (`Auth & Identity Service`, `Logistics & Field Service` e `Valuation & Exchange Engine`).

A máquina de estados operada pelo **Coleta & Schedule Core Engine** responde às requisições síncronas e publica as transições de status no **Event Message Bus** (RabbitMQ ou Apache Kafka). Componentes como o **Notification Worker** e o **ESG & Audit Service** atuam como consumidores assíncronos desses eventos, processando notificações push/e-mail e gerando documentos sem inflar o tempo de resposta das requisições principais.

### Justificativa da Arquitetura

1. **Desacoplamento e Notificações Assíncronas (Pub/Sub):** Mudanças de status (`coleta.agendada`, `coleta.urgente`, `coleta.concluida`) geram eventos no barramento de mensagens. Isso permite que o Gateway responda em menos de 2,0 segundos ao usuário enquanto notificações e PDFs são gerados em segundo plano.
2. **Consistência Transacional (ACID Local):** Requisitos críticos como o encerramento da coleta exigem que a gravação do peso real, a geração de pontos/créditos e a trava de edição ocorram juntas. O uso de um Monólito Modular sobre um banco relacional (PostgreSQL) permite garantir transações ACID locais sem recorrer à complexidade de transações distribuídas (Padrão Saga) na fase de lançamento.
3. **Escalabilidade Direcionada:** A separação em módulos com fronteiras bem definidas permite isolar e escalar independentemente os componentes com maior carga de leitura (como a listagem de coletas disponíveis para coletores) e de processamento de fundo (como o worker de notificações).

---

## 3. Interfaces Públicas (Contratos de API) e Dependências

### Contratos de API RESTful

#### 1. Auth & Identity Service (`/api/v1/auth`)

* `POST /auth/register` — Cadastra estabelecimento ou coletor com validação formal de dados e documentos.
* `POST /auth/login` — Autentica o usuário e retorna o token de acesso (JWT).
* `DELETE /users/me` — Solicita a exclusão da conta do usuário.

#### 2. Coleta & Schedule Service (`/api/v1/coletas`)

* `POST /coletas` — Cria uma nova solicitação de coleta (tipo venda ou troca, volume estimado, data e janela).
* `POST /coletas/{id}/alerta-cheio` — Registra alerta de armazenamento quase cheio para priorização na fila.
* `PATCH /coletas/{id}/cancelar` — Cancela a coleta validando o tempo limite (2h antes) ou a justificativa do coletor.
* `GET /coletas/{id}/status` — Retorna o estado atual e o histórico de transições da coleta.

#### 3. Logistics & Dispatch Service (`/api/v1/logistica`)

* `GET /logistica/coletas-disponiveis?regiao={regiao}&data={data}` — Consulta coletas disponíveis por região e data.
* `POST /logistica/coletas/{id}/aceitar` — Coletor aceita a coleta (aplica *optimistic locking* para remoção imediata da visão dos demais).
* `POST /logistica/coletas/{id}/concluir` — Registra a pesagem real e valida o código de confirmação (OTP) ou assinatura para finalizar o descarte.

#### 4. Valuation & Exchange Engine (`/api/v1/cotacao`)

* `GET /cotacao/valor-litro` — Retorna o valor base atual do litro de óleo de mercado.
* `GET /cotacao/produtos-troca` — Lista os produtos de limpeza e as regras de conversão disponíveis.
* `POST /cotacao/calcular` — Calcula o valor financeiro ou o saldo de produtos com base no volume real pesado.

#### 5. ESG Audit & Certificate Service (`/api/v1/esg`)

* `GET /esg/historico?inicio={data}&fim={data}` — Retorna o histórico de descartes e o volume total acumulado.
* `GET /esg/comprovante/{coletaId}/download` — Gera e disponibiliza o download do comprovante em PDF.
* `GET /esg/comprovante/validar/{hash}` — Endpoint público para verificação da autenticidade e integridade do comprovante.

---

### Mapeamento de Dependências entre Componentes

* **API Gateway $\rightarrow$ Todos os Serviços:** Comunicação síncrona via HTTPS/REST para roteamento de tráfego externamente exposto.
* **Logistics & Dispatch Service $\rightarrow$ Auth & Identity Service:** Chamada síncrona interna para validação de perfis (RBAC) e status de homologação de coletores.
* **Logistics & Dispatch Service $\rightarrow$ Valuation & Exchange Engine:** Chamada síncrona interna para cálculo final de trocas e valoração no momento da pesagem.
* **Coleta & Schedule Service $\rightarrow$ Event Message Bus:** Publicação assíncrona de eventos de alteração de status.
* **Event Message Bus $\rightarrow$ Notification Worker:** Consumo assíncrono para envio de notificações Push e E-mail.
* **Event Message Bus $\rightarrow$ ESG Audit & Certificate Service:** Consumo assíncrono para disparo da geração do comprovante e gravação de logs de auditoria.
* **ESG Audit & Certificate Service $\rightarrow$ PostgreSQL Primary DB:** Acesso direto para gravação do histórico e leitura de dados auditáveis.
</details> 

<details>

 <summary>📍 Agrupamento Semântico e Tópicos Funcionais </summary>


**Objetivo**
- Agrupar user stories por proximidade semântica para definir os domínios/módulos funcionais do sistema.

## 1. Domínio: Gestão de Identidade, Autenticação e Controle de Acesso (IAM)

Concentra os fluxos de ciclo de vida do usuário, mecanismos de autenticação, autorização de perfis e requisitos fundamentais de segurança da informação.

* **Histórias Pertencentes:**
* `US1` (Cadastro de Estabelecimento e Gestão de Conta)
* `US8` (Segurança, Criptografia, Token e Bloqueio)


* **Conceitos-Chave:**
* **Identidade & Perfis (RBAC):** Diferenciação estrita de perfis (*Estabelecimento* vs. *Coletor*).
* **Validação de Documentos/Dados:** Formatação e unicidade de CPF/CNPJ e e-mail no backend.
* **Autenticação Segura & Autorização:** Criptografia de senhas (Hash/Salt), uso de tokens sem estado (JWT/OAuth2), tráfego seguro via HTTPS/TLS.
* **Políticas de Acesso & Proteção:** Regras de complexidade de senha, bloqueio temporário por força bruta e direito ao esquecimento/exclusão de conta (LGPD).



---

## 2. Domínio: Operações de Agendamento e Gestão de Coletas

Responsável por orquestrar todo o ciclo de vida de uma solicitação de coleta, desde sua criação até as regras de priorização e cancelamento.

* **Histórias Pertencentes:**
* `US2` (Agendamento de Coleta e Regras de Cancelamento)
* `US3` (Acompanhamento e Mudanças de Status)
* `US11` (Alerta de Armazenamento Quase Cheio e Priorização)
* `US15` (Prevenção de Incidentes e Qualificação de Operadores)


* **Conceitos-Chave:**
* **Máquina de Estados da Coleta:** Transição formal de status (`Agendada` $\rightarrow$ `Atribuída` $\rightarrow$ `A Caminho` $\rightarrow$ `Concluída` ou `Cancelada`).
* **Janelas de Agendamento & Regras Operacionais:** Validação de volumes mínimos ($>0$), restrição a datas futuras e cálculo da janela limite para cancelamento pelo cliente (até 2h antes).
* **Fila de Prioridade em Tempo Real:** Mecanismo de transbordo/urgência para recipientes quase cheios (destaque visual no topo da lista).
* **Compliance Operacional:** Restrição de aceite apenas para coletores com documentação/veículo homologados e registro de incidentes/vazamentos.



---

## 3. Domínio: Logística, Distribuição e Execução de Campo

Contempla as ferramentas voltadas ao Coletor para descoberta geotemporal de demandas, aceite de solicitações e execução física no local.

* **Histórias Pertencentes:**
* `US4` (Visualização, Filtros e Aceite de Coleta)
* `US5` (Registro de Pesagem Real, Confirmação e Bloqueio de Adulteração)


* **Conceitos-Chave:**
* **Concorrência & Bloqueio Exclusivo (Optimistic/Pessimistic Locking):** Remoção imediata da coleta da visão dos demais coletores assim que aceita por um operador.
* **Filtros Geográficos e Temporais:** Roteamento de coletas por região e data.
* **Proof of Delivery (PoD) & Confirmação Bilateral:** Fechamento de coleta mediante medição do volume real e validação síncrona pelo Estabelecimento (OTP/Código PIN ou Assinatura).
* **Tratamento de Anomalias:** Algoritmo de tolerância a desvios (Volume Estimado vs. Volume Real) e sinalização para revisão de auditores.
* **Imutabilidade Pós-Execução:** Trava de alteração de dados após a transição para o status `Concluída`.



---

## 4. Domínio: Benefícios, Valoração Econômica e Troca de Mercadorias

Gerencia as regras financeiras, valoração monetária do óleo recolhido e o motor de trocas por itens de limpeza.

* **Histórias Pertencentes:**
* `US6` (Valoração do Óleo, Cálculo de Venda e Modelo de Troca por Produtos)


* **Conceitos-Chave:**
* **Tabela Dinâmica de Preços:** Definição e aplicação da taxa de mercado base por litro (L = XR$).
* **Motor de Troca/Conversão:** Regra de cálculo do saldo disponível (Volume Real \Preço Base) e catálogo de produtos de limpeza disponíveis para resgate.
* **Manifesto de Entrega Combinada:** Vínculo formal entre a coleta de óleo e a lista de itens físicos que o coletor deve transportar/entregar no ato.



---

## 5. Domínio: Histórico, Impacto Ambiental e Auditabilidade

Focado no consolidado de dados, transparência operacional, métricas ESG e auditoria de transações.

* **Histórias Pertencentes:**
* `US7` (Histórico de Descartes e Emissão de Comprovante PDF)
* `US14` (Integridade Transacional, Logs de Auditoria e Backup)


* **Conceitos-Chave:**
* **Integridade Transacional (ACID):** Atomicidade entre a conclusão da pesagem e a atribuição de créditos/pagamentos (evitando *double-spending* ou duplo crédito).
* **Comprovante de Descarte Certificado:** Geração automática de relatórios em PDF contendo *hash* ou código de verificação para auditorias ambientais.
* **Audit Log (Trilha de Auditoria):** Registro imutável de eventos (*Who, What, When*) para alterações críticas.
* **Resiliência de Dados:** Estratégia de backup diário automatizado e política de recuperação de desastres (DRP).



---

## 6. Arquitetura Transversal: Requisitos Não-Funcionais, Experiência (UX) e Engenharia de Plataforma

Engloba as diretrizes técnicas que garantem desempenho, desacoplamento de notificações, usabilidade e suporte a múltiplas plataformas.

* **Histórias Pertencentes:**
* `US9` (Usabilidade e Simplicidade de Interface)
* `US10` (Disponibilidade, Latência e Tempo de Resposta)
* `US12` (Desempenho e Reatividade do Sistema)
* `US13` (Cross-Platform, Responsividade e Sincronização)


* **Conceitos-Chave:**
* **Arquitetura Orientada a Eventos (Pub/Sub):** Disparo assíncrono de notificações via Push/E-mail com SLA $<1\text{ min}$ para mudanças de status.
* **Acordo de Nível de Serviço (SLA/SLO):** Métricas de disponibilidade ($99,0\%$ a $99,5\%$) e tempos limite de resposta ($<2,0\text{s}$ para requisições do dashboard; $<3,0\text{s}$ para sincronização de listas; $<5,0\text{s}$ para PDFs).
* **Experiência do Usuário (UX/Accessibility):** Interfaces fluidas com fluxos orientados a tarefas simples (onboarding/agendamento $<3\text{ min}$).
* **Sincronização Multi-Dispositivo:** Design responsivo (Web/Mobile) consumindo a mesma camada de APIs REST/GraphQL integradas a um repositório centralizado.

</details>


<details>

  <summary>📝 User Stories e Critérios de Aceitação</summary>


**Objetivo**
- Garantir que o backlog de User Stories + critérios de aceitação esteja bem-formado e sem ambiguidades antes do design.


### **User Story 1 (Funcional)**

Eu, **como estabelecimento** do Connect Oil, **quero** poder cadastrar meus dados e informações de contato, **para que** eu possa criar uma conta e acessar os serviços oferecidos pela plataforma.

### **Critérios de Aceitação:**

- O sistema deve permitir que o estabelecimento insira seu nome, CNPJ/CPF, endereço, email e telefone.
- O sistema deve validar o formato do endereço de e-mail e do CNPJ/CPF antes de permitir o cadastro.
- O sistema não deve permitir cadastro com e-mail ou CNPJ/CPF ja cadastrado.
- O sistema deve permitir que o estabelecimento crie uma senha segura, com requisitos mínimos de complexidade (mínimo de 8 caracteres, 1 caractere especial, 1 letra maiúscula e 1 número).
- O sistema deve permir que o usuário exclua a conta criada.

### **Fonte: Time**

### **User Story 2 (Funcional)**

Eu, **como Estabelecimento** do Connect Oil, **quero** agendar uma coleta informando a quantidade aproximada de óleo, **para que** eu não precise procurar por conta própria quem recolha.

### **Critérios de Aceitação:**

- O sistema deve permitir informar data, faixa de horário e volume estimado em litros(ex: janelas fixas de 2 horas, turnos de Manhã/Tarde ou horário livre).
- O sistema não deve permitir agendamento em data passada nem volume menor ou igual a zero.
- O sistema deve criar a coleta com status "agendada".
- O sistema deve permitir selecionar o tipo de agendamento(venda ou troca).
- O sistema deve permitir cancelar a coleta até 2 horas antes do horário agendado.
- O sistema deve permitir que o coletor cancele a coleta caso ocorra algum imprevisto(necessitando de uma justificação).

### **Fonte: Time**

### **User Story 3 (Funcional)**

Eu, **como Estabelecimento**, **quero** acompanhar o status da minha coleta, **para que** eu possa me organizar e saber quando o óleo será recolhido.

### **Critérios de Aceitação:**

- O sistema deve exibir os status: agendada, atribuída, a caminho, concluída e cancelada.
- O sistema deve notificar o Estabelecimento a cada mudança de status.

### **Fonte: Time e IA**

### **User Story 4 (Funcional)**

Eu, **como Coletor**, **quero** visualizar as coletas agendadas na minha região, **para que** eu possa planejar minha rota.

### **Critérios de Aceitação:**

- O sistema deve listar endereço, horário, volume estimado e nome do Estabelecimento.
- O sistema deve permitir filtrar por data e região.
- O sistema deve permitir que o Coletor aceite uma coleta, removendo-a da lista dos demais Coletores.

### **Fonte: IA**

### **User Story 5 (Funcional)**

Eu, **como Coletor**, **quero** registrar a pesagem do óleo no momento da coleta, **para que** o volume descartado seja confiável e a pontuação seja justa.

### **Critérios de Aceitação:**

- O sistema deve exigir o peso/volume real para concluir a coleta.
- O sistema deve exigir a confirmação do Estabelecimento (código(gerado pelo aplicativo) ou assinatura) antes de finalizar.
- O sistema deve sinalizar para revisão coletas em que a diferença entre o volume estimado e o real ultrapasse um limite definido(ajustando o valor e retornando uma mensagem ao responsável).
- O sistema não deve permitir a edição da coleta após sua conclusão.
- O sistema deve sinalizar um erro caso o valor informado no agendamento da coleta seja diferente em grande ou pequena escala, o usuário/estabelecimento será notificado

### **Fonte: IA**

### **User Story 6 (Funcional)**

Eu, **como Estabelecimento**, **quero** vender o óleo usado para o coletor, ou troca-lo por produtos de limpeza, **para que** eu seja recompensado por descartar o óleo corretamente.

### **Critérios de Aceitação:**

- O sistema deve dar um valor base(mercado base) para a venda do litro do óleo (1L = R$ [Valor do litro de óleo]).
- O sistema deve calcular o valor da venda com base nos litros pesados.
- Em caso de troca por produtos, o sistema irá fornecer um valor com base no cálculo (Qta. de óleo x Valor do litro) e mostrará a lista de produtos e quantidades disponíveis para troca.
- O estabelecimento escolhe os produtos e a quantidade e solicita a coleta.
- O coletor deve trazer os produtos solicitados no momento da coleta.

### **Fonte: Time**

### **User Story 7 (Funcional)**

Eu, **como Estabelecimento**, **quero** consultar meu histórico de descartes e meu impacto ambiental, **para que** eu acompanhe minha contribuição e possa comprovar boas práticas.

### **Critérios de Aceitação:**

- O sistema deve listar data, litros e o coletor de cada coleta concluída.
- O sistema deve permitir filtrar por período e exibir o total acumulado de litros.
- O sistema deve gerar comprovante de descarte em PDF com código de verificação.

### **Fonte: IA**

### **User Story 8 (Não Funcional - Segurança)**

Eu, **como usuário** do Connect Oil, **quero** que meus dados e acessos estejam protegidos, **para que** eu use a plataforma com confiança.

### **Critérios de Aceitação:**

- O sistema deve armazenar senhas com hash, nunca em texto puro.
- O sistema deve exigir token de autenticação válido em todas as rotas protegidas.
- O sistema deve restringir cada perfil apenas às telas e ações do seu papel.
- O sistema deve ser acessível somente via HTTPS.
- O sistema deve bloquear temporariamente a conta após 5 tentativas de login inválidas.

### **Fonte: Time e IA**

### **User Story 9 (Não Funcional - Usabilidade)**

Eu, **como Estabelecimento** com pouca familiaridade com tecnologia, **quero** uma interface simples e fácil de usar, para que eu consiga agendar uma coleta sem ajuda.

### **Critérios de Aceitação:**

- Um usuário novo deve conseguir agendar a primeira coleta em até 3 minutos, em teste com usuários reais.
- Os textos, botões e mensagens de erro devem usar linguagem simples, sem termos técnicos.

### **Fonte: Time**

### **User Story 10 (Não Funcional - Desempenho e Disponibilidade)**

Eu, **como usuário** do Connect Oil, **quero** que a plataforma seja estável, responsiva e com alta disponibilidade, **para que** eu consiga realizar e acompanhar minhas coletas sem falhas operacionais.

### **Critérios de Aceitação:**

- O sistema deve manter disponibilidade mínima de 99,0% ao mês durante a fase de piloto e 99,5% em produção.
- As telas de Dashboard, Agendamento e Lista de Coletas devem responder em até 2,0 segundos para 95% das requisições com até 100 usuários simultâneos.
- A lista de coletas disponíveis para Coletores deve ser atualizada em até 3 segundos após qualquer atribuição ou aceite.
- O comprovante de descarte em PDF deve ser gerado e disponibilizado para download em até 5 segundos.
- As notificações de mudança de status devem ser disparadas ao cliente em no máximo 1 minuto após a alteração no sistema.

### **Fonte: Time**

### **User Story 11 (Funcional)**

Eu, **como Estabelecimento,** **quero** alertar o sistema quando o armazenamento de óleo estiver quase cheio, para dar tempo hábil para a coleta ser realizada.

### **Critérios de Aceitação:**

- O sistema recebe um alerta após o estabelecimento notificar sobre o quase preenchimento.
- A empresa vai dar prioridade(exibi-las no topo da lista do coletor com destaque visual e notificação push imediata) para essas coletas.

### **Fonte: Time**

### **User Story 12 (Não Funcional - Desempenho)**

Eu, **como Estabelecimento,** **quero** que as ações principais do sistema sejam concluídas rapidamente, para que eu consiga agendar e acompanhar coletas sem espera.

### **Critérios de Aceitação:**

- O sistema deve emitir uma notificação (push/e-mail) para o Estabelecimento a cada mudança no status da coleta. (US03)
- O sistema deve disponibilizar o botão de download do comprovante em PDF assim que a coleta for alterada para o status. (US07)
- O sistema deve remover automaticamente da visualização de outros coletores qualquer coleta que tenha sido aceita por um Coletor. (US04)

### **Fonte: Time**

### **User Story 13 (Não Funcional - Compatibilidade)**

Eu, **como Estabelecimento,** **quero** acessar o Connect Oil tanto pelo aplicativo quanto por uma versão web no computador, para que eu possa gerenciar minhas coletas pelo dispositivo que for mais conveniente.

### **Critérios de Aceitação:**

- O sistema deve funcionar nas duas últimas versões dos navegadores Chrome, Firefox, Edge e Safari.
- O aplicativo deve funcionar nas versões do Android e do iOS definidas pela equipe como mínimas(ainda sem definição).
- A versão web deve se adaptar a telas de celular, tablet e computador sem perda de funcionalidade.
- A mesma conta deve acessar os mesmos dados no aplicativo e na versão web, de forma sincronizada.

### **Fonte: Time**

### **User Story 14 (Não Funcional - Integridade)**

Eu, **como usuário do Connect Oil,** **quero** que os dados de coletas, pesagens e pontos sejam sempre corretos e não sofram alterações indevidas, para que eu possa confiar nos registros e comprovantes da plataforma.

### **Critérios de Aceitação:**

- O sistema deve garantir que a conclusão da coleta e a atribuição de pontos/valores ocorram juntas.
- O sistema deve impedir que a mesma coleta gere pontos ou pagamentos mais de uma vez.
- O sistema deve registrar um log de auditoria (quem, o quê e quando) de todas as alterações em coletas, pesagens e pontuações.
- O comprovante de descarte deve conter um código de verificação que permita confirmar sua autenticidade e detectar adulteração.
- O sistema deve realizar backup automático dos dados ao menos uma vez por dia, com possibilidade de restauração.
- O sistema deve validar os dados no servidor, mesmo que já tenham sido validados na interface.

### **Fonte: Time e IA**

### **User Story 15 (Funcional)**

Eu, **como Estabelecimento, quero** que o sistema ajude a evitar vazamentos e descarte incorreto do óleo, para que minha atividade não contamine o solo e a água.

### **Critérios de Aceitação:**

- O sistema deve exibir orientações de armazenamento seguro do óleo durante o agendamento.
- O sistema deve permitir que o Estabelecimento ou o Coletor registre ocorrências de vazamento ou derramamento, vinculadas à coleta.
- O sistema deve permitir que somente Coletores com cadastro validado (documentos e dados do veículo) aceitem coletas.
- O sistema deve priorizar a notificação do Coletor quando houver alerta de armazenamento quase cheio (US11).

### **Fonte: Time**

</details>


<details>


 <summary>📊 Geração de Diagramas UML (PlantUML / Mermaid)</summary>

<br>

### Diagrama de Componentes (UML 2.5)

![Diagrama de Componentes](utils/diagrama_componentes.png)

📄 Código-fonte PlantUML: [diagrama_componentes.puml](utils/diagrama_componentes.puml)

#### Como ler o diagrama

- **Estilo:** Monólito Modular Orientado a Eventos. Os cinco módulos de negócio ficam em um único deploy; o Event Message Bus e o Notification Service rodam como processos separados.
- **Cores:** azul = núcleo (Core), verde = operação de campo, amarelo = regras de valor, lilás = histórico e auditoria, cinza = infraestrutura comum, laranja = fila de mensagens.
- **Interfaces (círculo):** são os contratos de API expostos por cada módulo. A linha tracejada indica dependência (interface requerida) e a linha sólida indica fluxo ou interface fornecida.
- **Chamadas internas:** Logistics → Auth, Logistics → Coleta e Coleta → Valuation são chamadas em memória (in-process), sem HTTP.
- **Eventos:** a máquina de estados da coleta publica os eventos no Bus, e o Notification Service e o ESG os consomem de forma assíncrona (catálogo de eventos ao lado do Bus).

<details>
<summary>🔎 Material de apoio A — Versão detalhada com os contratos de API (endpoints)</summary>

<br>

![Diagrama de Componentes detalhado](utils/diagrama_componentes_detalhado.png)

📄 Código-fonte: [diagrama_componentes_detalhado.puml](utils/diagrama_componentes_detalhado.puml)

</details>

<details>
<summary>🔄 Material de apoio B — Estados da coleta</summary>

<br>

![Estados da coleta](utils/estados_coleta.png)

📄 Código-fonte: [estados_coleta.puml](utils/estados_coleta.puml)

</details>

<details>
<summary>🧭 Material de apoio C — Jornada do Estabelecimento</summary>

<br>

![Jornada do Estabelecimento](utils/jornada_estabelecimento.png)

📄 Código-fonte: [jornada_estabelecimento.puml](utils/jornada_estabelecimento.puml)

</details>

<details>
<summary>🚚 Material de apoio D — Jornada do Coletor</summary>

<br>

![Jornada do Coletor](utils/jornada_coletor.png)

📄 Código-fonte: [jornada_coletor.puml](utils/jornada_coletor.puml)

</details>

</details>


## V5  —  Seleção de Prioridade e Story Point

**Objetivo**

- Atualizar o Product Backlog no repositório (como GitHub/GitLab) para dar transparência ao que precisa ser feito, registrando o valor de negócio (prioridade do cliente) e o esforço técnico (story points).
- Aplicar a técnica de estimativa colaborativa vista em aula para medir a complexidade e o esforço real de cada User Story (US), evitando chutes individuais.
- Focar nas funcionalidades essenciais primeiro para garantir que o núcleo mais importante do sistema funcione antes, reduzindo riscos do projeto.

<details>


<summary>📊 Tabela de Prioridade e Story Point </summary>



| ID da User Story | Prioridade de acordo <br/>com o cliente | Story Point definido com o Planning <br/>Poker (esforço e complexidade <br/>de implementação) |
| :--- | :---|:----|
| US01 | Essencial | 5 |
| US02 | Essencial | 5 |
| US03 | Essencial | 3 |
| US04 | Essencial | 5 |
| US05 | Essencial | 8 |
| US06 | Importante | 13 |
| US07 | Essencial | 5 |
| US08 | Importante | 8 |
| US09 | Importante | 5 |
| US10 | Importante | 13 |
| US11 | Importante | 3 |
| US12 | Importante | 3 |
| US13 | Desejável | 13 |
| US14 | Importante | 13 |
| US15 | Importante | 8 |


</details>
