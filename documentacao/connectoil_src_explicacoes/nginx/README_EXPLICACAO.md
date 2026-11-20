# Pasta: `ConnectOil-main/nginx` (Proxy Reverso e Terminação SSL/HTTPS)

## Visão Geral
Atua como **Proxy Reverso (Reverse Proxy)** e ponto único de entrada HTTP/HTTPS na camada de infraestrutura/produção do sistema **ConnectOil**. Roteia requisições externas para os containers internos `backend` (Spring Boot) e `frontend` (Next.js), além de gerenciar certificados TLS/SSL, cabeçalhos de segurança, compressão Gzip e cache de arquivos estáticos.

## Arquivos (1 arquivo)
- **`proxy.conf`**: Configuração completa dos servidores virtuais (`server`) e grupos de destino (`upstream`) do Nginx:
  - **Definição de Upstreams**:
    - `upstream backend`: aponta para o serviço Docker `backend:8080`.
    - `upstream frontend`: aponta para o serviço Docker `frontend:80` (observação: no `docker-compose.yml` atual o container Next.js expõe a porta `3000`, então em produção o upstream/porta deve estar alinhado com o proxy reverso utilizado).
  - **Redirecionamento HTTP -> HTTPS (Porta 80)**:
    - Escuta `api.seudominio.com` e `app.seudominio.com`.
    - Libera `/.well-known/acme-challenge/` em `/var/www/certbot` para renovação automática de certificados SSL via **Let's Encrypt / Certbot**.
    - Redireciona (`301`) todo o restante do tráfego HTTP para `https://`.
  - **Bloco da API Backend (`api.seudominio.com` — Porta 443 SSL/HTTP2)**:
    - Configura certificados (`/etc/nginx/ssl/fullchain.pem` e `privkey.pem`) com protocolos `TLSv1.2` e `TLSv1.3` e cifras seguras.
    - Injeta cabeçalhos de segurança HTTP (`X-Frame-Options: SAMEORIGIN`, `X-Content-Type-Options: nosniff`, `X-XSS-Protection`, `Referrer-Policy`).
    - **`location /api/`**: encaminha requisições para `http://backend`, preserva cabeçalhos originais do cliente (`Host`, `X-Real-IP`, `X-Forwarded-For`, `X-Forwarded-Proto`), habilita suporte a `Upgrade`/`Connection` (WebSockets/HTTP 1.1) e define timeouts de `60s` (`proxy_connect_timeout`, `proxy_send_timeout`, `proxy_read_timeout`).
    - **`location /api/actuator/health`**: restringe o endpoint de monitoramento de saúde do Spring Boot Actuator apenas para redes privadas internas (`10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`), bloqueando acesso externo (`deny all`).
  - **Bloco do Frontend (`app.seudominio.com` — Porta 443 SSL/HTTP2)**:
    - Aplica a mesma configuração TLS 1.2/1.3 e cabeçalhos de segurança.
    - Ativa compressão **Gzip** (`gzip on`, mínimo `1024` bytes) para respostas de texto, CSS, XML, JavaScript e JSON.
    - **`location /`**: encaminha as rotas da interface web para `http://frontend`.
    - **`location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$`**: aplica cache agressivo de 1 ano (`expires 1y` e `Cache-Control: public, immutable`) para assets estáticos.
