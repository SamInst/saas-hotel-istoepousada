package saas.hotel.istoepousada.controller;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import saas.hotel.istoepousada.dto.Relatorio;
import saas.hotel.istoepousada.dto.RelatorioHistorico;
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
    return relatorioService.buscar(
        id,
        data_inicio,
        data_fim,
        funcionario_id,
        quarto_id,
        tipo_pagamento_id,
        registro,
        despesa_pessoal,
        page,
        size);
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public Relatorio criar(
      @RequestPart("relatorio") Relatorio.Request relatorio,
      @RequestPart(value = "arquivo", required = false) MultipartFile arquivo)
      throws IOException {
    return relatorioService.criar(relatorio, arquivo);
  }

  @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public Relatorio atualizar(
      @RequestPart("relatorio") Relatorio.Update relatorio,
      @RequestPart(value = "arquivo", required = false) MultipartFile arquivo)
      throws IOException {
    return relatorioService.atualizar(relatorio, arquivo);
  }

  @GetMapping("/{id}/historico")
  public List<RelatorioHistorico> historico(@PathVariable Long id) {
    return relatorioService.buscarHistorico(id);
  }
}
