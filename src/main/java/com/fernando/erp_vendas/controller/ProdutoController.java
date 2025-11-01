package com.fernando.erp_vendas.controller;

import com.fernando.erp_vendas.model.Produto;
import com.fernando.erp_vendas.model.User;
import com.fernando.erp_vendas.repository.ProdutoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/produtos")
public class ProdutoController {

    @Autowired
    private ProdutoRepository produtoRepository;

    // 🆕 MÉTODO PARA OBTER USUÁRIO LOGADO (COM DEBUG)
    private User getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            System.out.println("🔐 DEBUG - Authentication: " + authentication);
            System.out.println("🔐 DEBUG - Principal type: " + (authentication != null ? authentication.getPrincipal().getClass().getName() : "null"));
            System.out.println("🔐 DEBUG - Principal: " + (authentication != null ? authentication.getPrincipal() : "null"));
            System.out.println("🔐 DEBUG - Is authenticated: " + (authentication != null ? authentication.isAuthenticated() : "false"));

            if (authentication != null && authentication.getPrincipal() instanceof User) {
                User user = (User) authentication.getPrincipal();
                System.out.println("✅ DEBUG - User found: " + user.getEmail() + " ID: " + user.getId());
                return user;
            }

            System.out.println("❌ DEBUG - User not found in SecurityContext");
            throw new RuntimeException("Usuário não autenticado");
        } catch (Exception e) {
            System.out.println("❌ DEBUG - Error in getCurrentUser: " + e.getMessage());
            throw new RuntimeException("Usuário não autenticado");
        }
    }

    // GET - Listar todos os produtos DO USUÁRIO LOGADO
    @GetMapping
    public ResponseEntity<?> listarTodos() {
        try {
            System.out.println("🔐 DEBUG - Starting listarTodos()");
            User currentUser = getCurrentUser();
            List<Produto> produtos = produtoRepository.findByUser(currentUser);
            System.out.println("✅ DEBUG - Found " + produtos.size() + " products for user " + currentUser.getEmail());
            return ResponseEntity.ok(produtos);
        } catch (Exception e) {
            System.out.println("❌ DEBUG - Error in listarTodos: " + e.getMessage());
            return ResponseEntity.badRequest().body("Erro ao listar produtos: " + e.getMessage());
        }
    }

    // GET - Buscar produto por ID DO USUÁRIO LOGADO
    @GetMapping("/{id}")
    public ResponseEntity<?> buscarPorId(@PathVariable Long id) {
        try {
            User currentUser = getCurrentUser();
            Optional<Produto> produto = produtoRepository.findByIdAndUser(id, currentUser);
            return produto.map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao buscar produto: " + e.getMessage());
        }
    }

    // POST - Criar novo produto PARA O USUÁRIO LOGADO
    @PostMapping
    public ResponseEntity<?> criarProduto(@RequestBody Produto produto) {
        try {
            User currentUser = getCurrentUser();

            // 🆕 VERIFICAR SE SKU JÁ EXISTE PARA ESTE USUÁRIO
            if (produtoRepository.existsBySkuAndUser(produto.getSku(), currentUser)) {
                return ResponseEntity.badRequest().body("Já existe um produto com este SKU");
            }

            // 🆕 ASSOCIAR USUÁRIO AO PRODUTO
            produto.setUser(currentUser);

            Produto produtoSalvo = produtoRepository.save(produto);
            return ResponseEntity.ok(produtoSalvo);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao criar produto: " + e.getMessage());
        }
    }

    // PUT - Atualizar produto DO USUÁRIO LOGADO
    @PutMapping("/{id}")
    public ResponseEntity<?> atualizarProduto(@PathVariable Long id, @RequestBody Produto produtoAtualizado) {
        try {
            User currentUser = getCurrentUser();

            // 🆕 BUSCAR PRODUTO PERTENCENTE AO USUÁRIO
            Optional<Produto> produtoExistente = produtoRepository.findByIdAndUser(id, currentUser);
            if (produtoExistente.isPresent()) {
                Produto produto = produtoExistente.get();

                // 🆕 VERIFICAR SE NOVO SKU JÁ EXISTE PARA OUTRO PRODUTO DO USUÁRIO
                if (!produto.getSku().equals(produtoAtualizado.getSku()) &&
                        produtoRepository.existsBySkuAndUser(produtoAtualizado.getSku(), currentUser)) {
                    return ResponseEntity.badRequest().body("Já existe outro produto com este SKU");
                }

                // ATUALIZAR DADOS
                produto.setNome(produtoAtualizado.getNome());
                produto.setSku(produtoAtualizado.getSku());
                produto.setAsin(produtoAtualizado.getAsin());
                produto.setDescricao(produtoAtualizado.getDescricao());
                produto.setEstoqueMinimo(produtoAtualizado.getEstoqueMinimo());

                Produto produtoAtualizadoSalvo = produtoRepository.save(produto);
                return ResponseEntity.ok(produtoAtualizadoSalvo);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao atualizar produto: " + e.getMessage());
        }
    }

    // DELETE - Deletar produto DO USUÁRIO LOGADO
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletarProduto(@PathVariable Long id) {
        try {
            User currentUser = getCurrentUser();

            // 🆕 VERIFICAR SE PRODUTO EXISTE E PERTENCE AO USUÁRIO
            Optional<Produto> produto = produtoRepository.findByIdAndUser(id, currentUser);
            if (produto.isPresent()) {
                produtoRepository.deleteById(id);
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao deletar produto: " + e.getMessage());
        }
    }

    // 🆕 GET - Buscar produtos por nome (busca parcial)
    @GetMapping("/buscar")
    public ResponseEntity<?> buscarPorNome(@RequestParam String nome) {
        try {
            User currentUser = getCurrentUser();
            List<Produto> produtos = produtoRepository.findByNomeContainingAndUser(nome, currentUser);
            return ResponseEntity.ok(produtos);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao buscar produtos: " + e.getMessage());
        }
    }

    // 🆕 GET - Buscar produto por SKU DO USUÁRIO LOGADO
    @GetMapping("/sku/{sku}")
    public ResponseEntity<?> buscarPorSku(@PathVariable String sku) {
        try {
            User currentUser = getCurrentUser();
            Optional<Produto> produto = produtoRepository.findBySkuAndUser(sku, currentUser);
            return produto.map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao buscar produto por SKU: " + e.getMessage());
        }
    }

    // 🆕 GET - Produtos com estoque baixo
    @GetMapping("/estoque-baixo")
    public ResponseEntity<?> getProdutosComEstoqueBaixo() {
        try {
            User currentUser = getCurrentUser();
            List<Produto> produtos = produtoRepository.findProdutosComEstoqueBaixo(currentUser);
            return ResponseEntity.ok(produtos);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao buscar produtos com estoque baixo: " + e.getMessage());
        }
    }
}