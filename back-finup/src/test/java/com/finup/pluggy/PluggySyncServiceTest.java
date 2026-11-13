package com.finup.pluggy;

import ai.pluggy.client.PluggyApiService;
import ai.pluggy.client.PluggyClient;
import ai.pluggy.client.request.TransactionsSearchRequest;
import ai.pluggy.client.response.Account;
import ai.pluggy.client.response.AccountsResponse;
import ai.pluggy.client.response.Merchant;
import ai.pluggy.client.response.Transaction;
import ai.pluggy.client.response.TransactionType;
import ai.pluggy.client.response.TransactionsResponse;
import com.finup.categoria.Categoria;
import com.finup.categoria.CategoriaRepository;
import com.finup.classePrincipal.ClassePrincipal;
import com.finup.classePrincipal.ClassePrincipalRepository;
import com.finup.cnpjs.Cnpj;
import com.finup.cnpjs.CnpjService;
import com.finup.pessoaFisica.PessoaFisica;
import com.finup.pessoaFisica.PessoaFisicaRepository;
import com.finup.pluggy.contaBancaria.ContaBancaria;
import com.finup.pluggy.contaBancaria.ContaBancariaRepository;
import com.finup.pluggy.itemBancario.ItemBancario;
import com.finup.pluggy.itemBancario.ItemBancarioRepository;
import com.finup.subclasse.SubClasse;
import com.finup.transacao.TipoGasto;
import com.finup.transacao.TipoPagamento;
import com.finup.transacao.Transacao;
import com.finup.transacao.TransacaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import retrofit2.Call;
import retrofit2.Response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Garante que o sync grava exatamente o que gravava antes, só que com menos idas ao banco:
 * uma consulta de "já importadas" e um saveAll por página, e sem repetir buscas de
 * CNPJ/categoria já resolvidas na mesma execução.
 */
class PluggySyncServiceTest {

    private static final String ITEM_ID = "item-1";
    private static final String CONTA_ID = "conta-1";
    private static final String CNPJ_MERCADO = "11222333000181";
    private static final String CNPJ_POSTO = "11444777000161";

    private final ItemBancarioRepository itemRepo = mock(ItemBancarioRepository.class);
    private final ContaBancariaRepository contaRepo = mock(ContaBancariaRepository.class);
    private final TransacaoRepository transacaoRepo = mock(TransacaoRepository.class);
    private final PessoaFisicaRepository pessoaRepo = mock(PessoaFisicaRepository.class);
    private final CnpjService cnpjService = mock(CnpjService.class);
    private final ClassePrincipalRepository classeRepo = mock(ClassePrincipalRepository.class);
    private final CategoriaRepository categoriaRepo = mock(CategoriaRepository.class);
    private final PluggyApiService api = mock(PluggyApiService.class);

    private final PessoaFisica pessoa = PessoaFisica.builder().id(7L).build();
    private final ItemBancario item = ItemBancario.builder().id(1L).pluggyItemId(ITEM_ID).pessoaFisica(pessoa).build();
    private final ContaBancaria conta = ContaBancaria.builder().id(3L).pluggyAccountId(CONTA_ID).tipo("BANK").build();

    private final ClassePrincipal indefinido = ClassePrincipal.builder().id(99L).nome("Indefinido").build();
    private final ClassePrincipal alimentacao = ClassePrincipal.builder().id(10L).nome("Alimentação").build();
    private final SubClasse mercado = SubClasse.builder().id(20L).nome("Mercado").classePrincipal(alimentacao).build();

    private final Categoria categoriaMercado = Categoria.builder().id(100L).build();
    private final Categoria categoriaPadrao = Categoria.builder().id(101L).build();

    private final Cnpj empresaMercado = Cnpj.builder().id(50L).cnpj(CNPJ_MERCADO)
            .classePrincipal(alimentacao).subClasse(mercado).build();

    private PluggySyncService service;

    @BeforeEach
    void setUp() throws Exception {
        service = new PluggySyncService(itemRepo, contaRepo, transacaoRepo, pessoaRepo, cnpjService, classeRepo);
        PluggyClient pluggy = mock(PluggyClient.class);
        when(pluggy.service()).thenReturn(api);
        ReflectionTestUtils.setField(service, "pluggy", pluggy);
        ReflectionTestUtils.setField(service, "categoriaRepository", categoriaRepo);

        when(itemRepo.findByPluggyItemId(ITEM_ID)).thenReturn(Optional.of(item));
        when(contaRepo.findByPluggyAccountId(CONTA_ID)).thenReturn(Optional.of(conta));
        when(classeRepo.findFirstByNomeOrderByIdAsc("Indefinido")).thenReturn(Optional.of(indefinido));

        when(categoriaRepo.findByPessoaFisicaIdAndSubClasseId(7L, 20L)).thenReturn(Optional.of(categoriaMercado));
        when(categoriaRepo.findByPessoaFisicaIdAndClassePrincipalId(7L, 99L)).thenReturn(Optional.empty());
        when(categoriaRepo.findFirstByPessoaFisicaId(7L)).thenReturn(categoriaPadrao);

        when(cnpjService.buscarOuCriarCnpj(CNPJ_MERCADO)).thenReturn(Optional.of(empresaMercado));
        when(transacaoRepo.findAllByExternalIdIn(anyCollection())).thenReturn(new ArrayList<>());

        Account account = mock(Account.class);
        when(account.getId()).thenReturn(CONTA_ID);
        when(account.getType()).thenReturn("BANK");
        AccountsResponse accounts = mock(AccountsResponse.class);
        when(accounts.getResults()).thenReturn(List.of(account));
        Call<AccountsResponse> chamadaContas = chamada(accounts);
        when(api.getAccounts(ITEM_ID)).thenReturn(chamadaContas);
    }

