package com.finup.transacao.dtos;

import java.util.List;

public record TransacaoPageResponse(
        List<DetailTransacaoResponse> content,
        Long totalElements,
        Integer totalPages,
        Integer currentPage,
        Integer pageSize
) {
}
