//package saas.hotel.istoepousada.dto;
//
//import com.fasterxml.jackson.annotation.JsonFormat;
//
//import java.time.LocalTime;
//import java.util.List;
//import java.util.Map;
//
//public record QuartoResponse(List<QuartoData> datas) {
//  public record QuartoData(
//      @JsonFormat(pattern = "dd/MM/yyyy") LocalDate data,
//      Integer quantidade_total_hospedados,
//      Integer total_quartos,
//      Integer quartos_ocupados,
//      List<Categoria> categorias) {
//    public record Categoria(
//        Long id,
//        String nome,
//        String descricao,
//        ModeloCobranca modelo,
//        List<Sazonalidade> sazonalidades,
//        List<Quarto> quartos) {
//
//      public record Quarto(
//          Long id,
//          String descricao,
//          Integer quantidade_pessoa,
//          Sta status,
//          Integer quantidade_cama_casal,
//          Integer quantidade_cama_solteiro,
//          Integer quantidade_beliche,
//          Integer quantidade_rede,
//          List<QuartoItem> quartoItens,
//          List<QuartoDetalhe> quartoDetalhes,
//          List<QuartoManutencao> quartoManutencoes,
//          List<QuartoLimpeza> quartoLimpezas,
//          Pernoite quartoPernoite,
//          DayUse quartoDayUse) {
//
//
//
//        public record QuartoDetalhe(
//                Long id,
//                Integer quantidade,
//                String descricao) {
//          public record Request(
//                  Integer quantidade,
//                  String descricao) {}
//        }
//
//        public record QuartoManutencao(
//            Long id,
//            Long quarto_id,
//            Funcionario funcionario,
//            String descricao,
//            @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_registro,
//            @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_inicio,
//            @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_fim,
//            String nome_responsavel,
//            Boolean ativo) {
//          public record Request(
//              Long quarto_id,
//              Long funcionario_id,
//              String descricao,
//              String nome_responsavel,
//              @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_inicio,
//              @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_fim,
//              Boolean ativo) {}
//        }
//
//
//      }
//    }
//  }
//
//  public enum Semanal {
//    SEGUNDA,
//    TERCA,
//    QUARTA,
//    QUINTA,
//    SEXTA,
//    SABADO,
//    DOMINGO
//  }
//
//  public enum Anual {
//    JANEIRO,
//    FEVEREIRO,
//    MARCO,
//    ABRIL,
//    MAIO,
//    JUNHO,
//    JULHO,
//    AGOSTO,
//    SETEMBRO,
//    OUTUBRO,
//    NOVEMBRO,
//    DEZEMBRO
//  }
//
//  public enum Mensal {
//    D1,
//    D2,
//    D3,
//    D4,
//    D5,
//    D6,
//    D7,
//    D8,
//    D9,
//    D10,
//    D11,
//    D12,
//    D13,
//    D14,
//    D15,
//    D16,
//    D17,
//    D18,
//    D19,
//    D20,
//    D21,
//    D22,
//    D23,
//    D24,
//    D25,
//    D26,
//    D27,
//    D28,
//    D29,
//    D30,
//    D31
//  }
//
//
//
//
//
//  public record Pagamento(
//      Long id,
//      Funcionario funcionario,
//      TipoPagamento tipo_pagamento,
//      @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_registro,
//      String nome_pagador,
//      String descricao,
//      Float valor,
//      Desconto desconto) {
//    public record Request(
//        Long funcionario_id,
//        Long tipo_pagamento_id,
//        String nome_pagador,
//        String descricao,
//        Float valor) {}
//
//    public record Desconto(
//        Long id,
//        Funcionario funcionario,
//        Integer porcentagem,
//        Float valor,
//        @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_registro) {
//      public record Request(Long funcionario_id, Integer porcentagem, Float valor) {}
//    }
//  }
//
//
//
//
//  public record DayUse(
//      Long id,
//      Quarto quarto,
//      Funcionario funcionario,
//      LocalTime hora_inicio,
//      LocalTime hora_fim,
//      @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_registro,
//      Status status,
//      List<Pessoa> pessoas,
//      List<Consumo> consumos,
//      List<Pagamento> pagamentos) {
//    public record Request(
//            Long quarto_id,
//            Long funcionario_id,
//            LocalTime hora_fim,
//            Status status,
//            List<Pessoa.Request.Id> pessoas_ids,
//            List<Consumo.Request> consumos,
//            List<Pagamento.Request> pagamentos
//    ){}
//    public enum Status {
//      ATIVO,
//      CANCELADO,
//      PAGAMENTO_PENDENTE,
//      FINALIZADO,
//      FINALIZADO_PAGAMENTO_PENDENTE
//    }
//  }
//
//  public record Sazonalidade(
//      Long id,
//      String descricao,
//      Funcionario funcionario,
//      @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_registro,
//      ModeloOperacao modeloOperacao,
//      ModeloCobranca modeloCobranca,
//      @JsonFormat(pattern = "dd/MM/yyyy") LocalDate data_inicio_sazonalidade,
//      @JsonFormat(pattern = "dd/MM/yyyy") LocalDate data_fim_sazonalidade,
//      Boolean diario_integral,
//      LocalTime diario_hora_inicio_ciclo,
//      LocalTime diario_hora_fim_ciclo,
//      Map<Semanal, String> semanal,
//      Map<Mensal, String> mensal,
//      Map<Anual, String> anual,
//      LocalTime hora_checkin,
//      LocalTime hora_checkout) {
//    public record Request(
//            Funcionario.Request.Id funcionario_id,
//            String descricao,
//            ModeloOperacao modeloOperacao,
//            ModeloCobranca modeloCobranca,
//            @JsonFormat(pattern = "dd/MM/yyyy") LocalDate data_inicio_sazonalidade,
//            @JsonFormat(pattern = "dd/MM/yyyy") LocalDate data_fim_sazonalidade,
//            Boolean diario_integral,
//            LocalTime diario_hora_inicio_ciclo,
//            LocalTime diario_hora_fim_ciclo,
//            Map<Semanal, String> semanal,
//            Map<Mensal, String> mensal,
//            Map<Anual, String> anual,
//            LocalTime hora_checkin,
//            LocalTime hora_checkout
//    ){}
//  }
//}
