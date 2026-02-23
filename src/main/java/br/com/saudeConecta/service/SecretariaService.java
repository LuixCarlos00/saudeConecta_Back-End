package br.com.saudeConecta.service;

import br.com.saudeConecta.domain.historicodadospessoais.EntidadeTipo;
import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.paciente.Paciente;
import br.com.saudeConecta.domain.secretaria.Secretaria;
import br.com.saudeConecta.domain.secretaria.StatusSecretaria;
import br.com.saudeConecta.domain.usuario.TipoUsuarioNovo;
import br.com.saudeConecta.domain.usuario.Usuario;
import br.com.saudeConecta.domain.usuario.StatusUsuario;
import br.com.saudeConecta.email.CredenciaisEmailService;
import br.com.saudeConecta.email.EmailCadastroService;
import br.com.saudeConecta.infra.tenant.RequiresTenant;
import br.com.saudeConecta.infra.tenant.TenantHelper;
import br.com.saudeConecta.infrastructure.persistence.repository.OrganizacaoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.SecretariaRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.UsuarioRepository;
import br.com.saudeConecta.presentation.dto.secretaria.CadastrarSecretariaRequest;
import br.com.saudeConecta.util.EmailUnicoService;
import br.com.saudeConecta.util.SnapshotUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

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
    private final EmailCadastroService emailCadastroService;
    private final Executor emailTaskExecutor;
    private final EmailUnicoService emailUnicoService;
    private final HistoricoDadosPessoaisService historicoDadosPessoaisService;


    public SecretariaService(
            SecretariaRepository secretariaRepository,
            UsuarioRepository usuarioRepository,
            OrganizacaoRepository organizacaoRepository,
            PasswordEncoder passwordEncoder,
            TenantHelper tenantHelper,
            EmailCadastroService emailCadastroService,
            @Qualifier("emailTaskExecutor") Executor emailTaskExecutor,
            EmailUnicoService emailUnicoService,
            HistoricoDadosPessoaisService historicoDadosPessoaisService) {
        this.secretariaRepository = secretariaRepository;
        this.usuarioRepository = usuarioRepository;
        this.organizacaoRepository = organizacaoRepository;
        this.passwordEncoder = passwordEncoder;
        this.tenantHelper = tenantHelper;
        this.emailCadastroService = emailCadastroService;
        this.emailTaskExecutor = emailTaskExecutor;
        this.emailUnicoService = emailUnicoService;
        this.historicoDadosPessoaisService = historicoDadosPessoaisService;
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
            .orElseThrow(() -> new IllegalArgumentException("Secretária não encontrada"));

        Secretaria snapshot = SnapshotUtil.copiarSnapshot(antes);


        // Atualiza apenas os campos permitidos (nome e email)
        if (dadosAtualizados.getNome() != null) {
            antes.setNome(dadosAtualizados.getNome());
        }
        if (dadosAtualizados.getEmail() != null) {
            antes.setEmail(dadosAtualizados.getEmail());
        }
       Secretaria resultado = secretariaRepository.save(antes);
        log.info("Secretária atualizada com sucesso. ID: {}", antes.getId());

        historicoDadosPessoaisService.registrarAlteracoesDeObjeto(
                EntidadeTipo.PROFISSIONAL,
                resultado.getId(),
                tenantHelper.getCurrentUserId(),
                snapshot,
                resultado
        );

        return resultado;
    }

    @RequiresTenant
    @Transactional
    public Secretaria cadastrarSecretariaByOrg(CadastrarSecretariaRequest request) {
        Long orgId = tenantHelper.getCurrentTenantId();
        log.info("Cadastrando secretária: {} na organização: {}", request.nome(), orgId);

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
                .orElseThrow(() -> new IllegalStateException("Organização não encontrada"));

        String senhaGerada = gerarSenhaAleatoria();
        String senhaCriptografada = passwordEncoder.encode(senhaGerada);

        Usuario usuario = Usuario.builder()
                .login(cpfLimpo)
                .senha(senhaCriptografada)
                .tipoUsuario((byte) 2) // RECEPCIONISTA
                .tipoUsuarioNovo(TipoUsuarioNovo.RECEPCIONISTA)
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
        log.info("Secretária cadastrada com sucesso. ID: {}", salva.getId());

        // Envio de email assíncrono otimizado
        CompletableFuture.runAsync(() -> {
            try {
                emailCadastroService.enviarCredenciaisSecretariaAsync(
                        request.email(),
                        request.nome(),
                        cpfLimpo,
                        senhaGerada
                );
                log.info("Email enviado para: {}", request.email());
            } catch (Exception e) {
                log.error("Erro ao enviar email para: {}", request.email(), e);
            }
        }, emailTaskExecutor);

        return salva;
    }

    @RequiresTenant
    @Transactional
    public void deletarSecretariaIdByOrg(Long idSecretaria) {
        Long orgId = tenantHelper.getCurrentTenantId();
        log.info("Deletando secretária ID: {} da organização: {}", idSecretaria, orgId);

        // 1. Buscar a secretária na tabela secretaria
        Secretaria secretaria = secretariaRepository.findByIdAndOrganizacao_Id(idSecretaria, orgId)
                .orElseThrow(() -> new IllegalArgumentException("Secretária não encontrada"));

        Usuario usuario = secretaria.getUsuario();
        if (usuario == null) {
            throw new IllegalStateException("Secretária não possui usuário associado");
        }

        try {
            // 2. Deletar o registro de secretaria na tabela de secretaria primeiro
            secretariaRepository.delete(secretaria);
            
            // 3. Deletar o usuário da tabela usuario
            usuarioRepository.delete(usuario);
            
            log.info("Secretária e usuário deletados com sucesso. ID Secretária: {}, ID Usuário: {}", 
                    idSecretaria, usuario.getId());
                    
        } catch (Exception e) {
            // 4. Caso haja relacionamento que impeça o delete, cancelar e avisar
            String errorMessage = "Não foi possível deletar a secretária devido a relacionamentos existentes: " + e.getMessage();
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
