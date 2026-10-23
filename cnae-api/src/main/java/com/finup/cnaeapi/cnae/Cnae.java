package com.finup.cnaeapi.cnae;

import com.finup.cnaeapi.classePrincipal.ClassePrincipal;
import com.finup.cnaeapi.subclasse.SubClasse;
import jakarta.persistence.*;
import lombok.*;

@Table(name = "cnae")
@Entity(name = "Cnae")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Cnae {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String cnae;
    private String descricao;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classe_principal_id", nullable = false)
    private ClassePrincipal classePrincipal;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subclasse_id")
    private SubClasse subClasse;
}
