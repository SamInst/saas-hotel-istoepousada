package saas.hotel.istoepousada.repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.web.multipart.MultipartFile;

@Repository
public class ArquivoRepository {
  private final JdbcTemplate jdbcTemplate;

  private static final String SISTEMA = System.getProperty("os.name").toLowerCase();

  public static final String BASE_PATH;

  static {
    String userHome = System.getProperty("user.home");

    if (SISTEMA.contains("win")) {
      BASE_PATH = Paths.get(userHome, "IstoEPousada").toString();
    } else if (SISTEMA.contains("mac")) {
      BASE_PATH = Paths.get(userHome, "IstoEPousada").toString();
    } else {
      BASE_PATH = Paths.get(userHome, "IstoEPousada").toString();
    }
  }

  public static final String CAMINHO_COMPROVANTES = Paths.get(BASE_PATH, "Comprovantes").toString();

  public static final String CAMINHO_FOTOS = Paths.get(BASE_PATH, "FotoPerfil").toString();

  public ArquivoRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public String salvarComprovante(MultipartFile file) throws IOException {
    return salvarArquivo(file, CAMINHO_COMPROVANTES);
  }

  public String salvarFotoPerfil(MultipartFile file) throws IOException {
    return salvarArquivo(file, CAMINHO_FOTOS);
  }

  private String salvarArquivo(MultipartFile file, String caminho) throws IOException {
    if (file == null || file.isEmpty()) {
      throw new IllegalArgumentException("Arquivo inválido.");
    }

    Path diretorio = Paths.get(caminho);

    if (!Files.exists(diretorio)) {
      Files.createDirectories(diretorio);
    }

    String nomeArquivo = gerarNomeSeguro(file.getOriginalFilename());
    Path caminhoCompleto = diretorio.resolve(nomeArquivo);

    Files.write(caminhoCompleto, file.getBytes());

    return caminhoCompleto.toString();
  }

  private String gerarNomeSeguro(String nomeOriginal) {
    return System.currentTimeMillis() + "_" + nomeOriginal;
  }

  public void deletarArquivo(String caminhoCompleto) throws IOException {
    if (caminhoCompleto == null || caminhoCompleto.isBlank())
      throw new IllegalArgumentException("Caminho inválido.");

    Path basePath = Paths.get(BASE_PATH).toAbsolutePath().normalize();
    Path path = Paths.get(caminhoCompleto).toAbsolutePath().normalize();

    if (!path.startsWith(basePath))
      throw new SecurityException("Acesso negado para deletar este arquivo.");

    if (!Files.exists(path)) throw new RuntimeException("Arquivo não encontrado.");

    if (!Files.isWritable(path)) throw new RuntimeException("Arquivo não pode ser deletado.");

    Files.delete(path);
  }

  public String buscaPathArquivoByPagamentoUUID(UUID uuid) {
    return jdbcTemplate.queryForObject(
        """
                SELECT pagamento.path_arquivo
                FROM pagamento
                WHERE pagamento.id = ?
                """,
        String.class,
        uuid);
  }

  public void setPathArquivoPagamento(String path, UUID uuid) {
    jdbcTemplate.update(
        """
            update pagamento
            set path_arquivo = ?
            where id = ?
            """,
        path,
        uuid);
  }
}
