package com.fernando.erp_vendas.repository;

import com.fernando.erp_vendas.model.Produto;
import com.fernando.erp_vendas.model.User;
import com.fernando.erp_vendas.model.Venda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

@Repository
public interface VendaRepository extends JpaRepository<Venda, Long> {

    // 🆕 MÉTODOS MULTI-TENANCY - TODOS FILTRADOS POR USER

    // Buscar TODAS as vendas do usuário
    List<Venda> findByUser(User user);

    // ✅ NOVO MÉTODO: Buscar vendas com produto carregado (resolve LazyInitialization)
    @Query("SELECT v FROM Venda v JOIN FETCH v.produto WHERE v.user = :user")
    List<Venda> findByUserWithProduto(@Param("user") User user);

    // Buscar vendas pela plataforma DO USUÁRIO
    List<Venda> findByPlataformaAndUser(String plataforma, User user);

    // Buscar uma VENDA pelo ID do pedido E USUÁRIO
    Optional<Venda> findByIdPedidoAndUser(String idPedido, User user);

    // Buscar vendas por produto E USUÁRIO
    List<Venda> findByProdutoAndUser(Produto produto, User user);

    // Buscar vendas por período E USUÁRIO
    List<Venda> findByDataBetweenAndUser(LocalDateTime inicio, LocalDateTime fim, User user);

    // 🆕 Buscar venda por ID e usuário
    Optional<Venda> findByIdAndUser(Long id, User user);

    // 🆕 Contar total de vendas do usuário
    long countByUser(User user);

    // 🆕 Buscar vendas recentes do usuário (últimas 10)
    List<Venda> findTop10ByUserOrderByDataDesc(User user);

    // CONSULTAS COMPLEXAS MULTI-TENANT

    // Consultar lucro por plataforma DO USUÁRIO
    @Query("SELECT v.plataforma, SUM(v.precoVenda) as faturamento FROM Venda v WHERE v.user = :user GROUP BY v.plataforma")
    List<Object[]> findFaturamentoPorPlataforma(@Param("user") User user);

    // Consultar total de vendas DO USUÁRIO
    @Query("SELECT COUNT(v) FROM Venda v WHERE v.user = :user")
    Long countTotalVendas(@Param("user") User user);

    // Consultar total de vendas do mês DO USUÁRIO
    @Query("SELECT COUNT(v) FROM Venda v WHERE v.user = :user AND YEAR(v.data) = :ano AND MONTH(v.data) = :mes")
    Long countVendasDoMes(@Param("user") User user, @Param("ano") int ano, @Param("mes") int mes);

    // Consultar total de vendas por dia DO USUÁRIO
    @Query("SELECT DATE(v.data), COUNT(v) FROM Venda v WHERE v.user = :user GROUP BY DATE(v.data) ORDER BY DATE(v.data)")
    List<Object[]> findVendasPorDia(@Param("user") User user);

    // Consultar produtos mais vendidos DO USUÁRIO
    @Query("SELECT v.produto.nome, SUM(v.quantidade) FROM Venda v WHERE v.user = :user GROUP BY v.produto.nome ORDER BY SUM(v.quantidade) DESC")
    List<Object[]> findProdutosMaisVendidos(@Param("user") User user);

    // Consultar Lucro liquido total DO USUÁRIO
    @Query("SELECT SUM(v.precoVenda + v.fretePagoPeloCliente - v.tarifaPlataforma - v.custoProdutoVendido - v.custoEnvio) FROM Venda v WHERE v.user = :user")
    Double findLucroLiquidoTotal(@Param("user") User user);

    // Custo Efetivo Total (CET) de todas as vendas DO USUÁRIO
    @Query("SELECT SUM(v.custoProdutoVendido + v.custoEnvio) FROM Venda v WHERE v.user = :user")
    Double findCustoEfetivoTotal(@Param("user") User user);

    // Lucro Bruto Total de todas as vendas DO USUÁRIO
    @Query("SELECT SUM(v.precoVenda + v.fretePagoPeloCliente - v.tarifaPlataforma) FROM Venda v WHERE v.user = :user")
    Double findLucroBrutoTotal(@Param("user") User user);

