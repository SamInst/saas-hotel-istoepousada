package saas.hotel.istoepousada.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import saas.hotel.istoepousada.websocket.CalendarWebSocketHandler;
import saas.hotel.istoepousada.websocket.JwtHandshakeInterceptor;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

  private final CalendarWebSocketHandler calendarHandler;
  private final JwtHandshakeInterceptor jwtHandshakeInterceptor;

  public WebSocketConfig(
      CalendarWebSocketHandler calendarHandler, JwtHandshakeInterceptor jwtHandshakeInterceptor) {
    this.calendarHandler = calendarHandler;
    this.jwtHandshakeInterceptor = jwtHandshakeInterceptor;
  }

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry
        .addHandler(calendarHandler, "/ws/calendar")
        .addInterceptors(jwtHandshakeInterceptor)
        .setAllowedOriginPatterns("*");
  }
}
