package br.com.saudeConecta.application.service;

import br.com.saudeConecta.domain.secretaria.Secretaria;
import br.com.saudeConecta.domain.usuario.Usuario;
import br.com.saudeConecta.email.EnviarService.CredenciaisEmailService;
import br.com.saudeConecta.infrastructure.persistence.repository.SecretariaRepository;
import br.com.saudeConecta.infrastructure.persistence.repository.UsuarioRepository;
import br.com.saudeConecta.presentation.dto.secretaria.AtualizarSecretariaRequest;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
@RequiredArgsConstructor
@Slf4j
public class SecretariaService {

    private final SecretariaRepository secretariaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final CredenciaisEmailService credenciaisEmailService;
    private final Executor emailTaskExecutor;
    
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

    public void deletar(Long id) throws Exception {
        log.info("Iniciando exclusão da secretária ID: {}", id);

        if (id == null || id <= 0) {
            log.warn("Tentativa de exclusão com ID inválido: {}", id);
            throw new IllegalArgumentException("ID inválido");
        }

        var secretariaOpt = secretariaRepository.findById(id);
        if (secretariaOpt.isEmpty()) {
            log.warn("Secretária não encontrada para exclusão ID: {}", id);
            throw new Exception("Registro não encontrado");
        }

        Secretaria secretaria = secretariaOpt.get();
        Usuario usuario = secretaria.getSecreUsuario();

        try {
            // 1. Deletar a secretária primeiro (remove a FK)
            secretariaRepository.deleteById(id);
            log.info("Secretária ID: {} excluída com sucesso", id);

            // 2. Deletar o usuário associado
            if (usuario != null) {
                usuarioRepository.deleteById(usuario.getId());
                log.info("Usuário ID: {} associado à secretária excluído com sucesso", usuario.getId());
            }
        } catch (Exception e) {
            log.error("Erro ao excluir secretária ID: {}", id, e);
            throw new Exception("Violação de Integridade", e);
        }
    }

    /**
     * Cadastra secretária completa: cria usuário com CPF como login, gera senha com BCrypt e envia por email.
     */
    public Secretaria cadastrarCompleto(CadastrarSecretariaCompletoRequest dados) {
        log.info("Iniciando cadastro de secretária: {}", dados.secreNome());
        
        String cpfLimpo = limparCpf(dados.secreCpf());
        
        // Verificação rápida de CPF existente
        if (usuarioRepository.existsByLogin(cpfLimpo)) {
            log.warn("CPF já cadastrado: {}", cpfLimpo);
            throw new IllegalStateException("CPF já cadastrado no sistema");
        }
        
        // Geração de senha
        String senhaGerada = gerarSenhaAleatoria();
        String senhaCriptografada = passwordEncoder.encode(senhaGerada);
        
        // Criação do usuário
        Usuario usuario = new Usuario();
        usuario.setLogin(cpfLimpo);
        usuario.setSenha(senhaCriptografada);
        usuario.setTipoUsuario((byte) 2);
        usuario.setStatus((byte) 1);
        
        // Criação da secretária
        Secretaria secretaria = new Secretaria();
        secretaria.setSecreNome(dados.secreNome());
        secretaria.setSecreEmail(dados.secreEmail());
        secretaria.setSecreCodigoAtorizacao(dados.secreCodigoAutorizacao());
        secretaria.setSecreStatus((byte) 1);
        secretaria.setSecreDataCriacao(Date.valueOf(LocalDate.now()));
        
        // Salvar usuário primeiro para obter o ID
        Usuario usuarioSalvo = usuarioRepository.save(usuario);
        secretaria.setSecreUsuario(usuarioSalvo);
        
        // Salvar secretária
        Secretaria secretariaSalva = secretariaRepository.save(secretaria);
        
        log.info("Secretária cadastrada com sucesso. ID: {}", secretariaSalva.getSecreCodigo());
        
        // Envio de email assíncrono otimizado
        CompletableFuture.runAsync(() -> {
            try {
                credenciaisEmailService.enviarCredenciaisSecretaria(dados.secreEmail(), dados.secreNome(), cpfLimpo, senhaGerada);
                log.info("Email enviado para: {}", dados.secreEmail());
            } catch (Exception e) {
                log.error("Erro ao enviar email para: {}", dados.secreEmail(), e);
            }
        }, emailTaskExecutor);
        
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

    /**
     * Atualiza os dados de uma secretária existente.
     * @param id ID da secretária
     * @param dados DTO com dados atualizados
     * @return Secretaria atualizada
     * @throws IllegalArgumentException se secretária não for encontrada
     */
    public Secretaria atualizar(Long id, AtualizarSecretariaRequest dados) {
        log.info("Atualizando secretária ID: {}", id);
        
        var secretariaOpt = buscarPorId(id);
        if (secretariaOpt.isEmpty()) {
            log.warn("Secretária não encontrada para atualização: {}", id);
            throw new IllegalArgumentException("Secretária não encontrada");
        }
        
        Secretaria secretaria = secretariaOpt.get();
        
        // Atualiza dados da secretária
        if (dados.secreNome() != null) secretaria.setSecreNome(dados.secreNome());
        if (dados.secreEmail() != null) secretaria.setSecreEmail(dados.secreEmail());
        if (dados.secreCodigoAutorizacao() != null) secretaria.setSecreCodigoAtorizacao(dados.secreCodigoAutorizacao());
        
        Secretaria secretariaAtualizada = secretariaRepository.save(secretaria);
        log.info("Secretária ID: {} atualizada com sucesso", id);
        
        return secretariaAtualizada;
    }
}
