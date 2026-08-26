package com.giovanni.assistenterag.service;

import com.giovanni.assistenterag.model.Usuario;
import com.giovanni.assistenterag.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor

public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public Usuario registrar(String nome, String email, String senha){
        if (usuarioRepository.findByEmail(email).isPresent()){
            throw new IllegalStateException("Ja existe um usuario com esse email.");
        }
        Usuario usuario=new Usuario();
        usuario.setNome(nome);
        usuario.setEmail(email);
        usuario.setSenhaHash(passwordEncoder.encode(senha));

        return usuarioRepository.save(usuario);
    }

    public String login(String email, String senha) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Email ou senha invalidos."));

        if (!passwordEncoder.matches(senha, usuario.getSenhaHash())) {
            throw new IllegalArgumentException("Email ou senha invalidos.");
        }

        return jwtService.gerarToken(usuario.getId(), usuario.getEmail());
    }
}
