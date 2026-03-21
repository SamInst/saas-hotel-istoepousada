package saas.hotel.istoepousada.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import saas.hotel.istoepousada.dto.Relatorio;
import saas.hotel.istoepousada.security.RequireTela;
import saas.hotel.istoepousada.service.RelatorioService;

import java.time.LocalDate;

@RestController
@RequestMapping("/relatorio")
@RequireTela("FINANCEIRO")
public class RelatorioController {

  private final RelatorioService relatorioService;

  public RelatorioController(RelatorioService relatorioService) {
    this.relatorioService = relatorioService;
  }

  @GetMapping
  public Relatorio.Extrato listar(
       @RequestParam(required = false) Long id,
       @RequestParam(required = false) LocalDate data_inicio,
       @RequestParam(required = false) LocalDate data_fim,
       @RequestParam(required = false) Long funcionario_id,
       @RequestParam(required = false) Long quarto_id,
       @RequestParam(required = false) Long tipo_pagamento_id,
       @RequestParam(required = false) Relatorio.Registro registro,
       @RequestParam(required = false) Boolean despesa_pessoal,
       @RequestParam(defaultValue = "0") int page,
       @RequestParam(defaultValue = "10") int size) {
    return relatorioService.buscar(id, data_inicio, data_fim, funcionario_id, quarto_id, tipo_pagamento_id, registro, despesa_pessoal, page, size);
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
