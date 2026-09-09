package com.finup.pluggy.itemBancario;

import com.finup.pessoaFisica.PessoaFisica;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Table(name = "item_bancario")
@Entity(name = "ItemBancario")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class ItemBancario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pluggy_item_id", nullable = false, unique = true)
    private String pluggyItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pessoa_fisica_id", nullable = false)
    private PessoaFisica pessoaFisica;

    @Column(name = "nome_instituicao")
    private String nomeInstituicao;

    @CreationTimestamp
    @Column(name = "data_conexao", insertable = false, updatable = false)
    private LocalDateTime dataConexao;
}