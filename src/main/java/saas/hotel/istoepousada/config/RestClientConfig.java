package saas.hotel.istoepousada.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

  @Bean("viaCepClient")
  public RestClient restClient() {
    return RestClient.builder().baseUrl("https://viacep.com.br").build();
  }

  @Bean("cnpjaClient")
  public RestClient cnpjaClient() {
    return RestClient.builder().baseUrl("https://open.cnpja.com").build();
  }
}
