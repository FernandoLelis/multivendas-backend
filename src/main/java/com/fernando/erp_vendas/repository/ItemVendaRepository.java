package com.fernando.erp_vendas.repository;

import com.fernando.erp_vendas.model.ItemVenda;
import com.fernando.erp_vendas.model.User;
import com.fernando.erp_vendas.model.Venda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ItemVendaRepository extends JpaRepository<ItemVenda, Long> {

    // ✅ MÉTODO EXISTENTE: Buscar itens por venda e usuário (todos os itens)
    List<ItemVenda> findByVendaAndUser(Venda venda, User user);

    // ✅ NOVO MÉTODO v46.8.2: Buscar apenas itens com produto não nulo
    @Query("SELECT i FROM ItemVenda i WHERE i.venda = :venda AND i.user = :user AND i.produto IS NOT NULL")
    List<ItemVenda> findByVendaAndUserAndProdutoNotNull(@Param("venda") Venda venda, @Param("user") User user);

    // ✅ MÉTODO EXISTENTE: Buscar itens por usuário
    List<ItemVenda> findByUser(User user);

    // ✅ MÉTODO: Contar itens por venda
    @Query("SELECT COUNT(i) FROM ItemVenda i WHERE i.venda = :venda")
    Long countByVenda(@Param("venda") Venda venda);

    // ✅ NOVO MÉTODO: Buscar itens por produto
    @Query("SELECT i FROM ItemVenda i WHERE i.produto.id = :produtoId AND i.user = :user")
    List<ItemVenda> findByProdutoIdAndUser(@Param("produtoId") Long produtoId, @Param("user") User user);

    // ✅ NOVO MÉTODO: Buscar itens por lote
    @Query("SELECT i FROM ItemVenda i WHERE i.lote.id = :loteId AND i.user = :user")
    List<ItemVenda> findByLoteIdAndUser(@Param("loteId") Long loteId, @Param("user") User user);

    // ✅ NOVO MÉTODO: Buscar itens processados pelo PEPS
    @Query("SELECT i FROM ItemVenda i WHERE i.processadoPeps = true AND i.user = :user")
    List<ItemVenda> findProcessadosByUser(@Param("user") User user);

    // ✅ NOVO MÉTODO: Buscar itens NÃO processados pelo PEPS
    @Query("SELECT i FROM ItemVenda i WHERE i.processadoPeps = false AND i.user = :user")
    List<ItemVenda> findNaoProcessadosByUser(@Param("user") User user);

    // ✅ MÉTODO EXISTENTE: Verificar se existe item com lote específico
    boolean existsByLoteIdAndUser(Long loteId, User user);

    // ✅ NOVO MÉTODO: Buscar itens por venda ID
    @Query("SELECT i FROM ItemVenda i WHERE i.venda.id = :vendaId AND i.user = :user AND i.produto IS NOT NULL")
    List<ItemVenda> findByVendaIdAndUser(@Param("vendaId") Long vendaId, @Param("user") User user);

    // ✅ NOVO MÉTODO: Calcular quantidade total vendida de um produto
    @Query("SELECT COALESCE(SUM(i.quantidade), 0) FROM ItemVenda i WHERE i.produto.id = :produtoId AND i.user = :user")
    Integer calcularQuantidadeVendidaPorProduto(@Param("produtoId") Long produtoId, @Param("user") User user);

    // ✅ NOVO MÉTODO: Calcular custo total de vendas por produto
    @Query("SELECT COALESCE(SUM(i.custoUnitario * i.quantidade), 0) FROM ItemVenda i WHERE i.produto.id = :produtoId AND i.user = :user")
    Double calcularCustoTotalPorProduto(@Param("produtoId") Long produtoId, @Param("user") User user);

    // ✅ NOVO MÉTODO: Calcular faturamento total por produto
    @Query("SELECT COALESCE(SUM(i.precoUnitario * i.quantidade), 0) FROM ItemVenda i WHERE i.produto.id = :produtoId AND i.user = :user")
    Double calcularFaturamentoTotalPorProduto(@Param("produtoId") Long produtoId, @Param("user") User user);

    // ✅ MÉTODO EXISTENTE: Deletar itens por venda
    void deleteByVenda(Venda venda);

    // ✅ NOVO MÉTODO: Deletar itens por produto
    void deleteByProdutoIdAndUser(Long produtoId, User user);

    // ✅ NOVO MÉTODO v46.8.2: Deletar itens com produto null para um usuário específico
    @Modifying
    @Transactional
    @Query("DELETE FROM ItemVenda i WHERE i.produto IS NULL AND i.user = :user")
    int deleteByProdutoIsNullAndUser(@Param("user") User user);

    // ✅ NOVO MÉTODO: Buscar itens com produto null (para limpeza)
    @Query("SELECT i FROM ItemVenda i WHERE i.produto IS NULL AND i.user = :user")
    List<ItemVenda> findByProdutoIsNullAndUser(@Param("user") User user);

    // ✅ NOVO MÉTODO: Verificar se venda tem itens
    @Query("SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END FROM ItemVenda i WHERE i.venda = :venda AND i.user = :user")
    boolean existsByVendaAndUser(@Param("venda") Venda venda, @Param("user") User user);

    // 🚀🚀🚀 NOVO MÉTODO PARA CORREÇÃO DO ESTOQUE SERVICE 🚀🚀🚀
    @Modifying
    @Transactional
    @Query("DELETE FROM ItemVenda i WHERE i.venda.id = :vendaId AND i.user = :user")
    void deleteAllByVendaIdAndUser(@Param("vendaId") Long vendaId, @Param("user") User user);
}