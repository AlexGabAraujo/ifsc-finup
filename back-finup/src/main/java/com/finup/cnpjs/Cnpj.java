package com.finup.cnpjs;

import com.finup.classePrincipal.ClassePrincipal;
import com.finup.subclasse.SubClasse;
import jakarta.persistence.*;
import lombok.*;

@Table(name = "cnpj")
@Entity(name = "Cnpj")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Cnpj {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nomeEmpresa;
    private String cnpj;

    /**
     * ClassePrincipal inferida a partir do CNAE principal da empresa,
     * consultado na API do CNPJá e classificado pela cnae-api. "Indefinido" quando o CNAE não está classificado.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classe_principal_id", nullable = true)
    private ClassePrincipal classePrincipal;

    /**
     * SubClasse retornada pela cnae-api para o CNAE principal. Nula quando o CNAE não tem subclasse.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subclasse_id", nullable = true)
    private SubClasse subClasse;
}
