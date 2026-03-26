package br.com.saudeConecta.service;

import br.com.saudeConecta.domain.common.Sexo;
import br.com.saudeConecta.domain.endereco.Endereco;
import br.com.saudeConecta.domain.historicodadospessoais.EntidadeTipo;
import br.com.saudeConecta.domain.mensageria.Mensageria;
import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.profissional.Especialidade;
import br.com.saudeConecta.domain.profissional.Profissional;
import br.com.saudeConecta.domain.profissional.StatusProfissional;
import br.com.saudeConecta.domain.profissional.TipoProfissional;
import br.com.saudeConecta.domain.usuario.StatusUsuario;
import br.com.saudeConecta.domain.usuario.TipoUsuarioNovo;
import br.com.saudeConecta.domain.usuario.Usuario;
import br.com.saudeConecta.email.EmailNotificacaoService;
import br.com.saudeConecta.infra.tenant.RequiresTenant;
import br.com.saudeConecta.infra.tenant.TenantHelper;
import br.com.saudeConecta.infrastructure.persistence.repository.*;
import br.com.saudeConecta.presentation.dto.profissional.AtualizarClinicoRequest;
import br.com.saudeConecta.presentation.dto.profissional.CadastrarClinicoRequest;
import br.com.saudeConecta.util.EmailUnicoService;
import br.com.saudeConecta.util.SnapshotUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Slf4j
public class ProfissionalService {
    
    private final ProfissionalRepository profissionalRepository;
    private final TipoProfissionalRepository tipoProfissionalRepository;
    private final EspecialidadeRepository especialidadeRepository;
    private final OrganizacaoRepository organizacaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final EnderecoRepository enderecoRepository;
    private final PasswordEncoder passwordEncoder;
    private final TenantHelper tenantHelper;
    private final EmailNotificacaoService emailNotificacaoService;
    private final EmailUnicoService emailUnicoService;
    private final HistoricoDadosPessoaisService historicoDadosPessoaisService;
    private final LimitePlanoService limitePlanoService;
    private final MensageriaRepository mensageriaRepository;


    private static final String CARACTERES_SENHA = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#$%&*";

    public ProfissionalService(
            ProfissionalRepository profissionalRepository,
            TipoProfissionalRepository tipoProfissionalRepository,
            EspecialidadeRepository especialidadeRepository,
            OrganizacaoRepository organizacaoRepository,
            UsuarioRepository usuarioRepository,
            EnderecoRepository enderecoRepository,
            PasswordEncoder passwordEncoder,
            TenantHelper tenantHelper,
            EmailNotificacaoService emailNotificacaoService,
            EmailUnicoService emailUnicoService,
            HistoricoDadosPessoaisService historicoDadosPessoaisService,
            LimitePlanoService limitePlanoService,
            MensageriaRepository mensageriaRepository
    ) {
        this.profissionalRepository = profissionalRepository;
        this.tipoProfissionalRepository = tipoProfissionalRepository;
        this.especialidadeRepository = especialidadeRepository;
        this.organizacaoRepository = organizacaoRepository;
        this.usuarioRepository = usuarioRepository;
        this.enderecoRepository = enderecoRepository;
        this.passwordEncoder = passwordEncoder;
        this.tenantHelper = tenantHelper;
        this.emailNotificacaoService = emailNotificacaoService;
        this.emailUnicoService = emailUnicoService;
        this.historicoDadosPessoaisService = historicoDadosPessoaisService;
        this.limitePlanoService = limitePlanoService;
        this.mensageriaRepository = mensageriaRepository;
    }




