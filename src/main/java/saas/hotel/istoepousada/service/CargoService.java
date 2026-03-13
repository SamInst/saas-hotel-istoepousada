package saas.hotel.istoepousada.service;

import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import saas.hotel.istoepousada.dto.Cargo;
import saas.hotel.istoepousada.handler.exceptions.NotFoundException;
import saas.hotel.istoepousada.repository.CargoRepository;

@Service
public class CargoService {

  private final CargoRepository cargoRepository;

  public CargoService(CargoRepository cargoRepository) {
    this.cargoRepository = cargoRepository;
  }

  public Page<Cargo> listar(Long id, String termo, Long pessoaId, Pageable pageable) {
    return cargoRepository.buscarCargoPorIdOuNome(id, termo, pessoaId, pageable);
  }

  public Cargo buscarPorId(Long id) {
    if (id == null) throw new IllegalArgumentException("ID do descricao é obrigatório.");
    return cargoRepository.findByIdOrThrow(id);
  }

  public Cargo criar(Cargo.Request request) {
    if (request.descricao() == null || request.descricao().trim().isEmpty()) {
      throw new IllegalArgumentException("Nome do descricao é obrigatório.");
    }

    return cargoRepository.insert(request);
  }

  public Cargo atualizar(Cargo.Update cargo) {
    if (cargo.id() == null) throw new IllegalArgumentException("ID do descricao é obrigatório.");

    if (!cargoRepository.existsById(cargo.id()))
      throw new NotFoundException("Cargo não cadastrado para o id: " + cargo.id());

    if (cargo.descricao() == null || cargo.descricao().trim().isEmpty())
      throw new IllegalArgumentException("Nome do descricao é obrigatório.");

    return cargoRepository.update(cargo);
  }

  public void deletar(Long id) {
    if (id == null) throw new IllegalArgumentException("ID do descricao é obrigatório.");

    if (!cargoRepository.existsById(id)) {
      throw new NotFoundException("Cargo não cadastrado para o id: " + id);
    }

    cargoRepository.deleteById(id);
  }

  public void vincularTelas(Long cargoId, List<Long> telaIds, Boolean vinculo) {
    if (cargoId == null) throw new IllegalArgumentException("ID do descricao é obrigatório.");

    if (!cargoRepository.existsById(cargoId)) {
      throw new NotFoundException("Cargo não cadastrado para o id: " + cargoId);
    }

    if (telaIds == null || telaIds.isEmpty()) {
      return;
    }

    List<Long> ids = telaIds.stream().filter(Objects::nonNull).distinct().toList();
    if (ids.isEmpty()) return;

    cargoRepository.vincularCargoTelas(cargoId, ids, vinculo);
  }

  public void vincularPermissoes(Long cargoId, List<Long> permissaoIds, Boolean vinculo) {
    if (cargoId == null) throw new IllegalArgumentException("ID do descricao é obrigatório.");

    if (!cargoRepository.existsById(cargoId)) {
      throw new NotFoundException("Cargo não cadastrado para o id: " + cargoId);
    }

    if (permissaoIds == null || permissaoIds.isEmpty()) {
      return;
    }

    List<Long> ids = permissaoIds.stream().filter(Objects::nonNull).distinct().toList();
    if (ids.isEmpty()) return;

    cargoRepository.vincularPermissoesCargo(cargoId, ids, vinculo);
  }
}
