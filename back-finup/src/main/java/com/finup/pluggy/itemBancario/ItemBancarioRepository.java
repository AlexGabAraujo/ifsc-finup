package com.finup.pluggy.itemBancario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ItemBancarioRepository extends JpaRepository<ItemBancario, Long> {
    Optional<ItemBancario> findByPluggyItemId(String pluggyItemId);
}