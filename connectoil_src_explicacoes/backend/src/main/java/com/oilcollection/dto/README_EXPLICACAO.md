# Pasta: `backend/src/main/java/com/oilcollection/dto`

## Visão Geral
Camada de **Data Transfer Objects** (e DTOs aninhados/de requisição/resposta). Isola os contratos de entrada e saída da API REST em relação às entidades JPA (`model`), evitando exposição direta de campos internos ou ciclos de serialização.

## Arquivos (15 DTOs)
- **`AuthRequestDTO.java`** e **`AuthResponseDTO.java`** (inclui `UserInfoDTO`): Credenciais de login/registro e payloads de token/usuário autenticado.
- **`CollectionPointDTO.java`**, **`CollectionRecordDTO.java`**, **`CollectionScheduleDTO.java`** (inclui `ExchangeItemDTO` interno): Representações de pontos, registros e agendamentos de coleta.
- **`CollectorCoverageDTO.java`** e **`CollectorPerformanceDTO.java`**: Dados de cobertura e desempenho de coletores.
- **`DailyVolumeDTO.java`** e **`OilTypeVolumeDTO.java`**: Agregações de volume coletado por dia e por tipo de óleo.
- **`ExchangeItemDTO.java`** (inclui `ExchangeItemRequestDTO`), **`IncidentDTO.java`**, **`NotificationDTO.java`**, **`ProductDTO.java`**, **`SystemConfigDTO.java`**, **`UserDTO.java`**: Payloads de troca de itens, ocorrências, notificações, produtos, configurações e usuários.
