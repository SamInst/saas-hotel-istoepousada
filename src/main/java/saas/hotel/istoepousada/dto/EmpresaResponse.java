package saas.hotel.istoepousada.dto;

public record EmpresaResponse(
    String cnpj,
    String razaoSocial,
    String nomeFantasia,
    String tipoEmpresa,
    Empresa.Status status,
    String dataAbertura,
    Endereco endereco,
    String telefone,
    String email) {}
