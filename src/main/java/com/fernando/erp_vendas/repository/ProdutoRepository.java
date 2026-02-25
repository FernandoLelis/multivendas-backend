package com.fernando.erp_vendas.repository;

import com.fernando.erp_vendas.model.Produto;
import com.fernando.erp_vendas.model.User;
import com.fernando.erp_vendas.dto.ProdutoDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    // 🆕 MÉTODOS MULTI-TENANCY - TODOS FILTRADOS POR USER

    boolean existsBySkuAndUser(String sku, User user);

    Optional<Produto> findBySkuAndUser(String sku, User user);

    Optional<Produto> findByAsinAndUser(String asin, User user);

    List<Produto> findByNomeContainingAndUser(String nome, User user);

    @EntityGraph(attributePaths = {"entradaEstoques"})
    List<Produto> findByUser(User user);

    // ✅ QUERY CORRIGIDA: Agora usando ic.custoUnitario para ItemCompra
    // e iv.precoUnitario para ItemVenda, conforme suas classes!
    @Query("SELECT new com.fernando.erp_vendas.dto.ProdutoDTO(" +
            "p.id, p.nome, p.sku, p.asin, p.descricao, " +
            "(SELECT COALESCE(SUM(e.saldo), 0) FROM p.entradaEstoques e), " +
            "p.estoqueMinimo, p.dataCriacao, " +
            "(SELECT COALESCE(SUM(iv.quantidade), 0L) FROM ItemVenda iv WHERE iv.produto = p), " +
            "(SELECT COALESCE(AVG(ic.custoUnitario), 0.0) FROM ItemCompra ic WHERE ic.produto = p), " +
            "(SELECT COALESCE(AVG(iv.precoUnitario), 0.0) FROM ItemVenda iv WHERE iv.produto = p), " +
            "p.peso, p.comprimento, p.largura, p.altura, p.imagemUrl) " + // ✅ NOVOS CAMPOS ADICIONADOS AQUI
            "FROM Produto p WHERE p.user = :user")
    List<ProdutoDTO> findAllWithMetricsByUser(@Param("user") User user);

    @EntityGraph(attributePaths = {"entradaEstoques"})
    Optional<Produto> findByIdAndUser(Long id, User user);

    @Query("SELECT p FROM Produto p WHERE p.user = :user AND " +
            "(SELECT COALESCE(SUM(e.saldo), 0) FROM p.entradaEstoques e) < p.estoqueMinimo")
    List<Produto> findProdutosComEstoqueBaixo(@Param("user") User user);

    @Query("SELECT p FROM Produto p WHERE p.user = :user AND " +
            "(SELECT COALESCE(SUM(e.saldo), 0) FROM p.entradaEstoques e) = 0")
    List<Produto> findProdutosComEstoqueZero(@Param("user") User user);

    long countByUser(User user);

    // ✅ MÉTODOS LEGACY

    @Deprecated
    boolean existsBySku(String sku);

    @Deprecated
    Produto findBySku(String sku);

    @Deprecated
    Produto findByAsin(String asin);

    @Deprecated
    List<Produto> findByNomeContaining(String nome);

    @Deprecated
    @EntityGraph(attributePaths = {"entradaEstoques"})
    List<Produto> findAll();

    @Deprecated
    @EntityGraph(attributePaths = {"entradaEstoques"})
    Optional<Produto> findById(Long id);
}