package br.com.saudeConecta.service;

import br.com.saudeConecta.domain.admin.AdminOrganizacao;
import br.com.saudeConecta.domain.historicodadospessoais.EntidadeTipo;
import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.profissional.Profissional;
import br.com.saudeConecta.domain.usuario.TipoUsuarioNovo;
import br.com.saudeConecta.domain.usuario.Usuario;
import br.com.saudeConecta.domain.usuario.StatusUsuario;
import br.com.saudeConecta.email.EmailNotificacaoService;
import br.com.saudeConecta.infra.tenant.TenantHelper;
import br.com.saudeConecta.infrastructure.persistence.repository.AdminOrganizacaoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.OrganizacaoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.UsuarioRepository;
import br.com.saudeConecta.domain.endereco.Endereco;
import br.com.saudeConecta.domain.organizacao.TipoOrganizacao;
import br.com.saudeConecta.domain.organizacao.StatusOrganizacao;
import br.com.saudeConecta.infrastructure.persistence.repository.EnderecoRepository;
import br.com.saudeConecta.presentation.dto.admin.AdminOrgCompletoResponse;
import br.com.saudeConecta.presentation.dto.admin.AtualizarAdminOrgCompletoRequest;
import br.com.saudeConecta.presentation.dto.admin.CadastrarAdminOrgCompletoRequest;
import br.com.saudeConecta.presentation.dto.admin.CadastrarAdminRequest;
import br.com.saudeConecta.util.EmailUnicoService;
import br.com.saudeConecta.util.SnapshotUtil;
import br.com.saudeConecta.service.ConfiguracaoGraficoDashboardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Optional;

@Service
@Slf4j
public class AdminOrganizacaoService {

    private static final String CARACTERES_SENHA = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#$%&*";
    private static final int TAMANHO_SENHA = 10;

    private final AdminOrganizacaoRepository adminOrganizacaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final OrganizacaoRepository organizacaoRepository;
    private final EnderecoRepository enderecoRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailNotificacaoService emailNotificacaoService;
    private final TenantHelper tenantHelper;
    private final EmailUnicoService emailUnicoService;
    private final HistoricoDadosPessoaisService historicoDadosPessoaisService;
    private final ConfiguracaoGraficoDashboardService configuracaoGraficoDashboardService;
    private final LimitePlanoService limitePlanoService;



    public AdminOrganizacaoService(
            AdminOrganizacaoRepository adminOrganizacaoRepository,
            UsuarioRepository usuarioRepository,
            OrganizacaoRepository organizacaoRepository,
            EnderecoRepository enderecoRepository,
            PasswordEncoder passwordEncoder,
            TenantHelper tenantHelper,
            EmailNotificacaoService emailNotificacaoService,
            EmailUnicoService emailUnicoService,
            HistoricoDadosPessoaisService historicoDadosPessoaisService,
            ConfiguracaoGraficoDashboardService configuracaoGraficoDashboardService,
            LimitePlanoService limitePlanoService) {
        this.adminOrganizacaoRepository = adminOrganizacaoRepository;
        this.usuarioRepository = usuarioRepository;
        this.organizacaoRepository = organizacaoRepository;
        this.enderecoRepository = enderecoRepository;
        this.passwordEncoder = passwordEncoder;
        this.tenantHelper = tenantHelper;
        this.emailNotificacaoService = emailNotificacaoService;
        this.emailUnicoService = emailUnicoService;
        this.historicoDadosPessoaisService = historicoDadosPessoaisService;
        this.configuracaoGraficoDashboardService = configuracaoGraficoDashboardService;
        this.limitePlanoService = limitePlanoService;
    }

    @Transactional(readOnly = true)
    public Optional<AdminOrganizacao> buscarrAdminByOrg(Long id) {
        log.debug("Buscando administrador por ID: {}", id);
        Long orgId = tenantHelper.getCurrentTenantIdOrNull();
        if (orgId == null) {
            log.debug("SUPER_ADMIN: buscando administrador por ID sem filtro de organização");
            return adminOrganizacaoRepository.findById(id);
        }
        return adminOrganizacaoRepository.findByIdAndOrganizacao_Id(id, orgId);
    }

