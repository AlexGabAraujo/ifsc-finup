export interface CreateAccountRequest {
  email: string;
  senha: string;
  username: string;
  nome: string;
  cpf: string;
  telefone: string;
  dataNascimento: string | null;
}