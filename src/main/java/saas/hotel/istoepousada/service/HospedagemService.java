package saas.hotel.istoepousada.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import saas.hotel.istoepousada.dto.*;
import saas.hotel.istoepousada.handler.exceptions.NotFoundException;
import saas.hotel.istoepousada.repository.HospedagemRepository;

@Service
public class HospedagemService {
  private static final Logger log = LoggerFactory.getLogger(HospedagemService.class);
  private final HospedagemRepository hospedagemRepository;
  private final PagamentoService pagamentoService;
  private final PessoaService pessoaService;
  private final CalcularPrecoService calcularPrecoService;

  public HospedagemService(
      HospedagemRepository hospedagemRepository,
      PagamentoService pagamentoService,
      PessoaService pessoaService,
      CalcularPrecoService calcularPrecoService) {
    this.hospedagemRepository = hospedagemRepository;
    this.pagamentoService = pagamentoService;
    this.pessoaService = pessoaService;
    this.calcularPrecoService = calcularPrecoService;
  }

  public Boolean isQuartoDisponivel(
      Long quartoId, LocalDateTime checkin, LocalDateTime checkout, Long hospedagemIdExcluido) {
    log.info(
        "Validando disponibilidade do quarto {} para checkin {} e checkout {}",
        quartoId,
        checkin,
        checkout);

    var statusAtual = hospedagemRepository.statusQuarto(quartoId);

    if (statusAtual != Quarto.Status.OCUPADO) {
      log.info("Quarto não disponivel: STATUS {}", Quarto.Status.OCUPADO);
      return true;
    }

    var hospedagemConflito =
        hospedagemRepository.isQuartoDisponivel(quartoId, checkin, checkout, hospedagemIdExcluido);

    log.info("Quarto disponivel: {}", hospedagemConflito);
    return hospedagemConflito;
  }

  public List<Hospedagem.Diaria> listarDiarias(Long hospedagemId) {
    return hospedagemRepository.listarDiarias(hospedagemId);
  }

  private Boolean isDiariaJaExiste(Long hospedagemId, Hospedagem.Diaria.Request diaria) {
    return hospedagemRepository.isDiariaJaExiste(hospedagemId, diaria);
  }

  public void adicionarDiarias(Long hospedagemId, List<Hospedagem.Diaria.Request> diarias) {
    hospedagemRepository.buscarPorId(hospedagemId);

    List<CalcularPreco.Request> calcularPrecoRequests = new ArrayList<>();
    List<Hospedagem.Diaria.Request> diariasNaoCadastradas = new ArrayList<>();

    diarias.forEach(
        diaria -> {
          log.info(
              "Adicionando diarias para a hospedagem {} e quarto {}",
              hospedagemId,
              diaria.quarto_id());
          if (isDiariaJaExiste(hospedagemId, diaria)) {
            log.info(
                "Diária [{}>{}] já existe para a hospedagem {} e quarto {}",
                diaria.checkin(),
                diaria.checkout(),
                hospedagemId,
                diaria.quarto_id());
            return;
          }
          if (!isQuartoDisponivel(
              diaria.quarto_id(), diaria.checkin(), diaria.checkout(), hospedagemId)) {
            log.info(
                "Diaria: [{}>{}] não disponivel para o quarto: {}. O apartamento se encontra indisponível ou ocupado por outra hospedagem.",
                diaria.checkin(),
                diaria.checkout(),
                diaria.quarto_id());
            throw new IllegalStateException(
                "Diaria: ["
                    + diaria.checkin()
                    + ">"
                    + diaria.checkout()
                    + "] não disponivel para o quarto: "
                    + diaria.quarto_id()
                    + ". O apartamento se encontra indisponivel ou ocupado por outra hospedagem.");
          }

          List<LocalDate> datasNascimento = new ArrayList<>();

          diaria
              .pessoas()
              .forEach(
                  pessoaId -> {
                    Pessoa pessoa = pessoaService.findById(pessoaId);
                    datasNascimento.add(pessoa.data_nascimento());
                    log.info(
                        "Adicionando pessoa {} para a hospedagem {} e quarto {}",
                        pessoaId,
                        hospedagemId,
                        diaria.quarto_id());
                  });

          calcularPrecoRequests.add(
              new CalcularPreco.Request(
                  diaria.quarto_id(),
                  diaria.checkin().toLocalDate(),
                  diaria.checkout().toLocalDate(),
                  datasNascimento,
                  null,
                  null));
          diariasNaoCadastradas.add(diaria);
        });
    var resultadoCalculo = calcularPrecoService.calcularPreco(calcularPrecoRequests);

    hospedagemRepository.adicionarDiarias(
        hospedagemId, diariasNaoCadastradas, resultadoCalculo.getFirst().valor_total());
  }