    @Transactional
    public AdminOrganizacao cadastrarAdminByOrg(CadastrarAdminRequest request, Long organizacaoId) {
        log.info("Cadastrando administrador: {} para organização ID: {}", request.nome(), organizacaoId);

        limitePlanoService.validarLimiteAdminOrg(organizacaoId);

        if (usuarioRepository.existsByLogin(request.cpf())) {
            log.warn("CPF já cadastrado como login: {}", request.cpf());
            throw new IllegalStateException("CPF já cadastrado no sistema");
        }

        // Verificar se o email já existe em qualquer tabela
        if (emailUnicoService.emailJaExiste(request.email())) {
            String tabela = emailUnicoService.ondeEmailFoiEncontrado(request.email());
            throw new IllegalStateException("Email já cadastrado no sistema como " + tabela);
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

        emailNotificacaoService.enviarCredenciaisAdministrador(
                request.email(),
                request.nome(),
                cpfLimpo,
                senhaGerada,
                organizacaoId
        );

        return admin;
    }

    @Transactional
    public AdminOrganizacao atualizarAdmByOrg(Long id, String nome, String email) {
        log.info("Atualizando administrador ID: {}", id);
        Long orgId = tenantHelper.getCurrentTenantId();


        AdminOrganizacao antes = adminOrganizacaoRepository.findById(id )
            .orElseThrow(() -> new IllegalArgumentException("Administrador não encontrado"));


        AdminOrganizacao snapshot = SnapshotUtil.copiarSnapshot(antes);

        if (nome != null && !nome.isBlank()) {
            antes.setNome(nome);
        }
        if (email != null && !email.isBlank()) {
            antes.setEmail(email);
        }

        AdminOrganizacao  resultado = adminOrganizacaoRepository.save(antes);
        log.info("Administrador atualizado com sucesso. ID: {}", resultado.getId());

        historicoDadosPessoaisService.registrarAlteracoesDeObjeto(
                EntidadeTipo.PROFISSIONAL,
                resultado.getId(),
                tenantHelper.getCurrentUserId(),
                snapshot,
                resultado
        );
        return antes;
    }

 
    @Transactional
    public void deletarAdmByOrg(Long idAdmin) {
        Long orgId = tenantHelper.getCurrentTenantIdOrNull();
        log.info("Deletando administrador ID: {} da organização: {}", idAdmin, orgId);

        // 1. Buscar o administrador na tabela admin_organizacao
        AdminOrganizacao admin;
        if (orgId == null) {
            log.debug("SUPER_ADMIN: deletando administrador por ID sem filtro de organização");
            admin = adminOrganizacaoRepository.findById(idAdmin)
                    .orElseThrow(() -> new IllegalArgumentException("Administrador não encontrado"));
        } else {
            admin = adminOrganizacaoRepository.findByIdAndOrganizacao_Id(idAdmin, orgId)
                    .orElseThrow(() -> new IllegalArgumentException("Administrador não encontrado"));
        }

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

 
    /**
     * Busca dados completos do AdminOrg (admin + organização + endereço).
     * Exclusivo para SUPER_ADMIN.
     *
     * @param id ID do AdminOrganizacao
     * @return AdminOrgCompletoResponse com dados completos
     */
    @Transactional(readOnly = true)
    public AdminOrgCompletoResponse buscarAdminOrgCompleto(Long id) {
        log.debug("SUPER_ADMIN: buscando AdminOrg completo por ID: {}", id);
        AdminOrganizacao admin = adminOrganizacaoRepository.findByIdWithOrgAndEndereco(id)
                .orElseThrow(() -> new IllegalArgumentException("Administrador não encontrado"));
        return AdminOrgCompletoResponse.fromEntity(admin);
    }

    /**
     * Atualiza dados completos do AdminOrg (admin + organização + endereço).
     * Exclusivo para SUPER_ADMIN.
     *
     * @param id      ID do AdminOrganizacao
     * @param request dados atualizados
     * @return AdminOrgCompletoResponse com dados atualizados
     */
    @Transactional
    public AdminOrgCompletoResponse atualizarAdminOrgCompleto(Long id, AtualizarAdminOrgCompletoRequest request) {
        log.info("SUPER_ADMIN: atualizando AdminOrg completo ID: {}", id);

        AdminOrganizacao admin = adminOrganizacaoRepository.findByIdWithOrgAndEndereco(id)
                .orElseThrow(() -> new IllegalArgumentException("Administrador não encontrado"));

        // Atualiza dados do admin
        if (request.nome() != null && !request.nome().isBlank()) {
            admin.setNome(request.nome());
        }
        if (request.cargo() != null && !request.cargo().isBlank()) {
            admin.setCargo(request.cargo());
        }
        if (request.email() != null && !request.email().isBlank()) {
            admin.setEmail(request.email());
        }

        // Atualiza dados da organização
        Organizacao org = admin.getOrganizacao();
        if (org != null) {
            if (request.nomeClinica() != null && !request.nomeClinica().isBlank()) {
                org.setNome(request.nomeClinica());
            }
            if (request.razaoSocial() != null) {
                org.setRazaoSocial(request.razaoSocial());
            }
            if (request.cnpj() != null && !request.cnpj().isBlank()) {
                org.setCnpj(limparCnpj(request.cnpj()));
            }
            if (request.tipoClinica() != null && !request.tipoClinica().isBlank()) {
                org.setTipo(TipoOrganizacao.valueOf(request.tipoClinica()));
            }
            if (request.emailClinica() != null) {
                org.setEmail(request.emailClinica());
            }
            if (request.telefone() != null) {
                org.setTelefone(request.telefone());
            }
            organizacaoRepository.save(org);

            // Atualiza endereço
            Endereco end = org.getEndereco();
            if (end != null) {
                if (request.cep() != null) end.setEndCep(request.cep());
                if (request.uf() != null) end.setEndUF(request.uf());
                if (request.municipio() != null) end.setEndMunicipio(request.municipio());
                if (request.bairro() != null) end.setEndBairro(request.bairro());
                if (request.rua() != null) end.setEndRua(request.rua());
                if (request.numero() != null) end.setEndNumero(request.numero());
                if (request.complemento() != null) end.setEndComplemento(request.complemento());
                enderecoRepository.save(end);
            }
        }

        AdminOrganizacao salvo = adminOrganizacaoRepository.save(admin);
        log.info("SUPER_ADMIN: AdminOrg ID {} atualizado com sucesso", id);
        return AdminOrgCompletoResponse.fromEntity(salvo);
    }

    /**
     * Cadastra um Admin de Organização completo pelo SUPER_ADMIN.
     * Cria: Endereco → Organizacao → Usuario (login e senha = CNPJ) → AdminOrganizacao.
     * Email de credenciais é enviado para o email da clínica.
     *
     * @param request dados completos do admin e da organização
     * @return AdminOrganizacao criado
     */
    @Transactional
    public AdminOrganizacao cadastrarAdminOrgCompleto(CadastrarAdminOrgCompletoRequest request) {
        String cnpjLimpo = limparCnpj(request.cnpj());
        log.info("Cadastrando Admin Org completo. CNPJ: {}, Clínica: {}", cnpjLimpo, request.nomeClinica());

        if (usuarioRepository.existsByLogin(cnpjLimpo)) {
            throw new IllegalStateException("CNPJ já cadastrado como login no sistema");
        }

        if (organizacaoRepository.existsByCnpj(cnpjLimpo)) {
            throw new IllegalStateException("CNPJ já cadastrado no sistema");
        }

        Endereco endereco = enderecoRepository.save(Endereco.builder()
                .endCep(request.cep())
                .endUF(request.uf())
                .endMunicipio(request.municipio())
                .endBairro(request.bairro())
                .endRua(request.rua())
                .endNumero(request.numero())
                .endComplemento(request.complemento())
                .build());

        Organizacao organizacao = organizacaoRepository.save(Organizacao.builder()
                .nome(request.nomeClinica())
                .razaoSocial(request.razaoSocial())
                .cnpj(cnpjLimpo)
                .tipo(TipoOrganizacao.valueOf(request.tipoClinica()))
                .email(request.emailClinica())
                .telefone(request.telefone())
                .endereco(endereco)
                .status(StatusOrganizacao.ATIVO)
                .build());


        String senhaGerada = gerarSenhaAleatoria();

        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .login(cnpjLimpo)
                .senha(passwordEncoder.encode(senhaGerada))
                .tipoUsuario((byte) 1)
                .tipoUsuarioNovo(TipoUsuarioNovo.ADMIN_ORG)
                .status(StatusUsuario.ATIVO)
                .organizacao(organizacao)
                .build());

        AdminOrganizacao admin = adminOrganizacaoRepository.save(AdminOrganizacao.builder()
                .organizacao(organizacao)
                .usuario(usuario)
                .nome(request.nome())
                .email(request.email())
                .cargo(request.cargo())
                .isOwner(true)
                .status(AdminOrganizacao.StatusAdmin.ATIVO)
                .build());

        configuracaoGraficoDashboardService.inicializarParaNovoUsuario(usuario);

        emailNotificacaoService.enviarCredenciaisAdministrador(
                request.emailClinica(),
                request.nome(),
                cnpjLimpo,
                senhaGerada,
                organizacao.getId()
        );

        log.info("Admin Org criado com sucesso. ID: {}, Org: {}", admin.getId(), organizacao.getId());
        return admin;
    }





    private String limparCnpj(String cnpj) {
        return cnpj.replaceAll("[^0-9]", "");
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
