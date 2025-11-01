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

    // 🆕 MÉTODOS MULTI-TENANCY - TODOS FILTRADOS POR USER

    // Encontra todas as entradas de estoque de um produto do usuário que ainda têm saldo
    List<EntradaEstoque> findByProdutoAndUserAndSaldoGreaterThanOrderByDataEntradaAsc(
            Produto produto, User user, Integer saldo);

    // Encontra todas as entradas de um produto do usuário
    List<EntradaEstoque> findByProdutoAndUserOrderByDataEntradaAsc(Produto produto, User user);

    // 🆕 Buscar entrada por ID e usuário
    Optional<EntradaEstoque> findByIdAndUser(Long id, User user);

    // 🆕 Buscar entrada por ID do pedido de compra e usuário
    Optional<EntradaEstoque> findByIdPedidoCompraAndUser(String idPedidoCompra, User user);

    // 🆕 Listar todas as entradas do usuário ordenadas por data (mais recentes primeiro)
    List<EntradaEstoque> findByUserOrderByDataEntradaDesc(User user);

    // 🆕 Soma o saldo total de um produto DO USUÁRIO
    @Query("SELECT COALESCE(SUM(e.saldo), 0) FROM EntradaEstoque e WHERE e.produto = :produto AND e.user = :user")
    Integer findSaldoTotalByProdutoAndUser(@Param("produto") Produto produto, @Param("user") User user);

    // 🆕 Buscar entradas por categoria do usuário
    List<EntradaEstoque> findByCategoriaAndUser(String categoria, User user);

    // 🆕 Buscar entradas por fornecedor do usuário
    List<EntradaEstoque> findByFornecedorContainingAndUser(String fornecedor, User user);

    // 🆕 Contar total de entradas do usuário
    long countByUser(User user);

    // 🆕 Buscar entradas com saldo baixo (menos de 5 unidades) do usuário
    @Query("SELECT e FROM EntradaEstoque e WHERE e.user = :user AND e.saldo > 0 AND e.saldo < 5")
    List<EntradaEstoque> findEntradasComSaldoBaixo(@Param("user") User user);

    // ✅ MÉTODOS LEGACY (MANTIDOS PARA COMPATIBILIDADE - USAR COM CAUTELA)

    // @deprecated - Use findByProdutoAndUserAndSaldoGreaterThanOrderByDataEntradaAsc em vez disso
    @Deprecated
    List<EntradaEstoque> findByProdutoAndSaldoGreaterThanOrderByDataEntradaAsc(Produto produto, Integer saldo);

    // @deprecated - Use findByProdutoAndUserOrderByDataEntradaAsc em vez disso
    @Deprecated
    List<EntradaEstoque> findByProdutoOrderByDataEntradaAsc(Produto produto);

    // @deprecated - Use findByUserOrderByDataEntradaDesc em vez disso
    @Deprecated
    List<EntradaEstoque> findAllByOrderByDataEntradaDesc();

    // @deprecated - Use findSaldoTotalByProdutoAndUser em vez disso
    @Deprecated
    @Query("SELECT COALESCE(SUM(e.saldo), 0) FROM EntradaEstoque e WHERE e.produto = :produto")
    Integer findSaldoTotalByProduto(@Param("produto") Produto produto);
}