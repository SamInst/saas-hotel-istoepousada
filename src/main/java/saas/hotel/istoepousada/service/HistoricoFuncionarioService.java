package saas.hotel.istoepousada.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import saas.hotel.istoepousada.dto.HistoricoFuncionario;
import saas.hotel.istoepousada.repository.HistoricoFuncionarioRepository;

@Service
public class HistoricoFuncionarioService {

    private final HistoricoFuncionarioRepository historicoFuncionarioRepository;

    public HistoricoFuncionarioService(HistoricoFuncionarioRepository historicoFuncionarioRepository) {
        this.historicoFuncionarioRepository = historicoFuncionarioRepository;
    }

    public List<HistoricoFuncionario> listarPorFuncionario(Long funcionarioId) {
        return historicoFuncionarioRepository.listarPorFuncionario(funcionarioId);
    }

    public HistoricoFuncionario buscarPorId(Long id) {
        return historicoFuncionarioRepository.findById(id);
    }

    @Transactional
    public HistoricoFuncionario salvar(HistoricoFuncionario historico) {
        return historicoFuncionarioRepository.save(historico);
    }
}