    // 🆕 Consultar faturamento mensal DO USUÁRIO
    @Query("SELECT YEAR(v.data), MONTH(v.data), SUM(v.precoVenda + v.fretePagoPeloCliente - v.tarifaPlataforma) " +
            "FROM Venda v WHERE v.user = :user GROUP BY YEAR(v.data), MONTH(v.data) ORDER BY YEAR(v.data), MONTH(v.data)")
    List<Object[]> findFaturamentoMensal(@Param("user") User user);

    // 🆕 Consultar ROI médio DO USUÁRIO
    @Query("SELECT AVG((v.precoVenda + v.fretePagoPeloCliente - v.tarifaPlataforma - v.custoProdutoVendido - v.custoEnvio) / (v.custoProdutoVendido + v.custoEnvio) * 100) " +
            "FROM Venda v WHERE v.user = :user AND (v.custoProdutoVendido + v.custoEnvio) > 0")
    Double findRoiMedio(@Param("user") User user);

    // ✅ MÉTODOS LEGACY (MANTIDOS PARA COMPATIBILIDADE - USAR COM CAUTELA)

    // @deprecated - Use findByPlataformaAndUser em vez disso
    @Deprecated
    List<Venda> findByPlataforma(String plataforma);

    // @deprecated - Use findByIdPedidoAndUser em vez disso
    @Deprecated
    Venda findByIdPedido(String idPedido);

    // @deprecated - Use findByProdutoAndUser em vez disso
    @Deprecated
    List<Venda> findByProduto(Produto produto);

    // @deprecated - Use findByDataBetweenAndUser em vez disso
    @Deprecated
    List<Venda> findByDataBetween(LocalDateTime inicio, LocalDateTime fim);

    // @deprecated - Use findFaturamentoPorPlataforma com user em vez disso
    @Deprecated
    @Query("SELECT v.plataforma, SUM(v.precoVenda) as faturamento FROM Venda v GROUP BY v.plataforma")
    List<Object[]> findFaturamentoPorPlataforma();

    // @deprecated - Use countTotalVendas com user em vez disso
    @Deprecated
    @Query("SELECT COUNT(v) FROM Venda v")
    Long countTotalVendas();

    // @deprecated - Use countVendasDoMes com user em vez disso
    @Deprecated
    @Query("SELECT COUNT(v) FROM Venda v WHERE YEAR(v.data) = :ano AND MONTH(v.data) = :mes")
    Long countVendasDoMes(@Param("ano") int ano, @Param("mes") int mes);

    // @deprecated - Use findVendasPorDia com user em vez disso
    @Deprecated
    @Query("SELECT DATE(v.data), COUNT(v) FROM Venda v GROUP BY DATE(v.data) ORDER BY DATE(v.data)")
    List<Object[]> findVendasPorDia();

    // @deprecated - Use findProdutosMaisVendidos com user em vez disso
    @Deprecated
    @Query("SELECT v.produto.nome, SUM(v.quantidade) FROM Venda v GROUP BY v.produto.nome ORDER BY SUM(v.quantidade) DESC")
    List<Object[]> findProdutosMaisVendidos();

    // @deprecated - Use findLucroLiquidoTotal com user em vez disso
    @Deprecated
    @Query("SELECT SUM(v.precoVenda + v.fretePagoPeloCliente - v.tarifaPlataforma - v.custoProdutoVendido - v.custoEnvio) FROM Venda v")
    Double findLucroLiquidoTotal();

    // @deprecated - Use findCustoEfetivoTotal com user em vez disso
    @Deprecated
    @Query("SELECT SUM(v.custoProdutoVendido + v.custoEnvio) FROM Venda v")
    Double findCustoEfetivoTotal();

    // @deprecated - Use findLucroBrutoTotal com user em vez disso
    @Deprecated
    @Query("SELECT SUM(v.precoVenda + v.fretePagoPeloCliente - v.tarifaPlataforma) FROM Venda v")
    Double findLucroBrutoTotal();
}