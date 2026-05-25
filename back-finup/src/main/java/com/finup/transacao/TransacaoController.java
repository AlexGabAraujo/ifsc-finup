package com.finup.transacao;

import com.finup.transacao.dtos.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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



    @GetMapping("/insights")
    public ResponseEntity<InsightsResponse> getInsights(@RequestParam(required = false) Integer mes,
                                                        @RequestParam(required = false) Integer ano) {
        return ResponseEntity.ok(transacaoService.getInsights(mes, ano));
    }

    @GetMapping
    public ResponseEntity<TransacaoPageResponse> listarTransacoes(@RequestParam(required = false) Integer mes,
                                                                  @RequestParam(required = false) Integer ano,
                                                                  @RequestParam(required = false) Long categoriaId,
                                                                  @RequestParam(required = false) String categoriaType,
                                                                  @RequestParam(defaultValue = "0") int page) {
        return ResponseEntity.ok(transacaoService.listarTransacoes(mes, ano, categoriaId, categoriaType, page));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DetailTransacaoResponse> atualizarTransacao(@PathVariable Long id,
                                                                       @RequestBody @Valid UpdateTransacaoRequest dados) {
        return ResponseEntity.ok(transacaoService.atualizarTransacao(id, dados));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarTransacao(@PathVariable Long id) {
        transacaoService.deletarTransacao(id);
        return ResponseEntity.noContent().build();
    }

}
