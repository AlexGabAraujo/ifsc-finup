package com.finup.cnpjs;

import com.finup.classePrincipal.ClassePrincipal;
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
     * consultado na API do CNPJá. Pode ser nulo se não houver mapeamento.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classe_principal_id", nullable = true)
    private ClassePrincipal classePrincipal;
}
