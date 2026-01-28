package saas.hotel.istoepousada.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import saas.hotel.istoepousada.dto.*;
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

      if (viaCep == null || Boolean.TRUE.equals(viaCep.erro())) {
        throw new RuntimeException("CEP não encontrado");
      }

      Objeto pais =
          localidadeRepository
              .buscarPaisPorNome("Brasil")
              .orElseThrow(
                  () -> new RuntimeException("País 'Brasil' não encontrado no banco de dados"));

      Objeto estado =
          localidadeRepository
              .buscarEstadoPorNome(viaCep.estado())
              .orElseThrow(
                  () ->
                      new RuntimeException(
                          "Estado '" + viaCep.estado() + "' não encontrado no banco de dados"));

      Objeto municipio =
          localidadeRepository
              .buscarMunicipioPorNome(viaCep.localidade(), estado.id())
              .orElseThrow(
                  () ->
                      new RuntimeException(
                          "Município '"
                              + viaCep.localidade()
                              + "' não encontrado no banco de dados"));

      return new Endereco(
          viaCep.cep().replaceAll("\\D", ""),
          viaCep.logradouro(),
          viaCep.bairro(),
          viaCep.complemento(),
          pais,
          estado,
          municipio);

    } catch (RestClientException e) {
      throw new RuntimeException("Erro ao consultar CEP na API ViaCEP: " + e.getMessage(), e);
    }
  }

  public EmpresaResponse buscarEmpresaPorCnpj(String cnpj) {
    try {
      CnpjaResponse cnpjaData = localidadeRepository.buscarPorCnpj(cnpj);

      if (cnpjaData == null || cnpjaData.address() == null)
        throw new RuntimeException("CNPJ não encontrado");

      String tipoEmpresa = formatarTipoEmpresa(cnpjaData.company().size());

      Endereco endereco = buscarEnderecoPorCep(cnpjaData.address().zip());
      String telefone = formatarTelefone(cnpjaData.phones());
      String email =
          cnpjaData.emails() != null && !cnpjaData.emails().isEmpty()
              ? cnpjaData.emails().getFirst().address()
              : null;

      Empresa.Status status =
          cnpjaData.status().text().contains("Ativa")
              ? Empresa.Status.ATIVO
              : Empresa.Status.BLOQUEADO;

      return new EmpresaResponse(
          formatarCnpj(cnpjaData.taxId()),
          cnpjaData.company().name(),
          cnpjaData.alias(),
          tipoEmpresa,
          status,
          formatarData(cnpjaData.founded()),
          endereco,
          telefone,
          email);

    } catch (RestClientException e) {
      throw new RuntimeException("Erro ao consultar CNPJ na API CNPJA: " + e.getMessage(), e);
    }
  }

  private String formatarTipoEmpresa(CnpjaResponse.Size size) {
    if (size == null) {
      return null;
    }
    return size.text() + " (" + size.acronym() + ")";
  }

  private String formatarTelefone(List<CnpjaResponse.Phone> phones) {
    if (phones == null || phones.isEmpty()) {
      return null;
    }

    CnpjaResponse.Phone phone = phones.getFirst();
    return "(" + phone.area() + ") " + phone.number();
  }

  private String formatarCnpj(String cnpj) {
    if (cnpj == null) {
      return null;
    }
    return cnpj.replaceAll("\\D", "");
  }

  private String formatarData(String data) {
    if (data == null || data.length() < 10) {
      return data;
    }

    String[] partes = data.substring(0, 10).split("-");
    if (partes.length == 3) {
      return partes[2] + "/" + partes[1] + "/" + partes[0];
    }

    return data;
  }
}
