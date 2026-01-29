package saas.hotel.istoepousada.dto;

import java.util.List;

public record PessoaBatchRequest(List<Pessoa> pessoas, List<Long> empresasIds) {}
