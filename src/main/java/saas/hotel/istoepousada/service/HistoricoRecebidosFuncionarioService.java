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
import saas.hotel.istoepousada.dto.Funcionario;
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

  public List<Funcionario.Historico.Recebido> buscar(Long historicoFuncionarioId) {
    if (historicoFuncionarioId == null) {
      throw new IllegalArgumentException("ID do histórico do funcionário é obrigatório.");
    }
    return historicoRecebidosFuncionarioRepository.buscar(historicoFuncionarioId);
  }

  @Transactional(rollbackFor = Exception.class)
  public Funcionario.Historico.Recebido inserir(Funcionario.Historico.Recebido.Request recebido)
      throws IOException {

    if (recebido.historico().id() == null)
      throw new IllegalArgumentException("ID do histórico do funcionário é obrigatório.");

    validarInsert(recebido);

    String pathArquivo = salvarArquivo(recebido.pagamento().arquivo());

    Funcionario.Historico.Recebido.Request requestComArquivo =
        new Funcionario.Historico.Recebido.Request(
            recebido.funcionario(),
            recebido.historico(),
            recebido.data_hora_inicio(),
            recebido.data_hora_fim(),
            recebido.data_hora_pagamento(),
            recebido.pagamento(),
            pathArquivo);

    return historicoRecebidosFuncionarioRepository.insert(recebido);
  }

  @Transactional(rollbackFor = Exception.class)
  public Funcionario.Historico.Recebido atualizar(Funcionario.Historico.Recebido.Update recebido)
      throws IOException {
    if (recebido == null) throw new IllegalArgumentException("Dados do recebido são obrigatórios.");

    if (recebido.id() == null) throw new IllegalArgumentException("ID do recebido é obrigatório.");

    if (recebido.data_hora_inicio() == null)
      throw new IllegalArgumentException("Data/hora de início é obrigatória.");

    if (recebido.data_hora_pagamento() == null)
      throw new IllegalArgumentException("Data/hora de pagamento é obrigatória.");

    salvarArquivo(recebido.arquivo());

    return historicoRecebidosFuncionarioRepository.update(recebido);
  }

  public String salvarArquivo(MultipartFile arquivo) throws IOException {
    if (arquivo == null || arquivo.isEmpty()) {
      return null;
    }

    Files.createDirectories(UPLOAD_DIR);

    String original = arquivo.getOriginalFilename();
    String ext = "";
    if (original != null) {
      int dot = original.lastIndexOf('.');
      if (dot >= 0 && dot < original.length() - 1) {
        ext = original.substring(dot);
      }
    }

    String nomeGerado = UUID.randomUUID() + ext;
    Path destino = UPLOAD_DIR.resolve(nomeGerado).normalize();

    Files.copy(arquivo.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);
    return destino.toString();
  }

  private void validarInsert(Funcionario.Historico.Recebido.Request request) {
    if (request == null) {
      throw new IllegalArgumentException("Dados do recebido são obrigatórios.");
    }
    if (request.funcionario() == null || request.funcionario().id() == null) {
      throw new IllegalArgumentException("Funcionário é obrigatório.");
    }
    if (request.data_hora_inicio() == null) {
      throw new IllegalArgumentException("Data/hora de início é obrigatória.");
    }
    if (request.data_hora_pagamento() == null) {
      throw new IllegalArgumentException("Data/hora de pagamento é obrigatória.");
    }
    if (request.pagamento() == null) {
      throw new IllegalArgumentException("Pagamento é obrigatório.");
    }
    if (request.pagamento().tipo_pagamento() == null
        || request.pagamento().tipo_pagamento().id() == null) {
      throw new IllegalArgumentException("Tipo de pagamento é obrigatório.");
    }
    if (request.pagamento().funcionario() == null
        || request.pagamento().funcionario().id() == null) {
      throw new IllegalArgumentException("Funcionário do pagamento é obrigatório.");
    }
    if (request.pagamento().nome_pagador() == null
        || request.pagamento().nome_pagador().isBlank()) {
      throw new IllegalArgumentException("Nome do pagador é obrigatório.");
    }
    if (request.pagamento().valor() == null) {
      throw new IllegalArgumentException("Valor do pagamento é obrigatório.");
    }
  }
}
