export type TipoGasto = 'CREDITO' | 'DEBITO';
export type TipoPagamento = 'CARTAO_DE_CREDITO' | 'CARTAO_DE_DEBITO' | 'PIX' | 'BOLETO' | 'DINHEIRO' | 'CHEQUE';
export type CategoriaType = 'CLASSE_PRINCIPAL' | 'SUBCLASSE';

export interface DetailTransacaoResponse {
  id: number;
  valor: number;
  pessoaFisicaId: number;
  tipoPagamento: TipoPagamento;
  tipoGasto: TipoGasto;
  subClasseId: number | null;
  classePrincipalId: number | null;
  cnpjId: number | null;
  dataTransacao: string;
}

export interface InsightsResponse {
  totalTransacoes: number;
  totalReceitas: number;
  totalDespesas: number;
  transacoesPorTipo: Record<TipoGasto, number>;
}

export interface TransacaoPageResponse {
  content: DetailTransacaoResponse[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
  pageSize: number;
}

export interface CreateTransacaoRequest {
  valor: number;
  tipoPagamento: TipoPagamento;
  tipoGasto: TipoGasto;
  dataTransacao: string;
  subClasseId?: number | null;
  classePrincipalId?: number | null;
  cnpjId?: number | null;
}

export interface UpdateTransacaoRequest extends CreateTransacaoRequest {}

export interface FiltroTransacao {
  mes: number | null;
  ano: number | null;
  categoriaId: number | null;
  categoriaType: CategoriaType | null;
}

export interface CategoriaOption {
  id: number;
  nome: string;
  type: CategoriaType;
}
