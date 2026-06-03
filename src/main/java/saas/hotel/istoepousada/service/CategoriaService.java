package saas.hotel.istoepousada.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import saas.hotel.istoepousada.dto.Categoria;
import saas.hotel.istoepousada.repository.CategoriaRepository;

@Service
public class CategoriaService {

  private final CategoriaRepository categoriaRepository;
  private final PessoaService pessoaService;

  public CategoriaService(CategoriaRepository categoriaRepository, PessoaService pessoaService) {
    this.categoriaRepository = categoriaRepository;
    this.pessoaService = pessoaService;
  }

  public Page<Categoria> buscar(Long id, String termo, Pageable pageable) {
    if (pageable == null) throw new IllegalArgumentException("Pageable é obrigatório.");
    String termoNorm = StringUtils.hasText(termo) ? termo.trim() : null;
    return categoriaRepository.buscar(id, termoNorm, pageable);
  }

  public Categoria buscarPorId(Long id) {
    if (id == null) throw new IllegalArgumentException("Id é obrigatório.");
    return categoriaRepository.findByIdOrThrow(id);
  }

  @Transactional
  public Categoria criar(Categoria.Request request) {
    validar(request.nome());
    validarSubDados(request);
    return categoriaRepository.insert(request, getFuncionarioId());
  }

  @Transactional
  public Categoria atualizar(Categoria.Update request) {
    if (request.id() == null) throw new IllegalArgumentException("Id é obrigatório.");
    validar(request.nome());
    validarSubDados(request);
    return categoriaRepository.update(request, getFuncionarioId());
  }

  private void validar(String nome) {
    if (!StringUtils.hasText(nome)) throw new IllegalArgumentException("Nome é obrigatório.");
  }

  private void validarSubDados(Categoria.Request request) {
    if (request.modelos_ocupacao() != null) {
      for (var mo : request.modelos_ocupacao()) {
        if (mo.quantidade() == null || mo.quantidade() <= 0)
          throw new IllegalArgumentException(
              "Quantidade do modelo de ocupação deve ser maior que 0.");
        if (mo.valor() == null || mo.valor() < 0)
          throw new IllegalArgumentException("Valor do modelo de ocupação não pode ser negativo.");
      }
    }
    if (request.modelos_fixo() != null) {
      for (var mf : request.modelos_fixo()) {
        if (mf.valor() == null || mf.valor() < 0)
          throw new IllegalArgumentException("Valor do modelo fixo não pode ser negativo.");
      }
    }
    if (request.day_use() != null) {
      for (var duo : request.day_use()) {
        if (duo.ativo() == null)
          throw new IllegalArgumentException("Campo 'ativo' do day use é obrigatório.");
        if (duo.padrao() != null) {
          var p = duo.padrao();
          if (p.preco_base() == null || p.preco_base() < 0)
            throw new IllegalArgumentException("Preço base do day use não pode ser negativo.");
          if (p.hora_preco_base() == null || p.hora_preco_base() <= 0)
            throw new IllegalArgumentException("Hora do preço base deve ser maior que 0.");
        }
        if (duo.ocupacoes() != null) {
          for (var oc : duo.ocupacoes()) {
            if (oc.quantidade_pessoa() == null || oc.quantidade_pessoa() <= 0)
              throw new IllegalArgumentException(
                  "Quantidade de pessoa no day use deve ser maior que 0.");
          }
        }
      }
    }
    if (request.menores_idade() != null) {
      for (var mi : request.menores_idade()) {
        if (mi.modelo() == null)
          throw new IllegalArgumentException("Modelo de menor idade é obrigatório.");
        if (mi.idade_gratuidade() != null && mi.idade_gratuidade() < 0)
          throw new IllegalArgumentException("Idade de gratuidade não pode ser negativa.");
        if (mi.faixas_etarias() != null) {
          for (var fe : mi.faixas_etarias()) {
            if (fe.faixa_etaria() == null || fe.faixa_etaria().size() != 2)
              throw new IllegalArgumentException(
                  "Faixa etária deve conter exatamente 2 valores [min, max].");
            if (fe.faixa_etaria().get(1) <= fe.faixa_etaria().get(0))
              throw new IllegalArgumentException(
                  "Idade máxima deve ser maior que a mínima na faixa etária.");
          }
        }
        if (mi.porcentagens_por_quantidade() != null) {
          for (var pq : mi.porcentagens_por_quantidade()) {
            if (pq.porcentagem() < 0 || pq.porcentagem() > 100)
              throw new IllegalArgumentException("Porcentagem deve estar entre 0 e 100.");
          }
        }
      }
    }
  }

  private Long getFuncionarioId() {
    return pessoaService.getFuncionarioIdFromRequest();
  }

  private void validarSubDados(Categoria.Update request) {
    // Reutiliza as mesmas validações via request virtual
    validarSubDados(
        new Categoria.Request(
            request.nome(),
            request.descricao(),
            request.hora_checkin(),
            request.hora_checkout(),
            request.modelos_ocupacao(),
            request.modelos_fixo(),
            request.day_use(),
            request.fk_quartos(),
            request.fk_sazonalidades(),
            request.menores_idade()));
  }

  //  public float calcularValorTotal(
  //          Long categoriaId,
  //          LocalDate dataEntrada,
  //          int diarias,
  //          int quantidadePessoas) {
  //    if (categoriaId == null){
  //      throw new NotFoundException("Categoria nao encontrada para o id: " + categoriaId);
  //    }
  //    if (dataEntrada == null){
  //      throw new IllegalArgumentException("Data de checkin nao informada");
  //    }
  //    if (quantidadePessoas <= 0){
  //      throw new IllegalArgumentException("Quantidade de pessoas deve ser maior que 0.");
  //    }
  //    if (diarias <= 0){
  //      throw new IllegalArgumentException("Diarias deve ser maior que 0.");
  //    }
  //    return categoriaRepository.calcularValorTotal(categoriaId, dataEntrada, diarias,
  // quantidadePessoas);
  //  }
}
