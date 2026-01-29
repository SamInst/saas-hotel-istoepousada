package saas.hotel.istoepousada.handler.exceptions;

public class UnavaiableException extends RuntimeException {
  public UnavaiableException(String message) {
    super(message);
  }

  public UnavaiableException(String message, Throwable cause) {
    super(message, cause);
  }
}
