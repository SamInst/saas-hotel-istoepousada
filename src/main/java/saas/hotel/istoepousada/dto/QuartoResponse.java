package saas.hotel.istoepousada.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

public record QuartoResponse(List<QuartoData> datas) {
  public record QuartoData(
      LocalDate data,
      Integer quantidade_total_hospedados,
      Integer total_quartos,
      Integer quartos_ocupados,
      List<Categoria> categorias) {
    /*tb: categoria*/
    public record Categoria(
        Long id,
        String nome,
        String descricao,
        ModeloCobranca modelo,
        List<Sazonalidade> sazonalidades,
        List<Quarto> quartos) {

      /*tb: quarto*/
      public record Quarto(
          Long id,
          String descricao,
          Integer quantidade_pessoa,
          StatusQuarto status,
          Integer quantidade_cama_casal,
          Integer quantidade_cama_solteiro,
          Integer quantidade_beliche,
          Integer quantidade_rede,
          List<QuartoItem> quartoItens,
          List<QuartoDetalhe> quartoDetalhes,
          List<QuartoManutencao> quartoManutencoes,
          List<QuartoLimpeza> quartoLimpezas,
          Pernoite quartoPernoite,
          DayUse quartoDayUse) {
        /*tb: quarto_item*/
        public record QuartoItem(
            Long id, Item item, Integer quantidade_atual, Integer quantidade_padrao) {
          /*tb: item*/
          public record Item(Long id, String descricao, Float valorVenda) {}
        }

        /*tb: quarto_detalhe*/
        public record QuartoDetalhe(Long id, Integer quantidade, String descricao) {}

        /*tb: quarto_manutencao*/
        public record QuartoManutencao(
            Long id,
            Long quarto_id,
            Funcionario funcionario,
            String descricao,
            LocalDateTime data_hora_registro,
            LocalDateTime data_hora_inicio,
            LocalDateTime data_hora_fim,
            String nome_responsavel,
            Boolean ativo) {}

        /*tb: quarto_limpeza*/
        public record QuartoLimpeza(
            Long id,
            Long quarto_id,
            Funcionario funcionario,
            LocalDateTime data_hora_registro,
            LocalDateTime data_hora_inicio,
            LocalDateTime data_hora_fim,
            Boolean ativo) {}
      }
    }
  }

  /*tb: funcionario*/
  public record Funcionario(Long id, String nome) {}

  public enum ModeloCobranca {
    OCUPACAO,
    TARIFA_FIXA
  }

  public enum ModeloOperacao {
    DATA_ESPECIFICA,
    DIARIO,
    SEMANAL,
    MENSAL,
    ANUAL
  }

  public enum StatusQuarto {
    OCUPADO,
    DISPONIVEL,
    RESERVADO,
    LIMPEZA,
    DIARIA_ENCERRADA,
    MANUTENCAO
  }

  public enum Semanal {
    SEGUNDA,
    TERCA,
    QUARTA,
    QUINTA,
    SEXTA,
    SABADO,
    DOMINGO
  }

  public enum Anual {
    JANEIRO,
    FEVEREIRO,
    MARCO,
    ABRIL,
    MAIO,
    JUNHO,
    JULHO,
    AGOSTO,
    SETEMBRO,
    OUTUBRO,
    NOVEMBRO,
    DEZEMBRO
  }

  public enum Mensal {
    D1,
    D2,
    D3,
    D4,
    D5,
    D6,
    D7,
    D8,
    D9,
    D10,
    D11,
    D12,
    D13,
    D14,
    D15,
    D16,
    D17,
    D18,
    D19,
    D20,
    D21,
    D22,
    D23,
    D24,
    D25,
    D26,
    D27,
    D28,
    D29,
    D30,
    D31
  }

  public enum PernoiteStatus {
    ATIVO,
    CANCELADO,
    PAGAMENTO_PENDENTE,
    FINALIZADO,
    FINALIZADO_PAGAMENTO_PENDENTE
  }

  public enum DayUseStatus {
    ATIVO,
    CANCELADO,
    PAGAMENTO_PENDENTE,
    FINALIZADO,
    FINALIZADO_PAGAMENTO_PENDENTE
  }

  public record Pagamento(
      Long id,
      Funcionario funcionario,
      TipoPagamento tipo_pagamento,
      LocalDateTime data_hora_registro,
      String nome_pagador,
      String descricao,
      Float valor,
      PagamentoDesconto pagamentoDesconto) {
    public record PagamentoDesconto(
        Long id,
        Funcionario funcionario,
        Integer porcentagem,
        Float valor,
        LocalDateTime data_hora_registro) {}
  }

  /*tb: consumo*/
  public record Consumo(
      Long id,
      Funcionario funcionario,
      Pagamento pagamento,
      Item item,
      Integer quantidade,
      LocalDateTime data_hora_registro) {
    public record Item(Long id, String descricao, Float valorVenda) {}
  }

  /*tb: day_use*/
  public record DayUse(
      Long id,
      Long quarto_id,
      Funcionario funcionario,
      LocalTime hora_inicio,
      LocalTime hora_fim,
      LocalDateTime data_hora_registro,
      DayUseStatus dayUseStatus) {}

  /* tb: pernoite */
  public record Pernoite(
      Long id,
      Funcionario funcionario,
      LocalDateTime data_hora_registro,
      LocalDateTime checkin,
      LocalDateTime checkout,
      PernoiteStatus pernoite_status,
      Integer quantidade_pessoas,
      List<Diaria> diarias) {
    /*tb: diaria */
    public record Diaria(
        Long id,
        Integer numero,
        Integer numero_diaria_atual,
        Long quarto_id,
        Long pernoite_id,
        LocalDateTime data_hora_inicio,
        LocalDateTime data_hora_fim,
        List<DiariaPessoa> pessoas,
        List<DiariaConsumo> diariaConsumos,
        List<DiariaPagamento> diariaPagamentos) {
      /*tb: diaria_pessoa */
      public record DiariaPessoa(
          Long id, String nome, Boolean titular, String numero_telefone, String email) {}

      /*tb: diaria_pagamento*/
      public record DiariaPagamento(Long id, Pagamento pagamento) {}

      /*tb: diaria_consumo*/
      public record DiariaConsumo(Long id, Consumo consumo) {}
    }
  }

  /*tb: sazonalidade*/
  public record Sazonalidade(
      Long id,
      String descricao,
      ModeloOperacao modeloOperacao,
      ModeloCobranca modeloCobranca,
      LocalDate data_inicio_sazonalidade,
      LocalDate data_fim_sazonalidade,
      Boolean diario_integral,
      LocalTime diario_hora_inicio_ciclo,
      LocalTime diario_hora_fim_ciclo,
      Map<Semanal, String> semanal,
      Map<Mensal, String> mensal,
      Map<Anual, String> anual,
      LocalTime hora_checkin,
      LocalTime hora_checkout) {}
}
