package com.finup.pluggy.itemBancario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ItemBancarioRepository extends JpaRepository<ItemBancario, Long> {
    Optional<ItemBancario> findByPluggyItemId(String pluggyItemId);
    List<ItemBancario> findAllByPessoaFisicaId(Long pessoaFisicaId);
}