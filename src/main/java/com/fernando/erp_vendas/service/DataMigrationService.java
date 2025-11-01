package com.fernando.erp_vendas.service;

import com.fernando.erp_vendas.model.Produto;
import com.fernando.erp_vendas.model.EntradaEstoque;
import com.fernando.erp_vendas.model.User;
import com.fernando.erp_vendas.repository.ProdutoRepository;
import com.fernando.erp_vendas.repository.EntradaEstoqueRepository;
import com.fernando.erp_vendas.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class DataMigrationService {

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private EntradaEstoqueRepository entradaEstoqueRepository;

    @Autowired
    private UserRepository userRepository;

    // 🆕 MÉTODO TEMPORÁRIO: Migração básica sem usar métodos complexos
    public String migrarEstoqueParaPEPS(Long userId) {
        try {
            // Buscar usuário específico
            Optional<User> userOpt = userRepository.findById(userId);
            if (!userOpt.isPresent()) {
                return "Erro: Usuário não encontrado com ID: " + userId;
            }

            User user = userOpt.get();

            // 🆕 USAR MÉTODO LEGACY TEMPORARIAMENTE
            List<Produto> produtos = produtoRepository.findAll(); // Método legacy
            int migrados = 0;

            for (Produto produto : produtos) {
                // 🆕 VERIFICAR SE O PRODUTO JÁ TEM USER (se não, associar)
                if (produto.getUser() == null) {
                    produto.setUser(user);
                    produtoRepository.save(produto);
                }

                // 🆕 VERIFICAR SE JÁ EXISTE ENTRADA (usando método legacy)
                List<EntradaEstoque> entradasExistentes = entradaEstoqueRepository
                        .findByProdutoOrderByDataEntradaAsc(produto); // Método legacy

                if (entradasExistentes.isEmpty()) {
                    // Criar entrada de estoque inicial
                    EntradaEstoque entrada = new EntradaEstoque();
                    entrada.setProduto(produto);
                    entrada.setQuantidade(10);
                    entrada.setSaldo(10);
                    entrada.setCustoTotal(BigDecimal.valueOf(50.00));
                    entrada.setCustoUnitario(BigDecimal.valueOf(5.00));
                    entrada.setFornecedor("Fornecedor Migração");
                    entrada.setIdPedidoCompra("MIG_" + produto.getId() + "_" + System.currentTimeMillis());
                    entrada.setCategoria("Produto");
                    entrada.setObservacoes("Estoque inicial migrado para PEPS");
                    entrada.setUser(user); // 🆕 ASSOCIAR USUÁRIO

                    entradaEstoqueRepository.save(entrada);
                    migrados++;
                }
            }

            return "Migração concluída para usuário " + user.getEmail() + ": " + migrados + " entradas criadas";

        } catch (Exception e) {
            return "Erro na migração: " + e.getMessage();
        }
    }

    // 🆕 MÉTODO SIMPLIFICADO: Status básico
    public String verificarStatusMigracao() {
        try {
            List<User> usuarios = userRepository.findAll();
            StringBuilder status = new StringBuilder();

            status.append("=== STATUS DA MIGRAÇÃO PEPS ===\n");

            for (User user : usuarios) {
                // 🆕 USAR MÉTODOS LEGACY TEMPORARIAMENTE
                List<Produto> produtos = produtoRepository.findAll(); // Todos produtos
                List<EntradaEstoque> entradas = entradaEstoqueRepository.findAll(); // Todas entradas

                long produtosUsuario = produtos.stream().filter(p -> user.equals(p.getUser())).count();
                long entradasUsuario = entradas.stream().filter(e -> user.equals(e.getUser())).count();

                status.append("\nUsuário: ").append(user.getEmail())
                        .append(" | Produtos: ").append(produtosUsuario)
                        .append(" | Entradas PEPS: ").append(entradasUsuario)
                        .append(" | Cobertura: ").append(produtosUsuario > 0 ?
                                (entradasUsuario * 100 / produtosUsuario) + "%" : "0%");
            }

            return status.toString();

        } catch (Exception e) {
            return "Erro ao verificar status: " + e.getMessage();
        }
    }

    // 🆕 MÉTODO SIMPLIFICADO: Corrigir produtos sem usuário
    public String corrigirProdutosSemUsuario() {
        try {
            // Buscar produtos sem usuário (usando método legacy)
            List<Produto> produtos = produtoRepository.findAll();
            List<Produto> produtosSemUsuario = produtos.stream()
                    .filter(p -> p.getUser() == null)
                    .toList();

            if (produtosSemUsuario.isEmpty()) {
                return "Nenhum produto sem usuário encontrado.";
            }

            // Buscar usuário admin padrão (primeiro usuário)
            Optional<User> adminUser = userRepository.findAll().stream().findFirst();
            if (!adminUser.isPresent()) {
                return "Erro: Nenhum usuário encontrado no sistema.";
            }

            User user = adminUser.get();
            int corrigidos = 0;

            for (Produto produto : produtosSemUsuario) {
                produto.setUser(user);
                produtoRepository.save(produto);
                corrigidos++;
            }

            return "Corrigidos " + corrigidos + " produtos sem usuário (associados a " + user.getEmail() + ")";

        } catch (Exception e) {
            return "Erro na correção: " + e.getMessage();
        }
    }

    // 🆕 MÉTODO SIMPLIFICADO: Migrar todos usuários
    public String migrarEstoqueParaTodosUsuarios() {
        try {
            List<User> usuarios = userRepository.findAll();
            int totalMigrados = 0;
            StringBuilder resultado = new StringBuilder();

            for (User user : usuarios) {
                String resultadoUsuario = migrarEstoqueParaPEPS(user.getId());
                resultado.append(resultadoUsuario).append("\n");
                totalMigrados++;
            }

            resultado.append("\n=== RESUMO GERAL ===\n");
            resultado.append("Total de usuários processados: ").append(usuarios.size()).append("\n");
            resultado.append("Total de migrações realizadas: ").append(totalMigrados);

            return resultado.toString();

        } catch (Exception e) {
            return "Erro na migração geral: " + e.getMessage();
        }
    }
}