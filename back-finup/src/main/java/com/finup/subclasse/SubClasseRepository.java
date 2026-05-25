package com.finup.subclasse;

import com.finup.cnpjs.Cnpj;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubClasseRepository extends JpaRepository<SubClasse, Long> {

    List<SubClasse> findByClassePrincipalId(Long classePrincipalId);

}
