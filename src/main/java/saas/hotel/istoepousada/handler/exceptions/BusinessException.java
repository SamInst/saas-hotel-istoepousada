package saas.hotel.istoepousada.handler.exceptions;

public class BusinessException extends RuntimeException {
  public BusinessException(String mensagem) {
    super(mensagem);
  }
}
