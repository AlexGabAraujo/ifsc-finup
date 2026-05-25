package com.finup.categoria;

import com.finup.classePrincipal.ClassePrincipal;
import com.finup.subclasse.SubClasse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    List<Categoria> findAllByPessoaFisicaId(Long pessoaFisicaId);

    Optional<Categoria> findByPessoaFisicaIdAndClassePrincipalId(Long pessoaFisicaId, Long classePrincipalId);

    Optional<Categoria> findByPessoaFisicaIdAndSubClasseId(Long pessoaFisicaId, Long subClasseId);

    // Buscar classes principais cadastradas pelo usuário
    @Query("SELECT DISTINCT c.classePrincipal FROM Categoria c WHERE c.pessoaFisica.id = :pessoaFisicaId AND c.classePrincipal IS NOT NULL")
    List<ClassePrincipal> findClassesPrincipaisByPessoaFisicaId(@Param("pessoaFisicaId") Long pessoaFisicaId);

    // Buscar subclasses de uma classe principal cadastradas pelo usuário
    @Query("SELECT DISTINCT c.subClasse FROM Categoria c WHERE c.pessoaFisica.id = :pessoaFisicaId AND c.classePrincipal.id = :classePrincipalId AND c.subClasse IS NOT NULL")
    List<SubClasse> findSubClassesByPessoaFisicaIdAndClassePrincipalId(@Param("pessoaFisicaId") Long pessoaFisicaId, @Param("classePrincipalId") Long classePrincipalId);

    // Usado no cadastro de TRANSAÇÃO — traz classes diretas E classes pai de subclasses do usuário
    @Query(value = """
        SELECT DISTINCT cp.id, cp.nome FROM categoria_personalizada cat
        JOIN subclasse s ON cat.subclasse_id = s.id
        JOIN classe_principal cp ON s.classe_principal_id = cp.id
        WHERE cat.pessoa_fisica_id = :pessoaFisicaId
        UNION
        SELECT DISTINCT cp.id, cp.nome FROM categoria_personalizada cat
        JOIN classe_principal cp ON cat.classe_principal_id = cp.id
        WHERE cat.pessoa_fisica_id = :pessoaFisicaId
    """, nativeQuery = true)
    List<Object[]> findClassesPrincipaisParaTransacaoByPessoaFisicaId(@Param("pessoaFisicaId") Long pessoaFisicaId);

    // Usado no cadastro de TRANSAÇÃO — traz subclasses do usuário filtradas pela classe principal da subclasse
    @Query(value = """
        SELECT DISTINCT s.id, s.nome FROM categoria_personalizada cat
        JOIN subclasse s ON cat.subclasse_id = s.id
        WHERE cat.pessoa_fisica_id = :pessoaFisicaId
        AND s.classe_principal_id = :classePrincipalId
    """, nativeQuery = true)
    List<Object[]> findSubClassesParaTransacaoByPessoaFisicaIdAndClassePrincipalId(
            @Param("pessoaFisicaId") Long pessoaFisicaId,
            @Param("classePrincipalId") Long classePrincipalId
    );
}
