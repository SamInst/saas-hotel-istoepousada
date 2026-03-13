package saas.hotel.istoepousada.dto;

import jakarta.validation.constraints.NotNull;

public record Login(@NotNull String token) {}
