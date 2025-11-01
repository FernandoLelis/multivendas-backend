package com.fernando.erp_vendas.controller;

import com.fernando.erp_vendas.dto.EntradaEstoqueDTO;
import com.fernando.erp_vendas.model.EntradaEstoque;
import com.fernando.erp_vendas.model.Produto;
import com.fernando.erp_vendas.model.User;
import com.fernando.erp_vendas.repository.EntradaEstoqueRepository;
import com.fernando.erp_vendas.repository.ProdutoRepository;
import com.fernando.erp_vendas.service.EstoqueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/estoque")
public class EntradaEstoqueController {

    @Autowired
    private EntradaEstoqueRepository entradaEstoqueRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private EstoqueService estoqueService;

    // 🆕 MÉTODO PARA OBTER USUÁRIO LOGADO
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            return (User) authentication.getPrincipal();
        }
        throw new RuntimeException("Usuário não autenticado");
    }

    // ✅ ATUALIZADO: Registrar nova entrada de estoque (COMPRA) PARA O USUÁRIO
    @PostMapping("/entrada")
    public ResponseEntity<?> registrarEntrada(
            @RequestParam Long produtoId,
            @RequestParam Integer quantidade,
            @RequestParam BigDecimal custoTotal,
            @RequestParam(required = false) String fornecedor,
            @RequestParam String idPedidoCompra,
            @RequestParam String categoria,
            @RequestParam(required = false) String observacoes) {

        try {
            User currentUser = getCurrentUser();

            // Busca o produto pelo ID E USUÁRIO
            Produto produto = produtoRepository.findByIdAndUser(produtoId, currentUser)
                    .orElseThrow(() -> new RuntimeException("Produto não encontrado ou não pertence ao usuário"));

            // Validação dos campos obrigatórios
            if (idPedidoCompra == null || idPedidoCompra.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("ID do Pedido de Compra é obrigatório");
            }
            if (categoria == null || categoria.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Categoria é obrigatório");
            }

            // 🆕 VERIFICAR SE JÁ EXISTE COMPRA COM MESMO ID PEDIDO PARA ESTE USUÁRIO
            if (entradaEstoqueRepository.findByIdPedidoCompraAndUser(idPedidoCompra, currentUser).isPresent()) {
                return ResponseEntity.badRequest()
                        .body("Já existe uma compra cadastrada com este ID do Pedido: " + idPedidoCompra);
            }

            // Cria e salva a nova entrada de estoque com todos os campos
            EntradaEstoque entrada = new EntradaEstoque(
                    produto,
                    quantidade,
                    custoTotal,
                    fornecedor != null ? fornecedor : "",
                    idPedidoCompra,
                    categoria,
                    observacoes != null ? observacoes : "",
                    currentUser // 🆕 ASSOCIAR USUÁRIO
            );

            EntradaEstoque entradaSalva = entradaEstoqueRepository.save(entrada);

            return ResponseEntity.ok(entradaSalva);

        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.badRequest()
                    .body("Erro de integridade de dados: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao registrar entrada: " + e.getMessage());
        }
    }

    // ✅ ATUALIZADO: Atualizar entrada de estoque (COMPRA) DO USUÁRIO
    @PutMapping("/entrada/{id}")
    public ResponseEntity<?> atualizarEntrada(
            @PathVariable Long id,
            @RequestParam Long produtoId,
            @RequestParam Integer quantidade,
            @RequestParam BigDecimal custoTotal,
            @RequestParam(required = false) String fornecedor,
            @RequestParam String idPedidoCompra,
            @RequestParam String categoria,
            @RequestParam(required = false) String observacoes) {

        try {
            User currentUser = getCurrentUser();

            // Busca a entrada existente DO USUÁRIO
            EntradaEstoque entradaExistente = entradaEstoqueRepository.findByIdAndUser(id, currentUser)
                    .orElseThrow(() -> new RuntimeException("Compra não encontrada ou não pertence ao usuário"));

            // Busca o produto pelo ID E USUÁRIO
            Produto produto = produtoRepository.findByIdAndUser(produtoId, currentUser)
                    .orElseThrow(() -> new RuntimeException("Produto não encontrado ou não pertence ao usuário"));

            // Validação dos campos obrigatórios
            if (idPedidoCompra == null || idPedidoCompra.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("ID do Pedido de Compra é obrigatório");
            }
            if (categoria == null || categoria.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Categoria é obrigatório");
            }

            // 🆕 VERIFICAR SE JÁ EXISTE OUTRA COMPRA COM MESMO ID PEDIDO (exceto esta)
            Optional<EntradaEstoque> compraComMesmoPedido = entradaEstoqueRepository
                    .findByIdPedidoCompraAndUser(idPedidoCompra, currentUser);
            if (compraComMesmoPedido.isPresent() && !compraComMesmoPedido.get().getId().equals(id)) {
                return ResponseEntity.badRequest()
                        .body("Já existe outra compra cadastrada com este ID do Pedido: " + idPedidoCompra);
            }

            // ✅ CORREÇÃO SIMPLIFICADA: Lógica PEPS para atualizar saldo
            Integer saldoAtual = entradaExistente.getSaldo();
            Integer quantidadeAntiga = entradaExistente.getQuantidade();

            // Se o saldo atual é igual à quantidade antiga (lote intacto), atualiza o saldo
            if (saldoAtual != null && saldoAtual.equals(quantidadeAntiga)) {
                entradaExistente.setSaldo(quantidade);
            }
            // Se não tem saldo definido, define como a nova quantidade
            else if (saldoAtual == null) {
                entradaExistente.setSaldo(quantidade);
            }
            // Se o lote já foi parcialmente consumido, NÃO permite alterar quantidade
            else {
                return ResponseEntity.badRequest()
                        .body("Não é possível alterar quantidade de um lote que já foi parcialmente consumido. " +
                                "Saldo atual: " + saldoAtual + ", Quantidade antiga: " + quantidadeAntiga + ". " +
                                "Exclua as VENDAS que utilizaram este lote para liberar a edição.");
            }

            // Atualiza os campos da entrada existente
            entradaExistente.setProduto(produto);
            entradaExistente.setQuantidade(quantidade);
            entradaExistente.setCustoTotal(custoTotal);
            entradaExistente.setFornecedor(fornecedor != null ? fornecedor : "");
            entradaExistente.setIdPedidoCompra(idPedidoCompra);
            entradaExistente.setCategoria(categoria);
            entradaExistente.setObservacoes(observacoes != null ? observacoes : "");

            // ✅ CORREÇÃO: Recalcula o custo unitário COM TRATAMENTO DE ERRO
            if (quantidade != null && quantidade > 0) {
                try {
                    entradaExistente.setCustoUnitario(custoTotal.divide(
                            BigDecimal.valueOf(quantidade),
                            2,
                            java.math.RoundingMode.HALF_UP
                    ));
                } catch (ArithmeticException e) {
                    double custoUnitarioDouble = custoTotal.doubleValue() / quantidade;
                    entradaExistente.setCustoUnitario(BigDecimal.valueOf(custoUnitarioDouble)
                            .setScale(2, java.math.RoundingMode.HALF_UP));
                }
            }

            EntradaEstoque entradaAtualizada = entradaEstoqueRepository.save(entradaExistente);
            return ResponseEntity.ok(entradaAtualizada);

        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.badRequest()
                    .body("Erro de integridade de dados: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao atualizar compra: " + e.getMessage());
        }
    }

    // ✅ ATUALIZADO: Listar todas as entradas de estoque (COMPRAS) DO USUÁRIO - AGORA COM DTO
    @GetMapping("/entradas")
    public ResponseEntity<?> listarTodasEntradas() {
        try {
            User currentUser = getCurrentUser();
            List<EntradaEstoque> entradas = entradaEstoqueRepository.findByUserOrderByDataEntradaDesc(currentUser);

            // ✅ CONVERTER PARA DTO
            List<EntradaEstoqueDTO> entradasDTO = entradas.stream()
                    .map(EntradaEstoqueDTO::new)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(entradasDTO);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao listar compras: " + e.getMessage());
        }
    }

    // ✅ ATUALIZADO: Listar entradas de estoque de um produto (HISTÓRICO DE COMPRAS) DO USUÁRIO
    @GetMapping("/produto/{produtoId}")
    public ResponseEntity<?> listarEntradasPorProduto(@PathVariable Long produtoId) {
        try {
            User currentUser = getCurrentUser();

            // Buscar produto DO USUÁRIO
            Produto produto = produtoRepository.findByIdAndUser(produtoId, currentUser)
                    .orElseThrow(() -> new RuntimeException("Produto não encontrado ou não pertence ao usuário"));

            List<EntradaEstoque> entradas = entradaEstoqueRepository.findByProdutoAndUserOrderByDataEntradaAsc(produto, currentUser);

            // ✅ CONVERTER PARA DTO
            List<EntradaEstoqueDTO> entradasDTO = entradas.stream()
                    .map(EntradaEstoqueDTO::new)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(entradasDTO);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao listar compras do produto: " + e.getMessage());
        }
    }

    // ✅ ATUALIZADO: Verificar saldo total de um produto DO USUÁRIO
    @GetMapping("/saldo/{produtoId}")
    public ResponseEntity<?> verificarSaldo(@PathVariable Long produtoId) {
        try {
            User currentUser = getCurrentUser();

            Produto produto = produtoRepository.findByIdAndUser(produtoId, currentUser)
                    .orElseThrow(() -> new RuntimeException("Produto não encontrado ou não pertence ao usuário"));

            Integer saldo = estoqueService.verificarSaldoTotal(produto);
            return ResponseEntity.ok(saldo);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao verificar saldo: " + e.getMessage());
        }
    }

    // ✅ ATUALIZADO: Calcular custo de uma venda (LÓGICA PEPS) DO USUÁRIO
    @GetMapping("/calcular-custo/{produtoId}")
    public ResponseEntity<?> calcularCustoVenda(
            @PathVariable Long produtoId,
            @RequestParam Integer quantidade) {

        try {
            User currentUser = getCurrentUser();

            Produto produto = produtoRepository.findByIdAndUser(produtoId, currentUser)
                    .orElseThrow(() -> new RuntimeException("Produto não encontrado ou não pertence ao usuário"));

            BigDecimal custo = estoqueService.calcularCustoVenda(produto, quantidade);
            return ResponseEntity.ok(custo);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao calcular custo: " + e.getMessage());
        }
    }

    // ✅ ATUALIZADO: Excluir entrada de estoque (COMPRA) DO USUÁRIO COM VALIDAÇÃO PEPS
    @DeleteMapping("/entrada/{id}")
    public ResponseEntity<?> excluirEntrada(@PathVariable Long id) {
        try {
            User currentUser = getCurrentUser();

            EntradaEstoque entrada = entradaEstoqueRepository.findByIdAndUser(id, currentUser)
                    .orElseThrow(() -> new RuntimeException("Compra não encontrada ou não pertence ao usuário"));

            // ✅ NOVO: VALIDAÇÃO PEPS - Não permite excluir lote parcialmente consumido
            Integer saldoAtual = entrada.getSaldo();
            Integer quantidadeOriginal = entrada.getQuantidade();

            // Se o saldo atual é menor que a quantidade original, significa que parte foi vendida
            if (saldoAtual != null && saldoAtual < quantidadeOriginal) {
                return ResponseEntity.badRequest()
                        .body("Não é possível excluir um lote que já foi parcialmente consumido. " +
                                "Saldo atual: " + saldoAtual + ", Quantidade original: " + quantidadeOriginal + ". " +
                                "Exclua as VENDAS que utilizaram este lote primeiro para liberar a exclusão.");
            }

            // ✅ CORRETO: No PEPS, apenas excluímos o lote se estiver intacto
            entradaEstoqueRepository.delete(entrada);

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao excluir compra: " + e.getMessage());
        }
    }

    // 🆕 GET - Buscar entrada por ID DO USUÁRIO
    @GetMapping("/entrada/{id}")
    public ResponseEntity<?> buscarPorId(@PathVariable Long id) {
        try {
            User currentUser = getCurrentUser();
            Optional<EntradaEstoque> entrada = entradaEstoqueRepository.findByIdAndUser(id, currentUser);
            return entrada.map(ent -> ResponseEntity.ok(new EntradaEstoqueDTO(ent)))
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao buscar compra: " + e.getMessage());
        }
    }

    // 🆕 GET - Buscar entradas por categoria DO USUÁRIO
    @GetMapping("/categoria/{categoria}")
    public ResponseEntity<?> buscarPorCategoria(@PathVariable String categoria) {
        try {
            User currentUser = getCurrentUser();
            List<EntradaEstoque> entradas = entradaEstoqueRepository.findByCategoriaAndUser(categoria, currentUser);

            // ✅ CONVERTER PARA DTO
            List<EntradaEstoqueDTO> entradasDTO = entradas.stream()
                    .map(EntradaEstoqueDTO::new)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(entradasDTO);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao buscar compras por categoria: " + e.getMessage());
        }
    }

    // 🆕 GET - Buscar entradas por fornecedor DO USUÁRIO
    @GetMapping("/fornecedor/{fornecedor}")
    public ResponseEntity<?> buscarPorFornecedor(@PathVariable String fornecedor) {
        try {
            User currentUser = getCurrentUser();
            List<EntradaEstoque> entradas = entradaEstoqueRepository.findByFornecedorContainingAndUser(fornecedor, currentUser);

            // ✅ CONVERTER PARA DTO
            List<EntradaEstoqueDTO> entradasDTO = entradas.stream()
                    .map(EntradaEstoqueDTO::new)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(entradasDTO);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao buscar compras por fornecedor: " + e.getMessage());
        }
    }

    // 🆕 GET - Entradas com saldo baixo DO USUÁRIO
    @GetMapping("/saldo-baixo")
    public ResponseEntity<?> getEntradasComSaldoBaixo() {
        try {
            User currentUser = getCurrentUser();
            List<EntradaEstoque> entradas = entradaEstoqueRepository.findEntradasComSaldoBaixo(currentUser);

            // ✅ CONVERTER PARA DTO
            List<EntradaEstoqueDTO> entradasDTO = entradas.stream()
                    .map(EntradaEstoqueDTO::new)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(entradasDTO);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao buscar compras com saldo baixo: " + e.getMessage());
        }
    }
}