# Assistente de Documentos com RAG

Aplicação que permite conversar com seus próprios documentos em linguagem natural, usando **RAG (Retrieval-Augmented Generation)**: o sistema recupera os trechos mais relevantes do documento e os envia como contexto para um modelo de linguagem gerar a resposta.

> **Status:** backend completo e frontend funcional. Deploy pendente.

---

## Estrutura

```
.
├── backend/    API REST em Java 21 + Spring Boot 4
└── frontend/   Interface em React + Vite
```

---

## Stack

| Camada | Tecnologia |
|---|---|
| Backend | Java 21, Spring Boot 4.1, Spring Data JPA |
| Segurança | Spring Security + JWT (JJWT), senhas em BCrypt |
| Banco de dados | MySQL 8 |
| Extração de texto | Apache PDFBox 3 |
| Embeddings | OpenAI `text-embedding-3-small` (1536 dimensões) |
| Geração | OpenAI (modelo configurável) |
| Frontend | React 19, Vite |
| Build | Maven (backend), npm (frontend) |

---

## Como funciona

**Ingestão**

```
PDF enviado
   ↓
Extração do texto (PDFBox)
   ↓
Divisão em trechos de ~1000 caracteres, com 200 de sobreposição
   ↓
Geração de embedding para cada trecho
   ↓
Persistência no MySQL
```

**Consulta**

```
Pergunta do usuário
   ↓
Embedding da pergunta
   ↓
Similaridade de cosseno contra todos os trechos armazenados
   ↓
Os 3 trechos mais relevantes + as últimas 10 mensagens da conversa
   ↓
LLM gera a resposta ancorada nesse contexto
   ↓
Pergunta e resposta são persistidas na conversa
```

---

## Modelo de dados

```
usuarios ──┬──< documentos ──< trechos
           └──< conversas  ──< mensagens
```

| Tabela | Responsabilidade |
|---|---|
| `usuarios` | Identidade, e-mail único e hash da senha |
| `documentos` | Metadados dos arquivos enviados |
| `trechos` | Pedaços do texto + embedding (vetor) de cada um |
| `conversas` | Sessões de chat |
| `mensagens` | Perguntas e respostas de cada conversa |

O schema completo está em [`backend/schema.sql`](./backend/schema.sql).

---

## Arquitetura

```
React (Vite)  →  cliente da API centralizado, token em localStorage
      ↓ HTTP
Filtro JWT    →  autentica a requisição e popula o contexto de segurança
      ↓
Controller    →  recebe HTTP, converte para DTO, devolve status
      ↓
Service       →  regra de negócio (extração, chunking, embeddings, busca, geração)
      ↓
Repository    →  acesso ao banco via Spring Data JPA
```

Cada camada conhece apenas a de baixo. O Controller nunca acessa o Repository diretamente, e a Service não sabe que existe HTTP.

---

## Decisões técnicas

### Por que armazenar embeddings no MySQL em vez de um vector database?

O projeto usa uma coluna `JSON` na tabela `trechos` para guardar o vetor, e a similaridade de cosseno é calculada em Java, na camada de serviço.

A escolha é deliberada: adicionar Pinecone, Qdrant ou pgvector traria uma dependência de infraestrutura a mais sem ganho real na escala deste projeto. Manter tudo em um único banco relacional simplifica o deploy e deixa o cálculo de similaridade explícito no código, em vez de escondido atrás de uma abstração.

A limitação é conhecida e assumida: a busca carrega todos os trechos em memória e compara um a um, o que é O(n). Com milhares de trechos continua instantâneo; com milhões, deixaria de escalar, e aí um índice vetorial dedicado passaria a compensar.

### Chunking por tamanho fixo com sobreposição, e não por parágrafo

A primeira implementação dividia o texto nas quebras de parágrafo (`\n\s*\n`). Funcionava em teoria, mas falhou no primeiro PDF real: a extração do PDFBox não preservou as quebras duplas, e um documento de 8.283 caracteres virou um único trecho — inútil para busca semântica, já que não há o que comparar.

A estratégia atual divide em blocos de 1.000 caracteres avançando 800 por vez, gerando 200 caracteres de sobreposição entre trechos vizinhos. A sobreposição resolve um problema concreto: uma informação que caia exatamente na fronteira entre dois blocos seria partida ao meio e nenhum dos lados faria sentido isoladamente. Com o overlap, ela aparece íntegra em pelo menos um trecho.

