package com.fernando.erp_vendas.repository;

import com.fernando.erp_vendas.model.Compra;
import com.fernando.erp_vendas.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CompraRepository extends JpaRepository<Compra, Long> {

    // ✅ MÉTODOS MULTI-TENANCY - TODOS FILTRADOS POR USER

    // Buscar TODAS as compras do usuário
    List<Compra> findByUser(User user);

    // Buscar compras do usuário ordenadas por data (mais recente primeiro)
    List<Compra> findByUserOrderByDataDesc(User user);

    // Buscar compra por ID e usuário
    Optional<Compra> findByIdAndUser(Long id, User user);

    // Buscar compra por ID do pedido e usuário
    Optional<Compra> findByIdPedidoCompraAndUser(String idPedidoCompra, User user);

    // Buscar compras por fornecedor (contém) e usuário
    List<Compra> findByFornecedorContainingAndUser(String fornecedor, User user);

    // Buscar compras por período E USUÁRIO
    List<Compra> findByDataBetweenAndUser(LocalDateTime inicio, LocalDateTime fim, User user);

    // 🆕 Contar total de compras do usuário
    long countByUser(User user);

    // 🆕 Buscar compras recentes do usuário (últimas 10)
    List<Compra> findTop10ByUserOrderByDataDesc(User user);

    // Buscar compras que contenham um produto específico (através dos itens)
    @Query("SELECT DISTINCT c FROM Compra c JOIN c.itens i WHERE i.produto.id = :produtoId AND c.user = :user")
    List<Compra> findByProdutoIdInItens(@Param("produtoId") Long produtoId, @Param("user") User user);

    // ✅ CONSULTAS SIMPLES (SEM totalCompra)

    // Consultar fornecedores mais usados DO USUÁRIO
    @Query("SELECT c.fornecedor, COUNT(c) FROM Compra c WHERE c.user = :user GROUP BY c.fornecedor ORDER BY COUNT(c) DESC")
    List<Object[]> findFornecedoresMaisUsados(@Param("user") User user);

    // ✅ MÉTODOS LEGACY (MANTIDOS PARA COMPATIBILIDADE - USAR COM CAUTELA)

    // @deprecated - Use findByIdAndUser em vez disso
    @Deprecated
    @Override
    Optional<Compra> findById(Long id);

    // @deprecated - Use findByIdPedidoCompraAndUser em vez disso
    @Deprecated
    Optional<Compra> findByIdPedidoCompra(String idPedidoCompra);

    // @deprecated - Use findByFornecedorContainingAndUser em vez disso
    @Deprecated
    List<Compra> findByFornecedorContaining(String fornecedor);

    // @deprecated - Use findByDataBetweenAndUser em vez disso
    @Deprecated
    List<Compra> findByDataBetween(LocalDateTime inicio, LocalDateTime fim);
}