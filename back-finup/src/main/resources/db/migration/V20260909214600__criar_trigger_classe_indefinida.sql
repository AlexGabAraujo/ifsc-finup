DELIMITER $$

CREATE TRIGGER criar_classe_indefinida
    AFTER INSERT ON pessoa_fisica
    FOR EACH ROW
BEGIN
    INSERT INTO categoria_personalizada (pessoa_fisica_id, classe_principal_id)
    VALUES (NEW.id, 1);
    END$$

DELIMITER ;