O custo dessa escolha aparece nos resultados: trechos vizinhos compartilham conteúdo, então a busca pode retornar dois trechos com a mesma informação. É um trade-off aceito em favor da integridade semântica.

O corte também procura o fim de frase mais próximo (`. `) dentro da metade final do bloco, evitando quebrar palavras no meio.

### Ancoragem do prompt no contexto recuperado

A instrução enviada como mensagem `system` delimita explicitamente o que o modelo pode usar: apenas os trechos fornecidos, sem recorrer ao conhecimento de treino, e declarando quando a informação não está no documento.

Esse comportamento foi validado empiricamente com perguntas cujo assunto não existe no documento. Em um dos testes, a recuperação trouxe trechos irrelevantes e o modelo respondeu que a informação não constava no material — em vez de completar a lacuna com conhecimento próprio. Em um assistente de documentos, essa recusa é a funcionalidade: o valor do sistema depende de o usuário poder confiar que a resposta veio do arquivo dele.

### Janela deslizante de histórico

Conversas mantêm contexto enviando as mensagens anteriores junto com a pergunta atual. Como cada mensagem antiga é recobrada como tokens a cada nova requisição, o histórico é limitado às últimas 10 mensagens.

Alternativas mais sofisticadas existem — resumir mensagens antigas em vez de descartá-las, por exemplo — mas a janela fixa resolve o caso comum com custo previsível e sem introduzir mais uma chamada de modelo no caminho crítico.

### Autenticação stateless com JWT

O login devolve um token assinado que carrega o id do usuário e expira em 24 horas. O servidor não mantém sessão: cada requisição se identifica sozinha pelo header `Authorization`, o que elimina estado compartilhado e simplifica o deploy horizontal.

Senhas são persistidas apenas como hash BCrypt, que gera um sal aleatório por senha — dois usuários com a mesma senha produzem hashes diferentes, inviabilizando ataques por tabela pré-computada.

Trocar a chave de assinatura invalida todos os tokens emitidos de uma vez, o que serve como mecanismo de revogação em massa caso a chave seja comprometida.

### Respostas que não revelam a existência de recursos

Duas decisões seguem o mesmo princípio: não confirmar ao atacante aquilo que ele está tentando descobrir.

No login, e-mail inexistente e senha incorreta retornam a mesma mensagem — caso contrário, seria possível enumerar quais e-mails estão cadastrados.

No acesso a conversas, uma conversa que pertence a outro usuário responde `404`, como se não existisse, em vez de `403`. Um `403` confirmaria que aquele id está em uso, permitindo mapear o volume de dados do sistema. Essa verificação de propriedade fecha uma classe de falha conhecida como IDOR (*Insecure Direct Object Reference*).

### Token no localStorage: conveniência com limitação conhecida

O frontend guarda o token no `localStorage`, o que mantém a sessão após recarregar a página sem exigir infraestrutura adicional. A contrapartida é que qualquer JavaScript executado na página consegue lê-lo, o que torna a aplicação sensível a XSS.

A alternativa mais segura seria um cookie `httpOnly`, inacessível ao JavaScript, ao custo de configuração adicional de CORS e CSRF. Para o escopo atual, a simplicidade prevaleceu.

### Limitação conhecida: a recuperação não considera o histórico

A busca semântica gera o embedding apenas da pergunta atual, isolada da conversa. Perguntas de continuidade que carregam assunto próprio funcionam bem — *"e o preço?"* recupera corretamente os trechos sobre custo, porque "preço" tem significado próprio no espaço vetorial.

O problema aparece em perguntas que são pura referência, sem conteúdo semântico: *"explique melhor"* não aponta para nenhuma região do documento, e a recuperação retorna trechos arbitrários. Nesses casos o histórico salva a resposta, mas o modelo trabalha com o contexto errado em mãos.

A solução conhecida é **query rewriting**: usar a LLM para reescrever a pergunta de forma autônoma antes de buscar. O custo é uma chamada adicional de modelo por pergunta. A implementação está no roadmap.

### Por que `ddl-auto=validate` em vez de `update`?

O schema é versionado manualmente em `schema.sql` e o Hibernate atua apenas como validador na subida da aplicação. Com `update`, o Hibernate alteraria as tabelas por conta própria a cada mudança nas entidades — conveniente no início, arriscado depois, porque o estado real do banco deixa de ser rastreável.

