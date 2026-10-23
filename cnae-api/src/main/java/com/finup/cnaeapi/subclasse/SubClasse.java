package com.finup.cnaeapi.subclasse;

import com.finup.cnaeapi.classePrincipal.ClassePrincipal;
import jakarta.persistence.*;
import lombok.*;

/**
 * Os ids são os mesmos da tabela subclasse do back-finup.
 */
@Table(name = "subclasse")
@Entity(name = "SubClasse")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class SubClasse {

    @Id
    private Long id;
    private String nome;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classe_principal_id", nullable = false)
    private ClassePrincipal classePrincipal;
}
