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
    if (id == null) throw new IllegalArgumentException("ID do cargo é obrigatório.");
    return cargoRepository.findByIdOrThrow(id);
  }

  public Cargo criar(Cargo.Request request) {
    if (request.descricao() == null || request.descricao().trim().isEmpty()) {
      throw new IllegalArgumentException("Nome do cargo é obrigatório.");
    }

    // Garante que o ID é null para inserção
    if (request.id() != null) {
      throw new IllegalArgumentException("ID deve ser null ao criar um novo cargo.");
    }

    return cargoRepository.insert(request);
  }

  public Cargo atualizar(Long id, Cargo.Request request) {
    if (id == null) throw new IllegalArgumentException("ID do cargo é obrigatório.");

    if (!cargoRepository.existsById(id)) {
      throw new NotFoundException("Cargo não cadastrado para o id: " + id);
    }

    if (request.descricao() == null || request.descricao().trim().isEmpty()) {
      throw new IllegalArgumentException("Nome do cargo é obrigatório.");
    }

    // Cria um novo request com o ID correto
    Cargo.Request requestComId =
        new Cargo.Request(id, request.descricao(), request.telasIds(), request.permissoesIds());

    return cargoRepository.update(requestComId);
  }

  public void deletar(Long id) {
    if (id == null) throw new IllegalArgumentException("ID do cargo é obrigatório.");

    if (!cargoRepository.existsById(id)) {
      throw new NotFoundException("Cargo não cadastrado para o id: " + id);
    }

    cargoRepository.deleteById(id);
  }

  public void vincularTelas(Long cargoId, List<Long> telaIds, Boolean vinculo) {
    if (cargoId == null) throw new IllegalArgumentException("ID do cargo é obrigatório.");

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
    if (cargoId == null) throw new IllegalArgumentException("ID do cargo é obrigatório.");

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
