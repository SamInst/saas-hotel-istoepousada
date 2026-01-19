package saas.hotel.istoepousada.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    public JwtInterceptor(JwtUtil jwtUtil, ObjectMapper objectMapper) {
        this.jwtUtil = jwtUtil;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        // Permitir OPTIONS (CORS preflight)
        if ("OPTIONS".equals(request.getMethod())) {
            return true;
        }

        // Extrair token do header Authorization
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            Map<String, String> error = Map.of(
                    "error", "Unauthorized",
                    "message", "Token JWT não fornecido ou inválido"
            );

            response.getWriter().write(objectMapper.writeValueAsString(error));
            return false;
        }

        String token = authHeader.substring(7);

        // Validar token
        if (!jwtUtil.validateToken(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            Map<String, String> error = Map.of(
                    "error", "Unauthorized",
                    "message", "Token JWT inválido ou expirado"
            );

            response.getWriter().write(objectMapper.writeValueAsString(error));
            return false;
        }

        // Adicionar dados do funcionário no request para uso posterior
        var funcionario = jwtUtil.getFuncionarioFromToken(token);
        request.setAttribute("funcionario", funcionario);
        request.setAttribute("funcionarioId", funcionario.id());
        request.setAttribute("usuarioId", funcionario.usuarioId());
        request.setAttribute("pessoaId", funcionario.pessoaId());

        return true;
    }
}
