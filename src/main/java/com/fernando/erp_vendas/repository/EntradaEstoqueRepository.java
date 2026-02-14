package com.fernando.erp_vendas.repository;

import com.fernando.erp_vendas.model.EntradaEstoque;
import com.fernando.erp_vendas.model.Produto;
import com.fernando.erp_vendas.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EntradaEstoqueRepository extends JpaRepository<EntradaEstoque, Long> {

    // ✅ CORREÇÃO: Adicionado 'IdAsc' para desempatar lotes com a mesma Data/Hora
    List<EntradaEstoque> findByProdutoAndUserAndSaldoGreaterThanOrderByDataEntradaAscIdAsc(
            Produto produto, User user, Integer saldo);

    // --- MÉTODOS EXISTENTES (Mantenha o resto igual) ---

    List<EntradaEstoque> findByProdutoAndUserOrderByDataEntradaAsc(Produto produto, User user);
    Optional<EntradaEstoque> findByIdAndUser(Long id, User user);
    Optional<EntradaEstoque> findByIdPedidoCompraAndUser(String idPedidoCompra, User user);
    List<EntradaEstoque> findByUserOrderByDataEntradaDesc(User user);

    @Query("SELECT COALESCE(SUM(e.saldo), 0) FROM EntradaEstoque e WHERE e.produto = :produto AND e.user = :user")
    Integer findSaldoTotalByProdutoAndUser(@Param("produto") Produto produto, @Param("user") User user);

    List<EntradaEstoque> findByCategoriaAndUser(String categoria, User user);
    List<EntradaEstoque> findByFornecedorContainingAndUser(String fornecedor, User user);
    long countByUser(User user);

    @Query("SELECT e FROM EntradaEstoque e WHERE e.user = :user AND e.saldo > 0 AND e.saldo < 5")
    List<EntradaEstoque> findEntradasComSaldoBaixo(@Param("user") User user);

    // Métodos Deprecated mantidos para compatibilidade...
    @Deprecated
    List<EntradaEstoque> findByProdutoAndSaldoGreaterThanOrderByDataEntradaAsc(Produto produto, Integer saldo);
    @Deprecated
    List<EntradaEstoque> findByProdutoOrderByDataEntradaAsc(Produto produto);
    @Deprecated
    List<EntradaEstoque> findAllByOrderByDataEntradaDesc();
    @Deprecated
    @Query("SELECT COALESCE(SUM(e.saldo), 0) FROM EntradaEstoque e WHERE e.produto = :produto")
    Integer findSaldoTotalByProduto(@Param("produto") Produto produto);
}