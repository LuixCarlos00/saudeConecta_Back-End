package br.com.saudeConecta.application.service;

import br.com.saudeConecta.domain.organizacao.Organizacao;
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

    public SecretariaService(
            SecretariaRepository secretariaRepository,
            UsuarioRepository usuarioRepository,
            OrganizacaoRepository organizacaoRepository,
            PasswordEncoder passwordEncoder,
            TenantHelper tenantHelper,
            EmailCadastroService emailCadastroService,
            @Qualifier("emailTaskExecutor") Executor emailTaskExecutor) {
        this.secretariaRepository = secretariaRepository;
        this.usuarioRepository = usuarioRepository;
        this.organizacaoRepository = organizacaoRepository;
        this.passwordEncoder = passwordEncoder;
        this.tenantHelper = tenantHelper;
        this.emailCadastroService = emailCadastroService;
        this.emailTaskExecutor = emailTaskExecutor;
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
        
        Secretaria secretaria = secretariaRepository.findByIdAndOrganizacao_Id(id, orgId)
            .orElseThrow(() -> new IllegalArgumentException("Secretária não encontrada"));

        // Atualiza apenas os campos permitidos (nome e email)
        if (dadosAtualizados.getNome() != null) {
            secretaria.setNome(dadosAtualizados.getNome());
        }
        if (dadosAtualizados.getEmail() != null) {
            secretaria.setEmail(dadosAtualizados.getEmail());
        }
         return secretariaRepository.save(secretaria);
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
