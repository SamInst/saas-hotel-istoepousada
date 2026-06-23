package saas.hotel.istoepousada.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Rate limiting por IP para os endpoints públicos de auto-cadastro. Usa um token-bucket em memória
 * (Caffeine) — suficiente para conter enumeração de CPF e spam de cadastro em uma instância única.
 *
 * <p>OBS: o estado é por instância. Em ambiente com múltiplos dynos/réplicas seria necessário um
 * store compartilhado (ex.: Redis). Atrás de proxy (Heroku) o IP é lido de {@code X-Forwarded-For}.
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

  private final ObjectMapper objectMapper;

  /** Limite de cadastros (POST) por IP por minuto. */
  @Value("${ratelimit.auto-cadastro.per-minute:5}")
  private int autoCadastroPerMinute;

  /** Limite de consultas de CPF (GET) por IP por minuto. */
  @Value("${ratelimit.cpf-lookup.per-minute:20}")
  private int cpfLookupPerMinute;

  private final Cache<String, Bucket> buckets =
      Caffeine.newBuilder()
          .expireAfterAccess(Duration.ofMinutes(15))
          .maximumSize(100_000)
          .build();

  public RateLimitInterceptor(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    if ("OPTIONS".equals(request.getMethod())) return true;

    String path = request.getRequestURI();
    String rule;
    int perMinute;
    if (path.startsWith("/pessoa/auto-cadastro")) {
      rule = "auto-cadastro";
      perMinute = Math.max(1, autoCadastroPerMinute);
    } else if (path.startsWith("/pessoa/cpf")) {
      rule = "cpf-lookup";
      perMinute = Math.max(1, cpfLookupPerMinute);
    } else {
      return true; // caminho não limitado
    }

    String key = rule + "|" + clientIp(request);
    Bucket bucket = buckets.get(key, k -> new Bucket(perMinute));

    if (bucket.tryConsume()) {
      return true;
    }

    long retryAfter = bucket.retryAfterSeconds();
    response.setStatus(429); // Too Many Requests
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    response.setHeader("Retry-After", String.valueOf(retryAfter));
    response
        .getWriter()
        .write(
            objectMapper.writeValueAsString(
                Map.of(
                    "error",
                    "Too Many Requests",
                    "message",
                    "Muitas requisições. Aguarde alguns instantes e tente novamente.")));
    return false;
  }

  private String clientIp(HttpServletRequest request) {
    String xff = request.getHeader("X-Forwarded-For");
    if (xff != null && !xff.isBlank()) {
      return xff.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }

  /** Token-bucket simples e thread-safe: capacidade = vazão por minuto. */
  static final class Bucket {
    private final double capacity;
    private final double refillPerMilli;
    private double tokens;
    private long lastRefill;

    Bucket(double perMinute) {
      this.capacity = perMinute;
      this.refillPerMilli = perMinute / 60_000.0;
      this.tokens = perMinute;
      this.lastRefill = System.currentTimeMillis();
    }

    synchronized boolean tryConsume() {
      refill();
      if (tokens >= 1.0) {
        tokens -= 1.0;
        return true;
      }
      return false;
    }

    synchronized long retryAfterSeconds() {
      refill();
      double needed = 1.0 - tokens;
      if (needed <= 0) return 0;
      return (long) Math.ceil(needed / (refillPerMilli * 1000.0));
    }

    private void refill() {
      long now = System.currentTimeMillis();
      tokens = Math.min(capacity, tokens + (now - lastRefill) * refillPerMilli);
      lastRefill = now;
    }
  }
}
