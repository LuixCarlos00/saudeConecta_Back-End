package br.com.saudeConecta.application.usecase.consulta;

import br.com.saudeConecta.domain.consulta.Consulta;

import java.util.List;

public interface BuscarTodasConsultasUseCase {

    List<Consulta> executar();
}
