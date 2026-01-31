package br.com.saudeConecta.application.usecase.administrador;

import br.com.saudeConecta.domain.administrador.Administrador;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface BuscarTodosAdministradoresUseCase {

    List<Administrador> executar();
    
    Page<Administrador> executar(Pageable pageable);
}
