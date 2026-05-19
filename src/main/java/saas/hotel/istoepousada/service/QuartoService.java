package saas.hotel.istoepousada.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import saas.hotel.istoepousada.dto.Hospedagem;
import saas.hotel.istoepousada.dto.Quarto;
import saas.hotel.istoepousada.dto.Recepcao;
import saas.hotel.istoepousada.handler.exceptions.NotFoundException;
import saas.hotel.istoepousada.repository.QuartoRepository;
import saas.hotel.istoepousada.repository.QuartoRepository.QuartoComCategoria;

@Service
public class QuartoService {
  private final QuartoRepository quartoRepository;
  private final ItemService itemService;
  private final HospedagemService hospedagemService;
  private final PessoaService pessoaService;

  public QuartoService(
      QuartoRepository quartoRepository,
      ItemService itemService,
      HospedagemService hospedagemService,
      PessoaService pessoaService) {
    this.quartoRepository = quartoRepository;
    this.itemService = itemService;
    this.hospedagemService = hospedagemService;
    this.pessoaService = pessoaService;
  }

  public Page<Quarto> buscar(Long id, String termo, Quarto.Status status, Pageable pageable) {
    if (pageable == null) throw new IllegalArgumentException("pageable é obrigatório.");
    return quartoRepository.buscar(
        id, StringUtils.hasText(termo) ? termo.trim() : null, status, pageable);
  }

  @Transactional
  public Quarto criar(Quarto.Request quarto) {
    validarRequest(quarto);
    var novoQuarto = quartoRepository.insert(quarto);
    quartoRepository.vincularCategoriaAtiva(novoQuarto.id(), quarto.categoria().id(), getFuncionarioId());
    return novoQuarto;
  }

  @Transactional
  public Quarto atualizar(Quarto.Update quarto) {
    validarUpdate(quarto);
    var updatedQuarto = quartoRepository.update(quarto);
    quartoRepository.atualizarCategoriaAtiva(quarto.id(), quarto.categoria().id(), getFuncionarioId());
    return updatedQuarto;
  }

  @Transactional
  public Quarto alterarStatus(Long id, Quarto.Status status) {
    if (id == null) throw new IllegalArgumentException("ID do quarto é obrigatório.");
    if (status == null) throw new IllegalArgumentException("Status é obrigatório.");
    quartoRepository.updateStatus(id, status);
    return quartoRepository.findByIdOrThrow(id);
  }

  public Recepcao.QuartoData buscarRecepcao(LocalDate data) {
    if (data == null) data = LocalDate.now();

    List<QuartoComCategoria> quartoRows = quartoRepository.buscarQuartosComCategoria();
    Map<Long, Quarto.QuartoManutencao> manutencaoPorQuarto =
        quartoRepository.buscarManutencaoAtivaPorQuarto();
    Map<Long, Quarto.QuartoLimpeza> limpezaPorQuarto =
        quartoRepository.buscarLimpezaAtivaPorQuarto();

    List<Long> quartoIds = quartoRows.stream().map(QuartoComCategoria::quartoId).toList();
    Map<Long, List<Quarto.ItemQuarto>> itensPorQuarto =
        quartoRepository.buscarItensPorQuartos(quartoIds);

    Map<Long, Hospedagem> hospedagemPorQuarto =
        hospedagemService.buscarAtivasPorQuartoNaData(data);

    Map<Long, List<Recepcao.QuartoData.Categoria.Quartos>> quartosPorCat = new LinkedHashMap<>();
    Map<Long, String[]> catNames = new LinkedHashMap<>();

    for (QuartoComCategoria qr : quartoRows) {
      Quarto quarto =
          new Quarto(
              qr.quartoId(),
              qr.descricao(),
              qr.qtdPessoas(),
              qr.status(),
              qr.camaCasal(),
              qr.camaSolteiro(),
              qr.rede(),
              qr.beliche(),
              itensPorQuarto.getOrDefault(qr.quartoId(), List.of()),
              manutencaoPorQuarto.get(qr.quartoId()),
              limpezaPorQuarto.get(qr.quartoId()));

      quartosPorCat
          .computeIfAbsent(qr.categoriaId(), k -> new ArrayList<>())
          .add(new Recepcao.QuartoData.Categoria.Quartos(quarto, hospedagemPorQuarto.get(qr.quartoId())));

      catNames.putIfAbsent(
          qr.categoriaId(), new String[]{qr.categoriaNome(), qr.categoriaDescricao()});
    }

    List<Recepcao.QuartoData.Categoria> categorias = new ArrayList<>();
    for (Map.Entry<Long, List<Recepcao.QuartoData.Categoria.Quartos>> entry :
        quartosPorCat.entrySet()) {
      String[] names = catNames.get(entry.getKey());
      categorias.add(
          new Recepcao.QuartoData.Categoria(entry.getKey(), names[0], names[1], entry.getValue()));
    }

    return new Recepcao.QuartoData(data, 0, categorias);
  }

