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

    // =========================================================================
    // 1. QUERIES NATIVAS DO DASHBOARD (MATEMÁTICA CORRETA)
    // =========================================================================

    // Lucro por Plataforma (MENSAL)
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

    // NOVO: Lucro por Plataforma (ANUAL - Sem filtro de mês)
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

    // Gráfico de Vendas (Ordenado)
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


    // =========================================================================
    // 2. MÉTODOS HÍBRIDOS (CORREÇÃO DE COMPILAÇÃO)
    // =========================================================================

    // --- FATURAMENTO MÊS ATUAL ---
    @Query(value = "SELECT SUM(preco_venda) FROM venda WHERE user_id = :userId AND EXTRACT(MONTH FROM data) = EXTRACT(MONTH FROM CURRENT_DATE) AND EXTRACT(YEAR FROM data) = EXTRACT(YEAR FROM CURRENT_DATE)", nativeQuery = true)
    Double calcularFaturamentoMesAtual(@Param("userId") Long userId);

    default Double calcularFaturamentoMesAtual(User user) {
        return calcularFaturamentoMesAtual(user.getId());
    }

    // --- LUCRO LÍQUIDO MÊS ATUAL ---
    @Query(value = "SELECT SUM(COALESCE(preco_venda,0) + COALESCE(frete_pago_pelo_cliente,0) - COALESCE(tarifa_plataforma,0) - COALESCE(custo_produto_vendido,0) - COALESCE(custo_envio,0) - COALESCE(despesas_operacionais,0)) FROM venda WHERE user_id = :userId AND EXTRACT(MONTH FROM data) = EXTRACT(MONTH FROM CURRENT_DATE) AND EXTRACT(YEAR FROM data) = EXTRACT(YEAR FROM CURRENT_DATE)", nativeQuery = true)
    Double calcularLucroLiquidoMesAtual(@Param("userId") Long userId);

    default Double calcularLucroLiquidoMesAtual(User user) {
        return calcularLucroLiquidoMesAtual(user.getId());
    }

    // --- CONTAGEM VENDAS MÊS ATUAL ---
    @Query(value = "SELECT COUNT(*) FROM venda WHERE user_id = :userId AND EXTRACT(MONTH FROM data) = EXTRACT(MONTH FROM CURRENT_DATE) AND EXTRACT(YEAR FROM data) = EXTRACT(YEAR FROM CURRENT_DATE)", nativeQuery = true)
    Long countVendasMesAtual(@Param("userId") Long userId);

    default Long countVendasMesAtual(User user) {
        return countVendasMesAtual(user.getId());
    }

    // --- MÉTODOS JPQL (CUSTOS E LUCRO BRUTO) ---
    @Query("SELECT SUM(COALESCE(v.custoProdutoVendido, 0) + COALESCE(v.custoEnvio, 0) + COALESCE(v.tarifaPlataforma, 0)) FROM Venda v WHERE v.user = :user AND EXTRACT(MONTH FROM v.data) = EXTRACT(MONTH FROM CURRENT_DATE) AND EXTRACT(YEAR FROM v.data) = EXTRACT(YEAR FROM CURRENT_DATE)")
    Double calcularCustoEfetivoMesAtual(@Param("user") User user);

    @Query("SELECT SUM(COALESCE(v.precoVenda, 0) - COALESCE(v.custoProdutoVendido, 0)) FROM Venda v WHERE v.user = :user AND EXTRACT(MONTH FROM v.data) = EXTRACT(MONTH FROM CURRENT_DATE) AND EXTRACT(YEAR FROM v.data) = EXTRACT(YEAR FROM CURRENT_DATE)")
    Double calcularLucroBrutoMesAtual(@Param("user") User user);

    // --- CONTAGEM ANO ATUAL ---
    @Query("SELECT COUNT(v) FROM Venda v WHERE v.user = :user AND EXTRACT(YEAR FROM v.data) = EXTRACT(YEAR FROM CURRENT_DATE)")
    Long countVendasAnoAtual(@Param("user") User user);

    @Query("SELECT COUNT(v) FROM Venda v WHERE v.user.id = :userId AND EXTRACT(YEAR FROM v.data) = EXTRACT(YEAR FROM CURRENT_DATE)")
    Long countVendasAnoAtual(@Param("userId") Long userId);

    // --- CONTAGEM MÊS ANTERIOR ---
    @Query(value = "SELECT COUNT(*) FROM venda WHERE user_id = :userId AND data >= (CURRENT_DATE - INTERVAL '1 month') AND data < CURRENT_DATE", nativeQuery = true)
    Long countVendasMesAnterior(@Param("userId") Long userId);

    default Long countVendasMesAnterior(User user) {
        return countVendasMesAnterior(user.getId());
    }

    // --- ANUAIS EXTRAS ---
    @Query("SELECT SUM(COALESCE(v.precoVenda, 0)) FROM Venda v WHERE v.user = :user AND EXTRACT(YEAR FROM v.data) = EXTRACT(YEAR FROM CURRENT_DATE)")
    Double calcularFaturamentoAnoAtual(@Param("user") User user);

    @Query("SELECT SUM(COALESCE(v.precoVenda, 0)) FROM Venda v WHERE v.user.id = :userId AND EXTRACT(YEAR FROM v.data) = EXTRACT(YEAR FROM CURRENT_DATE)")
    Double calcularFaturamentoAnoAtual(@Param("userId") Long userId);

    @Query("SELECT SUM(COALESCE(v.custoProdutoVendido, 0)) FROM Venda v WHERE v.user = :user AND EXTRACT(YEAR FROM v.data) = EXTRACT(YEAR FROM CURRENT_DATE)")
    Double calcularCustoEfetivoAnoAtual(@Param("user") User user);

    @Query("SELECT SUM(COALESCE(v.precoVenda, 0) - COALESCE(v.custoProdutoVendido, 0)) FROM Venda v WHERE v.user = :user AND EXTRACT(YEAR FROM v.data) = EXTRACT(YEAR FROM CURRENT_DATE)")
    Double calcularLucroBrutoAnoAtual(@Param("user") User user);


    // =========================================================================
    // 3. MÉTODOS PADRÃO (COMPATIBILIDADE)
    // =========================================================================

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
}