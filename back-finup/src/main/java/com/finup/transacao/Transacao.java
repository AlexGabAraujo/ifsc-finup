package com.finup.transacao;

import com.finup.classePrincipal.ClassePrincipal;
import com.finup.cnpjs.Cnpj;
import com.finup.pessoaFisica.PessoaFisica;
import com.finup.subclasse.SubClasse;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Table(name = "transacao")
@Entity(name = "Transacao")
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Transacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    BigDecimal valor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pessoa_fisica_id", nullable = false)
    PessoaFisica pessoaFisica;

    TipoPagamento tipoPagamento;
    TipoGasto tipoGasto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subclasse_id", nullable = true)
    SubClasse subClasse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classe_principal_id", nullable = true)
    ClassePrincipal classePrincipal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cnpj_id", nullable = true)
    Cnpj cnpj;
}
