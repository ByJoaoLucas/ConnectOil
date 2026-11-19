# Erros no Frontend (Rotas API, SSR, Testes e Ambiente)

---

## ERR-05 [ALTA] — Duplicação de `/api/api` e URL fixa no navegador (`frontend/src/lib/api.ts`)
- **Local**: `frontend/src/lib/api.ts` (linhas 5–12)
- **Código atual**:
  ```ts
  const getApiBaseUrl = () => {
    if (typeof window !== 'undefined') {
      // Browser: use host machine's localhost
      return 'http://localhost:8080/api/api';
    }
    // Server-side (SSR): use Docker network
    return `${process.env.NEXT_PUBLIC_API_URL || 'http://backend:8080/api'}/api`;
  };
  ```
- **Problemas**:
  1. No navegador, a URL ignora completamente `process.env.NEXT_PUBLIC_API_URL`, quebrando qualquer ambiente fora de `localhost:8080` (ex.: porta customizada ou domínio de produção).
  2. O backend tem `server.servlet.context-path: /api` e controllers com `@RequestMapping("/api/...")`, gerando URLs redundantes como `/api/api/auth/login`.
- **Correção**:
  - **Opção A (mantendo o backend atual sem alterar controllers)**:
    ```ts
    const getApiBaseUrl = () => {
      if (typeof window !== 'undefined') {
        const base = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api';
        return `${base}/api`;
      }
      return `${process.env.NEXT_PUBLIC_INTERNAL_API_URL || 'http://backend:8080/api'}/api`;
    };
    ```
  - **Opção B (mais limpa — recomendada)**: Remover `context-path: /api` do `application.yml` (já que todos os controllers já começam com `/api/...`) e usar `http://localhost:8080/api` diretamente.

---

## ERR-06 [ALTA] — Uso direto de `window.location.pathname` no render de `Layout.tsx`
- **Local**: `frontend/src/components/Layout.tsx` (linha 87)
- **Código atual**:
  ```tsx
  className={`flex items-center px-3 py-2.5 rounded-lg text-sm font-medium transition-colors ${
    window.location.pathname === item.href
      ? 'bg-primary-50 text-primary-700'
      : 'text-gray-600 hover:bg-gray-100 hover:text-gray-900'
  }`}
  ```
- **Problema**: Pode gerar `ReferenceError: window is not defined` durante pré-renderização SSR do Next.js ou divergência de hidratação (`Hydration mismatch`), além de não reagir a mudanças de rota do Next.js App Router.
- **Correção**: Usar o hook oficial `usePathname()` do `next/navigation`:
  ```tsx
  import { useRouter, usePathname } from 'next/navigation';
  // dentro do componente Layout:
  const pathname = usePathname();
  // na classe do Link:
  pathname === item.href
  ```

---

## ERR-07 [MÉDIA] — Nome inválido de chave no Jest (`frontend/jest.config.js`)
- **Local**: `frontend/jest.config.js` (linha 3)
- **Código atual**:
  ```js
  setupFilesAfterSetup: ['<rootDir>/jest.setup.js'],
  ```
- **Problema**: `setupFilesAfterSetup` não existe no Jest (o Jest emite aviso de opção desconhecida e ignora o arquivo `jest.setup.js`).
- **Correção**:
  ```js
  setupFilesAfterEnv: ['<rootDir>/jest.setup.js'],
  ```

---

## ERR-08 [MÉDIA] — Páginas autenticadas sem `layout.tsx` de proteção de rota
- **Local**: `frontend/src/app/` nas pastas:
  - `collector/page.tsx` e `collector/complete/[id]/page.tsx`
  - `incidents/page.tsx`
  - `products/page.tsx`
  - `collection-points/[id]/page.tsx`
  - `records/[id]/page.tsx`
  - `schedules/[id]/page.tsx`
- **Problema**: Enquanto `/dashboard`, `/users`, `/reports` e `/profile` usam um `layout.tsx` que valida `isAuthenticated` e redireciona para `/login`, essas sub-rotas não possuem `layout.tsx` próprio (caso não dependam de um wrapper interno consistente).
- **Correção**: Padronizar a proteção de rotas autenticadas via `middleware.ts` no Next.js ou um route group `(protected)/layout.tsx`.

---

## ERR-10 [BAIXA] — Erro de digitação na variável de ambiente do Google Maps
- **Local**: `.env.example` (linha 17) vs `frontend/src/app/collection-points/[id]/page.tsx` (linha 386)
- **Problema**:
  - `.env.example` define: `NEXT_PUBLIC_GOGLE_MAPS_API_KEY=` (com um `O` só).
  - O código frontend lê: `process.env.NEXT_PUBLIC_GOOGLE_MAPS_API_KEY`.
- **Correção**: Renomear no `.env.example` para `NEXT_PUBLIC_GOOGLE_MAPS_API_KEY=`.
