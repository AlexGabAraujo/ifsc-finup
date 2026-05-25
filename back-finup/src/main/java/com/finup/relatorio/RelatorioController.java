package com.finup.relatorio;

import com.finup.auth.AuthService;
import com.finup.relatorio.dto.RelatorioGraficoBarrasResponse;
import com.finup.relatorio.dto.RelatorioGraficoPizzaResponse;
import com.finup.relatorio.dto.RelatorioResumoResponse;
import com.finup.relatorio.dto.RelatorioTopCategoriasMesResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/relatorio")
@CrossOrigin(origins = "http://localhost:4200")
public class RelatorioController {

    @Autowired
    private RelatorioService relatorioService;

    @Autowired
    private AuthService authService;

    @GetMapping("/resumo")
    public ResponseEntity<RelatorioResumoResponse> getResumo(
            @RequestParam(defaultValue = "ULTIMOS_6_MESES") String periodo) {
        var usuarioLogado = authService.getUsuarioAutenticado();
        return ResponseEntity.ok(relatorioService.buscarResumo(usuarioLogado.getId(), periodo));
    }

    @GetMapping("/graficoBarras")
    public ResponseEntity<RelatorioGraficoBarrasResponse> getGraficoBarras(
            @RequestParam(defaultValue = "ULTIMOS_6_MESES") String periodo) {
        var usuarioLogado = authService.getUsuarioAutenticado();
        return ResponseEntity.ok(relatorioService.buscarGraficoBarras(usuarioLogado.getId(), periodo));
    }

    @GetMapping("/graficoPizza")
    public ResponseEntity<List<RelatorioGraficoPizzaResponse>> getGraficoPizza(
            @RequestParam(defaultValue = "ULTIMOS_6_MESES") String periodo) {
        var usuarioLogado = authService.getUsuarioAutenticado();
        return ResponseEntity.ok(relatorioService.buscarGraficoPizza(usuarioLogado.getId(), periodo));
    }

    @GetMapping("/topCategoriasPorMes")
    public ResponseEntity<List<RelatorioTopCategoriasMesResponse>> getTopCategoriasPorMes(
            @RequestParam(defaultValue = "ULTIMOS_6_MESES") String periodo) {
        var usuarioLogado = authService.getUsuarioAutenticado();
        return ResponseEntity.ok(relatorioService.buscarTopCategoriasPorMes(usuarioLogado.getId(), periodo));
    }
}
