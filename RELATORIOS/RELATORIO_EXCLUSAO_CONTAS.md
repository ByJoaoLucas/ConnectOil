# Relatório: exclusão de contas de coletor e estabelecimento

**Data:** 17/11/2026  
**Escopo:** endpoints de conta no backend e teste funcional com PostgreSQL.

## Implementação

Foi adicionado `DELETE /api/auth/account`. A requisição exige autenticação e aceita somente usuários com perfil `COLLECTOR` ou `ESTABLISHMENT`. O usuário identificado pelo token só pode excluir a própria conta. A resposta de sucesso é `204 No Content`.

A operação desativa a conta (`is_active = false`) e registra uma auditoria `DELETE` da entidade `USER`. A linha é mantida para preservar vínculos e histórico de coletas, agendamentos e incidentes. O filtro JWT não autentica usuários inativos, as rotas de conta exigem autenticação e novas emissões de token são recusadas.

## Teste funcional

O backend e o PostgreSQL foram iniciados com Docker Compose, e a imagem do backend foi reconstruída com a implementação. Para cada perfil, foi feito cadastro pela API, seguido imediatamente pela exclusão autenticada.

| Perfil | Cadastro | Exclusão | Estado final no banco | Auditoria |
|---|---:|---:|---|---|
| `COLLECTOR` | `200 OK` | `204 No Content` | `is_active = false` | `CREATE`, `DELETE` |
| `ESTABLISHMENT` | `200 OK` | `204 No Content` | `is_active = false` | `CREATE`, `DELETE` |

IDs criados durante o teste:

- Coletor: `fbe3845b-1377-4431-b839-ef6773a2d08b`
- Estabelecimento: `d9cb051d-f85d-4df2-b848-77ab0a73598a`

Após excluir o coletor, uma tentativa de login retornou `400` com a mensagem `Account is deactivated`. A consulta ao banco confirmou os dois registros desativados e os quatro eventos de auditoria esperados.

Também foi feita uma chamada a `/api/auth/me` usando o access token emitido antes da exclusão. O backend recusou a chamada (`403`), confirmando que o token não continua autenticando a conta inativa.

## Verificação de compilação e testes

- A imagem Docker do backend foi compilada e iniciada com sucesso; o teste funcional foi executado contra essa instância.
- `mvn test` executado no ambiente local não chegou à compilação: o Maven tentou usar `--release 21`, mas o JDK local disponível não oferece essa versão (`release version 21 not supported`).
- Testes automatizados focados foram compilados e executados em um container Maven com JDK 21: `AuthServiceAccountDeletionTest`, `AuthControllerAccountDeletionTest` e `AuthServiceTest` — **15 testes, 0 falhas, 0 erros**.

## Endpoint

```http
DELETE /api/auth/account
Authorization: Bearer <access_token>
```

Não requer corpo. O endpoint administrativo existente `DELETE /api/users/{id}` permanece restrito ao administrador e continua desativando usuários.