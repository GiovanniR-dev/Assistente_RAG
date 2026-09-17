package com.giovanni.assistenterag.controller;

import com.giovanni.assistenterag.config.UsuarioAtual;
import com.giovanni.assistenterag.service.BuscaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/busca")
@RequiredArgsConstructor

public class BuscaController {

    private final BuscaService buscaService;
    private final UsuarioAtual UsuarioAtual;

    @GetMapping
    public List<ResultadoBusca>buscar(

            @RequestParam String pergunta,
            @RequestParam(defaultValue="3")int quantidade){
        return buscaService
                .buscarTrechosRelevantes(pergunta, quantidade, UsuarioAtual.get().getId())
                .stream()
                .map(r -> new ResultadoBusca(
                        r.trecho().getId(),
                        r.nomeDocumento(),
                        r.trecho().getOrdem(),
                        r.trecho().getConteudo(),
                        Math.round(r.similaridade() * 1000) / 1000.0
                ))
                .toList();
    }
    public record ResultadoBusca(Long id, String documento, Integer ordem,
                                 String conteudo, double similaridade) {}}
