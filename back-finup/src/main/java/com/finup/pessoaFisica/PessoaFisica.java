package com.finup.pessoaFisica;

import com.finup.pessoaFisica.dto.UpdatePessoaFisicaRequest;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Table(name = "pessoa_fisica")
@Entity(name = "PessoaFisica")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class PessoaFisica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nome;
    private String cpf;
    private String telefone;
    private LocalDate dataNascimento;
    private LocalDateTime data_inicio;
    private LocalDateTime data_fim;
    private Boolean ativo;
    private BigDecimal metaEconomia;
    private BigDecimal metaEcoAtual;

    public void atualizarInformacoes(UpdatePessoaFisicaRequest dados) {
         if(dados.telefone() != null)
            this.telefone = dados.telefone();

        if(dados.dataNascimento() != null)
            this.dataNascimento = dados.dataNascimento();

        if(dados.nome() != null)
            this.nome = dados.nome();
    }

    public void excluir(){
        this.ativo = false;
        this.data_fim = LocalDateTime.now();
    }

}
