package saas.hotel.istoepousada.dto;

public record Endereco(
    String cep,
    String endereco,
    String bairro,
    String complemento,
    Objeto pais,
    Objeto estado,
    Objeto municipio) {}
