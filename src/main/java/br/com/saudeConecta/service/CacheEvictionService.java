package br.com.saudeConecta.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

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
@Component
@Slf4j
public class CacheEvictionService {

    private static final String CACHE_PERFIL_USUARIO = "perfil-usuario";
    private static final String CACHE_USUARIOS_AGRUPADOS = "usuarios-agrupados";
    private static final String CHAVE_SUPER_ADMIN = "super-admin";

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

    private void evict(String cacheName, Object key) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            log.warn("Cache '{}' nao encontrado ao tentar invalidar chave: {}", cacheName, key);
            return;
        }
        cache.evict(key);
        log.debug("Cache '{}' invalidado para a chave: {}", cacheName, key);
    }
}
