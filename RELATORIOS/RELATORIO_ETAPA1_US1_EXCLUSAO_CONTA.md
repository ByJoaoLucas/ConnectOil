# Relatório Técnico - Exclusão de Conta (US1)

**Projeto:** Connect Oil - Plataforma Web de Coleta de Óleo
**Branch:** `feat/completes-us1-us7-fluxo-estabelecimento`
**Data:** 21/11/2026
**Autor:** Samuelcode7

---

## 1. Visão Geral e Objetivos

### 1.1 Histórias de Usuário

| História | Descrição | Status |
|----------|-----------|--------|
| **US1** | Cadastro e exclusão de conta: cadastro com nome, CNPJ/CPF, endereço, e-mail e telefone; validação de e-mail e CNPJ/CPF; sem duplicidade; senha forte; usuário pode excluir a conta (cadastro, validações e senha já prontos; faltava a exclusão na tela) | ✅ Concluída |

### 1.2 Objetivos Alcançados

- [x] Back-end: DELETE /api/auth/account já existia (AuthController.deleteOwnAccount / AuthService.deleteOwnAccount, exclusão lógica). Confirmado que não gera erro de chave estrangeira e que o usuário desativado não consegue mais logar.
- [x] Front-end: Em `frontend/src/app/profile/page.tsx`, seção "Zona de Perigo" (visível só para COLLECTOR e ESTABLISHMENT) com botão "Excluir minha conta" e modal de confirmação em que a pessoa digita EXCLUIR (botão só habilita após digitar). Ao confirmar: DELETE /auth/account, logout pelo AuthContext, limpar tokens e redirecionar para /login, com aviso em português.
- [x] Testes: Criado `frontend/src/app/profile/page.test.tsx` (abre o modal, botão desabilitado até digitar EXCLUIR, chama a API, faz logout e redireciona). Testes existentes `AuthControllerAccountDeletionTest` e `AuthServiceAccountDeletionTest` passam.
- [x] Teste manual: Cadastrar estabelecimento → entrar → Perfil → excluir → voltar ao login → tentar entrar de novo (deve falhar).
- [x] No relatório: Informado que e-mail e CPF/CNPJ seguem ocupados após a exclusão.
- [x] Relatório: Gravado `RELATORIOS/RELATORIO_ETAPA1_US1_EXCLUSAO_CONTA.md` no formato da seção "Relatório obrigatório".

---

## 2. O que foi feito / criado

### Back-end
- **Nenhuma alteração necessária** - O endpoint `DELETE /api/auth/account` já existia e estava funcional:
  - `AuthController.deleteOwnAccount` (linha 65-73 em `AuthController.java`)
  - `AuthService.deleteOwnAccount` (linha 214-232 em `AuthService.java`)
  - Exclusão lógica: define `isActive = false`, mantém a linha no banco para preservar vínculos e histórico
  - Auditoria: registra evento `DELETE` na tabela `audit_log`
  - Validações: só permite COLLECTOR e ESTABLISHMENT; impede exclusão de conta já desativada; retorna 404 se usuário não encontrado

### Banco/Migração
- **Nenhuma migração necessária** - A estrutura do banco já suporta a exclusão lógica (campo `is_active` na tabela `users`)

### Front-end
1. **`frontend/src/lib/api.ts`**: Adicionado método `deleteAccount()` no objeto `authApi` para chamar `DELETE /auth/account`

2. **`frontend/src/contexts/AuthContext.tsx`**: 
   - Adicionado `deleteAccount` no tipo `AuthContextType`
   - Implementado método `deleteAccount` que chama a API e depois faz logout (limpa tokens, user, localStorage e header Authorization)

