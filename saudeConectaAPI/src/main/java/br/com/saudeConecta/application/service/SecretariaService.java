package br.com.saudeConecta.application.service;

import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.secretaria.Secretaria;
import br.com.saudeConecta.domain.secretaria.StatusSecretaria;
import br.com.saudeConecta.domain.usuario.TipoUsuarioNovo;
import br.com.saudeConecta.domain.usuario.Usuario;
import br.com.saudeConecta.email.EnviarService.CredenciaisEmailService;
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
import java.util.List;
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
    private final CredenciaisEmailService credenciaisEmailService;
    private final Executor emailTaskExecutor;

    public SecretariaService(
            SecretariaRepository secretariaRepository,
            UsuarioRepository usuarioRepository,
            OrganizacaoRepository organizacaoRepository,
            PasswordEncoder passwordEncoder,
            TenantHelper tenantHelper,
            CredenciaisEmailService credenciaisEmailService,
            @Qualifier("emailTaskExecutor") Executor emailTaskExecutor) {
        this.secretariaRepository = secretariaRepository;
        this.usuarioRepository = usuarioRepository;
        this.organizacaoRepository = organizacaoRepository;
        this.passwordEncoder = passwordEncoder;
        this.tenantHelper = tenantHelper;
        this.credenciaisEmailService = credenciaisEmailService;
        this.emailTaskExecutor = emailTaskExecutor;
    }

    @RequiresTenant
    @Transactional(readOnly = true)
    public List<Secretaria> buscarTodas() {
        Long orgId = tenantHelper.getCurrentTenantId();
        return secretariaRepository.findByOrganizacao_Id(orgId);
    }

    @RequiresTenant
    @Transactional(readOnly = true)
    public List<Secretaria> buscarAtivas() {
        Long orgId = tenantHelper.getCurrentTenantId();
        return secretariaRepository.findByOrganizacao_IdAndStatus(orgId, StatusSecretaria.ATIVO);
    }

    @RequiresTenant
    @Transactional(readOnly = true)
    public Optional<Secretaria> buscarPorId(Long id) {
        Long orgId = tenantHelper.getCurrentTenantId();
        return secretariaRepository.findByIdAndOrganizacao_Id(id, orgId);
    }

    @RequiresTenant
    @Transactional
    public Secretaria cadastrar(CadastrarSecretariaRequest request) {
        Long orgId = tenantHelper.getCurrentTenantId();
        log.info("Cadastrando secretária: {} na organização: {}", request.secreNome(), orgId);

        String cpfLimpo = limparCpf(request.secreCpf());

        if (usuarioRepository.existsByLogin(cpfLimpo)) {
            throw new IllegalStateException("CPF já cadastrado no sistema");
        }

        Organizacao organizacao = organizacaoRepository.findById(orgId)
            .orElseThrow(() -> new IllegalStateException("Organização não encontrada"));

        String senhaGerada = gerarSenhaAleatoria();
        String senhaCriptografada = passwordEncoder.encode(senhaGerada);

        Usuario usuario = new Usuario();
        usuario.setLogin(cpfLimpo);
        usuario.setSenha(senhaCriptografada);
        usuario.setTipoUsuario((byte) 2); // RECEPCIONISTA
        usuario.setTipoUsuarioNovo(TipoUsuarioNovo.RECEPCIONISTA);
        usuario.setOrganizacao(organizacao);
        usuario.setStatus((byte) 1);

        Secretaria secretaria = Secretaria.builder()
            .organizacao(organizacao)
            .usuario(usuario)
            .nome(request.secreNome())
            .cpf(cpfLimpo)
            .email(request.secreEmail())
            .status(StatusSecretaria.ATIVO)
            .build();

        usuarioRepository.save(usuario);
        Secretaria salva = secretariaRepository.save(secretaria);
        log.info("Secretária cadastrada com sucesso. ID: {}", salva.getId());

        // Envio de email assíncrono otimizado
        CompletableFuture.runAsync(() -> {
            try {
                credenciaisEmailService.enviarCredenciaisSecretaria(
                    request.secreEmail(),
                    request.secreNome(),
                    cpfLimpo,
                    senhaGerada
                );
                log.info("Email enviado para: {}", request.secreEmail());
            } catch (Exception e) {
                log.error("Erro ao enviar email para: {}", request.secreEmail(), e);
            }
        }, emailTaskExecutor);

        return salva;
    }

    @RequiresTenant
    @Transactional
    public void inativar(Long id) {
        Long orgId = tenantHelper.getCurrentTenantId();
        Secretaria secretaria = secretariaRepository.findByIdAndOrganizacao_Id(id, orgId)
            .orElseThrow(() -> new IllegalArgumentException("Secretária não encontrada"));

        secretaria.setStatus(StatusSecretaria.INATIVO);
        secretariaRepository.save(secretaria);

        if (secretaria.getUsuario() != null) {
            secretaria.getUsuario().setStatus((byte) 0);
            usuarioRepository.save(secretaria.getUsuario());
        }

        log.info("Secretária ID: {} inativada com sucesso", id);
    }

    @RequiresTenant
    @Transactional(readOnly = true)
    public Long contarAtivas() {
        Long orgId = tenantHelper.getCurrentTenantId();
        return secretariaRepository.countAtivosByOrganizacaoId(orgId);
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
