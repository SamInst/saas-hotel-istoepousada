package saas.hotel.istoepousada.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import saas.hotel.istoepousada.dto.Empresa;
import saas.hotel.istoepousada.security.RequireTela;
import saas.hotel.istoepousada.service.EmpresaService;

@RestController
@RequestMapping("/empresa")
@RequireTela("CADASTRO")
public class EmpresaController {
  private final EmpresaService empresaService;

  public EmpresaController(EmpresaService empresaService) {
    this.empresaService = empresaService;
  }

  @GetMapping
  public Page<Empresa> listar(
      @RequestParam(required = false) Long id,
      @RequestParam(required = false) String termo,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {
    Pageable pageable = PageRequest.of(page, size);
    return empresaService.buscarPorIdNomeOuCnpj(id, termo, pageable);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Empresa criar(@RequestBody Empresa.Request empresa) {
    return empresaService.salvar(empresa);
  }

  @PutMapping
  public Empresa update(@RequestBody Empresa.Update empresa) {
    return empresaService.update(empresa);
  }

  @PutMapping("/vincular-pessoa")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void vincularOuDesvincularPessoa(@RequestBody Empresa.Vincular vincular) {
    empresaService.vincularPessoa(vincular);
  }
}
