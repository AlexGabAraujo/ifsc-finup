package com.finup.pluggy.contaBancaria;
import com.finup.pluggy.itemBancario.ItemBancario;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Table(name = "conta_bancaria")
@Entity(name = "ContaBancaria")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class ContaBancaria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_bancario_id", nullable = false)
    private ItemBancario itemBancario;

    @Column(name = "pluggy_account_id", nullable = false, unique = true)
    private String pluggyAccountId;

    @Column(name = "marketing_name")
    private String marketingName;

    @Column(nullable = false)
    private String tipo; // "BANK" ou "CREDIT"

    @Column(nullable = false)
    private String subtipo; // "CHECKING_ACCOUNT" ou "CREDIT_CARD"

    @CreationTimestamp
    @Column(name = "data_criacao", insertable = false, updatable = false)
    private LocalDateTime dataCriacao;
}