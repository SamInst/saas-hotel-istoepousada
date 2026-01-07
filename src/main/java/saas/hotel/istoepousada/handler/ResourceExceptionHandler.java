package saas.hotel.istoepousada.handler;

import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Objects;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import saas.hotel.istoepousada.handler.exceptions.*;

@ControllerAdvice
@Order(HIGHEST_PRECEDENCE)
public class ResourceExceptionHandler {
  static final String INTERNAL_SERVER_ERROR = "Erro interno no servidor";
  static final String BAD_SQL_GRAMAR = "SQL COM ERRO DE SINTAXE.";
  static final String UNAUTHORIZED = "Não autorizado";
  static final String CONFLICT = "Conflito";
  static final String NOT_FOUND = "Entidade não encontrada";
  static final String UNAVAIABLE = "Entidade não disponível";
  static final String TIME_OUT = "Tempo de Requisicao Esgotado";
  static final String ARGUMENTO_INVALIDO = "Argumento Invalido";
  static final String NULL_POINTER = "Exceção de ponteiro NULL";

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<Object> errorInternalServerErrorKeyException(
      NotFoundException e, HttpServletRequest request) {
    var error =
        new StandardError(
            Instant.now(),
            HttpStatus.NOT_FOUND.value(),
            NOT_FOUND,
            e.getMessage(),
            request.getRequestURI());
    e.printStackTrace();
    return ResponseEntity.status(HttpStatus.NOT_FOUND.value()).body(error);
  }

  @ExceptionHandler(UnauthorizedException.class)
  public ResponseEntity<Object> errorInternalServerErrorKeyException(
      UnauthorizedException e, HttpServletRequest request) {
    var error =
        new StandardError(
            Instant.now(),
            HttpStatus.UNAUTHORIZED.value(),
            UNAUTHORIZED,
            e.getMessage(),
            request.getRequestURI());
    e.printStackTrace();
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED.value()).body(error);
  }

  @ExceptionHandler(ConflictException.class)
  public ResponseEntity<Object> errorInternalServerErrorKeyException(
      ConflictException e, HttpServletRequest request) {
    var error =
        new StandardError(
            Instant.now(),
            HttpStatus.CONFLICT.value(),
            CONFLICT,
            e.getMessage(),
            request.getRequestURI());
    e.printStackTrace();
    return ResponseEntity.status(HttpStatus.CONFLICT.value()).body(error);
  }

  @ExceptionHandler(UnavaiableException.class)
  public ResponseEntity<Object> errorInternalServerErrorKeyException(
      UnavaiableException e, HttpServletRequest request) {
    var error =
        new StandardError(
            Instant.now(),
            HttpStatus.SERVICE_UNAVAILABLE.value(),
            UNAVAIABLE,
            e.getMessage(),
            request.getRequestURI());
    e.printStackTrace();
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE.value()).body(error);
  }

  @ExceptionHandler(TimeOutException.class)
  public ResponseEntity<Object> errorInternalServerErrorKeyException(
      TimeOutException e, HttpServletRequest request) {
    var error =
        new StandardError(
            Instant.now(),
            HttpStatus.REQUEST_TIMEOUT.value(),
            TIME_OUT,
            e.getMessage(),
            request.getRequestURI());
    e.printStackTrace();
    return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT.value()).body(error);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Object> errorInternalServerErrorKeyException(
      IllegalArgumentException e, HttpServletRequest request) {
    var error =
        new StandardError(
            Instant.now(),
            HttpStatus.BAD_REQUEST.value(),
            ARGUMENTO_INVALIDO,
            e.getMessage(),
            request.getRequestURI());
    e.printStackTrace();
    return ResponseEntity.status(HttpStatus.BAD_REQUEST.value()).body(error);
  }

  @ExceptionHandler(BadSqlGrammarException.class)
  public ResponseEntity<Object> errorInternalServerErrorKeyException(
      BadSqlGrammarException e, HttpServletRequest request) {
    var error =
        new StandardError(
            Instant.now(),
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            INTERNAL_SERVER_ERROR,
            BAD_SQL_GRAMAR + " " + Objects.requireNonNull(e.getSQLException()).getMessage(),
            request.getRequestURI());
    e.printStackTrace();
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR.value()).body(error);
  }

  @ExceptionHandler(NullPointerException.class)
  public ResponseEntity<Object> errorInternalServerErrorKeyException(
      NullPointerException e, HttpServletRequest request) {
    var error =
        new StandardError(
            Instant.now(),
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            NULL_POINTER,
            e.getMessage(),
            request.getRequestURI());
    e.printStackTrace();
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR.value()).body(error);
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<String> handleMediaTypeNotSupportedException(
      HttpMediaTypeNotSupportedException ex) {
    return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body("Unsupported Media Type");
  }
}