    @CacheEvict(value = "profissionais-org", allEntries = true)
    @RequiresTenant
    @Transactional
    public Profissional cadastraClinicoByOrg(CadastrarClinicoRequest request) {
        Long orgId = tenantHelper.getCurrentTenantId();
        log.info("Cadastrando clinico: {} na organizacao: {}", request.nome(), orgId);

        limitePlanoService.validarLimiteProfissional(orgId);

        String cpfLimpo = limparCpf(request.cpf());

        if (usuarioRepository.existsByLogin(request.cpf())) {
            throw new IllegalStateException("CPF ja cadastrado no sistema");
        }

        // Verificar se o email ja existe em qualquer tabela
        if (emailUnicoService.emailJaExiste(request.email())) {
            String tabela = emailUnicoService.ondeEmailFoiEncontrado(request.email());
            throw new IllegalStateException("Email ja cadastrado no sistema como " + tabela);
        }

        Organizacao organizacao = organizacaoRepository.findById(orgId)
            .orElseThrow(() -> new IllegalStateException("Organização nao encontrada"));

        // Determina o tipo profissional com base no request
        String tipoCodigo = request.tipoProfissional() != null ? request.tipoProfissional() : "MEDICO";
        TipoProfissional tipoProfissional = tipoProfissionalRepository.findByCodigo(tipoCodigo)
            .orElseThrow(() -> new IllegalStateException("Tipo " + tipoCodigo + " nao encontrado"));

        String senhaGerada = gerarSenhaAleatoria();
        String senhaCriptografada = passwordEncoder.encode(senhaGerada);

        Usuario usuario = Usuario.builder()
                .login(cpfLimpo)
                .senha(senhaCriptografada)
                .tipoUsuario((byte) 3)
                .tipoUsuarioNovo(TipoUsuarioNovo.PROFISSIONAL)
                .organizacao(organizacao)
                .status(StatusUsuario.ATIVO)
                .build();


        Endereco endereco = Endereco.builder()
                .endNacionalidade(request.nacionalidade())
                .endUF(request.uf())
                .endMunicipio(request.municipio())
                .endBairro(request.bairro())
                .endCep(request.cep())
                .endRua(request.rua())
                .endNumero(request.numero() != null ? request.numero().longValue() : null)
                .endComplemento(request.complemento())
                .build();

        enderecoRepository.save(endereco);
        Usuario usuarioSalvo = usuarioRepository.save(usuario);

        // Processa especialidades se fornecidas
        Set<Especialidade> especialidades = new HashSet<>();
        if (request.especialidade() != null && !request.especialidade().isEmpty()) {
            // Busca especialidade pelo nome (enviado pelo frontend)
            String nomeEspecialidade = request.especialidade().trim();
            
            // Busca especialidade ativa pelo nome e tipo profissional
            especialidadeRepository.findByTipoProfissional_IdAndNome(tipoProfissional.getId(), nomeEspecialidade)
                .ifPresentOrElse(
                    especialidades::add,
                    () -> {
                        // Se nao encontrar, cria nova especialidade
                        log.info("Criando nova especialidade: {} para tipo: {}", nomeEspecialidade, tipoProfissional.getCodigo());
                        Especialidade novaEspecialidade = Especialidade.builder()
                            .tipoProfissional(tipoProfissional)
                            .nome(nomeEspecialidade)
                            .codigo(nomeEspecialidade.toUpperCase().replace(" ", "_"))
                            .status((byte) 1)
                            .build();
                        especialidadeRepository.save(novaEspecialidade);
                        especialidades.add(novaEspecialidade);
                    }
                );
        }

        Profissional profissional = Profissional.builder()
            .organizacao(organizacao)
            .tipoProfissional(tipoProfissional)
            .nome(request.nome())
            .sexo(converterSexo(request.sexo()))
            .dataNascimento(request.dataNascimento() != null ? java.time.LocalDate.parse(request.dataNascimento()) : null)
            .registroConselho(request.registroConselho())
            .cpf(cpfLimpo)
            .rg(request.rg())
            .email(request.email())
            .telefone(request.telefone())
            .formacao(request.formacao())
            .instituicao(request.instituicao())
            .tempoConsultaMinutos(request.tempoConsultaMinutos() != null ? request.tempoConsultaMinutos() : 30)
            .usuario(usuarioSalvo)
            .endereco(endereco)
            .valorConsulta(new BigDecimal("50.00"))
            .status(StatusProfissional.ATIVO)
            .especialidades(especialidades)
            .build();

        Profissional salvo = profissionalRepository.save(profissional);
        log.info("Clinico cadastrado com sucesso. ID: {}", salvo.getId());

        emailNotificacaoService.enviarCredenciaisClinico(
            request.email(),
            request.nome(),
            cpfLimpo,
            senhaGerada,
            organizacao.getNome(),
            orgId,
            salvo.getId()
        );

        return salvo;
    }


    
    @RequiresTenant
    @Transactional(readOnly = true)
    public Optional<Profissional> buscarClinicoIdByOrg(Long id) {
        Long orgId = tenantHelper.getCurrentTenantId();
        return profissionalRepository.buscarClinicoIdByOrg(id, orgId);
    }

