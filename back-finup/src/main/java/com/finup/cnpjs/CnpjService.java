package com.finup.cnpjs;

import com.finup.classePrincipal.ClassePrincipal;
import com.finup.classePrincipal.ClassePrincipalRepository;
import com.finup.cnpjs.client.CnpjaClient;
import com.finup.cnpjs.client.dto.CnpjaActivityDto;
import com.finup.cnpjs.client.dto.CnpjaOfficeResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
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

    private final CnpjRepository cnpjRepository;
    private final ClassePrincipalRepository classePrincipalRepository;
    private final CnpjaClient cnpjaClient;

    /**
     * Mapeamento de prefixo de código CNAE → nome da ClassePrincipal.
     *
     * O código CNAE tem 7 dígitos. Usamos os primeiros 2 (seção/divisão)
     * como chave de mapeamento, do mais específico para o mais genérico.
     *
     * Referência: https://concla.ibge.gov.br/busca-online-cnae.html
     */
    private static final Map<String, String> CNAE_PARA_CLASSE = Map.ofEntries(
            // Alimentação (seções 10, 11, 47.1, 56)
            Map.entry("101", "Alimentação"),  // Abate e fabricação de produtos de carne
            Map.entry("102", "Alimentação"),  // Fabricação de produtos do pescado
            Map.entry("103", "Alimentação"),  // Conservas de frutas/legumes
            Map.entry("104", "Alimentação"),  // Fabricação de óleos e gorduras
            Map.entry("105", "Alimentação"),  // Laticínios
            Map.entry("106", "Alimentação"),  // Moagem, farinhas e amidos
            Map.entry("107", "Alimentação"),  // Fabricação de açúcar
            Map.entry("108", "Alimentação"),  // Torrefação e moagem de café
            Map.entry("109", "Alimentação"),  // Outros alimentos
            Map.entry("110", "Alimentação"),  // Fabricação de bebidas
            Map.entry("471", "Alimentação"),  // Comércio varejista de produtos alimentícios
            Map.entry("472", "Alimentação"),  // Comércio varejista de carnes e pescados
            Map.entry("561", "Alimentação"),  // Restaurantes e similares
            Map.entry("562", "Alimentação"),  // Serviços de catering / fornecimento de alimentos

            // Transporte (seções 45, 49, 50, 51, 52)
            Map.entry("451", "Transporte"),   // Comércio de veículos
            Map.entry("452", "Transporte"),   // Manutenção e reparação de veículos
            Map.entry("473", "Transporte"),   // Comércio varejista de combustíveis
            Map.entry("491", "Transporte"),   // Transporte ferroviário
            Map.entry("492", "Transporte"),   // Transporte rodoviário de passageiros
            Map.entry("493", "Transporte"),   // Transporte rodoviário de carga
            Map.entry("501", "Transporte"),   // Transporte aquaviário
            Map.entry("511", "Transporte"),   // Transporte aéreo
            Map.entry("522", "Transporte"),   // Atividades auxiliares de transporte

            // Saúde (seções 21, 47.3, 86, 87, 88)
            Map.entry("212", "Saúde"),        // Fabricação de medicamentos
            Map.entry("477", "Saúde"),        // Comércio varejista de artigos farmacêuticos
            Map.entry("861", "Saúde"),        // Atividades hospitalares
            Map.entry("862", "Saúde"),        // Serviços médicos ambulatoriais
            Map.entry("863", "Saúde"),        // Odontologia
            Map.entry("864", "Saúde"),        // Fisioterapia e ocupacional
            Map.entry("865", "Saúde"),        // Atividades de psicologia e psicanálise
            Map.entry("869", "Saúde"),        // Outros serviços de saúde

            // Educação (seções 85)
            Map.entry("851", "Educação"),     // Educação infantil e fundamental
            Map.entry("852", "Educação"),     // Ensino médio
            Map.entry("853", "Educação"),     // Educação superior
            Map.entry("854", "Educação"),     // Educação profissional
            Map.entry("855", "Educação"),     // Cursos livres / outros
            Map.entry("856", "Educação"),     // Atividades de suporte à educação

            // Moradia (seções 41, 43, 68)
            Map.entry("410", "Moradia"),      // Construção de edifícios
            Map.entry("411", "Moradia"),      // Incorporação imobiliária
            Map.entry("432", "Moradia"),      // Instalações elétricas, hidráulicas
            Map.entry("433", "Moradia"),      // Obras de acabamento
            Map.entry("681", "Moradia"),      // Atividades imobiliárias
            Map.entry("682", "Moradia"),      // Gestão e administração de imóveis

            // Academia (seção 93.1)
            Map.entry("931", "Academia"),     // Atividades esportivas
            Map.entry("932", "Academia"),     // Parques de diversão e temáticos

            // Lazer (seções 59, 60, 90, 91, 93.2, 47.x varejo geral)
            Map.entry("591", "Lazer"),        // Cinema e vídeo
            Map.entry("601", "Lazer"),        // Rádio
            Map.entry("602", "Lazer"),        // Televisão
            Map.entry("631", "Lazer"),        // Tratamento de dados / streaming
            Map.entry("900", "Lazer"),        // Artes cênicas
            Map.entry("910", "Lazer"),        // Bibliotecas, arquivos, museus
            Map.entry("474", "Lazer"),        // Varejo de equipamentos eletrônicos
            Map.entry("476", "Lazer"),        // Varejo de livros, revistas
            Map.entry("479", "Lazer"),        // Comércio varejista não especializado

            // Pet (seção 75)
            Map.entry("750", "Pet"),          // Atividades veterinárias
            Map.entry("751", "Pet"),          // Veterinária
            Map.entry("478", "Pet")           // Varejo de animais e artigos pet
    );

    public CnpjService(CnpjRepository cnpjRepository,
                       ClassePrincipalRepository classePrincipalRepository,
                       CnpjaClient cnpjaClient) {
        this.cnpjRepository = cnpjRepository;
        this.classePrincipalRepository = classePrincipalRepository;
        this.cnpjaClient = cnpjaClient;
    }

    /**
     * Retorna um Cnpj enriquecido para o número informado.
     * Consulta o cache local primeiro; se não encontrar, chama a API do CNPJá.
     *
     * @param numeroCnpj CNPJ em qualquer formato (com ou sem pontuação)
     * @return Optional com o Cnpj persistido, ou vazio se a consulta falhar
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

        // 2. Consulta a API do CNPJá
        log.info("Consultando API do CNPJá para CNPJ: {}", cnpjLimpo);
        Optional<CnpjaOfficeResponse> resposta = cnpjaClient.consultarCnpj(cnpjLimpo);

        if (resposta.isEmpty()) {
            log.warn("API do CNPJá não retornou dados para {}. Salvando CNPJ sem enriquecimento.", cnpjLimpo);
            Cnpj cnpjSemDados = Cnpj.builder()
                    .cnpj(cnpjLimpo)
                    .nomeEmpresa(null)
                    .build();
            return Optional.of(cnpjRepository.save(cnpjSemDados));
        }

        CnpjaOfficeResponse dados = resposta.get();

        // 3. Mapeia CNAE principal para ClassePrincipal
        ClassePrincipal classePrincipal = resolverClassePrincipal(dados.mainActivity());

        // 4. Persiste o Cnpj enriquecido
        Cnpj cnpj = Cnpj.builder()
                .cnpj(cnpjLimpo)
                .nomeEmpresa(dados.nomeExibicao())
                .classePrincipal(classePrincipal)
                .build();

        Cnpj salvo = cnpjRepository.save(cnpj);
        log.info("CNPJ {} salvo: empresa='{}', classe='{}'",
                cnpjLimpo,
                salvo.getNomeEmpresa(),
                classePrincipal != null ? classePrincipal.getNome() : "sem mapeamento");

        return Optional.of(salvo);
    }

    /**
     * Tenta encontrar uma ClassePrincipal correspondente ao CNAE informado.
     * A busca usa os primeiros 3 dígitos do código CNAE como chave.
     * Se não encontrar mapeamento, retorna null (a transação ficará sem categoria automática).
     */
    private ClassePrincipal resolverClassePrincipal(CnpjaActivityDto atividade) {
        if (atividade == null || atividade.id() == null || atividade.id().isBlank()) {
            return null;
        }

        // Código CNAE tem 7 dígitos. Usamos os primeiros 3 para o lookup.
        String codigoCnae = atividade.id().replaceAll("[^0-9]", "");
        if (codigoCnae.length() < 3) {
            return null;
        }

        String prefixo = codigoCnae.substring(0, 3);
        String nomeClasse = CNAE_PARA_CLASSE.get(prefixo);

        if (nomeClasse == null) {
            log.debug("Nenhum mapeamento de ClassePrincipal para CNAE prefixo '{}' ({})", prefixo, atividade.text());
            return null;
        }

        return classePrincipalRepository.findByNome(nomeClasse).orElseGet(() -> {
            log.warn("ClassePrincipal '{}' mapeada para CNAE '{}' não existe no banco.", nomeClasse, prefixo);
            return null;
        });
    }
}
