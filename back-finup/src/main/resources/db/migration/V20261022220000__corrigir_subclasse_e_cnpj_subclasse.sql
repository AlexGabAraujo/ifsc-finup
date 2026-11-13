-- 1. Corrige a classe principal das subclasses padrão.
-- Em V20260909213357 elas foram inseridas com o id da classe deslocado em 1
-- (ex: 'Mercado' apontava para 1 = Indefinido em vez de 2 = Alimentação).
-- Só altera as subclasses padrão, identificadas por id E nome.
UPDATE subclasse
SET classe_principal_id = CASE id
    WHEN 1  THEN 2 WHEN 2  THEN 2 WHEN 3  THEN 2 WHEN 4  THEN 2 WHEN 5  THEN 2   -- Alimentação
    WHEN 6  THEN 3 WHEN 7  THEN 3 WHEN 8  THEN 3 WHEN 9  THEN 3                  -- Transporte
    WHEN 10 THEN 4 WHEN 11 THEN 4 WHEN 12 THEN 4                                 -- Saúde
    WHEN 13 THEN 5 WHEN 14 THEN 5                                                -- Educação
    WHEN 15 THEN 6 WHEN 16 THEN 6 WHEN 17 THEN 6 WHEN 18 THEN 6                  -- Moradia
    WHEN 19 THEN 8 WHEN 20 THEN 8 WHEN 21 THEN 8 WHEN 22 THEN 8                  -- Lazer
    WHEN 23 THEN 9 WHEN 24 THEN 9                                                -- Pet
END
WHERE (id, nome) IN (
    (1, 'Mercado'), (2, 'Restaurante'), (3, 'Padaria'), (4, 'Bar'), (5, 'Delivery'),
    (6, 'Combustível'), (7, 'Transporte Público'), (8, 'Transporte Particular'), (9, 'Manutenção do Veículo'),
    (10, 'Farmácia'), (11, 'Médico'), (12, 'Plano de Saúde'),
    (13, 'Curso'), (14, 'Escola/Ensino Superior'),
    (15, 'Água'), (16, 'Energia'), (17, 'Aluguel'), (18, 'Internet'),
    (19, 'Compras'), (20, 'Compras Online'), (21, 'Streaming'), (22, 'Viagens'),
    (23, 'Ração'), (24, 'Veterinário')
);

-- 2. Adiciona referência à SubClasse na tabela cnpj,
-- permitindo salvar a subclasse retornada pela cnae-api.
ALTER TABLE cnpj
    ADD COLUMN subclasse_id BIGINT NULL,
    ADD CONSTRAINT fk_cnpj_subclasse
        FOREIGN KEY (subclasse_id) REFERENCES subclasse (id)
            ON DELETE SET NULL ON UPDATE CASCADE;
