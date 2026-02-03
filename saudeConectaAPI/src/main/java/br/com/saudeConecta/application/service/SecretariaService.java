package br.com.saudeConecta.application.service;

import br.com.saudeConecta.domain.secretaria.Secretaria;
import br.com.saudeConecta.domain.usuario.Usuario;
import br.com.saudeConecta.email.EnviarService.CredenciaisEmailService;
import br.com.saudeConecta.infrastructure.persistence.repository.SecretariaRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.UsuarioRepository;
import br.com.saudeConecta.presentation.dto.secretaria.CadastrarSecretariaCompletoRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SecretariaService {

    private final SecretariaRepository secretariaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final CredenciaisEmailService credenciaisEmailService;
    
    private static final String CARACTERES_SENHA = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#$%&*";
    private static final int TAMANHO_SENHA = 10;

    public Optional<Secretaria> buscarPorId(Long id) {
        log.debug("Buscando secretária por ID: {}", id);
        return secretariaRepository.findById(id);
    }

    public Optional<Secretaria> buscarPorIdUsuario(Long usuarioId) {
        log.debug("Buscando secretária por ID de usuário: {}", usuarioId);
        return secretariaRepository.findBySecreUsuario_Id(usuarioId);
    }

    public List<Secretaria> buscarTodos() {
        log.debug("Buscando todas as secretárias");
        return secretariaRepository.findAll();
    }

    public Secretaria cadastrar(Secretaria secretaria) {
        log.info("Cadastrando nova secretária: {}", secretaria.getSecreNome());
        return secretariaRepository.save(secretaria);
    }

    public void deletar(Long id) {
        log.info("Deletando secretária ID: {}", id);
        secretariaRepository.deleteById(id);
    }

    /**
     * Cadastra secretária completa: cria usuário com CPF como login, gera senha com BCrypt e envia por email.
     */
    public Secretaria cadastrarCompleto(CadastrarSecretariaCompletoRequest dados) {
        log.info("Cadastrando secretária completa: {} com CPF: {}", dados.secreNome(), dados.secreCpf());
        
        String cpfLimpo = limparCpf(dados.secreCpf());
        
        if (usuarioRepository.existsByLogin(cpfLimpo)) {
            log.warn("CPF já cadastrado como login: {}", cpfLimpo);
            throw new IllegalStateException("CPF já cadastrado no sistema");
        }
        
        String senhaGerada = gerarSenhaAleatoria();
        String senhaCriptografada = passwordEncoder.encode(senhaGerada);
        
        Usuario usuario = new Usuario();
        usuario.setLogin(cpfLimpo);
        usuario.setSenha(senhaCriptografada);
        usuario.setTipoUsuario((byte) 2); // 2 = Secretária
        usuario.setStatus((byte) 1);
        Usuario usuarioSalvo = usuarioRepository.save(usuario);
        log.info("Usuário criado para secretária. ID: {}", usuarioSalvo.getId());
        
        Secretaria secretaria = new Secretaria();
        secretaria.setSecreNome(dados.secreNome());
        secretaria.setSecreEmail(dados.secreEmail());
        secretaria.setSecreCodigoAtorizacao(dados.secreCodigoAutorizacao());
        secretaria.setSecreStatus((byte) 1);
        secretaria.setSecreDataCriacao(Date.valueOf(LocalDate.now()));
        secretaria.setSecreUsuario(usuarioSalvo);
        
        Secretaria secretariaSalva = secretariaRepository.save(secretaria);
        log.info("Secretária cadastrada com sucesso. ID: {}", secretariaSalva.getSecreCodigo());
        
        credenciaisEmailService.enviarCredenciaisSecretaria(dados.secreEmail(), dados.secreNome(), cpfLimpo, senhaGerada);
        
        return secretariaSalva;
    }
    
    private String gerarSenhaAleatoria() {
        SecureRandom random = new SecureRandom();
        StringBuilder senha = new StringBuilder(TAMANHO_SENHA);
        for (int i = 0; i < TAMANHO_SENHA; i++) {
            int index = random.nextInt(CARACTERES_SENHA.length());
            senha.append(CARACTERES_SENHA.charAt(index));
        }
        return senha.toString();
    }
    
    private String limparCpf(String cpf) {
        return cpf.replaceAll("[^0-9]", "");
    }
}
