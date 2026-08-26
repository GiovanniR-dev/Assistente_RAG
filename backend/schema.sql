-- Schema do Assistente de Documentos com RAG
-- Execute com: mysql -u root -p < schema.sql

CREATE DATABASE IF NOT EXISTS assistente_rag;
USE assistente_rag;

-- Remove as tabelas na ordem inversa das dependências
DROP TABLE IF EXISTS mensagens;
DROP TABLE IF EXISTS trechos;
DROP TABLE IF EXISTS conversas;
DROP TABLE IF EXISTS documentos;
DROP TABLE IF EXISTS usuarios;

-- Usuários do sistema
CREATE TABLE usuarios (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    senha_hash VARCHAR(255) NOT NULL,
    criado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Documentos enviados por cada usuário
CREATE TABLE documentos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    nome_arquivo VARCHAR(255) NOT NULL,
    tipo VARCHAR(50),
    criado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
);

-- Pedaços de texto de cada documento, com seu embedding
CREATE TABLE trechos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    documento_id BIGINT NOT NULL,
    conteudo TEXT NOT NULL,
    embedding JSON NOT NULL,
    ordem INT NOT NULL,
    criado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (documento_id) REFERENCES documentos(id) ON DELETE CASCADE
);

-- Sessões de chat
CREATE TABLE conversas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    titulo VARCHAR(150),
    criado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
);

-- Mensagens de cada conversa (papel: 'usuario' ou 'assistente')
CREATE TABLE mensagens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversa_id BIGINT NOT NULL,
    papel VARCHAR(20) NOT NULL,
    conteudo TEXT NOT NULL,
    criado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (conversa_id) REFERENCES conversas(id) ON DELETE CASCADE
);
