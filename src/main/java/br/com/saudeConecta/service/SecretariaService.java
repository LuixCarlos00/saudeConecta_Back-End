package br.com.saudeConecta.service;

import br.com.saudeConecta.domain.historicodadospessoais.EntidadeTipo;
import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.secretaria.Secretaria;
import br.com.saudeConecta.domain.secretaria.StatusSecretaria;
import br.com.saudeConecta.domain.usuario.StatusUsuario;
import br.com.saudeConecta.domain.usuario.TipoUsuarioNovo;
import br.com.saudeConecta.domain.usuario.Usuario;
import br.com.saudeConecta.email.EmailNotificacaoService;
import br.com.saudeConecta.infra.tenant.RequiresTenant;
import br.com.saudeConecta.infra.tenant.TenantHelper;
import br.com.saudeConecta.infrastructure.persistence.repository.OrganizacaoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.SecretariaRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.UsuarioRepository;
import br.com.saudeConecta.presentation.dto.secretaria.CadastrarSecretariaRequest;
import br.com.saudeConecta.util.EmailUnicoService;
import br.com.saudeConecta.util.SnapshotUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Optional;

@Service
@Slf4j
public class SecretariaService {

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
    public Secretaria atualizarSecretariaIdByOrg(Long id, Secretaria dadosAtualizados) {
        Long orgId = tenantHelper.getCurrentTenantId();
        
        Secretaria antes = secretariaRepository.findByIdAndOrganizacao_Id(id, orgId)
            .orElseThrow(() -> new IllegalArgumentException("Secretaria nao encontrada"));

        Secretaria snapshot = SnapshotUtil.copiarSnapshot(antes);


        // Atualiza campos permitidos (nome, email e telefone)
        if (dadosAtualizados.getNome() != null) {
            antes.setNome(dadosAtualizados.getNome());
        }
        if (dadosAtualizados.getEmail() != null) {
            antes.setEmail(dadosAtualizados.getEmail());
        }
        if (dadosAtualizados.getTelefone() != null) {
            antes.setTelefone(dadosAtualizados.getTelefone());
        }
       Secretaria resultado = secretariaRepository.save(antes);
        log.info("Secretaria atualizada com sucesso. ID: {}", antes.getId());

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
        log.info("Cadastrando Secretaria: {} na Organizacao: {}", request.nome(), orgId);

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
        log.info("Secretaria cadastrada com sucesso. ID: {}", salva.getId());

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
        log.info("Deletando Secretaria ID: {} da Organizacao: {}", idSecretaria, orgId);

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

            log.info("Secretaria e Usuario deletados com sucesso. ID Secretaria: {}, ID Usuario: {}", 
                    idSecretaria, usuario.getId());
                    
        } catch (Exception e) {
            // 4. Caso haja relacionamento que impeça o delete, cancelar e avisar
            String errorMessage = "Não foi possível deletar a Secretaria devido a relacionamentos existentes: " + e.getMessage();
            log.error(errorMessage);
            throw new IllegalStateException(errorMessage);
        }
    }

    private String gerarSenhaAleatoria() {
        SecureRandom random = new SecureRandom();
        StringBuilder senha = new StringBuilder(TAMANHO_SENHA);
        for (int i = 0; i < TAMANHO_SENHA; i++) {
            int index = random.nextInt(CARACTERES_SENHA.length());
            senha.append(CARACTERES_SENHA.charAt(index));
        }
        return senha.toString();
    }

    private String limparCpf(String cpf) {
        return cpf != null ? cpf.replaceAll("[^0-9]", "") : null;
    }
}
