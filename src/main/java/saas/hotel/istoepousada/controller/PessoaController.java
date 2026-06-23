package saas.hotel.istoepousada.controller;

import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

  /**
   * Busca pública por CPF exato — usada pelo formulário de auto-cadastro. Retorna apenas a projeção
   * mínima ({@link Pessoa.AutoCadastro}), nunca a busca genérica por nome/termo (que exige
   * autenticação), evitando enumeração de dados pessoais.
   *
   * <ul>
   *   <li>200 + corpo → CPF já cadastrado, campos para preencher
   *   <li>204 sem corpo → CPF válido e ainda não cadastrado
   *   <li>400 → CPF inválido
   * </ul>
   */
  @GetMapping("/cpf/{cpf}")
  @PublicEndpoint
  public ResponseEntity<Pessoa.AutoCadastro> buscarPorCpfPublico(@PathVariable String cpf) {
    if (!PessoaService.cpfValido(cpf)) {
      return ResponseEntity.badRequest().build();
    }
    Pessoa.AutoCadastro pessoa = pessoaService.buscarPorCpf(cpf);
    return pessoa == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(pessoa);
  }

  @GetMapping
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

  /**
   * Auto-cadastro público (upsert por CPF). Único endpoint de escrita exposto sem autenticação.
   *
   * <ul>
   *   <li>O registro alvo é resolvido pelo CPF no servidor — o cliente não envia {@code id}, então
   *       não é possível sobrescrever o cadastro de terceiros por id (sem IDOR).
   *   <li>{@code status}, {@code funcionario}, {@code titular} e {@code empresas} são controlados
   *       pelo servidor (sem mass-assignment).
   * </ul>
   */
  @PostMapping("/auto-cadastro")
  @PublicEndpoint
  public ResponseEntity<?> autoCadastro(@RequestBody Pessoa.AutoCadastroRequest request) {
    if (request == null || !PessoaService.cpfValido(request.cpf())) {
      return ResponseEntity.badRequest().body(Map.of("message", "CPF inválido."));
    }
    try {
      return ResponseEntity.ok(pessoaService.autoCadastro(request));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public List<Pessoa> criar(@RequestBody Pessoa.BatchRequest request) {
    return pessoaService.salvarListaPessoas(request);
  }

  @PutMapping
  public Pessoa atualizar(@RequestBody Pessoa.Update pessoa) {
    return pessoaService.atualizarPessoa(pessoa);
  }

  @PutMapping("/vincular-titular")
  public void vincularTitular(@RequestBody Pessoa.VinculoTitular vinculo) {
    pessoaService.vincularTitular(vinculo);
  }
}
