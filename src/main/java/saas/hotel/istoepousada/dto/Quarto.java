package saas.hotel.istoepousada.dto;

import java.sql.ResultSet;
import java.sql.SQLException;
import saas.hotel.istoepousada.enums.StatusQuarto;

public record Quarto(
        Long id,
        String descricao,
        Integer quantidade_pessoas,
        StatusQuarto status_quarto,
        Integer qtd_cama_casal,
        Integer qtd_cama_solteiro,
        Integer qtd_rede,
        Integer qtd_beliche
) {

    public static Quarto mapQuarto(ResultSet rs) throws SQLException {
        return mapQuarto(rs, "");
    }

    /**
     * Espera colunas com os aliases:
     * - {prefix}id
     * - {prefix}descricao
     * - {prefix}quantidade_pessoas
     * - {prefix}status_quarto_enum
     * - {prefix}qtd_cama_casal
     * - {prefix}qtd_cama_solteiro
     * - {prefix}qtd_rede
     * - {prefix}qtd_beliche
     *
     * Categoria:
     * - aqui fica null por padrão (normalmente vem de JOIN com alias próprio).
     * - se você fizer JOIN com categoria e quiser mapear aqui, use Categoria.mapCategoria(rs, "cat_")
     *   e passe o resultado no construtor.
     *
     * Status:
     * - no banco é int2 (ordinal). Aqui faço StatusQuarto.values()[ordinal].
     * - isso exige que a ordem do enum seja exatamente a mesma do banco.
     * - se seu banco usa códigos (1,2,3...) e seu enum começa em 0, ajuste (ex: ordinal-1).
     */
    public static Quarto mapQuarto(ResultSet rs, String prefix) throws SQLException {
        Long id = rs.getObject(prefix + "id", Long.class);
        String descricao = rs.getString(prefix + "descricao");
        Integer qtdPessoas = rs.getObject(prefix + "quantidade_pessoas", Integer.class);

        Short statusDb = rs.getObject(prefix + "status_quarto_enum", Short.class);
        StatusQuarto status =
                statusDb == null ? null : StatusQuarto.values()[statusDb.intValue()];

        Integer camaCasal = rs.getObject(prefix + "qtd_cama_casal", Integer.class);
        Integer camaSolteiro = rs.getObject(prefix + "qtd_cama_solteiro", Integer.class);
        Integer rede = rs.getObject(prefix + "qtd_rede", Integer.class);
        Integer beliche = rs.getObject(prefix + "qtd_beliche", Integer.class);

        return new Quarto(
                id,
                descricao,
                qtdPessoas,
                status,
                camaCasal,
                camaSolteiro,
                rede,
                beliche
        );
    }
}
