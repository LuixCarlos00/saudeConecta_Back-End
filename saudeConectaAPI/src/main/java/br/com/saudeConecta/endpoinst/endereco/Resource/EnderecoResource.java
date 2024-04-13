package br.com.saudeConecta.endpoinst.endereco.Resource;

import br.com.saudeConecta.endpoinst.endereco.DTO.DadosCadastroEndereco;
import br.com.saudeConecta.endpoinst.endereco.DTO.DadosEnderecoView;
import br.com.saudeConecta.endpoinst.endereco.Entity.Endereco;
import br.com.saudeConecta.endpoinst.endereco.Service.EnderecoService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping(value = "/endereco")
@CrossOrigin(origins = "http://localhost:4200")
public class EnderecoResource {



    @Autowired
    private EnderecoService service;


    @CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
    @GetMapping(value = "/buscarId/{id}")
    @Transactional
    public ResponseEntity<DadosEnderecoView> buscarMedicoPorId(@NotNull @Valid @PathVariable("id") Long Id) {
        Optional<Endereco> endereco = service.buscarMedicoPorId(Id);

        return ResponseEntity.status(HttpStatus.OK).body(new DadosEnderecoView((endereco.get())));
    }





    // @RolesAllowed("ROLE_PROFESSOR")
    @CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
    @PostMapping("/post")
    @Transactional
    public ResponseEntity<DadosEnderecoView> CadastraEndereco (@RequestBody @Valid @org.jetbrains.annotations.NotNull DadosCadastroEndereco dados, @NotNull @org.jetbrains.annotations.NotNull UriComponentsBuilder uriBuilder ) {
        Endereco endereco =  dados.toEntity();
        service.CadastraRegistroEndereco(endereco);

        URI uri = uriBuilder.path("/plano/{id}").buildAndExpand(endereco.getEndCodigo()).toUri();

        return ResponseEntity.created(uri).body(new DadosEnderecoView(endereco));

    }

    @GetMapping(value = "/{id}")
    @CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
    public ResponseEntity<DadosEnderecoView> BuscaPorId(@PathVariable("id") Long id) {
        Optional<Endereco> registro = service.BuscarPorId(id);
        return registro.map(cidade -> ResponseEntity.ok().body(new DadosEnderecoView(cidade))).orElseGet(() -> ResponseEntity.notFound().build());
    }


    @CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
    @GetMapping("/medicopagina")
    public Page<DadosEnderecoView> BuscarPorPaginas(@PageableDefault(size = 12, sort = { "CIDCódigo" }) Pageable paginacao) {
        return service.BuscarPorPaginas(paginacao);
    }


    @CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
    @GetMapping(value = "/listatodosmedicos")
    public List<Endereco> buscarTodosEnderecos() {
        return 	 service.buscarTodosEndereco();
    }



    @CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
    @DeleteMapping("/{id}" )
    public ResponseEntity<Void> deleteById(@PathVariable("id") Long id)  throws  Exception{
        service.deletarPorId(id);
        return ResponseEntity.noContent().build();
    }
}