    @CacheEvict(value = "profissionais-org", allEntries = true)
    @RequiresTenant
    @Transactional
    public Profissional atualizarClinicoIdByOrg(Long id, AtualizarClinicoRequest dadosAtualizados) {
        Long orgId = tenantHelper.getCurrentTenantId();

        Profissional antes = profissionalRepository.buscarClinicoIdByOrg(id, orgId)
                .orElseThrow(() -> new IllegalArgumentException("Profissional nao encontrado"));

        Profissional snapshot = SnapshotUtil.copiarSnapshot(antes);

        // Atualiza campos básicos
        antes.setNome(dadosAtualizados.nome());
        antes.setCpf(dadosAtualizados.cpf());
        antes.setRg(dadosAtualizados.rg());
        antes.setRegistroConselho(dadosAtualizados.registroConselho());
        antes.setTelefone(dadosAtualizados.telefone());
        antes.setEmail(dadosAtualizados.email());
        antes.setFormacao(dadosAtualizados.formacao());
        antes.setInstituicao(dadosAtualizados.instituicao());
        antes.setValorConsulta(dadosAtualizados.valorConsulta());
        antes.setTempoConsultaMinutos(dadosAtualizados.tempoConsultaMinutos());
        antes.setDataNascimento(dadosAtualizados.dataNascimento());

        // Atualiza tipo profissional se fornecido
        if (dadosAtualizados.tipoProfissional() != null && !dadosAtualizados.tipoProfissional().isEmpty()) {
            String tipoCodigo = dadosAtualizados.tipoProfissional();
            TipoProfissional tipoProfissional = tipoProfissionalRepository.findByCodigo(tipoCodigo)
                .orElseThrow(() -> new IllegalStateException("Tipo " + tipoCodigo + " nao encontrado"));
            antes.setTipoProfissional(tipoProfissional);
        }

        // Atualiza especialidade se fornecida (recebe nome da especialidade)
        if (dadosAtualizados.especialidade() != null && !dadosAtualizados.especialidade().isEmpty()) {
            String nomeEspecialidade = dadosAtualizados.especialidade().trim();
            TipoProfissional tipoProfissional = antes.getTipoProfissional();
            
            // Busca especialidade pelo nome e tipo profissional
            especialidadeRepository.findByTipoProfissional_IdAndNome(tipoProfissional.getId(), nomeEspecialidade)
                .ifPresentOrElse(
                    especialidade -> {
                        Set<Especialidade> especialidades = new HashSet<>();
                        especialidades.add(especialidade);
                        antes.setEspecialidades(especialidades);
                    },
                    () -> {
                        // Se nao encontrar, cria nova especialidade
                        log.info("Criando nova especialidade: {} para tipo: {}", nomeEspecialidade, tipoProfissional.getCodigo());
                        Especialidade novaEspecialidade = Especialidade.builder()
                            .tipoProfissional(tipoProfissional)
                            .nome(nomeEspecialidade)
                            .codigo(nomeEspecialidade.toUpperCase().replace(" ", "_"))
                            .status((byte) 1)
                            .build();
                        especialidadeRepository.save(novaEspecialidade);
                        
                        Set<Especialidade> especialidades = new HashSet<>();
                        especialidades.add(novaEspecialidade);
                        antes.setEspecialidades(especialidades);
                    }
                );
        }

        // Atualiza endereço se fornecido
        if (dadosAtualizados.endereco() != null) {
            Endereco endereco = antes.getEndereco();

            if (endereco == null) {
                // Cria novo endereço
                endereco = dadosAtualizados.endereco().toEntity();
                endereco = enderecoRepository.save(endereco);
            } else {
                // Atualiza endereço existente
                dadosAtualizados.endereco().updateEntity(endereco);
            }

            antes.setEndereco(endereco);
        }


        Profissional resultado =  profissionalRepository.save(antes);
        log.info("Profissional atualizado: {}", resultado);

        historicoDadosPessoaisService.registrarAlteracoesDeObjeto(
                EntidadeTipo.PROFISSIONAL,
                resultado.getId(),
                tenantHelper.getCurrentUserId(),
                snapshot,
                resultado
        );
return resultado ;
    }



