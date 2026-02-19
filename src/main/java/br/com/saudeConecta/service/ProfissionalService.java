package br.com.saudeConecta.service;

import br.com.saudeConecta.domain.common.Sexo;
import br.com.saudeConecta.domain.endereco.Endereco;
import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.profissional.Especialidade;
import br.com.saudeConecta.domain.profissional.Profissional;
import br.com.saudeConecta.domain.profissional.StatusProfissional;
import br.com.saudeConecta.domain.profissional.TipoProfissional;
import br.com.saudeConecta.domain.usuario.TipoUsuarioNovo;
import br.com.saudeConecta.domain.usuario.Usuario;
import br.com.saudeConecta.domain.usuario.StatusUsuario;
import br.com.saudeConecta.email.EmailCadastroService;
import br.com.saudeConecta.infra.tenant.RequiresTenant;
import br.com.saudeConecta.infra.tenant.TenantHelper;
import br.com.saudeConecta.infrastructure.persistence.repository.*;
import br.com.saudeConecta.presentation.dto.profissional.AtualizarClinicoRequest;
import br.com.saudeConecta.presentation.dto.profissional.CadastrarClinicoRequest;
import br.com.saudeConecta.presentation.dto.profissional.ProfissionalResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.stream.Collectors;
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
    private final EmailCadastroService emailCadastroService;
    
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
            EmailCadastroService emailCadastroService) {
        this.profissionalRepository = profissionalRepository;
        this.tipoProfissionalRepository = tipoProfissionalRepository;
        this.especialidadeRepository = especialidadeRepository;
        this.organizacaoRepository = organizacaoRepository;
        this.usuarioRepository = usuarioRepository;
        this.enderecoRepository = enderecoRepository;
        this.passwordEncoder = passwordEncoder;
        this.tenantHelper = tenantHelper;
        this.emailCadastroService = emailCadastroService;
    }




    @RequiresTenant
    @Transactional
    public Profissional cadastraClinicoByOrg(CadastrarClinicoRequest request) {
        Long orgId = tenantHelper.getCurrentTenantId();
        log.info("Cadastrando clínico: {} na organização: {}", request.nome(), orgId);

        String cpfLimpo = limparCpf(request.cpf());

        if (cpfLimpo != null && profissionalRepository.existsByCpfAndOrganizacao_Id(cpfLimpo, orgId)) {
            throw new IllegalStateException("CPF já cadastrado no sistema");
        }

        Organizacao organizacao = organizacaoRepository.findById(orgId)
            .orElseThrow(() -> new IllegalStateException("Organização não encontrada"));

        // Determina o tipo profissional com base no request
        String tipoCodigo = request.tipoProfissional() != null ? request.tipoProfissional() : "MEDICO";
        TipoProfissional tipoProfissional = tipoProfissionalRepository.findByCodigo(tipoCodigo)
            .orElseThrow(() -> new IllegalStateException("Tipo " + tipoCodigo + " não encontrado"));

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
                        // Se não encontrar, cria nova especialidade
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
        log.info("Clínico cadastrado com sucesso. ID: {}", salvo.getId());

        // Enviar email de credenciais de forma assíncrona
        // Em caso de falha, o EmailCadastroService registrará na tabela de mensageria
        emailCadastroService.enviarCredenciaisClinicoAsync(
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

    @RequiresTenant
    @Transactional
    public Profissional atualizarClinicoIdByOrg(Long id, AtualizarClinicoRequest dadosAtualizados) {
        Long orgId = tenantHelper.getCurrentTenantId();

        Profissional profissional = profissionalRepository.buscarClinicoIdByOrg(id, orgId)
                .orElseThrow(() -> new IllegalArgumentException("Profissional não encontrado"));

        // Atualiza campos básicos
        profissional.setNome(dadosAtualizados.nome());
        profissional.setCpf(dadosAtualizados.cpf());
        profissional.setRg(dadosAtualizados.rg());
        profissional.setRegistroConselho(dadosAtualizados.registroConselho());
        profissional.setTelefone(dadosAtualizados.telefone());
        profissional.setEmail(dadosAtualizados.email());
        profissional.setFormacao(dadosAtualizados.formacao());
        profissional.setInstituicao(dadosAtualizados.instituicao());
        profissional.setValorConsulta(dadosAtualizados.valorConsulta());
        profissional.setTempoConsultaMinutos(dadosAtualizados.tempoConsultaMinutos());
        profissional.setDataNascimento(dadosAtualizados.dataNascimento());

        // Atualiza tipo profissional se fornecido
        if (dadosAtualizados.tipoProfissional() != null && !dadosAtualizados.tipoProfissional().isEmpty()) {
            String tipoCodigo = dadosAtualizados.tipoProfissional();
            TipoProfissional tipoProfissional = tipoProfissionalRepository.findByCodigo(tipoCodigo)
                .orElseThrow(() -> new IllegalStateException("Tipo " + tipoCodigo + " não encontrado"));
            profissional.setTipoProfissional(tipoProfissional);
        }

        // Atualiza especialidade se fornecida (recebe nome da especialidade)
        if (dadosAtualizados.especialidade() != null && !dadosAtualizados.especialidade().isEmpty()) {
            String nomeEspecialidade = dadosAtualizados.especialidade().trim();
            TipoProfissional tipoProfissional = profissional.getTipoProfissional();
            
            // Busca especialidade pelo nome e tipo profissional
            especialidadeRepository.findByTipoProfissional_IdAndNome(tipoProfissional.getId(), nomeEspecialidade)
                .ifPresentOrElse(
                    especialidade -> {
                        Set<Especialidade> especialidades = new HashSet<>();
                        especialidades.add(especialidade);
                        profissional.setEspecialidades(especialidades);
                    },
                    () -> {
                        // Se não encontrar, cria nova especialidade
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
                        profissional.setEspecialidades(especialidades);
                    }
                );
        }

        // Atualiza endereço se fornecido
        if (dadosAtualizados.endereco() != null) {
            Endereco endereco = profissional.getEndereco();

            if (endereco == null) {
                // Cria novo endereço
                endereco = dadosAtualizados.endereco().toEntity();
                endereco = enderecoRepository.save(endereco);
            } else {
                // Atualiza endereço existente
                dadosAtualizados.endereco().updateEntity(endereco);
            }

            profissional.setEndereco(endereco);
        }

        log.info("Profissional atualizado: {}", profissional);
        return profissionalRepository.save(profissional);
    }



    @RequiresTenant
    @Transactional
    public void deletarClinicoIdByOrg(Long idProfissional) {
        Long orgId = tenantHelper.getCurrentTenantId();
        log.info("Deletando profissional ID: {} da organização: {}", idProfissional, orgId);

        // 1. Buscar o profissional na tabela profissional
        Profissional profissional = profissionalRepository.buscarClinicoIdByOrg(idProfissional, orgId)
                .orElseThrow(() -> new IllegalArgumentException("Profissional não encontrado"));

        Usuario usuario = profissional.getUsuario();
        if (usuario == null) {
            throw new IllegalStateException("Profissional não possui usuário associado");
        }

        try {
            // 2. Deletar o registro de profissional na tabela profissional primeiro
            profissionalRepository.delete(profissional);
            
            // 3. Deletar o usuário da tabela usuario
            usuarioRepository.delete(usuario);
            
            log.info("Profissional e usuário deletados com sucesso. ID Profissional: {}, ID Usuário: {}", 
                    idProfissional, usuario.getId());
                    
        } catch (Exception e) {
            // 4. Caso haja relacionamento que impeça o delete, cancelar e avisar
            String errorMessage = "Não foi possível deletar o profissional devido a relacionamentos existentes: " + e.getMessage();
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


    @Transactional(readOnly = true)
    public List<Profissional> buscarPorOrganizacao(Long organizacaoId) {
        return profissionalRepository.findByOrganizacaoIdAndStatusWithRelations(organizacaoId, StatusProfissional.ATIVO);
    }



































    // ========== MÉTODOS DE BUSCA PARA AUTOCOMPLETE COM FILTRO ==========
    
    @RequiresTenant
    @Transactional(readOnly = true)
    public List<Profissional> buscarPorNome(String nome) {
        return buscarPorNomeComFiltro(nome, "ALL");
    }
    
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
    public List<Profissional> buscarPorCRM(String crm) {
        return buscarPorCRMComFiltro(crm, "ALL");
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
    public List<Profissional> buscarPorCidade(String cidade) {
        return buscarPorCidadeComFiltro(cidade, "ALL");
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
    public List<Profissional> buscarPorEspecialidade(String especialidade) {
        return buscarPorEspecialidadeComFiltro(especialidade, "ALL");
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
    
    @RequiresTenant
    @Transactional(readOnly = true)
    public List<Profissional> buscarTodos() {
        return buscarTodosComFiltro("ALL");
    }
    
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
