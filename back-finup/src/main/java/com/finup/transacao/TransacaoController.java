package com.finup.transacao;

import com.finup.transacao.dtos.CreateTransacaoRequest;
import com.finup.transacao.dtos.DetailTransacaoResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/transacao")
@SecurityRequirement(name = "bearer-key")
public class TransacaoController {
    @Autowired
    private TransacaoService transacaoService;

    @PostMapping
    public ResponseEntity<DetailTransacaoResponse> cadastrarTransacao(@RequestBody @Valid CreateTransacaoRequest dadosTransacao,
                                                                      UriComponentsBuilder uriBuilder)
    {
        DetailTransacaoResponse response = transacaoService.createTransacao(dadosTransacao);

        var uri = uriBuilder.path("/transacao/{id}").buildAndExpand(response.id()).toUri();

        return ResponseEntity.created(uri).body(response);
    }
}