    @CacheEvict(value = "profissionais-org", allEntries = true)
    @RequiresTenant
    @Transactional
    public void deletarClinicoIdByOrg(Long idProfissional) {
        Long orgId = tenantHelper.getCurrentTenantId();
        log.info("Deletando profissional ID: {} da organização: {}", idProfissional, orgId);

        // 1. Buscar o profissional na tabela profissional
        Profissional profissional = profissionalRepository.buscarClinicoIdByOrg(idProfissional, orgId)
                .orElseThrow(() -> new IllegalArgumentException("Profissional nao encontrado"));

        Usuario usuario = profissional.getUsuario();
        if (usuario == null) {
            throw new IllegalStateException("Profissional nao possui usuario associado");
        }

        try {
            // 2. Deletar registros relacionados na tabela mensageria primeiro
            List<Mensageria> mensagens = mensageriaRepository.findByDestinatarioEntidadeId(profissional.getId());
            if (!mensagens.isEmpty()) {
                log.info("Deletando {} registros de mensageria do profissional ID: {}", mensagens.size(), idProfissional);
                mensageriaRepository.deleteAll(mensagens);
            }

            // 3. Deletar o registro de profissional na tabela profissional
            profissionalRepository.delete(profissional);

            // 4. Deletar o usuario da tabela usuario
            usuarioRepository.delete(usuario);
            
            log.info("Profissional e usuario deletados com sucesso. ID Profissional: {}, ID usuario: {}", 
                    idProfissional, usuario.getId());
                    
        } catch (Exception e) {
            // 5. Caso haja relacionamento que impeça o delete, cancelar e avisar
            String errorMessage = "nao foi possível deletar o profissional devido a relacionamentos existentes: " + e.getMessage();
            log.error(errorMessage);
            throw new IllegalStateException(errorMessage);
        }
    }

    private String gerarSenhaAleatoria() {
        SecureRandom random = new SecureRandom();
        StringBuilder senha = new StringBuilder(10);
        for (int i = 0; i < 10; i++) {
            int index = random.nextInt(CARACTERES_SENHA.length());
            senha.append(CARACTERES_SENHA.charAt(index));
        }
        return senha.toString();
    }

    private String limparCpf(String cpf) {
        return cpf != null ? cpf.replaceAll("[^0-9]", "") : null;
    }

    private Sexo converterSexo(String sexo) {
        if (sexo == null || sexo.isEmpty()) {
            return null;
        }
        return switch (sexo) {
            case "1", "MASCULINO" -> Sexo.MASCULINO;
            case "2", "FEMININO" -> Sexo.FEMININO;
            case "3", "OUTRO" -> Sexo.OUTRO;
            default -> null;
        };
    }



    public Long contarAtivosPorOrganizacao(Long organizacaoId) {
        return profissionalRepository.countAtivosByOrganizacaoId(organizacaoId);
    }

    /**
     * Conta todos os profissionais ativos em todas as organizações (SuperAdmin)
     *
     * @return Quantidade total de profissionais ativos
     */
    public Long getEstatisticasMedicosAtivosByOrg() {
        return profissionalRepository.countTodosAtivos();
    }


