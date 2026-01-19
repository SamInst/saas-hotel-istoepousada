package saas.hotel.istoepousada.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request de login")
public record Login(
    @Schema(description = "Username do usuário", example = "joao.silva") String username,
    @Schema(description = "Senha do usuário", example = "senha123") String senha) {}
