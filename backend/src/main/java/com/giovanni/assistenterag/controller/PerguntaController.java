package com.giovanni.assistenterag.controller;

import com.giovanni.assistenterag.service.RespostaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/perguntar")
@RequiredArgsConstructor
public class PerguntaController {

    private final RespostaService respostaService;

    @PostMapping
    public RespostaDto perguntar(@RequestBody PerguntaDto corpo) {
        return new RespostaDto(respostaService.responder(corpo.pergunta()));
    }

    public record PerguntaDto(String pergunta) {}
    public record RespostaDto(String resposta) {}
}