package saas.hotel.istoepousada.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import saas.hotel.istoepousada.dto.Quarto;
import saas.hotel.istoepousada.repository.QuartoRepository;

@Service
public class QuartoService {

  private final QuartoRepository quartoRepository;

  public QuartoService(QuartoRepository quartoRepository) {
    this.quartoRepository = quartoRepository;
  }

  public Page<Quarto> buscar(Long id, String termo, Quarto.Status status, Pageable pageable) {
    if (pageable == null) throw new IllegalArgumentException("pageable é obrigatório.");
    String termoNorm = StringUtils.hasText(termo) ? termo.trim() : null;
    return quartoRepository.buscar(id, termoNorm, status, pageable);
  }

  @Transactional
  public Quarto criar(Quarto.Request quarto) {
    validarParametros(quarto);
    return quartoRepository.insert(quarto);
  }

  @Transactional
  public Quarto atualizar(Quarto.Update quarto) {
    if (quarto.id() == null) throw new IllegalArgumentException("uuid é obrigatório.");
    validarParametros(quarto);
    return quartoRepository.update(quarto);
  }

  private void validarParametros(Quarto.Request quarto) {
    if (quarto == null) throw new IllegalArgumentException("Quarto é obrigatório.");
    if (!StringUtils.hasText(quarto.descricao()))
      throw new IllegalArgumentException("descricao é obrigatória.");
    if (quarto.quantidade_pessoas() != null && quarto.quantidade_pessoas() <= 0)
      throw new IllegalArgumentException("qtd_pessoas deve ser maior que 0.");
    if (quarto.quantidade_cama_casal() != null && quarto.quantidade_cama_casal() < 0)
      throw new IllegalArgumentException("quantidade_cama_casal não pode ser negativo.");
    if (quarto.quantidade_cama_solteiro() != null && quarto.quantidade_cama_solteiro() < 0)
      throw new IllegalArgumentException("quantidade_cama_solteiro não pode ser negativo.");
    if (quarto.quantidade_rede() != null && quarto.quantidade_rede() < 0)
      throw new IllegalArgumentException("quantidade_rede não pode ser negativo.");
    if (quarto.quantidade_beliche() != null && quarto.quantidade_beliche() < 0)
      throw new IllegalArgumentException("quantidade_beliche não pode ser negativo.");
    if (quarto.descricao() == null || quarto.descricao().isBlank())
      throw new IllegalArgumentException("Descrição é obrigatória.");
    if (quarto.categoria() == null || quarto.categoria().id() == null)
      throw new IllegalArgumentException("Categoria é obrigatória.");
  }

  private void validarParametros(Quarto.Update quarto) {
    if (quarto.id() == null) throw new IllegalArgumentException("uuid é obrigatório.");
    if (quarto == null) throw new IllegalArgumentException("Quarto é obrigatório.");
    if (!StringUtils.hasText(quarto.descricao()))
      throw new IllegalArgumentException("descricao é obrigatória.");
    if (quarto.quantidade_pessoas() != null && quarto.quantidade_pessoas() <= 0)
      throw new IllegalArgumentException("qtd_pessoas deve ser maior que 0.");
    if (quarto.quantidade_cama_casal() != null && quarto.quantidade_cama_casal() < 0)
      throw new IllegalArgumentException("quantidade_cama_casal não pode ser negativo.");
    if (quarto.quantidade_cama_solteiro() != null && quarto.quantidade_cama_solteiro() < 0)
      throw new IllegalArgumentException("quantidade_cama_solteiro não pode ser negativo.");
    if (quarto.quantidade_rede() != null && quarto.quantidade_rede() < 0)
      throw new IllegalArgumentException("quantidade_rede não pode ser negativo.");
    if (quarto.quantidade_beliche() != null && quarto.quantidade_beliche() < 0)
      throw new IllegalArgumentException("quantidade_beliche não pode ser negativo.");
    if (quarto.status() == null) throw new IllegalArgumentException("Status é obrigatório.");
    if (quarto.descricao() == null || quarto.descricao().isBlank())
      throw new IllegalArgumentException("Descrição é obrigatória.");
    if (quarto.categoria() == null || quarto.categoria().id() == null)
      throw new IllegalArgumentException("Categoria é obrigatória.");
  }
}
