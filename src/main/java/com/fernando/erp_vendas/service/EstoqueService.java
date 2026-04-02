package com.fernando.erp_vendas.service;

import com.fernando.erp_vendas.model.*;
import com.fernando.erp_vendas.repository.EntradaEstoqueRepository;
import com.fernando.erp_vendas.repository.VendaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class EstoqueService {

    @Autowired
    private EntradaEstoqueRepository entradaEstoqueRepository;

    @Autowired
    private VendaRepository vendaRepository;

    @Transactional(readOnly = true)
    public boolean verificarEstoqueSuficiente(Produto produto, Integer quantidadeNecessaria) {
        Integer saldoTotal = entradaEstoqueRepository.findSaldoTotalByProdutoAndUser(produto, produto.getUser());
        return saldoTotal != null && saldoTotal >= quantidadeNecessaria;
    }

    @Transactional(readOnly = true)
    public Integer verificarSaldoTotal(Produto produto) {
        Integer saldo = entradaEstoqueRepository.findSaldoTotalByProdutoAndUser(produto, produto.getUser());
        return saldo != null ? saldo : 0;
    }

    @Transactional(readOnly = true)
    public BigDecimal calcularCustoVenda(Produto produto, Integer quantidade) {
        List<EntradaEstoque> lotes = entradaEstoqueRepository
                .findByProdutoAndUserAndSaldoGreaterThanOrderByDataEntradaAscIdAsc(
                        produto, produto.getUser(), 0
                );

        BigDecimal custoTotal = BigDecimal.ZERO;
        int qtdRestante = quantidade;

        for (EntradaEstoque lote : lotes) {
            if (qtdRestante <= 0) break;
            int qtdParaCalculo = Math.min(qtdRestante, lote.getSaldo());
            BigDecimal custoParcial = lote.getCustoUnitario().multiply(new BigDecimal(qtdParaCalculo));
            custoTotal = custoTotal.add(custoParcial);
            qtdRestante -= qtdParaCalculo;
        }
        return custoTotal;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void reverterEstoqueVenda(Venda venda) {
        if (venda.getItens() == null || venda.getItens().isEmpty()) return;

        System.out.println("🔄 [ESTOQUE] Revertendo estoque para venda: " + venda.getIdPedido());

        for (ItemVenda item : venda.getItens()) {
            if (item.getLote() != null && item.getQuantidade() > 0) {
                EntradaEstoque lote = item.getLote();
                lote.setSaldo(lote.getSaldo() + item.getQuantidade());
                entradaEstoqueRepository.save(lote);
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void reverterEstoqueExclusaoVenda(Venda venda) {
        // Se a venda está ativa, devolve o estoque (comportamento original)
        if (venda.getStatus() == Venda.StatusVenda.ATIVA) {
            reverterEstoqueVenda(venda);
        } else if (venda.getStatus() == Venda.StatusVenda.CANCELADA) {
            // Se cancelada e NÃO retornou ao estoque, devolve o estoque (pois o cancelamento deu baixa)
            if (venda.getRetornouEstoque() == null || !venda.getRetornouEstoque()) {
                reverterEstoqueVenda(venda);
            }
            // Se cancelada e retornou ao estoque, não faz nada porque o estoque já foi revertido no cancelamento
        }
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void processarVendaComPeps(Venda venda) {
        System.out.println("🔄 [ESTOQUE] PEPS iniciado para venda: " + venda.getIdPedido());

        List<ItemVenda> itensDesejados = new ArrayList<>(venda.getItens());
        venda.getItens().clear();
        vendaRepository.saveAndFlush(venda);

        List<ItemVenda> novosItensDefinitivos = new ArrayList<>();
        double custoTotalVenda = 0.0;
        User user = venda.getUser();

        for (ItemVenda rascunho : itensDesejados) {
            Produto produto = rascunho.getProduto();
            int qtdFaltante = rascunho.getQuantidade();

            List<EntradaEstoque> lotes = entradaEstoqueRepository
                    .findByProdutoAndUserAndSaldoGreaterThanOrderByDataEntradaAscIdAsc(produto, user, 0);

            for (EntradaEstoque lote : lotes) {
                if (qtdFaltante <= 0) break;

                int qtdDisponivelNoLote = lote.getSaldo();
                int qtdParaConsumir = Math.min(qtdFaltante, qtdDisponivelNoLote);

                lote.setSaldo(qtdDisponivelNoLote - qtdParaConsumir);
                entradaEstoqueRepository.save(lote);

                ItemVenda novoItem = new ItemVenda();
                novoItem.setVenda(venda);
                novoItem.setProduto(produto);
                novoItem.setLote(lote);
                novoItem.setQuantidade(qtdParaConsumir);
                novoItem.setPrecoUnitario(rascunho.getPrecoUnitario());
                novoItem.setCustoUnitario(lote.getCustoUnitario());
                novoItem.setUser(user);
                novoItem.setProcessadoPeps(true);

                novosItensDefinitivos.add(novoItem);
                custoTotalVenda += (lote.getCustoUnitario().doubleValue() * qtdParaConsumir);
                qtdFaltante -= qtdParaConsumir;
            }

            if (qtdFaltante > 0) {
                throw new RuntimeException("Estoque insuficiente para: " + produto.getNome());
            }
        }

        venda.setItens(novosItensDefinitivos);
        venda.setCustoProdutoVendido(custoTotalVenda);
        vendaRepository.save(venda);
        System.out.println("✅ [ESTOQUE] PEPS concluído.");
    }

    // ========== NOVO MÉTODO DE CANCELAMENTO ==========
    @Transactional(propagation = Propagation.REQUIRED)
    public void cancelarVenda(Venda venda, String motivo, Double custoRetorno, Boolean retornarEstoque) {
        if (venda.getStatus() == Venda.StatusVenda.CANCELADA) {
            throw new RuntimeException("Venda já está cancelada.");
        }

        if (retornarEstoque) {
            reverterEstoqueVenda(venda);
        }

        venda.setStatus(Venda.StatusVenda.CANCELADA);
        venda.setMotivoCancelamento(motivo);
        venda.setCustoRetorno(custoRetorno);
        venda.setRetornouEstoque(retornarEstoque);

        vendaRepository.save(venda);
    }
}