package saas.hotel.istoepousada.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import saas.hotel.istoepousada.repository.ArquivoRepository;

@Service
public class ArquivoService {
  private final ArquivoRepository arquivoRepository;

  public ArquivoService(ArquivoRepository arquivoRepository) {
    this.arquivoRepository = arquivoRepository;
  }

  public String salvarComprovante(MultipartFile file) throws IOException {
    return arquivoRepository.salvarComprovante(file);
  }

  public String salvarFotoPerfil(MultipartFile file) throws IOException {
    return arquivoRepository.salvarFotoPerfil(file);
  }

  public String buscaPathArquivoByPagamentoUUID(UUID uuid) {
    return arquivoRepository.buscaPathArquivoByPagamentoUUID(uuid);
  }

  public void deletarArquivo(String path) throws IOException {
    arquivoRepository.deletarArquivo(path);
  }

  public void setPath(String path, UUID uuid) {
    arquivoRepository.setPathArquivoPagamento(path, uuid);
  }

  public Resource buscarArquivoResource(String caminhoCompleto) throws IOException {
    Path basePath = Paths.get(ArquivoRepository.BASE_PATH).toAbsolutePath().normalize();
    Path path = Paths.get(caminhoCompleto).toAbsolutePath().normalize();

    if (!path.startsWith(basePath)) {
      throw new SecurityException("Acesso negado.");
    }

    if (!Files.exists(path)) {
      throw new RuntimeException("Arquivo não encontrado.");
    }

    return new UrlResource(path.toUri());
  }
}
