package br.com.saudeConecta.application.secretaria;

import br.com.saudeConecta.application.assinatura.LimitePlanoService;
import br.com.saudeConecta.application.cache.CacheEvictionService;
import br.com.saudeConecta.application.historico.HistoricoDadosPessoaisService;
import br.com.saudeConecta.application.historico.SnapshotUtil;
import br.com.saudeConecta.application.notificacao.EmailNotificacaoService;
import br.com.saudeConecta.application.secretaria.dto.AtualizarSecretariaRequest;
import br.com.saudeConecta.application.secretaria.dto.CadastrarSecretariaRequest;
import br.com.saudeConecta.application.usuario.EmailUnicoService;
import br.com.saudeConecta.config.tenant.RequiresTenant;
import br.com.saudeConecta.config.tenant.TenantHelper;
import br.com.saudeConecta.domain.historicodadospessoais.EntidadeTipo;
import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.secretaria.Secretaria;
import br.com.saudeConecta.domain.secretaria.StatusSecretaria;
import br.com.saudeConecta.domain.usuario.StatusUsuario;
import br.com.saudeConecta.domain.usuario.TipoUsuarioNovo;
import br.com.saudeConecta.domain.usuario.Usuario;
import br.com.saudeConecta.infrastructure.persistence.repository.OrganizacaoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.SecretariaRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Optional;

@Service
@Slf4j
public class SecretariaService {

    /** Um gerador só por classe: criar um SecureRandom por senha gasta entropia à toa (S2119). */
    private static final SecureRandom GERADOR_SENHA = new SecureRandom();
    private static final String CARACTERES_SENHA = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#$%&*";
    private static final int TAMANHO_SENHA = 10;

    private final SecretariaRepository secretariaRepository;
    private final UsuarioRepository usuarioRepository;
    private final OrganizacaoRepository organizacaoRepository;
    private final PasswordEncoder passwordEncoder;
    private final TenantHelper tenantHelper;
    private final EmailNotificacaoService emailNotificacaoService;
    private final EmailUnicoService emailUnicoService;
    private final HistoricoDadosPessoaisService historicoDadosPessoaisService;
    private final LimitePlanoService limitePlanoService;
    private final CacheEvictionService cacheEvictionService;

    public SecretariaService(
            SecretariaRepository secretariaRepository,
            UsuarioRepository usuarioRepository,
            OrganizacaoRepository organizacaoRepository,
            PasswordEncoder passwordEncoder,
            TenantHelper tenantHelper,
            EmailNotificacaoService emailNotificacaoService,
            EmailUnicoService emailUnicoService,
            HistoricoDadosPessoaisService historicoDadosPessoaisService,
            LimitePlanoService limitePlanoService,
            CacheEvictionService cacheEvictionService) {
        this.secretariaRepository = secretariaRepository;
        this.usuarioRepository = usuarioRepository;
        this.organizacaoRepository = organizacaoRepository;
        this.passwordEncoder = passwordEncoder;
        this.tenantHelper = tenantHelper;
        this.emailNotificacaoService = emailNotificacaoService;
        this.emailUnicoService = emailUnicoService;
        this.historicoDadosPessoaisService = historicoDadosPessoaisService;
        this.limitePlanoService = limitePlanoService;
        this.cacheEvictionService = cacheEvictionService;
    }

    @RequiresTenant
    @Transactional(readOnly = true)
    public Optional<Secretaria> buscarSecretariaIdByOrg(Long idSecretaria) {
        Long orgId = tenantHelper.getCurrentTenantId();
        return secretariaRepository.findByIdAndOrganizacao_Id(idSecretaria, orgId);
    }

    @RequiresTenant
    @Transactional
    public Secretaria atualizarSecretariaIdByOrg(Long id, AtualizarSecretariaRequest dadosAtualizados) {
        Long orgId = tenantHelper.getCurrentTenantId();
        
        Secretaria antes = secretariaRepository.findByIdAndOrganizacao_Id(id, orgId)
            .orElseThrow(() -> new IllegalArgumentException("Secretaria nao encontrada"));

        // Trocar o e-mail de outro usuário permite tomar a conta pelo "esqueci minha senha"
        tenantHelper.validarGestorOuDono(antes.getUsuario() != null ? antes.getUsuario().getId() : null);

        // E-mail duplicado desvia o "esqueci minha senha" (que busca por e-mail) para a conta errada
        emailUnicoService.validarEmailLivreParaAtualizacao(dadosAtualizados.email(), antes.getEmail(),
                antes.getUsuario() != null ? antes.getUsuario().getId() : null);

        Secretaria snapshot = SnapshotUtil.copiarSnapshot(antes);

        // Atualiza campos permitidos (nome, email e telefone)
        if (dadosAtualizados.nome() != null) {
            antes.setNome(dadosAtualizados.nome());
        }
        if (dadosAtualizados.email() != null) {
            antes.setEmail(dadosAtualizados.email());
        }
        if (dadosAtualizados.telefone() != null) {
            antes.setTelefone(dadosAtualizados.telefone());
        }
       Secretaria resultado = secretariaRepository.save(antes);
        log.info("secretaria.atualizar ok secretariaId={}", antes.getId());

        historicoDadosPessoaisService.registrarAlteracoesDeObjeto(
                EntidadeTipo.SECRETARIA,
                resultado.getId(),
                tenantHelper.getCurrentUserId(),
                snapshot,
                resultado
        );

        cacheEvictionService.evictPerfilEUsuariosAgrupados(resultado.getUsuario().getId(), orgId);

        return resultado;
    }

