package br.com.saudeConecta.service;

import br.com.saudeConecta.domain.admin.AdminOrganizacao;
import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.usuario.TipoUsuarioNovo;
import br.com.saudeConecta.domain.usuario.Usuario;
import br.com.saudeConecta.domain.usuario.StatusUsuario;
import br.com.saudeConecta.email.CredenciaisEmailService;
import br.com.saudeConecta.email.EmailCadastroService;
import br.com.saudeConecta.infra.tenant.TenantHelper;
import br.com.saudeConecta.infrastructure.persistence.repository.AdminOrganizacaoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.OrganizacaoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.UsuarioRepository;
import br.com.saudeConecta.presentation.dto.admin.CadastrarAdminRequest;
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
public class AdminOrganizacaoService {

    private static final String CARACTERES_SENHA = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#$%&*";
    private static final int TAMANHO_SENHA = 10;

    private final AdminOrganizacaoRepository adminOrganizacaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final OrganizacaoRepository organizacaoRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailCadastroService emailCadastroService;
    private final Executor emailTaskExecutor;
    private final TenantHelper tenantHelper;


    public AdminOrganizacaoService(
            AdminOrganizacaoRepository adminOrganizacaoRepository,
            UsuarioRepository usuarioRepository,
            OrganizacaoRepository organizacaoRepository,
            PasswordEncoder passwordEncoder,
            TenantHelper tenantHelper,
            EmailCadastroService emailCadastroService,
            @Qualifier("emailTaskExecutor") Executor emailTaskExecutor) {
        this.adminOrganizacaoRepository = adminOrganizacaoRepository;
        this.usuarioRepository = usuarioRepository;
        this.organizacaoRepository = organizacaoRepository;
        this.passwordEncoder = passwordEncoder;
        this.tenantHelper = tenantHelper;
        this.emailCadastroService = emailCadastroService;
        this.emailTaskExecutor = emailTaskExecutor;
    }

    @Transactional(readOnly = true)
    public Optional<AdminOrganizacao> buscarrAdminByOrg(Long id) {
        log.debug("Buscando administrador por ID: {}", id);
        Long orgId = tenantHelper.getCurrentTenantId();
        return adminOrganizacaoRepository.findByIdAndOrganizacao_Id(id,orgId);
    }

    @Transactional
    public AdminOrganizacao cadastrarAdminByOrg(CadastrarAdminRequest request, Long organizacaoId) {
        log.info("Cadastrando administrador: {} para organização ID: {}", request.nome(), organizacaoId);

        // Verifica se CPF já existe como login
        //Todo Colocar validação de CPF de login na tabela de usuários nao da entidade
        if (usuarioRepository.existsByLogin(request.cpf())) {
            log.warn("CPF já cadastrado como login: {}", request.cpf());
            throw new IllegalStateException("CPF já cadastrado no sistema");
        }

        // Busca a organização
        Organizacao organizacao = organizacaoRepository.findById(organizacaoId)
            .orElseThrow(() -> new IllegalArgumentException("Organização não encontrada"));

        String cpfLimpo = limparCpf(request.cpf());
        String senhaGerada = gerarSenhaAleatoria();

        Usuario usuario = Usuario.builder()
                .login(cpfLimpo)
                .senha(passwordEncoder.encode(senhaGerada))
                .tipoUsuario((byte) 1) // ADMIN_ORG
                .tipoUsuarioNovo(TipoUsuarioNovo.ADMIN_ORG)
                .status(StatusUsuario.ATIVO)
                .organizacao(organizacao)
                .build();
        

         AdminOrganizacao admin = AdminOrganizacao.builder()
            .organizacao(organizacao)
            .usuario(usuario)
            .nome(request.nome())
            .email(request.email())
            .cargo("Administrador")
            .isOwner(false)
            .status(AdminOrganizacao.StatusAdmin.ATIVO)
            .build();

        usuario = usuarioRepository.save(usuario);
        log.debug("Usuário criado com ID: {}", usuario.getId());
        admin = adminOrganizacaoRepository.save(admin);
        log.info("Administrador cadastrado com sucesso. ID: {}", admin.getId());

        // Envio de email assíncrono otimizado
        CompletableFuture.runAsync(() -> {
            try {
                emailCadastroService.enviarCredenciaisAdministradorAsync(
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

        return admin;
    }

    @Transactional
    public AdminOrganizacao atualizarAdmByOrg(Long id, String nome, String email) {
        log.info("Atualizando administrador ID: {}", id);

        AdminOrganizacao admin = adminOrganizacaoRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Administrador não encontrado"));

        if (nome != null && !nome.isBlank()) {
            admin.setNome(nome);
        }
        if (email != null && !email.isBlank()) {
            admin.setEmail(email);
        }

        admin = adminOrganizacaoRepository.save(admin);
        log.info("Administrador atualizado com sucesso. ID: {}", admin.getId());

        return admin;
    }

 
    @Transactional
    public void deletarAdmByOrg(Long idAdmin) {
        Long orgId = tenantHelper.getCurrentTenantId();
        log.info("Deletando administrador ID: {} da organização: {}", idAdmin, orgId);

        // 1. Buscar o administrador na tabela admin_organizacao
        AdminOrganizacao admin = adminOrganizacaoRepository.findByIdAndOrganizacao_Id(idAdmin, orgId)
                .orElseThrow(() -> new IllegalArgumentException("Administrador não encontrado"));

        Usuario usuario = admin.getUsuario();
        if (usuario == null) {
            throw new IllegalStateException("Administrador não possui usuário associado");
        }

        try {
            // 2. Deletar o registro de administrador na tabela admin_organizacao primeiro
            adminOrganizacaoRepository.delete(admin);
            
            // 3. Deletar o usuário da tabela usuario
            usuarioRepository.delete(usuario);
            
            log.info("Administrador e usuário deletados com sucesso. ID Admin: {}, ID Usuário: {}", 
                    idAdmin, usuario.getId());
                    
        } catch (Exception e) {
            // 4. Caso haja relacionamento que impeça o delete, cancelar e avisar
            String errorMessage = "Não foi possível deletar o administrador devido a relacionamentos existentes: " + e.getMessage();
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
        return cpf.replaceAll("[^0-9]", "");
    }
}
