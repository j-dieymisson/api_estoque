package com.api.estoque.dto.response;

import java.util.Set;

// A sua IDE provavelmente irá adicionar o import abaixo automaticamente
import com.api.estoque.dto.response.PermissaoResponse;

public record CargoResponse(
        Long id,
        String nome,
        Set<PermissaoResponse> permissoes
) {}