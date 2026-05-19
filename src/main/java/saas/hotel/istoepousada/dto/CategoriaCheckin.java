package saas.hotel.istoepousada.dto;

import java.time.LocalTime;

public record CategoriaCheckin(
    Long id, String nome, LocalTime hora_checkin, LocalTime hora_checkout) {}
