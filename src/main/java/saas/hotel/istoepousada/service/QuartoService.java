package saas.hotel.istoepousada.service;

import java.util.Objects;
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

  public Page<Quarto> buscar(Long id, String termo, Quarto.StatusQuarto status, Pageable pageable) {
    if (pageable == null) throw new IllegalArgumentException("pageable é obrigatório.");
    String termoNorm = StringUtils.hasText(termo) ? termo.trim() : null;
    return quartoRepository.buscar(id, termoNorm, status, pageable);
  }

  @Transactional
  public Quarto criar(Quarto quarto) {
    validarEntrada(quarto);
    return quartoRepository.insert(quarto);
  }

  @Transactional
  public Quarto atualizar(Long id, Quarto quarto) {
    if (id == null) throw new IllegalArgumentException("id é obrigatório.");
    validarEntrada(quarto);
    if (quarto.id() != null && !Objects.equals(quarto.id(), id)) {
      quarto =
          new Quarto(
              id,
              quarto.descricao(),
              quarto.quantidade_pessoas(),
              quarto.status_quarto(),
              quarto.qtd_cama_casal(),
              quarto.qtd_cama_solteiro(),
              quarto.qtd_rede(),
              quarto.qtd_beliche());
    }
    return quartoRepository.update(id, quarto);
  }

  @Transactional
  public void remover(Long id) {
    quartoRepository.delete(id);
  }

  private void validarEntrada(Quarto quarto) {
    if (quarto == null) throw new IllegalArgumentException("Quarto é obrigatório.");
    if (!StringUtils.hasText(quarto.descricao()))
      throw new IllegalArgumentException("descricao é obrigatória.");
    if (quarto.quantidade_pessoas() != null && quarto.quantidade_pessoas() <= 0)
      throw new IllegalArgumentException("qtd_pessoas deve ser maior que 0.");
    if (quarto.qtd_cama_casal() != null && quarto.qtd_cama_casal() < 0)
      throw new IllegalArgumentException("qtd_cama_casal não pode ser negativo.");
    if (quarto.qtd_cama_solteiro() != null && quarto.qtd_cama_solteiro() < 0)
      throw new IllegalArgumentException("qtd_cama_solteiro não pode ser negativo.");
    if (quarto.qtd_rede() != null && quarto.qtd_rede() < 0)
      throw new IllegalArgumentException("qtd_rede não pode ser negativo.");
    if (quarto.qtd_beliche() != null && quarto.qtd_beliche() < 0)
      throw new IllegalArgumentException("qtd_beliche não pode ser negativo.");
  }
}
