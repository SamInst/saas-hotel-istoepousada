package saas.hotel.istoepousada.websocket;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * Handler de WebSocket cru para o calendário de reservas. Mantém um registro de sessões abertas e
 * faz broadcast de eventos de mudança (ex.: reserva criada/editada/cancelada) para todos os
 * clientes conectados, que então recarregam o calendário.
 */
@Slf4j
@Component
public class CalendarWebSocketHandler extends TextWebSocketHandler {

  // session.getId() -> sessão (decorada para envio thread-safe a partir de múltiplas threads HTTP)
  private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

  @Override
  public void afterConnectionEstablished(WebSocketSession session) {
    // ConcurrentWebSocketSessionDecorator serializa os envios e protege contra escritas
    // concorrentes
    sessions.put(
        session.getId(), new ConcurrentWebSocketSessionDecorator(session, 5_000, 64 * 1024));
    log.debug("WS calendário conectado: {} (total {})", session.getId(), sessions.size());
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
    sessions.remove(session.getId());
    log.debug("WS calendário desconectado: {} (total {})", session.getId(), sessions.size());
  }

  /** Envia o payload JSON para todos os clientes conectados. */
  public void broadcast(String payload) {
    if (sessions.isEmpty()) return;
    TextMessage message = new TextMessage(payload);
    for (WebSocketSession session : sessions.values()) {
      try {
        if (session.isOpen()) session.sendMessage(message);
      } catch (IOException e) {
        log.warn("Falha ao enviar mensagem WS para {}: {}", session.getId(), e.getMessage());
      }
    }
  }
}
