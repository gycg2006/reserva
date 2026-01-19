# Microsserviço de Reservas

Microsserviço desenvolvido em Spring Boot para gerenciamento de reservas de veículos, com integração ao serviço de frota para verificação de disponibilidade.

## 📋 Índice

- [Pré-requisitos](#pré-requisitos)
- [Executando com Docker](#executando-com-docker) 🐳
- [Configuração Manual](#configuração-manual)
- [Executando o Projeto](#executando-o-projeto)
- [Funcionalidades](#funcionalidades)
- [Testando as Funcionalidades](#testando-as-funcionalidades)
- [Validações Implementadas](#validações-implementadas)
- [Tratamento de Erros](#tratamento-de-erros)
- [Estrutura do Projeto](#estrutura-do-projeto)

## Pré-requisitos

- **Java 17** ou superior
- **Maven 3.6+**
- **Docker** e **Docker Compose** (opcional, mas recomendado)
- **PostgreSQL 12+** (ou use Docker)
- **Serviço de Frota** configurado via variável de ambiente `FROTA_SERVICE_URL` (para verificação de disponibilidade)

---

## 🐳 Executando com Docker

A forma mais fácil de executar o projeto é usando Docker Compose, que já configura o PostgreSQL automaticamente.

### Opção 1: Apenas PostgreSQL com Docker

Se você quer apenas subir o PostgreSQL em um container e executar a aplicação localmente:

```bash
# Subir apenas o PostgreSQL
docker-compose up -d postgres

# Verificar se está rodando
docker-compose ps

# Ver logs
docker-compose logs -f postgres
```

O PostgreSQL estará disponível em `localhost:5432` com as credenciais:
- **Database:** `reserva_db`
- **User:** `admin`
- **Password:** `123456`

Depois, execute a aplicação normalmente (Maven ou IDE).

### Opção 2: Aplicação Completa com Docker

Para executar tanto o PostgreSQL quanto a aplicação em containers:

```bash
# Construir e subir todos os serviços
docker-compose up -d

# Ver logs
docker-compose logs -f

# Parar os serviços
docker-compose down

# Parar e remover volumes (apaga dados do banco)
docker-compose down -v
```

### Comandos Úteis Docker

```bash
# Ver status dos containers
docker-compose ps

# Ver logs do PostgreSQL
docker-compose logs postgres

# Ver logs da aplicação
docker-compose logs reserva-service

# Parar serviços
docker-compose stop

# Reiniciar serviços
docker-compose restart

# Acessar shell do PostgreSQL
docker-compose exec postgres psql -U admin -d reserva_db

# Remover tudo (containers, volumes, networks)
docker-compose down -v --remove-orphans
```

### Estrutura Docker

O projeto inclui:

- **`docker-compose.yml`**: Configuração do PostgreSQL e da aplicação
- **`Dockerfile`**: Imagem Docker para a aplicação Spring Boot
- **`.dockerignore`**: Arquivos ignorados no build

### Configuração do Docker Compose

O `docker-compose.yml` inclui:

- **PostgreSQL 15** (Alpine - imagem leve)
- **Volume persistente** para dados do banco
- **Health check** para garantir que o banco está pronto
- **Network** isolada para comunicação entre serviços

### Variáveis de Ambiente (Docker)

Se precisar alterar as configurações, edite o `docker-compose.yml`:

```yaml
environment:
  POSTGRES_DB: reserva_db     
  POSTGRES_USER: admin          
  POSTGRES_PASSWORD: 123456     
ports:
  - "5432:5432"           
```

**Importante:** Se alterar as credenciais no Docker, atualize também o `application.properties`.

---

## Configuração Manual

Se preferir não usar Docker, siga os passos abaixo:

### 1. Banco de Dados PostgreSQL

Crie o banco de dados e configure as credenciais:

```sql
CREATE DATABASE reserva_db;
```

As configurações padrão estão em `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/reserva_db
spring.datasource.username=admin
spring.datasource.password=123456
```

**Nota:** Ajuste as credenciais conforme seu ambiente.

### 2. Serviço de Frota

O microsserviço depende do serviço de frota configurado através da variável de ambiente `FROTA_SERVICE_URL` (padrão: `https://seu-servico.onrender.com`). O serviço utiliza os seguintes endpoints:

```
GET /api/veiculos
```

Este endpoint deve retornar uma lista de veículos (`List<VeiculoResponse>`) com as seguintes propriedades:
- `id` (Long)
- `modelo` (String)
- `marca` (String)
- `ano` (Integer)
- `placa` (String)
- `preco` (BigDecimal)
- `status` (String)

O serviço de reserva verifica se existe pelo menos um veículo com `status` igual a "disponível" (case-insensitive) para permitir a criação da reserva.

Também está disponível o endpoint:
```
GET /api/veiculos/{id}
```

Para consultar um veículo específico por ID.

## Executando o Projeto

### Maven

```bash
cd reserva
mvn clean install
mvn spring-boot:run
```

O serviço estará disponível em: `http://localhost:8080`

## Funcionalidades

### 1. Criar Reserva
- **Endpoint:** `POST /reservas`
- **Descrição:** Cria uma nova reserva de veículo
- **Validações:** 
  - Verifica disponibilidade com o serviço de frota
  - Valida datas (início não pode ser no passado, fim deve ser futuro)
  - Calcula valor total estimado (R$ 100,00 por diária)

### 2. Buscar Reserva por ID
- **Endpoint:** `GET /reservas/{id}`
- **Descrição:** Retorna os detalhes de uma reserva específica

## Testando as Funcionalidades

### Ferramentas Recomendadas

- **Postman**
- **cURL**
- **HTTPie**
- **Insomnia**
- **Thunder Client** (VS Code)

---

## 📝 Casos de Teste Detalhados

### 1. Criar Reserva - Caso de Sucesso

**Requisição:**
```http
POST http://localhost:8080/reservas
Content-Type: application/json

{
  "clienteId": 1,
  "categoriaCarroId": 2,
  "dataInicio": "2026-12-20T10:00:00",
  "dataFim": "2026-12-25T10:00:00"
}
```

**Resposta Esperada (201 Created):**
```json
{
  "id": 1,
  "clienteId": 1,
  "categoriaCarroId": 2,
  "dataInicio": "2024-12-20T10:00:00",
  "dataFim": "2024-12-25T10:00:00",
  "valorTotalEstimado": 500.00,
  "status": "PENDENTE"
}
```

**Observações:**
- O valor total é calculado como: número de dias × R$ 100,00
- Mínimo de 1 diária (mesmo que seja o mesmo dia)
- Status inicial é sempre `PENDENTE`

**cURL:**
```bash
curl -X POST http://localhost:8080/reservas \
  -H "Content-Type: application/json" \
  -d '{
    "clienteId": 1,
    "categoriaCarroId": 2,
    "dataInicio": "2024-12-20T10:00:00",
    "dataFim": "2024-12-25T10:00:00"
  }'
```

---

### 2. Buscar Reserva por ID - Caso de Sucesso

**Requisição:**
```http
GET http://localhost:8080/reservas/1
```

**Resposta Esperada (200 OK):**
```json
{
  "id": 1,
  "clienteId": 1,
  "categoriaCarroId": 2,
  "dataInicio": "2024-12-20T10:00:00",
  "dataFim": "2024-12-25T10:00:00",
  "valorTotalEstimado": 500.00,
  "status": "PENDENTE"
}
```

**cURL:**
```bash
curl -X GET http://localhost:8080/reservas/1
```

---

### 3. Buscar Reserva por ID - Reserva Não Encontrada

**Requisição:**
```http
GET http://localhost:8080/reservas/999
```

**Resposta Esperada (404 Not Found):**
```
Reserva não encontrada com id: 999
```

**cURL:**
```bash
curl -X GET http://localhost:8080/reservas/999
```

---

### 4. Criar Reserva - Campos Obrigatórios Ausentes

**Requisição:**
```http
POST http://localhost:8080/reservas
Content-Type: application/json

{
  "clienteId": 1,
  "dataInicio": "2024-12-20T10:00:00"
}
```

**Resposta Esperada (400 Bad Request):**
```json
{
  "timestamp": "2024-12-15T10:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "O ID da categoria do carro não pode ser nulo.",
  "path": "/reservas"
}
```

**cURL:**
```bash
curl -X POST http://localhost:8080/reservas \
  -H "Content-Type: application/json" \
  -d '{
    "clienteId": 1,
    "dataInicio": "2024-12-20T10:00:00"
  }'
```

---

### 5. Criar Reserva - Data de Início no Passado

**Requisição:**
```http
POST http://localhost:8080/reservas
Content-Type: application/json

{
  "clienteId": 1,
  "categoriaCarroId": 2,
  "dataInicio": "2020-01-01T10:00:00",
  "dataFim": "2024-12-25T10:00:00"
}
```

**Resposta Esperada (400 Bad Request):**
```
A data de início não pode ser no passado
```

**cURL:**
```bash
curl -X POST http://localhost:8080/reservas \
  -H "Content-Type: application/json" \
  -d '{
    "clienteId": 1,
    "categoriaCarroId": 2,
    "dataInicio": "2020-01-01T10:00:00",
    "dataFim": "2024-12-25T10:00:00"
  }'
```

---

### 6. Criar Reserva - Data de Fim no Passado ou Presente

**Requisição:**
```http
POST http://localhost:8080/reservas
Content-Type: application/json

{
  "clienteId": 1,
  "categoriaCarroId": 2,
  "dataInicio": "2024-12-20T10:00:00",
  "dataFim": "2024-12-15T10:00:00"
}
```

**Resposta Esperada (400 Bad Request):**
```
A data de fim deve ser uma data futura
```

**cURL:**
```bash
curl -X POST http://localhost:8080/reservas \
  -H "Content-Type: application/json" \
  -d '{
    "clienteId": 1,
    "categoriaCarroId": 2,
    "dataInicio": "2024-12-20T10:00:00",
    "dataFim": "2024-12-15T10:00:00"
  }'
```

---

### 7. Criar Reserva - Data de Fim Anterior à Data de Início

**Requisição:**
```http
POST http://localhost:8080/reservas
Content-Type: application/json

{
  "clienteId": 1,
  "categoriaCarroId": 2,
  "dataInicio": "2024-12-25T10:00:00",
  "dataFim": "2024-12-20T10:00:00"
}
```

**Resposta Esperada (400 Bad Request):**
```
A data de devolução deve ser posterior à data de retirada.
```

**cURL:**
```bash
curl -X POST http://localhost:8080/reservas \
  -H "Content-Type: application/json" \
  -d '{
    "clienteId": 1,
    "categoriaCarroId": 2,
    "dataInicio": "2024-12-25T10:00:00",
    "dataFim": "2024-12-20T10:00:00"
  }'
```

---

### 8. Criar Reserva - Data de Fim Igual à Data de Início

**Requisição:**
```http
POST http://localhost:8080/reservas
Content-Type: application/json

{
  "clienteId": 1,
  "categoriaCarroId": 2,
  "dataInicio": "2024-12-20T10:00:00",
  "dataFim": "2024-12-20T10:00:00"
}
```

**Resposta Esperada (400 Bad Request):**
```
A data de devolução deve ser posterior à data de retirada.
```

**Observação:** Mesmo que a validação passe, o sistema calcula no mínimo 1 diária.

**cURL:**
```bash
curl -X POST http://localhost:8080/reservas \
  -H "Content-Type: application/json" \
  -d '{
    "clienteId": 1,
    "categoriaCarroId": 2,
    "dataInicio": "2024-12-20T10:00:00",
    "dataFim": "2024-12-20T10:00:00"
  }'
```

---

### 9. Criar Reserva - Carro Não Disponível

**Requisição:**
```http
POST http://localhost:8080/reservas
Content-Type: application/json

{
  "clienteId": 1,
  "categoriaCarroId": 2,
  "dataInicio": "2024-12-20T10:00:00",
  "dataFim": "2024-12-25T10:00:00"
}
```

**Resposta Esperada (400 Bad Request):**
```
Não há carros disponíveis para esta categoria nestas datas.
```

**Observação:** Este erro ocorre quando o serviço de frota não retorna nenhum veículo com status "disponível" na lista de veículos.

**cURL:**
```bash
curl -X POST http://localhost:8080/reservas \
  -H "Content-Type: application/json" \
  -d '{
    "clienteId": 1,
    "categoriaCarroId": 2,
    "dataInicio": "2024-12-20T10:00:00",
    "dataFim": "2024-12-25T10:00:00"
  }'
```

---

### 10. Criar Reserva - Serviço de Frota Indisponível

**Requisição:**
```http
POST http://localhost:8080/reservas
Content-Type: application/json

{
  "clienteId": 1,
  "categoriaCarroId": 2,
  "dataInicio": "2024-12-20T10:00:00",
  "dataFim": "2024-12-25T10:00:00"
}
```

**Resposta Esperada (503 Service Unavailable):**
```
Erro ao comunicar com o serviço de frota. Tente novamente mais tarde.
```

**Observação:** Este erro ocorre quando o serviço de frota não está acessível ou retorna um erro HTTP.

**cURL:**
```bash
curl -X POST http://localhost:8080/reservas \
  -H "Content-Type: application/json" \
  -d '{
    "clienteId": 1,
    "categoriaCarroId": 2,
    "dataInicio": "2024-12-20T10:00:00",
    "dataFim": "2024-12-25T10:00:00"
  }'
```

**Para testar este cenário:**
1. Configure uma URL inválida para `FROTA_SERVICE_URL` ou pare o serviço de frota
2. Execute a requisição acima
3. Restaure a configuração correta do serviço de frota

---

## Validações Implementadas

### Validações de Entrada (ReservaRequest)

1. **clienteId**: Obrigatório, não pode ser nulo
2. **categoriaCarroId**: Obrigatório, não pode ser nulo
3. **dataInicio**: 
   - Obrigatória, não pode ser nula
   - Deve ser presente ou futura (`@FutureOrPresent`)
4. **dataFim**: 
   - Obrigatória, não pode ser nula
   - Deve ser futura (`@Future`)

### Validações de Negócio

1. **Data de fim deve ser posterior à data de início**: Validação customizada no service
2. **Disponibilidade de veículos**: Verificação com o serviço de frota
3. **Cálculo de valor**: Mínimo de 1 diária (R$ 100,00)

---

## Tratamento de Erros

O microsserviço possui tratamento de erros centralizado no controller:

| Exceção | HTTP Status | Descrição |
|---------|-------------|-----------|
| `IllegalArgumentException` | 400 Bad Request | Validações de negócio falharam |
| `IllegalStateException` | 503 Service Unavailable | Erro de comunicação com serviço externo |
| `EntityNotFoundException` | 404 Not Found | Recurso não encontrado |
| `MethodArgumentNotValidException` | 400 Bad Request | Validações de entrada falharam |

---

## Cálculo de Valor

O valor total estimado é calculado da seguinte forma:

```
valorTotalEstimado = número_de_dias × R$ 100,00
```

**Regras:**
- Mínimo de 1 diária (mesmo que seja o mesmo dia)
- Cálculo baseado na diferença entre `dataFim` e `dataInicio`
- Exemplo: 5 dias = R$ 500,00

---

## Status de Reserva

Os possíveis status de uma reserva são:

- `PENDENTE`: Reserva criada, aguardando confirmação
- `CONFIRMADA`: Reserva confirmada
- `CANCELADA`: Reserva cancelada
- `NO_SHOW`: Cliente não compareceu
- `EM_ANDAMENTO`: Reserva em andamento
- `CONCLUIDA`: Reserva finalizada

**Nota:** Atualmente, todas as reservas são criadas com status `PENDENTE`.

---

## Estrutura do Projeto

```
reserva/
├── src/
│   ├── main/
│   │   ├── java/com/reserva/
│   │   │   ├── ReservaApplication.java      # Classe principal
│   │   │   ├── client/
│   │   │   │   └── FrotaClient.java        # Cliente Feign para serviço de frota
│   │   │   ├── controller/
│   │   │   │   └── ReservaController.java   # Endpoints REST
│   │   │   ├── dto/
│   │   │   │   ├── ReservaRequest.java      # DTO de entrada
│   │   │   │   └── ReservaResponse.java    # DTO de saída
│   │   │   ├── model/
│   │   │   │   ├── Reserva.java            # Entidade JPA
│   │   │   │   └── ReservaStatus.java      # Enum de status
│   │   │   ├── repository/
│   │   │   │   └── ReservaRepository.java   # Repositório JPA
│   │   │   └── service/
│   │   │       └── ReservaService.java     # Lógica de negócio
│   │   └── resources/
│   │       └── application.properties      # Configurações
│   └── test/
└── pom.xml
```

---

