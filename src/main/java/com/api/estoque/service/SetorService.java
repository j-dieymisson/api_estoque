package com.api.estoque.service;

import com.api.estoque.exception.ResourceNotFoundException;
import com.api.estoque.model.Setor;
import com.api.estoque.model.Usuario;
import com.api.estoque.repository.SetorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
public class SetorService {

    private final SetorRepository setorRepository;

    public SetorService(SetorRepository setorRepository) {
        this.setorRepository = setorRepository;
    }

    @Transactional(readOnly = true)
    public List<Setor> listarTodos(boolean apenasAtivos, Usuario usuarioLogado) {

        String cargo = usuarioLogado.getCargo().getNome();

        // 1. ADMIN vê todos os setores
        if ("ADMIN".equals(cargo)) {
            if (apenasAtivos) {
                return setorRepository.findAllByAtivoTrueOrderByNomeAsc();
            }
            return setorRepository.findAllByOrderByNomeAsc();
        }
        // 2. GESTOR vê apenas o seu próprio setor
        else if ("GESTOR".equals(cargo)) {
            Setor setorDoGestor = usuarioLogado.getSetor();
            if (setorDoGestor != null && (!apenasAtivos || setorDoGestor.isAtivo())) {
                // Retorna uma lista contendo APENAS o setor do gestor
                return List.of(setorDoGestor);
            }
            // Gestor sem setor (ou se o setor estiver inativo) não pode atribuir nada
            return Collections.emptyList();
        }
        // 3. COLABORADOR não deve chamar isto
        else {
            return Collections.emptyList();
        }
    }

    @Transactional(readOnly = true)
    public Setor buscarPorId(Long id) {
        return setorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Setor não encontrado com o ID: " + id));
    }

    @Transactional
    public Setor criarSetor(Setor setor) {
        // (Poderíamos adicionar validação de nome duplicado aqui se quiséssemos)
        setor.setAtivo(true); // Garante que novos setores começam ativos
        return setorRepository.save(setor);
    }

    @Transactional
    public Setor atualizarSetor(Long id, Setor setorDetails) {
        Setor setor = buscarPorId(id);
        setor.setNome(setorDetails.getNome());
        // Não permitimos alterar o status 'ativo' por este método
        return setorRepository.save(setor);
    }

    @Transactional
    public void desativarSetor(Long id) {
        Setor setor = buscarPorId(id);
        // (Poderíamos adicionar lógica aqui para verificar se o setor tem utilizadores antes de desativar)
        setor.setAtivo(false);
        setorRepository.save(setor);
    }

    @Transactional
    public void ativarSetor(Long id) {
        Setor setor = buscarPorId(id);
        setor.setAtivo(true);
        setorRepository.save(setor);
    }
}