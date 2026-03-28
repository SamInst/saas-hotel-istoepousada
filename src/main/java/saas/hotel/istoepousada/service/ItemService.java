package saas.hotel.istoepousada.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import saas.hotel.istoepousada.dto.CategoriaItem;
import saas.hotel.istoepousada.dto.Item;
import saas.hotel.istoepousada.repository.ItemRepository;

import java.util.List;

@Service
public class ItemService {

  private final ItemRepository itemRepository;

  public ItemService(ItemRepository itemRepository) {
    this.itemRepository = itemRepository;
  }

  public Page<Item> buscar(
      Long id,
      String termo,
      Long categoriaId,
      Pageable pageable) {
    return itemRepository.buscar(
        id, termo, categoriaId, pageable);
  }

  public Item buscarPorId(Long id) {
    if (id == null) {
      throw new IllegalArgumentException("ID do item é obrigatório.");
    }
    return itemRepository.findById(id);
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

    itemRepository.registrarHistoricoReposicao(request);
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

    itemRepository.atualizarHistoricoEstoque(request);
  }

  @Transactional
  public Long criarCategoria(CategoriaItem.Request request) {
    if (request == null) {
      throw new IllegalArgumentException("Dados da categoria são obrigatórios.");
    }
    if (request.nome() == null || request.nome().isBlank()) {
      throw new IllegalArgumentException("Nome da categoria é obrigatório.");
    }

    return itemRepository.criarCategoria(request);
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
    itemRepository.atualizarCategoria(categoria);
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
  public void cancelarConsumo(Long consumoId) {
    if (consumoId == null) {
      throw new IllegalArgumentException("ID do consumo é obrigatório.");
    }
    itemRepository.cancelarConsumo(consumoId);
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
    itemRepository.consumirItem(request);
  }

  public List<Item.Consumo> listarConsumos(Pageable pageable) {
    return itemRepository.listarConsumos(pageable);
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
}
