package com.finup.subclasse;

import com.finup.classePrincipal.ClassePrincipal;
import jakarta.persistence.*;
import lombok.*;

@Table(name = "subclasse")
@Entity(name = "SubClasse")
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class SubClasse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nome;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classe_principal_id", nullable = false)
    private ClassePrincipal classePrincipal;
}
