package saas.hotel.istoepousada.websocket;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Após qualquer requisição mutante (POST/PUT/PATCH/DELETE) bem-sucedida em endpoints que afetam o
 * calendário, dispara um broadcast genérico via WebSocket. Os clientes conectados recarregam as
 * reservas. Mantém a lógica de broadcast em um único ponto, sem tocar em controllers/serviços.
 */
@Component
public class CalendarBroadcastInterceptor implements HandlerInterceptor {

  private static final Set<String> MUTATING = Set.of("POST", "PUT", "PATCH", "DELETE");

  private final CalendarWebSocketHandler socket;

  public CalendarBroadcastInterceptor(CalendarWebSocketHandler socket) {
    this.socket = socket;
  }

  @Override
  public void afterCompletion(
      HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
    if (ex != null) return;
    if (!MUTATING.contains(request.getMethod())) return;
    int status = response.getStatus();
    if (status < 200 || status >= 300) return;

    socket.broadcast(
        "{\"type\":\"calendar.changed\",\"path\":\""
            + request.getRequestURI()
            + "\",\"ts\":"
            + System.currentTimeMillis()
            + "}");
  }
}
