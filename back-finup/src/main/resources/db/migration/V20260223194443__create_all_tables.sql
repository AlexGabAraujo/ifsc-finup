SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE;
SET SQL_MODE='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

CREATE SCHEMA IF NOT EXISTS finup DEFAULT CHARACTER SET utf8mb4;
USE finup;

CREATE TABLE IF NOT EXISTS pessoa_fisica (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nome VARCHAR(200) NOT NULL,
    cpf VARCHAR(16) NOT NULL,
    telefone VARCHAR(20) NOT NULL,
    data_nascimento DATE NOT NULL,
    data_inicio DATETIME NOT NULL,
    data_fim DATETIME NULL,
    ativo TINYINT(1) NULL,
    meta_eco_atual DECIMAL(10,2) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_pessoa_fisica_cpf (cpf)
    ) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS classe_principal (
    id BIGINT NOT NULL AUTO_INCREMENT,
	nome VARCHAR(55) NOT NULL,
    PRIMARY KEY (id)
    ) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS subclasse (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nome VARCHAR(55) NOT NULL,
    classe_principal_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    KEY fk_subclasse_classe_principal_idx (classe_principal_id),
    CONSTRAINT fk_subclasse_classe_principal
    FOREIGN KEY (classe_principal_id)
    REFERENCES classe_principal (id)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION
    ) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS categoria_personalizada (
    id BIGINT NOT NULL AUTO_INCREMENT,
    icone VARCHAR(50),
    cor VARCHAR(50),
    orcamento DECIMAL(10,2),
    pessoa_fisica_id BIGINT NOT NULL,
    subclasse_id BIGINT NULL,
    classe_principal_id BIGINT NULL,
    PRIMARY KEY (id),
		KEY fk_categoria_personalizada_pessoa_idx (pessoa_fisica_id),
		KEY fk_categoria_personalizada_subclasse_idx (subclasse_id),
		KEY fk_categoria_personalizada_classe_idx (classe_principal_id),
    CONSTRAINT fk_categoria_personalizada_pessoa
        FOREIGN KEY (pessoa_fisica_id)
        REFERENCES pessoa_fisica (id)
        ON DELETE CASCADE
        ON UPDATE NO ACTION,
    CONSTRAINT fk_categoria_personalizada_subclasse
        FOREIGN KEY (subclasse_id)
        REFERENCES subclasse (id)
        ON DELETE CASCADE
        ON UPDATE NO ACTION,
    CONSTRAINT fk_categoria_personalizada_classe
        FOREIGN KEY (classe_principal_id)
        REFERENCES classe_principal (id)
        ON DELETE CASCADE
        ON UPDATE NO ACTION,
    CONSTRAINT chk_categoria_personalizada_tipo
        CHECK (
            (classe_principal_id IS NOT NULL AND subclasse_id IS NULL)
            OR
            (classe_principal_id IS NULL AND subclasse_id IS NOT NULL)
        )
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS cnpj (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nome_empresa VARCHAR(255) NOT NULL,
    cnpj VARCHAR(17) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_cnpj_numero (cnpj)
    ) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS credencial (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(255) NOT NULL,
    senha VARCHAR(255) NOT NULL,
    username VARCHAR(45) NOT NULL,
    pessoa_fisica_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_credencial_username (username),
    UNIQUE KEY uk_credencial_pessoa_fisica (pessoa_fisica_id),
    CONSTRAINT fk_credencial_pessoa_fisica
    FOREIGN KEY (pessoa_fisica_id)
    REFERENCES pessoa_fisica (id)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION
    ) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS transacao (
    id BIGINT NOT NULL AUTO_INCREMENT,
    valor DECIMAL(10,2) NOT NULL,
    pessoa_fisica_id BIGINT NOT NULL,
    tipo_pagamento ENUM('CARTAO_DE_CREDITO', 'CARTAO_DE_DEBITO', 'PIX', 'BOLETO', 'DINHEIRO', 'CHEQUE') NOT NULL,
    tipo_gasto ENUM('CREDITO', 'DEBITO') NOT NULL,
    subclasse_id BIGINT,
    cnpj_id BIGINT,
    classe_principal_id BIGINT,
    data_insercao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY fk_transacao_pessoa_fisica_idx (pessoa_fisica_id),
	KEY fk_transacao_subclasse_idx (subclasse_id),
	KEY fk_transacao_cnpj_idx (cnpj_id),
	KEY fk_transacao_classe_principal_idx (classe_principal_id),
    CONSTRAINT fk_transacao_pessoa_fisica
    FOREIGN KEY (pessoa_fisica_id)
    REFERENCES pessoa_fisica (id)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
    CONSTRAINT fk_transacao_subclasse
    FOREIGN KEY (subclasse_id)
    REFERENCES subclasse (id)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
    CONSTRAINT fk_transacao_cnpj
    FOREIGN KEY (cnpj_id)
    REFERENCES cnpj (id)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
    CONSTRAINT fk_transacao_classe_principal
    FOREIGN KEY (classe_principal_id)
    REFERENCES classe_principal (id)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
    CONSTRAINT chk_classificacao_obrigatoria
            CHECK (
                (classe_principal_id IS NOT NULL AND subclasse_id IS NULL)
                OR
                (classe_principal_id IS NULL AND subclasse_id IS NOT NULL)
            )
    ) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS cnpj_classe_principal (
	cnpj_id BIGINT NOT NULL,
    classe_principal_id BIGINT NOT NULL,
    PRIMARY KEY (cnpj_id, classe_principal_id),
    KEY fk_cnpj_classe_principal_classe_idx (classe_principal_id),
    CONSTRAINT fk_cnpj_classe_principal_cnpj
    FOREIGN KEY (cnpj_id)
    REFERENCES cnpj (id)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
    CONSTRAINT fk_cnpj_classe_principal_classe
    FOREIGN KEY (classe_principal_id)
    REFERENCES classe_principal (id)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION
    ) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS subclasse_cnpj (
    subclasse_id BIGINT NOT NULL,
	cnpj_id BIGINT NOT NULL,
    PRIMARY KEY (subclasse_id, cnpj_id),
    KEY fk_subclasse_cnpj_cnpj_idx (cnpj_id),
    CONSTRAINT fk_subclasse_cnpj_subclasse
    FOREIGN KEY (subclasse_id)
    REFERENCES subclasse (id)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
    CONSTRAINT fk_subclasse_cnpj_cnpj
    FOREIGN KEY (cnpj_id)
    REFERENCES cnpj (id)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION
    ) ENGINE=InnoDB;

SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;