  public void validarTransicaoDeStatus(Hospedagem.Status anterior, Hospedagem.Status novo) {
    Map<Hospedagem.Status, Set<Hospedagem.Status>> transicoesPermitidas =
        Map.ofEntries(
            Map.entry(
                Hospedagem.Status.ORCAMENTO,
                EnumSet.of(
                    Hospedagem.Status.ORCAMENTO_CANCELADO, Hospedagem.Status.RESERVA_SOLICITADA)),
            Map.entry(
                Hospedagem.Status.ORCAMENTO_CANCELADO, EnumSet.of(Hospedagem.Status.ORCAMENTO)),
            Map.entry(
                Hospedagem.Status.RESERVA_SOLICITADA,
                EnumSet.of(Hospedagem.Status.RESERVA_ATIVA, Hospedagem.Status.RESERVA_CANCELADA)),
            Map.entry(
                Hospedagem.Status.RESERVA_ATIVA,
                EnumSet.of(
                    Hospedagem.Status.RESERVA_CANCELADA,
                    Hospedagem.Status.RESERVA_AUSENTE,
                    Hospedagem.Status.PERNOITE_ATIVA)),
            Map.entry(
                Hospedagem.Status.RESERVA_AUSENTE, EnumSet.of(Hospedagem.Status.RESERVA_ATIVA)),
            Map.entry(
                Hospedagem.Status.PERNOITE_ATIVA,
                EnumSet.of(
                    Hospedagem.Status.PERNOITE_CANCELADA,
                    Hospedagem.Status.PERNOITE_FINALIZADA,
                    Hospedagem.Status.PERNOITE_FINALIZADA_PAGAMENTO_PENDENTE)),
            Map.entry(
                Hospedagem.Status.PERNOITE_FINALIZADA_PAGAMENTO_PENDENTE,
                EnumSet.of(Hospedagem.Status.PERNOITE_FINALIZADA)),
            Map.entry(
                Hospedagem.Status.DAY_USE_SOLICITADA,
                EnumSet.of(
                    Hospedagem.Status.DAY_USE_AUSENTE,
                    Hospedagem.Status.DAY_USE_ATIVA,
                    Hospedagem.Status.DAY_USE_CANCELADA)),
            Map.entry(
                Hospedagem.Status.DAY_USE_ATIVA,
                EnumSet.of(
                    Hospedagem.Status.DAY_USE_FINALIZADA,
                    Hospedagem.Status.DAY_USE_FINALIZADA_PAGAMENTO_PENDENTE)),
            Map.entry(
                Hospedagem.Status.DAY_USE_FINALIZADA_PAGAMENTO_PENDENTE,
                EnumSet.of(Hospedagem.Status.DAY_USE_FINALIZADA)));

    Set<Hospedagem.Status> estadosFinais =
        EnumSet.of(
            Hospedagem.Status.RESERVA_CANCELADA,
            Hospedagem.Status.PERNOITE_CANCELADA,
            Hospedagem.Status.PERNOITE_FINALIZADA,
            Hospedagem.Status.DAY_USE_CANCELADA,
            Hospedagem.Status.DAY_USE_FINALIZADA);

    if (estadosFinais.contains(anterior)) {
      throw new IllegalStateException(
          "Status " + anterior + " é um estado final e não pode ser alterado.");
    }

    Set<Hospedagem.Status> permitidos = transicoesPermitidas.get(anterior);
    if (permitidos == null || !permitidos.contains(novo)) {
      throw new IllegalStateException("Transição de status inválida: " + anterior + " -> " + novo);
    }
  }

