package com.finup.cnaeapi.classePrincipal;

import jakarta.persistence.*;
import lombok.*;

/**
 * Os ids são os mesmos da tabela classe_principal do back-finup.
 */
@Table(name = "classe_principal")
@Entity(name = "ClassePrincipal")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class ClassePrincipal {

    @Id
    private Long id;
    private String nome;
}
