package com.fernando.erp_vendas.repository;

import com.fernando.erp_vendas.model.Venda;
import com.fernando.erp_vendas.model.User;
import com.fernando.erp_vendas.model.Produto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VendaRepository extends JpaRepository<Venda, Long> {

    @Query(value = """
        SELECT 
            v.plataforma, 
            SUM(
                COALESCE(v.preco_venda, 0) + 
                COALESCE(v.frete_pago_pelo_cliente, 0) - 
                COALESCE(v.tarifa_plataforma, 0) - 
                COALESCE(v.custo_produto_vendido, 0) - 
                COALESCE(v.custo_envio, 0) - 
                COALESCE(v.despesas_operacionais, 0)
            ) as lucro_total
        FROM venda v 
        WHERE v.user_id = :userId 
        AND EXTRACT(MONTH FROM v.data) = :mes 
        AND EXTRACT(YEAR FROM v.data) = :ano 
        GROUP BY v.plataforma
    """, nativeQuery = true)
    List<Object[]> findLucroPorPlataformaNative(@Param("userId") Long userId, @Param("mes") int mes, @Param("ano") int ano);

    @Query(value = """
        SELECT 
            v.plataforma, 
            SUM(
                COALESCE(v.preco_venda, 0) + 
                COALESCE(v.frete_pago_pelo_cliente, 0) - 
                COALESCE(v.tarifa_plataforma, 0) - 
                COALESCE(v.custo_produto_vendido, 0) - 
                COALESCE(v.custo_envio, 0) - 
                COALESCE(v.despesas_operacionais, 0)
            ) as lucro_total
        FROM venda v 
        WHERE v.user_id = :userId 
        AND EXTRACT(YEAR FROM v.data) = :ano 
        GROUP BY v.plataforma
    """, nativeQuery = true)
    List<Object[]> findLucroPorPlataformaAnualNative(@Param("userId") Long userId, @Param("ano") int ano);

    @Query(value = """
        SELECT 
            CAST(v.data AS DATE) as dia, 
            SUM(COALESCE(v.preco_venda, 0)) as total
        FROM venda v 
        WHERE v.user_id = :userId 
        AND EXTRACT(MONTH FROM v.data) = :mes 
        AND EXTRACT(YEAR FROM v.data) = :ano 
        GROUP BY CAST(v.data AS DATE) 
        ORDER BY dia ASC
    """, nativeQuery = true)
    List<Object[]> findVendasPorDiaDoMesNative(@Param("userId") Long userId, @Param("mes") int mes, @Param("ano") int ano);

    @Query(value = """
        SELECT 
            p.id as produtoId,
            p.nome as produtoNome,
            p.imagem_url as imagemUrl,
            SUM(i.quantidade) as quantidadeVendida,
            AVG(i.preco_unitario) as precoMedioVenda,
            AVG(COALESCE(i.custo_unitario, 0)) as custoMedio,
            (AVG(i.preco_unitario) - AVG(COALESCE(i.custo_unitario, 0))) as lucroPorUnidade
        FROM item_venda i
        INNER JOIN venda v ON i.venda_id = v.id
        INNER JOIN produto p ON i.produto_id = p.id
        WHERE v.user_id = :userId
          AND (CAST(:dataInicio AS TIMESTAMP) IS NULL OR v.data >= CAST(:dataInicio AS TIMESTAMP))
        GROUP BY p.id, p.nome, p.imagem_url
        ORDER BY quantidadeVendida DESC
        LIMIT :limit
    """, nativeQuery = true)
    List<Object[]> findTopProdutosPorPeriodoNative(
            @Param("userId") Long userId,
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("limit") int limit
    );

    @Query(value = "SELECT SUM(preco_venda) FROM venda WHERE user_id = :userId AND EXTRACT(MONTH FROM data) = EXTRACT(MONTH FROM CURRENT_DATE) AND EXTRACT(YEAR FROM data) = EXTRACT(YEAR FROM CURRENT_DATE)", nativeQuery = true)
    Double calcularFaturamentoMesAtual(@Param("userId") Long userId);
    default Double calcularFaturamentoMesAtual(User user) { return calcularFaturamentoMesAtual(user.getId()); }

    @Query(value = "SELECT SUM(COALESCE(preco_venda,0) + COALESCE(frete_pago_pelo_cliente,0) - COALESCE(tarifa_plataforma,0) - COALESCE(custo_produto_vendido,0) - COALESCE(custo_envio,0) - COALESCE(despesas_operacionais,0)) FROM venda WHERE user_id = :userId AND EXTRACT(MONTH FROM data) = EXTRACT(MONTH FROM CURRENT_DATE) AND EXTRACT(YEAR FROM data) = EXTRACT(YEAR FROM CURRENT_DATE)", nativeQuery = true)
    Double calcularLucroLiquidoMesAtual(@Param("userId") Long userId);
    default Double calcularLucroLiquidoMesAtual(User user) { return calcularLucroLiquidoMesAtual(user.getId()); }

    @Query(value = "SELECT COUNT(*) FROM venda WHERE user_id = :userId AND EXTRACT(MONTH FROM data) = EXTRACT(MONTH FROM CURRENT_DATE) AND EXTRACT(YEAR FROM data) = EXTRACT(YEAR FROM CURRENT_DATE)", nativeQuery = true)
    Long countVendasMesAtual(@Param("userId") Long userId);
    default Long countVendasMesAtual(User user) { return countVendasMesAtual(user.getId()); }

    @Query("SELECT SUM(COALESCE(v.custoProdutoVendido, 0) + COALESCE(v.custoEnvio, 0) + COALESCE(v.tarifaPlataforma, 0)) FROM Venda v WHERE v.user = :user AND EXTRACT(MONTH FROM v.data) = EXTRACT(MONTH FROM CURRENT_DATE) AND EXTRACT(YEAR FROM v.data) = EXTRACT(YEAR FROM CURRENT_DATE)")
    Double calcularCustoEfetivoMesAtual(@Param("user") User user);

    @Query("SELECT SUM(COALESCE(v.precoVenda, 0) - COALESCE(v.custoProdutoVendido, 0)) FROM Venda v WHERE v.user = :user AND EXTRACT(MONTH FROM v.data) = EXTRACT(MONTH FROM CURRENT_DATE) AND EXTRACT(YEAR FROM v.data) = EXTRACT(YEAR FROM CURRENT_DATE)")
    Double calcularLucroBrutoMesAtual(@Param("user") User user);

    @Query("SELECT COUNT(v) FROM Venda v WHERE v.user = :user AND EXTRACT(YEAR FROM v.data) = EXTRACT(YEAR FROM CURRENT_DATE)")
    Long countVendasAnoAtual(@Param("user") User user);

    @Query("SELECT COUNT(v) FROM Venda v WHERE v.user.id = :userId AND EXTRACT(YEAR FROM v.data) = EXTRACT(YEAR FROM CURRENT_DATE)")
    Long countVendasAnoAtual(@Param("userId") Long userId);

    @Query(value = "SELECT COUNT(*) FROM venda WHERE user_id = :userId AND data >= (CURRENT_DATE - INTERVAL '1 month') AND data < CURRENT_DATE", nativeQuery = true)
    Long countVendasMesAnterior(@Param("userId") Long userId);
    default Long countVendasMesAnterior(User user) { return countVendasMesAnterior(user.getId()); }

    @Query("SELECT SUM(COALESCE(v.precoVenda, 0)) FROM Venda v WHERE v.user = :user AND EXTRACT(YEAR FROM v.data) = EXTRACT(YEAR FROM CURRENT_DATE)")
    Double calcularFaturamentoAnoAtual(@Param("user") User user);

    @Query("SELECT SUM(COALESCE(v.precoVenda, 0)) FROM Venda v WHERE v.user.id = :userId AND EXTRACT(YEAR FROM v.data) = EXTRACT(YEAR FROM CURRENT_DATE)")
    Double calcularFaturamentoAnoAtual(@Param("userId") Long userId);

    @Query("SELECT SUM(COALESCE(v.custoProdutoVendido, 0)) FROM Venda v WHERE v.user = :user AND EXTRACT(YEAR FROM v.data) = EXTRACT(YEAR FROM CURRENT_DATE)")
    Double calcularCustoEfetivoAnoAtual(@Param("user") User user);

    @Query("SELECT SUM(COALESCE(v.precoVenda, 0) - COALESCE(v.custoProdutoVendido, 0)) FROM Venda v WHERE v.user = :user AND EXTRACT(YEAR FROM v.data) = EXTRACT(YEAR FROM CURRENT_DATE)")
    Double calcularLucroBrutoAnoAtual(@Param("user") User user);

    Optional<Venda> findByIdAndUser(Long id, User user);
    Optional<Venda> findByIdPedidoAndUser(String idPedido, User user);
    List<Venda> findByUser(User user);

    @Query("SELECT v FROM Venda v LEFT JOIN FETCH v.itens WHERE v.user = :user")
    List<Venda> findByUserWithProduto(@Param("user") User user);

    List<Venda> findByPlataformaAndUser(String plataforma, User user);
    List<Venda> findByDataBetweenAndUser(LocalDateTime start, LocalDateTime end, User user);

    @Query("SELECT v FROM Venda v JOIN v.itens i WHERE i.produto = :produto AND v.user = :user")
    List<Venda> findByProdutoInItens(@Param("produto") Produto produto, @Param("user") User user);

    @Query("SELECT CAST(v.data AS date), SUM(COALESCE(v.precoVenda, 0)) FROM Venda v WHERE v.user = :user GROUP BY CAST(v.data AS date) ORDER BY CAST(v.data AS date) ASC")
    List<Object[]> findVendasPorDia(@Param("user") User user);

    @Query("SELECT CAST(v.data AS date), SUM(COALESCE(v.precoVenda, 0)) FROM Venda v WHERE v.user = :user AND EXTRACT(MONTH FROM v.data) = :mes AND EXTRACT(YEAR FROM v.data) = :ano GROUP BY CAST(v.data AS date) ORDER BY CAST(v.data AS date) ASC")
    List<Object[]> findVendasPorDiaDoMes(@Param("user") User user, @Param("mes") Integer mes, @Param("ano") Integer ano);

    // ================== MÉTODO NATIVO PARA MÉTRICAS AGREGADAS ==================
    @Query(value = "SELECT " +
            "COALESCE(SUM(CASE WHEN status = 'ATIVA' THEN preco_venda + frete_pago_pelo_cliente ELSE 0 END), 0) as faturamento, " +
            "COALESCE(SUM(CASE WHEN status = 'ATIVA' THEN custo_produto_vendido + custo_envio + tarifa_plataforma " +
            "WHEN status = 'CANCELADA' AND retornou_estoque = true THEN custo_envio + custo_retorno " +
            "WHEN status = 'CANCELADA' AND retornou_estoque = false THEN custo_produto_vendido + custo_envio + custo_retorno " +
            "ELSE 0 END), 0) as custo_efetivo, " +
            "COALESCE(SUM(despesas_operacionais), 0) as despesas_operacionais " +
            "FROM venda v WHERE v.user_id = :userId AND v.data >= :inicio AND v.data < :fim", nativeQuery = true)
    List<Object[]> getMetricasAgregadasNative(@Param("userId") Long userId,
                                              @Param("inicio") LocalDateTime inicio,
                                              @Param("fim") LocalDateTime fim);
}