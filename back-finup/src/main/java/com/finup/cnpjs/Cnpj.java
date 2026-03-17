package com.finup.cnpjs;

import jakarta.persistence.*;
import lombok.*;

@Table(name = "cnpj")
@Entity(name = "Cnpj")
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Cnpj {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nomeEmpresa;
    private String cnpj;
}
