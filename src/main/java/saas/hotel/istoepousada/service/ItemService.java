package saas.hotel.istoepousada.service;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import saas.hotel.istoepousada.dto.CategoriaItem;
import saas.hotel.istoepousada.dto.Item;
import saas.hotel.istoepousada.dto.Pagamento;
import saas.hotel.istoepousada.repository.ItemRepository;
import saas.hotel.istoepousada.repository.PagamentoRepository;
import saas.hotel.istoepousada.repository.RelatorioRepository;

@Service
public class ItemService {

  private final ItemRepository itemRepository;
  private final PessoaService pessoaService;
  private final PagamentoRepository pagamentoRepository;
  private final RelatorioRepository relatorioRepository;

  public ItemService(
      ItemRepository itemRepository,
      PessoaService pessoaService,
      PagamentoRepository pagamentoRepository,
      RelatorioRepository relatorioRepository) {
    this.itemRepository = itemRepository;
    this.pessoaService = pessoaService;
    this.pagamentoRepository = pagamentoRepository;
    this.relatorioRepository = relatorioRepository;
  }

  public Page<Item> buscar(Long id, String termo, Long categoriaId, Pageable pageable) {
    return itemRepository.buscar(id, termo, categoriaId, pageable);
  }

  @Transactional
  public Item criarItem(Item.Request request) {
    validarRequestItem(request);
    return itemRepository.insert(request);
  }

  @Transactional
  public Item atualizarItem(Item.Update request) {
    if (request == null) {
      throw new IllegalArgumentException("Dados do item são obrigatórios.");
    }
    if (request.id() == null) {
      throw new IllegalArgumentException("ID do item é obrigatório.");
    }
    validarDescricao(request.descricao());
    validarCategoria(request.categoria_item());
    return itemRepository.update(request);
  }

  public List<Item.HistoricoEstoque> listarHistoricoReposicaoPorItemId(Long itemId) {
    if (itemId == null) {
      throw new IllegalArgumentException("ID do item é obrigatório.");
    }
    return itemRepository.listarHistoricoReposicaoPorItemId(itemId);
  }

  @Transactional
  public void registrarHistoricoReposicao(Item.HistoricoEstoque.Request request) {
    if (request == null) {
      throw new IllegalArgumentException("Dados do histórico de reposição são obrigatórios.");
    }
    if (request.item() == null || request.item().id() == null) {
      throw new IllegalArgumentException("ID do item é obrigatório.");
    }
    if (request.quantidade_unidades() == null || request.quantidade_unidades() == 0) {
      throw new IllegalArgumentException("Quantidade de unidades deve ser diferente de zero.");
    }
    itemRepository.registrarHistoricoReposicao(request, getFuncionarioId());
  }

  @Transactional
  public void atualizarHistoricoReposicao(Item.HistoricoEstoque.Update request) {
    if (request == null) {
      throw new IllegalArgumentException("Dados da atualização da reposição são obrigatórios.");
    }
    if (request.id() == null) {
      throw new IllegalArgumentException("ID do histórico é obrigatório.");
    }
    if (request.quantidade_unidades() == null || request.quantidade_unidades() == 0) {
      throw new IllegalArgumentException("Quantidade de unidades deve ser diferente de zero.");
    }
    itemRepository.atualizarHistoricoEstoque(request, getFuncionarioId());
  }

  @Transactional
  public Long criarCategoria(CategoriaItem.Request request) {
    if (request == null) {
      throw new IllegalArgumentException("Dados da categoria são obrigatórios.");
    }
    if (request.nome() == null || request.nome().isBlank()) {
      throw new IllegalArgumentException("Nome da categoria é obrigatório.");
    }
    return itemRepository.criarCategoria(request, getFuncionarioId());
  }

  @Transactional
  public void atualizarCategoria(CategoriaItem.Update categoria) {
    if (categoria == null) {
      throw new IllegalArgumentException("Dados da categoria são obrigatórios.");
    }
    if (categoria.id() == null) {
      throw new IllegalArgumentException("ID da categoria é obrigatório.");
    }
    if (categoria.nome() == null || categoria.nome().isBlank()) {
      throw new IllegalArgumentException("Nome da categoria é obrigatório.");
    }
    itemRepository.atualizarCategoria(categoria, getFuncionarioId());
  }

  public List<CategoriaItem> listarCategorias() {
    return itemRepository.listarCategorias();
  }

