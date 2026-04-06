package com.finup.auth;

import br.com.caelum.stella.tinytype.CPF;
import br.com.caelum.stella.validation.CPFValidator;
import br.com.caelum.stella.validation.InvalidStateException;
import com.finup.auth.dtos.CreateAccountRequest;
import com.finup.auth.dtos.DetailAccountResponse;
import com.finup.auth.dtos.UpdateAccountRequest;
import com.finup.credencial.Credencial;
import com.finup.credencial.CredencialRepository;
import com.finup.infra.exceptions.ValidacaoException;
import com.finup.pessoaFisica.PessoaFisica;
import com.finup.pessoaFisica.PessoaFisicaRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

@Service
public class AuthService {

    @Autowired
    private PessoaFisicaRepository pessoaFisicaRepository;

    @Autowired
    private CredencialRepository credencialRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional
    public DetailAccountResponse criarConta(CreateAccountRequest dados) {

        if (credencialRepository.existsByEmail(dados.email())) {
            throw new ValidacaoException("Email já cadastrado");
        }

        if (credencialRepository.existsByUsername(dados.username())) {
            throw new ValidacaoException("Username já cadastrado");
        }

        if (pessoaFisicaRepository.existsByCpf(dados.cpf())) {
            throw new ValidacaoException("CPF já cadastrado");
        }

        CPFValidator cpfValidator = new CPFValidator();

        try {
            cpfValidator.assertValid(dados.cpf());
        } catch (InvalidStateException e) {
            throw new ValidacaoException("CPF inválido");
        }

        PessoaFisica pessoaFisica = PessoaFisica.builder()
                .nome(dados.nome())
                .cpf(limparNumeros(dados.cpf()))
                .telefone(limparNumeros(dados.telefone()))
                .dataNascimento(dados.dataNascimento())
                .data_inicio(LocalDateTime.now())
                .data_fim(null)
                .ativo(true)
                .build();

        pessoaFisica = pessoaFisicaRepository.save(pessoaFisica);

        Credencial credencial = Credencial.builder()
                .email(dados.email().trim().toLowerCase())
                .senha(passwordEncoder.encode(dados.senha()))
                .username(dados.username().trim())
                .pessoaFisica(pessoaFisica)
                .build();

        credencial = credencialRepository.save(credencial);

        return new DetailAccountResponse(
                credencial.getId(),
                credencial.getEmail(),
                credencial.getUsername(),
                pessoaFisica.getId(),
                pessoaFisica.getNome(),
                pessoaFisica.getCpf(),
                pessoaFisica.getTelefone(),
                pessoaFisica.getDataNascimento(),
                pessoaFisica.getData_inicio(),
                pessoaFisica.getData_fim(),
                pessoaFisica.getAtivo()
        );
    }

    private String limparNumeros(String valor) {
        return valor == null ? null : valor.replaceAll("\\D", "");
    }

    //pega as informaçoes da pessoaFisicalogada
    public PessoaFisica getUsuarioAutenticado() {
        String username = (String) Objects.requireNonNull(
                SecurityContextHolder.getContext().getAuthentication()).getPrincipal();

        return credencialRepository.findPessoaFisicaByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado ou sem vínculo de Pessoa Física"));
    }

    public DetailAccountResponse getContaAutenticada() {
        PessoaFisica pessoa = getUsuarioAutenticado();
        Credencial credencial = credencialRepository.findByPessoaFisicaId(pessoa.getId());

        return new DetailAccountResponse(
                credencial.getId(),
                credencial.getEmail(),
                credencial.getUsername(),
                pessoa.getId(),
                pessoa.getNome(),
                pessoa.getCpf(),
                pessoa.getTelefone(),
                pessoa.getDataNascimento(),
                pessoa.getData_inicio(),
                pessoa.getData_fim(),
                pessoa.getAtivo()
        );
    }

    //Regras de negocio para atualizar usuário:
    public void atualizarUsuario(UpdateAccountRequest dados){
        var usuarioAutenticado = getUsuarioAutenticado();

        var pessoa = pessoaFisicaRepository.getReferenceById(usuarioAutenticado.getId());
        var credencial = credencialRepository.findByPessoaFisicaId(usuarioAutenticado.getId());

        validarDadosUpdate(dados);

        pessoa.atualizarInformacoes(dados.toPessoaRequest());
        credencial.atualizarInformacoes(dados.toCredencialRequest());
    }

    public void validarDadosUpdate(UpdateAccountRequest dados){
        String usernameAutenticado = (String) Objects.requireNonNull(
                SecurityContextHolder.getContext().getAuthentication()).getPrincipal();

        Credencial credencialAtual = credencialRepository.findByUsername(usernameAutenticado)
                .orElseThrow(() -> new EntityNotFoundException("Credencial não encontrada"));

        if (!credencialAtual.getUsername().equals(dados.username()) && credencialRepository.existsByUsername(dados.username()))
            throw new ValidacaoException("Username já cadastrado");

        if (!credencialAtual.getEmail().equals(dados.email()) && credencialRepository.existsByEmail(dados.email()))
            throw new ValidacaoException("E-mail já cadastrado");
    }
}
