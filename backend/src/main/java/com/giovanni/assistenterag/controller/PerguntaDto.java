package com.giovanni.assistenterag.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PerguntaDto(
        @NotBlank(message = "A pergunta nao pode estar vazia")
        @Size(max = 500, message = "A pergunta deve ter no maximo 500 caracteres")
        String pergunta
) {}