  public CategoriaItem buscarCategoriaPorId(Long id) {
    if (id == null) {
      throw new IllegalArgumentException("ID da categoria é obrigatório.");
    }
    return itemRepository.findCategoriaById(id);
  }

  public Item.HistoricoEstoque.Estoque listarEstoque() {
    return itemRepository.listarEstoque();
  }

  @Transactional
  public void consumirItem(Item.Consumo.Request request) {
    if (request == null) {
      throw new IllegalArgumentException("Dados do consumo são obrigatórios.");
    }
    if (request.item() == null || request.item().id() == null) {
      throw new IllegalArgumentException("ID do item é obrigatório.");
    }
    if (request.quantidade() == null || request.quantidade() <= 0) {
      throw new IllegalArgumentException("Quantidade deve ser maior que zero.");
    }
    if (!Boolean.TRUE.equals(request.despesa_pessoal()) && request.pagamento() == null) {
      throw new IllegalArgumentException("Pagamento é obrigatório quando não for despesa pessoal.");
    }

    boolean despesaPessoal = Boolean.TRUE.equals(request.despesa_pessoal());
    boolean temQuarto = request.quarto() != null;

    UUID pagamentoId = null;
    if (!despesaPessoal && request.pagamento() != null) {
      Pagamento pagamento = pagamentoRepository.create(request.pagamento(), getFuncionarioId());
      pagamentoId = pagamento.uuid();
      String itemDescricao = itemRepository.buscarDescricaoItem(request.item().id());
      relatorioRepository.registrarRelatorioDeConsumo(
          pagamento, getFuncionarioId(), "Consumo: " + itemDescricao);
    }

    itemRepository.inserirConsumo(
        pagamentoId,
        request.item().id(),
        getFuncionarioId(),
        request.quantidade(),
        despesaPessoal,
        request.quarto() != null ? request.quarto().id() : null);

    if (temQuarto) {
      Integer qtdQuarto =
          itemRepository.buscarQuantidadeQuartoItem(request.quarto().id(), request.item().id());
      if (qtdQuarto != null && qtdQuarto >= request.quantidade()) {
        itemRepository.descontarQuartoItem(
            request.quarto().id(), request.item().id(), request.quantidade());
        return;
      }
    }

    itemRepository.descontarEstoquePorItem(request.item().id(), request.quantidade());
  }

  @Transactional
  public void cancelarConsumo(Long consumoId) {
    if (consumoId == null) {
      throw new IllegalArgumentException("ID do consumo é obrigatório.");
    }
    var consumo = itemRepository.buscarConsumoParaCancelamento(consumoId);

    if (consumo.quartoId() != null) {
      int updated =
          itemRepository.incrementarQuartoItem(
              consumo.quartoId(), consumo.itemId(), consumo.quantidade());
      if (updated == 0) {
        itemRepository.incrementarEstoquePorItem(consumo.itemId(), consumo.quantidade());
      }
    } else {
      itemRepository.incrementarEstoquePorItem(consumo.itemId(), consumo.quantidade());
    }

    if (consumo.pagamentoId() != null) {
      pagamentoRepository.cancelarPagamento(consumo.pagamentoId());
    }

    itemRepository.marcarConsumoCancelado(consumoId);
  }

  public List<Item.Consumo> listarConsumos(Pageable pageable) {
    return itemRepository.listarConsumos(pageable);
  }

  public void retirarDoEstoque(Long item_id, Integer quantidade) {
    itemRepository.retirarDoEstoque(item_id, quantidade);
  }

  public Boolean estoqueExisteParaItem(Long itemId) {
    return itemRepository.estoqueExisteParaItem(itemId);
  }

  private void validarRequestItem(Item.Request request) {
    if (request == null) {
      throw new IllegalArgumentException("Dados do item são obrigatórios.");
    }
    validarDescricao(request.descricao());
    validarCategoria(request.categoria_item());
  }

  private void validarDescricao(String descricao) {
    if (descricao == null || descricao.isBlank()) {
      throw new IllegalArgumentException("Descrição do item é obrigatória.");
    }
  }

  private void validarCategoria(CategoriaItem.Id categoria) {
    if (categoria == null || categoria.id() == null) {
      throw new IllegalArgumentException("Categoria do item é obrigatória.");
    }
  }

  private Long getFuncionarioId() {
    return pessoaService.getFuncionarioIdFromRequest();
  }
}
