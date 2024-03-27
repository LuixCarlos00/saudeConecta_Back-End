package br.com.saudeConecta.infra.exceptions;


import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.constraints.NotNull;
import jdk.jfr.Name;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

@RestControllerAdvice
public class TratadorDeErros {


    @Name("Funcionalidade do teste - #T01#  " +
            "- captura exceção do tipo  POST   de uma entidade que tem  relacionamento com outra entidade. Primeiramente e feito uma busca no repositorio," +
            "se essa entitdade relacionada exister é feito o cadastro , se NAO existir lanca o erro.")
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<String> TrataErroResourceNotFoundException(@NotNull ResourceNotFoundException ex) {
        String mensageErro = ex.getMessage();
        return new ResponseEntity<>("Erro de validade. # -TratadorDeErros - T01# " + mensageErro, HttpStatus.BAD_REQUEST);
    }

    @Name("Funcionalidade do teste - #T02# " +
            "- captura exceção do tipo  GET  de uma entidade nao encontrada no banco de dados ")
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<String> TratarErro404(EntityNotFoundException ex) {
        return new ResponseEntity<>("Entidade não encontrada. # -TratadorDeErros - T02# ", HttpStatus.NOT_FOUND);
    }

    @Name("Funcionalidade do teste - #T03# " +
            "- captura exceção quando houver uma requisicao do tipo POST com chave duplicada (mais usado em tabelas compotas )")
    @ExceptionHandler(SQLException.class)
    public ResponseEntity<String> tratarErroIntegridade(@NotNull SQLException ex) {
        String mensagemErro = ex.getMessage() + " # -TratadorDeErros - T03# ";
        return new ResponseEntity<>(mensagemErro, HttpStatus.BAD_REQUEST);
    }

    @Name("Funcionalidade do teste - #T04# " +
            "- captura exceção do tipo   GET  quando houver uma requisicao passando uma entidade nao existente  ")
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<String> ErroValorInexisteteNoBanco(NoSuchElementException ex) {
        return new ResponseEntity<>("Entidade não é está presente no banco de dados. # -TratadorDeErros - T04# ", HttpStatus.NOT_FOUND);
    }

    @Name("Funcionalidade do teste  - #T05#  " +
            "- captura exceção do tipo   GET  quando houver uma requisicao passando o ID  vazio ou  uma String  ou long negativo ")
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<String> tratarErrodeParametroInvalido(@NotNull MethodArgumentTypeMismatchException ex) {
        return new ResponseEntity<>("O valor fornecido não é válido para o parâmetro: " + ex.getName() + " # -TratadorDeErros - T05# ", HttpStatus.BAD_REQUEST);
    }


    @Name("Funcionalidade do teste - #T06#  " +
            "- captura exceção do tipo   POST  quando houver uma requisicao passando os campos vazio ")
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Map<String, String> ValidadoErros(@NotNull MethodArgumentNotValidException ex) {

        Map<String, String> erros = new HashMap<>();

        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String nomeErro = ((FieldError) error).getField();
            String ErroMensagem = error.getDefaultMessage() + "  # -TratadorDeErros - T06#  ";
            erros.put(nomeErro, ErroMensagem);
        });
        return erros;
    }


    @Name("Funcionalidade do teste - #T07# - captura exceção do tipo POST quando houver uma requisição passando uma String ',' ou JSON  vazio  ")
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<String> TratarErroJsonInvalido(@NotNull HttpMessageNotReadableException ex) {
        String errorMessage = "Erro na requisição: JSON parse error. Causa: " + ex.getMessage() + " - TratadorDeErros - T07";
        return new ResponseEntity<>(errorMessage, HttpStatus.BAD_REQUEST);
    }


//    @Name("Funcionalidade do teste - #T08# - captura exceção quando o Token nao é valido ")
//    @ExceptionHandler(JWTCreationException.class)
//    public ResponseEntity<String> tratarErroTokenInvalido(@NotNull JWTCreationException ex) {
//        String errorMessage = "Erro na requisição: Token invalido  Causa: " + ex.getMessage() + " - TratadorDeErros - T08";
//        return new ResponseEntity<>(errorMessage, HttpStatus.UNAUTHORIZED);
//    }
//
//
//    @Name("Funcionalidade do teste - #T09# - captura exceção quando o Token nao é valido ")
//    @ExceptionHandler(TokenExpiredException.class)
//    public ResponseEntity<String> tratarErroTokenExpirado(@NotNull TokenExpiredException ex) {
//        String errorMessage = "Erro na requisição: Token Expirado  Causa: " + ex.getMessage() + " - TratadorDeErros - T09";
//        return new ResponseEntity<>(errorMessage, HttpStatus.UNAUTHORIZED);
//    }

}
