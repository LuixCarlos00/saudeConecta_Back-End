package br.com.saudeConecta.application.historico;

 import br.com.saudeConecta.domain.historicodadospessoais.EntidadeTipo;
 import br.com.saudeConecta.domain.historicodadospessoais.HistoricoDadosPessoais;
 import br.com.saudeConecta.domain.organizacao.Organizacao;
 import br.com.saudeConecta.domain.usuario.Usuario;
 import br.com.saudeConecta.config.tenant.TenantAccessDeniedException;
 import br.com.saudeConecta.config.tenant.TenantHelper;
import br.com.saudeConecta.infrastructure.persistence.repository.HistoricoDadosPessoaisRepository;
 import br.com.saudeConecta.infrastructure.persistence.repository.OrganizacaoRepository;
 import br.com.saudeConecta.infrastructure.persistence.repository.UsuarioRepository;
 import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
 import java.lang.reflect.Field;
 import java.lang.reflect.Modifier;
 import java.math.BigDecimal;
   import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class HistoricoDadosPessoaisService {

    private final HistoricoDadosPessoaisRepository historicoRepository;
    private final OrganizacaoRepository organizacaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final TenantHelper tenantHelper;

    private static final Set<String> CAMPOS_IGNORADOS = Set.of(
            "serialVersionUID",
            "organizacao",
            "usuario",
            "endereco",           // endereço é tratado separadamente
            "especialidades",     // coleções não comparadas aqui
            "tipoProfissional",   // relação lazy
            "assinaturas",        // coleção lazy da Organizacao
            "status",             // status da Organizacao não é editável pelo Admin
            "logoUrl",            // não editável na tela de dados pessoais
            "createdAt",
            "updatedAt",
            "criadoEm",
            // campos ID de cada entidade
            "id", "paciCodigo", "endCodigo"
    );

    // ==========================================
    // Consultas
    // ==========================================

    /**
     * Busca histórico de alterações por tipo de entidade e ID da entidade.
     * Apenas SUPER_ADMIN pode acessar este método.
     *
     * @param entidade   tipo da entidade (enum EntidadeTipo como String)
     * @param idEntidade ID do registro da entidade
     * @return lista de histórico ordenado por data (mais recente primeiro)
     */
    @Transactional(readOnly = true)
    public List<HistoricoDadosPessoais> buscarHistoricoByEntidade(String entidade, Long idEntidade) {
        // Checagem explícita do papel: ausência de organização no token não basta.
        if (!tenantHelper.isSuperAdmin()) {
            throw new TenantAccessDeniedException("Acesso negado: apenas SUPER_ADMIN pode acessar histórico por entidade");
        }
        log.debug("historico.buscar escopo=ENTIDADE entidade={} entidadeId={}", entidade, idEntidade);
        return historicoRepository.findByEntidadeAndIdEntidadeOrderByCriadoEmDesc(entidade, idEntidade);
    }

    /**
     * Busca histórico de alterações por usuário.
     * Apenas SUPER_ADMIN pode acessar este método.
     *
     * @param idUsuario ID do usuário
     * @return lista de histórico ordenado por data (mais recente primeiro)
     */
    @Transactional(readOnly = true)
    public List<HistoricoDadosPessoais> buscarHistoricoByUsuario(Long idUsuario) {
        // Checagem explícita do papel: ausência de organização no token não basta.
        if (!tenantHelper.isSuperAdmin()) {
            throw new TenantAccessDeniedException("Acesso negado: apenas SUPER_ADMIN pode acessar histórico por usuário");
        }
        log.debug("historico.buscar escopo=USUARIO usuarioId={}", idUsuario);
        return historicoRepository.findByUsuario_IdOrderByCriadoEmDesc(idUsuario);
    }

    /**
     * Busca os 10 registros mais recentes de histórico globalmente.
     * Apenas SUPER_ADMIN pode acessar este método.
     *
     * @return lista dos 10 registros mais recentes
     */
    @Transactional(readOnly = true)
    public List<HistoricoDadosPessoais> buscarHistoricoRecentes() {
        // Checagem explícita do papel: ausência de organização no token não basta.
        if (!tenantHelper.isSuperAdmin()) {
            throw new TenantAccessDeniedException("Acesso negado: apenas SUPER_ADMIN pode acessar histórico");
        }
        log.debug("historico.buscar escopo=RECENTES");
        return historicoRepository.findTop10ByOrderByCriadoEmDesc();
    }

   //=======================================

    /**
     * Compara dois objetos (antes e depois da atualização) via reflection,
     * detecta quais campos mudaram e persiste uma linha por campo alterado.
     *
     * Suporta: Paciente, Profissional, Secretaria, AdminOrganizacao
     *
     * @param entidade     tipo da entidade (enum EntidadeTipo)
     * @param idEntidade   ID do registro que foi alterado
     * @param idUsuario    ID do usuário que realizou a alteração
     * @param antes        objeto com estado ANTES da atualização
     * @param depois       objeto com estado DEPOIS da atualização (já salvo)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarAlteracoesDeObjeto(EntidadeTipo entidade, Long idEntidade,
                                            Long idUsuario, Object antes, Object depois) {
        if (antes == null || depois == null) {
            log.warn("historico.registrar ignorado motivo=OBJETO_NULO entidade={} entidadeId={}", entidade, idEntidade);
            return;
        }

        if (!antes.getClass().equals(depois.getClass())) {
            throw new IllegalArgumentException("Os objetos 'antes' e 'depois' devem ser do mesmo tipo");
        }

        Long orgId = tenantHelper.getCurrentTenantId();

        Organizacao organizacao = organizacaoRepository.findById(orgId)
                .orElseThrow(() -> new IllegalStateException("Organização não encontrada no contexto"));

        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new IllegalStateException("Usuário não encontrado: " + idUsuario));

        List<HistoricoDadosPessoais> registros = new ArrayList<>();

        // Coleta todos os campos da classe e superclasses
        List<Field> campos = getAllFieldsFromClass(antes.getClass());

        for (Field campo : campos) {
            if (CAMPOS_IGNORADOS.contains(campo.getName()) || Modifier.isStatic(campo.getModifiers())) continue;

            campo.setAccessible(true);

            try {
                Object valorAntes  = campo.get(antes);
                Object valorDepois = campo.get(depois);

                // Sem mudança — ignora (com tratamento especial para BigDecimal)
                if (!saoIguais(valorAntes, valorDepois)) {
                    log.debug("historico.registrar campo alterado entidade={} entidadeId={} campo={}",
                            entidade, idEntidade, campo.getName());
                    registros.add(montarRegistro(organizacao, usuario, entidade, idEntidade,
                            campo.getName(), valorAntes, valorDepois));
                }

            } catch (IllegalAccessException e) {
                log.warn("historico.registrar falha motivo=CAMPO_INACESSIVEL campo={} causa={}", campo.getName(), e.getClass().getSimpleName());
            }
        }

        if (registros.isEmpty()) {
            log.debug("historico.registrar sem alteracao entidade={} entidadeId={}", entidade, idEntidade);
            return;
        }

        historicoRepository.saveAll(registros);
        log.debug("historico.registrar ok campos={} entidade={} entidadeId={}", registros.size(), entidade, idEntidade);
    }

    private HistoricoDadosPessoais montarRegistro(Organizacao organizacao, Usuario usuario, EntidadeTipo entidade,
                                                  Long idEntidade, String campo, Object valorAntes, Object valorDepois) {
        return HistoricoDadosPessoais.builder()
                .organizacao(organizacao)
                .usuario(usuario)
                .entidade(entidade)
                .idEntidade(idEntidade)
                .campo(campo)
                .valorAnterior(valorAntes != null ? valorAntes.toString() : null)
                .valorNovo(valorDepois != null ? valorDepois.toString() : null)
                .build();
    }

    /**
     * Compara dois valores com tratamento especial para BigDecimal (ignora scale).
     * BigDecimal.equals(250.00, 250) retorna false; compareTo retorna 0.
     */
    private boolean saoIguais(Object a, Object b) {
        if (Objects.equals(a, b)) return true;
        if (a instanceof BigDecimal bigA && b instanceof BigDecimal bigB) {
            return bigA.compareTo(bigB) == 0;
        }
        return false;
    }

    public static List<Field> getAllFieldsFromClass(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            fields.addAll(Arrays.asList(current.getDeclaredFields()));
            current = current.getSuperclass();
        }
        return fields;
    }

}