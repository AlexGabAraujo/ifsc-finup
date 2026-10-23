package com.finup.cnaeapi.cnae;

import com.finup.cnaeapi.classePrincipal.ClassePrincipal;
import com.finup.cnaeapi.classePrincipal.ClassePrincipalRepository;
import com.finup.cnaeapi.cnae.dto.CnaeClassificacaoResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Valida um código CNAE e o classifica com base na tabela cnae.
 * CNAE válido mas não cadastrado é classificado como "Indefinido", sem subclasse.
 */
@Service
public class CnaeService {

    private static final long CLASSE_INDEFINIDA_ID = 1L;

    private final CnaeRepository cnaeRepository;
    private final ClassePrincipalRepository classePrincipalRepository;

    public CnaeService(CnaeRepository cnaeRepository, ClassePrincipalRepository classePrincipalRepository) {
        this.cnaeRepository = cnaeRepository;
        this.classePrincipalRepository = classePrincipalRepository;
    }

    @Transactional(readOnly = true)
    public CnaeClassificacaoResponse classificar(String codigo) {
        String somenteDigitos = codigo == null ? "" : codigo.replaceAll("[^0-9]", "");
        if (somenteDigitos.length() != 7) {
            throw new CnaeInvalidoException("CNAE inválido: '" + codigo + "'. O código deve ter 7 dígitos.");
        }

        return cnaeRepository.findByCnae(somenteDigitos)
                .map(CnaeClassificacaoResponse::new)
                .orElseGet(() -> CnaeClassificacaoResponse.naoClassificado(somenteDigitos, classeIndefinida()));
    }

    private ClassePrincipal classeIndefinida() {
        return classePrincipalRepository.findById(CLASSE_INDEFINIDA_ID)
                .orElseThrow(() -> new IllegalStateException("ClassePrincipal 'Indefinido' (id 1) não cadastrada."));
    }
}
