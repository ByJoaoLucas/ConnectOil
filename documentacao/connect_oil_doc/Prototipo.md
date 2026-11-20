# 📱 Protótipo

## USs Mais Importantes & BDD

<details>
<summary>User Stories Escolhidas: US2, US3, US6, US7, US11</summary>

### User Story 2 (Funcional)

Eu, **como Estabelecimento** do Connect Oil, **quero** agendar uma coleta informando a quantidade aproximada de óleo, **para que** eu não precise procurar por conta própria quem recolha.

**Critérios de Aceitação:**

- O sistema deve permitir informar data, faixa de horário e volume estimado em litros.
- O sistema não deve permitir agendamento em data passada nem volume menor ou igual a zero.
- O sistema deve criar a coleta com status "agendada".
- O sistema deve permitir cancelar a coleta até 2 horas antes do horário agendado.

**Fonte:** Time

---

### User Story 3 (Funcional)

Eu, **como Estabelecimento**, **quero** acompanhar o status da minha coleta, **para que** eu possa me organizar e saber quando o óleo será recolhido.

**Critérios de Aceitação:**

- O sistema deve exibir os status: agendada, atribuída, a caminho, concluída e cancelada.
- O sistema deve notificar o Estabelecimento a cada mudança de status.

**Fonte:** Time e IA

---

### User Story 6 (Funcional)

Eu, **como Estabelecimento**, **quero** vender o óleo usado para o coletor, ou trocá-lo por produtos de limpeza, **para que** eu seja recompensado por descartar o óleo corretamente.

**Critérios de Aceitação:**

- O sistema deve dar um valor base para a venda do litro do óleo (1L = R$ [valor do litro de óleo]).
- O sistema deve calcular o valor da venda com base nos litros pesados.
- Em caso de troca por produtos, o sistema irá fornecer um valor com base no cálculo (Qtd. de óleo × Valor do litro) e mostrará a lista de produtos e quantidades disponíveis para troca.
- O estabelecimento escolhe os produtos e a quantidade e solicita a coleta.
- O coletor deve trazer os produtos solicitados no momento da coleta.

**Fonte:** Time

---

### User Story 7 (Funcional)

Eu, **como Estabelecimento**, **quero** consultar meu histórico de descartes e meu impacto ambiental, **para que** eu acompanhe minha contribuição e possa comprovar boas práticas.

**Critérios de Aceitação:**

- O sistema deve listar data, litros e o coletor de cada coleta concluída.
- O sistema deve permitir filtrar por período e exibir o total acumulado de litros.
- O sistema deve gerar comprovante de descarte em PDF com código de verificação.

**Fonte:** IA

---

### User Story 11 (Funcional - Desempenho e Disponibilidade)

Eu, **como Estabelecimento**, **quero** alertar o sistema quando o armazenamento de óleo estiver quase cheio, **para** dar tempo hábil para a coleta ser realizada.

**Critérios de Aceitação:**

- O sistema recebe um alerta após o estabelecimento notificar sobre o quase preenchimento.
- A empresa vai dar prioridade para essas coletas.
---
</details>

<details>
<summary>Cenários BDD</summary>

### User Story 2 (Funcional)

```gherkin
Funcionalidade: Agendamento de coleta de óleo

  Cenário: Agendar coleta com sucesso
    Dado que o estabelecimento está no sistema Connect Oil
    Quando informar uma data futura, uma faixa de horário, um endereço e um volume estimado de óleo maior que zero
    E confirmar o agendamento
    Então o sistema deve criar a coleta
    E definir o status como "agendada"

  Cenário: Impedir agendamento em data passada
    Dado que o estabelecimento está na tela de agendamento
    Quando informar uma data passada
    Então o sistema deve impedir o agendamento
    E exibir uma mensagem informando que a data é inválida

  Cenário: Impedir volume de óleo inválido
    Dado que o estabelecimento está na tela de agendamento
    Quando informar um volume de óleo menor ou igual a zero
    Então o sistema deve impedir o agendamento
    E exibir uma mensagem informando que o volume deve ser maior que zero

  Cenário: Cancelar coleta dentro do prazo permitido
    Dado que o estabelecimento possui uma coleta agendada
    E faltam mais de 2 horas para o horário da coleta
    Quando solicitar o cancelamento
    Então o sistema deve cancelar a coleta

  Cenário: Impedir cancelamento fora do prazo
    Dado que o estabelecimento possui uma coleta agendada
    E faltam menos de 2 horas para o horário da coleta
    Quando solicitar o cancelamento
    Então o sistema deve impedir o cancelamento
    E informar que o prazo permitido foi ultrapassado

  Cenário: Permitir cancelamento exatamente 2 horas antes
    Dado que o estabelecimento possui uma coleta agendada
    E faltam exatamente 2 horas para o horário da coleta
    Quando solicitar o cancelamento
    Então o sistema deve permitir o cancelamento
```

