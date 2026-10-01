# Documento de Requisitos

## Introdução

Este documento descreve os requisitos para correção de três bugs identificados nas telas de **Relatório** e **Transações** do frontend Angular do projeto FinUp.

- **Bug 1 e 2**: Os gráficos Highcharts da tela de Relatório não renderizam na carga inicial e não atualizam ao aplicar o filtro de tipo (Todos/Receitas/Despesas), porque o input obrigatório `[Highcharts]` está ausente nos elementos `<highcharts-chart>` e porque `filtro.tipo` não é passado para as chamadas ao backend.
- **Bug 3**: Subtítulos descritivos desnecessários aparecem nos cabeçalhos das telas de Relatório e Transações, poluindo a interface.

## Glossário

- **Relatorio**: Componente Angular localizado em `relatorio/relatorio.ts` e `relatorio/relatorio.html`, responsável pela exibição de gráficos e transações do período.
- **Transacoes**: Componente Angular localizado em `transacoes/transacoes.html`, responsável pela listagem e gerenciamento de transações.
- **highcharts-chart**: Elemento do pacote `highcharts-angular` que exige o input `[Highcharts]` para saber qual instância de biblioteca usar.
- **filtro.tipo**: Propriedade do objeto `FiltroRelatorio` com valores `TODOS`, `CREDITO` ou `DEBITO`, usada para filtrar transações por tipo.
- **RelatorioService**: Serviço Angular em `core/services/relatorio.service.ts` responsável por todas as chamadas HTTP relacionadas ao relatório.
- **carregarTransacoesComPeriodo**: Método do `RelatorioService` que consulta o endpoint `/api/relatorio/transacoes`.
- **Subtítulo**: Elemento `<p>` de classe `text-letra2 text-sm mt-1` presente nos cabeçalhos das telas de Relatório e Transações.

---

## Requisitos

### Requisito 1 — Binding obrigatório do Highcharts no template de Relatório

**User Story:** Como usuário, quero que os gráficos da tela de Relatório sejam exibidos corretamente ao carregar a página, para que eu possa visualizar minhas finanças sem precisar interagir com a tela primeiro.

#### Critérios de Aceitação

1. WHEN o componente `Relatorio` é renderizado, THE `Relatorio` SHALL expor a instância `Highcharts` como propriedade pública `readonly Highcharts = Highcharts`.
2. WHEN o template `relatorio.html` é compilado, THE `Relatorio` SHALL conter o binding `[Highcharts]="Highcharts"` em todos os elementos `<highcharts-chart>` presentes no arquivo.
3. WHEN os dados dos gráficos são carregados pelo backend, THE `Relatorio` SHALL renderizar cada gráfico imediatamente, sem exigir interação adicional do usuário.
4. IF o binding `[Highcharts]` estiver ausente em qualquer `<highcharts-chart>`, THEN THE `Relatorio` SHALL emitir erro de compilação ou de runtime indicando o input obrigatório faltante.

---

### Requisito 2 — Filtro de tipo passado nas chamadas ao backend

**User Story:** Como usuário, quero que ao selecionar o filtro "Receitas" ou "Despesas" na tela de Relatório, apenas as transações do tipo selecionado sejam retornadas, para que a lista reflita exatamente o que escolhi.

#### Critérios de Aceitação

1. WHEN o usuário altera `filtro.tipo` para `CREDITO`, `DEBITO` ou `TODOS`, THE `Relatorio` SHALL invocar `carregarTransacoesComPeriodo` passando o valor atual de `filtro.tipo` como parâmetro.
2. WHEN `carregarTransacoesComPeriodo` é chamado com um `tipo` informado, THE `RelatorioService` SHALL incluir o parâmetro `tipo` na query string da requisição HTTP ao endpoint `/api/relatorio/transacoes`.
3. WHEN `filtro.tipo` é `TODOS`, THE `RelatorioService` SHALL omitir o parâmetro `tipo` da query string ou enviá-lo com o valor `TODOS`, conforme a convenção do backend.
4. IF o método `carregarTransacoesComPeriodo` não aceitar o parâmetro `tipo`, THEN THE `RelatorioService` SHALL ter sua assinatura atualizada para aceitar `tipo: TipoRelatorio` como terceiro parâmetro opcional.

---

### Requisito 3 — Remoção dos subtítulos de cabeçalho

**User Story:** Como usuário, quero que os cabeçalhos das telas de Relatório e Transações apresentem apenas o título principal, para que a interface seja mais limpa e objetiva.

#### Critérios de Aceitação

1. WHEN o template `relatorio.html` é renderizado, THE `Relatorio` SHALL exibir somente o elemento `<h1>` com o texto "Relatórios" no cabeçalho, sem o `<p>` de subtítulo de classe `text-letra2 text-sm mt-1`.
2. WHEN o template `transacoes.html` é renderizado, THE `Transacoes` SHALL exibir somente o elemento `<h1>` com o texto "Transações" no cabeçalho, sem o `<p>` de subtítulo de classe `text-letra2 text-sm mt-1`.
3. THE `Relatorio` SHALL manter todos os demais elementos do cabeçalho (filtros, botão "Baixar Excel") inalterados após a remoção do subtítulo.
4. THE `Transacoes` SHALL manter todos os demais elementos do cabeçalho (filtros de mês/ano, botão "Nova Transação") inalterados após a remoção do subtítulo.
