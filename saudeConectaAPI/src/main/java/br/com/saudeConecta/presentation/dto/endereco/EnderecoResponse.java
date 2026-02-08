package br.com.saudeConecta.presentation.dto.endereco;

import br.com.saudeConecta.domain.endereco.Endereco;
import com.fasterxml.jackson.annotation.JsonProperty;

public record EnderecoResponse(
        Long codigo,
        String nacionalidade,
        String uf,
        String municipio,
        String bairro,
        String cep,
        String rua,
        Long numero,
        String complemento
) {
    // Construtor a partir da entidade (para leitura)
    public EnderecoResponse(Endereco endereco) {
        this(
                endereco.getEndCodigo(),
                endereco.getEndNacionalidade(),
                endereco.getEndUF(),
                endereco.getEndMunicipio(),
                endereco.getEndBairro(),
                endereco.getEndCep(),
                endereco.getEndRua(),
                endereco.getEndNumero(),
                endereco.getEndComplemento()
        );
    }

    // Método para converter Response em Entity (para escrita)
    public Endereco toEntity() {
        return Endereco.builder()
                .endCodigo(this.codigo)
                .endNacionalidade(this.nacionalidade)
                .endUF(this.uf)
                .endMunicipio(this.municipio)
                .endBairro(this.bairro)
                .endCep(this.cep)
                .endRua(this.rua)
                .endNumero(this.numero)
                .endComplemento(this.complemento)
                .build();
    }

    // Método para atualizar uma entidade existente
    public void updateEntity(Endereco endereco) {
        // Não atualiza o código (ID) - mantém o existente
        endereco.setEndNacionalidade(this.nacionalidade);
        endereco.setEndUF(this.uf);
        endereco.setEndMunicipio(this.municipio);
        endereco.setEndBairro(this.bairro);
        endereco.setEndCep(this.cep);
        endereco.setEndRua(this.rua);
        endereco.setEndNumero(this.numero);
        endereco.setEndComplemento(this.complemento);
    }
}