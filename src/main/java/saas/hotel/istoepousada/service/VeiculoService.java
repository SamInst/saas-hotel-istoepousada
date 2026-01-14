// package saas.hotel.istoepousada.service;
//
// import java.util.List;
// import org.springframework.stereotype.Service;
// import org.springframework.transaction.annotation.Transactional;
// import saas.hotel.istoepousada.dto.Veiculo;
// import saas.hotel.istoepousada.handler.exceptions.NotFoundException;
// import saas.hotel.istoepousada.repository.VeiculoRepository;
//
// @Service
// public class VeiculoService {
//  private final VeiculoRepository veiculoRepository;
//
//  public VeiculoService(VeiculoRepository veiculoRepository) {
//    this.veiculoRepository = veiculoRepository;
//  }
//
//  @Transactional(readOnly = true)
//  public List<Veiculo> listarPorPessoa(Long pessoaId) {
//    return veiculoRepository.findAllByPessoaId(pessoaId);
//  }
//
//  @Transactional
//  public Veiculo salvar(Long pessoa_id, Veiculo veiculo) {
//    if (veiculo.id() != null) {
//      veiculoRepository
//          .findById(veiculo.id())
//          .orElseThrow(() -> new NotFoundException("Veículo não encontrado: id=" + veiculo.id()));
//    }
//    return veiculoRepository.save(pessoa_id, veiculo);
//  }
//
//  @Transactional
//  public void vincularPessoa(Long pessoaId, Long veiculoId) {
//    veiculoRepository
//        .findById(veiculoId)
//        .orElseThrow(() -> new NotFoundException("Veículo não encontrado: id=" + veiculoId));
//
//    veiculoRepository.vincularPessoa(pessoaId, veiculoId);
//  }
//
//  @Transactional
//  public void setVinculoAtivo(Long pessoaId, Long veiculoId, boolean ativo) {
//    veiculoRepository
//        .findById(veiculoId)
//        .orElseThrow(() -> new NotFoundException("Veículo não encontrado: id=" + veiculoId));
//
//    veiculoRepository.setVinculoAtivo(pessoaId, veiculoId, ativo);
//  }
// }
