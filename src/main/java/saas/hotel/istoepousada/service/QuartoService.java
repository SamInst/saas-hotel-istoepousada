package saas.hotel.istoepousada.service;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import saas.hotel.istoepousada.dto.Quarto;
import saas.hotel.istoepousada.dto.Recepcao;
import saas.hotel.istoepousada.repository.QuartoRepository;

@Service
public class QuartoService {

  private final QuartoRepository quartoRepository;

  public QuartoService(QuartoRepository quartoRepository) {
    this.quartoRepository = quartoRepository;
  }

  // ── Buscar / CRUD quarto ─────────────────────────────────────────────────────

  public Page<Quarto> buscar(Long id, String termo, Quarto.Status status, Pageable pageable) {
    if (pageable == null) throw new IllegalArgumentException("pageable é obrigatório.");
    return quartoRepository.buscar(
        id, StringUtils.hasText(termo) ? termo.trim() : null, status, pageable);
  }

  @Transactional
  public Quarto criar(Quarto.Request quarto) {
    validarRequest(quarto);
    return quartoRepository.insert(quarto);
  }

  @Transactional
  public Quarto atualizar(Quarto.Update quarto) {
    validarUpdate(quarto);
    return quartoRepository.update(quarto);
  }

  @Transactional
  public Quarto alterarStatus(Long id, Quarto.Status status) {
    if (id == null) throw new IllegalArgumentException("ID do quarto é obrigatório.");
    if (status == null) throw new IllegalArgumentException("Status é obrigatório.");
    quartoRepository.findByIdOrThrow(id);
    quartoRepository.updateStatus(id, status);
    return quartoRepository.findByIdOrThrow(id);
  }

  // ── Recepção ─────────────────────────────────────────────────────────────────

  public Recepcao.QuartoData buscarRecepcao(LocalDate data) {
    if (data == null) data = LocalDate.now();
    return quartoRepository.buscarRecepcao(data);
  }

  // ── Itens ─────────────────────────────────────────────────────────────────────

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
    return quartoRepository.adicionarItem(quartoId, req);
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
    return quartoRepository.reporItem(req);
  }

  // ── Manutenção ────────────────────────────────────────────────────────────────

  @Transactional
  public Quarto.QuartoManutencao criarManutencao(Quarto.QuartoManutencao.Request req) {
    if (req.quarto() == null || req.quarto().id() == null)
      throw new IllegalArgumentException("Quarto é obrigatório.");
    if (!StringUtils.hasText(req.descricao()))
      throw new IllegalArgumentException("Descrição é obrigatória.");
    return quartoRepository.inserirManutencao(req);
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
    return quartoRepository.acionarLimpeza(quartoId, req);
  }

  @Transactional
  public void finalizarLimpeza(Long id) {
    if (id == null) throw new IllegalArgumentException("ID é obrigatório.");
    quartoRepository.finalizarLimpeza(id);
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
}
