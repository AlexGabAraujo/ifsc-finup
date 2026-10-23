package com.finup.cnpjs;

import com.finup.classePrincipal.ClassePrincipal;
import com.finup.classePrincipal.ClassePrincipalRepository;
import com.finup.cnpjs.client.CnaeApiClient;
import com.finup.cnpjs.client.CnpjaClient;
import com.finup.cnpjs.client.ServicoExternoIndisponivelException;
import com.finup.cnpjs.client.dto.CnaeClassificacaoResponse;
import com.finup.cnpjs.client.dto.CnpjaOfficeResponse;
import com.finup.subclasse.SubClasse;
import com.finup.subclasse.SubClasseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Serviço responsável por enriquecer um CNPJ com dados da API do CNPJá
 * e mapeá-lo para uma ClassePrincipal do sistema.
 *
 * Fluxo:
 *   1. Verifica o cache local (tabela cnpj).
 *   2. Se não encontrado, consulta a API do CNPJá.
 *   3. Persiste o resultado no cache local e retorna o Cnpj enriquecido.
 */
@Service
public class CnpjService {

    private static final Logger log = LoggerFactory.getLogger(CnpjService.class);
    private static final String CLASSE_INDEFINIDA = "Indefinido";

    private final CnpjRepository cnpjRepository;
    private final ClassePrincipalRepository classePrincipalRepository;
    private final SubClasseRepository subClasseRepository;
    private final CnaeApiClient cnaeApiClient;
    private final CnpjaClient cnpjaClient;

    public CnpjService(CnpjRepository cnpjRepository,
                       ClassePrincipalRepository classePrincipalRepository,
                       SubClasseRepository subClasseRepository,
                       CnaeApiClient cnaeApiClient,
                       CnpjaClient cnpjaClient) {
        this.cnpjRepository = cnpjRepository;
        this.classePrincipalRepository = classePrincipalRepository;
        this.subClasseRepository = subClasseRepository;
        this.cnaeApiClient = cnaeApiClient;
        this.cnpjaClient = cnpjaClient;
    }

    /**
     * Retorna um Cnpj enriquecido para o número informado.
     * Consulta o cache local primeiro; se não encontrar, chama a API do CNPJá,
     * classifica o CNAE principal na cnae-api e salva o resultado na tabela cnpj.
     *
     * @param numeroCnpj CNPJ em qualquer formato (com ou sem pontuação)
     * @return Optional com o Cnpj persistido, ou vazio se o CNPJá/cnae-api estiverem indisponíveis
     */
    @Transactional
    public Optional<Cnpj> buscarOuCriarCnpj(String numeroCnpj) {
        if (numeroCnpj == null || numeroCnpj.isBlank()) {
            return Optional.empty();
        }

        String cnpjLimpo = numeroCnpj.replaceAll("[^0-9]", "");

        // 1. Verifica cache local
        Optional<Cnpj> cached = cnpjRepository.findByCnpj(cnpjLimpo);
        if (cached.isPresent()) {
            log.debug("CNPJ {} encontrado no cache local.", cnpjLimpo);
            return cached;
        }

        // 2-4. CNPJá -> cnae-api -> salva. Em falha temporária não salva nada,
        // para que o CNPJ seja consultado de novo na próxima transação.
        try {
            return consultarEClassificar(cnpjLimpo);
        } catch (ServicoExternoIndisponivelException e) {
            log.warn("CNPJ {} não classificado agora (serviço externo indisponível). Será consultado novamente depois.", cnpjLimpo);
            return Optional.empty();
        }
    }

    /**
     * Consulta o CNPJá, classifica o CNAE principal na cnae-api e salva na tabela cnpj.
     * CNPJ inexistente no CNPJá é salvo como "Indefinido".
     *
     * @throws ServicoExternoIndisponivelException se o CNPJá ou a cnae-api falharem temporariamente
     */
    private Optional<Cnpj> consultarEClassificar(String cnpjLimpo) {
        // 2. Consulta a API do CNPJá
        log.info("Consultando API do CNPJá para CNPJ: {}", cnpjLimpo);
        Optional<CnpjaOfficeResponse> resposta = cnpjaClient.consultarCnpj(cnpjLimpo);

        if (resposta.isEmpty()) {
            log.warn("API do CNPJá não retornou dados para {}. Salvando CNPJ sem enriquecimento.", cnpjLimpo);
            Cnpj cnpjSemDados = Cnpj.builder()
                    .cnpj(cnpjLimpo)
                    .nomeEmpresa(null)
                    .classePrincipal(classeIndefinida())
                    .build();
            return Optional.of(cnpjRepository.save(cnpjSemDados));
        }

        CnpjaOfficeResponse dados = resposta.get();

        // 3. Classifica pelo CNAE principal na cnae-api (não cadastrado → Indefinido)
        String codigoCnae = dados.mainActivity() != null ? dados.mainActivity().codigoCnae() : null;
        Classificacao classificacao = classificar(codigoCnae);

        // 4. Persiste o Cnpj enriquecido
        Cnpj cnpj = Cnpj.builder()
                .cnpj(cnpjLimpo)
                .nomeEmpresa(dados.nomeExibicao())
                .classePrincipal(classificacao.classePrincipal())
                .subClasse(classificacao.subClasse())
                .build();

        Cnpj salvo = cnpjRepository.save(cnpj);
        log.info("CNPJ {} salvo: empresa='{}', classe='{}', subclasse='{}'",
                cnpjLimpo,
                salvo.getNomeEmpresa(),
                classificacao.classePrincipal().getNome(),
                classificacao.subClasse() != null ? classificacao.subClasse().getNome() : null);

        return Optional.of(salvo);
    }

    private record Classificacao(ClassePrincipal classePrincipal, SubClasse subClasse) {
    }

    /**
     * Consulta a cnae-api e retorna a ClassePrincipal e a SubClasse correspondentes ao CNAE.
     * Sem CNAE ou CNAE não cadastrado → "Indefinido", sem subclasse.
     * Se a cnae-api estiver indisponível, lança ServicoExternoIndisponivelException.
     */
    private Classificacao classificar(String codigoCnae) {
        if (codigoCnae != null && !codigoCnae.isBlank()) {
            // A cnae-api tem banco próprio: os ids de lá podem não bater com os deste banco,
            // por isso a classe e a subclasse são localizadas pelo NOME retornado.
            Optional<CnaeClassificacaoResponse> resposta = cnaeApiClient.classificar(codigoCnae);
            Optional<ClassePrincipal> classe = resposta
                    .map(c -> c.classePrincipal().nome())
                    .flatMap(classePrincipalRepository::findFirstByNomeOrderByIdAsc);
            if (classe.isPresent()) {
                SubClasse subClasse = resposta
                        .map(CnaeClassificacaoResponse::subClasse)
                        .flatMap(s -> subClasseRepository
                                .findFirstByNomeAndClassePrincipalIdOrderByIdAsc(s.nome(), classe.get().getId()))
                        .orElse(null);
                return new Classificacao(classe.get(), subClasse);
            }
        }

        return new Classificacao(classeIndefinida(), null);
    }

    private ClassePrincipal classeIndefinida() {
        return classePrincipalRepository.findFirstByNomeOrderByIdAsc(CLASSE_INDEFINIDA)
                .orElseThrow(() -> new IllegalStateException("ClassePrincipal 'Indefinido' não cadastrada."));
    }
}
