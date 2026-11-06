package br.com.saudeConecta.application.organizacao;

import br.com.saudeConecta.application.assinatura.AssinaturaTenantService;
import br.com.saudeConecta.application.assinatura.LimitePlanoService;
import br.com.saudeConecta.application.cache.CacheEvictionService;
import br.com.saudeConecta.application.dashboard.ConfiguracaoGraficoDashboardService;
import br.com.saudeConecta.application.historico.HistoricoDadosPessoaisService;
import br.com.saudeConecta.application.historico.SnapshotUtil;
import br.com.saudeConecta.application.notificacao.EmailNotificacaoService;
import br.com.saudeConecta.application.organizacao.dto.AdminOrgCompletoResponse;
import br.com.saudeConecta.application.organizacao.dto.AtualizarAdminOrgCompletoRequest;
import br.com.saudeConecta.application.organizacao.dto.CadastrarAdminOrgCompletoRequest;
import br.com.saudeConecta.application.organizacao.dto.CadastrarAdminRequest;
import br.com.saudeConecta.application.organizacao.dto.CadastroSaasRequest;
import br.com.saudeConecta.application.usuario.EmailUnicoService;
import br.com.saudeConecta.config.logging.MascaraLog;
import br.com.saudeConecta.config.tenant.TenantAccessDeniedException;
import br.com.saudeConecta.config.tenant.TenantHelper;
import br.com.saudeConecta.domain.admin.AdminOrganizacao;
import br.com.saudeConecta.domain.endereco.Endereco;
import br.com.saudeConecta.domain.historicodadospessoais.EntidadeTipo;
import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.organizacao.StatusOrganizacao;
import br.com.saudeConecta.domain.organizacao.TipoOrganizacao;
import br.com.saudeConecta.domain.usuario.StatusUsuario;
import br.com.saudeConecta.domain.usuario.TipoUsuarioNovo;
import br.com.saudeConecta.domain.usuario.Usuario;
import br.com.saudeConecta.infrastructure.persistence.repository.AdminOrganizacaoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.EnderecoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.OrganizacaoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Optional;

@Service
@Slf4j
public class AdminOrganizacaoService {
    private static final String MSG_ADMIN_NAO_ENCONTRADO = "Administrador não encontrado";

