package saas.hotel.istoepousada.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import saas.hotel.istoepousada.dto.Funcionario;

@Component
public class JwtUtil {
  @Value("${jwt.secret}")
  private String jwtSecret;

  @Value("${jwt.expiration}")
  private long jwtExpiration;

  private final ObjectMapper objectMapper;

  public JwtUtil(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  private SecretKey getSigningKey() {
    byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
    return Keys.hmacShaKeyFor(keyBytes);
  }

  public String generateToken(Funcionario.Authorization funcionario) {
    try {
      String funcionarioJson = objectMapper.writeValueAsString(funcionario);
      @SuppressWarnings("unchecked")
      Map<String, Object> funcionarioMap = objectMapper.readValue(funcionarioJson, Map.class);
      return Jwts.builder()
          .subject(funcionario.usuario().username())
          .claim("funcionario", funcionarioMap)
          .claim("usuarioId", funcionario.usuario().id())
          .claim("funcionarioId", funcionario.id())
          .claim("pessoaId", funcionario.pessoa().id())
          .issuedAt(new Date())
          .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
          .signWith(getSigningKey())
          .compact();
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Erro ao gerar token JWT", e);
    }
  }

  public Funcionario.Authorization getFuncionarioFromToken(String token) {
    try {
      String resolvedToken = resolveToken(token);

      Claims claims =
          Jwts.parser()
              .verifyWith(getSigningKey())
              .build()
              .parseSignedClaims(resolvedToken)
              .getPayload();

      @SuppressWarnings("unchecked")
      Map<String, Object> funcionarioMap = (Map<String, Object>) claims.get("funcionario");

      String funcionarioJson = objectMapper.writeValueAsString(funcionarioMap);
      return objectMapper.readValue(funcionarioJson, Funcionario.Authorization.class);
    } catch (Exception e) {
      throw new RuntimeException("Erro ao extrair funcionário do token", e);
    }
  }

  public boolean validateToken(String token) {
    try {
      String resolvedToken = resolveToken(token);
      if (resolvedToken == null || resolvedToken.isBlank()) return false;

      Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(resolvedToken);

      return true;
    } catch (JwtException | IllegalArgumentException e) {
      return false;
    }
  }

  private String resolveToken(String token) {
    if (token == null || token.isBlank()) return null;
    token = token.trim();
    if (token.startsWith("Bearer ")) {
      token = token.substring(7).trim();
    }
    return token;
  }
}
