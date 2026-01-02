package com.fernando.erp_vendas.repository;

import com.fernando.erp_vendas.model.ItemCompra;
import com.fernando.erp_vendas.model.Produto;
import com.fernando.erp_vendas.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ItemCompraRepository extends JpaRepository<ItemCompra, Long> {

    // ✅ MÉTODOS MULTI-TENANCY - TODOS FILTRADOS POR USER

    // Buscar todos os itens de uma compra específica DO USUÁRIO
    @Query("SELECT i FROM ItemCompra i WHERE i.compra.id = :compraId AND i.compra.user = :user")
    List<ItemCompra> findByCompraIdAndUser(@Param("compraId") Long compraId, @Param("user") User user);

    // Buscar itens por produto (através do produto) DO USUÁRIO
    @Query("SELECT i FROM ItemCompra i WHERE i.produto.id = :produtoId AND i.compra.user = :user")
    List<ItemCompra> findByProdutoIdAndUser(@Param("produtoId") Long produtoId, @Param("user") User user);

    // 🆕 Buscar item por ID e usuário
    @Query("SELECT i FROM ItemCompra i WHERE i.id = :id AND i.compra.user = :user")
    Optional<ItemCompra> findByIdAndUser(@Param("id") Long id, @Param("user") User user);

    // 🆕 Buscar todos os itens do usuário
    @Query("SELECT i FROM ItemCompra i WHERE i.compra.user = :user")
    List<ItemCompra> findByUser(@Param("user") User user);

    // Buscar itens por lote E USUÁRIO
    @Query("SELECT i FROM ItemCompra i WHERE i.lote.id = :loteId AND i.compra.user = :user")
    List<ItemCompra> findByLoteIdAndUser(@Param("loteId") Long loteId, @Param("user") User user);

    // Buscar itens por período E USUÁRIO
    @Query("SELECT i FROM ItemCompra i WHERE i.compra.data BETWEEN :inicio AND :fim AND i.compra.user = :user")
    List<ItemCompra> findByPeriodoAndUser(@Param("inicio") LocalDateTime inicio,
                                          @Param("fim") LocalDateTime fim,
                                          @Param("user") User user);

    // Consultar quantidade total comprada por produto DO USUÁRIO
    @Query("SELECT i.produto.nome, SUM(i.quantidade) FROM ItemCompra i WHERE i.compra.user = :user GROUP BY i.produto.nome")
    List<Object[]> findQuantidadeCompradaPorProduto(@Param("user") User user);

    // Consultar valor total investido por produto DO USUÁRIO
    @Query("SELECT i.produto.nome, SUM(i.custoTotal) FROM ItemCompra i WHERE i.compra.user = :user GROUP BY i.produto.nome")
    List<Object[]> findValorInvestidoPorProduto(@Param("user") User user);

    // Consultar custo médio por produto DO USUÁRIO
    @Query("SELECT i.produto.nome, AVG(i.custoUnitario) FROM ItemCompra i WHERE i.compra.user = :user GROUP BY i.produto.nome")
    List<Object[]> findCustoMedioPorProduto(@Param("user") User user);

    // Contar quantos itens uma compra possui DO USUÁRIO
    @Query("SELECT COUNT(i) FROM ItemCompra i WHERE i.compra.id = :compraId AND i.compra.user = :user")
    Long countByCompraAndUser(@Param("compraId") Long compraId, @Param("user") User user);

    // Verificar se existem itens para um produto específico DO USUÁRIO
    @Query("SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END FROM ItemCompra i WHERE i.produto.id = :produtoId AND i.compra.user = :user")
    boolean existsByProdutoIdAndUser(@Param("produtoId") Long produtoId, @Param("user") User user);

    // Verificar se existem itens para um lote específico DO USUÁRIO
    @Query("SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END FROM ItemCompra i WHERE i.lote.id = :loteId AND i.compra.user = :user")
    boolean existsByLoteIdAndUser(@Param("loteId") Long loteId, @Param("user") User user);

    // 🆕 Contar total de itens do usuário
    @Query("SELECT COUNT(i) FROM ItemCompra i WHERE i.compra.user = :user")
    long countByUser(@Param("user") User user);

    // ✅ MÉTODOS LEGACY (MANTIDOS PARA COMPATIBILIDADE)

    // @deprecated - Use findByCompraIdAndUser em vez disso
    @Deprecated
    List<ItemCompra> findByCompraId(Long compraId);

    // @deprecated - Use findByProdutoIdAndUser em vez disso
    @Deprecated
    List<ItemCompra> findByProdutoId(Long produtoId);

    // @deprecated - Use findByLoteIdAndUser em vez disso
    @Deprecated
    List<ItemCompra> findByLoteId(Long loteId);
}