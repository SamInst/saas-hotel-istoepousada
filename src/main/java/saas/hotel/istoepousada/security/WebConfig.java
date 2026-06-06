package saas.hotel.istoepousada.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import saas.hotel.istoepousada.websocket.CalendarBroadcastInterceptor;

@Configuration
public class WebConfig implements WebMvcConfigurer {
  private final JwtInterceptor jwtInterceptor;
  private final PermissionInterceptor permissionInterceptor;
  private final CalendarBroadcastInterceptor calendarBroadcastInterceptor;

  public WebConfig(
      JwtInterceptor jwtInterceptor,
      PermissionInterceptor permissionInterceptor,
      CalendarBroadcastInterceptor calendarBroadcastInterceptor) {
    this.jwtInterceptor = jwtInterceptor;
    this.permissionInterceptor = permissionInterceptor;
    this.calendarBroadcastInterceptor = calendarBroadcastInterceptor;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry
        .addInterceptor(jwtInterceptor)
        .addPathPatterns("/**")
        .excludePathPatterns(
            "/auth/**",
            "/ws/**",
            "/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/error");
    registry
        .addInterceptor(permissionInterceptor)
        .addPathPatterns("/**")
        .excludePathPatterns(
            "/auth/**",
            "/ws/**",
            "/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/error");
    // Broadcast de mudanças do calendário via WebSocket após mutações bem-sucedidas
    registry
        .addInterceptor(calendarBroadcastInterceptor)
        .addPathPatterns("/reserva/**", "/hospedagem/**", "/dayuse/**", "/orcamento/**");
  }

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry
        .addMapping("/**")
        .allowedOrigins("*")
        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        .allowedHeaders("*")
        .maxAge(3600);
  }
}
