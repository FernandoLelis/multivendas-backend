package com.fernando.erp_vendas.service;

import com.fernando.erp_vendas.model.EntradaEstoque;
import com.fernando.erp_vendas.model.ItemVenda;
import com.fernando.erp_vendas.model.Produto;
import com.fernando.erp_vendas.model.User;
import com.fernando.erp_vendas.model.Venda;
import com.fernando.erp_vendas.repository.EntradaEstoqueRepository;
import com.fernando.erp_vendas.repository.ItemVendaRepository;
import com.fernando.erp_vendas.repository.VendaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class EstoqueService {

    @Autowired
    private EntradaEstoqueRepository entradaEstoqueRepository;

    @Autowired
    private ItemVendaRepository itemVendaRepository;

    @Autowired
    private VendaRepository vendaRepository;

    // 🆕 MÉTODO PARA OBTER USUÁRIO LOGADO
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            return (User) authentication.getPrincipal();
        }
        throw new RuntimeException("Usuário não autenticado");
    }

    // 🆕 LÓGICA PEPS: Busca o lote mais antigo que ainda tem saldo PARA O USUÁRIO LOGADO
    public EntradaEstoque encontrarLoteParaVenda(Produto produto, Integer quantidade) {
        User currentUser = getCurrentUser();
        List<EntradaEstoque> lotesComSaldo = entradaEstoqueRepository
                .findByProdutoAndUserAndSaldoGreaterThanOrderByDataEntradaAsc(produto, currentUser, 0);

        if (lotesComSaldo.isEmpty()) {
            throw new RuntimeException("Produto sem estoque: " + produto.getNome());
        }

        // PEPS: Retorna o lote mais antigo (primeiro da lista)
        return lotesComSaldo.get(0);
    }

    // ✅ CORRIGIDO: Calcula o custo total E registra os itens da venda COM MULTI-TENANCY
    @Transactional
    public BigDecimal calcularCustoVendaERegistrarItens(Venda venda) {
        User currentUser = getCurrentUser();

        // 🆕 VERIFICAR SE O PRODUTO PERTENCE AO USUÁRIO
        if (!venda.getProduto().getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Produto não pertence ao usuário logado");
        }

        // 1️⃣ PRIMEIRO: Salvar a Venda para gerar ID
        Venda vendaSalva = vendaRepository.save(venda);

        BigDecimal custoTotal = BigDecimal.ZERO;
        Integer quantidadeRestante = venda.getQuantidade();
        Produto produto = venda.getProduto();

        List<EntradaEstoque> lotesComSaldo = entradaEstoqueRepository
                .findByProdutoAndUserAndSaldoGreaterThanOrderByDataEntradaAsc(produto, currentUser, 0);

        List<ItemVenda> itensVenda = new ArrayList<>();

        for (EntradaEstoque lote : lotesComSaldo) {
            if (quantidadeRestante <= 0) break;

            Integer quantidadeUsada = Math.min(quantidadeRestante, lote.getSaldo());
            BigDecimal custoLote = lote.getCustoUnitario().multiply(BigDecimal.valueOf(quantidadeUsada));

            // ✅ CORRIGIDO: Usar vendaSalva (já persistida) em vez de venda (transient)
            // 🆕 ADICIONAR USUÁRIO AO ITEM VENDA
            ItemVenda itemVenda = new ItemVenda(vendaSalva, lote, quantidadeUsada, lote.getCustoUnitario(), currentUser);
            itensVenda.add(itemVenda);

            // Baixar estoque do lote
            lote.setSaldo(lote.getSaldo() - quantidadeUsada);
            entradaEstoqueRepository.save(lote);

            custoTotal = custoTotal.add(custoLote);
            quantidadeRestante -= quantidadeUsada;
        }

        if (quantidadeRestante > 0) {
            throw new RuntimeException("Estoque insuficiente para o produto: " + produto.getNome());
        }

        // ✅ SALVAR todos os itens da venda
        itemVendaRepository.saveAll(itensVenda);

        // ✅ CORRIGIDO: Converter BigDecimal para double
        vendaSalva.setCustoProdutoVendido(custoTotal.doubleValue());
        vendaRepository.save(vendaSalva);

        return custoTotal;
    }

    // ✅ ATUALIZADO: Baixa estoque usando PEPS E rastreia os itens COM MULTI-TENANCY
    @Transactional
    public void baixarEstoque(Venda venda) {
        User currentUser = getCurrentUser();
        Integer quantidadeRestante = venda.getQuantidade();
        Produto produto = venda.getProduto();

        List<EntradaEstoque> lotesComSaldo = entradaEstoqueRepository
                .findByProdutoAndUserAndSaldoGreaterThanOrderByDataEntradaAsc(produto, currentUser, 0);

        for (EntradaEstoque lote : lotesComSaldo) {
            if (quantidadeRestante <= 0) break;

            Integer quantidadeBaixa = Math.min(quantidadeRestante, lote.getSaldo());
            lote.setSaldo(lote.getSaldo() - quantidadeBaixa);
            entradaEstoqueRepository.save(lote);

            quantidadeRestante -= quantidadeBaixa;
        }

        if (quantidadeRestante > 0) {
            throw new RuntimeException("Erro ao baixar estoque. Estoque insuficiente.");
        }
    }

    // ✅ CORRIGIDO: Reverter estoque baseado nos itens rastreados da venda COM MULTI-TENANCY
    @Transactional
    public void reverterEstoqueVenda(Venda venda) {
        User currentUser = getCurrentUser();

        // 🆕 VERIFICAR SE A VENDA PERTENCE AO USUÁRIO
        if (!venda.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Venda não pertence ao usuário logado");
        }

        // Buscar todos os itens da venda
        List<ItemVenda> itensVenda = itemVendaRepository.findByVendaAndUser(venda, currentUser);

        if (itensVenda.isEmpty()) {
            throw new RuntimeException("Nenhum item encontrado para a venda: " + venda.getIdPedido());
        }

        // Reverter estoque para cada lote usado
        for (ItemVenda item : itensVenda) {
            // ✅ CORRIGIDO: Usar getLote() em vez de getEntradaEstoque()
            EntradaEstoque lote = item.getLote();
            Integer quantidadeReverter = item.getQuantidade();

            // Incrementar o saldo do lote original
            lote.setSaldo(lote.getSaldo() + quantidadeReverter);
            entradaEstoqueRepository.save(lote);

            System.out.println("✅ Estoque revertido: " + quantidadeReverter +
                    " unidades devolvidas ao lote " + lote.getId() +
                    " (Custo: " + item.getCustoUnitario() + ")");
        }

        // ✅ NOVO: Excluir os itens da venda após reverter estoque
        itemVendaRepository.deleteAll(itensVenda);

        System.out.println("✅ Reversão completa: " + itensVenda.size() +
                " itens revertidos para a venda " + venda.getIdPedido());
    }

    // ✅ MANTIDO: Método antigo para compatibilidade (será depreciado) COM MULTI-TENANCY
    @Transactional
    public void baixarEstoque(Produto produto, Integer quantidade) {
        User currentUser = getCurrentUser();
        Integer quantidadeRestante = quantidade;

        List<EntradaEstoque> lotesComSaldo = entradaEstoqueRepository
                .findByProdutoAndUserAndSaldoGreaterThanOrderByDataEntradaAsc(produto, currentUser, 0);

        for (EntradaEstoque lote : lotesComSaldo) {
            if (quantidadeRestante <= 0) break;

            Integer quantidadeBaixa = Math.min(quantidadeRestante, lote.getSaldo());
            lote.setSaldo(lote.getSaldo() - quantidadeBaixa);
            entradaEstoqueRepository.save(lote);

            quantidadeRestante -= quantidadeBaixa;
        }

        if (quantidadeRestante > 0) {
            throw new RuntimeException("Erro ao baixar estoque. Estoque insuficiente.");
        }
    }

    // ✅ MANTIDO: Calcula o custo total para uma venda (sem registrar itens) COM MULTI-TENANCY
    public BigDecimal calcularCustoVenda(Produto produto, Integer quantidadeVenda) {
        User currentUser = getCurrentUser();
        BigDecimal custoTotal = BigDecimal.ZERO;
        Integer quantidadeRestante = quantidadeVenda;

        List<EntradaEstoque> lotesComSaldo = entradaEstoqueRepository
                .findByProdutoAndUserAndSaldoGreaterThanOrderByDataEntradaAsc(produto, currentUser, 0);

        for (EntradaEstoque lote : lotesComSaldo) {
            if (quantidadeRestante <= 0) break;

            Integer quantidadeUsada = Math.min(quantidadeRestante, lote.getSaldo());
            BigDecimal custoLote = lote.getCustoUnitario().multiply(BigDecimal.valueOf(quantidadeUsada));

            custoTotal = custoTotal.add(custoLote);
            quantidadeRestante -= quantidadeUsada;
        }

        if (quantidadeRestante > 0) {
            throw new RuntimeException("Estoque insuficiente para o produto: " + produto.getNome());
        }

        return custoTotal;
    }

    // ✅ CORREÇÃO CRÍTICA: Verifica saldo total de um produto PARA O USUÁRIO LOGADO
    public Integer verificarSaldoTotal(Produto produto) {
        User currentUser = getCurrentUser();

        try {
            // ✅ PRIMEIRO: Tentar usar o método do repositório
            Integer saldo = entradaEstoqueRepository.findSaldoTotalByProdutoAndUser(produto, currentUser);
            if (saldo != null) {
                System.out.println("📦 Saldo via repositório: " + saldo + " para produto: " + produto.getNome());
                return saldo;
            }
        } catch (Exception e) {
            System.out.println("⚠️ Método repositório falhou, usando cálculo manual: " + e.getMessage());
        }

        // ✅ FALLBACK: Cálculo manual seguro
        List<EntradaEstoque> entradas = entradaEstoqueRepository.findByProdutoAndUserOrderByDataEntradaAsc(produto, currentUser);
        Integer saldoManual = 0;

        for (EntradaEstoque entrada : entradas) {
            // ✅ TRATAR saldo null como 0
            Integer saldoEntrada = entrada.getSaldo();
            if (saldoEntrada != null) {
                saldoManual += saldoEntrada;
            }
        }

        System.out.println("📦 Saldo manual calculado: " + saldoManual + " para produto: " + produto.getNome());
        System.out.println("📦 Total de entradas encontradas: " + entradas.size());

        return saldoManual;
    }
}