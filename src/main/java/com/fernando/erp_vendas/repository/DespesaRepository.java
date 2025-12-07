package com.fernando.erp_vendas.repository;

import com.fernando.erp_vendas.model.Despesa;
import com.fernando.erp_vendas.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DespesaRepository extends JpaRepository<Despesa, Long> {

    // 🆕 MÉTODOS MULTI-TENANCY - TODOS FILTRADOS POR USER

    // Buscar TODAS as despesas do usuário ordenadas por data (mais recente primeiro)
    List<Despesa> findByUserOrderByDataDesc(User user);

    // Buscar despesas por categoria DO USUÁRIO
    List<Despesa> findByCategoriaAndUserOrderByDataDesc(String categoria, User user);

    // Buscar despesas por período E USUÁRIO
    List<Despesa> findByDataBetweenAndUserOrderByDataDesc(LocalDate inicio, LocalDate fim, User user);

    // Buscar despesas recorrentes DO USUÁRIO
    List<Despesa> findByRecorrenteTrueAndUserOrderByDataDesc(User user);

    // 🆕 Buscar despesa por ID e usuário
    Optional<Despesa> findByIdAndUser(Long id, User user);

    // 🆕 Buscar despesas por descrição (busca parcial) DO USUÁRIO
    List<Despesa> findByDescricaoContainingAndUser(String descricao, User user);

    // 🆕 Contar total de despesas do usuário
    long countByUser(User user);

    // 🆕 Buscar despesas do mês atual DO USUÁRIO
    @Query("SELECT d FROM Despesa d WHERE d.user = :user AND YEAR(d.data) = YEAR(CURRENT_DATE) AND MONTH(d.data) = MONTH(CURRENT_DATE) ORDER BY d.data DESC")
    List<Despesa> findDespesasDoMesAtual(@Param("user") User user);

    // CONSULTAS COMPLEXAS MULTI-TENANT

    // Calcular total de despesas por período DO USUÁRIO
    @Query("SELECT SUM(d.valor) FROM Despesa d WHERE d.user = :user AND d.data BETWEEN :inicio AND :fim")
    BigDecimal calcularTotalDespesasPorPeriodo(@Param("user") User user,
                                               @Param("inicio") LocalDate inicio,
                                               @Param("fim") LocalDate fim);

    // Calcular total de despesas por categoria e período DO USUÁRIO
    @Query("SELECT SUM(d.valor) FROM Despesa d WHERE d.user = :user AND d.categoria = :categoria AND d.data BETWEEN :inicio AND :fim")
    BigDecimal calcularTotalDespesasPorCategoriaEPeriodo(@Param("user") User user,
                                                         @Param("categoria") String categoria,
                                                         @Param("inicio") LocalDate inicio,
                                                         @Param("fim") LocalDate fim);

    // 🆕 Calcular total de despesas do mês atual DO USUÁRIO
    @Query("SELECT SUM(d.valor) FROM Despesa d WHERE d.user = :user AND YEAR(d.data) = YEAR(CURRENT_DATE) AND MONTH(d.data) = MONTH(CURRENT_DATE)")
    BigDecimal calcularTotalDespesasMesAtual(@Param("user") User user);

    // 🆕 Calcular total de despesas do ANO ATUAL DO USUÁRIO
    @Query("SELECT SUM(d.valor) FROM Despesa d WHERE d.user = :user AND YEAR(d.data) = YEAR(CURRENT_DATE)")
    BigDecimal calcularTotalDespesasAnoAtual(@Param("user") User user);

    // 🆕 Calcular média mensal de despesas DO USUÁRIO
    @Query("SELECT AVG(d.valor) FROM Despesa d WHERE d.user = :user AND d.data >= :inicio")
    BigDecimal calcularMediaMensalDespesas(@Param("user") User user, @Param("inicio") LocalDate inicio);

    // Buscar categorias distintas DO USUÁRIO
    @Query("SELECT DISTINCT d.categoria FROM Despesa d WHERE d.user = :user ORDER BY d.categoria")
    List<String> findCategoriasDistintas(@Param("user") User user);

    // 🆕 Buscar top 5 categorias com maior gasto DO USUÁRIO
    @Query("SELECT d.categoria, SUM(d.valor) FROM Despesa d WHERE d.user = :user GROUP BY d.categoria ORDER BY SUM(d.valor) DESC LIMIT 5")
    List<Object[]> findTopCategoriasComMaiorGasto(@Param("user") User user);

    // 🆕 Buscar despesas recorrentes totais por mês DO USUÁRIO
    @Query("SELECT d.categoria, SUM(d.valor) FROM Despesa d WHERE d.user = :user AND d.recorrente = true GROUP BY d.categoria")
    List<Object[]> findDespesasRecorrentesTotais(@Param("user") User user);

    // ✅ MÉTODOS LEGACY (MANTIDOS PARA COMPATIBILIDADE - USAR COM CAUTELA)

    // @deprecated - Use findByUserOrderByDataDesc em vez disso
    @Deprecated
    List<Despesa> findAllByOrderByDataDesc();

    // @deprecated - Use findByCategoriaAndUserOrderByDataDesc em vez disso
    @Deprecated
    List<Despesa> findByCategoriaOrderByDataDesc(String categoria);

    // @deprecated - Use findByDataBetweenAndUserOrderByDataDesc em vez disso
    @Deprecated
    List<Despesa> findByDataBetweenOrderByDataDesc(LocalDate inicio, LocalDate fim);

    // @deprecated - Use findByRecorrenteTrueAndUserOrderByDataDesc em vez disso
    @Deprecated
    List<Despesa> findByRecorrenteTrueOrderByDataDesc();

    // @deprecated - Use calcularTotalDespesasPorPeriodo com user em vez disso
    @Deprecated
    @Query("SELECT SUM(d.valor) FROM Despesa d WHERE d.data BETWEEN :inicio AND :fim")
    BigDecimal calcularTotalDespesasPorPeriodo(@Param("inicio") LocalDate inicio,
                                               @Param("fim") LocalDate fim);

    // @deprecated - Use calcularTotalDespesasPorCategoriaEPeriodo com user em vez disso
    @Deprecated
    @Query("SELECT SUM(d.valor) FROM Despesa d WHERE d.categoria = :categoria AND d.data BETWEEN :inicio AND :fim")
    BigDecimal calcularTotalDespesasPorCategoriaEPeriodo(@Param("categoria") String categoria,
                                                         @Param("inicio") LocalDate inicio,
                                                         @Param("fim") LocalDate fim);

    // @deprecated - Use findCategoriasDistintas com user em vez disso
    @Deprecated
    @Query("SELECT DISTINCT d.categoria FROM Despesa d ORDER BY d.categoria")
    List<String> findCategoriasDistintas();
}