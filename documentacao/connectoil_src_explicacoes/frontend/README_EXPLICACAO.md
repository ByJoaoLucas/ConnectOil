# Pasta: `ConnectOil-main/frontend` (Raiz do Frontend)

## Visão Geral
Aplicação web frontend construída com **Next.js 14 (App Router)**, **React 18**, **TypeScript** e **Tailwind CSS**, responsável pela interface de operação e gestão do sistema ConnectOil (administração, pontos de coleta, agendamentos, execução de coletas em campo, registros, relatórios, incidentes e catálogo de troca).

## Arquivos de Configuração e Infraestrutura (10 arquivos)
- **`package.json`**: Define scripts (`dev`, `build`, `start`, `lint`, `test`) e dependências principais: `@tanstack/react-query` (cache e estado assíncrono), `axios` (cliente HTTP), `react-hook-form` + `zod` (formulários e validação), `lucide-react` (ícones), `recharts` (gráficos), `leaflet`/`react-leaflet` (mapas), `date-fns` (datas) e `react-hot-toast` (notificações visuais).
- **`next.config.js`**: Configura `reactStrictMode: true`, build `output: 'standalone'` (otimizado para Docker), domínios de imagem permitidos (`localhost`) e alias `@` apontando para `./src`.
- **`tsconfig.json`**: Configuração do compilador TypeScript em modo `strict`, resolução `bundler`, JSX `preserve` e mapeamento de caminho `@/* -> ./src/*`.
- **`tailwind.config.js`**: Configura os caminhos de varredura de classes CSS, a paleta de cores customizada (`primary` em tons de verde e `secondary` em tons de âmbar/laranja) e a fonte `Inter`.
- **`postcss.config.js`**: Pipeline PostCSS integrando `tailwindcss` e `autoprefixer`.
- **`jest.config.js` e `jest.setup.js`**: Configuração de testes unitários/componentes com Jest, ambiente `jsdom`, `ts-jest`, mapeamento `@/` e matchers do `@testing-library/jest-dom`.
- **`Dockerfile` e `.dockerignore`**: Build multi-estágio em Node 20 Alpine que injeta `NEXT_PUBLIC_API_URL` em tempo de build, gera o bundle standalone e executa `node server.js` com usuário não-root (`nextjs`).
- **`next-env.d.ts`**: Arquivo automático de tipos do Next.js para TypeScript.
