package saas.hotel.istoepousada.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import saas.hotel.istoepousada.dto.HistoricoRecebidosFuncionario;
import saas.hotel.istoepousada.repository.HistoricoRecebidosFuncionarioRepository;

@Service
public class HistoricoRecebidosFuncionarioService {

    private static final Path UPLOAD_DIR =
            Path.of("storage", "comprovantes", "recebidos-funcionario");

    private final HistoricoRecebidosFuncionarioRepository historicoRecebidosFuncionarioRepository;

    public HistoricoRecebidosFuncionarioService(
            HistoricoRecebidosFuncionarioRepository historicoRecebidosFuncionarioRepository) {
        this.historicoRecebidosFuncionarioRepository = historicoRecebidosFuncionarioRepository;
    }

    public List<HistoricoRecebidosFuncionario> buscar(Long historicoFuncionarioId) {
        return historicoRecebidosFuncionarioRepository.buscar(historicoFuncionarioId);
    }

    @Transactional
    public HistoricoRecebidosFuncionario inserir(
            HistoricoRecebidosFuncionario recebido, MultipartFile arquivo) throws IOException {

        String pathArquivo = salvarArquivo(arquivo);

        HistoricoRecebidosFuncionario entity =
                new HistoricoRecebidosFuncionario(
                        recebido.historicoFuncionario(),
                        recebido.valorRecebido(),
                        recebido.dataHoraInicio(),
                        recebido.dataHoraFim(),
                        recebido.dataHoraPagamento(),
                        recebido.tipoPagamento(),
                        recebido.descricao(),
                        pathArquivo);

        return historicoRecebidosFuncionarioRepository.insert(entity);
    }

    @Transactional
    public HistoricoRecebidosFuncionario atualizar(
            HistoricoRecebidosFuncionario recebido, MultipartFile arquivo) throws IOException {

        String pathArquivo = salvarArquivo(arquivo);

        HistoricoRecebidosFuncionario entity =
                new HistoricoRecebidosFuncionario(
                        recebido.id(),
                        recebido.historicoFuncionario(),
                        recebido.valorRecebido(),
                        recebido.dataHoraInicio(),
                        recebido.dataHoraFim(),
                        recebido.dataHoraPagamento(),
                        recebido.tipoPagamento(),
                        recebido.descricao(),
                        pathArquivo);

        return historicoRecebidosFuncionarioRepository.update(entity);
    }

    public String salvarArquivo(MultipartFile arquivo) throws IOException {
        if (arquivo == null || arquivo.isEmpty()) return null;

        Files.createDirectories(UPLOAD_DIR);

        String original = arquivo.getOriginalFilename();
        String ext = "";
        if (original != null) {
            int dot = original.lastIndexOf('.');
            if (dot >= 0 && dot < original.length() - 1) ext = original.substring(dot);
        }

        String nomeGerado = UUID.randomUUID() + ext;
        Path destino = UPLOAD_DIR.resolve(nomeGerado).normalize();

        Files.copy(arquivo.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);
        return destino.toString();
    }
}
