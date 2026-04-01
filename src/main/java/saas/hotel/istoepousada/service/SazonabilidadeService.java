package saas.hotel.istoepousada.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import saas.hotel.istoepousada.dto.Sazonalidade;
import saas.hotel.istoepousada.handler.exceptions.ConflictException;
import saas.hotel.istoepousada.repository.SazonabilidadeRepository;

@Service
public class SazonabilidadeService {

  private final SazonabilidadeRepository sazonabilidadeRepository;

  public SazonabilidadeService(SazonabilidadeRepository sazonabilidadeRepository) {
    this.sazonabilidadeRepository = sazonabilidadeRepository;
  }

  public Page<Sazonalidade> buscar(Long id, String termo, Pageable pageable) {
    if (pageable == null) throw new IllegalArgumentException("Pageable é obrigatório.");
    return sazonabilidadeRepository.buscar(
        id, StringUtils.hasText(termo) ? termo.trim() : null, pageable);
  }

  public Sazonalidade buscarPorId(Long id) {
    if (id == null) throw new IllegalArgumentException("Id é obrigatório.");
    return sazonabilidadeRepository.findByIdOrThrow(id);
  }

  @Transactional
  public Sazonalidade criar(Sazonalidade.Request request) {
    validar(request);
    if (request.fk_categorias() != null) {
      Sazonalidade temp = fromRequest(request);
      for (Long catId : request.fk_categorias()) {
        verificarConflitos(temp, catId, null);
      }
    }
    return sazonabilidadeRepository.insert(request);
  }

  @Transactional
  public Sazonalidade atualizar(Sazonalidade.Update request) {
    if (request.id() == null) throw new IllegalArgumentException("Id é obrigatório.");
    validarUpdate(request);

    Sazonalidade temp = fromUpdate(request);

    // Se fk_categorias não informado, verifica conflitos nas categorias atualmente vinculadas
    List<Long> categoriasParaVerificar;
    if (request.fk_categorias() != null) {
      categoriasParaVerificar = request.fk_categorias();
    } else {
      Sazonalidade existente = sazonabilidadeRepository.findByIdOrThrow(request.id());
      categoriasParaVerificar = existente.categorias() == null ? List.of()
          : existente.categorias().stream().map(cv -> cv.categoria().id()).toList();
    }

    for (Long catId : categoriasParaVerificar) {
      verificarConflitos(temp, catId, request.id());
    }

    return sazonabilidadeRepository.update(request);
  }

  @Transactional
  public void vincular(Sazonalidade.VinculoCategoriaRequest request) {
    if (request.fk_sazonalidade() == null) throw new IllegalArgumentException("fk_sazonalidade é obrigatório.");
    if (request.fk_categoria() == null) throw new IllegalArgumentException("fk_categoria é obrigatório.");
    Sazonalidade sazon = sazonabilidadeRepository.findByIdOrThrow(request.fk_sazonalidade());
    verificarConflitos(sazon, request.fk_categoria(), sazon.id());
    sazonabilidadeRepository.vincular(request.fk_sazonalidade(), request.fk_categoria());
  }

  public void toggleAtivo(Sazonalidade.VinculoCategoriaToggle request) {
    if (request.id() == null) throw new IllegalArgumentException("Id é obrigatório.");
    if (request.ativo() == null) throw new IllegalArgumentException("Ativo é obrigatório.");
    sazonabilidadeRepository.toggleAtivo(request.id(), request.ativo());
  }

  public void removerVinculo(Long id) {
    if (id == null) throw new IllegalArgumentException("Id é obrigatório.");
    sazonabilidadeRepository.removerVinculo(id);
  }

  // ── Validações ────────────────────────────────────────────────────────────

