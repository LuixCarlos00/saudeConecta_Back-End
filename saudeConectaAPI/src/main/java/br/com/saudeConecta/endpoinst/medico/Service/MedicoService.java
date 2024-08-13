package br.com.saudeConecta.endpoinst.medico.Service;

import br.com.saudeConecta.endpoinst.endereco.Entity.Endereco;
import br.com.saudeConecta.endpoinst.endereco.Repository.EnderecoRepository;
import br.com.saudeConecta.endpoinst.medico.DTO.AlteraDadosEnderecoMedico;
import br.com.saudeConecta.endpoinst.medico.DTO.AlterarDadosMedicos;
import br.com.saudeConecta.endpoinst.medico.DTO.DadosCadastraMedico;
import br.com.saudeConecta.endpoinst.medico.DTO.DadosMedicoView;
import br.com.saudeConecta.endpoinst.medico.Entity.Medico;
import br.com.saudeConecta.endpoinst.medico.Repository.MedicoRepository;
import br.com.saudeConecta.endpoinst.usuario.Entity.Usuario;
import br.com.saudeConecta.endpoinst.usuario.Repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class MedicoService {

    @Autowired
    private MedicoRepository repository;

    @Autowired
    private EnderecoRepository enderecoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    public Optional<Medico> buscarMedicoPorId(Long id) {
        return repository.findByUsuario_Id(id);
    }


    public void cadastrar(@NotNull Medico medico) {


        repository.save(medico);

    }

    public Optional<Medico> BuscarPorId(Long id) {
        return repository.findById(id);
    }


    public Page<DadosMedicoView> BuscarPorPaginas(Pageable paginacao) {
        return repository.findAll(paginacao).map(DadosMedicoView::new);
    }


    @Transactional
    public void deletarPorId(Long id) throws Exception {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("ID inválido");
        }

        if (!repository.existsById(id)) {
            throw new Exception("Registro não encontrado");
        }

        try {
            repository.deleteById(id);
        } catch (Exception e) {
            throw new Exception("Violação de Integridade", e);
        }
    }


    public List<Medico> buscarTodosMedicos() {
        return repository.findAll();

    }


    public void CadastraRegistroMedico(Medico medico) {

        repository.save(medico);
    }


    public Optional<Medico> buscarMedicoPorEmail(String email) {
        return repository.findByMedEmail(email);
    }


    public List<Medico> buscarMedicoPorCRM(String crm) {
        return repository.findByMedCrmContainingIgnoreCase(crm);
    }

    public List<Medico> buscarMedicoPorCidade(String cidade) {
        return repository.findByEndereco_EndMunicipioContainingIgnoreCase(cidade);
    }

    public List<Medico> buscarMedicoPorEspecialidade(String especialidade) {
        return repository.findByMedEspecialidadeContainingIgnoreCase(especialidade);
    }


    public List<Medico> buscarMedicoPorNome(String nome) {
        return repository.findByMedNomeContainingIgnoreCase(nome);
    }

    public Optional<Medico> buscarMedicoPorIdDeUsusario(Long id) {

        return repository.findByUsuario_Id(id);
    }

    public void AleterarDadosMedico(Long id, AlterarDadosMedicos dados) {
        if (id == null) {
            throw new IllegalArgumentException("O ID não pode ser nulo");
        }

        Medico medico = repository.findById(id).orElseThrow(() -> new EntityNotFoundException("Médico não encontrado"));

        // Atualize o médico conforme os dados
        medico.setMedCpf(dados.MedCpf());
        medico.setMedCrm(dados.MedCrm());
        medico.setMedDataNacimento(dados.MedDataNacimento());
        medico.setMedEmail(dados.MedEmail());
        medico.setMedEspecialidade(dados.MedEspecialidade());
        medico.setMedNome(dados.MedNome());
        medico.setMedRg(dados.MedRg());
        medico.setMedSexo(dados.MedSexo());
        medico.setMedTelefone(dados.MedTelefone());
        medico.setMedTempoDeConsulta(dados.MedTempoDeConsulta());

        medico.setEndereco(enderecoRepository.getReferenceById(dados.Endereco()));
        medico.setUsuario(usuarioRepository.getReferenceById(dados.Usuario()));

        repository.save(medico);
    }

    public void AtualizarEnderecoMedico(Long id, AlteraDadosEnderecoMedico dados) {
        if (id == null) {
            throw new IllegalArgumentException("O ID não pode ser nulo");
        }

        Endereco endereco = enderecoRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Endereço não encontrado"));

        endereco.setEndBairro(dados.EndBairro());
        endereco.setEndCep(dados.EndCep());
        endereco.setEndMunicipio(dados.EndMunicipio());
        endereco.setEndNacionalidade(dados.EndNacionalidade());
        endereco.setEndRua(dados.EndRua());
        endereco.setEndComplemento(dados.EndComplemento());
        endereco.setEndNumero(dados.EndNumero());
        endereco.setEndUF(dados.EndUF());

        enderecoRepository.save(endereco);
    }



}