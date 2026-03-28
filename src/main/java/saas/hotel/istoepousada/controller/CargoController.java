package saas.hotel.istoepousada.controller;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import saas.hotel.istoepousada.dto.Cargo;
import saas.hotel.istoepousada.dto.Objeto;
import saas.hotel.istoepousada.dto.Permissao;
import saas.hotel.istoepousada.dto.Tela;
import saas.hotel.istoepousada.repository.ObjectRepository;
import saas.hotel.istoepousada.security.AcessoLiberado;
import saas.hotel.istoepousada.security.RequireTela;
import saas.hotel.istoepousada.service.CargoService;

@RestController
@RequestMapping("/cargo")
@RequireTela("ADMIN")
public class CargoController {

  private final CargoService cargoService;
  private final ObjectRepository objectRepository;

  public CargoController(CargoService cargoService, ObjectRepository objectRepository) {
    this.cargoService = cargoService;
    this.objectRepository = objectRepository;
  }

  @GetMapping
  @AcessoLiberado({"ITENS"})
  public Page<Cargo> listar(@ModelAttribute Cargo.Buscar buscar) {
    return cargoService.listar(buscar);
  }

  @PostMapping()
  @ResponseStatus(HttpStatus.CREATED)
  public Cargo criar(@RequestBody Cargo.Request cargo) {
    return cargoService.criar(cargo);
  }

  @PutMapping()
  public Cargo atualizar(@RequestBody Cargo.Update cargo) {
    return cargoService.atualizar(cargo);
  }

  @DeleteMapping()
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deletar(@RequestBody Cargo.Id cargo) {
    cargoService.deletar(cargo);
  }

  @PatchMapping("/vincular/telas")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void vincularTelas(@RequestBody Cargo.Vincular vincular) {
    cargoService.vincularTelas(vincular);
  }

  @PatchMapping("/vincular/permissoes")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void vincularPermissoes(@RequestBody Permissao.Vincular vinculo) {
    cargoService.vincularPermissoes(vinculo);
  }

  @GetMapping("/telas")
  public List<Tela> telas() {
    return objectRepository.telasComPermissoes();
  }

  @GetMapping("/permissoes")
  public List<Objeto> permissoes(@RequestParam Long telaId) {
    return objectRepository.permissoes(telaId);
  }
}