  public void removerPessoas(Long hospedagemId, List<Long> pessoasIds) {
    hospedagemRepository.buscarPorId(hospedagemId);
    List<Long> pessoasExistentes =
        hospedagemRepository.filtrarPessoasExistentes(hospedagemId, pessoasIds);
    if (pessoasExistentes.isEmpty()) return;
    hospedagemRepository.removerPessoas(hospedagemId, pessoasExistentes);
    log.info("Pessoas {} removidas da hospedagem {}", pessoasExistentes, hospedagemId);
  }

  public void adicionarPessoas(Long hospedagemId, List<Long> pessoasIds) {
    hospedagemRepository.buscarPorId(hospedagemId);
    List<Long> pessoasPendentes =
        hospedagemRepository.filtrarPessoasDuplicadas(hospedagemId, pessoasIds);
    if (pessoasPendentes.isEmpty()) return;
    hospedagemRepository.adicionarPessoas(hospedagemId, pessoasPendentes);
  }

  public List<Long> adicionarOrcamentos(Long hospedagemId, List<Orcamento.Request> requests) {
    if (hospedagemId == null
        || hospedagemId <= 0
        || hospedagemRepository.buscarPorId(hospedagemId) == null) {
      throw new NotFoundException("Hospedagem não encontrada para o id: " + hospedagemId);
    }
    validarCamposOrcamento(requests, false);
    return hospedagemRepository.adicionarOrcamentos(hospedagemId, requests, getFuncionarioId());
  }

  private void validarCamposOrcamento(List<Orcamento.Request> requests, Boolean isUpdate) {
    if (requests == null || requests.isEmpty())
      throw new IllegalArgumentException("Requisição de Orcamento não informada");

    requests.forEach(
        request -> {
          if (isUpdate) {
            if (request.id() == null || request.id() <= 0) {
              throw new IllegalArgumentException("Id de orçamento não informado ou inválido");
            }
          }
          if (request.nome_solicitante() == null || request.nome_solicitante().isEmpty()) {
            throw new IllegalArgumentException("Nome do solicitante não informado");
          }
          if (request.categoria() == null) {
            throw new IllegalArgumentException("Categoria do solicitante não informado");
          } else if (request.categoria().id() == null || request.categoria().id() <= 0) {
            throw new IllegalArgumentException(
                "Categoria do solicitante não informado ou inválido");
          }
          if (request.checkin() == null) {
            throw new IllegalArgumentException("Data de checkin não informada");
          } else if (request.checkin().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException(
                "Data de checkin não pode ser anterior a data atual");
          }
          if (request.checkout() == null) {
            throw new IllegalArgumentException("Data de checkout não informada");
          } else if (request.checkout().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException(
                "Data de checkout não pode ser anterior a data atual");
          }
          if (request.checkin().equals(request.checkout())) {
            throw new IllegalArgumentException("Data de checkin e checkout devem ser diferentes");
          }
        });
  }

  private void validarCamposPessoasOrcamento(
      List<Orcamento.Pessoa.Request> pessoas, Boolean isUpdate) {
    pessoas.forEach(
        request -> {
          if (request == null) {
            throw new IllegalArgumentException("Requisição de pessoas no orçamento não informada");
          }
          if (isUpdate) {
            if (request.id() == null || request.id() <= 0) {
              throw new IllegalArgumentException("Id do orçamento não informado ou inválido");
            }
          }
          if (request.nome() == null || request.nome().isEmpty()) {
            throw new IllegalArgumentException("Nome da pessoa não informado");
          }
          if (request.data_nascimento() == null) {
            throw new IllegalArgumentException("Data de nascimento não informada");
          } else if (request.data_nascimento().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Data de nascimento inválida");
          }
        });
  }

