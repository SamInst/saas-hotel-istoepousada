package saas.hotel.istoepousada.repository;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestClient;
import saas.hotel.istoepousada.dto.CnpjaResponse;
import saas.hotel.istoepousada.dto.ViaCep;

@Repository
public class LocalidadeRepository {
  private final RestClient viaCepClient;
  private final RestClient cnpjaClient;

  public LocalidadeRepository(
      @Qualifier("viaCepClient") RestClient viaCepClient,
      @Qualifier("cnpjaClient") RestClient cnpjaClient) {
    this.viaCepClient = viaCepClient;
    this.cnpjaClient = cnpjaClient;
  }

  public ViaCep buscarPorCep(String cep) {
    String cepLimpo = limparCep(cep);
    return viaCepClient
        .get()
        .uri("/ws/{cep}/json/", cepLimpo)
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .body(ViaCep.class);
  }

  public CnpjaResponse buscarPorCnpj(String cnpj) {
    String cnpjLimpo = limparCnpj(cnpj);
    return cnpjaClient
        .get()
        .uri("/office/{cnpj}", cnpjLimpo)
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .body(CnpjaResponse.class);
  }

  private String limparCep(String cep) {
    if (cep == null) return "";
    return cep.replaceAll("\\D", "");
  }

  private String limparCnpj(String cnpj) {
    if (cnpj == null) return "";
    return cnpj.replaceAll("\\D", "");
  }
}
