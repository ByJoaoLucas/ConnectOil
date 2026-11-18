# Changelog: CRUD de Estabelecimento - Cadastro e Exclusão de Conta

**Branch:** `feature/crud-estabelecimento`  
**Data:** 17/11/2026  
**Autor:** kauaduartt

---

## 📋 Resumo das Mudanças

Implementação completa da user story de cadastro de estabelecimento, incluindo validações de negócio, segurança de senha e exclusão de conta própria.

### ✅ Critérios de Aceitação Atendidos

| Critério | Status | Implementação |
|----------|--------|---------------|
| Cadastrar nome, CNPJ/CPF, endereço, email, telefone | ✅ | `UserDTO` + endpoint `POST /api/auth/register` |
| Validar formato de e-mail | ✅ | `@Email` no DTO |
| Validar formato de CNPJ/CPF | ✅ | Regex `@Pattern` (11 dígitos CPF, 14 dígitos CNPJ) + validação no service |
| Bloquear e-mail duplicado | ✅ | `existsByEmail()` → erro `EMAIL_ALREADY_EXISTS` |
| Bloquear CNPJ/CPF duplicado | ✅ | `existsByCpf()` + `existsByCnpj()` → erros específicos |
| Senha segura (mín. 8 chars, 1 especial, 1 maiúscula, 1 número) | ✅ | Configurável via `SystemConfigService` (defaults atendem ao requisito) |
| Permitir exclusão da própria conta | ✅ | `DELETE /api/auth/account` (implementado anteriormente) |

---

## 🔧 Arquivos Modificados

### 1. `../backend/src/main/java/com/oilcollection/dto/UserDTO.java`

**Mudanças:**
- Removido `@NotBlank` do campo `cpf` (agora opcional para `ESTABLISHMENT`)
- Senha: `@Size(min = 8)` (era 6) — atende requisito mínimo
- Mantido `@Pattern` para CPF (`^\d{11}$`) e CNPJ (`^\d{14}$`)
- Campos de endereço do estabelecimento já existiam: `establishmentName`, `establishmentAddress`, `establishmentCity`, `establishmentState`, `establishmentZipCode`

### 2. `../backend/src/main/java/com/oilcollection/service/AuthService.java`

**Método `register()` reescrito com:**

```java
// Validação por role
if (role == User.UserRole.ESTABLISHMENT) {
    // Exige CNPJ válido (14 dígitos)
} else if (role == User.UserRole.COLLECTOR || role == User.UserRole.CLIENT) {
    // Exige CPF válido (11 dígitos)
}

// Verificações de duplicidade
existsByEmail()     → EMAIL_ALREADY_EXISTS
existsByCpf()       → CPF_ALREADY_EXISTS   (se CPF informado)
existsByCnpj()      → CNPJ_ALREADY_EXISTS  (se CNPJ informado)  ← NOVO

// Códigos de erro específicos
CNPJ_REQUIRED       // ESTABLISHMENT sem CNPJ
CPF_REQUIRED        // COLLECTOR/CLIENT sem CPF
INVALID_CNPJ        // Formato CNPJ inválido
INVALID_CPF         // Formato CPF inválido
```

### 3. `../backend/src/test/java/com/oilcollection/service/AuthServiceTest.java`

**6 novos testes adicionados:**

| Teste | Cenário Coberto |
|-------|-----------------|
| `register_Establishment_Success` | Cadastro completo de establishment com CNPJ, endereço, etc. |
| `register_CnpjAlreadyExists_ThrowsException` | CNPJ já cadastrado → `CNPJ_ALREADY_EXISTS` |
| `register_EstablishmentWithoutCnpj_ThrowsException` | ESTABLISHMENT sem CNPJ → `CNPJ_REQUIRED` |
| `register_CollectorWithoutCpf_ThrowsException` | COLLECTOR sem CPF → `CPF_REQUIRED` |
| `register_EstablishmentWithInvalidCnpjFormat_ThrowsException` | CNPJ com formato inválido → `INVALID_CNPJ` |
| `register_CollectorWithInvalidCpfFormat_ThrowsException` | CPF com formato inválido → `INVALID_CPF` |

---

## 🧪 Testes Executados

```bash
# Todos os 14 testes unitários de AuthServiceTest passam
AuthServiceTest: 14 testes ✅
```

---

## 🔐 Segurança de Senha

A complexidade da senha é configurável via banco de dados (tabela `system_config`):

| Chave | Default | Descrição |
|-------|---------|-----------|
| `password_min_length` | 8 | Comprimento mínimo |
| `password_require_uppercase` | true | Exige letra maiúscula |
| `password_require_lowercase` | true | Exige letra minúscula |
| `password_require_number` | true | Exige número |
| `password_require_special` | true | Exige caractere especial |

**Os defaults já atendem ao requisito da user story.**

---

## 📦 Endpoints Afetados

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| `POST` | `/api/api/auth/register` | Cadastro de collector ou establishment |
| `DELETE` | `/api/api/auth/account` | Exclusão da própria conta (soft delete) |

> **Observação:** A URL tem `/api` duplicado devido ao `context-path: /api` no `application.yml` + `@RequestMapping("/api/auth")` no controller.

---

## 🎯 Exemplos de Uso

### Cadastro de Estabelecimento
```bash
curl -X POST http://localhost:8080/api/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Posto Exemplo",
    "email": "contato@postoexemplo.com",
    "password": "Senha@123",
    "cnpj": "12345678000199",
    "phoneNumber": "+5511999999999",
    "role": "ESTABLISHMENT",
    "establishmentName": "Posto Exemplo Ltda",
    "establishmentAddress": "Av. Principal, 1000",
    "establishmentCity": "São Paulo",
    "establishmentState": "SP",
    "establishmentZipCode": "01234567"
  }'
```

### Cadastro de Coletor
```bash
curl -X POST http://localhost:8080/api/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "João Coletor",
    "email": "joao@coletor.com",
    "password": "Senha@123",
    "cpf": "12345678901",
    "phoneNumber": "+5511988887777",
    "role": "COLLECTOR"
  }'
```

### Exclusão da Própria Conta
```bash
curl -X DELETE http://localhost:8080/api/api/auth/account \
  -H "Authorization: Bearer <access_token>"
```

---

## 🔄 Próximos Passos Sugeridos

1. **Validação completa de CNPJ/CPF** (dígitos verificadores) — atualmente apenas formato (regex)
2. **Endpoint de atualização de perfil** (`PUT /api/auth/me`) para estabelecimento atualizar dados
3. **Testes de integração** com Testcontainers (requer Docker no CI)
4. **Documentação Swagger/OpenAPI** para os novos campos de establishment
5. **Remover duplicação `/api/api/`** ajustando `context-path` ou `@RequestMapping`

---

## 📝 Commits Relacionados

```bash
<commit-hash> feat: implement establishment registration (CRUD) with validation
<commit-hash> docs: add changelog for establishment CRUD feature
```