  public List<Quarto.ItemQuarto> listarItens(Long quartoId) {
    if (quartoId == null) throw new IllegalArgumentException("ID do quarto é obrigatório.");
    return quartoRepository.listarItens(quartoId);
  }

  @Transactional
  public Quarto.ItemQuarto adicionarItem(Long quartoId, Quarto.QuartoItem.Request req) {
    if (quartoId == null) throw new IllegalArgumentException("ID do quarto é obrigatório.");
    if (req.item() == null || req.item().id() == null)
      throw new IllegalArgumentException("Item é obrigatório.");
    if (req.quantidade_padrao() == null || req.quantidade_padrao() < 0)
      throw new IllegalArgumentException("quantidade_padrao inválida.");
    if (req.quantidade_atual() == null || req.quantidade_atual() < 0)
      throw new IllegalArgumentException("quantidade_atual inválida.");
    var itemQuarto = quartoRepository.adicionarItem(quartoId, req, getFuncionarioId());
    itemService.retirarDoEstoque(itemQuarto.item().id(), itemQuarto.quantidade_atual());
    return itemQuarto;
  }

  @Transactional
  public Quarto.ItemQuarto atualizarItem(Quarto.QuartoItem.Update req) {
    if (req.id() == null) throw new IllegalArgumentException("ID do item é obrigatório.");
    return quartoRepository.atualizarItem(req);
  }

  @Transactional
  public Quarto.ItemQuarto consumirItem(Quarto.QuartoItem.Consumir req) {
    if (req.id() == null) throw new IllegalArgumentException("ID do item é obrigatório.");
    if (req.quantidade() == null || req.quantidade() <= 0)
      throw new IllegalArgumentException("Quantidade deve ser maior que zero.");
    return quartoRepository.consumirItem(req);
  }

  @Transactional
  public Quarto.ItemQuarto reporItem(Quarto.QuartoItem.Repor req) {
    if (req.id() == null) throw new IllegalArgumentException("ID do item é obrigatório.");
    if (req.quantidade() == null || req.quantidade() <= 0)
      throw new IllegalArgumentException("Quantidade deve ser maior que zero.");
    Long itemId = quartoRepository.reporItemNoQuarto(req, getFuncionarioId());
    if (!itemService.estoqueExisteParaItem(itemId)) {
      throw new NotFoundException("Estoque nao encontrado para o item: " + itemId);
    }
    quartoRepository.descontarEstoque(itemId, req.quantidade());
    return quartoRepository.findItemById(req.id());
  }

  // ── Manutenção ────────────────────────────────────────────────────────────────

  @Transactional
  public Quarto.QuartoManutencao criarManutencao(Quarto.QuartoManutencao.Request req) {
    if (req.quarto() == null || req.quarto().id() == null)
      throw new IllegalArgumentException("Quarto é obrigatório.");
    if (!StringUtils.hasText(req.descricao()))
      throw new IllegalArgumentException("Descrição é obrigatória.");
    return quartoRepository.inserirManutencao(req, getFuncionarioId());
  }

