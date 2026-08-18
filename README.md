# Assistente de Documentos com RAG

Aplicação backend que permite conversar com seus próprios documentos em linguagem natural, usando **RAG (Retrieval-Augmented Generation)**: o sistema busca os trechos mais relevantes do documento e os envia como contexto para um modelo de linguagem gerar a resposta.

> **Status:** em desenvolvimento — pipeline de ingestão funcionando, busca semântica em construção.

---

## Stack

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 21 |
| Framework | Spring Boot 4.1 |
| Persistência | Spring Data JPA + Hibernate |
| Banco de dados | MySQL 8 |
| Extração de texto | Apache PDFBox 3 |
| Embeddings | OpenAI `text-embedding-3-small` (1536 dimensões) |
| Cliente HTTP | Spring `RestClient` |
| Build | Maven |

---

## Como funciona

**Ingestão (implementado):**

```
PDF enviado
   ↓
Extração do texto (PDFBox)
   ↓
Divisão em trechos de ~1000 caracteres, com 200 de sobreposição
   ↓
Geração de embedding para cada trecho (OpenAI)
   ↓
Persistência no MySQL
```

**Consulta (em construção):**

```
Pergunta do usuário
   ↓
Embedding da pergunta
   ↓
Similaridade de cosseno contra os trechos armazenados
   ↓
Trechos mais relevantes viram contexto do prompt
   ↓
LLM gera a resposta
```

---

## Modelo de dados

```
usuarios ──┬──< documentos ──< trechos
           └──< conversas  ──< mensagens
```

| Tabela | Responsabilidade |
|---|---|
| `usuarios` | Identidade do dono dos dados |
| `documentos` | Metadados dos arquivos enviados |
| `trechos` | Pedaços do texto + embedding (vetor) de cada um |
| `conversas` | Sessões de chat |
| `mensagens` | Perguntas e respostas de cada conversa |

O schema completo está em [`schema.sql`](./schema.sql).

---

## Arquitetura em camadas

```
Controller  →  recebe HTTP, converte para DTO, devolve status
    ↓
Service     →  regra de negócio (extração, chunking, embeddings)
    ↓
Repository  →  acesso ao banco via Spring Data JPA
```

Cada camada conhece apenas a de baixo. O Controller nunca acessa o Repository diretamente, e a Service não sabe que existe HTTP.

---

## Decisões técnicas

### Por que armazenar embeddings no MySQL em vez de um vector database?

O projeto usa uma coluna `JSON` na tabela `trechos` para guardar o vetor, e a comparação por similaridade de cosseno é feita em Java, na camada de serviço.

A escolha é deliberada: adicionar Pinecone, Qdrant ou pgvector traria uma dependência de infraestrutura a mais sem ganho real na escala deste projeto. Manter tudo em um único banco relacional simplifica o deploy e deixa explícito o cálculo de similaridade — que fica visível no código em vez de escondido atrás de uma abstração. Em um cenário com milhões de trechos, a decisão seria diferente: a busca linear deixaria de escalar e um índice vetorial dedicado passaria a compensar.

### Chunking por tamanho fixo com sobreposição, e não por parágrafo

A primeira implementação dividia o texto nas quebras de parágrafo (`\n\s*\n`). Funcionava em teoria, mas falhou no primeiro PDF real: a extração do PDFBox não preservou as quebras duplas, e um documento de 8.283 caracteres virou um único trecho — inútil para busca semântica, já que não há o que comparar.

A estratégia atual divide em blocos de 1.000 caracteres avançando 800 por vez, o que gera 200 caracteres de sobreposição entre trechos vizinhos. A sobreposição resolve um problema concreto: uma informação que caia exatamente na fronteira entre dois blocos seria partida ao meio e nenhum dos lados faria sentido isoladamente. Com o overlap, ela aparece íntegra em pelo menos um trecho.

O corte também procura o fim de frase mais próximo (`. `) dentro da metade final do bloco, evitando quebrar palavras no meio.

### Por que `ddl-auto=validate` em vez de `update`?

O schema é versionado manualmente em `schema.sql` e o Hibernate atua apenas como validador na subida da aplicação. Com `update`, o Hibernate alteraria as tabelas por conta própria a cada mudança nas entidades — conveniente no início, arriscado depois, porque o estado real do banco deixa de ser rastreável.

Com `validate`, qualquer divergência entre entidade e tabela derruba a aplicação no startup, em vez de gerar erro silencioso em produção. Na prática, foi isso que expôs um erro de mapeamento logo no começo: as tabelas usavam `INT` nas chaves primárias enquanto as entidades JPA declaravam `Long`, que o Hibernate mapeia para `BIGINT`. A validação falhou na subida e o schema foi migrado para `BIGINT`, alinhando os dois lados.

### Por que injeção de dependência via construtor?

Todas as services usam campos `final` com `@RequiredArgsConstructor` do Lombok. Isso torna as dependências obrigatórias e imutáveis, e permite instanciar a classe em testes sem subir o contexto do Spring — algo que a injeção por campo (`@Autowired` direto no atributo) dificulta.

### Nenhuma credencial no repositório

Senha do banco e chave de API são lidas exclusivamente de variáveis de ambiente. O `application.properties` versionado contém apenas as referências (`${DB_PASSWORD}`, `${OPENAI_API_KEY}`), nunca os valores.

---

## API

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/api/documentos/upload` | Envia um PDF (form-data, campo `arquivo`) |
| `GET` | `/api/documentos` | Lista os documentos processados |

Exemplo:

```bash
curl -X POST -F "arquivo=@documento.pdf" http://localhost:8080/api/documentos/upload
```

```json
{ "id": 1, "nomeArquivo": "documento.pdf" }
```

---

## Rodando localmente

### Pré-requisitos

- JDK 21 ou superior
- MySQL 8
- Uma chave de API da OpenAI

### 1. Criar o banco

```bash
mysql -u root -p < schema.sql
```

### 2. Configurar as variáveis de ambiente

A aplicação não guarda credenciais no código. Defina antes de rodar:

| Variável | Descrição | Padrão |
|---|---|---|
| `DB_HOST` | Host do MySQL | `localhost` |
| `DB_NAME` | Nome do banco | `assistente_rag` |
| `DB_USER` | Usuário do MySQL | — |
| `DB_PASSWORD` | Senha do MySQL | — |
| `OPENAI_API_KEY` | Chave da API da OpenAI | — |

### 3. Rodar

```bash
./mvnw spring-boot:run
```

A aplicação sobe em `http://localhost:8080`.

---

## Roadmap

- [x] Modelagem do banco de dados
- [x] Entidades JPA e repositories
- [x] Extração de texto de PDF
- [x] Chunking com sobreposição
- [x] Geração de embeddings
- [x] Endpoint de upload
- [ ] Busca por similaridade de cosseno
- [ ] Integração com a LLM para geração das respostas
- [ ] Endpoints de conversa
- [ ] Autenticação
- [ ] Frontend em React
- [ ] Deploy

---

## Autor

**Giovanni** — [github.com/GiovanniR-dev](https://github.com/GiovanniR-dev)
