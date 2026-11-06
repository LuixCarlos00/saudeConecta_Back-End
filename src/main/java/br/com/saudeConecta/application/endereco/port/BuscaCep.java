package br.com.saudeConecta.application.endereco.port;

/**
 * Porta para o serviço externo de CEP (hoje o ViaCEP, em {@code infrastructure/viacep}).
 * Existe para o controller não depender do adaptador de saída (regra 2 do ArchUnit) e para o
 * serviço poder ser trocado ou simulado em teste.
 */
public interface BuscaCep {

    /**
     * Busca o endereço de um CEP.
     *
     * @param cep CEP com 8 dígitos, só números
     * @return os dados do endereço como o serviço devolveu (repassados ao cliente sem conversão)
     * @throws CepNaoEncontradoException quando o serviço diz que o CEP não existe ou volta sem
     *         logradouro e sem localidade
     * @throws CepIndisponivelException  quando o serviço está fora do ar, lento além do timeout ou
     *         responde com erro
     */
    Object buscar(String cep);
}