  public void editarOrcamento(Orcamento.Request request) {
    validarCamposOrcamento(List.of(request), true);
    hospedagemRepository.editarOrcamento(request);
  }

  public void adicionarPessoasOrcamento(Long orcamentoId, List<Orcamento.Pessoa.Request> pessoas) {
    validarCamposPessoasOrcamento(pessoas, false);
    hospedagemRepository.adicionarPessoasOrcamento(orcamentoId, pessoas);
  }

  public void removerPessoasOrcamento(Long orcamentoId, List<Long> pessoasIds) {
    hospedagemRepository.removerPessoasOrcamento(orcamentoId, pessoasIds);
  }

  public Orcamento buscarOrcamento(Long hospedagemId) {
    return hospedagemRepository.buscarOrcamento(hospedagemId);
  }

  public void adicionarHospedagemPagamento(Long hospedagemId, List<UUID> pagamentosUUID) {
    hospedagemRepository.buscarPorId(hospedagemId);
    hospedagemRepository.adicionarHospedagemPagamento(hospedagemId, pagamentosUUID);
  }

  private Long getFuncionarioId() {
    return pessoaService.getFuncionarioIdFromRequest();
  }

  private void validarCampos(Hospedagem.Request request) {
    validarCampoPessoas(request);
    validarCamposPagamento(request);
    if (request.quarto_id() == null)
      throw new IllegalArgumentException("E necessário informar o quarto da hospedagem.");
    if (request.data_hora_checkin() == null)
      throw new IllegalArgumentException("E necessário informar a data de checkin da hospedagem.");
    if (request.data_hora_checkout() == null)
      throw new IllegalArgumentException("E necessário informar a data de checkout da hospedagem.");
    if (request.data_hora_checkin().isAfter(request.data_hora_checkout()))
      throw new IllegalArgumentException("A data de checkin deve ser anterior a data de checkout.");
    if (request.status() == null)
      throw new IllegalArgumentException("E necessário informar o status da hospedagem.");
  }

  public void validarCampoPessoas(Hospedagem.Request request) {
    if ((request.pessoas() == null || request.pessoas().isEmpty()) && request.status() != Hospedagem.Status.ORCAMENTO)
      throw new IllegalArgumentException("A hospedagem deve ter pelo menos uma pessoa.");
  }

  public void validarCamposPagamento(Hospedagem.Request request) {
    if (request.pagamentos() != null) {
      request
          .pagamentos()
          .forEach(
              pagamento -> {
                if (pagamento.tipo_pagamento() == null)
                  throw new IllegalArgumentException("Forma de pagamento não informada");
                if (pagamento.descricao() == null)
                  throw new IllegalArgumentException("Descrição do pagamento não informada");
                if (pagamento.valor() == null)
                  throw new IllegalArgumentException("Valor do pagamento não informado");
                if (pagamento.nome_pagador() == null)
                  throw new IllegalArgumentException("Nome do pagador não informado");
                if (pagamento.arquivo() != null) {
                  if (pagamento.arquivo().isEmpty())
                    throw new IllegalArgumentException("Arquivo do pagamento não informado");
                }
              });
    }
  }

