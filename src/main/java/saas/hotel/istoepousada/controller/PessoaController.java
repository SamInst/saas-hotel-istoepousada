package saas.hotel.istoepousada.controller;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import saas.hotel.istoepousada.dto.Pessoa;
import saas.hotel.istoepousada.security.PublicEndpoint;
import saas.hotel.istoepousada.security.RequireTela;
import saas.hotel.istoepousada.service.PessoaService;

@RestController
@RequestMapping("/pessoa")
@RequireTela("CADASTRO")
public class PessoaController {
  private final PessoaService pessoaService;

  public PessoaController(PessoaService pessoaService) {
    this.pessoaService = pessoaService;
  }

  @GetMapping
  @PublicEndpoint
  public Page<Pessoa> listar(
      @RequestParam(required = false) Long id,
      @RequestParam(required = false) String termo,
      @RequestParam(required = false) String placa,
      @RequestParam(required = false) Pessoa.Status status,
      @RequestParam(defaultValue = "NOME") Pessoa.Ordenacao ordenacao,
      @RequestParam(defaultValue = "ASC") Pessoa.Direcao direcao,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {
    Pageable pageable = PageRequest.of(page, size);
    return pessoaService.buscar(id, termo, placa, status, ordenacao, direcao, pageable);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PublicEndpoint
  public List<Pessoa> criar(@RequestBody Pessoa.BatchRequest request) {
    return pessoaService.salvarListaPessoas(request);
  }

  @PutMapping
  @PublicEndpoint
  public Pessoa atualizar(@RequestBody Pessoa.Update pessoa) {
    return pessoaService.atualizarPessoa(pessoa);
  }

  @PutMapping("/vincular-titular")
  public void vincularTitular(@RequestBody Pessoa.VinculoTitular vinculo) {
    pessoaService.vincularTitular(vinculo);
  }
}