  private void validar(Sazonalidade.Request r) {
    if (!StringUtils.hasText(r.descricao())) throw new IllegalArgumentException("Descrição é obrigatória.");
    validarModo(r.data_inicio(), r.data_fim(),
        r.diario_hora_inicio_ciclo(), r.diario_hora_fim_ciclo(),
        r.semanal(), r.mensal(), r.anual());
  }

  private void validarUpdate(Sazonalidade.Update r) {
    if (!StringUtils.hasText(r.descricao())) throw new IllegalArgumentException("Descrição é obrigatória.");
    validarModo(r.data_inicio(), r.data_fim(),
        r.diario_hora_inicio_ciclo(), r.diario_hora_fim_ciclo(),
        r.semanal(), r.mensal(), r.anual());
  }

  private void validarModo(
      LocalDate dataInicio, LocalDate dataFim,
      LocalTime diarioInicio, LocalTime diarioFim,
      List<Integer> semanal, List<Integer> mensal, List<Integer> anual) {

    int modos = 0;
    if (dataInicio != null || dataFim != null) modos++;
    if (diarioInicio != null || diarioFim != null) modos++;
    if (semanal != null && !semanal.isEmpty()) modos++;
    if (mensal != null && !mensal.isEmpty()) modos++;
    if (anual != null && !anual.isEmpty()) modos++;

    if (modos == 0) throw new IllegalArgumentException(
        "Informe ao menos um modo de operação: data específica, diário, semanal, mensal ou anual.");
    if (modos > 1) throw new IllegalArgumentException(
        "Apenas um modo de operação pode ser definido por sazonalidade.");

    if (dataInicio != null && dataFim != null && dataFim.isBefore(dataInicio))
      throw new IllegalArgumentException("Data de fim não pode ser anterior à data de início.");
    if (diarioInicio != null && diarioFim != null && diarioFim.isBefore(diarioInicio))
      throw new IllegalArgumentException("Hora de fim não pode ser anterior à hora de início.");
    if (semanal != null) {
      for (int d : semanal) {
        if (d < 1 || d > 7)
          throw new IllegalArgumentException("Dia da semana inválido: " + d + ". Use 1=Segunda a 7=Domingo.");
      }
    }
    if (mensal != null) {
      for (int d : mensal) {
        if (d < 1 || d > 31)
          throw new IllegalArgumentException("Dia do mês inválido: " + d + ". Use 1 a 31.");
      }
    }
    if (anual != null) {
      for (int m : anual) {
        if (m < 1 || m > 12)
          throw new IllegalArgumentException("Mês inválido: " + m + ". Use 1 a 12.");
      }
    }
  }

  // ── Verificação de conflitos ──────────────────────────────────────────────

  private void verificarConflitos(Sazonalidade nova, Long fkCategoria, Long excludeSazonId) {
    List<Sazonalidade> existentes =
        sazonabilidadeRepository.buscarAtivosParaCategoria(fkCategoria, excludeSazonId);
    for (Sazonalidade existente : existentes) {
      verificarConflito(nova, existente);
    }
  }