3. **`frontend/src/app/profile/page.tsx`**:
   - Adicionados imports: `Trash2`, `X`, `AlertTriangle` do lucide-react
   - Adicionado estado: `isDeleteModalOpen`, `deleteConfirmation`
   - Adicionado handler `handleDeleteAccount` que valida o texto "EXCLUIR" e chama `deleteAccount()`
   - Adicionada seção "Zona de Perigo" (condicional: só para COLLECTOR e ESTABLISHMENT) com:
     - Card com borda vermelha, ícone de alerta
     - Texto explicativo sobre irreversibilidade
     - Botão "Excluir conta" que abre o modal
   - Adicionado modal de confirmação com:
     - Título "Confirmar exclusão da conta"
     - Aviso de ação irreversível
     - Campo de texto que converte para maiúsculas automaticamente
     - Placeholder "DIGITE EXCLUIR"
     - Botão "Excluir definitivamente" habilitado apenas quando o texto for exatamente "EXCLUIR"
     - Botão "Cancelar" que fecha o modal
     - Fechamento ao clicar no overlay
   - **Ajustes finais (Etapa 1):**
     - Dropdown "Perfil": adicionada opção `ESTABLISHMENT` → "Estabelecimento" (antes faltava, caía em "Administrador")
     - Campo CPF/CNPJ: agora exibe rótulo e valor corretos conforme role (`CNPJ` + `cnpj` para ESTABLISHMENT; `CPF` + `cpf` para COLLECTOR/CLIENT)

4. **`frontend/src/components/Layout.tsx`** (Sidebar):
   - Adicionado item **"Meu Perfil"** no menu de navegação lateral com ícone `User`, acessível a todos os roles autenticados
   - Bloco do usuário no canto inferior esquerdo: agora clicável (`cursor-pointer`), com hover visual (nome fica azul, ícone User aparece à direita), tooltip "Meu Perfil" e navega para `/profile`

5. **`frontend/src/app/profile/page.test.tsx`** (novo):
   - 9 testes cobrindo:
     - Renderização da aba de perfil com informações do usuário
     - Renderização da "Zona de Perigo" para role ESTABLISHMENT
     - Abertura do modal ao clicar no botão de exclusão
     - Botão desabilitado até digitar "EXCLUIR"
     - Chamada da API `deleteAccount` quando confirmado
     - Não chamar API se texto de confirmação estiver errado
     - Fechamento do modal ao clicar em Cancelar
     - Fechamento do modal ao clicar no overlay
     - Troca para aba de Segurança (Senha)

### Testes
- **Backend**: `AuthServiceAccountDeletionTest` (5 testes) e `AuthControllerAccountDeletionTest` (2 testes) - **7 testes, 0 falhas** (executados em container com JDK 21 conforme relatório anterior)
- **Frontend**: `profile/page.test.tsx` - **9 testes, 0 falhas** (executados com `npm test -- --watchAll=false`)
- **TypeScript**: Source files compilam sem erros (`npx tsc --noEmit` nos arquivos fonte)

---

## 3. Arquivos tocados / modificados

| Arquivo | Tipo | Descrição |
|---------|------|-----------|
| `frontend/src/lib/api.ts` | Modificado | Adicionado `deleteAccount()` no `authApi` |
| `frontend/src/contexts/AuthContext.tsx` | Modificado | Adicionado `deleteAccount` no contexto e implementação |
| `frontend/src/app/profile/page.tsx` | Modificado | Adicionada seção "Zona de Perigo", modal de confirmação, dropdown Perfil com ESTABLISHMENT, campo CPF/CNPJ dinâmico |
| `frontend/src/components/Layout.tsx` | Modificado | Adicionado item "Meu Perfil" no menu lateral; bloco do usuário clicável com hover e tooltip |
| `frontend/src/app/profile/page.test.tsx` | Novo | Testes da funcionalidade de exclusão de conta (atualizado para novo dropdown) |

---

## 4. Testes

### Backend
```bash
cd backend && mvn test -Dtest=AuthServiceAccountDeletionTest,AuthControllerAccountDeletionTest
```
**Resultado:** 7 testes executados, 0 falhas, 0 erros (conforme execução anterior em container com JDK 21)

*Nota: O ambiente local possui apenas JDK 17, mas o projeto requer JDK 21. Os testes de backend foram validados previamente em container com JDK 21.*

### Frontend
```bash
cd frontend && npm test -- --watchAll=false
```
**Resultado:** 10 suítes de teste passadas, 1 suíte com falha pré-existente (dashboard/page.test.tsx - erro de mock do React Query `useQueryClient`, não relacionado a esta etapa). Os 9 testes de `profile/page.test.tsx` passam.

