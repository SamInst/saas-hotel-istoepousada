package saas.hotel.istoepousada.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class BrLocalDateDeserializer extends StdDeserializer<LocalDate> {

  private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  public BrLocalDateDeserializer() {
    super(LocalDate.class);
  }

  @Override
  public LocalDate deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
    return LocalDate.parse(p.getText(), FMT);
  }
}
