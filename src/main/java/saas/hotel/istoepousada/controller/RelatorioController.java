package saas.hotel.istoepousada.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import saas.hotel.istoepousada.dto.Relatorio;
import saas.hotel.istoepousada.security.RequireTela;
import saas.hotel.istoepousada.service.RelatorioService;

@RestController
@RequestMapping("/relatorio")
@RequireTela("FINANCEIRO")
public class RelatorioController {

  private final RelatorioService relatorioService;

  public RelatorioController(RelatorioService relatorioService) {
    this.relatorioService = relatorioService;
  }

  @GetMapping
  public Relatorio.Extrato listar(@RequestBody Relatorio.Buscar relatorio) {
    return relatorioService.buscar(relatorio);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Relatorio criar(@RequestBody Relatorio.Request request) {
    return relatorioService.criar(request);
  }

  @PutMapping
  public Relatorio atualizar(@RequestBody Relatorio.Update relatorio) {
    return relatorioService.atualizar(relatorio);
  }
}
