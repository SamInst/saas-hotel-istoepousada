package saas.hotel.istoepousada.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import saas.hotel.istoepousada.dto.Funcionario;
import saas.hotel.istoepousada.repository.HistoricoFuncionarioRepository;

@Service
public class HistoricoFuncionarioService {

  private final HistoricoFuncionarioRepository historicoFuncionarioRepository;

  public HistoricoFuncionarioService(
      HistoricoFuncionarioRepository historicoFuncionarioRepository) {
    this.historicoFuncionarioRepository = historicoFuncionarioRepository;
  }

  public List<Funcionario.Historico> listarPorFuncionario(Long funcionarioId) {
    return historicoFuncionarioRepository.listarPorFuncionario(funcionarioId);
  }

  public Funcionario.Historico buscarPorId(Long id) {
    return historicoFuncionarioRepository.findById(id);
  }

  @Transactional
  public Funcionario.Historico insert(Funcionario.Historico.Request historico) {
    return historicoFuncionarioRepository.insert(historico);
  }

  @Transactional
  public Funcionario.Historico update(Funcionario.Historico.Update historico) {
    return historicoFuncionarioRepository.update(historico);
  }
}
