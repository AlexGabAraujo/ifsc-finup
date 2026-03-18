package com.finup.subclasse;

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
    String nome;
}
