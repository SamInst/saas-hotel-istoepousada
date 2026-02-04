package saas.hotel.istoepousada.service;

import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import saas.hotel.istoepousada.dto.Cargo;
import saas.hotel.istoepousada.dto.Tela;
import saas.hotel.istoepousada.handler.exceptions.NotFoundException;
import saas.hotel.istoepousada.repository.CargoTelaPermissaoRepository;

@Service
public class CargoTelaPermissaoService {

  private final CargoTelaPermissaoRepository cargoTelaPermissaoRepository;

  public CargoTelaPermissaoService(CargoTelaPermissaoRepository cargoTelaPermissaoRepository) {
    this.cargoTelaPermissaoRepository = cargoTelaPermissaoRepository;
  }

  public Page<Cargo> listar(Long id, String termo, Long pessoaId, Pageable pageable) {
    return cargoTelaPermissaoRepository.buscarCargoPorIdOuNome(id, termo, pessoaId, pageable);
  }

  public Cargo save(Cargo.Request request) {
    if (request.descricao() == null || request.descricao().trim().isEmpty())
      throw new IllegalArgumentException("Nome do cargo é obrigatório.");

    Cargo cargo;
    if (request.id() == null) cargo = insert(request);
    else cargo = update(request);

    var telas = request.telasIds();
    if (telas != null && !telas.isEmpty()) {
      cargoTelaPermissaoRepository.vincularCargoTelas(
          cargo.id(), request.telasIds().stream().map(Tela.Request::id).toList(), true);
    }

    return cargoTelaPermissaoRepository.findByIdOrThrow(cargo.id());
  }

  public Cargo update(Cargo.Request cargo) {
    if (cargo.id() == null) throw new IllegalArgumentException("ID do cargo é obrigatório.");
    if (!cargoTelaPermissaoRepository.existsById(cargo.id()))
      throw new NotFoundException("Cargo não cadastrado para o id: " + cargo.id());
    if (cargo.descricao() == null || cargo.descricao().trim().isEmpty())
      throw new IllegalArgumentException("Nome do cargo é obrigatório.");
    return cargoTelaPermissaoRepository.update(cargo);
  }

  public Cargo insert(Cargo.Request cargo) {
    if (cargo.descricao() == null || cargo.descricao().trim().isEmpty()) {
      throw new IllegalArgumentException("Nome do cargo é obrigatório.");
    }
    return cargoTelaPermissaoRepository.insert(cargo);
  }

  public void vincularCargoTelas(Long cargoId, List<Long> telaIds, Boolean vinculo) {
    if (cargoId == null) throw new IllegalArgumentException("ID do cargo é obrigatório.");
    if (telaIds == null || telaIds.isEmpty()) {
      return;
    }
    cargoTelaPermissaoRepository.vincularCargoTelas(cargoId, telaIds, vinculo);
  }

  public void vincularPermissoesPessoa(Long pessoaId, List<Long> permissaoIds, Boolean vinculo) {
    if (pessoaId == null) throw new IllegalArgumentException("pessoaId é obrigatório.");
    if (permissaoIds == null || permissaoIds.isEmpty()) return;
    List<Long> ids = permissaoIds.stream().filter(Objects::nonNull).distinct().toList();
    if (ids.isEmpty()) return;
    cargoTelaPermissaoRepository.vincularPermissoesPessoa(pessoaId, ids, vinculo);
  }
}
