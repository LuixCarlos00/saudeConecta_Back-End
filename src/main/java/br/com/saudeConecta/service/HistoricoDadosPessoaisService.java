package br.com.saudeConecta.service;

 import br.com.saudeConecta.domain.historicodadospessoais.EntidadeTipo;
 import br.com.saudeConecta.domain.historicodadospessoais.HistoricoDadosPessoais;
 import br.com.saudeConecta.domain.organizacao.Organizacao;
 import br.com.saudeConecta.domain.usuario.Usuario;
 import br.com.saudeConecta.infra.tenant.TenantHelper;
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





//    @Transactional(readOnly = true)
//    public List<HistoricoDadosPessoais> buscarHistoricoByEntidade(String entidade, Long idEntidade) {
//        Long orgId = tenantHelper.getCurrentTenantId();
//        return historicoRepository.findByOrganizacao_IdAndEntidadeAndIdEntidadeOrderByCriadoEmDesc(
//                orgId, entidade, idEntidade);
//    }

    @Transactional(readOnly = true)
    public List<HistoricoDadosPessoais> buscarHistoricoByUsuario(Long idUsuario) {
        Long orgId = tenantHelper.getCurrentTenantId();
        return historicoRepository.findByOrganizacao_IdAndUsuario_IdOrderByCriadoEmDesc(orgId, idUsuario);
    }








   ///=======================================


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
            log.warn("Historico ignorado - objeto antes ou depois e nulo. Entidade: {}, ID: {}", entidade, idEntidade);
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
            if (CAMPOS_IGNORADOS.contains(campo.getName())) continue;
            if (Modifier.isStatic(campo.getModifiers())) continue;

            campo.setAccessible(true);

            try {
                Object valorAntes  = campo.get(antes);
                Object valorDepois = campo.get(depois);

                // Sem mudança — ignora (com tratamento especial para BigDecimal)
                if (saoIguais(valorAntes, valorDepois)) continue;

                String strAntes  = valorAntes  != null ? valorAntes.toString()  : null;
                String strDepois = valorDepois != null ? valorDepois.toString() : null;

                log.debug("Campo alterado - Entidade: {}, ID: {}, Campo: {}, Antes: {}, Depois: {}",
                        entidade, idEntidade, campo.getName(), strAntes, strDepois);

                registros.add(HistoricoDadosPessoais.builder()
                        .organizacao(organizacao)
                        .usuario(usuario)
                        .entidade(entidade)
                        .idEntidade(idEntidade)
                        .campo(campo.getName())
                        .valorAnterior(strAntes)
                        .valorNovo(strDepois)
                        .build());

            } catch (IllegalAccessException e) {
                log.warn("Nao foi possivel acessar campo '{}' para historico: {}", campo.getName(), e.getMessage());
            }
        }

        if (registros.isEmpty()) {
            log.debug("Nenhuma alteracao detectada para Entidade: {}, ID: {}", entidade, idEntidade);
            return;
        }

        historicoRepository.saveAll(registros);
        log.info("{} campo(s) registrado(s) no historico. Entidade: {}, ID: {}", registros.size(), entidade, idEntidade);
    }

    /**
     * Compara dois valores com tratamento especial para BigDecimal (ignora scale).
     * BigDecimal.equals(250.00, 250) retorna false; compareTo retorna 0.
     */
    private boolean saoIguais(Object a, Object b) {
        if (Objects.equals(a, b)) return true;
        if (a instanceof BigDecimal && b instanceof BigDecimal) {
            return ((BigDecimal) a).compareTo((BigDecimal) b) == 0;
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