    /** Um gerador só por classe: criar um SecureRandom por senha gasta entropia à toa (S2119). */
    private static final SecureRandom GERADOR_SENHA = new SecureRandom();
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
    private final AssinaturaTenantService assinaturaTenantService;
    private final CacheEvictionService cacheEvictionService;

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
            LimitePlanoService limitePlanoService,
            AssinaturaTenantService assinaturaTenantService,
            CacheEvictionService cacheEvictionService) {
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
        this.assinaturaTenantService = assinaturaTenantService;
        this.cacheEvictionService = cacheEvictionService;
    }

    @Transactional(readOnly = true)
    public Optional<AdminOrganizacao> buscarrAdminByOrg(Long id) {
        log.debug("adminOrg.buscar adminId={}", id);
        Long orgId = tenantHelper.getCurrentTenantIdOrNullIfSuperAdmin();
        if (orgId == null) {
            log.debug("adminOrg.buscar escopo=SUPER_ADMIN adminId={}", id);
            return adminOrganizacaoRepository.findById(id);
        }
        return adminOrganizacaoRepository.findByIdAndOrganizacao_Id(id, orgId);
    }

    @Transactional
    public AdminOrganizacao cadastrarAdminByOrg(CadastrarAdminRequest request, Long organizacaoId) {
        log.info("adminOrg.cadastrar inicio organizacaoId={}", organizacaoId);

        limitePlanoService.validarLimiteAdminOrg(organizacaoId);

        // O login gravado é o CPF limpo: checar o bruto (com máscara) deixava passar duplicado
        String cpfLimpo = limparCpf(request.cpf());
        if (usuarioRepository.existsByLogin(cpfLimpo)) {
            log.warn("adminOrg.cadastrar negado motivo=CPF_JA_CADASTRADO organizacaoId={}", organizacaoId);
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

        String senhaGerada = gerarSenhaAleatoria();

        Usuario usuario = Usuario.builder()
                .login(cpfLimpo)
                .senha(passwordEncoder.encode(senhaGerada))
                .tipoUsuarioNovo(TipoUsuarioNovo.GESTOR)
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
        log.debug("adminOrg.cadastrar usuario criado usuarioId={}", usuario.getId());
        admin = adminOrganizacaoRepository.save(admin);
        log.info("adminOrg.cadastrar ok adminId={}", admin.getId());

        cacheEvictionService.evictPerfilEUsuariosAgrupados(usuario.getId(), organizacaoId);

        emailNotificacaoService.enviarCredenciaisAdministrador(
                request.email(),
                request.nome(),
                cpfLimpo,
                senhaGerada,
                organizacaoId,
                admin.getId()
        );

        return admin;
    }

    /**
     * Atualiza os dados pessoais do Admin Org logado + dados da organização + endereço.
     * Usado na tela "Meus Dados" pelo próprio Admin Org.
     *
     * @param adminId ID do AdminOrganizacao
     * @param request dados atualizados (admin + org + endereço)
     */
    @Transactional
    public void atualizarMeusDadosAdminOrg(Long adminId, AtualizarAdminOrgCompletoRequest request) {
        Long orgId = tenantHelper.getCurrentTenantId();
        Long userId = tenantHelper.getCurrentUserId();
        log.info("adminOrg.atualizarMeusDados inicio adminId={} organizacaoId={}", adminId, orgId);

        AdminOrganizacao admin = adminOrganizacaoRepository.findByIdWithOrgAndEndereco(adminId)
                .orElseThrow(() -> new IllegalArgumentException(MSG_ADMIN_NAO_ENCONTRADO));

        // Validação de tenant
        if (!admin.getOrganizacao().getId().equals(orgId)) {
            throw new IllegalArgumentException("Acesso negado: administrador não pertence à organização");
        }
        // "Meus dados": só o dono do cadastro. Outro gestor da clínica usa atualizarAdmByOrg.
        if (admin.getUsuario() == null || !admin.getUsuario().getId().equals(userId)) {
            log.warn("adminOrg.atualizarMeusDados negado motivo=OUTRO_ADMIN usuarioId={} adminId={}", userId, adminId);
            throw new TenantAccessDeniedException("Acesso negado: só é possível alterar os próprios dados");
        }

        emailUnicoService.validarEmailLivreParaAtualizacao(request.email(), admin.getEmail(),
                admin.getUsuario() != null ? admin.getUsuario().getId() : null);

        // Snapshot para histórico
        AdminOrganizacao snapshot = SnapshotUtil.copiarSnapshot(admin);

        // Atualiza dados do admin
        admin.setNome(request.nome());
        admin.setCargo(request.cargo());
        admin.setEmail(request.email());

        AdminOrganizacao resultado = adminOrganizacaoRepository.save(admin);

        // Registra histórico do admin
        historicoDadosPessoaisService.registrarAlteracoesDeObjeto(
                EntidadeTipo.ADMIN, resultado.getId(), userId, snapshot, resultado);

        cacheEvictionService.evictPerfilEUsuariosAgrupados(userId, orgId);

        // Atualiza dados da organização
        Organizacao org = admin.getOrganizacao();
        if (org != null) {
            Organizacao orgSnapshot = SnapshotUtil.copiarSnapshot(org);

            org.setNome(request.nomeClinica());
            org.setRazaoSocial(request.razaoSocial());
            org.setTipo(TipoOrganizacao.valueOf(request.tipoClinica()));
            org.setEmail(request.emailClinica());
            org.setTelefone(request.telefone());

            Organizacao orgAtualizada = organizacaoRepository.save(org);
            historicoDadosPessoaisService.registrarAlteracoesDeObjeto(
                    EntidadeTipo.ORGANIZACAO, orgAtualizada.getId(), userId, orgSnapshot, orgAtualizada);

            // Atualiza endereço da organização
            Endereco end = org.getEndereco();
            if (end != null) {
                Endereco endSnapshot = SnapshotUtil.copiarSnapshot(end);

                end.setEndCep(request.cep());
                end.setEndUF(request.uf());
                end.setEndMunicipio(request.municipio());
                end.setEndBairro(request.bairro());
                end.setEndRua(request.rua());
                end.setEndNumero(request.numero());
                end.setEndComplemento(request.complemento());

                Endereco endAtualizado = enderecoRepository.save(end);
                historicoDadosPessoaisService.registrarAlteracoesDeObjeto(
                        EntidadeTipo.ENDERECO, endAtualizado.getEndCodigo(), userId, endSnapshot, endAtualizado);
            }
        }

        log.info("adminOrg.atualizarMeusDados ok adminId={}", adminId);
    }

    /**
     * Atualiza nome e e-mail de um administrador da organização do usuário logado.
     *
     * @param id    ID do AdminOrganizacao
     * @param nome  novo nome (ignorado se nulo ou em branco)
     * @param email novo e-mail (ignorado se nulo ou em branco)
     * @return administrador atualizado
     * @throws IllegalArgumentException se o administrador não existir na organização do token
     */
    @Transactional
    public AdminOrganizacao atualizarAdmByOrg(Long id, String nome, String email) {
        log.info("adminOrg.atualizar inicio adminId={}", id);
        Long orgId = tenantHelper.getCurrentTenantId();

        // Busca restrita ao tenant do token: trocar o e-mail de um gestor de outra clínica
        // permitiria tomar a conta via "esqueci minha senha". Admin de outra org responde
        // igual a inexistente para não revelar quais IDs existem.
        AdminOrganizacao antes = adminOrganizacaoRepository.findByIdAndOrganizacao_Id(id, orgId)
            .orElseThrow(() -> new IllegalArgumentException(MSG_ADMIN_NAO_ENCONTRADO));

        // E-mail duplicado desvia o "esqueci minha senha" (que busca por e-mail) para a conta errada
        emailUnicoService.validarEmailLivreParaAtualizacao(email, antes.getEmail(),
                antes.getUsuario() != null ? antes.getUsuario().getId() : null);

        AdminOrganizacao snapshot = SnapshotUtil.copiarSnapshot(antes);

        if (nome != null && !nome.isBlank()) {
            antes.setNome(nome);
        }
        if (email != null && !email.isBlank()) {
            antes.setEmail(email);
        }

        AdminOrganizacao  resultado = adminOrganizacaoRepository.save(antes);
        log.info("adminOrg.atualizar ok adminId={}", resultado.getId());

        historicoDadosPessoaisService.registrarAlteracoesDeObjeto(
                EntidadeTipo.ADMIN,
                resultado.getId(),
                tenantHelper.getCurrentUserId(),
                snapshot,
                resultado
        );

        cacheEvictionService.evictPerfilEUsuariosAgrupados(antes.getUsuario().getId(), orgId);

        return antes;
    }

 
    @Transactional
    public void deletarAdmByOrg(Long idAdmin) {
        Long orgId = tenantHelper.getCurrentTenantIdOrNullIfSuperAdmin();
        log.info("adminOrg.deletar inicio adminId={} organizacaoId={}", idAdmin, orgId);

        // 1. Buscar o administrador na tabela admin_organizacao
        AdminOrganizacao admin;
        if (orgId == null) {
            log.debug("adminOrg.deletar escopo=ROOT adminId={}", idAdmin);
            admin = adminOrganizacaoRepository.findById(idAdmin)
                    .orElseThrow(() -> new IllegalArgumentException(MSG_ADMIN_NAO_ENCONTRADO));
        } else {
            admin = adminOrganizacaoRepository.findByIdAndOrganizacao_Id(idAdmin, orgId)
                    .orElseThrow(() -> new IllegalArgumentException("Administrador nao encontrado"));
        }

        Usuario usuario = admin.getUsuario();
        if (usuario == null) {
            throw new IllegalStateException("Administrador nao possui usuario associado");
        }

        try {
            Long usuarioId = usuario.getId();
            Long orgIdAfetada = admin.getOrganizacao() != null ? admin.getOrganizacao().getId() : orgId;

            // 2. Deletar o registro de administrador na tabela admin_organizacao primeiro
            adminOrganizacaoRepository.delete(admin);
            
            // 3. Deletar o usuário da tabela usuario
            usuarioRepository.delete(usuario);

            cacheEvictionService.evictPerfilEUsuariosAgrupados(usuarioId, orgIdAfetada);

            log.info("adminOrg.deletar ok adminId={} usuarioId={}",
                    idAdmin, usuario.getId());
                    
        } catch (Exception e) {
            // 4. Caso haja relacionamento que impeça o delete, cancelar e avisar
            // Mensagem do banco (tabela/constraint) fica só no log; a causa segue encadeada
            String errorMessage = "Não foi possivel deletar o administrador devido a relacionamentos existentes";
            log.warn("adminOrg.deletar negado motivo=RELACIONAMENTOS_EXISTENTES adminId={}", idAdmin, e);
            throw new IllegalStateException(errorMessage, e);
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
        log.debug("adminOrg.buscar escopo=SUPER_ADMIN adminId={}", id);
        AdminOrganizacao admin = adminOrganizacaoRepository.findByIdWithOrgAndEndereco(id)
                .orElseThrow(() -> new IllegalArgumentException(MSG_ADMIN_NAO_ENCONTRADO));
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
        log.info("adminOrg.atualizar inicio escopo=SUPER_ADMIN adminId={}", id);

        AdminOrganizacao admin = adminOrganizacaoRepository.findByIdWithOrgAndEndereco(id)
                .orElseThrow(() -> new IllegalArgumentException(MSG_ADMIN_NAO_ENCONTRADO));

        // Atualiza dados do admin
        atualizarDadosAdmin(admin, request);

        // Atualiza dados da organização
        Organizacao org = admin.getOrganizacao();
        if (org != null) {
            atualizarDadosOrganizacao(org, request);
            organizacaoRepository.save(org);

            // Atualiza endereço
            Endereco end = org.getEndereco();
            if (end != null) {
                atualizarDadosEndereco(end, request);
                enderecoRepository.save(end);
            }
        }

        AdminOrganizacao salvo = adminOrganizacaoRepository.save(admin);
        log.info("adminOrg.atualizar ok escopo=SUPER_ADMIN adminId={}", id);

        Long orgIdAfetada = salvo.getOrganizacao() != null ? salvo.getOrganizacao().getId() : null;
        cacheEvictionService.evictPerfilEUsuariosAgrupados(salvo.getUsuario().getId(), orgIdAfetada);

        return AdminOrgCompletoResponse.fromEntity(salvo);
    }

    /** Atualização parcial do admin: nome, cargo e e-mail em branco ou nulos são ignorados. */
    private void atualizarDadosAdmin(AdminOrganizacao admin, AtualizarAdminOrgCompletoRequest request) {
        if (!isBlank(request.nome())) {
            admin.setNome(request.nome());
        }
        if (!isBlank(request.cargo())) {
            admin.setCargo(request.cargo());
        }
        if (!isBlank(request.email())) {
            admin.setEmail(request.email());
        }
    }

    /**
     * Atualização parcial da organização. Nome, CNPJ e tipo ignoram nulo e branco;
     * razão social, e-mail e telefone ignoram só nulo (texto vazio é aceito).
     */
    private void atualizarDadosOrganizacao(Organizacao org, AtualizarAdminOrgCompletoRequest request) {
        if (!isBlank(request.nomeClinica())) {
            org.setNome(request.nomeClinica());
        }
        if (request.razaoSocial() != null) {
            org.setRazaoSocial(request.razaoSocial());
        }
        if (!isBlank(request.cnpj())) {
            org.setCnpj(limparCnpj(request.cnpj()));
        }
        if (!isBlank(request.tipoClinica())) {
            org.setTipo(TipoOrganizacao.valueOf(request.tipoClinica()));
        }
        if (request.emailClinica() != null) {
            org.setEmail(request.emailClinica());
        }
        if (request.telefone() != null) {
            org.setTelefone(request.telefone());
        }
    }

    /** Atualização parcial do endereço: só os campos informados (não nulos) são alterados. */
    private void atualizarDadosEndereco(Endereco end, AtualizarAdminOrgCompletoRequest request) {
        if (request.cep() != null) end.setEndCep(request.cep());
        if (request.uf() != null) end.setEndUF(request.uf());
        if (request.municipio() != null) end.setEndMunicipio(request.municipio());
        if (request.bairro() != null) end.setEndBairro(request.bairro());
        if (request.rua() != null) end.setEndRua(request.rua());
        if (request.numero() != null) end.setEndNumero(request.numero());
        if (request.complemento() != null) end.setEndComplemento(request.complemento());
    }

    private static final String TIPO_PESSOA_JURIDICA = "JURIDICA";
    private static final String TIPO_PESSOA_FISICA = "FISICA";
    private static final String TELEFONE_PADRAO = "11999999999";

    /**
     * Cadastra um Admin de Organização completo pelo SUPER_ADMIN.
     * Cria: Endereco → Organizacao → Usuario → AdminOrganizacao → Assinatura.
     * Suporta Pessoa Jurídica (login = CNPJ) e Pessoa Física (login = CPF).
     *
     * @param request dados completos do admin e da organização
     * @return AdminOrganizacao criado
     */
    @Transactional
    public AdminOrganizacao cadastrarAdminOrgCompleto(CadastrarAdminOrgCompletoRequest request) {
        return criarTenantCompleto(request, request.isCortesia());
    }

    /**
     * Cadastra um novo tenant a partir do auto-cadastro público da landing page.
     * Usa exatamente as mesmas validações do cadastro feito pelo SUPER_ADMIN,
     * porém nunca permite criar assinatura de cortesia.
     *
     * @param request dados informados pelo visitante
     * @return AdminOrganizacao criado
     */
    @Transactional
    public AdminOrganizacao cadastrarTenantPublico(CadastroSaasRequest request) {
        log.info("adminOrg.autocadastro inicio tipoPessoa={} planoId={}",
                request.tipoPessoa(), request.planoId());
        return criarTenantCompleto(request.toAdminOrgRequest(), false);
    }

    /**
     * Fluxo compartilhado de criação de tenant:
     * Endereco → Organizacao → Usuario → AdminOrganizacao → Assinatura → e-mail de credenciais.
     *
     * @param request  dados completos do admin e da organização
     * @param cortesia true para criar a assinatura sem cobrança
     * @return AdminOrganizacao criado
     */
    private AdminOrganizacao criarTenantCompleto(CadastrarAdminOrgCompletoRequest request, boolean cortesia) {
        String tipoPessoa = request.tipoPessoa();
        log.info("adminOrg.cadastrar inicio tipoPessoa={} planoId={}", tipoPessoa, request.planoId());

        validarTipoPessoa(tipoPessoa);
        boolean isJuridica = TIPO_PESSOA_JURIDICA.equalsIgnoreCase(tipoPessoa);

        if (isJuridica) {
            validarCamposObrigatoriosJuridica(request);
        }

        String login = determinarLogin(request, isJuridica);
        validarUnicidadeLogin(login, isJuridica);

        if (isJuridica) {
            validarUnicidadeCnpj(request.cnpj());
        }

        String senhaGerada = gerarSenhaAleatoria();

        Endereco endereco = criarEndereco(request);
        Organizacao organizacao = criarOrganizacao(request, isJuridica, endereco);
        Usuario usuario = criarUsuario(login, organizacao, senhaGerada);
        AdminOrganizacao admin = criarAdminOrganizacao(request, organizacao, usuario);

        assinaturaTenantService.assinar(organizacao.getId(), request.planoId(), cortesia);
        log.info("adminOrg.cadastrar plano organizacaoId={} planoId={} cortesia={}",
                organizacao.getId(), request.planoId(), cortesia);

        configuracaoGraficoDashboardService.inicializarParaNovoUsuario(usuario);

        cacheEvictionService.evictPerfilEUsuariosAgrupados(usuario.getId(), organizacao.getId());

        String emailDestino = isJuridica ? request.emailClinica() : request.email();
        emailNotificacaoService.enviarCredenciaisAdministrador(
                emailDestino,
                request.nome(),
                login,
                senhaGerada,
                organizacao.getId(),
                admin.getId()
        );

        log.info("adminOrg.cadastrar ok adminId={} organizacaoId={} login={}", admin.getId(), organizacao.getId(), MascaraLog.cpf(login));
        return admin;
    }

    private void validarTipoPessoa(String tipoPessoa) {
        if (!TIPO_PESSOA_JURIDICA.equalsIgnoreCase(tipoPessoa) && !TIPO_PESSOA_FISICA.equalsIgnoreCase(tipoPessoa)) {
            throw new IllegalArgumentException("Tipo de pessoa inválido. Use FISICA ou JURIDICA");
        }
    }

    private void validarCamposObrigatoriosJuridica(CadastrarAdminOrgCompletoRequest request) {
        if (isBlank(request.cnpj())) {
            throw new IllegalArgumentException("CNPJ é obrigatório para pessoa jurídica");
        }
        if (isBlank(request.nomeClinica())) {
            throw new IllegalArgumentException("Nome da clínica é obrigatório para pessoa jurídica");
        }
        if (isBlank(request.razaoSocial())) {
            throw new IllegalArgumentException("Razão social é obrigatória para pessoa jurídica");
        }
        if (isBlank(request.tipoClinica())) {
            throw new IllegalArgumentException("Tipo da clínica é obrigatório para pessoa jurídica");
        }
        if (isBlank(request.emailClinica())) {
            throw new IllegalArgumentException("Email da clínica é obrigatório para pessoa jurídica");
        }
        if (isBlank(request.telefone())) {
            throw new IllegalArgumentException("Telefone é obrigatório para pessoa jurídica");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String determinarLogin(CadastrarAdminOrgCompletoRequest request, boolean isJuridica) {
        return isJuridica ? limparCnpj(request.cnpj()) : limparCpf(request.cpf());
    }

    private void validarUnicidadeLogin(String login, boolean isJuridica) {
        if (usuarioRepository.existsByLogin(login)) {
            String documento = isJuridica ? "CNPJ" : "CPF";
            throw new IllegalStateException(documento + " já cadastrado como login no sistema");
        }
    }

    private void validarUnicidadeCnpj(String cnpj) {
        String cnpjLimpo = limparCnpj(cnpj);
        if (organizacaoRepository.existsByCnpj(cnpjLimpo)) {
            throw new IllegalStateException("CNPJ já cadastrado no sistema");
        }
    }

    private Endereco criarEndereco(CadastrarAdminOrgCompletoRequest request) {
        return enderecoRepository.save(Endereco.builder()
                .endCep(request.cep())
                .endUF(request.uf())
                .endMunicipio(request.municipio())
                .endBairro(request.bairro())
                .endRua(request.rua())
                .endNumero(request.numero())
                .endComplemento(request.complemento())
                .build());
    }

    private Organizacao criarOrganizacao(CadastrarAdminOrgCompletoRequest request, boolean isJuridica, Endereco endereco) {
        if (isJuridica) {
            return criarOrganizacaoJuridica(request, endereco);
        } else {
            return criarOrganizacaoFisica(request, endereco);
        }
    }

    private Organizacao criarOrganizacaoJuridica(CadastrarAdminOrgCompletoRequest request, Endereco endereco) {
        String cnpjLimpo = limparCnpj(request.cnpj());
        return organizacaoRepository.save(Organizacao.builder()
                .nome(request.nomeClinica())
                .razaoSocial(request.razaoSocial())
                .cnpj(cnpjLimpo)
                .tipo(TipoOrganizacao.valueOf(request.tipoClinica()))
                .email(request.emailClinica())
                .telefone(request.telefone())
                .endereco(endereco)
                .status(StatusOrganizacao.ATIVO)
                .build());
    }

    private Organizacao criarOrganizacaoFisica(CadastrarAdminOrgCompletoRequest request, Endereco endereco) {
        return organizacaoRepository.save(Organizacao.builder()
                .nome(request.nome() + " - Consultório")
                .razaoSocial(request.nome())
                .cnpj(null)
                .tipo(TipoOrganizacao.CONSULTORIO)
                .email(request.email())
                .telefone(request.telefone() != null ? request.telefone() : TELEFONE_PADRAO)
                .endereco(endereco)
                .status(StatusOrganizacao.ATIVO)
                .build());
    }

    private Usuario criarUsuario(String login, Organizacao organizacao, String senhaGerada) {
        return usuarioRepository.save(Usuario.builder()
                .login(login)
                .senha(passwordEncoder.encode(senhaGerada))
                .tipoUsuarioNovo(TipoUsuarioNovo.GESTOR)
                .status(StatusUsuario.ATIVO)
                .organizacao(organizacao)
                .build());
    }

    private AdminOrganizacao criarAdminOrganizacao(CadastrarAdminOrgCompletoRequest request, Organizacao organizacao, Usuario usuario) {
        return adminOrganizacaoRepository.save(AdminOrganizacao.builder()
                .organizacao(organizacao)
                .usuario(usuario)
                .nome(request.nome())
                .email(request.email())
                .cargo(request.cargo())
                .isOwner(true)
                .status(AdminOrganizacao.StatusAdmin.ATIVO)
                .build());
    }

    private String limparCnpj(String cnpj) {
        return cnpj.replaceAll("\\D", "");
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
        return cpf.replaceAll("\\D", "");
    }
}
