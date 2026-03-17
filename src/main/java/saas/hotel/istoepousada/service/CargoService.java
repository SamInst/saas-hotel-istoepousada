package saas.hotel.istoepousada.service;

import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import saas.hotel.istoepousada.dto.Cargo;
import saas.hotel.istoepousada.dto.Permissao;
import saas.hotel.istoepousada.dto.Tela;
import saas.hotel.istoepousada.handler.exceptions.NotFoundException;
import saas.hotel.istoepousada.repository.CargoRepository;

@Service
public class CargoService {

  private final CargoRepository cargoRepository;

  public CargoService(CargoRepository cargoRepository) {
    this.cargoRepository = cargoRepository;
  }

  public Page<Cargo> listar(Cargo.Buscar buscar) {
    return cargoRepository.buscarCargoPorIdOuNome(buscar);
  }

  public Cargo criar(Cargo.Request request) {
    if (request.descricao() == null || request.descricao().trim().isEmpty()) {
      throw new IllegalArgumentException("Nome do descricao é obrigatório.");
    }

    return cargoRepository.insert(request);
  }

  public Cargo atualizar(Cargo.Update cargo) {
    if (cargo.id() == null) throw new IllegalArgumentException("ID do descricao é obrigatório.");

    if (!cargoRepository.existsById(new Cargo.Id(cargo.id())))
      throw new NotFoundException("Cargo não cadastrado para o uuid: " + cargo.id());

    if (cargo.descricao() == null || cargo.descricao().trim().isEmpty())
      throw new IllegalArgumentException("Nome do descricao é obrigatório.");

    return cargoRepository.update(cargo);
  }

  public void deletar(Cargo.Id cargo) {
    if (cargo == null) throw new IllegalArgumentException("ID do descricao é obrigatório.");

    if (!cargoRepository.existsById(cargo))
      throw new NotFoundException("Cargo não cadastrado para o uuid: " + cargo.id());

    cargoRepository.deleteById(cargo);
  }

  public void vincularTelas(Cargo.Vincular vinculo) {
    if (vinculo.cargo().id() == null)
      throw new IllegalArgumentException("ID do descricao é obrigatório.");

    if (!cargoRepository.existsById(vinculo.cargo()))
      throw new NotFoundException("Cargo não cadastrado para o id: " + vinculo.cargo().id());

    if (vinculo.telas() == null || vinculo.telas().isEmpty()) return;

    List<Tela.Id> ids = vinculo.telas().stream().filter(Objects::nonNull).distinct().toList();

    if (ids.isEmpty()) return;

    vinculo.telas().clear();
    vinculo.telas().addAll(ids);

    cargoRepository.vincularCargoTelas(vinculo);
  }

  public void vincularPermissoes(Permissao.Vincular vinculo) {
    if (vinculo.cargo().id() == null)
      throw new IllegalArgumentException("ID do descricao é obrigatório.");

    if (!cargoRepository.existsById(vinculo.cargo()))
      throw new NotFoundException("Cargo não cadastrado para o id: " + vinculo.cargo().id());

    if (vinculo.permissoes() == null || vinculo.permissoes().isEmpty()) return;

    List<Permissao.Id> ids =
        vinculo.permissoes().stream().filter(Objects::nonNull).distinct().toList();

    if (ids.isEmpty()) return;

    vinculo.permissoes().clear();
    vinculo.permissoes().addAll(ids);

    cargoRepository.vincularPermissoesCargo(vinculo);
  }
}
