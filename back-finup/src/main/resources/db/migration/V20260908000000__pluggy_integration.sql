CREATE TABLE item_bancario (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pluggy_item_id VARCHAR(255) NOT NULL UNIQUE,
    pessoa_fisica_id BIGINT NOT NULL,
    nome_instituicao VARCHAR(255),
    data_conexao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_item_bancario_pessoa_fisica FOREIGN KEY (pessoa_fisica_id) REFERENCES pessoa_fisica(id)
);

CREATE TABLE conta_bancaria (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    item_bancario_id BIGINT NOT NULL,
    pluggy_account_id VARCHAR(255) NOT NULL UNIQUE,
    marketing_name VARCHAR(255),
    tipo VARCHAR(50) NOT NULL,
    subtipo VARCHAR(50) NOT NULL,
    data_criacao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_conta_bancaria_item FOREIGN KEY (item_bancario_id) REFERENCES item_bancario(id)
);

ALTER TABLE transacao
    ADD COLUMN external_id VARCHAR(255) NULL UNIQUE,
    ADD COLUMN conta_bancaria_id BIGINT NULL,
    ADD CONSTRAINT fk_transacao_conta_bancaria FOREIGN KEY (conta_bancaria_id) REFERENCES conta_bancaria(id);