Com `validate`, qualquer divergência entre entidade e tabela derruba a aplicação no startup, em vez de gerar erro silencioso em produção. Na prática, foi isso que expôs um erro de mapeamento logo no começo: as tabelas usavam `INT` nas chaves primárias enquanto as entidades JPA declaravam `Long`, que o Hibernate mapeia para `BIGINT`. A validação falhou na subida e o schema foi migrado para `BIGINT`.

### `open-in-view` desabilitado

O padrão do Spring mantém a sessão do Hibernate aberta durante toda a requisição, permitindo que relações `LAZY` sejam carregadas em qualquer ponto — inclusive na serialização da resposta. Isso esconde consultas em lugares inesperados e prolonga a posse de conexões do pool.

Com `spring.jpa.open-in-view=false`, o acesso a dados não carregados fora da camada de serviço falha explicitamente, tornando visível o que antes era silencioso.

### Nenhuma credencial no repositório

Senha do banco, chave de API e segredo de assinatura JWT são lidos exclusivamente de variáveis de ambiente. O `application.properties` versionado contém apenas as referências (`${DB_PASSWORD}`, `${OPENAI_API_KEY}`, `${JWT_SECRET}`), nunca os valores.

---

## API

Todas as rotas exigem autenticação, exceto `/api/auth/**`. O token vai no header:

```
Authorization: Bearer <token>
```

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/api/auth/registrar` | Cria uma conta (`nome`, `email`, `senha`) |
| `POST` | `/api/auth/login` | Devolve o token JWT |
| `POST` | `/api/documentos/upload` | Envia um PDF (form-data, campo `arquivo`) |
| `GET` | `/api/documentos` | Lista os documentos do usuário |
| `GET` | `/api/busca?pergunta=...&quantidade=3` | Trechos mais relevantes com seus scores |
| `POST` | `/api/conversas` | Cria uma nova conversa |
| `GET` | `/api/conversas` | Lista as conversas do usuário |
| `POST` | `/api/conversas/{id}/mensagens` | Envia uma pergunta e recebe a resposta |
| `GET` | `/api/conversas/{id}/mensagens` | Retorna o histórico da conversa |

O endpoint de busca expõe o score de similaridade de cada trecho, o que permite auditar *por que* uma resposta foi gerada — útil para diagnosticar quando o resultado não é o esperado.

---

## Rodando localmente

### Pré-requisitos

- JDK 21 ou superior
- Node.js 20 ou superior
- MySQL 8
- Uma chave de API da OpenAI

### 1. Banco de dados

```bash
mysql -u root -p < backend/schema.sql
```

### 2. Variáveis de ambiente

A aplicação não guarda credenciais no código. Defina antes de rodar o backend:

| Variável | Descrição | Padrão |
|---|---|---|
| `DB_HOST` | Host do MySQL | `localhost` |
| `DB_NAME` | Nome do banco | `assistente_rag` |
| `DB_USER` | Usuário do MySQL | — |
| `DB_PASSWORD` | Senha do MySQL | — |
| `OPENAI_API_KEY` | Chave da API da OpenAI | — |
| `JWT_SECRET` | Segredo de assinatura (mínimo 32 caracteres) | — |

Para gerar o segredo JWT:

```bash
openssl rand -base64 48
```

### 3. Backend

```bash
cd backend
./mvnw spring-boot:run
```

Sobe em `http://localhost:8080`.

### 4. Frontend

Em outro terminal:

```bash
cd frontend
npm install
npm run dev
```

Sobe em `http://localhost:5173`.

---

## Roadmap

- [x] Modelagem do banco de dados
- [x] Entidades JPA e repositories
- [x] Extração de texto de PDF
- [x] Chunking com sobreposição
- [x] Geração de embeddings
- [x] Busca por similaridade de cosseno
- [x] Geração de resposta ancorada no contexto
- [x] Histórico de conversas com janela deslizante
- [x] Registro e login com JWT
- [x] Isolamento de dados por usuário
- [x] Tratamento centralizado de erros
- [x] Interface web com upload e chat
- [ ] Query rewriting para perguntas de continuidade
- [ ] Testes automatizados
- [ ] Deploy

---

## Autor

**Giovanni** — [github.com/GiovanniR-dev](https://github.com/GiovanniR-dev)