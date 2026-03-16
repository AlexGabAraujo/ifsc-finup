package com.finup.credencial;

import com.finup.pessoaFisica.PessoaFisica;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Table(name = "credencial")
@Entity(name = "Credencial")
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Credencial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String email;
    private String senha;
    private String username;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pessoa_fisica_id", nullable = false)
    private PessoaFisica pessoaFisica;
}
