package saas.hotel.istoepousada.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record Funcionario(
    @NotNull Long id,
    @NotNull Pessoa.DadosPrincipais pessoa,
    @NotNull @JsonFormat(pattern = "dd/MM/yyyy") LocalDate data_admissao,
    @NotNull Float salario,
    @NotNull Cargo cargo,
    Usuario usuario) {
  public record Id(@NotNull Long id){}

  public record Request(
      @NotNull Pessoa.Id pessoa,
      @NotNull LocalDate data_admissao,
      @NotNull Cargo.Id cargo,
      Usuario.Request usuario,
      @NotNull Float salario) {
  }
  public record Update(
          @NotNull Long id,
          @NotNull LocalDate data_admissao,
          @NotNull Cargo.Id cargo,
          @NotNull Float salario) {}

  public record Nome(
          @NotNull Long id,
          @NotNull String nome) {}

  public record Authorization(
          @NotNull Long id,
          @NotNull Usuario usuario,
          @NotNull Pessoa.DadosPrincipais pessoa,
          @NotNull @JsonFormat(pattern = "dd/MM/yyyy") LocalDate data_admissao,
          @NotNull Cargo cargo) {}

  public record Historico(
          @NotNull Long id,
          @NotNull Cargo.Descricao cargo,
          @NotNull Funcionario.Nome funcionario,
          @NotNull @JsonFormat(pattern = "dd/MM/yyyy") LocalDate data_admissao,
          @NotNull Float salario) {
    public record Id(@NotNull Long id){}
    public record Request(
            @NotNull Cargo.Id cargo,
            @NotNull Funcionario.Id funcionario,
            @NotNull Float salario
    ){}
    public record Update(
            @NotNull Long id,
            @NotNull Cargo.Id cargo,
            @NotNull Float salario
    ){}
    public record Recebido(
            @NotNull Long id,
            @NotNull Funcionario.Nome funcionario,
            @NotNull @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_inicio,
            @NotNull @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_fim,
            @NotNull @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_pagamento,
            @NotNull Pagamento pagamento,
            String path_arquivo
    ){
      public record Request(
              @NotNull Funcionario.Id funcionario,
              @NotNull Historico.Id historico,
              @NotNull @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_inicio,
              @NotNull @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_fim,
              @NotNull @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_pagamento,
              @NotNull Pagamento.Request pagamento,
              String path_arquivo
      ){}
      public record Update(
              @NotNull Long id,
              @NotNull @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_inicio,
              @NotNull @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_fim,
              @NotNull @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_pagamento,
              MultipartFile arquivo
      ){}
    }
  }
}