  private void verificarConflito(Sazonalidade nova, Sazonalidade existente) {
    boolean nData    = nova.data_inicio() != null || nova.data_fim() != null;
    boolean nDiario  = nova.diario_hora_inicio_ciclo() != null || nova.diario_hora_fim_ciclo() != null;
    boolean nSemanal = nova.semanal() != null && !nova.semanal().isEmpty();
    boolean nMensal  = nova.mensal() != null && !nova.mensal().isEmpty();
    boolean nAnual   = nova.anual() != null && !nova.anual().isEmpty();

    boolean eData    = existente.data_inicio() != null || existente.data_fim() != null;
    boolean eDiario  = existente.diario_hora_inicio_ciclo() != null || existente.diario_hora_fim_ciclo() != null;
    boolean eSemanal = existente.semanal() != null && !existente.semanal().isEmpty();
    boolean eMensal  = existente.mensal() != null && !existente.mensal().isEmpty();
    boolean eAnual   = existente.anual() != null && !existente.anual().isEmpty();

    String nome = "'" + existente.descricao() + "'";

    // ── Mesmo modo ────────────────────────────────────────────────────────

    if (nData && eData) {
      // Overlap: n.inicio <= e.fim AND e.inicio <= n.fim (null = aberto)
      boolean n0_le_e1 = nova.data_inicio() == null || existente.data_fim() == null
          || !nova.data_inicio().isAfter(existente.data_fim());
      boolean e0_le_n1 = existente.data_inicio() == null || nova.data_fim() == null
          || !existente.data_inicio().isAfter(nova.data_fim());
      if (n0_le_e1 && e0_le_n1)
        throw new ConflictException("Período de data em conflito com a sazonalidade " + nome + ".");
      return;
    }

    if (nDiario && eDiario) {
      boolean n0_le_e1 = nova.diario_hora_inicio_ciclo() == null || existente.diario_hora_fim_ciclo() == null
          || !nova.diario_hora_inicio_ciclo().isAfter(existente.diario_hora_fim_ciclo());
      boolean e0_le_n1 = existente.diario_hora_inicio_ciclo() == null || nova.diario_hora_fim_ciclo() == null
          || !existente.diario_hora_inicio_ciclo().isAfter(nova.diario_hora_fim_ciclo());
      if (n0_le_e1 && e0_le_n1)
        throw new ConflictException("Horário diário em conflito com a sazonalidade " + nome + ".");
      return;
    }

    if (nSemanal && eSemanal) {
      Set<Integer> inter = new HashSet<>(nova.semanal());
      inter.retainAll(existente.semanal());
      if (!inter.isEmpty())
        throw new ConflictException("Dias da semana " + inter + " em conflito com a sazonalidade " + nome + ".");
      return;
    }

    if (nMensal && eMensal) {
      Set<Integer> inter = new HashSet<>(nova.mensal());
      inter.retainAll(existente.mensal());
      if (!inter.isEmpty())
        throw new ConflictException("Dias do mês " + inter + " em conflito com a sazonalidade " + nome + ".");
      return;
    }

    if (nAnual && eAnual) {
      Set<Integer> inter = new HashSet<>(nova.anual());
      inter.retainAll(existente.anual());
      if (!inter.isEmpty())
        throw new ConflictException("Meses " + inter + " em conflito com a sazonalidade " + nome + ".");
      return;
    }

    // ── Modo cruzado ──────────────────────────────────────────────────────

    // Data vs Diário: sempre conflita (ambos cobrem os mesmos instantes)
    if ((nData && eDiario) || (nDiario && eData)) {
      throw new ConflictException(
          "Sazonalidade de data específica não pode coexistir com sazonalidade diária " + nome + ".");
    }

    // Data vs Semanal
    if (nData && eSemanal) {
      if (dateRangeOverlapsWeekdays(nova.data_inicio(), nova.data_fim(), existente.semanal()))
        throw new ConflictException("Período de data abrange dias da semana da sazonalidade " + nome + ".");
      return;
    }
    if (nSemanal && eData) {
      if (dateRangeOverlapsWeekdays(existente.data_inicio(), existente.data_fim(), nova.semanal()))
        throw new ConflictException("Dias da semana estão no período de data da sazonalidade " + nome + ".");
      return;
    }

    // Data vs Mensal
    if (nData && eMensal) {
      if (dateRangeOverlapsDaysOfMonth(nova.data_inicio(), nova.data_fim(), existente.mensal()))
        throw new ConflictException("Período de data abrange dias do mês da sazonalidade " + nome + ".");
      return;
    }
    if (nMensal && eData) {
      if (dateRangeOverlapsDaysOfMonth(existente.data_inicio(), existente.data_fim(), nova.mensal()))
        throw new ConflictException("Dias do mês estão no período de data da sazonalidade " + nome + ".");
      return;
    }

    // Data vs Anual
    if (nData && eAnual) {
      if (dateRangeOverlapsMonths(nova.data_inicio(), nova.data_fim(), existente.anual()))
        throw new ConflictException("Período de data abrange meses da sazonalidade " + nome + ".");
      return;
    }
    if (nAnual && eData) {
      if (dateRangeOverlapsMonths(existente.data_inicio(), existente.data_fim(), nova.anual()))
        throw new ConflictException("Meses estão no período de data da sazonalidade " + nome + ".");
      return;
    }

    // Diário vs Semanal / Mensal / Anual: aplica todo dia, sempre conflita
    if ((nDiario && (eSemanal || eMensal || eAnual)) || (eDiario && (nSemanal || nMensal || nAnual))) {
      throw new ConflictException("Sazonalidade diária em conflito com a sazonalidade " + nome + ".");
    }

    // Semanal vs Mensal / Anual
    if ((nSemanal && (eMensal || eAnual)) || (eSemanal && (nMensal || nAnual))) {
      throw new ConflictException("Sazonalidade semanal em conflito com a sazonalidade " + nome + ".");
    }

    // Mensal vs Anual
    if ((nMensal && eAnual) || (eMensal && nAnual)) {
      throw new ConflictException("Sazonalidade mensal em conflito com a sazonalidade " + nome + ".");
    }
  }

