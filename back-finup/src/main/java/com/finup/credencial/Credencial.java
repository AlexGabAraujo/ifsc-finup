package com.finup.credencial;

import com.finup.credencial.dtos.UpdateCredencialRequest;
import com.finup.pessoaFisica.PessoaFisica;
import com.finup.pessoaFisica.dto.UpdatePessoaFisicaRequest;
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

    public void atualizarInformacoes(UpdateCredencialRequest dados) {
        if(dados.email() != null)
            this.email = dados.email();

        if(dados.senha() != null)
            this.senha = dados.senha();

        if(dados.username() != null)
            this.username = dados.username();
    }
}
