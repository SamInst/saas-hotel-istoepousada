package saas.hotel.istoepousada.controller;

import java.io.IOException;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import saas.hotel.istoepousada.service.ArquivoService;

@RestController
@RequestMapping("/arquivo")
public class ArquivoController {
  private final ArquivoService arquivoService;

  public ArquivoController(ArquivoService arquivoService) {
    this.arquivoService = arquivoService;
  }

  @GetMapping("/download")
  public ResponseEntity<Resource> download(@RequestParam String path) throws IOException {
    Resource resource = arquivoService.buscarArquivoResource(path);
    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"" + resource.getFilename() + "\"")
        .body(resource);
  }
}
