export interface DetailAccountResponse {
  id_credencial: number;
  email: string;
  username: string;
  id_pessoa: number;
  nome: string;
  cpf: string;
  telefone: string;
  dataNascimento: string;
  data_inicio: string;
  data_fim: string | null;
  ativo: boolean;
}

export interface UpdateAccountRequest {
  email: string;
  senha: string | null;
  username: string;
  nome: string;
  telefone: string;
  dataNascimento: string;
}