  // ── Helpers de verificação de sobreposição ────────────────────────────────

  private boolean dateRangeOverlapsWeekdays(LocalDate inicio, LocalDate fim, List<Integer> weekdays) {
    if (inicio == null || fim == null) return true; // intervalo aberto cobre todos os dias
    if (!inicio.plusDays(6).isAfter(fim)) return true; // >= 7 dias sempre cobre todos os dias da semana
    Set<Integer> days = new HashSet<>(weekdays);
    LocalDate d = inicio;
    while (!d.isAfter(fim)) {
      if (days.contains(d.getDayOfWeek().getValue())) return true;
      d = d.plusDays(1);
    }
    return false;
  }

  private boolean dateRangeOverlapsDaysOfMonth(LocalDate inicio, LocalDate fim, List<Integer> daysOfMonth) {
    if (inicio == null || fim == null) return true;
    if (!inicio.plusDays(30).isAfter(fim)) return true; // >= 31 dias cobre todos os dias do mês
    Set<Integer> days = new HashSet<>(daysOfMonth);
    LocalDate d = inicio;
    while (!d.isAfter(fim)) {
      if (days.contains(d.getDayOfMonth())) return true;
      d = d.plusDays(1);
    }
    return false;
  }

  private boolean dateRangeOverlapsMonths(LocalDate inicio, LocalDate fim, List<Integer> months) {
    if (inicio == null || fim == null) return true;
    Set<Integer> monthsSet = new HashSet<>(months);
    LocalDate current = inicio.withDayOfMonth(1);
    while (!current.isAfter(fim)) {
      if (monthsSet.contains(current.getMonthValue())) return true;
      current = current.plusMonths(1);
    }
    return false;
  }

  // ── Helpers de conversão ──────────────────────────────────────────────────

  private Sazonalidade fromRequest(Sazonalidade.Request r) {
    return new Sazonalidade(null, r.descricao(),
        r.data_inicio(), r.data_fim(),
        r.diario_hora_inicio_ciclo(), r.diario_hora_fim_ciclo(),
        r.semanal(), r.mensal(), r.anual(),
        r.hora_checkin(), r.hora_checkout(), null);
  }

  private Sazonalidade fromUpdate(Sazonalidade.Update r) {
    return new Sazonalidade(r.id(), r.descricao(),
        r.data_inicio(), r.data_fim(),
        r.diario_hora_inicio_ciclo(), r.diario_hora_fim_ciclo(),
        r.semanal(), r.mensal(), r.anual(),
        r.hora_checkin(), r.hora_checkout(), null);
  }
}
