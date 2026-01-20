package saas.hotel.istoepousada.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import saas.hotel.istoepousada.dto.FuncionarioAuth;
import saas.hotel.istoepousada.dto.Login;
import saas.hotel.istoepousada.dto.LoginResponse;
import saas.hotel.istoepousada.service.AuthService;

@Tag(name = "Autenticação", description = "Endpoints de autenticação JWT")
@RestController
@RequestMapping("/auth")
public class AuthController {
  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @Operation(
      summary = "Login",
      description = "Autentica um funcionário e retorna um token JWT com seus dados e permissões")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Login bem-sucedido",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = LoginResponse.class))),
    @ApiResponse(
        responseCode = "401",
        description = "Credenciais inválidas",
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
  })
  @PostMapping("/login")
  public LoginResponse login(
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "Credenciais de login",
              required = true,
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      schema = @Schema(implementation = Login.class),
                      examples =
                          @ExampleObject(
                              name = "Exemplo",
                              value =
                                  """
                                {
                                  "username": "joao.silva2",
                                  "senha": "senha123"
                                }
                                """)))
          @RequestBody
          Login request) {
    return authService.login(request);
  }

  @Operation(
      summary = "Validar token",
      description = "Valida um token JWT e retorna os dados do funcionário")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Token válido",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = FuncionarioAuth.class))),
    @ApiResponse(responseCode = "401", description = "Token inválido")
  })
  @GetMapping("/validate")
  public FuncionarioAuth validarToken(@RequestHeader("Authorization") String authHeader) {
    String token = authHeader.replace("Bearer ", "");
    return authService.validarToken(token);
  }
}
