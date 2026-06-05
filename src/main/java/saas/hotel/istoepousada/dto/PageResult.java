package saas.hotel.istoepousada.dto;

import java.util.List;

/** Resultado paginado genérico (serializado como content/page/size/totalElements/totalPages). */
public record PageResult<T>(
    List<T> content, int page, int size, long totalElements, int totalPages) {}
