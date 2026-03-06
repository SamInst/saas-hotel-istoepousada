package saas.hotel.istoepousada.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response de autenticação com token JWT")
public record LoginResponse(
    @Schema(description = "Token JWT") String token,
    @Schema(description = "Tipo do token", example = "Bearer") String type) {

  public LoginResponse(String token) {
    this(token, "Bearer");
  }
}