  @Transactional
  public Quarto.QuartoManutencao atualizarManutencao(Quarto.QuartoManutencao.Update req) {
    if (req.id() == null) throw new IllegalArgumentException("ID é obrigatório.");
    return quartoRepository.atualizarManutencao(req);
  }

  @Transactional
  public void finalizarManutencao(Long id) {
    if (id == null) throw new IllegalArgumentException("ID é obrigatório.");
    quartoRepository.finalizarManutencao(id);
  }

  // ── Limpeza ───────────────────────────────────────────────────────────────────

  @Transactional
  public Quarto.QuartoLimpeza acionarLimpeza(Long quartoId, Quarto.QuartoLimpeza.Request req) {
    if (quartoId == null) throw new IllegalArgumentException("ID do quarto é obrigatório.");
    Long funcionarioId = req.funcionario() != null
        ? req.funcionario().id()
        : pessoaService.getFuncionarioIdFromRequest();
    return quartoRepository.acionarLimpeza(quartoId, funcionarioId);
  }

  @Transactional
  public void finalizarLimpeza(Long id) {
    if (id == null) throw new IllegalArgumentException("ID é obrigatório.");
    Long quartoId = quartoRepository.finalizarLimpeza(id);
    Quarto.Status novoStatus =
        hospedagemService.temReservaAtivaParaQuartoHoje(quartoId)
            ? Quarto.Status.RESERVADO
            : Quarto.Status.DISPONIVEL;
    quartoRepository.updateStatus(quartoId, novoStatus);
  }

  // ── Validações ────────────────────────────────────────────────────────────────

  private void validarRequest(Quarto.Request q) {
    if (q == null) throw new IllegalArgumentException("Quarto é obrigatório.");
    if (!StringUtils.hasText(q.descricao()))
      throw new IllegalArgumentException("Descrição é obrigatória.");
    if (q.categoria() == null || q.categoria().id() == null)
      throw new IllegalArgumentException("Categoria é obrigatória.");
    if (q.quantidade_pessoas() != null && q.quantidade_pessoas() <= 0)
      throw new IllegalArgumentException("quantidade_pessoas deve ser maior que 0.");
    validarCamas(
        q.quantidade_cama_casal(),
        q.quantidade_cama_solteiro(),
        q.quantidade_rede(),
        q.quantidade_beliche());
  }

  private void validarUpdate(Quarto.Update q) {
    if (q == null) throw new IllegalArgumentException("Quarto é obrigatório.");
    if (q.id() == null) throw new IllegalArgumentException("ID é obrigatório.");
    if (!StringUtils.hasText(q.descricao()))
      throw new IllegalArgumentException("Descrição é obrigatória.");
    if (q.categoria() == null || q.categoria().id() == null)
      throw new IllegalArgumentException("Categoria é obrigatória.");
    if (q.status() == null) throw new IllegalArgumentException("Status é obrigatório.");
    if (q.quantidade_pessoas() != null && q.quantidade_pessoas() <= 0)
      throw new IllegalArgumentException("quantidade_pessoas deve ser maior que 0.");
    validarCamas(
        q.quantidade_cama_casal(),
        q.quantidade_cama_solteiro(),
        q.quantidade_rede(),
        q.quantidade_beliche());
  }

  private void validarCamas(Integer casal, Integer solteiro, Integer rede, Integer beliche) {
    if (casal != null && casal < 0)
      throw new IllegalArgumentException("quantidade_cama_casal não pode ser negativo.");
    if (solteiro != null && solteiro < 0)
      throw new IllegalArgumentException("quantidade_cama_solteiro não pode ser negativo.");
    if (rede != null && rede < 0)
      throw new IllegalArgumentException("quantidade_rede não pode ser negativo.");
    if (beliche != null && beliche < 0)
      throw new IllegalArgumentException("quantidade_beliche não pode ser negativo.");
  }

  public Map<Long, String> findQuartosDescricao(List<Long> quartoIds) {
    return quartoRepository.findQuartosDescricao(quartoIds);
  }

  private Long getFuncionarioId(){
    return pessoaService.getFuncionarioIdFromRequest();
  }
}
