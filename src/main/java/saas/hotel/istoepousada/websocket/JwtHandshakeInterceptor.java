package saas.hotel.istoepousada.websocket;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import saas.hotel.istoepousada.security.JwtUtil;

/**
 * Valida o JWT no handshake do WebSocket. Como o cliente WebSocket do navegador não consegue enviar
 * cabeçalhos personalizados, o token chega como query param: {@code /ws/calendar?token=<jwt>}.
 */
@Slf4j
@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

  private final JwtUtil jwtUtil;

  public JwtHandshakeInterceptor(JwtUtil jwtUtil) {
    this.jwtUtil = jwtUtil;
  }

  @Override
  public boolean beforeHandshake(
      ServerHttpRequest request,
      ServerHttpResponse response,
      WebSocketHandler wsHandler,
      Map<String, Object> attributes) {

    String token = extractToken(request);
    if (token == null || !jwtUtil.validateToken(token)) {
      response.setStatusCode(HttpStatus.UNAUTHORIZED);
      log.debug("Handshake WS rejeitado: token ausente ou inválido");
      return false;
    }
    return true;
  }

  @Override
  public void afterHandshake(
      ServerHttpRequest request,
      ServerHttpResponse response,
      WebSocketHandler wsHandler,
      Exception exception) {
    // nada a fazer
  }

  private String extractToken(ServerHttpRequest request) {
    String query = request.getURI().getQuery();
    if (query == null || query.isBlank()) return null;
    for (String pair : query.split("&")) {
      if (pair.startsWith("token=")) {
        return URLDecoder.decode(pair.substring("token=".length()), StandardCharsets.UTF_8);
      }
    }
    return null;
  }
}
