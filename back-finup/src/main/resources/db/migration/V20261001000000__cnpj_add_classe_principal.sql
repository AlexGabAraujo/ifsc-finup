-- Adiciona referência à ClassePrincipal na tabela cnpj,
-- permitindo que o enriquecimento via API do CNPJá salve a categoria inferida pelo CNAE.
ALTER TABLE cnpj
    ADD COLUMN classe_principal_id BIGINT NULL,
    ADD CONSTRAINT fk_cnpj_classe_principal
        FOREIGN KEY (classe_principal_id) REFERENCES classe_principal (id)
            ON DELETE SET NULL ON UPDATE CASCADE;