    @Test
    void importaTransacoesNovasComAMesmaClassificacaoDeAntes() throws Exception {
        pagina(
                tx("tx-1", -50.25, TransactionType.DEBIT, "2026-11-10T12:00:00.000Z", CNPJ_MERCADO),
                tx("tx-2", 1000.0, TransactionType.CREDIT, "2026-11-11T08:00:00.000Z", null));

        service.sincronizarTransacoes(ITEM_ID);

        List<Transacao> salvas = salvasNaPagina();
        assertThat(salvas).hasSize(2);

        Transacao compra = salvas.get(0);
        assertThat(compra.getExternalId()).isEqualTo("tx-1");
        assertThat(compra.getValor()).isEqualByComparingTo(new BigDecimal("50.25"));
        assertThat(compra.getTipoGasto()).isEqualTo(TipoGasto.DEBITO);
        assertThat(compra.getTipoPagamento()).isEqualTo(TipoPagamento.TRANSFERENCIA);
        assertThat(compra.getDataTransacao()).isEqualTo(LocalDateTime.of(2026, 11, 10, 0, 0));
        assertThat(compra.getPessoaFisica()).isSameAs(pessoa);
        assertThat(compra.getContaBancaria()).isSameAs(conta);
        assertThat(compra.getCnpj()).isSameAs(empresaMercado);
        assertThat(compra.getClassePrincipal()).isSameAs(alimentacao);
        assertThat(compra.getSubClasse()).isSameAs(mercado);
        assertThat(compra.getCategoria()).isSameAs(categoriaMercado);

        Transacao salario = salvas.get(1);
        assertThat(salario.getExternalId()).isEqualTo("tx-2");
        assertThat(salario.getTipoGasto()).isEqualTo(TipoGasto.CREDITO);
        assertThat(salario.getCnpj()).isNull();
        assertThat(salario.getClassePrincipal()).isSameAs(indefinido);
        assertThat(salario.getCategoria()).isSameAs(categoriaPadrao);
    }

    @Test
    void fazUmaConsultaEUmSaveAllPorPaginaESemRepetirBuscasJaResolvidas() throws Exception {
        pagina(
                tx("tx-1", -10.0, TransactionType.DEBIT, "2026-11-01", CNPJ_MERCADO),
                tx("tx-2", -20.0, TransactionType.DEBIT, "2026-11-02", CNPJ_MERCADO),
                tx("tx-3", -30.0, TransactionType.DEBIT, "2026-11-03", CNPJ_MERCADO),
                tx("tx-4", -40.0, TransactionType.DEBIT, "2026-11-04", null),
                tx("tx-5", -50.0, TransactionType.DEBIT, "2026-11-05", null));

        service.sincronizarTransacoes(ITEM_ID);

        assertThat(salvasNaPagina()).extracting(Transacao::getExternalId)
                .containsExactly("tx-1", "tx-2", "tx-3", "tx-4", "tx-5");

        verify(transacaoRepo, times(1)).findAllByExternalIdIn(anyCollection());
        verify(transacaoRepo, never()).findByExternalId(anyString());
        verify(transacaoRepo, never()).save(any(Transacao.class));
        verify(cnpjService, times(1)).buscarOuCriarCnpj(CNPJ_MERCADO);
        verify(categoriaRepo, times(1)).findByPessoaFisicaIdAndSubClasseId(7L, 20L);
        verify(categoriaRepo, times(1)).findByPessoaFisicaIdAndClassePrincipalId(7L, 99L);
        verify(categoriaRepo, times(1)).findFirstByPessoaFisicaId(7L);
    }

    @Test
    void cnpjQueFalhouContinuaSendoTentadoDeNovoNaMesmaExecucao() throws Exception {
        // 1ª consulta falha (ex.: limite do CNPJá), a 2ª já responde — como antes, a segunda
        // transação sai classificada e a primeira fica para o próximo sync.
        when(cnpjService.buscarOuCriarCnpj(CNPJ_POSTO))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(Cnpj.builder().id(51L).cnpj(CNPJ_POSTO).classePrincipal(alimentacao).build()));
        when(categoriaRepo.findByPessoaFisicaIdAndClassePrincipalId(7L, 10L)).thenReturn(Optional.empty());
        pagina(
                tx("tx-1", -10.0, TransactionType.DEBIT, "2026-11-01", CNPJ_POSTO),
                tx("tx-2", -20.0, TransactionType.DEBIT, "2026-11-02", CNPJ_POSTO));

