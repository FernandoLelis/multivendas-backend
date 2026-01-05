package com.fernando.erp_vendas.repository;

import com.fernando.erp_vendas.model.ItemCompra;
import com.fernando.erp_vendas.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemCompraRepository extends JpaRepository<ItemCompra, Long> {

    // ✅ NOVO: Buscar todos os ItemCompra de um usuário
    List<ItemCompra> findByUser(User user);

    // ✅ Buscar ItemCompra por Compra ID e usuário
    @Query("SELECT i FROM ItemCompra i WHERE i.compra.id = :compraId AND i.user = :user")
    List<ItemCompra> findByCompraIdAndUser(@Param("compraId") Long compraId, @Param("user") User user);

    // ✅ Buscar ItemCompra por Produto ID e usuário
    @Query("SELECT i FROM ItemCompra i WHERE i.produto.id = :produtoId AND i.user = :user")
    List<ItemCompra> findByProdutoIdAndUser(@Param("produtoId") Long produtoId, @Param("user") User user);
}