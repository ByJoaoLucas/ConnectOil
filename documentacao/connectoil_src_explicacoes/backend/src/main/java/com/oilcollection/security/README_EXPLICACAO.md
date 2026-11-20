# Pasta: `backend/src/main/java/com/oilcollection/security`

## Visão Geral
Implementa a segurança da aplicação com **Spring Security** e **JWT (JSON Web Token)**, controlando autenticação, validação de tokens, carregamento de usuários e regras de acesso às rotas.

## Arquivos
- **`SecurityConfig.java`**: Configuração central (`@Configuration`, `@EnableWebSecurity`) de cadeias de filtros, políticas de sessão/CORS e permissões de endpoints.
- **`JwtProperties.java`**: Mapeia propriedades JWT (`app.jwt.*`) a partir da configuração da aplicação.
- **`JwtTokenProvider.java`**: Gera, valida e extrai claims de tokens JWT.
- **`JwtAuthenticationFilter.java`**: Filtro (`OncePerRequestFilter`) que intercepta requisições HTTP, extrai o Bearer token e popula o `SecurityContextHolder`.
- **`CustomUserDetailsService.java`**: Implementa `UserDetailsService` para carregar dados de autenticação/autorização do usuário a partir da base.
