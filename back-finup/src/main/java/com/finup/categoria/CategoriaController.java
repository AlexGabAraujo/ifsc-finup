package com.finup.categoria;

import com.finup.categoria.dto.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/categoria")
@SecurityRequirement(name = "bearer-key")
public class CategoriaController {

    @Autowired
    private CategoriaService categoriaService;

    @PostMapping
    public ResponseEntity<DetailCategoriaResponse> criarCategoria(@RequestBody @Valid CreateCategoriaRequest dados, UriComponentsBuilder uriBuilder) {
        DetailCategoriaResponse response = categoriaService.criarCategoria(dados);

        var uri = uriBuilder.path("/categoria/{id}").buildAndExpand(response.id()).toUri();

        return ResponseEntity.created(uri).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DetailCategoriaResponse> atualizarCategoria(
            @PathVariable Long id,
            @RequestBody @Valid UpdateCategoriaRequest dados
    ) {

        DetailCategoriaResponse response =
                categoriaService.atualizarCategoria(id, dados);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarCategoria(@PathVariable Long id) {
        categoriaService.deletarCategoria(id);
        return ResponseEntity.noContent().build();
    }


    @GetMapping("/{id}/resumo")
    public ResponseEntity<DetailCategoriaCardResponse> getResumoCategoria(
            @PathVariable Long id,
            @RequestParam(defaultValue = "MES_ATUAL") PeriodoFiltro periodo
    ) {
        return ResponseEntity.ok(categoriaService.getResumoCategoria(id, periodo));
    }

    @GetMapping("/orcamento-total")
    public ResponseEntity<DetailCategoriaOrcamentoResponse> getOrcamentoTotal(
            @RequestParam(defaultValue = "MES_ATUAL") PeriodoFiltro periodo
    ) {
        return ResponseEntity.ok(categoriaService.getOrcamentoTotal(periodo));
    }

    @GetMapping("/grafico-linha")
    public ResponseEntity<DetailGraficoCategoriaResponse> getDadosGraficoLinha(
            @RequestParam(defaultValue = "ULTIMOS_6_MESES") PeriodoFiltro periodo
    ) {
        return ResponseEntity.ok(categoriaService.getDadosGraficoLinha(periodo));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DetailCategoriaResponse> buscarPorId(@PathVariable Long id) {

        var categoria = categoriaService.buscarPorId(id);

        return ResponseEntity.ok(categoria);
    }

}