    @Cacheable(value = "profissionais-org", key = "'org-' + #organizacaoId")
    @Transactional(readOnly = true)
    public List<Profissional> buscarPorOrganizacao(Long organizacaoId) {
        return profissionalRepository.findByOrganizacaoIdAndStatusWithRelations(organizacaoId, StatusProfissional.ATIVO);
    }



































    // ========== MÉTODOS DE BUSCA PARA AUTOCOMPLETE COM FILTRO ==========


    @RequiresTenant
    @Transactional(readOnly = true)
    public List<Profissional> buscarPorNomeComFiltro(String nome, String filtro) {
        Long orgId = tenantHelper.getCurrentTenantId();
        log.debug("Buscando profissionais por nome: {} na organização: {} com filtro: {}", nome, orgId, filtro);
        
        if ("ATIVO".equals(filtro)) {
            return profissionalRepository.findByOrganizacaoIdAndNomeContainingWithFiltro(orgId, nome, filtro, StatusProfissional.ATIVO);
        } else {
            // ALL - usa consulta original
            return profissionalRepository.findByOrganizacaoIdAndNomeContaining(orgId, nome);
        }
    }

    @RequiresTenant
    @Transactional(readOnly = true)
    public List<Profissional> buscarPorCRMComFiltro(String crm, String filtro) {
        Long orgId = tenantHelper.getCurrentTenantId();
        log.debug("Buscando profissionais por CRM: {} na organização: {} com filtro: {}", crm, orgId, filtro);
        
        if ("ATIVO".equals(filtro)) {
            return profissionalRepository.findByOrganizacaoIdAndCrmContainingWithFiltro(orgId, crm, filtro, StatusProfissional.ATIVO);
        } else {
            // ALL - usa consulta original
            return profissionalRepository.findByOrganizacaoIdAndCrmContaining(orgId, crm);
        }
    }


    @RequiresTenant
    @Transactional(readOnly = true)
    public List<Profissional> buscarPorCidadeComFiltro(String cidade, String filtro) {
        Long orgId = tenantHelper.getCurrentTenantId();
        log.debug("Buscando profissionais por cidade: {} na organização: {} com filtro: {}", cidade, orgId, filtro);
        
        if ("ATIVO".equals(filtro)) {
            return profissionalRepository.findByOrganizacaoIdAndCidadeContainingWithFiltro(orgId, cidade, filtro, StatusProfissional.ATIVO);
        } else {
            // ALL - usa consulta original
            return profissionalRepository.findByOrganizacaoIdAndCidadeContaining(orgId, cidade);
        }
    }


    @RequiresTenant
    @Transactional(readOnly = true)
    public List<Profissional> buscarPorEspecialidadeComFiltro(String especialidade, String filtro) {
        Long orgId = tenantHelper.getCurrentTenantId();
        log.debug("Buscando profissionais por especialidade: {} na organização: {} com filtro: {}", especialidade, orgId, filtro);
        
        if ("ATIVO".equals(filtro)) {
            return profissionalRepository.findByOrganizacaoIdAndEspecialidadeContainingWithFiltro(orgId, especialidade, filtro, StatusProfissional.ATIVO);
        } else {
            // ALL - usa consulta original
            return profissionalRepository.findByOrganizacaoIdAndEspecialidadeContaining(orgId, especialidade);
        }
    }


    @Cacheable(value = "profissionais-org", key = "'tenant-filtro-' + #filtro")
    @RequiresTenant
    @Transactional(readOnly = true)
    public List<Profissional> buscarTodosComFiltro(String filtro) {
        Long orgId = tenantHelper.getCurrentTenantId();
        log.debug("Buscando todos os profissionais na organização: {} com filtro: {}", orgId, filtro);
        
        if ("ATIVO".equals(filtro)) {
            return profissionalRepository.findByOrganizacao_IdWithUsuarioWithFiltro(orgId, filtro, StatusProfissional.ATIVO);
        } else {
            // ALL - usa consulta original
            return profissionalRepository.findByOrganizacao_IdWithUsuario(orgId);
        }
    }




}
