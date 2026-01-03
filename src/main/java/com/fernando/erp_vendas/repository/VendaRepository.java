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

    // ✅✅✅ CORRIGIDO: Buscar vendas com itens carregados (agora com múltiplos produtos)
    @Query("SELECT DISTINCT v FROM Venda v LEFT JOIN FETCH v.itens i LEFT JOIN FETCH i.lote l LEFT JOIN FETCH l.produto WHERE v.user = :user")
    List<Venda> findByUserWithProduto(@Param("user") User user);

    // Buscar vendas pela plataforma DO USUÁRIO
    List<Venda> findByPlataformaAndUser(String plataforma, User user);

    // ✅ REMOVIDO: findByProdutoAndUser - não faz mais sentido com múltiplos produtos
    // List<Venda> findByProdutoAndUser(Produto produto, User user);

    // Buscar vendas por período E USUÁRIO
    List<Venda> findByDataBetweenAndUser(LocalDateTime inicio, LocalDateTime fim, User user);

    // 🆕 Buscar venda por ID e usuário
    Optional<Venda> findByIdAndUser(Long id, User user);

    // Buscar uma VENDA pelo ID do pedido E USUÁRIO
    Optional<Venda> findByIdPedidoAndUser(String idPedido, User user);

    // 🆕 Contar total de vendas do usuário
    long countByUser(User user);

    // 🆕 Buscar vendas recentes do usuário (últimas 10)
    List<Venda> findTop10ByUserOrderByDataDesc(User user);

    // CONSULTAS COMPLEXAS MULTI-TENANT

    // Consultar lucro por plataforma DO USUÁRIO (OK - não usa produto)
    @Query("SELECT v.plataforma, SUM(v.precoVenda) as faturamento FROM Venda v WHERE v.user = :user GROUP BY v.plataforma")
    List<Object[]> findFaturamentoPorPlataforma(@Param("user") User user);

    // Consultar total de vendas DO USUÁRIO (OK)
    @Query("SELECT COUNT(v) FROM Venda v WHERE v.user = :user")
    Long countTotalVendas(@Param("user") User user);

    // Consultar total de vendas do mês DO USUÁRIO (OK)
    @Query("SELECT COUNT(v) FROM Venda v WHERE v.user = :user AND YEAR(v.data) = :ano AND MONTH(v.data) = :mes")
    Long countVendasDoMes(@Param("user") User user, @Param("ano") int ano, @Param("mes") int mes);

    // Consultar total de vendas por dia DO USUÁRIO (OK)
    @Query("SELECT DATE(v.data), COUNT(v) FROM Venda v WHERE v.user = :user GROUP BY DATE(v.data) ORDER BY DATE(v.data)")
    List<Object[]> findVendasPorDia(@Param("user") User user);

    // 🆕 CONSULTA ADICIONADA: Consultar total de vendas por dia COM FILTRO DE MÊS/ANO (OK)
    @Query("SELECT DATE(v.data), COUNT(v) FROM Venda v WHERE v.user = :user AND YEAR(v.data) = :ano AND MONTH(v.data) = :mes GROUP BY DATE(v.data) ORDER BY DATE(v.data)")
    List<Object[]> findVendasPorDiaDoMes(@Param("user") User user, @Param("mes") int mes, @Param("ano") int ano);

    // ✅✅✅ CORRIGIDO: Consultar produtos mais vendidos DO USUÁRIO (AGORA via itens)
    @Query("SELECT p.nome, SUM(i.quantidade) FROM Venda v JOIN v.itens i JOIN i.lote l JOIN l.produto p WHERE v.user = :user GROUP BY p.nome ORDER BY SUM(i.quantidade) DESC")
    List<Object[]> findProdutosMaisVendidos(@Param("user") User user);

    // Consultar Lucro liquido total DO USUÁRIO (OK - não usa produto)
    @Query("SELECT SUM(v.precoVenda + v.fretePagoPeloCliente - v.tarifaPlataforma - v.custoProdutoVendido - v.custoEnvio) FROM Venda v WHERE v.user = :user")
    Double findLucroLiquidoTotal(@Param("user") User user);

    // Custo Efetivo Total (CET) de todas as vendas DO USUÁRIO (OK)
    @Query("SELECT SUM(v.custoProdutoVendido + v.custoEnvio) FROM Venda v WHERE v.user = :user")
    Double findCustoEfetivoTotal(@Param("user") User user);

    // ✅ CORRIGIDO: Lucro Bruto Total de todas as vendas DO USUÁRIO (OK)
    @Query("SELECT SUM(v.precoVenda + v.fretePagoPeloCliente - v.custoProdutoVendido - v.custoEnvio - v.tarifaPlataforma) FROM Venda v WHERE v.user = :user")
    Double findLucroBrutoTotal(@Param("user") User user);

    // ✅ CORRIGIDO: Consultar faturamento mensal DO USUÁRIO (OK)
    @Query("SELECT YEAR(v.data), MONTH(v.data), SUM(v.precoVenda + v.fretePagoPeloCliente) " +
            "FROM Venda v WHERE v.user = :user GROUP BY YEAR(v.data), MONTH(v.data) ORDER BY YEAR(v.data), MONTH(v.data)")
    List<Object[]> findFaturamentoMensal(@Param("user") User user);

    // ✅ CORRIGIDO: Consultar ROI médio DO USUÁRIO (OK)
    @Query("SELECT AVG((v.precoVenda + v.fretePagoPeloCliente - v.tarifaPlataforma - v.custoProdutoVendido - v.custoEnvio) / (v.custoProdutoVendido + v.custoEnvio) * 100) " +
            "FROM Venda v WHERE v.user = :user AND (v.custoProdutoVendido + v.custoEnvio) > 0")
    Double findRoiMedio(@Param("user") User user);

    // ✅ CORRIGIDO: Calcular faturamento do mês atual DO USUÁRIO (OK)
    @Query("SELECT SUM(v.precoVenda + v.fretePagoPeloCliente) FROM Venda v WHERE v.user = :user AND YEAR(v.data) = YEAR(CURRENT_DATE) AND MONTH(v.data) = MONTH(CURRENT_DATE)")
    Double calcularFaturamentoMesAtual(@Param("user") User user);

    // 🆕 Calcular custo efetivo do mês atual DO USUÁRIO (OK)
    @Query("SELECT SUM(v.custoProdutoVendido + v.custoEnvio + v.tarifaPlataforma) FROM Venda v WHERE v.user = :user AND YEAR(v.data) = YEAR(CURRENT_DATE) AND MONTH(v.data) = MONTH(CURRENT_DATE)")
    Double calcularCustoEfetivoMesAtual(@Param("user") User user);

    // ✅ CORRIGIDO: Calcular lucro bruto do mês atual DO USUÁRIO (OK)
    @Query("SELECT SUM(v.precoVenda + v.fretePagoPeloCliente - v.custoProdutoVendido - v.custoEnvio - v.tarifaPlataforma) FROM Venda v WHERE v.user = :user AND YEAR(v.data) = YEAR(CURRENT_DATE) AND MONTH(v.data) = MONTH(CURRENT_DATE)")
    Double calcularLucroBrutoMesAtual(@Param("user") User user);

    // ✅ CORRIGIDO: Calcular lucro líquido do mês atual DO USUÁRIO (OK)
    @Query("SELECT SUM(v.precoVenda + v.fretePagoPeloCliente - v.tarifaPlataforma - v.custoProdutoVendido - v.custoEnvio - v.despesasOperacionais) FROM Venda v WHERE v.user = :user AND YEAR(v.data) = YEAR(CURRENT_DATE) AND MONTH(v.data) = MONTH(CURRENT_DATE)")
    Double calcularLucroLiquidoMesAtual(@Param("user") User user);

    // 🆕 ADICIONADO: Calcular faturamento do ano atual DO USUÁRIO
    @Query("SELECT SUM(v.precoVenda + v.fretePagoPeloCliente) FROM Venda v WHERE v.user = :user AND YEAR(v.data) = YEAR(CURRENT_DATE)")
    Double calcularFaturamentoAnoAtual(@Param("user") User user);

    // 🆕 ADICIONADO: Calcular custo efetivo do ano atual DO USUÁRIO
    @Query("SELECT SUM(v.custoProdutoVendido + v.custoEnvio + v.tarifaPlataforma) FROM Venda v WHERE v.user = :user AND YEAR(v.data) = YEAR(CURRENT_DATE)")
    Double calcularCustoEfetivoAnoAtual(@Param("user") User user);

    // 🆕 ADICIONADO: Calcular lucro bruto do ano atual DO USUÁRIO
    @Query("SELECT SUM(v.precoVenda + v.fretePagoPeloCliente - v.custoProdutoVendido - v.custoEnvio - v.tarifaPlataforma) FROM Venda v WHERE v.user = :user AND YEAR(v.data) = YEAR(CURRENT_DATE)")
    Double calcularLucroBrutoAnoAtual(@Param("user") User user);

    // 🆕 MÉTODOS PARA QUANTIDADE DE VENDAS - MULTI-TENANCY (CORRIGIDOS PARA POSTGRESQL)

    // Contar vendas do mês atual DO USUÁRIO (PostgreSQL)
    @Query(value = "SELECT COUNT(*) FROM venda v WHERE v.user_id = :userId " +
            "AND EXTRACT(YEAR FROM v.data) = EXTRACT(YEAR FROM CURRENT_DATE) " +
            "AND EXTRACT(MONTH FROM v.data) = EXTRACT(MONTH FROM CURRENT_DATE)",
            nativeQuery = true)
    Long countVendasMesAtual(@Param("userId") Long userId);

    // Contar vendas do mês anterior DO USUÁRIO (PostgreSQL - CORRIGIDO)
    @Query(value = "SELECT COUNT(*) FROM venda v WHERE v.user_id = :userId " +
            "AND EXTRACT(YEAR FROM v.data) = EXTRACT(YEAR FROM CURRENT_DATE - INTERVAL '1 month') " +
            "AND EXTRACT(MONTH FROM v.data) = EXTRACT(MONTH FROM CURRENT_DATE - INTERVAL '1 month')",
            nativeQuery = true)
    Long countVendasMesAnterior(@Param("userId") Long userId);

    // Contar vendas do ano atual DO USUÁRIO (PostgreSQL)
    @Query(value = "SELECT COUNT(*) FROM venda v WHERE v.user_id = :userId " +
            "AND EXTRACT(YEAR FROM v.data) = EXTRACT(YEAR FROM CURRENT_DATE)",
            nativeQuery = true)
    Long countVendasAnoAtual(@Param("userId") Long userId);

    // ✅ MÉTODOS LEGACY (MANTIDOS PARA COMPATIBILIDADE - USAR COM CAUTELA)
    // Nota: Removi os métodos depreciados que usam v.produto pois não funcionam mais

    // @deprecated - Use findByPlataformaAndUser em vez disso
    @Deprecated
    List<Venda> findByPlataforma(String plataforma);

    // @deprecated - Use findByIdPedidoAndUser em vez disso
    @Deprecated
    Venda findByIdPedido(String idPedido);

    // @deprecated - REMOVIDO: findByProduto não funciona mais
    // List<Venda> findByProduto(Produto produto);

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

    // @deprecated - REMOVIDO: findProdutosMaisVendidos() não funciona mais
    // @Query("SELECT v.produto.nome, SUM(v.quantidade) FROM Venda v GROUP BY v.produto.nome ORDER BY SUM(v.quantidade) DESC")
    // List<Object[]> findProdutosMaisVendidos();

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

    // ✅ NOVO: Buscar vendas que contenham um produto específico (para busca)
    @Query("SELECT DISTINCT v FROM Venda v JOIN v.itens i JOIN i.lote l WHERE l.produto = :produto AND v.user = :user")
    List<Venda> findByProdutoInItens(@Param("produto") Produto produto, @Param("user") User user);
}