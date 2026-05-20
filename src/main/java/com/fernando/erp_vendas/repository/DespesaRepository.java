package com.fernando.erp_vendas.repository;

import com.fernando.erp_vendas.model.Despesa;
import com.fernando.erp_vendas.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DespesaRepository extends JpaRepository<Despesa, Long> {

    // 🆕 MÉTODOS MULTI-TENANCY - TODOS FILTRADOS POR USER

    List<Despesa> findByUserOrderByDataDesc(User user);
    List<Despesa> findByCategoriaAndUserOrderByDataDesc(String categoria, User user);
    List<Despesa> findByDataBetweenAndUserOrderByDataDesc(LocalDate inicio, LocalDate fim, User user);
    List<Despesa> findByRecorrenteTrueAndUserOrderByDataDesc(User user);
    Optional<Despesa> findByIdAndUser(Long id, User user);
    List<Despesa> findByDescricaoContainingAndUser(String descricao, User user);
    long countByUser(User user);

    @Query("SELECT d FROM Despesa d WHERE d.user = :user AND YEAR(d.data) = YEAR(CURRENT_DATE) AND MONTH(d.data) = MONTH(CURRENT_DATE) ORDER BY d.data DESC")
    List<Despesa> findDespesasDoMesAtual(@Param("user") User user);

    // ✅ NOVO: Calcular total de despesas com datas dinâmicas (pro-rata)
    @Query("SELECT SUM(d.valor) FROM Despesa d WHERE d.user = :user AND d.data >= :inicio AND d.data <= :fim")
    BigDecimal calcularTotalDespesasPorPeriodoDinamico(@Param("user") User user,
                                                       @Param("inicio") LocalDate inicio,
                                                       @Param("fim") LocalDate fim);

    // Métodos existentes (mantidos para compatibilidade)
    @Query("SELECT SUM(d.valor) FROM Despesa d WHERE d.user = :user AND d.data BETWEEN :inicio AND :fim")
    BigDecimal calcularTotalDespesasPorPeriodo(@Param("user") User user,
                                               @Param("inicio") LocalDate inicio,
                                               @Param("fim") LocalDate fim);

    @Query("SELECT SUM(d.valor) FROM Despesa d WHERE d.user = :user AND d.categoria = :categoria AND d.data BETWEEN :inicio AND :fim")
    BigDecimal calcularTotalDespesasPorCategoriaEPeriodo(@Param("user") User user,
                                                         @Param("categoria") String categoria,
                                                         @Param("inicio") LocalDate inicio,
                                                         @Param("fim") LocalDate fim);

    @Query("SELECT SUM(d.valor) FROM Despesa d WHERE d.user = :user AND YEAR(d.data) = YEAR(CURRENT_DATE) AND MONTH(d.data) = MONTH(CURRENT_DATE)")
    BigDecimal calcularTotalDespesasMesAtual(@Param("user") User user);

    @Query("SELECT SUM(d.valor) FROM Despesa d WHERE d.user = :user AND YEAR(d.data) = YEAR(CURRENT_DATE)")
    BigDecimal calcularTotalDespesasAnoAtual(@Param("user") User user);

    @Query("SELECT AVG(d.valor) FROM Despesa d WHERE d.user = :user AND d.data >= :inicio")
    BigDecimal calcularMediaMensalDespesas(@Param("user") User user, @Param("inicio") LocalDate inicio);

    @Query("SELECT DISTINCT d.categoria FROM Despesa d WHERE d.user = :user ORDER BY d.categoria")
    List<String> findCategoriasDistintas(@Param("user") User user);

    @Query("SELECT d.categoria, SUM(d.valor) FROM Despesa d WHERE d.user = :user GROUP BY d.categoria ORDER BY SUM(d.valor) DESC LIMIT 5")
    List<Object[]> findTopCategoriasComMaiorGasto(@Param("user") User user);

    @Query("SELECT d.categoria, SUM(d.valor) FROM Despesa d WHERE d.user = :user AND d.recorrente = true GROUP BY d.categoria")
    List<Object[]> findDespesasRecorrentesTotais(@Param("user") User user);

    // ✅ MÉTODOS LEGACY (MANTIDOS PARA COMPATIBILIDADE)
    @Deprecated
    List<Despesa> findAllByOrderByDataDesc();
    @Deprecated
    List<Despesa> findByCategoriaOrderByDataDesc(String categoria);
    @Deprecated
    List<Despesa> findByDataBetweenOrderByDataDesc(LocalDate inicio, LocalDate fim);
    @Deprecated
    List<Despesa> findByRecorrenteTrueOrderByDataDesc();
    @Deprecated
    @Query("SELECT SUM(d.valor) FROM Despesa d WHERE d.data BETWEEN :inicio AND :fim")
    BigDecimal calcularTotalDespesasPorPeriodo(@Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);
    @Deprecated
    @Query("SELECT SUM(d.valor) FROM Despesa d WHERE d.categoria = :categoria AND d.data BETWEEN :inicio AND :fim")
    BigDecimal calcularTotalDespesasPorCategoriaEPeriodo(@Param("categoria") String categoria,
                                                         @Param("inicio") LocalDate inicio,
                                                         @Param("fim") LocalDate fim);
    @Deprecated
    @Query("SELECT DISTINCT d.categoria FROM Despesa d ORDER BY d.categoria")
    List<String> findCategoriasDistintas();
}