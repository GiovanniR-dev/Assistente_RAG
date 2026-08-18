# Assistente de Documentos com RAG

Aplicação backend que permite conversar com seus próprios documentos em linguagem natural, usando **RAG (Retrieval-Augmented Generation)**: o sistema busca os trechos mais relevantes do documento e os envia como contexto para um modelo de linguagem gerar a resposta.

> **Status:** em desenvolvimento — backend em andamento, frontend planejado.

---

## Stack

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 21 |
| Framework | Spring Boot 4.1 |
| Persistência | Spring Data JPA + Hibernate |
| Banco de dados | MySQL 8 |
| Extração de texto | Apache PDFBox 3 |
| Embeddings | OpenAI `text-embedding-3-small` |
| Build | Maven |

---

## Como funciona

```
┌─────────────────────────────────────────┐
│  1. Upload do documento (PDF)           │
└────────────────┬────────────────────────┘
                 ▼
┌─────────────────────────────────────────┐
│  2. Extração do texto (PDFBox)          │
└────────────────┬────────────────────────┘
                 ▼
┌─────────────────────────────────────────┐
│  3. Divisão em trechos (chunking)       │
└────────────────┬────────────────────────┘
                 ▼
┌─────────────────────────────────────────┐
│  4. Geração de embeddings por trecho    │
└────────────────┬────────────────────────┘
                 ▼
┌─────────────────────────────────────────┐
│  5. Persistência no MySQL               │
└─────────────────────────────────────────┘

Na pergunta do usuário:
pergunta → embedding → similaridade de cosseno contra os trechos
        → trechos mais relevantes → prompt para a LLM → resposta
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

## Decisões técnicas

### Por que armazenar embeddings no MySQL em vez de um vector database?

O projeto usa uma coluna `JSON` na tabela `trechos` para guardar o vetor, e a comparação por similaridade de cosseno é feita em Java, na camada de serviço.

A escolha é deliberada: adicionar Pinecone, Qdrant ou pgvector traria uma dependência de infraestrutura a mais sem ganho real na escala deste projeto. Manter tudo em um único banco relacional simplifica o deploy e deixa explícito o cálculo de similaridade — que fica visível no código em vez de escondido atrás de uma abstração. Em um cenário com milhões de trechos, a decisão seria diferente: a busca linear deixaria de escalar e um índice vetorial dedicado passaria a compensar.

### Por que `ddl-auto=validate` em vez de `update`?

O schema é versionado manualmente em `schema.sql` e o Hibernate atua apenas como validador na subida da aplicação. Com `update`, o Hibernate alteraria as tabelas por conta própria a cada mudança nas entidades — conveniente no início, arriscado depois, porque o estado real do banco deixa de ser rastreável.

Com `validate`, qualquer divergência entre entidade e tabela derruba a aplicação no startup, em vez de gerar erro silencioso em produção. Na prática, foi isso que expôs um erro de mapeamento logo no começo: as tabelas usavam `INT` nas chaves primárias enquanto as entidades JPA declaravam `Long`, que o Hibernate mapeia para `BIGINT`. A validação falhou na subida e o schema foi migrado para `BIGINT`, alinhando os dois lados.

### Por que injeção de dependência via construtor?

Todas as services usam campos `final` com `@RequiredArgsConstructor` do Lombok. Isso torna as dependências obrigatórias e imutáveis, e permite instanciar a classe em testes sem subir o contexto do Spring — algo que a injeção por campo (`@Autowired` direto no atributo) dificulta.

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
- [x] Serviço de upload com extração de texto e chunking
- [x] Geração de embeddings
- [ ] Busca por similaridade de cosseno
- [ ] Integração com a LLM para geração das respostas
- [ ] Endpoints REST
- [ ] Autenticação
- [ ] Frontend em React
- [ ] Deploy

---

## Autor

**Giovanni** — [github.com/GiovanniR-dev](https://github.com/GiovanniR-dev)
