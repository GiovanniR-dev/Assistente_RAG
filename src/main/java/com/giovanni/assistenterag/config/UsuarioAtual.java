package com.giovanni.assistenterag.config;

import com.giovanni.assistenterag.model.Usuario;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component

public class UsuarioAtual {
    public Usuario get(){
        Authentication auth =SecurityContextHolder.getContext().getAuthentication();

        if(auth== null || !(auth.getPrincipal() instanceof Usuario usuario)){
            throw new IllegalStateException("Nenhum usuario autenticado");
        }
        return usuario;
    }
}
