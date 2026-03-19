package com.finup.auth;

import com.finup.auth.dtos.CreateAccountRequest;
import com.finup.auth.dtos.DetailAccountResponse;
import com.finup.credencial.Credencial;
import com.finup.credencial.CredencialRepository;
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
            throw new RuntimeException("Email já cadastrado");
        }

        if (credencialRepository.existsByUsername(dados.username())) {
            throw new RuntimeException("Username já cadastrado");
        }

        if (pessoaFisicaRepository.existsByCpf(dados.cpf())) {
            throw new RuntimeException("CPF já cadastrado");
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

    public PessoaFisica getUsuarioAutenticado() {
        String username = (String) Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getPrincipal();

        return credencialRepository.findByUsername(username)
                .map(Credencial::getPessoaFisica)
                .flatMap(pf -> pessoaFisicaRepository.findById(pf.getId()))
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado ou sem vínculo de Pessoa Física"));
    }
}
