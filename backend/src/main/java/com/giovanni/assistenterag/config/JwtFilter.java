package com.giovanni.assistenterag.config;

import com.giovanni.assistenterag.model.Usuario;
import com.giovanni.assistenterag.repository.UsuarioRepository;
import com.giovanni.assistenterag.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor

public class JwtFilter extends OncePerRequestFilter {

    private static final String PREFIXO="Bearer ";

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException{

        String cabecalho = request.getHeader("Authorization");

        if (cabecalho != null && cabecalho.startsWith(PREFIXO)) {
            String token=cabecalho.substring(PREFIXO.length());

            if (jwtService.tokenValido(token)) {
                Long usuarioId = jwtService.extrairUsuarioId(token);
                Optional<Usuario> usuario = usuarioRepository.findById(usuarioId);

                if(usuario.isPresent() && SecurityContextHolder.getContext().getAuthentication()==null){
                    var auth=new UsernamePasswordAuthenticationToken(
                            usuario.get(), null,List.of());
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}
