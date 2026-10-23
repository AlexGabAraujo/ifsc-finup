-- Corrige a classe principal das subclasses padrão usando os NOMES, não os ids.
-- A V20261022220000 corrigia por id e, em bancos cujos ids não seguem a ordem
-- da migration de carga, moveu Mercado/Restaurante/Padaria/Bar para Transporte.
-- Aqui cada subclasse aponta para a classe com o nome certo (a de menor id, se houver
-- classes duplicadas). Em bancos onde já está correto, não altera nada.
UPDATE subclasse s
JOIN (SELECT nome, MIN(id) AS id FROM classe_principal GROUP BY nome) c
  ON c.nome = CASE s.nome
        WHEN 'Mercado'                THEN 'Alimentação'
        WHEN 'Restaurante'            THEN 'Alimentação'
        WHEN 'Padaria'                THEN 'Alimentação'
        WHEN 'Bar'                    THEN 'Alimentação'
        WHEN 'Delivery'               THEN 'Alimentação'
        WHEN 'Combustível'            THEN 'Transporte'
        WHEN 'Transporte Público'     THEN 'Transporte'
        WHEN 'Transporte Particular'  THEN 'Transporte'
        WHEN 'Manutenção do Veículo'  THEN 'Transporte'
        WHEN 'Farmácia'               THEN 'Saúde'
        WHEN 'Médico'                 THEN 'Saúde'
        WHEN 'Plano de Saúde'         THEN 'Saúde'
        WHEN 'Curso'                  THEN 'Educação'
        WHEN 'Escola/Ensino Superior' THEN 'Educação'
        WHEN 'Água'                   THEN 'Moradia'
        WHEN 'Energia'                THEN 'Moradia'
        WHEN 'Aluguel'                THEN 'Moradia'
        WHEN 'Internet'               THEN 'Moradia'
        WHEN 'Compras'                THEN 'Lazer'
        WHEN 'Compras Online'         THEN 'Lazer'
        WHEN 'Streaming'              THEN 'Lazer'
        WHEN 'Viagens'                THEN 'Lazer'
        WHEN 'Ração'                  THEN 'Pet'
        WHEN 'Veterinário'            THEN 'Pet'
     END
SET s.classe_principal_id = c.id;
