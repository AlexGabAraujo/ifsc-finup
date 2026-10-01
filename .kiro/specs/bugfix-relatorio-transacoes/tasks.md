# Plano de Implementação: Bugfix — Relatório e Transações

## Visão Geral

Correção de três bugs no frontend Angular do FinUp:
1. Adicionar o binding `[Highcharts]` ausente nos `<highcharts-chart>` do template de Relatório e expor a instância como propriedade pública no componente.
2. Passar `filtro.tipo` nas chamadas ao backend ao carregar transações no Relatório.
3. Remover os subtítulos de cabeçalho das telas de Relatório e Transações.

---

## Tarefas

- [ ] 1. Expor instância Highcharts como propriedade pública em `relatorio.ts`
  - Adicionar a propriedade `readonly Highcharts = Highcharts;` na classe `Relatorio`, logo abaixo da declaração `private relatorioService = inject(RelatorioService);`
  - A propriedade deve ser pública (sem modificador `private` ou `protected`) para ser acessível no template
  - _Requisitos: 1.1_

- [ ] 2. Adicionar `[Highcharts]="Highcharts"` em todos os `<highcharts-chart>` de `relatorio.html`
  - Localizar todos os elementos `<highcharts-chart>` no arquivo `relatorio.html` (há 5 ocorrências: barras, área, pizza, ranking e top categorias)
  - Adicionar o binding `[Highcharts]="Highcharts"` em cada um deles, logo após a abertura da tag
  - Verificar que nenhuma ocorrência ficou sem o binding
  - _Requisitos: 1.2, 1.3, 1.4_

- [ ] 3. Atualizar `carregarTransacoesComPeriodo` no `RelatorioService` para aceitar o filtro de tipo
  - Alterar a assinatura do método em `relatorio.service.ts` de `carregarTransacoesComPeriodo(periodo: PeriodoRelatorio, page: number)` para `carregarTransacoesComPeriodo(periodo: PeriodoRelatorio, page: number, tipo?: TipoRelatorio)`
  - Dentro do método, adicionar `if (tipo && tipo !== 'TODOS') params = params.set('tipo', tipo);` antes do `return`
  - _Requisitos: 2.2, 2.3, 2.4_

- [ ] 4. Passar `filtro.tipo` nas chamadas a `carregarTransacoesComPeriodo` em `relatorio.ts`
  - Localizar todas as chamadas a `this.relatorioService.carregarTransacoesComPeriodo(...)` no componente `Relatorio`
  - Acrescentar `this.filtro.tipo` como terceiro argumento em cada chamada
  - _Requisitos: 2.1_

- [ ] 5. Remover o subtítulo do cabeçalho de `relatorio.html`
  - Apagar a linha `<p class="text-letra2 text-sm mt-1">Análise histórica das suas finanças</p>` do template
  - Verificar que o `<h1>` e os demais elementos do cabeçalho (filtros, botão "Baixar Excel") permanecem intactos
  - _Requisitos: 3.1, 3.3_

- [ ] 6. Remover o subtítulo do cabeçalho de `transacoes.html`
  - Apagar a linha `<p class="text-letra2 text-sm mt-1">Gerencie todas as suas receitas e despesas</p>` do template
  - Verificar que o `<h1>` e os demais elementos do cabeçalho (filtros de mês/ano, botão "Nova Transação") permanecem intactos
  - _Requisitos: 3.2, 3.4_

- [ ] 7. Checkpoint final — Verificar integridade das alterações
  - Compilar o projeto Angular (`ng build` ou verificar erros no editor)
  - Confirmar que não há erros de compilação TypeScript nos arquivos modificados
  - Confirmar que os 5 elementos `<highcharts-chart>` possuem o binding `[Highcharts]`
  - Confirmar que nenhum dos dois subtítulos aparece mais nos templates
  - Confirmar que a assinatura de `carregarTransacoesComPeriodo` está atualizada e que todas as chamadas passam o tipo
  - Garantir que todos os testes passam. Perguntar ao usuário se houver dúvidas.

## Notas

- Todos os arquivos modificados estão em `front-finup/src/app/features/` e `front-finup/src/app/core/services/`
- Não há lógica nova a implementar — são apenas correções pontuais de template e assinatura de método
- A propriedade `Highcharts` deve ser declarada com `readonly` para evitar reatribuições acidentais
