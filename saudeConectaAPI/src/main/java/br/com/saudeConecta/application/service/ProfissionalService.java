package br.com.saudeConecta.application.service;

import br.com.saudeConecta.domain.common.Sexo;
import br.com.saudeConecta.domain.endereco.Endereco;
import br.com.saudeConecta.domain.organizacao.Organizacao;
import br.com.saudeConecta.domain.profissional.Especialidade;
import br.com.saudeConecta.domain.profissional.Profissional;
import br.com.saudeConecta.domain.profissional.StatusProfissional;
import br.com.saudeConecta.domain.profissional.TipoProfissional;
import br.com.saudeConecta.domain.usuario.TipoUsuarioNovo;
import br.com.saudeConecta.domain.usuario.Usuario;
import br.com.saudeConecta.email.EmailCadastroService;
import br.com.saudeConecta.infra.tenant.RequiresTenant;
import br.com.saudeConecta.infra.tenant.TenantHelper;
import br.com.saudeConecta.infrastructure.persistence.repository.*;
import br.com.saudeConecta.presentation.dto.profissional.CadastrarClinicoRequest;
import br.com.saudeConecta.presentation.dto.profissional.CadastrarProfissionalRequest;
import br.com.saudeConecta.presentation.dto.profissional.ProfissionalResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        TipoProfissional tipoMedico = tipoProfissionalRepository.findByCodigo("MEDICO")
            .orElseThrow(() -> new IllegalStateException("Tipo MEDICO não encontrado"));

        String senhaGerada = gerarSenhaAleatoria();
        String senhaCriptografada = passwordEncoder.encode(senhaGerada);

        Usuario usuario = Usuario.builder()
                .login(cpfLimpo)
                .senha(senhaCriptografada)
                .tipoUsuario((byte) 3)
                .tipoUsuarioNovo(TipoUsuarioNovo.PROFISSIONAL)
                .organizacao(organizacao)
                .status((byte) 1)
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
            // Assume que especialidade contém IDs separados por vírgula
            String[] especialidadeIds = request.especialidade().split(",");
            for (String especialidadeId : especialidadeIds) {
                try {
                    Long id = Long.parseLong(especialidadeId.trim());
                    especialidadeRepository.findById(id).ifPresent(especialidades::add);
                } catch (NumberFormatException e) {
                    log.warn("ID de especialidade inválido: {}", especialidadeId);
                }
            }
        }

        Profissional profissional = Profissional.builder()
            .organizacao(organizacao)
            .tipoProfissional(tipoMedico)
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
            .status(StatusProfissional.ATIVO)
            .especialidades(especialidades)
            .build();

        Profissional salvo = profissionalRepository.save(profissional);
        log.info("Clínico cadastrado com sucesso. ID: {}", salvo.getId());

        // Enviar email de credenciais de forma assíncrona
        emailCadastroService.enviarCredenciaisClinicoAsync(
            request.email(),
            request.nome(),
            cpfLimpo,
            senhaGerada,
            organizacao.getNome()
        );

        return salvo;
    }


    
    @RequiresTenant
    @Transactional(readOnly = true)
    public Optional<Profissional> buscarClinicoIdByOrg(Long id) {
        Long orgId = tenantHelper.getCurrentTenantId();
        return profissionalRepository.findByIdAndOrganizacao_Id(id, orgId);
    }

    @RequiresTenant
    @Transactional
    public Profissional atualizarClinicoIdByOrg(Long id, ProfissionalResponse dadosAtualizados) {
        Long orgId = tenantHelper.getCurrentTenantId();

        Profissional profissional = profissionalRepository.findByIdAndOrganizacao_Id(id, orgId)
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
        profissional.setTempoConsultaMinutos(dadosAtualizados.tempoConsultaMinutos());
        profissional.setDataNascimento(dadosAtualizados.dataNascimento());

        // Atualiza especialidades se fornecidas
        if (dadosAtualizados.especialidades() != null && !dadosAtualizados.especialidades().isEmpty()) {
            Set<Especialidade> especialidades = dadosAtualizados.especialidades().stream()
                    .map(especialidadeResumo -> especialidadeRepository.findById(especialidadeResumo.id())
                            .orElseThrow(() -> new IllegalArgumentException("Especialidade não encontrada: " + especialidadeResumo.id())))
                    .collect(Collectors.toSet());
            profissional.setEspecialidades(especialidades);
        }

        // Atualiza endereço se fornecido
        // Atualiza endereço usando o método helper
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



























    @RequiresTenant
    public List<Profissional> buscarTodos() {
        Long orgId = tenantHelper.getCurrentTenantId();
        return profissionalRepository.findByOrganizacao_Id(orgId);
    }

    @RequiresTenant
    public Page<Profissional> buscarTodos(Pageable pageable) {
        Long orgId = tenantHelper.getCurrentTenantId();
        return profissionalRepository.findByOrganizacao_Id(orgId, pageable);
    }

    @RequiresTenant
    public Optional<Profissional> buscarPorId(Long id) {
        Long orgId = tenantHelper.getCurrentTenantId();
        return profissionalRepository.findByIdAndOrganizacao_Id(id, orgId);
    }

    @RequiresTenant
    public List<Profissional> buscarPorTipo(String tipoCodigo) {
        Long orgId = tenantHelper.getCurrentTenantId();
        return profissionalRepository.findAtivosByOrganizacaoIdAndTipo(orgId, tipoCodigo);
    }

    @RequiresTenant
    public List<Profissional> buscarMedicos() {
        return buscarPorTipo("MEDICO");
    }

    @RequiresTenant
    public List<Profissional> buscarDentistas() {
        return buscarPorTipo("DENTISTA");
    }



    @RequiresTenant
    @Transactional
    public void deletar(Long id) {
        Long orgId = tenantHelper.getCurrentTenantId();
        Profissional profissional = profissionalRepository.findByIdAndOrganizacao_Id(id, orgId)
            .orElseThrow(() -> new IllegalArgumentException("Profissional não encontrado"));
        
        profissional.setStatus(StatusProfissional.INATIVO);
        profissionalRepository.save(profissional);
        
        if (profissional.getUsuario() != null) {
            profissional.getUsuario().setStatus((byte) 0);
            usuarioRepository.save(profissional.getUsuario());
        }
        
        log.info("Profissional ID: {} inativado com sucesso", id);
    }
    
    @RequiresTenant
    public Long contarAtivos() {
        Long orgId = tenantHelper.getCurrentTenantId();
        return profissionalRepository.countAtivosByOrganizacaoId(orgId);
    }

    // ==========================================
    // ESTATÍSTICAS POR ORGANIZAÇÃO
    // ==========================================

    public Long contarAtivosPorOrganizacao(Long organizacaoId) {
        return profissionalRepository.countAtivosByOrganizacaoId(organizacaoId);
    }

    @Transactional(readOnly = true)
    public List<Profissional> buscarPorOrganizacao(Long organizacaoId) {
        return profissionalRepository.findByOrganizacaoIdAndStatusWithRelations(organizacaoId, StatusProfissional.ATIVO);
    }

//    @RequiresTenant
//    public List<Profissional> buscarPorNome(String nome) {
//        Long orgId = tenantHelper.getCurrentTenantId();
//        return profissionalRepository.findByOrganizacaoIdAndNomeContaining(orgId, nome);
//    }

    // ==========================================
    // ESTATÍSTICAS GLOBAIS (SUPER ADMIN)
    // ==========================================

    public Long contarTodosAtivos() {
        return profissionalRepository.countAllAtivos();
    }

    public List<Profissional> buscarTodosAtivos() {
        return profissionalRepository.findByStatus(StatusProfissional.ATIVO);
    }
    





    @RequiresTenant
    @Transactional
    public Profissional cadastrar(CadastrarProfissionalRequest request) {
        Long orgId = tenantHelper.getCurrentTenantId();
        log.info("Cadastrando profissional: {} na organização: {}", request.nome(), orgId);

        if (request.cpf() != null && profissionalRepository.existsByCpf(limparCpf(request.cpf()))) {
            throw new IllegalStateException("CPF já cadastrado no sistema");
        }

        Organizacao organizacao = organizacaoRepository.findById(orgId)
                .orElseThrow(() -> new IllegalStateException("Organização não encontrada"));

        TipoProfissional tipo = tipoProfissionalRepository.findById(request.tipoProfissionalId())
                .orElseThrow(() -> new IllegalArgumentException("Tipo de profissional não encontrado"));

        String senhaGerada = gerarSenhaAleatoria();
        String senhaCriptografada = passwordEncoder.encode(senhaGerada);

        Usuario usuario = new Usuario();
        usuario.setLogin(limparCpf(request.cpf()));
        usuario.setSenha(senhaCriptografada);
        usuario.setTipoUsuario((byte) 3);
        usuario.setTipoUsuarioNovo(TipoUsuarioNovo.PROFISSIONAL);
        usuario.setOrganizacao(organizacao);
        usuario.setStatus((byte) 1);
        Usuario usuarioSalvo = usuarioRepository.save(usuario);

        Endereco endereco = null;
        if (request.endCep() != null) {
            endereco = new Endereco();
            endereco.setEndNacionalidade(request.endNacionalidade());
            endereco.setEndUF(request.endUF());
            endereco.setEndMunicipio(request.endMunicipio());
            endereco.setEndBairro(request.endBairro());
            endereco.setEndCep(request.endCep());
            endereco.setEndRua(request.endRua());
            endereco.setEndNumero(request.endNumero());
            endereco.setEndComplemento(request.endComplemento());
            endereco = enderecoRepository.save(endereco);
        }

        Set<Especialidade> especialidades = new HashSet<>();
        if (request.especialidadeIds() != null && !request.especialidadeIds().isEmpty()) {
            especialidades = new HashSet<>(especialidadeRepository.findAllById(request.especialidadeIds()));
        }

        Profissional profissional = Profissional.builder()
                .organizacao(organizacao)
                .tipoProfissional(tipo)
                .nome(request.nome())
                .sexo(request.sexo() != null ? Sexo.valueOf(request.sexo()) : null)
                .dataNascimento(request.dataNascimento())
                .registroConselho(request.registroConselho())
                .cpf(limparCpf(request.cpf()))
                .rg(request.rg())
                .email(request.email())
                .telefone(request.telefone())
                .formacao(request.formacao())
                .instituicao(request.instituicao())
                .tempoConsultaMinutos(request.tempoConsultaMinutos() != null ? request.tempoConsultaMinutos() : 30)
                .usuario(usuarioSalvo)
                .endereco(endereco)
                .status(StatusProfissional.ATIVO)
                .especialidades(especialidades)
                .build();

        Profissional salvo = profissionalRepository.save(profissional);
        log.info("Profissional cadastrado com sucesso. ID: {}", salvo.getId());

        return salvo;
    }
}
