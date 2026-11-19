# Pasta: `frontend/src/app/register`

## Visão Geral
Rota pública `/register` para autocadastro de novos usuários (Estabelecimento ou Coletor).

## Arquivos (1 arquivo)
- **`page.tsx`**: Formulário com seleção de tipo de conta (`ESTABLISHMENT` ou `COLLECTOR`), validação condicional com Zod (CPF para coletor; CNPJ e dados do estabelecimento como nome, endereço, cidade, estado e CEP para estabelecimento), regras de senha forte (mínimo 8 caracteres, maiúscula, minúscula, número e caractere especial) e autenticação automática via `register()` do `AuthContext`.