  @Transactional
  public void salvarHospedagem(List<Hospedagem.Request> hospedagens) {
    hospedagens.forEach(
        request -> {
          validarCampos(request);

          Hospedagem hospedagem;
          if (request.hospedagem_id() == null) {
            isQuartoDisponivel(
                request.quarto_id(),
                request.data_hora_checkin(),
                request.data_hora_checkout(),
                null);
            hospedagem = hospedagemRepository.insertHospedagem(request, getFuncionarioId());
          } else {

            hospedagem = hospedagemRepository.buscarPorId(request.hospedagem_id());
            validarTransicaoDeStatus(hospedagem.status(), request.status());
          }

          switch (request.status()) {
            case ORCAMENTO:
              {
                var orcamentosIds = adicionarOrcamentos(hospedagem.id(), request.orcamentos());

                request
                    .orcamentos()
                    .forEach(
                        requestOrcamento -> {
                          log.info(
                              "Adicionando requisição de orcamento para hospedagem: [{}], nas datas: [{},{}]",
                              hospedagem.id(),
                              requestOrcamento.checkin(),
                              requestOrcamento.checkout());
                          orcamentosIds.forEach(
                              orcamentoId -> {
                                adicionarPessoasOrcamento(orcamentoId, requestOrcamento.pessoas());
                                log.info(
                                    "Orcamento: [{}] adicionado pessoas: [{}]",
                                    orcamentoId,
                                    requestOrcamento.pessoas());
                              });
                        });

                alterarStatus(request.hospedagem_id(), Hospedagem.Status.ORCAMENTO);
              }
            case RESERVA_ATIVA:
              {
                adicionarPessoas(hospedagem.id(), request.pessoas());
                adicionarPagamentos(hospedagem.id(), request);
                alterarStatus(hospedagem.id(), Hospedagem.Status.RESERVA_ATIVA);
              }
            case PERNOITE_ATIVA:
              {
                List<Hospedagem.Diaria.Request> diarias =
                    new ArrayList<>(
                        List.of(
                            new Hospedagem.Diaria.Request(
                                request.quarto_id(),
                                request.data_hora_checkin(),
                                request.data_hora_checkout(),
                                request.pessoas(),
                                null)));

                adicionarDiarias(hospedagem.id(), diarias);
                adicionarPessoas(hospedagem.id(), request.pessoas());
                adicionarPagamentos(hospedagem.id(), request);
                alterarStatus(hospedagem.id(), Hospedagem.Status.PERNOITE_ATIVA);
              }
            case ORCAMENTO_CANCELADO:
              {
                if (request.motivo_cancelamento() == null)
                  throw new IllegalArgumentException("Motivo Cancelamento nao informado");
                adicionarMotivoCancelamento(request.motivo_cancelamento());
                alterarStatus(hospedagem.id(), Hospedagem.Status.ORCAMENTO_CANCELADO);
              }
            case RESERVA_CANCELADA:
              {
                if (request.motivo_cancelamento() == null)
                  throw new IllegalArgumentException("Motivo Cancelamento nao informado");
                adicionarMotivoCancelamento(request.motivo_cancelamento());
                alterarStatus(hospedagem.id(), Hospedagem.Status.RESERVA_CANCELADA);
              }
            case RESERVA_AUSENTE:
              {
                alterarStatus(hospedagem.id(), Hospedagem.Status.RESERVA_AUSENTE);
              }
            case PERNOITE_CANCELADA:
              {
                if (request.motivo_cancelamento() == null)
                  throw new IllegalArgumentException("Motivo Cancelamento nao informado");
                adicionarMotivoCancelamento(request.motivo_cancelamento());
                alterarStatus(hospedagem.id(), Hospedagem.Status.PERNOITE_CANCELADA);
              }
            case PERNOITE_FINALIZADA:
              {
                alterarStatus(hospedagem.id(), Hospedagem.Status.PERNOITE_FINALIZADA);
              }
            case PERNOITE_FINALIZADA_PAGAMENTO_PENDENTE:
              {
                alterarStatus(
                    hospedagem.id(), Hospedagem.Status.PERNOITE_FINALIZADA_PAGAMENTO_PENDENTE);
              }
              //            case DAY_USE_SOLICITADA: {
              //            }
              //            case DAY_USE_ATIVA: {
              //            }
              //            case DAY_USE_CANCELADA: {
              //            }
              //            case DAY_USE_FINALIZADA: {
              //            }
              //            case DAY_USE_FINALIZADA_PAGAMENTO_PENDENTE: {
              //            }
              //            case DAY_USE_AUSENTE: {
              //                alterarStatus(request.id(), Hospedagem.Status.DAY_USE_AUSENTE);
              //            }
            default:
              {
                throw new IllegalStateException(
                    "Status inválido para registro de request: " + request.status());
              }
          }
        });
  }

