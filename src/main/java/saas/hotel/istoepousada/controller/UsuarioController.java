package saas.hotel.istoepousada.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import saas.hotel.istoepousada.dto.Usuario;
import saas.hotel.istoepousada.security.RequireTela;
import saas.hotel.istoepousada.service.UsuarioService;

@Tag(name = "Usuários", description = "Endpoints de cadastro e consulta de usuários do sistema.")
@RestController
@RequestMapping("/usuario")
@RequireTela("CADASTRO")
public class UsuarioController {
  private final UsuarioService usuarioService;

  public UsuarioController(UsuarioService usuarioService) {
    this.usuarioService = usuarioService;
  }

  @Operation(
      summary = "Listar usuários (paginado) com filtros opcionais",
      description =
          """
                    Lista usuários paginados. Filtros são opcionais:
                    - id: busca específica por ID
                    - username: filtra por username (ILIKE)
                    - bloqueado=true: retorna somente usuários bloqueados

                    Se nenhum filtro for informado, retorna todos os usuários paginados.
                    """)
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Página de usuários",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = Page.class)))
  })
  @GetMapping
  public Page<Usuario.UsuarioResponse> listar(
      @Parameter(description = "ID do usuário") @RequestParam(required = false) Long id,
      @Parameter(description = "Username para busca (ILIKE)") @RequestParam(required = false)
          String username,
      @Parameter(description = "Se true, filtra apenas usuários bloqueados", example = "false")
          @RequestParam(required = false)
          Boolean bloqueado,
      @Parameter(description = "Número da página (0-based)", example = "0")
          @RequestParam(defaultValue = "0")
          int page,
      @Parameter(description = "Tamanho da página", example = "10")
          @RequestParam(defaultValue = "10")
          int size) {
    Pageable pageable = PageRequest.of(page, size);
    return usuarioService.buscar(id, username, bloqueado, pageable);
  }

  @Operation(
      summary = "Criar usuário",
      description = "Cria um novo usuário no sistema. A senha será armazenada em MD5.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "201",
        description = "Usuário criado",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = Usuario.UsuarioResponse.class))),
    @ApiResponse(responseCode = "400", description = "Requisição inválida ou username já existe")
  })
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Usuario.UsuarioResponse criar(
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "Dados do usuário",
              required = true,
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      examples =
                          @ExampleObject(
                              name = "Exemplo de requisição",
                              value =
                                  """
                                            {
                                              "username": "joao.silva",
                                              "senha": "senha123"
                                            }
                                            """)))
          @RequestBody
          Map<String, String> body) {
    String username = body.get("username");
    String senha = body.get("senha");
    return usuarioService.criar(username, senha);
  }

  @Operation(
      summary = "Alterar senha do usuário",
      description = "Altera a senha de um usuário existente. A nova senha será armazenada em MD5.")
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "Senha alterada com sucesso"),
    @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
  })
  @PatchMapping("/{id}/senha")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void alterarSenha(
      @Parameter(description = "ID do usuário", example = "1", required = true) @PathVariable
          Long id,
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "Nova senha",
              required = true,
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      examples =
                          @ExampleObject(
                              name = "Exemplo de requisição",
                              value =
                                  """
                                            {
                                              "senha": "novaSenha456"
                                            }
                                            """)))
          @RequestBody
          Map<String, String> body) {
    usuarioService.alterarSenha(id, body.get("senha"));
  }

  @Operation(
      summary = "Alterar status de bloqueio do usuário",
      description =
          """
                    Altera o status de bloqueio de um usuário.
                    - bloqueado=true: bloqueia o usuário, impedindo seu acesso ao sistema
                    - bloqueado=false: desbloqueia o usuário
                    """)
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "Status alterado com sucesso"),
    @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
  })
  @PatchMapping("/{id}/bloqueio")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void alterarStatusBloqueio(
      @Parameter(description = "ID do usuário", example = "1", required = true) @PathVariable
          Long id,
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "Status de bloqueio",
              required = true,
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      examples = {
                        @ExampleObject(
                            name = "Bloquear usuário",
                            value =
                                """
                                                    {
                                                      "bloqueado": true
                                                    }
                                                    """),
                        @ExampleObject(
                            name = "Desbloquear usuário",
                            value =
                                """
                                                    {
                                                      "bloqueado": false
                                                    }
                                                    """)
                      }))
          @RequestBody
          Map<String, Boolean> body) {
    usuarioService.alterarStatusBloqueio(id, body.get("bloqueado"));
  }

  @Operation(
      summary = "Autenticar usuário",
      description =
          """
                    Valida as credenciais de um usuário (username e senha).
                    Retorna sucesso se as credenciais forem válidas e o usuário não estiver bloqueado.
                    """)
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Autenticação bem-sucedida",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples =
                    @ExampleObject(
                        name = "Sucesso",
                        value =
                            """
                                            {
                                              "autenticado": true,
                                              "mensagem": "Login bem-sucedido"
                                            }
                                            """))),
    @ApiResponse(
        responseCode = "401",
        description = "Credenciais inválidas",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples =
                    @ExampleObject(
                        name = "Falha",
                        value =
                            """
                                            {
                                              "autenticado": false,
                                              "mensagem": "Credenciais inválidas"
                                            }
                                            """)))
  })
  @PostMapping("/autenticar")
  public Map<String, Object> autenticar(
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "Credenciais do usuário",
              required = true,
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      examples =
                          @ExampleObject(
                              name = "Exemplo de requisição",
                              value =
                                  """
                                            {
                                              "username": "joao.silva",
                                              "senha": "senha123"
                                            }
                                            """)))
          @RequestBody
          Map<String, String> body) {
    boolean autenticado = usuarioService.autenticar(body.get("username"), body.get("senha"));

    if (autenticado) {
      return Map.of("autenticado", true, "mensagem", "Login bem-sucedido");
    } else {
      return Map.of("autenticado", false, "mensagem", "Credenciais inválidas");
    }
  }
}
