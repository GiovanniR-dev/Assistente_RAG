package com.giovanni.assistenterag.controller;

import com.giovanni.assistenterag.model.Usuario;
import com.giovanni.assistenterag.service.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor

public class AuthController {
    private final AuthService authService;
    @PostMapping("/registrar")
    public ResponseEntity<UsuarioDto> registrar(@Valid @RequestBody RegistroDto corpo){
        Usuario usuario = authService.registrar(corpo.nome(), corpo.email(), corpo.senha());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new UsuarioDto(usuario.getId(), usuario.getNome(), usuario.getEmail()));

    }

    public record RegistroDto(
            @NotBlank String nome,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, message = "A senha deve ter ao menos 8 caracteres") String senha
    ){}


    public record UsuarioDto(Long id, String nome, String email){}

}