---

### User Story 3 (Funcional)

```gherkin
Funcionalidade: Acompanhar status da coleta de óleo

  Cenário: Visualizar o status da coleta
    Dado que o estabelecimento possui uma coleta cadastrada
    Quando acessar os detalhes da coleta
    Então o sistema deve exibir o status atual da coleta
    E o status deve ser agendada, atribuída, a caminho, concluída ou cancelada

  Cenário: Notificar mudança de status da coleta
    Dado que o estabelecimento possui uma coleta agendada
    Quando o status da coleta for alterado
    Então o sistema deve enviar uma notificação ao estabelecimento
    E a notificação deve informar o novo status da coleta

  Cenário: Acompanhar coleta atribuída
    Dado que o estabelecimento possui uma coleta cadastrada
    Quando o coletor aceitar a coleta
    Então o sistema deve atualizar o status para "atribuída"
    E notificar o estabelecimento sobre a mudança

  Cenário: Acompanhar coleta a caminho
    Dado que a coleta está com status "atribuída"
    Quando o coletor informar que está a caminho
    Então o sistema deve atualizar o status para "a caminho"
    E notificar o estabelecimento

  Cenário: Confirmar conclusão da coleta
    Dado que a coleta está com status "a caminho"
    Quando o coletor confirmar a conclusão do recolhimento
    Então o sistema deve atualizar o status para "concluída"
    E notificar o estabelecimento

  Cenário: Notificar cancelamento da coleta
    Dado que o estabelecimento possui uma coleta cadastrada
    Quando a coleta for cancelada
    Então o sistema deve atualizar o status para "cancelada"
    E notificar o estabelecimento sobre o cancelamento
```

---

### User Story 6 (Não Funcional)

```gherkin
Funcionalidade: Pontuação e resgate de dinheiro ou produtos

  Cenário: Creditar pontos após a confirmação da coleta
    Dado que o estabelecimento possui uma coleta agendada
    E a coleta foi confirmada como concluída
    E os litros de óleo foram pesados
    Quando o sistema processar a pontuação da coleta
    Então deve calcular os pontos conforme a regra vigente
    E creditar os pontos na conta do estabelecimento

  Cenário: Impedir crédito duplicado de pontos
    Dado que o estabelecimento já recebeu os pontos de uma coleta
    Quando o sistema processar novamente a mesma coleta
    Então não deve creditar os pontos novamente
    E deve manter o saldo correto de pontos

  Cenário: Calcular pontos conforme os litros pesados
    Dado que a coleta foi concluída
    E o volume de óleo pesado foi registrado
    E existe uma regra de pontuação vigente
    Quando o sistema calcular os pontos
    Então deve utilizar os litros pesados
    E aplicar a regra de pontuação vigente

  Cenário: Resgatar pontos por dinheiro
    Dado que o estabelecimento possui pontos suficientes para solicitar uma conversão em dinheiro
    E existe uma regra de conversão vigente
    Quando solicitar o resgate dos pontos em dinheiro
    Então o sistema deve calcular o valor correspondente
    E debitar os pontos utilizados do saldo
    E registrar a solicitação de pagamento
    E informar ao estabelecimento que a solicitação foi realizada

  Cenário: Impedir resgate em dinheiro com saldo insuficiente
    Dado que o estabelecimento não possui pontos suficientes para converter em dinheiro
    Quando solicitar o resgate dos pontos
    Então o sistema deve impedir a solicitação
    E informar que o saldo de pontos é insuficiente
    E manter o saldo de pontos inalterado

  Cenário: Resgatar pontos por produtos
    Dado que o estabelecimento possui pontos suficientes para resgatar um produto
    E o produto está disponível para resgate
    Quando solicitar o resgate do produto
    Então o sistema deve confirmar a disponibilidade do produto
    E debitar os pontos correspondentes ao produto
    E registrar o pedido de resgate
    E confirmar a solicitação ao estabelecimento

  Cenário: Impedir resgate de produtos sem pontos suficientes
    Dado que o estabelecimento não possui pontos suficientes para resgatar um produto
    Quando solicitar o resgate do produto
    Então o sistema deve impedir o resgate
    E informar que o saldo de pontos é insuficiente
    E manter o saldo de pontos inalterado

  Cenário: Impedir resgate de produtos indisponíveis
    Dado que o estabelecimento possui pontos suficientes para resgatar um produto
    E o produto está indisponível no catálogo
    Quando solicitar o resgate do produto
    Então o sistema deve impedir o resgate
    E informar que o produto está indisponível
    E manter o saldo de pontos inalterado

  Cenário: Consultar histórico de pontos e resgates
    Dado que o estabelecimento possui movimentações de pontos
    Quando acessar o histórico de pontuação e resgates
    Então o sistema deve exibir os pontos recebidos por coleta
    E apresentar os pontos utilizados em resgates
    E informar os valores convertidos em dinheiro ou os produtos solicitados
    E exibir o saldo atual de pontos disponíveis
```

