 package saas.hotel.istoepousada.dto;

 import com.fasterxml.jackson.annotation.JsonFormat;

 import java.time.LocalDate;
 import java.util.List;

 public record Hospedagem(List<QuartoData> datas) {
  public record QuartoData(
      @JsonFormat(pattern = "dd/MM/yyyy") LocalDate data,
      Integer quantidade_total_pessoas_hospedadas,
      List<Categoria> categorias) {
    public record Categoria(
        Long id,
        String nome,
        String descricao,
        List<Quarto> quartos) {

      public record Quarto(
          Long id,
          String descricao,
          Integer quantidade_pessoa,
          String status,
          Integer quantidade_cama_casal,
          Integer quantidade_cama_solteiro,
          Integer quantidade_beliche,
          Integer quantidade_rede,
          List<saas.hotel.istoepousada.dto.Quarto.QuartoItem> quarto_itens,
          List<saas.hotel.istoepousada.dto.Quarto.QuartoManutencao> quarto_manutencao,
          List<saas.hotel.istoepousada.dto.Quarto.QuartoLimpeza> quarto_limpeza,
          Pernoite quarto_pernoite,
          DayUse quarto_dayuse)
      {}
    }
  }

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





//  public record Pagamento(
//      Long uuid,
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
//        Long uuid,
//        Funcionario funcionario,
//        Integer porcentagem,
//        Float valor,
//        @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_registro) {
//      public record Request(Long funcionario_id, Integer porcentagem, Float valor) {}
//    }
//  }




//  public record DayUse(
//      Long uuid,
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


 }
