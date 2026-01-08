package saas.hotel.istoepousada.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import saas.hotel.istoepousada.dto.Objeto;
import saas.hotel.istoepousada.repository.LocalidadeRepository;

@ExtendWith(MockitoExtension.class)
class EnderecoServiceTest {
  @Mock private LocalidadeRepository localidadeRepository;

  @InjectMocks private EnderecoService enderecoService;

  private List<Objeto> objetosMock;

  @BeforeEach
  void setup() {
    objetosMock = List.of(new Objeto(1L, "Item 1"), new Objeto(2L, "Item 2"));
  }

  @Test
  void deveListarPaises() {
    when(localidadeRepository.listarPaises()).thenReturn(objetosMock);
    List<Objeto> result = enderecoService.listarPaises();
    assertEquals(objetosMock, result);
    verify(localidadeRepository).listarPaises();
  }

  @Test
  void deveListarEstadosPorPais() {
    Long paisId = 1L;
    when(localidadeRepository.listarEstadosPorPais(paisId)).thenReturn(objetosMock);
    List<Objeto> result = enderecoService.listarEstados(paisId);
    assertEquals(objetosMock, result);
    verify(localidadeRepository).listarEstadosPorPais(paisId);
  }

  @Test
  void deveListarMunicipiosPorEstado() {
    Long estadoId = 10L;
    when(localidadeRepository.listarMunicipiosPorEstado(estadoId)).thenReturn(objetosMock);
    List<Objeto> result = enderecoService.listarMunicipios(estadoId);
    assertEquals(objetosMock, result);
    verify(localidadeRepository).listarMunicipiosPorEstado(estadoId);
  }
}
