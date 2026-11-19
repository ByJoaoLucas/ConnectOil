# Pasta: `backend/src/main/java/com/oilcollection/exception`

## Visão Geral
Centraliza exceções de domínio e o tratamento global de erros da API REST, traduzindo falhas em respostas HTTP consistentes (status code, mensagem, timestamp/detalhes).

## Arquivos
- **`BusinessException.java`**: Exceção de tempo de execução (`RuntimeException`) para violações de regra de negócio.
- **`ResourceNotFoundException.java`**: Exceção (`RuntimeException`) para recursos inexistentes por ID/chave.
- **`GlobalExceptionHandler.java`**: Classe anotada com `@RestControllerAdvice` que intercepta exceções lançadas pelos controllers/services e formata as respostas de erro.