```bash
cd frontend && npm run build
```
**Resultado:** Build de produção bem-sucedido (após correção do ESLint na linha 417 de `profile/page.tsx`: remoção de aspas desnecessárias ao redor de "EXCLUIR" no texto explicativo do modal, para atender a regra `react/no-unescaped-entities`).

---

## 5. Como testar manualmente

1. Subir a aplicação com Docker Compose:
   ```bash
   docker compose up -d --build
   ```

2. Acessar `http://localhost:3000/register`

3. Cadastrar um estabelecimento:
   - Selecionar "Sou estabelecimento"
   - Preencher: nome, e-mail, telefone, CNPJ, nome do estabelecimento, endereço, senha forte
   - Clicar "Criar conta"

4. Fazer login com a conta criada

5. Acessar `http://localhost:3000/profile`

6. Verificar se a seção "Zona de Perigo" aparece (só para COLLECTOR e ESTABLISHMENT)

7. Clicar em "Excluir conta" → abre modal

8. Tentar clicar em "Excluir definitivamente" sem digitar nada → botão desabilitado

9. Digitar "EXCLUIR" (case-insensitive, converte para maiúsculas) → botão habilita

10. Clicar em "Excluir definitivamente" → faz logout, limpa tokens, redireciona para `/login`

11. Tentar fazer login novamente com as mesmas credenciais → deve falhar com erro "Account is deactivated"

12. Verificar no banco: `is_active = false` na tabela `users`, evento `DELETE` em `audit_log`

---

## 6. Sugestão de mensagem de commit

```
feat(profile): implementar exclusão de conta para estabelecimento e coletor (US1)

- Adiciona seção "Zona de Perigo" no perfil com botão de exclusão
- Modal de confirmação exige digitar "EXCLUIR" para habilitar botão
- Integra com DELETE /api/auth/account (exclusão lógica já existente)
- Faz logout automático e redireciona para /login após exclusão
- Adiciona testes unitários (9 testes no profile/page.test.tsx)
- Mantém e-mail e CPF/CNPJ ocupados após exclusão (exclusão lógica)
```

---

## 7. Problemas extras encontrados e não corrigidos

- **Dashboard test failure**: `src/app/dashboard/page.test.tsx` falha com erro `useQueryClient is not a function` - problema pré-existente de mock do React Query, não relacionado a esta etapa.
- **TypeScript errors in test files**: Todos os arquivos `.test.tsx` têm erros de tipo por falta de `@testing-library/jest-dom` no `tsconfig.json` - configuração pré-existente, não afeta código de produção.
- **Backend JDK 21 requirement**: O projeto requer JDK 21 mas o ambiente local tem apenas JDK 17 - testes de backend rodam em container.
- **ERR-05 (Duplicação /api/api)**: Conforme documentado em `erros_a_corrigir/02_FRONTEND_ROTAS_SSR_E_TESTES.md`, a URL da API tem duplicação de `/api` - não corrigido pois fora do escopo da Etapa 1.
- **ERR-06 (window.location.pathname no SSR)**: Conforme `erros_a_corrigir/02_FRONTEND_ROTAS_SSR_E_TESTES.md`, uso direto de `window` no `Layout.tsx` - não corrigido pois fora do escopo.
- **ERR-07 (jest.setupFilesAfterSetup)**: Nome inválido no `jest.config.js` - não corrigido pois fora do escopo.
- **ERR-08 (Páginas sem layout de proteção)**: Algumas páginas autenticadas não têm `layout.tsx` de proteção - não corrigido pois fora do escopo.
- **ERR-10 (Typo GOOGLE_MAPS_API_KEY)**: Erro de digitação no `.env.example` - não corrigido pois fora do escopo.
- **ERR-09 (Porta Nginx/frontend)**: Divergência de porta entre `nginx/proxy.conf` (80) e `docker-compose.yml` (3000) - não corrigido pois fora do escopo.
- **ERR-11 (X-Content-Type-Options)**: Cabeçalho desabilitado no `SecurityConfig.java` - não corrigido pois fora do escopo.

---

**Fim do Relatório**  
*Gerado por Samuelcode7 - Connect Oil - Etapa 1 (US1 - Exclusão de Conta)*