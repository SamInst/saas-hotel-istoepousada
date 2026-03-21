package saas.hotel.istoepousada.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import saas.hotel.istoepousada.dto.Funcionario;
import saas.hotel.istoepousada.security.RequireTela;
import saas.hotel.istoepousada.service.FuncionarioService;

@RestController
@RequestMapping("/funcionario")
@RequireTela("ADMIN")
public class FuncionarioController {
  private final FuncionarioService funcionarioService;

  public FuncionarioController(FuncionarioService funcionarioService) {
    this.funcionarioService = funcionarioService;
  }

  @GetMapping
  public Page<Funcionario> listar(
      @RequestParam(required = false) Long id,
      @RequestParam(required = false) String termo,
      @RequestParam(required = false) Long cargoId,
      @RequestParam(required = false) Long pessoaId,
      @RequestParam(required = false) Long usuarioId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {
    Pageable pageable = PageRequest.of(page, size);
    return funcionarioService.search(id, termo, cargoId, pessoaId, usuarioId, pageable);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Funcionario criar(@RequestBody Funcionario.Request request) {
    return funcionarioService.create(request);
  }

  @PutMapping
  public Funcionario atualizar(@RequestBody Funcionario.Update funcionario) {
    return funcionarioService.update(funcionario);
  }
}