  public void adicionarPagamentos(Long hospedagemId, Hospedagem.Request request) {
    if (request.pagamentos() != null && !request.pagamentos().isEmpty()) {
      List<UUID> pagamentosUUID = new ArrayList<>();
      request
          .pagamentos()
          .forEach(
              pagamento -> {
                var newPagamento = pagamentoService.criar(pagamento);
                pagamentosUUID.add(newPagamento.uuid());
              });
      adicionarHospedagemPagamento(hospedagemId, pagamentosUUID);
    }
  }

  public void alterarStatus(Long hospedagemId, Hospedagem.Status status) {
    hospedagemRepository.alterarStatus(hospedagemId, status);
    log.info("Status da hospedagem: [{}] alterado para: [{}]", hospedagemId, status);
  }

  public MotivoCancelamentoHospedagem buscarMotivoCancelamento(Long hospedagemId) {
    return hospedagemRepository.buscarMotivoCancelamento(hospedagemId);
  }

  public void adicionarMotivoCancelamento(MotivoCancelamentoHospedagem.Request request) {
    validarCamposMotivoCancelamento(request, false);
    hospedagemRepository.adicionarMotivoCancelamento(request, getFuncionarioId());
    log.info("Motivo cancelamento: [{}]", request.motivo_cancelamento());
  }

  public void editarMotivoCancelamento(MotivoCancelamentoHospedagem.Request request) {
    validarCamposMotivoCancelamento(request, true);
    hospedagemRepository.editarMotivoCancelamento(request);
    log.info("Motivo cancelamento: [{}]", request.motivo_cancelamento());
  }

  private void validarCamposMotivoCancelamento(
      MotivoCancelamentoHospedagem.Request request, Boolean isUpdate) {
    if (isUpdate) {
      if (request.id() == null || request.id() <= 0) {
        throw new IllegalArgumentException("Id do motivo de cancelamento nao informado");
      }
    }
    if (request.motivo_cancelamento() == null || request.motivo_cancelamento().isEmpty()) {
      throw new IllegalArgumentException("Motivo do cancelamento nao informado");
    }
  }

  public Map<Long, Hospedagem> buscarAtivasPorQuartoNaData(LocalDate data) {
    return hospedagemRepository.buscarAtivasPorQuartoNaData(data);
  }

  public boolean temReservaAtivaParaQuartoHoje(Long quartoId) {
    return hospedagemRepository.temReservaAtivaParaQuartoHoje(quartoId);
  }

  public void adicionarConsumo(Long hospedagemId, Item.Consumo.Request request) {
    hospedagemRepository.buscarPorId(hospedagemId);
    validarCamposConsumo(request, false);
    UUID pagamentoId = null;
    if (request.pagamento() != null) {
      pagamentoId = pagamentoService.criar(request.pagamento()).uuid();
    }
    hospedagemRepository.adicionarConsumo(hospedagemId, request, pagamentoId, getFuncionarioId());
  }

  public void editarConsumo(Item.Consumo.Request request) {
    validarCamposConsumo(request, true);
    hospedagemRepository.editarConsumo(request);
  }

  public List<Item.Consumo> buscarConsumosPorHospedagem(Long hospedagemId) {
    return hospedagemRepository.buscarConsumosPorHospedagem(hospedagemId);
  }

  private void validarCamposConsumo(Item.Consumo.Request request, Boolean isUpdate) {
    if (isUpdate) {
      if (request.id() == null || request.id() <= 0) {
        throw new IllegalArgumentException("Id do consumo nao informado");
      }
    }
    if (request.despesa_pessoal() == null) {
      throw new IllegalArgumentException("Despesa pessoal nao informada");
    }
    if (request.item() == null || request.item().id() == null || request.item().id() <= 0) {
      throw new IllegalArgumentException("Item nao informado");
    }
    if (request.quantidade() == null || request.quantidade() <= 0) {
      throw new IllegalArgumentException("Quantidade nao informada");
    }
    if (request.quarto() != null) {
      if (request.quarto().id() == null || request.quarto().id() <= 0) {
        throw new IllegalArgumentException("Quarto nao informado");
      }
    }
  }
}
