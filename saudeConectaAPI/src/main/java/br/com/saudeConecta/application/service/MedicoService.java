package br.com.saudeConecta.application.service;

import br.com.saudeConecta.application.port.in.medico.MedicoInputPort;
import br.com.saudeConecta.application.port.out.medico.MedicoOutputPort;
import br.com.saudeConecta.domain.endereco.Endereco;
import br.com.saudeConecta.domain.medico.Medico;
import br.com.saudeConecta.domain.usuario.Usuario;
import br.com.saudeConecta.email.EnviarService.CredenciaisEmailService;
import br.com.saudeConecta.infrastructure.persistence.repository.EnderecoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.UsuarioRepository;
import br.com.saudeConecta.presentation.dto.medico.CadastrarMedicoCompletoRequest;
import br.com.saudeConecta.presentation.dto.medico.CadastrarMedicoRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MedicoService implements MedicoInputPort {

    private final MedicoOutputPort medicoOutputPort;
    private final UsuarioRepository usuarioRepository;
    private final EnderecoRepository enderecoRepository;
    private final PasswordEncoder passwordEncoder;
    private final CredenciaisEmailService credenciaisEmailService;
    
    private static final String CARACTERES_SENHA = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#$%&*";
    private static final int TAMANHO_SENHA = 10;

    @Override
    public Optional<Medico> buscarPorId(Long id) {
        log.debug("Buscando médico por ID: {}", id);
        return medicoOutputPort.findById(id);
    }

    @Override
    public Optional<Medico> buscarPorIdUsuario(Long usuarioId) {
        log.debug("Buscando médico por ID de usuário: {}", usuarioId);
        return medicoOutputPort.buscarMedicoPorIdUsuario(usuarioId);
    }

    @Override
    public Optional<Medico> buscarPorEmail(String email) {
        log.debug("Buscando médico por email: {}", email);
        return medicoOutputPort.findByMedEmail(email);
    }

    @Override
    public List<Medico> buscarTodos() {
        log.debug("Buscando todos os médicos");
        return medicoOutputPort.findAll();
    }

    public List<Medico> buscarTodosComUsuario() {
        log.debug("Buscando todos os médicos com dados do usuário");
        return medicoOutputPort.findAllWithUsuario();
    }

    @Override
    public Page<Medico> buscarTodos(Pageable pageable) {
        log.debug("Buscando todos os médicos com paginação");
        return medicoOutputPort.findAll(pageable);
    }

    @Override
    public List<Medico> buscarPorCrm(String crm) {
        log.debug("Buscando médicos por CRM: {}", crm);
        return medicoOutputPort.findByMedCrmContainingIgnoreCase(crm);
    }

    @Override
    public List<Medico> buscarPorNome(String nome) {
        log.debug("Buscando médicos por nome: {}", nome);
        return medicoOutputPort.findByMedNomeContainingIgnoreCase(nome);
    }

    @Override
    public List<Medico> buscarPorEspecialidade(String especialidade) {
        log.debug("Buscando médicos por especialidade: {}", especialidade);
        return medicoOutputPort.findByMedEspecialidadeContainingIgnoreCase(especialidade);
    }

    @Override
    public List<Medico> buscarPorMunicipio(String municipio) {
        log.debug("Buscando médicos por município: {}", municipio);
        return medicoOutputPort.findByEndereco_EndMunicipioContainingIgnoreCase(municipio);
    }

    @Override
    public Long contarMedicosAtivos() {
        log.debug("Contando médicos ativos");
        return medicoOutputPort.contarMedicosAtivos();
    }

    @Override
    public Medico cadastrar(Medico medico) {
        log.info("Cadastrando novo médico: {}", medico.getMedNome());
        Medico medicoSalvo = medicoOutputPort.save(medico);
        log.info("Médico cadastrado com sucesso. ID: {}", medicoSalvo.getMedCodigo());
        return medicoSalvo;
    }

    @Override
    public void deletar(Long id) throws Exception {
        log.info("Iniciando exclusão do médico ID: {}", id);

        if (id == null || id <= 0) {
            log.warn("Tentativa de exclusão com ID inválido: {}", id);
            throw new IllegalArgumentException("ID inválido");
        }

        if (!medicoOutputPort.existsById(id)) {
            log.warn("Médico não encontrado para exclusão ID: {}", id);
            throw new Exception("Registro não encontrado");
        }

        try {
            medicoOutputPort.deleteById(id);
            log.info("Médico ID: {} excluído com sucesso", id);
        } catch (Exception e) {
            log.error("Erro ao excluir médico ID: {}", id, e);
            throw new Exception("Violação de Integridade", e);
        }
    }

    public List<Medico> buscarTodosMedicos() {
        return medicoOutputPort.findAll();
    }

    public Medico cadastrarComDados(CadastrarMedicoRequest dados, Long usuarioId, Long enderecoId) {
        var usuarioOptional = usuarioRepository.findById(usuarioId);
        if (usuarioOptional.isEmpty()) {
            throw new IllegalArgumentException("Usuário não encontrado");
        }

        var enderecoOptional = enderecoRepository.findById(enderecoId);
        if (enderecoOptional.isEmpty()) {
            throw new IllegalArgumentException("Endereço não encontrado");
        }

        Usuario usuario = usuarioOptional.get();
        Endereco endereco = enderecoOptional.get();
        Medico medico = new Medico(dados, usuario, endereco);
        
        return medicoOutputPort.save(medico);
    }

    /**
     * Cadastra médico completo: cria usuário com CPF como login, gera senha com BCrypt e envia por email.
     * @param dados DTO com dados do médico e endereço
     * @return Medico cadastrado
     * @throws IllegalStateException se CPF já estiver cadastrado como login
     */
    public Medico cadastrarCompleto(CadastrarMedicoCompletoRequest dados) {
        log.info("Cadastrando médico completo: {} com CPF: {}", dados.medNome(), dados.medCpf());
        
        String cpfLimpo = limparCpf(dados.medCpf());
        
        if (usuarioRepository.existsByLogin(cpfLimpo)) {
            log.warn("CPF já cadastrado como login: {}", cpfLimpo);
            throw new IllegalStateException("CPF já cadastrado no sistema");
        }
        
        String senhaGerada = gerarSenhaAleatoria();
        String senhaCriptografada = passwordEncoder.encode(senhaGerada);
        
        Usuario usuario = new Usuario();
        usuario.setLogin(cpfLimpo);
        usuario.setSenha(senhaCriptografada);
        usuario.setTipoUsuario((byte) 3); // 3 = Médico
        usuario.setStatus((byte) 1);
        Usuario usuarioSalvo = usuarioRepository.save(usuario);
        log.info("Usuário criado para médico. ID: {}", usuarioSalvo.getId());
        
        Endereco endereco = new Endereco();
        endereco.setEndNacionalidade(dados.endNacionalidade());
        endereco.setEndUF(dados.endUF());
        endereco.setEndMunicipio(dados.endMunicipio());
        endereco.setEndBairro(dados.endBairro());
        endereco.setEndCep(dados.endCep());
        endereco.setEndRua(dados.endRua());
        endereco.setEndNumero(dados.endNumero() != null ? dados.endNumero().longValue() : null);
        endereco.setEndComplemento(dados.endComplemento() != null ? dados.endComplemento() : "");
        Endereco enderecoSalvo = enderecoRepository.save(endereco);
        log.info("Endereço criado para médico. ID: {}", enderecoSalvo.getEndCodigo());
        
        Medico medico = new Medico();
        medico.setMedNome(dados.medNome());
        medico.setMedSexo(dados.medSexo());
        medico.setMedDataNacimento(dados.medDataNacimento() != null ? java.sql.Date.valueOf(dados.medDataNacimento()) : null);
        medico.setMedCrm(dados.medCrm());
        medico.setMedCpf(dados.medCpf());
        medico.setMedRg(dados.medRg());
        medico.setMedEmail(dados.medEmail());
        medico.setMedTelefone(dados.medTelefone());
        medico.setMedEspecialidade(dados.medEspecialidade());
        medico.setMedFormacoes(dados.medFormacoes());
        medico.setMedEmpresa(dados.medEmpresa());
        medico.setMedGraduacao(dados.medGraduacao());
        medico.setMedTempoDeConsulta(dados.medTempoDeConsulta());
        medico.setUsuario(usuarioSalvo);
        medico.setEndereco(enderecoSalvo);
        
        Medico medicoSalvo = medicoOutputPort.save(medico);
        log.info("Médico cadastrado com sucesso. ID: {}", medicoSalvo.getMedCodigo());
        
        credenciaisEmailService.enviarCredenciaisMedico(dados.medEmail(), dados.medNome(), cpfLimpo, senhaGerada);
        
        return medicoSalvo;
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