        service.sincronizarTransacoes(ITEM_ID);

        List<Transacao> salvas = salvasNaPagina();
        verify(cnpjService, times(2)).buscarOuCriarCnpj(CNPJ_POSTO);
        assertThat(salvas.get(0).getCnpj()).isNull();
        assertThat(salvas.get(0).getClassePrincipal()).isSameAs(indefinido);
        assertThat(salvas.get(1).getCnpj()).isNotNull();
        assertThat(salvas.get(1).getClassePrincipal()).isSameAs(alimentacao);
    }

    @Test
    void transacaoJaImportadaSoEhSalvaSeMudou() throws Exception {
        Transacao semClassificacao = Transacao.builder().id(1L).externalId("tx-1").pessoaFisica(pessoa)
                .tipoGasto(TipoGasto.DEBITO).classePrincipal(indefinido).build();
        Transacao tipoErrado = Transacao.builder().id(2L).externalId("tx-2").pessoaFisica(pessoa)
                .tipoGasto(TipoGasto.DEBITO).classePrincipal(alimentacao).build();
        Transacao igual = Transacao.builder().id(3L).externalId("tx-3").pessoaFisica(pessoa)
                .tipoGasto(TipoGasto.DEBITO).classePrincipal(alimentacao).build();
        when(transacaoRepo.findAllByExternalIdIn(anyCollection()))
                .thenReturn(new ArrayList<>(List.of(semClassificacao, tipoErrado, igual)));
        pagina(
                tx("tx-1", -10.0, TransactionType.DEBIT, "2026-11-01", CNPJ_MERCADO),
                tx("tx-2", 20.0, TransactionType.CREDIT, "2026-11-02", null),
                tx("tx-3", -30.0, TransactionType.DEBIT, "2026-11-03", null));

        service.sincronizarTransacoes(ITEM_ID);

        assertThat(salvasNaPagina()).containsExactly(semClassificacao, tipoErrado);
        assertThat(semClassificacao.getCnpj()).isSameAs(empresaMercado);
        assertThat(semClassificacao.getSubClasse()).isSameAs(mercado);
        assertThat(semClassificacao.getCategoria()).isSameAs(categoriaMercado);
        assertThat(tipoErrado.getTipoGasto()).isEqualTo(TipoGasto.CREDITO);
    }

    @Test
    void semNadaNovoNemAlteradoNaoGravaNada() throws Exception {
        Transacao igual = Transacao.builder().id(3L).externalId("tx-3").pessoaFisica(pessoa)
                .tipoGasto(TipoGasto.DEBITO).classePrincipal(alimentacao).build();
        when(transacaoRepo.findAllByExternalIdIn(anyCollection())).thenReturn(new ArrayList<>(List.of(igual)));
        pagina(tx("tx-3", -30.0, TransactionType.DEBIT, "2026-11-03", null));

        service.sincronizarTransacoes(ITEM_ID);

        verify(transacaoRepo, never()).saveAll(any());
        verify(transacaoRepo, never()).save(any(Transacao.class));
    }

    @Test
    void idRepetidoNaMesmaPaginaViraUmaUnicaTransacao() throws Exception {
        pagina(
                tx("tx-1", -10.0, TransactionType.DEBIT, "2026-11-01", null),
                tx("tx-1", -10.0, TransactionType.DEBIT, "2026-11-01", null));

        service.sincronizarTransacoes(ITEM_ID);

        assertThat(salvasNaPagina()).extracting(Transacao::getExternalId).containsExactly("tx-1");
    }

    // ── apoio ─────────────────────────────────────────────────────────────────

    private void pagina(Transaction... transacoes) {
        TransactionsResponse resposta = mock(TransactionsResponse.class);
        when(resposta.getResults()).thenReturn(Arrays.asList(transacoes));
        when(resposta.getTotalPages()).thenReturn(1);
        Call<TransactionsResponse> chamadaTransacoes = chamada(resposta);
        when(api.getTransactions(eq(CONTA_ID), any(TransactionsSearchRequest.class))).thenReturn(chamadaTransacoes);
    }

    private static Transaction tx(String id, double valor, TransactionType tipo, String data, String cnpj) {
        Transaction t = mock(Transaction.class);
        when(t.getId()).thenReturn(id);
        when(t.getAmount()).thenReturn(valor);
        when(t.getType()).thenReturn(tipo);
        when(t.getDate()).thenReturn(data);
        if (cnpj != null) {
            Merchant merchant = mock(Merchant.class);
            when(merchant.getCnpj()).thenReturn(cnpj);
            when(t.getMerchant()).thenReturn(merchant);
        }
        return t;
    }

    @SuppressWarnings("unchecked")
    private static <T> Call<T> chamada(T corpo) {
        Call<T> call = mock(Call.class);
        try {
            when(call.execute()).thenReturn(Response.success(corpo));
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
        return call;
    }

    @SuppressWarnings("unchecked")
    private List<Transacao> salvasNaPagina() {
        ArgumentCaptor<List<Transacao>> captor = ArgumentCaptor.forClass(List.class);
        verify(transacaoRepo, times(1)).saveAll(captor.capture());
        return captor.getValue();
    }
}
