package br.com.saudeConecta.service;

import br.com.saudeConecta.domain.endereco.Endereco;
import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.organizacao.StatusOrganizacao;
import br.com.saudeConecta.infra.tenant.TenantHelper;
import br.com.saudeConecta.infrastructure.persistence.repository.EnderecoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.OrganizacaoRepository;
import br.com.saudeConecta.presentation.dto.organizacao.AtualizarOrganizacaoRequest;
import br.com.saudeConecta.presentation.dto.organizacao.CriarOrganizacaoRequest;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizacaoService {

    private final OrganizacaoRepository organizacaoRepository;
    private final EnderecoRepository enderecoRepository;
    private final TenantHelper tenantHelper;

    public Optional<Organizacao> buscarAtual() {
        if (!tenantHelper.hasTenant()) {
            return Optional.empty();
        }
        Long orgId = tenantHelper.getCurrentTenantId();
        log.debug("Buscando organização atual: {}", orgId);
        return organizacaoRepository.findById(orgId);
    }

    public List<Organizacao> listarTodas() {
        log.debug("Listando todas as organizações");
        return organizacaoRepository.findAll();
    }

    public List<Organizacao> listarAtivas() {
        log.debug("Listando organizações ativas");
        return organizacaoRepository.findByStatus(StatusOrganizacao.ATIVO);
    }

    public Optional<Organizacao> buscarPorId(Long id) {
        log.debug("Buscando organização por ID: {}", id);
        return organizacaoRepository.findById(id);
    }

    public Optional<Organizacao> buscarPorCnpj(String cnpj) {
        log.debug("Buscando organização por CNPJ: {}", cnpj);
        return organizacaoRepository.findByCnpj(cnpj);
    }

    @Transactional
    public Organizacao criar(CriarOrganizacaoRequest request) {
        log.info("Criando nova organização: {}", request.nome());
        
        if (request.cnpj() != null && organizacaoRepository.existsByCnpj(request.cnpj())) {
            throw new IllegalArgumentException("CNPJ já cadastrado");
        }

        Endereco endereco = null;
        if (request.enderecoId() != null) {
            endereco = enderecoRepository.findById(request.enderecoId())
                .orElseThrow(() -> new EntityNotFoundException("Endereço não encontrado"));
        }

        Organizacao org = Organizacao.builder()
            .nome(request.nome())
            .razaoSocial(request.razaoSocial())
            .cnpj(request.cnpj())
            .tipo(request.tipo())
            .email(request.email())
            .telefone(request.telefone())
            .logoUrl(request.logoUrl())
            .endereco(endereco)
            .status(StatusOrganizacao.ATIVO)
            .build();

        Organizacao salva = organizacaoRepository.save(org);
        log.info("Organização criada com sucesso. ID: {}", salva.getId());
        return salva;
    }

    @Transactional
    public Organizacao atualizar(Long id, AtualizarOrganizacaoRequest request) {
        log.info("Atualizando organização ID: {}", id);
        
        Organizacao org = organizacaoRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Organização não encontrada"));

        if (request.nome() != null) org.setNome(request.nome());
        if (request.razaoSocial() != null) org.setRazaoSocial(request.razaoSocial());
        if (request.cnpj() != null) {
            if (!request.cnpj().equals(org.getCnpj()) && organizacaoRepository.existsByCnpj(request.cnpj())) {
                throw new IllegalArgumentException("CNPJ já cadastrado");
            }
            org.setCnpj(request.cnpj());
        }
        if (request.email() != null) org.setEmail(request.email());
        if (request.telefone() != null) org.setTelefone(request.telefone());
        if (request.logoUrl() != null) org.setLogoUrl(request.logoUrl());
        
        if (request.enderecoId() != null) {
            Endereco endereco = enderecoRepository.findById(request.enderecoId())
                .orElseThrow(() -> new EntityNotFoundException("Endereço não encontrado"));
            org.setEndereco(endereco);
        }

        Organizacao atualizada = organizacaoRepository.save(org);
        log.info("Organização ID: {} atualizada com sucesso", id);
        return atualizada;
    }

    @Transactional
    public void ativar(Long id) {
        log.info("Ativando organização ID: {}", id);
        Organizacao org = organizacaoRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Organização não encontrada"));
        org.setStatus(StatusOrganizacao.ATIVO);
        organizacaoRepository.save(org);
    }

    @Transactional
    public void inativar(Long id) {
        log.info("Inativando organização ID: {}", id);
        Organizacao org = organizacaoRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Organização não encontrada"));
        org.setStatus(StatusOrganizacao.INATIVO);
        organizacaoRepository.save(org);
    }

    public Long contarAtivas() {
        return organizacaoRepository.countByStatus(StatusOrganizacao.ATIVO);
    }
}
