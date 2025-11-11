package com.api.estoque.dto.response;

public record UsuarioResponse(
        Long id,
        String nome,
        String email,
        String nomeCargo, // Enviamos o nome do cargo para ser mais legível
        Long cargoId,
        boolean ativo,
        Long gestorImediatoId,
        String gestorImediatoNome
) {}