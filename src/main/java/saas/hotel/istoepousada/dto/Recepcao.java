package saas.hotel.istoepousada.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record Recepcao(List<QuartoData> datas) {
  public record QuartoData(
      @JsonFormat(pattern = "dd/MM/yyyy") LocalDate data,
      Integer quantidade_total_pessoas_hospedadas,
      List<Categoria> categorias) {
    public record Categoria(
        Long id,
        String nome,
        String descricao,
        @JsonFormat(pattern = "HH:mm") LocalTime checkin,
        @JsonFormat(pattern = "HH:mm") LocalTime checkout,
        List<Quartos> quartos) {
      public record Quartos(Quarto quarto, Hospedagem hospedagem) {}
    }
  }

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
