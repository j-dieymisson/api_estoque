package com.api.estoque.controller;

import com.api.estoque.model.Setor;
import com.api.estoque.service.SetorService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/setores")
public class SetorController {

    private final SetorService setorService;

    public SetorController(SetorService setorService) {
        this.setorService = setorService;
    }

    @GetMapping
    public ResponseEntity<List<Setor>> listar(
            @RequestParam(defaultValue = "false") boolean apenasAtivos) {
        List<Setor> setores = setorService.listarTodos(apenasAtivos);
        return ResponseEntity.ok(setores);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Setor> buscarPorId(@PathVariable Long id) {
        Setor setor = setorService.buscarPorId(id);
        return ResponseEntity.ok(setor);
    }

    @PostMapping
    public ResponseEntity<Setor> criar(@RequestBody @Valid Setor setor, UriComponentsBuilder uriBuilder) {
        Setor setorSalvo = setorService.criarSetor(setor);
        URI uri = uriBuilder.path("/setores/{id}").buildAndExpand(setorSalvo.getId()).toUri();
        return ResponseEntity.created(uri).body(setorSalvo);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Setor> atualizar(@PathVariable Long id, @RequestBody @Valid Setor setor) {
        Setor setorAtualizado = setorService.atualizarSetor(id, setor);
        return ResponseEntity.ok(setorAtualizado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desativar(@PathVariable Long id) {
        setorService.desativarSetor(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/ativar")
    public ResponseEntity<Void> ativar(@PathVariable Long id) {
        setorService.ativarSetor(id);
        return ResponseEntity.ok().build();
    }
}