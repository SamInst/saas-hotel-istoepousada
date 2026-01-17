package saas.hotel.istoepousada.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import saas.hotel.istoepousada.dto.Endereco;
import saas.hotel.istoepousada.dto.Objeto;
import saas.hotel.istoepousada.dto.ViaCep;
import saas.hotel.istoepousada.repository.LocalidadeRepository;

@Service
public class EnderecoService {

  private final LocalidadeRepository localidadeRepository;

  public EnderecoService(LocalidadeRepository localidadeRepository) {
    this.localidadeRepository = localidadeRepository;
  }

  public List<Objeto> listarPaises() {
    return localidadeRepository.listarPaises();
  }

  public List<Objeto> listarEstados(Long pais) {
    return localidadeRepository.listarEstadosPorPais(pais);
  }

  public List<Objeto> listarMunicipios(Long estado) {
    return localidadeRepository.listarMunicipiosPorEstado(estado);
  }

  public Endereco buscarEnderecoPorCep(String cep) {
    try {
      ViaCep viaCep = localidadeRepository.buscarPorCep(cep);

      if (viaCep == null || Boolean.TRUE.equals(viaCep.erro()))
        throw new RuntimeException("CEP não encontrado");

      Objeto pais = localidadeRepository.buscarPaisPorNome("Brasil")
              .orElseThrow(() -> new RuntimeException("País 'Brasil' não encontrado no banco de dados"));

      Objeto estado = localidadeRepository.buscarEstadoPorNome(viaCep.estado())
              .orElseThrow(() -> new RuntimeException("Estado '" + viaCep.estado() + "' não encontrado no banco de dados"));

      Objeto municipio = localidadeRepository.buscarMunicipioPorNome(viaCep.localidade(), estado.id())
              .orElseThrow(() -> new RuntimeException("Município '" + viaCep.localidade() + "' não encontrado no banco de dados"));

      return new Endereco(
              viaCep.cep().replaceAll("\\D", ""),
              viaCep.logradouro(),
              viaCep.bairro(),
              viaCep.complemento(),
              pais,
              estado,
              municipio
      );

    } catch (RestClientException e) {
      throw new RuntimeException("Erro ao consultar CEP na API ViaCEP: " + e.getMessage(), e);
    }
  }
}
