package saas.hotel.istoepousada.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import saas.hotel.istoepousada.dto.LoginResponse;

import java.util.Map;

@Component
public class PermissionInterceptor implements HandlerInterceptor {
    private final ObjectMapper objectMapper;

    public PermissionInterceptor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        if (!(handler instanceof HandlerMethod handlerMethod))
            return true;

        RequireTela requireTela = handlerMethod.getMethodAnnotation(RequireTela.class);

        if (requireTela == null)
            requireTela = handlerMethod.getBeanType().getAnnotation(RequireTela.class);

        if (requireTela == null)
            return true;

        LoginResponse.FuncionarioAuth funcionario =
                (LoginResponse.FuncionarioAuth) request.getAttribute("funcionario");

        if (funcionario == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            Map<String, String> error = Map.of(
                    "error", "Unauthorized",
                    "message", "Usuário não autenticado"
            );
            response.getWriter().write(objectMapper.writeValueAsString(error));
            return false;
        }

        String telaRequerida = requireTela.value();
        boolean temPermissao = funcionario.cargo().telas().stream()
                .anyMatch(tela -> tela.nome().equals(telaRequerida));

        if (!temPermissao) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            Map<String, String> error = Map.of(
                    "error", "Forbidden",
                    "message", "Você não tem permissão para acessar esta tela: " + telaRequerida
            );
            response.getWriter().write(objectMapper.writeValueAsString(error));
            return false;
        }
        return true;
    }
}

