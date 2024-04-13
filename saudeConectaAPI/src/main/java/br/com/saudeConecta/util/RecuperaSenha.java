package br.com.saudeConecta.util;

import br.com.saudeConecta.endpoinst.codigoVerificacao.Entity.CodigoVerificacao;
import br.com.saudeConecta.endpoinst.codigoVerificacao.Repository.CodigoVerificacaoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class RecuperaSenha {

    @Autowired
    private CodigoVerificacaoRepository codigoVerificacaoRepository;



    private final JdbcTemplate jdbcTemplate;



    public RecuperaSenha(CodigoVerificacaoRepository codigoVerificacaoRepository, JdbcTemplate jdbcTemplate) {
        this.codigoVerificacaoRepository = codigoVerificacaoRepository;
        this.jdbcTemplate = jdbcTemplate;
    }


    public String gerarCodigoVerificacaoTabelaUsuarios() {
        String codigo;
        boolean codigoExistente;
        do {
            codigo = UUID.randomUUID().toString().substring(0, 6);
            codigoExistente = existeEsseCodigoNoBanco(codigo);
        } while (codigoExistente);
        cadastraCodigoValidacao(codigo);
        return codigo;
    }

    public boolean existeEsseCodigoNoBanco(String codigo) {
        return codigoVerificacaoRepository != null && codigoVerificacaoRepository.existsByCodVerificacaoCodigo(codigo);
    }

    public void cadastraCodigoValidacao(String codigo) {
        CodigoVerificacao codigoVerificacao = new CodigoVerificacao();
        LocalDateTime tempo = LocalDateTime.now();

        codigoVerificacao.setCodVerificacaoCodigo(codigo);
        codigoVerificacao.setCodVerificacaoTempo(tempo);

        codigoVerificacaoRepository.save(codigoVerificacao);

    }

}

