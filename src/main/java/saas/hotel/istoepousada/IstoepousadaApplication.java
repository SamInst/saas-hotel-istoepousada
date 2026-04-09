package saas.hotel.istoepousada;

import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class IstoepousadaApplication {

  public static void main(String[] args) {
    TimeZone.setDefault(TimeZone.getTimeZone("America/Sao_Paulo"));
    SpringApplication.run(IstoepousadaApplication.class, args);
  }
}
