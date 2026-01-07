package saas.hotel.istoepousada.service;

import java.util.List;
import org.springframework.stereotype.Service;
import saas.hotel.istoepousada.dto.Objeto;
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
}
