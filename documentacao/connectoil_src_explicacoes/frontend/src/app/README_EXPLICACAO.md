# Pasta: `frontend/src/app` (App Router — Raiz)

## Visão Geral
Raiz do roteamento **App Router** do Next.js. Define o layout HTML global, estilos base, provedores de estado/cache/autenticação e o redirecionamento inicial da aplicação.

## Arquivos (4 arquivos)
- **`layout.tsx`**: `RootLayout` da aplicação. Carrega a fonte `Inter` (subset `latin`), define `lang="pt-BR"`, metadados (`Sistema de Coleta de Óleo`) e envolve todas as páginas com `<Providers>`.
- **`providers.tsx`**: Componente client (`'use client'`) que instancia o `QueryClient` do TanStack React Query (`staleTime` de 5 minutos, `retry: 1`, `refetchOnWindowFocus: false`), envolve a árvore no `AuthProvider` e configura o `<Toaster>` global (`react-hot-toast`).
- **`page.tsx`**: Página raiz (`/`). Executa redirecionamento automático no cliente (`router.push('/login')`) para a tela de autenticação.
- **`globals.css`**: Importa diretivas `@tailwind base/components/utilities` e define classes utilitárias reutilizáveis (`.btn`, `.btn-primary`, `.btn-secondary`, `.btn-outline`, `.btn-danger`, `.btn-ghost`, `.input`, `.card`, `.sidebar`, `.main-content`, `.spinner`).
