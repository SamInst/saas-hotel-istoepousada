package saas.hotel.istoepousada.service;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import saas.hotel.istoepousada.dto.*;
import saas.hotel.istoepousada.repository.ItemRepository;
import saas.hotel.istoepousada.repository.PessoaRepository;

@Service
public class ItemService {

  private final ItemRepository itemRepository;
  private final PessoaRepository pessoaRepository;

  public ItemService(ItemRepository itemRepository, PessoaRepository pessoaRepository) {
    this.itemRepository = itemRepository;
    this.pessoaRepository = pessoaRepository;
  }

  public Page<ItemResponse> buscar(
      Long id,
      String termo,
      Long categoriaId,
      LocalDate dataInicioCadastro,
      LocalDate dataFimCadastro,
      Pageable pageable) {
    return itemRepository.buscar(
        id, termo, categoriaId, dataInicioCadastro, dataFimCadastro, pageable);
  }

  public ItemResponse buscarPorId(Long id) {
    return itemRepository.findById(id);
  }

  @Transactional
  public ItemResponse criarItem(ItemResponse.ItemRequest request) {
    Long funcionarioIdLogado = pessoaRepository.getFuncionarioIdFromRequest();
    return itemRepository.insert(request, funcionarioIdLogado);
  }

  @Transactional
  public ItemResponse atualizarItem(Long id, ItemResponse.ItemRequest request) {
    Long funcionarioIdLogado = pessoaRepository.getFuncionarioIdFromRequest();
    return itemRepository.update(id, request, funcionarioIdLogado);
  }

  public ItemBuscaCompleta buscarCompleto(LocalDate dataInicioCadastro, LocalDate dataFimCadastro) {
    return itemRepository.buscarCompleto(dataInicioCadastro, dataFimCadastro);
  }

  public HistoricoReposicaoItem listarHistoricoReposicaoPorItemId(Long itemId) {
    if (itemId == null) {
      throw new IllegalArgumentException("ID do item é obrigatório.");
    }
    return itemRepository.listarHistoricoReposicaoPorItemId(itemId);
  }

  @Transactional
  public void reporEstoque(
      Long itemId,
      int quantidade,
      Double valorCompraUnidade,
      Double valorVendaUnidade,
      String fornecedor) {
    if (itemId == null) throw new IllegalArgumentException("ID do item é obrigatório.");
    if (quantidade <= 0) throw new IllegalArgumentException("Quantidade deve ser maior que zero.");
    Long funcionarioIdLogado = pessoaRepository.getFuncionarioIdFromRequest();
    itemRepository.reporEstoque(
        itemId, quantidade, valorCompraUnidade, valorVendaUnidade, fornecedor, funcionarioIdLogado);
  }

  @Transactional
  public void consumirEstoque(Long itemId, int quantidade) {
    Long funcionarioIdLogado = pessoaRepository.getFuncionarioIdFromRequest();
    if (itemId == null) throw new IllegalArgumentException("ID do item é obrigatório.");
    if (quantidade <= 0) throw new IllegalArgumentException("Quantidade deve ser maior que zero.");
    itemRepository.consumirEstoque(itemId, quantidade, funcionarioIdLogado);
  }

  public List<HistoricoPrecoItem> listarHistoricoPrecoPorItemId(Long itemId) {
    if (itemId == null) {
      throw new IllegalArgumentException("ID do item é obrigatório.");
    }
    return itemRepository.listarHistoricoPrecoPorItemId(itemId);
  }

  @Transactional
  public Long criarCategoria(String categoria, String descricao) {
    if (categoria == null || categoria.isBlank()) {
      throw new IllegalArgumentException("Nome da categoria é obrigatório.");
    }
    return itemRepository.criarCategoria(categoria, descricao);
  }

  @Transactional
  public void atualizarCategoria(Long id, String categoria, String descricao) {
    if (id == null) throw new IllegalArgumentException("ID da categoria é obrigatório.");
    if (categoria == null || categoria.isBlank()) {
      throw new IllegalArgumentException("Nome da categoria é obrigatório.");
    }
    itemRepository.atualizarCategoria(id, categoria, descricao);
  }

  public List<ItemCategoria> listarCategorias() {
    return itemRepository.listarCategorias();
  }
}
