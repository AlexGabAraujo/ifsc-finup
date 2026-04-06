package com.finup.dash;

import com.finup.auth.AuthService;
import com.finup.dash.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "http://localhost:4200")

public class DashController {
    @Autowired
    private DashService dashService;

    @Autowired
    private AuthService authService;

    //cards

    @PatchMapping("/metaEcoAtual")
    public ResponseEntity<Void> setMetaEcoAtuak(@RequestBody UpdateMetaEconomiaRequest dto){
        var usuarioLogado = authService.getUsuarioAutenticado();
        dashService.setAtualizarMetaEcoAtual(usuarioLogado.getId(), dto.valorEco() );
        return ResponseEntity.noContent().build();
    }
    @PatchMapping("/metaEconomia")
    public ResponseEntity<Void> setMetaEconomia(@RequestBody UpdateMetaEconomiaRequest dto){
        var usuarioLogado = authService.getUsuarioAutenticado();
        dashService.setAtualizarMeta(usuarioLogado.getId(), dto.novaMetaEco());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/informacoesGerais")
    public ResponseEntity<DetailInfoDashResponse> getInformacoesGerais() {
        var usuarioLogado = authService.getUsuarioAutenticado();
        DetailInfoDashResponse response = dashService.buscarResumoCompleto(usuarioLogado.getId());
        return ResponseEntity.ok(response);
    }

    //graficos

    @GetMapping("/graficoLinha")
    public ResponseEntity<DetailGraficoDashResponse> getGraficoLinha (){
        var usuarioLogado = authService.getUsuarioAutenticado();
        DetailGraficoDashResponse GraficoLinha = dashService.buscarGraficoLinha(usuarioLogado.getId());
        return  ResponseEntity.ok(GraficoLinha);
    }

    @GetMapping("/graficoPizza")
    public ResponseEntity<List<DetailGraficoPizzaResponse>> getGraficoPizza(){
        var usuarioLogado = authService.getUsuarioAutenticado();
        List<DetailGraficoPizzaResponse> graficoPizza = dashService.getGraficoPizza(usuarioLogado.getId());
        return ResponseEntity.ok(graficoPizza);
    }

    //Transacoes

    @GetMapping("/Transacoes")
    public ResponseEntity<List<DetailTransacoesResponse>> getUltimasTransacoes(){
        var usuarioLogado = authService.getUsuarioAutenticado();
        List<DetailTransacoesResponse> transacoesRecentes = dashService.buscarUltimasTransacoes(usuarioLogado.getId());
        return ResponseEntity.ok(transacoesRecentes);
    }


}
