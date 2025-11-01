package com.fernando.erp_vendas.repository;

import com.fernando.erp_vendas.model.Produto;
import com.fernando.erp_vendas.model.User;
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

    // Verifica se já existe um produto com o mesmo SKU para o usuário
    boolean existsBySkuAndUser(String sku, User user);

    // Busca um produto pelo SKU e usuário
    Optional<Produto> findBySkuAndUser(String sku, User user);

    // Buscar produto pelo ASIN e usuário
    Optional<Produto> findByAsinAndUser(String asin, User user);

    // Buscar produtos pelo nome e usuário
    List<Produto> findByNomeContainingAndUser(String nome, User user);

    // 🆕 Buscar TODOS os produtos do usuário com estoques carregados
    @EntityGraph(attributePaths = {"entradaEstoques"})
    List<Produto> findByUser(User user);

    // 🆕 Buscar produto por ID e usuário com estoques carregados
    @EntityGraph(attributePaths = {"entradaEstoques"})
    Optional<Produto> findByIdAndUser(Long id, User user);

    // 🆕 Buscar produtos com estoque baixo (abaixo do mínimo) por usuário
    @Query("SELECT p FROM Produto p WHERE p.user = :user AND " +
            "(SELECT COALESCE(SUM(e.saldo), 0) FROM p.entradaEstoques e) < p.estoqueMinimo")
    List<Produto> findProdutosComEstoqueBaixo(@Param("user") User user);

    // 🆕 Buscar produtos com estoque zero por usuário
    @Query("SELECT p FROM Produto p WHERE p.user = :user AND " +
            "(SELECT COALESCE(SUM(e.saldo), 0) FROM p.entradaEstoques e) = 0")
    List<Produto> findProdutosComEstoqueZero(@Param("user") User user);

    // 🆕 Contar total de produtos do usuário
    long countByUser(User user);

    // ✅ MÉTODOS LEGACY (MANTIDOS PARA COMPATIBILIDADE - USAR COM CAUTELA)

    // @deprecated - Use existsBySkuAndUser em vez disso
    @Deprecated
    boolean existsBySku(String sku);

    // @deprecated - Use findBySkuAndUser em vez disso
    @Deprecated
    Produto findBySku(String sku);

    // @deprecated - Use findByAsinAndUser em vez disso
    @Deprecated
    Produto findByAsin(String asin);

    // @deprecated - Use findByNomeContainingAndUser em vez disso
    @Deprecated
    List<Produto> findByNomeContaining(String nome);

    // @deprecated - Use findByUser em vez disso
    @Deprecated
    @EntityGraph(attributePaths = {"entradaEstoques"})
    List<Produto> findAll();

    // @deprecated - Use findByIdAndUser em vez disso
    @Deprecated
    @EntityGraph(attributePaths = {"entradaEstoques"})
    Optional<Produto> findById(Long id);
}