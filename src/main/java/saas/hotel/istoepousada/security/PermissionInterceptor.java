package saas.hotel.istoepousada.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import saas.hotel.istoepousada.dto.FuncionarioAuth;

@Component
public class PermissionInterceptor implements HandlerInterceptor {
  private static final Logger log = LogManager.getLogger(PermissionInterceptor.class);
  private final ObjectMapper objectMapper;

  public PermissionInterceptor(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
          throws Exception {

    if (!(handler instanceof HandlerMethod handlerMethod)) return true;

    RequireTela requireTela = handlerMethod.getMethodAnnotation(RequireTela.class);
    if (requireTela == null) requireTela = handlerMethod.getBeanType().getAnnotation(RequireTela.class);

    RequirePermissao requirePermissao = handlerMethod.getMethodAnnotation(RequirePermissao.class);
    if (requirePermissao == null) requirePermissao = handlerMethod.getBeanType().getAnnotation(RequirePermissao.class);

    if (requireTela == null && requirePermissao == null) return true;

    FuncionarioAuth funcionario = (FuncionarioAuth) request.getAttribute("funcionario");
    log.info("FuncionarioAuth no request: {}", funcionario);

    if (funcionario == null) {
      writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized", "Usuário não autenticado");
      return false;
    }

    // ====== ADMIN BYPASS (robusto) ======
    String cargoNome = funcionario.cargo() != null ? funcionario.cargo().nome() : null;
    if ("ADMINISTRADOR".equalsIgnoreCase(safe(cargoNome).trim())) {
      log.info("Usuário ADMINISTRADOR liberado para acesso total");
      return true;
    }

    List<FuncionarioAuth.CargoAuth.TelaAuth> telas =
            (funcionario.cargo() == null || funcionario.cargo().telas() == null)
                    ? List.of()
                    : funcionario.cargo().telas();

    FuncionarioAuth.CargoAuth.TelaAuth telaContexto = null;

    if (requireTela != null) {
      String telaRequerida = safe(requireTela.value()).trim();

      telaContexto =
              telas.stream()
                      .filter(t -> t != null && safe(t.nome()).equalsIgnoreCase(telaRequerida))
                      .findFirst()
                      .orElse(null);

      if (telaContexto == null) {
        writeJson(
                response,
                HttpServletResponse.SC_FORBIDDEN,
                "Forbidden",
                "Você não tem permissão para acessar essa tela: " + telaRequerida);
        return false;
      }
    }

    if (requirePermissao != null) {
      Set<String> required = normalizeRequired(requirePermissao);
      if (!required.isEmpty()) {
        Set<String> granted = new HashSet<>();

        if (telaContexto != null) granted.addAll(extractPermCodes(telaContexto));
        else for (var t : telas) granted.addAll(extractPermCodes(t));

        boolean ok = granted.containsAll(required);

        if (!ok) {
          writeJson(
                  response,
                  HttpServletResponse.SC_FORBIDDEN,
                  "Forbidden",
                  "Você não tem as permissões necessárias: " + required);
          return false;
        }
      }
    }

    return true;
  }

  private Set<String> normalizeRequired(RequirePermissao requirePermissao) {
    String[] raw = requirePermissao.value();
    Set<String> out = new HashSet<>();
    if (raw == null) return out;
    for (String s : raw) {
      if (s == null) continue;
      String n = s.trim().toUpperCase();
      if (!n.isBlank()) out.add(n);
    }
    return out;
  }

  private Set<String> extractPermCodes(FuncionarioAuth.CargoAuth.TelaAuth tela) {
    if (tela == null || tela.permissoes() == null) return Set.of();

    Set<String> perms = new HashSet<>();
    for (FuncionarioAuth.CargoAuth.PermissaoAuth p : tela.permissoes()) {
      if (p == null || p.permissao() == null) continue;
      String code = p.permissao().trim().toUpperCase();
      if (!code.isBlank()) perms.add(code);
    }
    return perms;
  }

  private String safe(String s) {
    return s == null ? "" : s;
  }

  private void writeJson(HttpServletResponse response, int status, String error, String message)
          throws Exception {
    response.setStatus(status);
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    response.getWriter().write(objectMapper.writeValueAsString(Map.of("error", error, "message", message)));
  }
}
