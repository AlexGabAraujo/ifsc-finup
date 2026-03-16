package com.finup.infra.security;

import com.finup.credencial.Credencial;
import com.finup.credencial.CredencialRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Optional;

@Component
public class MyUserDetailService implements UserDetailsService {

    @Autowired
    private CredencialRepository credencialRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Credencial user = credencialRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Não foi encontrado o usuario com esse login: " + username
                ));

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getSenha(),
                Collections.emptyList()
        );
    }
}

