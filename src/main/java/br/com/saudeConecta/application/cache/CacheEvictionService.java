package br.com.saudeConecta.application.cache;

import br.com.saudeConecta.application.assinatura.AcessoAssinaturaService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Centraliza a invalidação seletiva (por chave) dos caches relacionados a
 * usuários e organizações.
 *
 * Substitui o uso de {@code allEntries = true} nos pontos de mutação por
 * evicções pontuais, evitando que a atualização de um único registro
 * (ex.: nome de um Admin/Secretaria/Profissional) descarte o cache de
 * todos os demais usuários. Isso resolve o problema de dados desatualizados
 * (nome antigo) exibidos após um update, sem sacrificar o ganho de
 * performance do cache para os registros não afetados.
 */
@Service
@Slf4j
public class CacheEvictionService {

    private static final String CACHE_PERFIL_USUARIO = "perfil-usuario";
    private static final String CACHE_USUARIOS_AGRUPADOS = "usuarios-agrupados";
    private static final String CHAVE_SUPER_ADMIN = "super-admin";
    private static final String CACHE_PRONTUARIO_RECENTE = "prontuario-recente";
    private static final String CACHE_QUESTIONARIO_SAUDE = "questionario-saude";
    private static final String CACHE_ACESSO_ASSINATURA = AcessoAssinaturaService.CACHE_ACESSO_ASSINATURA;

    private final CacheManager cacheManager;

    public CacheEvictionService(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * Invalida o perfil completo em cache de um usuário específico.
     * Deve ser chamado sempre que dados de Profissional, AdminOrganizacao,
     * Secretaria ou Usuario forem alterados, criados ou removidos.
     *
     * @param usuarioId ID do usuário (tabela usuarios) cujo perfil deve ser invalidado
     */
    public void evictPerfilUsuario(Long usuarioId) {
        if (usuarioId == null) {
            return;
        }
        evict(CACHE_PERFIL_USUARIO, usuarioId);
    }

    /**
     * Invalida a listagem agrupada de usuários de uma organização.
     * Deve ser chamado sempre que um Profissional, Secretaria ou AdminOrganizacao
     * for criado, atualizado, removido ou tiver seu status alterado.
     *
     * @param organizacaoId ID da organização (null é ignorado; use evictUsuariosAgrupadosSuperAdmin())
     */
    public void evictUsuariosAgrupados(Long organizacaoId) {
        if (organizacaoId == null) {
            return;
        }
        evict(CACHE_USUARIOS_AGRUPADOS, organizacaoId);
    }

    /**
     * Invalida a listagem agrupada global (visão do SUPER_ADMIN, sem filtro de organização).
     */
    public void evictUsuariosAgrupadosSuperAdmin() {
        evict(CACHE_USUARIOS_AGRUPADOS, CHAVE_SUPER_ADMIN);
    }

    /**
     * Invalida, em uma única chamada, o perfil do usuário e a listagem agrupada
     * da sua organização (e a visão global do SUPER_ADMIN). Uso recomendado para
     * a maioria dos fluxos de cadastro/atualização/remoção.
     *
     * @param usuarioId     ID do usuário afetado (pode ser null)
     * @param organizacaoId ID da organização afetada (pode ser null)
     */
    public void evictPerfilEUsuariosAgrupados(Long usuarioId, Long organizacaoId) {
        evictPerfilUsuario(usuarioId);
        evictUsuariosAgrupados(organizacaoId);
        evictUsuariosAgrupadosSuperAdmin();
    }

    /**
     * Monta a chave dos caches indexados por consulta, sempre prefixada pela organização.
     * Usada tanto nas anotações {@code @Cacheable} (via SpEL) quanto nas evicções manuais,
     * garantindo o mesmo formato nos dois lados e impedindo que uma clínica leia o
     * valor em cache de outra apenas trocando o ID da consulta.
     *
     * @param organizacaoId ID da organização dona da consulta
     * @param consultaId    ID da consulta
     * @return chave no formato {@code org-<organizacaoId>-<consultaId>}
     */
    public static String chaveConsultaPorTenant(Long organizacaoId, Long consultaId) {
        return "org-" + organizacaoId + "-" + consultaId;
    }

    /**
     * Invalida o prontuário médico mais recente em cache de uma consulta.
     * Chamado via bean (e não por {@code @CacheEvict} na própria classe) porque a
     * auto-invocação não passa pelo proxy do Spring e a anotação seria ignorada.
     *
     * @param organizacaoId ID da organização dona da consulta
     * @param consultaId    ID da consulta
     */
    public void evictProntuarioRecente(Long organizacaoId, Long consultaId) {
        evictAposCommit(CACHE_PRONTUARIO_RECENTE, chaveConsultaPorTenant(organizacaoId, consultaId));
    }

    /**
     * Invalida o questionário de saúde em cache de uma consulta.
     *
     * @param organizacaoId ID da organização dona da consulta
     * @param consultaId    ID da consulta
     */
    public void evictQuestionarioSaude(Long organizacaoId, Long consultaId) {
        evictAposCommit(CACHE_QUESTIONARIO_SAUDE, chaveConsultaPorTenant(organizacaoId, consultaId));
    }

    /**
     * Invalida o acesso em cache da assinatura de uma organização (consultado pelo FiltroAcesso).
     * Chamado quando a assinatura muda, para a suspensão/reativação valer na próxima requisição.
     *
     * @param organizacaoId ID da organização (null é ignorado)
     */
    public void evictAcessoAssinatura(Long organizacaoId) {
        if (organizacaoId == null) {
            return;
        }
        evictAposCommit(CACHE_ACESSO_ASSINATURA, organizacaoId);
    }

    /**
     * Evicta somente depois do commit: dentro da transação, uma leitura concorrente poderia
     * repopular o cache com o dado antigo antes de o commit ficar visível. Sem transação ativa
     * (ex.: testes unitários) o evict é imediato.
     */
    private void evictAposCommit(String cacheName, Object key) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    evict(cacheName, key);
                }
            });
        } else {
            evict(cacheName, key);
        }
    }

    private void evict(String cacheName, Object key) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            log.warn("cache.invalidar negado motivo=CACHE_NAO_ENCONTRADO cache={} chave={}", cacheName, key);
            return;
        }
        cache.evict(key);
        log.debug("cache.invalidar ok cache={} chave={}", cacheName, key);
    }
}
