package saas.hotel.istoepousada.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
@RequireTela("ADMIN")
public class UsuarioController {

  private final UsuarioService usuarioService;

  public UsuarioController(UsuarioService usuarioService) {
    this.usuarioService = usuarioService;
  }

  public record AutenticacaoRequest(String username, String senha) {}

  public record BloqueioRequest(Boolean bloqueado) {}

  public record AutenticacaoResponse(Boolean autenticado, String mensagem) {}

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
  public Page<Usuario> listar(
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

  @Operation(summary = "Buscar usuário por ID", description = "Retorna um usuário pelo ID.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Usuário encontrado",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = Usuario.class))),
    @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
  })
  @GetMapping("/{id}")
  public Usuario buscarPorId(
      @Parameter(description = "ID do usuário", example = "1", required = true) @PathVariable
          Long id) {
    return usuarioService.findById(id);
  }

  @Operation(summary = "Criar usuário", description = "Cria um novo usuário no sistema.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "201",
        description = "Usuário criado",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = Usuario.class))),
    @ApiResponse(responseCode = "400", description = "Requisição inválida ou username já existe")
  })
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Usuario criar(
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "Dados do usuário",
              required = true,
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      schema = @Schema(implementation = Usuario.Request.class),
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
          @Valid
          @RequestBody
          Usuario.Request request) {
    return usuarioService.create(request);
  }

  @Operation(
      summary = "Atualizar username do usuário",
      description = "Atualiza somente o username de um usuário existente.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Usuário atualizado",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = Usuario.class))),
    @ApiResponse(responseCode = "400", description = "Requisição inválida"),
    @ApiResponse(responseCode = "401", description = "Usuário bloqueado"),
    @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
  })
  @PutMapping
  public Usuario atualizar(
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "Dados para atualização do usuário",
              required = true,
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      schema = @Schema(implementation = Usuario.class),
                      examples =
                          @ExampleObject(
                              name = "Exemplo de requisição",
                              value =
                                  """
                                            {
                                              "id": 1,
                                              "username": "novo.username",
                                              "bloqueado": false
                                            }
                                            """)))
          @Valid
          @RequestBody
          Usuario usuario) {
    return usuarioService.update(usuario);
  }

  @Operation(
      summary = "Alterar username e senha do usuário",
      description = "Altera username e senha de um usuário existente.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Username e senha alterados com sucesso",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = Usuario.class))),
    @ApiResponse(responseCode = "400", description = "Requisição inválida ou username já existe"),
    @ApiResponse(responseCode = "401", description = "Usuário bloqueado"),
    @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
  })
  @PatchMapping("/credenciais")
  public Usuario alterarUsernameESenha(
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "Novas credenciais",
              required = true,
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      schema = @Schema(implementation = Usuario.Update.class),
                      examples =
                          @ExampleObject(
                              name = "Exemplo de requisição",
                              value =
                                  """
                                            {
                                              "id": 1,
                                              "username": "novo.username",
                                              "senha": "novaSenha456",
                                              "bloqueado": false
                                            }
                                            """)))
          @Valid
          @RequestBody
          Usuario.Update update) {
    return usuarioService.alterarUsernameESenha(update);
  }

  @Operation(
      summary = "Alterar status de bloqueio do usuário",
      description =
          """
                    Altera o status de bloqueio de um usuário.
                    - bloqueado=true: bloqueia o usuário
                    - bloqueado=false: desbloqueia o usuário
                    """)
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Status alterado com sucesso",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = Usuario.class))),
    @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
  })
  @PatchMapping("/{id}/bloqueio")
  public Usuario alterarStatusBloqueio(
      @Parameter(description = "ID do usuário", example = "1", required = true) @PathVariable
          Long id,
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "Status de bloqueio",
              required = true,
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      schema = @Schema(implementation = BloqueioRequest.class),
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
          BloqueioRequest request) {
    return usuarioService.bloquear(id, request.bloqueado());
  }

  @Operation(summary = "Autenticar usuário", description = "Valida as credenciais de um usuário.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Autenticação processada",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = AutenticacaoResponse.class),
                examples = {
                  @ExampleObject(
                      name = "Sucesso",
                      value =
                          """
                                                    {
                                                      "autenticado": true,
                                                      "mensagem": "Login bem-sucedido"
                                                    }
                                                    """),
                  @ExampleObject(
                      name = "Falha",
                      value =
                          """
                                                    {
                                                      "autenticado": false,
                                                      "mensagem": "Credenciais inválidas"
                                                    }
                                                    """)
                }))
  })
  @PostMapping("/autenticar")
  public AutenticacaoResponse autenticar(
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "Credenciais do usuário",
              required = true,
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      schema = @Schema(implementation = AutenticacaoRequest.class),
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
          AutenticacaoRequest request) {

    boolean autenticado = usuarioService.autenticar(request.username(), request.senha());

    if (autenticado) {
      return new AutenticacaoResponse(true, "Login bem-sucedido");
    }

    return new AutenticacaoResponse(false, "Credenciais inválidas");
  }
}
