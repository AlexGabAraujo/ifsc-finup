package com.finup.openai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class IaResponseParser {

    private static final Logger log = LoggerFactory.getLogger(IaResponseParser.class);

    private static final Pattern VALOR_PATTERN     = Pattern.compile("VALOR:\\s*([\\d.,]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CNPJ_PATTERN      = Pattern.compile("CNPJ:\\s*(\\d{14}|null)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CATEGORIA_PATTERN = Pattern.compile("CATEGORIA:\\s*(.+)", Pattern.CASE_INSENSITIVE);

    private IaResponseParser() {}

    public static Optional<BigDecimal> parseValor(String resposta) {
        if (resposta == null) return Optional.empty();
        Matcher m = VALOR_PATTERN.matcher(resposta);
        if (!m.find()) return Optional.empty();
        try {
            String raw = m.group(1).trim().replace(".", "").replace(",", ".");
            return Optional.of(new BigDecimal(raw));
        } catch (NumberFormatException e) {
            log.warn("Nao foi possivel converter valor '{}' para BigDecimal.", m.group(1));
            return Optional.empty();
        }
    }

    public static Optional<String> parseCnpj(String resposta) {
        if (resposta == null) return Optional.empty();
        Matcher m = CNPJ_PATTERN.matcher(resposta);
        if (!m.find()) return Optional.empty();
        String val = m.group(1).trim();
        return "null".equalsIgnoreCase(val) ? Optional.empty() : Optional.of(val);
    }

    public static Optional<String> parseCategoria(String resposta) {
        if (resposta == null) return Optional.empty();
        Matcher m = CATEGORIA_PATTERN.matcher(resposta);
        if (!m.find()) return Optional.empty();
        String val = m.group(1).trim();
        return "null".equalsIgnoreCase(val) ? Optional.empty() : Optional.of(val);
    }
}