    @RequiresTenant
    @Transactional
    public Secretaria cadastrarSecretariaByOrg(CadastrarSecretariaRequest request) {
        Long orgId = tenantHelper.getCurrentTenantId();
        log.info("secretaria.cadastrar inicio organizacaoId={}", orgId);

        limitePlanoService.validarLimiteSecretaria(orgId);

        String cpfLimpo = limparCpf(request.cpf());

        if (usuarioRepository.existsByLogin(cpfLimpo)) {
            throw new IllegalStateException("CPF já cadastrado no sistema");
        }

        // Verificar se o email já existe em qualquer tabela
        if (emailUnicoService.emailJaExiste(request.email())) {
            String tabela = emailUnicoService.ondeEmailFoiEncontrado(request.email());
            throw new IllegalStateException("Email já cadastrado no sistema como " + tabela);
        }

        Organizacao organizacao = organizacaoRepository.findById(orgId)
                .orElseThrow(() -> new IllegalStateException("Organizacao nao encontrada"));

        String senhaGerada = gerarSenhaAleatoria();
        String senhaCriptografada = passwordEncoder.encode(senhaGerada);

        Usuario usuario = Usuario.builder()
                .login(cpfLimpo)
                .senha(senhaCriptografada)
                .tipoUsuarioNovo(TipoUsuarioNovo.ASSISTENTE)
                .organizacao(organizacao)
                .status(StatusUsuario.ATIVO)
                .build();

        Secretaria secretaria = Secretaria.builder()
                .organizacao(organizacao)
                .usuario(usuario)
                .nome(request.nome())
                .cpf(cpfLimpo)
                .telefone(request.telefone())
                .email(request.email())
                .status(StatusSecretaria.ATIVO)
                .build();

        usuarioRepository.save(usuario);
        Secretaria salva = secretariaRepository.save(secretaria);
        log.info("secretaria.cadastrar ok secretariaId={}", salva.getId());

        cacheEvictionService.evictPerfilEUsuariosAgrupados(usuario.getId(), orgId);

        emailNotificacaoService.enviarCredenciaisSecretaria(
                request.email(),
                request.nome(),
                cpfLimpo,
                senhaGerada,
                orgId,
                salva.getId()
        );

        return salva;
    }

    @RequiresTenant
    @Transactional
    public void deletarSecretariaIdByOrg(Long idSecretaria) {
        Long orgId = tenantHelper.getCurrentTenantId();
        log.info("secretaria.deletar inicio secretariaId={} organizacaoId={}", idSecretaria, orgId);

        // 1. Buscar a Secretaria na tabela secretaria
        Secretaria secretaria = secretariaRepository.findByIdAndOrganizacao_Id(idSecretaria, orgId)
                .orElseThrow(() -> new IllegalArgumentException("Secretaria nao encontrada"));

        Usuario usuario = secretaria.getUsuario();
        if (usuario == null) {
            throw new IllegalStateException("Secretaria nao possui Usuario associado");
        }

        try {
            Long usuarioId = usuario.getId();

            // 2. Deletar o registro de secretaria na tabela de secretaria primeiro
            secretariaRepository.delete(secretaria);
            
            // 3. Deletar o Usuario da tabela usuario
            usuarioRepository.delete(usuario);

            cacheEvictionService.evictPerfilEUsuariosAgrupados(usuarioId, orgId);

            log.info("secretaria.deletar ok secretariaId={} usuarioId={}",
                    idSecretaria, usuario.getId());
                    
        } catch (Exception e) {
            // 4. Caso haja relacionamento que impeça o delete, cancelar e avisar
            // Mensagem do banco (tabela/constraint) fica só no log; a causa segue encadeada
            String errorMessage = "Não foi possível deletar a Secretaria devido a relacionamentos existentes";
            log.warn("secretaria.deletar negado motivo=RELACIONAMENTOS_EXISTENTES secretariaId={}", idSecretaria, e);
            throw new IllegalStateException(errorMessage, e);
        }
    }

    private String gerarSenhaAleatoria() {
        StringBuilder senha = new StringBuilder(TAMANHO_SENHA);
        for (int i = 0; i < TAMANHO_SENHA; i++) {
            int index = GERADOR_SENHA.nextInt(CARACTERES_SENHA.length());
            senha.append(CARACTERES_SENHA.charAt(index));
        }
        return senha.toString();
    }

    private String limparCpf(String cpf) {
        return cpf != null ? cpf.replaceAll("\\D", "") : null;
    }
}
