package com.finup.classePrincipal;

import jakarta.persistence.*;
import lombok.*;

@Table(name = "classe_principal")
@Entity(name = "ClassePrincipal")
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class ClassePrincipal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    String nome;
}
