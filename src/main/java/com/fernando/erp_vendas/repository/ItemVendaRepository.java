package com.fernando.erp_vendas.repository;

import com.fernando.erp_vendas.model.ItemVenda;
import com.fernando.erp_vendas.model.User;
import com.fernando.erp_vendas.model.Venda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ItemVendaRepository extends JpaRepository<ItemVenda, Long> {

    // 🆕 MÉTODOS MULTI-TENANCY - TODOS FILTRADOS POR USER

    // Encontrar todos os itens de uma venda específica DO USUÁRIO
    List<ItemVenda> findByVendaAndUser(Venda venda, User user);

    // Encontrar itens por lote E USUÁRIO (útil para auditoria)
    List<ItemVenda> findByLoteIdAndUser(Long loteId, User user);

    // 🆕 Buscar item por ID e usuário
    Optional<ItemVenda> findByIdAndUser(Long id, User user);

    // 🆕 Buscar todos os itens do usuário
    List<ItemVenda> findByUser(User user);

    // 🆕 Buscar itens por produto (através do lote) DO USUÁRIO
    @Query("SELECT iv FROM ItemVenda iv WHERE iv.lote.produto.id = :produtoId AND iv.user = :user")
    List<ItemVenda> findByProdutoIdAndUser(@Param("produtoId") Long produtoId, @Param("user") User user);

    // 🆕 Buscar itens por período E USUÁRIO
    @Query("SELECT iv FROM ItemVenda iv WHERE iv.venda.data BETWEEN :inicio AND :fim AND iv.user = :user")
    List<ItemVenda> findByPeriodoAndUser(@Param("inicio") java.time.LocalDateTime inicio,
                                         @Param("fim") java.time.LocalDateTime fim,
                                         @Param("user") User user);

    // 🆕 Consultar custo total dos itens por venda DO USUÁRIO
    @Query("SELECT SUM(iv.custoUnitario * iv.quantidade) FROM ItemVenda iv WHERE iv.venda = :venda AND iv.user = :user")
    Double findCustoTotalByVendaAndUser(@Param("venda") Venda venda, @Param("user") User user);

    // 🆕 Consultar quantidade total vendida por produto DO USUÁRIO
    @Query("SELECT iv.lote.produto.nome, SUM(iv.quantidade) FROM ItemVenda iv WHERE iv.user = :user GROUP BY iv.lote.produto.nome")
    List<Object[]> findQuantidadeVendidaPorProduto(@Param("user") User user);

    // Contar quantos itens uma venda possui DO USUÁRIO
    Long countByVendaAndUser(Venda venda, User user);

    // Verificar se existem itens para um lote específico DO USUÁRIO
    boolean existsByLoteIdAndUser(Long loteId, User user);

    // 🆕 Contar total de itens do usuário
    long countByUser(User user);

    // ✅ MÉTODOS LEGACY (MANTIDOS PARA COMPATIBILIDADE - USAR COM CAUTELA)

    // @deprecated - Use findByVendaAndUser em vez disso
    @Deprecated
    List<ItemVenda> findByVenda(Venda venda);

    // @deprecated - Use findByLoteIdAndUser em vez disso
    @Deprecated
    List<ItemVenda> findByLoteId(Long loteId);

    // @deprecated - Use countByVendaAndUser em vez disso
    @Deprecated
    Long countByVenda(Venda venda);

    // @deprecated - Use existsByLoteIdAndUser em vez disso
    @Deprecated
    boolean existsByLoteId(Long loteId);
}