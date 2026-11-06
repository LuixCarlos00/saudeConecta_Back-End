package br.com.saudeConecta.application.endereco.port;

/** O serviço de CEP respondeu, mas o CEP não existe ou veio sem logradouro e sem localidade. */
public class CepNaoEncontradoException extends RuntimeException {

    /**
     * @param mensagem motivo, só para diagnóstico (não vai para o cliente)
     */
    public CepNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
