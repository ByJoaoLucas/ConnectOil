# Erros no Backend (Segurança, Regras de Negócio e Banco)

---

## ERR-01 [CRÍTICA] — Cadastro público permite criar usuário `ADMIN`
- **Local**: `backend/src/main/java/com/oilcollection/service/AuthService.java` (linhas 43–48)
- **Código atual**:
  ```java
  // TODO: remover ADMIN do cadastro público antes da entrega final
  private static final Set<User.UserRole> PUBLIC_REGISTRATION_ROLES = Set.of(
          User.UserRole.ESTABLISHMENT,
          User.UserRole.COLLECTOR,
          User.UserRole.ADMIN
  );
  ```
- **Impacto**: Qualquer pessoa na internet pode enviar `POST /api/auth/register` com `"role": "ADMIN"` e assumir controle administrativo total do sistema.
- **Correção**:
  ```java
  private static final Set<User.UserRole> PUBLIC_REGISTRATION_ROLES = Set.of(
          User.UserRole.ESTABLISHMENT,
          User.UserRole.COLLECTOR
  );
  ```

---

## ERR-02 [CRÍTICA] — Conflito entre `SecurityConfig.java` e `@PreAuthorize` nos Controllers
- **Local**: `backend/src/main/java/com/oilcollection/security/SecurityConfig.java` (linhas 86–98)
- **Problema**: O filtro HTTP do `SecurityConfig` restringe rotas antes mesmo de chegar ao `@PreAuthorize` dos métodos dos controllers, bloqueando funcionalidades legítimas:
  1. **`/api/records/**`**: `SecurityConfig` exige `hasAnyRole("ADMIN", "COLLECTOR")`, mas `CollectionRecordController` libera:
     - `PUT /api/records/{id}/confirm` para `ESTABLISHMENT` (confirmar coleta no estabelecimento)
     - `GET /api/records/{id}/certificate` para `ESTABLISHMENT` (emitir certificado)
     -> **Resultado**: Estabelecimentos recebem `403 Forbidden`.
  2. **`/api/incidents/**`**: `SecurityConfig` exige `hasAnyRole("ADMIN", "COLLECTOR")`, mas `IncidentController` libera `POST /api/incidents` para `ESTABLISHMENT`.
     -> **Resultado**: Estabelecimentos não conseguem abrir incidentes (`403 Forbidden`).
  3. **`/api/exchange-items/**`**: `SecurityConfig` exige `hasRole("ADMIN")`, mas `ExchangeItemController` libera `GET /api/exchange-items/schedule/{scheduleId}` para `COLLECTOR` e `ESTABLISHMENT`.
  4. **`/api/system-config/**`**: `SecurityConfig` exige `hasRole("ADMIN")`, mas `SystemConfigController` libera `GET /api/system-config/price/base-oil` para `COLLECTOR`.
  5. **`/api/collector-coverage/**`**: `SecurityConfig` exige `hasRole("ADMIN")`, mas `CollectorCoverageController` libera consultas (`/{id}`, `/collector/{collectorId}`, etc.) para `COLLECTOR`.
- **Correção recomendada no `SecurityConfig.java`**:
  Ajustar as regras de URL para refletir os perfis que acessam cada recurso (deixando o `@PreAuthorize` controlar a granularidade por endpoint):
  ```java
  .requestMatchers("/api/records/**").hasAnyRole("ADMIN", "COLLECTOR", "ESTABLISHMENT")
  .requestMatchers("/api/incidents/**").hasAnyRole("ADMIN", "COLLECTOR", "ESTABLISHMENT")
  .requestMatchers(HttpMethod.GET, "/api/exchange-items/**").hasAnyRole("ADMIN", "COLLECTOR", "ESTABLISHMENT")
  .requestMatchers("/api/exchange-items/**").hasRole("ADMIN")
  .requestMatchers(HttpMethod.GET, "/api/system-config/price/base-oil").hasAnyRole("ADMIN", "COLLECTOR")
  .requestMatchers("/api/system-config/**").hasRole("ADMIN")
  .requestMatchers(HttpMethod.GET, "/api/collector-coverage/**").hasAnyRole("ADMIN", "COLLECTOR")
  .requestMatchers("/api/collector-coverage/**").hasRole("ADMIN")
  ```

---

## ERR-03 [ALTA] — `NullPointerException` no `UserService` ao validar CPF nulo
- **Local**: `backend/src/main/java/com/oilcollection/service/UserService.java` (linhas 62 e 108)
- **Problema**: A migration `V4__make_cpf_nullable_in_users.sql` tornou `cpf` anulável (`DROP NOT NULL`) porque estabelecimentos usam `cnpj`. Porém, `UserService` ainda faz:
  ```java
  // Linha 62 (createUser):
  if (userRepository.existsByCpf(request.getCpf())) { ... }

  // Linha 108 (updateUser):
  if (!user.getCpf().equals(request.getCpf()) && userRepository.existsByCpf(request.getCpf())) { ... }
  ```
  Quando um administrador edita um usuário `ESTABLISHMENT` (cujo `user.getCpf()` no banco é `null`), `user.getCpf().equals(...)` lança **`NullPointerException` (HTTP 500)**.
- **Correção**:
  ```java
  // Em createUser:
  if (request.getCpf() != null && !request.getCpf().isBlank() && userRepository.existsByCpf(request.getCpf())) {
      throw new BusinessException("CPF already registered", "CPF_ALREADY_EXISTS");
  }

  // Em updateUser:
  if (request.getCpf() != null && !request.getCpf().isBlank()
          && !request.getCpf().equals(user.getCpf())
          && userRepository.existsByCpf(request.getCpf())) {
      throw new BusinessException("CPF already registered", "CPF_ALREADY_EXISTS");
  }
  ```

---

## ERR-04 [ALTA] — `JWT_SECRET` padrão exposto no repositório
- **Local**: `backend/src/main/resources/application.yml` (linha 58), `docker-compose.yml` (linha 26) e `.env.example` (linha 7)
- **Problema**: Existe uma chave Base64 fixa como fallback (`bXktc2VjcmV0LWtleS1mb3ItanN0LXRva2VuLWdlbmVyYXRpb24taW4tb2lsLWNvbGxlY3Rpb24tc3lzdGVt`). Se alguém subir em produção sem definir `JWT_SECRET`, qualquer atacante que conheça o repositório pode forjar tokens JWT válidos.
- **Correção**: Remover o fallback inseguro em produção (exigir `${JWT_SECRET}` obrigatório no profile `prod` e gerar uma chave aleatória forte via `openssl rand -base64 64`).

---

## ERR-11 [BAIXA] — `X-Content-Type-Options` desabilitado no Spring Security
- **Local**: `backend/src/main/java/com/oilcollection/security/SecurityConfig.java` (linha 71)
- **Código atual**:
  ```java
  .contentTypeOptions(contentTypeOptions -> contentTypeOptions.disable())
  ```
- **Correção**: Remover `.disable()` para manter o cabeçalho padrão `X-Content-Type-Options: nosniff` ativo também nas respostas diretas do backend (além do Nginx).