---

### User Story 7 (Funcional)

```gherkin
Funcionalidade: Consultar histórico de descartes e impacto ambiental

  Cenário: Visualizar histórico de coletas concluídas
    Dado que o estabelecimento possui coletas concluídas
    Quando acessar o histórico de descartes
    Então o sistema deve listar cada coleta concluída
    E exibir a data, os litros descartados, os pontos ganhos e o coletor responsável

  Cenário: Filtrar histórico por período
    Dado que o estabelecimento possui coletas concluídas
    Quando selecionar uma data inicial e uma data final
    Então o sistema deve exibir apenas as coletas concluídas dentro do período selecionado
    E apresentar o total acumulado de litros descartados nesse período

  Cenário: Gerar comprovante de descarte em PDF
    Dado que o estabelecimento possui uma coleta concluída
    Quando solicitar a geração do comprovante de descarte
    Então o sistema deve gerar um arquivo PDF
    E incluir os dados da coleta no comprovante
    E gerar um código de verificação único

  Cenário: Consultar histórico sem registros
    Dado que o estabelecimento não possui coletas concluídas
    Quando acessar o histórico de descartes
    Então o sistema deve informar que não existem registros
    E não deve exibir dados de coletas inexistentes

  Cenário: Filtrar histórico sem resultados
    Dado que o estabelecimento possui um histórico de descartes
    Quando selecionar um período sem coletas concluídas
    Então o sistema deve informar que não existem coletas concluídas no período selecionado
    E exibir o total de litros igual a zero
```

---

### User Story 11 (Funcional - Desempenho e Disponibilidade)

```gherkin
Funcionalidade: Alerta de quase preenchimento

  Cenário: Receber alerta de quase preenchimento
    Dado que o usuário possui um recipiente de óleo cadastrado
    Quando informar ao sistema que o recipiente está quase cheio
    Então o sistema deve registrar a notificação
    E enviar um alerta à empresa responsável

  Cenário: Priorizar usuários que notificaram o quase preenchimento
    Dado que a empresa recebeu um alerta de quase preenchimento
    Quando consultar as solicitações de coleta
    Então o sistema deve identificar a solicitação como prioritária
    E destacar essa solicitação para a empresa

  Cenário: Impedir alerta sem notificação do usuário
    Dado que o usuário não informou que o recipiente está quase cheio
    Quando a empresa consultar as solicitações de coleta
    Então o sistema não deve gerar um alerta de quase preenchimento
    E não deve atribuir prioridade com base nesse critério
```

</details>

---

## Protótipo inicial

Baseado nas USs escolhidas:

![PrototipoCO.png](utils/PrototipoCO.png)

## Simulação interativa

Simulação gerada pela IA mostrando o funcionamento do nosso software:

[Connect Oil — Protótipo interativo.html](utils/Connect%20Oil%20%E2%80%94%20Prot%C3%B3tipo%20interativo.html)
