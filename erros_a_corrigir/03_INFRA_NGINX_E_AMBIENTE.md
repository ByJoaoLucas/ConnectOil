# Erros de Infraestrutura, Nginx e Ambiente Local

---

## ERR-09 [MÉDIA] — Divergência de porta do frontend entre `nginx/proxy.conf` e `docker-compose.yml`
- **Local**: `nginx/proxy.conf` (linhas 6–8) vs `docker-compose.yml` (linhas 49–50)
- **Código atual (`nginx/proxy.conf`)**:
  ```nginx
  upstream frontend {
      server frontend:80;
  }
  ```
- **Problema**: No `frontend/Dockerfile` e no `docker-compose.yml`, o servidor Next.js (`node server.js`) escuta na porta **`3000`** (`ENV PORT=3000`), e não na porta `80`. Se o Nginx for conectado à rede Docker usando esse `proxy.conf`, todas as requisições para o frontend retornarão **`502 Bad Gateway`**.
- **Correção**:
  ```nginx
  upstream frontend {
      server frontend:3000;
  }
  ```

---

## Observações Adicionais para Rodar Localmente vs Produção
1. **Nginx não está declarado no `docker-compose.yml`**:
   - O `docker-compose.yml` sobe `postgres` (5432), `backend` (8080), `frontend` (3000) e `pgadmin` (5050).
   - O arquivo `nginx/proxy.conf` está preparado apenas como modelo para produção (com `api.seudominio.com`, `app.seudominio.com` e certificados SSL em `/etc/nginx/ssl/`).
2. **Para rodar em uma porta local sem erros**:
   - Suba via `docker compose up -d --build` e acesse:
     - Frontend: `http://localhost:3000`
     - Backend: `http://localhost:8080/api/api/...` (ou após corrigir o **ERR-05**, `http://localhost:8080/api/...`)
   - Se quiser unificar tudo em **uma única porta local** (ex.: `http://localhost:8088`) usando Nginx local sem SSL, basta criar um `nginx/local.conf` roteando `/api/` para `backend:8080` e `/` para `frontend:3000`.
