-- Mesmas classes e subclasses do back-finup, com os MESMOS ids,
-- porque o back-finup grava o id retornado por esta API.
CREATE TABLE classe_principal (
    id   BIGINT      NOT NULL,
    nome VARCHAR(55) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE subclasse (
    id                  BIGINT      NOT NULL,
    nome                VARCHAR(55) NOT NULL,
    classe_principal_id BIGINT      NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_subclasse_classe_principal
        FOREIGN KEY (classe_principal_id) REFERENCES classe_principal (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO classe_principal (id, nome) VALUES
(1, 'Indefinido'),
(2, 'Alimentação'),
(3, 'Transporte'),
(4, 'Saúde'),
(5, 'Educação'),
(6, 'Moradia'),
(7, 'Academia'),
(8, 'Lazer'),
(9, 'Pet');

INSERT INTO subclasse (id, nome, classe_principal_id) VALUES
(1,  'Mercado',                2),
(2,  'Restaurante',            2),
(3,  'Padaria',                2),
(4,  'Bar',                    2),
(5,  'Delivery',               2),
(6,  'Combustível',            3),
(7,  'Transporte Público',     3),
(8,  'Transporte Particular',  3),
(9,  'Manutenção do Veículo',  3),
(10, 'Farmácia',               4),
(11, 'Médico',                 4),
(12, 'Plano de Saúde',         4),
(13, 'Curso',                  5),
(14, 'Escola/Ensino Superior', 5),
(15, 'Água',                   6),
(16, 'Energia',                6),
(17, 'Aluguel',                6),
(18, 'Internet',               6),
(19, 'Compras',                8),
(20, 'Compras Online',         8),
(21, 'Streaming',              8),
(22, 'Viagens',                8),
(23, 'Ração',                  9),
(24, 'Veterinário',            9);
