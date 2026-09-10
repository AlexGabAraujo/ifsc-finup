ALTER TABLE transacao
DROP CONSTRAINT chk_classificacao_obrigatoria;

INSERT INTO classe_principal (nome) VALUES
                                        ('Indefinido'),
                                        ('Alimentação'),
                                        ('Transporte'),
                                        ('Saúde'),
                                        ('Educação'),
                                        ('Moradia'),
                                        ('Academia'),
                                        ('Lazer'),
                                        ('Pet');

INSERT INTO subclasse (nome, classe_principal_id) VALUES
                                                      ('Mercado', 1),
                                                      ('Restaurante', 1),
                                                      ('Padaria', 1),
                                                      ('Bar', 1),
                                                      ('Delivery', 1);

INSERT INTO subclasse (nome, classe_principal_id) VALUES
                                                      ('Combustível', 2),
                                                      ('Transporte Público', 2),
                                                      ('Transporte Particular', 2),
                                                      ('Manutenção do Veículo', 2);

INSERT INTO subclasse (nome, classe_principal_id) VALUES
                                                      ('Farmácia', 3),
                                                      ('Médico', 3),
                                                      ('Plano de Saúde', 3);

INSERT INTO subclasse (nome, classe_principal_id) VALUES
                                                      ('Curso', 4),
                                                      ('Escola/Ensino Superior', 4);

INSERT INTO subclasse (nome, classe_principal_id) VALUES
                                                      ('Água', 5),
                                                      ('Energia', 5),
                                                      ('Aluguel', 5),
                                                      ('Internet', 5);

INSERT INTO subclasse (nome, classe_principal_id) VALUES
                                                      ('Compras', 7),
                                                      ('Compras Online', 7),
                                                      ('Streaming', 7),
                                                      ('Viagens', 7);

INSERT INTO subclasse (nome, classe_principal_id) VALUES
                                                      ('Ração', 8),
                                                      ('Veterinário', 8);