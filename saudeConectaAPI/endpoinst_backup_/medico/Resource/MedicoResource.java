package br.com.saudeConecta.endpoinst.medico.Resource;

import br.com.saudeConecta.domain.endereco.Endereco;
import br.com.saudeConecta.domain.medico.Medico;
import br.com.saudeConecta.domain.usuario.Usuario;
import br.com.saudeConecta.endpoinst.medico.DTO.AlteraDadosEnderecoMedico;
import br.com.saudeConecta.endpoinst.medico.DTO.AlterarDadosMedicos;
import br.com.saudeConecta.endpoinst.medico.DTO.DadosCadastraMedico;
import br.com.saudeConecta.endpoinst.medico.DTO.DadosMedicoView;
import br.com.saudeConecta.endpoinst.medico.Service.MedicoService;
import br.com.saudeConecta.infrastructure.persistence.repository.EnderecoRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.UsuarioRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Optional;

@RequestMapping(value = "/medico")
@RestController
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
public class MedicoResource {

    @Autowired
    private MedicoService service;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EnderecoRepository enderecoRepository;

    @GetMapping(value = "/buscarId/{id}")
    @Transactional
    public ResponseEntity<DadosMedicoView> buscarMedicoPorId(@NotNull @Valid @PathVariable("id") Long Id) {
        Optional<Medico> medico = service.buscarMedicoPorId(Id);
        return ResponseEntity.status(HttpStatus.OK).body(new DadosMedicoView((medico.get())));
    }


    @GetMapping(value = "/buscarPorEmail/{email}")
    @Transactional
    public ResponseEntity<DadosMedicoView> buscarMedicoPorEmail(@NotNull @Valid @PathVariable("email") String email) {
        Optional<Medico> medico = service.buscarMedicoPorEmail(email);

        return ResponseEntity.status(HttpStatus.OK).body(new DadosMedicoView((medico.get())));
    }


    @PostMapping("/post")
    @Transactional
    public ResponseEntity<DadosMedicoView> cadastrarMedico(@RequestBody @Valid DadosCadastraMedico dados, @NotNull BindingResult result,
                                                          UriComponentsBuilder uriBuilder) {


        if (result.hasErrors()) {
            return ResponseEntity.badRequest().build();
        }

        Long idUsuario = dados.usuario();
        Long idEndereco = dados.endereco();


        Optional<Usuario> usuarioOptional = usuarioRepository.findById(idUsuario);
        Optional<Endereco> enderecoOptional = enderecoRepository.findById(idEndereco);


        if (usuarioOptional.isEmpty() || enderecoOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Usuario usuario = usuarioOptional.get();
        Endereco endereco = enderecoOptional.get();


        Medico medico = new Medico(dados , usuario , endereco );

        service.cadastrarMedico(medico);

        URI uri = uriBuilder.path("/plano/{id}").buildAndExpand(medico.getMedCodigo()).toUri();

        return ResponseEntity.created(uri).body(new DadosMedicoView(medico));

    }



    @GetMapping(value = "/medicopagina")
    public Page<DadosMedicoView> buscarPorPaginas(@PageableDefault(size = 12, sort = { "medCodigo" }) Pageable paginacao) {
        return service.buscarPorPaginas(paginacao);
    }


    @GetMapping(value = "/listatodosmedicos")
    public List<Medico> buscarTodosMedicos() {
        return 	 service.buscarTodosMedicos();
    }



    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable("id") Long id)  throws  Exception{
        service.deletarPorId(id);
        return ResponseEntity.noContent().build();
    }








    @GetMapping(value = "/buscarPorNome/{nome}")
    @Transactional
    public List<Medico> buscarMedicoPorNome(@NotNull @Valid @PathVariable("nome") String nome) {
        return service.buscarMedicoPorNome(nome);
    }

    @GetMapping(value = "/buscarPorCRM/{crm}")
    @Transactional
    public List<Medico> buscarMedicoPorCRM(@NotNull @Valid @PathVariable("crm") String crm) {
        return service.buscarMedicoPorCRM(crm);
    }


    @GetMapping(value = "/buscarPorCidade/{cidade}")
    @Transactional
    public List<Medico> buscarMedicoPorCidade(@NotNull @Valid @PathVariable("cidade") String cidade) {
        return service.buscarMedicoPorCidade(cidade);
    }

    @GetMapping(value = "/buscarPorMedEspecialidade/{especialidade}")
    @Transactional
    public List<Medico>buscarMedicoPorMedEspecialidade(@NotNull @Valid @PathVariable("especialidade") String especialidade) {
        return   service.buscarMedicoPorEspecialidade(especialidade);
    }

//  fazer comit , busca de usuarios para o admim nao aparece


    @GetMapping(value = "/buscarIdDeUsusario/{id}")
    @Transactional
    public ResponseEntity<DadosMedicoView> buscarPorIdUsuario(@NotNull @Valid @PathVariable("id") Long Id) {
        Optional<Medico> medico = service.buscarMedicoPorIdUsuario(Id);

        return ResponseEntity.status(HttpStatus.OK).body(new DadosMedicoView((medico.get())));
    }


    @PutMapping("/AlterarDadosMedico/{id}")
    public ResponseEntity<Void> atualizarDadosMedico(@PathVariable("id") Long id, @RequestBody @Valid AlterarDadosMedicos dados) throws Exception {
        service.atualizarDadosMedico(id, dados);
        return ResponseEntity.noContent().build();
    }




    @PutMapping("/AtualizarEnderecoMedico/{id}")
    public ResponseEntity<Void> atualizarEnderecoMedico(@PathVariable("id") Long id, @RequestBody @Valid AlteraDadosEnderecoMedico dados) throws Exception {
        service.atualizarEnderecoMedico(id, dados);
        return ResponseEntity.noContent().build();
    }


}
