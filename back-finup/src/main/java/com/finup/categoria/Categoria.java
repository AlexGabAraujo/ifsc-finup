package com.finup.categoria;

import com.finup.classePrincipal.ClassePrincipal;
import com.finup.pessoaFisica.PessoaFisica;
import com.finup.subclasse.SubClasse;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
@Entity
@Table(name = "categoria_personalizada")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Categoria {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "icone")
    private String icone;
    private String cor;

    private BigDecimal orcamento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pessoa_fisica_id", nullable = false)
    private PessoaFisica pessoaFisica;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subclasse_id", nullable = true)
    private SubClasse subClasse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classe_principal_id", nullable = true)
    private ClassePrincipal classePrincipal;


}
