package br.com.saudeConecta.application.endereco.port;

/** O serviço de CEP não respondeu a tempo ou respondeu com erro: falha externa, não da aplicação. */
public class CepIndisponivelException extends RuntimeException {

    /**
     * @param mensagem motivo, só para diagnóstico (não vai para o cliente)
     * @param causa    erro original do cliente HTTP
     */
    public CepIndisponivelException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
