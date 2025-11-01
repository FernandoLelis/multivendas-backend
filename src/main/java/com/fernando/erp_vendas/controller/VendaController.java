package com.fernando.erp_vendas.controller;

import com.fernando.erp_vendas.dto.VendaDTO;
import com.fernando.erp_vendas.model.Produto;
import com.fernando.erp_vendas.model.User;
import com.fernando.erp_vendas.model.Venda;
import com.fernando.erp_vendas.repository.ProdutoRepository;
import com.fernando.erp_vendas.repository.VendaRepository;
import com.fernando.erp_vendas.service.EstoqueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.math.BigDecimal;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/vendas")
public class VendaController {

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private VendaRepository vendaRepository;

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

    // ✅ ATUALIZADO: Método para calcular faturamento por plataforma DO USUÁRIO
    private Map<String, Double> calcularFaturamentoPorPlataforma(User user) {
        List<Venda> vendasUsuario = vendaRepository.findByUser(user);
        Map<String, Double> faturamentoPorPlataforma = new HashMap<>();

        // Inicializar todas as plataformas
        faturamentoPorPlataforma.put("AMAZON", 0.0);
        faturamentoPorPlataforma.put("MERCADO_LIVRE", 0.0);
        faturamentoPorPlataforma.put("SHOPEE", 0.0);

        // Calcular faturamento real usando o novo método
        for (Venda venda : vendasUsuario) {
            String plataforma = venda.getPlataforma();
            double faturamentoVenda = venda.calcularFaturamento();

            faturamentoPorPlataforma.put(plataforma,
                    faturamentoPorPlataforma.getOrDefault(plataforma, 0.0) + faturamentoVenda);
        }

        return faturamentoPorPlataforma;
    }

    // ✅ ATUALIZADO: Método para produtos mais vendidos com faturamento DO USUÁRIO
    private List<Map<String, Object>> calcularProdutosMaisVendidos(User user) {
        List<Venda> vendasUsuario = vendaRepository.findByUser(user);
        Map<String, Map<String, Object>> produtosMap = new HashMap<>();

        // Agrupar por produto
        for (Venda venda : vendasUsuario) {
            String produtoNome = venda.getProduto().getNome();

            if (!produtosMap.containsKey(produtoNome)) {
                Map<String, Object> produtoInfo = new HashMap<>();
                produtoInfo.put("produto", produtoNome);
                produtoInfo.put("quantidadeVendida", 0);
                produtoInfo.put("faturamento", 0.0);
                produtoInfo.put("lucroLiquido", 0.0);
                produtosMap.put(produtoNome, produtoInfo);
            }

            Map<String, Object> produtoInfo = produtosMap.get(produtoNome);
            int quantidadeAtual = (int) produtoInfo.get("quantidadeVendida");
            double faturamentoAtual = (double) produtoInfo.get("faturamento");
            double lucroLiquidoAtual = (double) produtoInfo.get("lucroLiquido");

            produtoInfo.put("quantidadeVendida", quantidadeAtual + venda.getQuantidade());
            produtoInfo.put("faturamento", faturamentoAtual + venda.calcularFaturamento());
            produtoInfo.put("lucroLiquido", lucroLiquidoAtual + venda.calcularLucroLiquido());
        }

        // Converter para lista e ordenar por quantidade vendida (decrescente)
        List<Map<String, Object>> produtosMaisVendidos = new ArrayList<>(produtosMap.values());
        produtosMaisVendidos.sort((a, b) -> {
            int quantidadeA = (int) a.get("quantidadeVendida");
            int quantidadeB = (int) b.get("quantidadeVendida");
            return Integer.compare(quantidadeB, quantidadeA); // Ordem decrescente
        });

        // Manter apenas top 5
        return produtosMaisVendidos.size() > 5 ? produtosMaisVendidos.subList(0, 5) : produtosMaisVendidos;
    }

    // ✅ ATUALIZADO: ENDPOINT DASHBOARD - VERSÃO FUNCIONAL COM MULTI-TENANCY
    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard() {
        try {
            User currentUser = getCurrentUser();
            Map<String, Object> dashboard = new HashMap<>();

            // Buscar todas as vendas DO USUÁRIO para calcular em tempo real
            List<Venda> vendasUsuario = vendaRepository.findByUser(currentUser);

            // ✅ CALCULAR TOTAIS USANDOS OS NOVOS MÉTODOS
            double faturamentoTotal = 0;
            double custoEfetivoTotal = 0;
            double lucroBrutoTotal = 0;
            double lucroLiquidoTotal = 0;
            double despesasOperacionaisTotal = 0;

            for (Venda venda : vendasUsuario) {
                faturamentoTotal += venda.calcularFaturamento();
                custoEfetivoTotal += venda.calcularCustoEfetivoTotal();
                lucroBrutoTotal += venda.calcularLucroBruto();
                lucroLiquidoTotal += venda.calcularLucroLiquido();
                despesasOperacionaisTotal += venda.getDespesasOperacionais();
            }

            // ✅ DADOS ATUALIZADOS
            dashboard.put("faturamentoTotal", faturamentoTotal);
            dashboard.put("custoEfetivoTotal", custoEfetivoTotal);
            dashboard.put("lucroBrutoTotal", lucroBrutoTotal);
            dashboard.put("lucroLiquidoTotal", lucroLiquidoTotal);
            dashboard.put("despesasOperacionaisTotal", despesasOperacionaisTotal);

            // Calcular ROI total
            double roiTotal = (custoEfetivoTotal > 0) ? (lucroLiquidoTotal / custoEfetivoTotal) * 100 : 0;
            dashboard.put("roiTotal", roiTotal);

            // ✅ DADOS EXISTENTES (agora calculados corretamente)
            dashboard.put("faturamentoPorPlataforma", calcularFaturamentoPorPlataforma(currentUser));
            dashboard.put("totalVendas", vendasUsuario.size());

            LocalDateTime agora = LocalDateTime.now();
            long vendasMesAtual = vendasUsuario.stream()
                    .filter(venda -> venda.getData().getMonthValue() == agora.getMonthValue()
                            && venda.getData().getYear() == agora.getYear())
                    .count();
            dashboard.put("vendasMesAtual", vendasMesAtual);

            dashboard.put("produtosMaisVendidos", calcularProdutosMaisVendidos(currentUser));

            return ResponseEntity.ok(dashboard);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao carregar dashboard: " + e.getMessage());
        }
    }

    // GET - Listar vendas por dia DO USUÁRIO
    @GetMapping("/vendas-por-dia")
    public ResponseEntity<?> getVendasPorDia() {
        try {
            User currentUser = getCurrentUser();
            List<Object[]> resultados = vendaRepository.findVendasPorDia(currentUser);
            Map<String, Integer> vendasPorDia = new HashMap<>();

            for (Object[] resultado : resultados) {
                Date data = (Date) resultado[0];
                Long quantidade = (Long) resultado[1];

                // Converter Date para String no formato YYYY-MM-DD
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                String dataStr = sdf.format(data);

                vendasPorDia.put(dataStr, quantidade.intValue());
            }

            return ResponseEntity.ok(vendasPorDia);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao buscar vendas por dia: " + e.getMessage());
        }
    }

    // ✅ ATUALIZADO: GET - Listar todas as vendas DO USUÁRIO - AGORA COM DTO
    @GetMapping
    public ResponseEntity<?> listarTodas() {
        try {
            User currentUser = getCurrentUser();
            List<Venda> vendas = vendaRepository.findByUser(currentUser);

            // ✅ CONVERTER PARA DTO
            List<VendaDTO> vendasDTO = vendas.stream()
                    .map(VendaDTO::new)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(vendasDTO);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao listar vendas: " + e.getMessage());
        }
    }

    // ✅ ATUALIZADO: GET - Buscar venda por ID DO USUÁRIO - AGORA COM DTO
    @GetMapping("/{id}")
    public ResponseEntity<?> buscarPorId(@PathVariable Long id) {
        try {
            User currentUser = getCurrentUser();
            Optional<Venda> venda = vendaRepository.findByIdAndUser(id, currentUser);
            return venda.map(v -> ResponseEntity.ok(new VendaDTO(v)))
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao buscar venda: " + e.getMessage());
        }
    }

    // ✅ CORRIGIDO: POST - Criar nova venda PARA O USUÁRIO
    @PostMapping
    public ResponseEntity<?> criarVenda(@RequestBody Venda venda) {
        try {
            User currentUser = getCurrentUser();

            // 1. Verificar se o produto existe E PERTENCE AO USUÁRIO
            Optional<Produto> produto = produtoRepository.findByIdAndUser(
                    venda.getProduto().getId(), currentUser);
            if (!produto.isPresent()) {
                return ResponseEntity.badRequest().body("Produto não encontrado ou não pertence ao usuário");
            }

            // 2. Verificar se já existe venda com mesmo ID do pedido PARA ESTE USUÁRIO
            if (vendaRepository.findByIdPedidoAndUser(venda.getIdPedido(), currentUser).isPresent()) {
                return ResponseEntity.badRequest().body("Já existe uma venda com este ID do pedido");
            }

            // 3. Verificar se tem estoque suficiente
            Integer saldoDisponivel = estoqueService.verificarSaldoTotal(produto.get());
            if (saldoDisponivel < venda.getQuantidade()) {
                return ResponseEntity.badRequest().body("Estoque insuficiente");
            }

            // 🆕 ASSOCIAR USUÁRIO À VENDA
            venda.setUser(currentUser);

            // ✅ CORRIGIDO: FLUXO CORRETO
            // 4. PRIMEIRO: Salvar a venda (para gerar ID)
            Venda vendaSalva = vendaRepository.save(venda);

            // 5. SEGUNDO: Calcular custo PEPS E registrar itens (passando venda já salva)
            BigDecimal custoPEPS = estoqueService.calcularCustoVendaERegistrarItens(vendaSalva);

            return ResponseEntity.ok(vendaSalva);

        } catch (Exception e) {
            System.out.println("❌ Erro ao criar venda: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Erro ao criar venda: " + e.getMessage());
        }
    }

    // PUT - Atualizar venda existente DO USUÁRIO
    @PutMapping("/{id}")
    public ResponseEntity<?> atualizarVenda(@PathVariable Long id, @RequestBody Venda vendaAtualizada) {
        try {
            User currentUser = getCurrentUser();

            // 1. Buscar venda existente DO USUÁRIO
            Optional<Venda> vendaExistenteOpt = vendaRepository.findByIdAndUser(id, currentUser);
            if (!vendaExistenteOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            Venda vendaExistente = vendaExistenteOpt.get();

            // 2. Verificar se o produto existe E PERTENCE AO USUÁRIO
            Optional<Produto> produto = produtoRepository.findByIdAndUser(
                    vendaAtualizada.getProduto().getId(), currentUser);
            if (!produto.isPresent()) {
                return ResponseEntity.badRequest().body("Produto não encontrado ou não pertence ao usuário");
            }

            // 3. Verificar se já existe outra venda com mesmo ID do pedido (exceto a própria)
            Optional<Venda> vendaComMesmoPedido = vendaRepository.findByIdPedidoAndUser(
                    vendaAtualizada.getIdPedido(), currentUser);
            if (vendaComMesmoPedido.isPresent() && !vendaComMesmoPedido.get().getId().equals(id)) {
                return ResponseEntity.badRequest().body("Já existe outra venda com este ID do pedido");
            }

            // 4. ✅ ATUALIZADO: Para edição de venda, é mais complexo com rastreamento
            // Por enquanto, vamos impedir edição que mude produto ou quantidade
            if (!vendaExistente.getProduto().getId().equals(vendaAtualizada.getProduto().getId()) ||
                    vendaExistente.getQuantidade() != vendaAtualizada.getQuantidade()) {

                return ResponseEntity.badRequest().body("Não é permitido alterar produto ou quantidade da venda");
            }

            // 5. Atualizar apenas campos permitidos
            vendaExistente.setData(vendaAtualizada.getData());
            vendaExistente.setIdPedido(vendaAtualizada.getIdPedido());
            vendaExistente.setPlataforma(vendaAtualizada.getPlataforma());
            vendaExistente.setPrecoVenda(vendaAtualizada.getPrecoVenda());
            vendaExistente.setFretePagoPeloCliente(vendaAtualizada.getFretePagoPeloCliente());
            vendaExistente.setCustoEnvio(vendaAtualizada.getCustoEnvio());
            vendaExistente.setTarifaPlataforma(vendaAtualizada.getTarifaPlataforma());
            vendaExistente.setDespesasOperacionais(vendaAtualizada.getDespesasOperacionais());

            // 6. Salvar venda atualizada
            Venda vendaSalva = vendaRepository.save(vendaExistente);

            return ResponseEntity.ok(vendaSalva);

        } catch (Exception e) {
            System.out.println("❌ Erro ao atualizar venda: " + e.getMessage());
            return ResponseEntity.badRequest().body("Erro ao atualizar venda: " + e.getMessage());
        }
    }

    // ✅ ATUALIZADO: DELETE - Excluir venda com reversão de estoque DO USUÁRIO
    @DeleteMapping("/{id}")
    public ResponseEntity<?> excluirVenda(@PathVariable Long id) {
        try {
            User currentUser = getCurrentUser();

            Optional<Venda> vendaOpt = vendaRepository.findByIdAndUser(id, currentUser);
            if (!vendaOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            Venda venda = vendaOpt.get();

            // ✅ NOVO: Reverter estoque baseado nos itens rastreados
            estoqueService.reverterEstoqueVenda(venda);

            // Excluir a venda (os itens serão excluídos automaticamente pelo cascade)
            vendaRepository.deleteById(id);

            System.out.println("✅ Venda excluída e estoque revertido: " + venda.getIdPedido());
            return ResponseEntity.noContent().build();

        } catch (Exception e) {
            System.out.println("❌ Erro ao reverter estoque na exclusão da venda: " + e.getMessage());
            return ResponseEntity.badRequest().body("Erro ao excluir venda: " + e.getMessage());
        }
    }

    // ✅ ATUALIZADO: GET - Buscar venda por Plataforma DO USUÁRIO - AGORA COM DTO
    @GetMapping("/plataforma/{plataforma}")
    public ResponseEntity<?> buscarPorPlataforma(@PathVariable String plataforma) {
        try {
            User currentUser = getCurrentUser();
            List<Venda> vendas = vendaRepository.findByPlataformaAndUser(plataforma, currentUser);

            // ✅ CONVERTER PARA DTO
            List<VendaDTO> vendasDTO = vendas.stream()
                    .map(VendaDTO::new)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(vendasDTO);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao buscar vendas por plataforma: " + e.getMessage());
        }
    }

    // ✅ ATUALIZADO: GET - Buscar venda por Periodo DO USUÁRIO - AGORA COM DTO
    @GetMapping("/periodo")
    public ResponseEntity<?> buscarPorPeriodo(
            @RequestParam LocalDateTime inicio,
            @RequestParam LocalDateTime fim) {
        try {
            User currentUser = getCurrentUser();
            List<Venda> vendas = vendaRepository.findByDataBetweenAndUser(inicio, fim, currentUser);

            // ✅ CONVERTER PARA DTO
            List<VendaDTO> vendasDTO = vendas.stream()
                    .map(VendaDTO::new)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(vendasDTO);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao buscar vendas por período: " + e.getMessage());
        }
    }

    // ✅ ATUALIZADO: GET - Buscar venda por nome DO USUÁRIO - AGORA COM DTO
    @GetMapping("/produto/{nome}")
    public ResponseEntity<?> buscarPorNomeProduto(@PathVariable String nome) {
        try {
            User currentUser = getCurrentUser();

            // 1. Buscar produtos com nome similar DO USUÁRIO
            List<Produto> produtos = produtoRepository.findByNomeContainingAndUser(nome, currentUser);

            if (produtos.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            // 2. Buscar vendas para cada produto encontrado DO USUÁRIO
            List<Venda> vendas = new ArrayList<>();
            for (Produto produto : produtos) {
                vendas.addAll(vendaRepository.findByProdutoAndUser(produto, currentUser));
            }

            // ✅ CONVERTER PARA DTO
            List<VendaDTO> vendasDTO = vendas.stream()
                    .map(VendaDTO::new)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(vendasDTO);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao buscar vendas por nome do produto: " + e.getMessage());
        }
    }

    // ✅ ATUALIZADO: GET - Cálculos financeiros da venda DO USUÁRIO
    @GetMapping("/{id}/calculos")
    public ResponseEntity<?> getCalculos(@PathVariable Long id) {
        try {
            User currentUser = getCurrentUser();
            Optional<Venda> vendaOpt = vendaRepository.findByIdAndUser(id, currentUser);
            if (!vendaOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            Venda venda = vendaOpt.get();
            Map<String, Double> calculos = new HashMap<>();
            calculos.put("faturamento", venda.calcularFaturamento());
            calculos.put("custoEfetivoTotal", venda.calcularCustoEfetivoTotal());
            calculos.put("lucroBruto", venda.calcularLucroBruto());
            calculos.put("lucroLiquido", venda.calcularLucroLiquido());
            calculos.put("roi", venda.calcularROI());
            calculos.put("despesasOperacionais", venda.getDespesasOperacionais());

            return ResponseEntity.ok(calculos);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao calcular métricas da venda: " + e.getMessage());
        }
    }

    // ✅ ATUALIZADO: 🆕 Resumo mensal DO USUÁRIO - AGORA COM DTO
    @GetMapping("/resumo-mensal")
    public ResponseEntity<?> resumoMensal(@RequestParam int mes, @RequestParam int ano) {
        try {
            User currentUser = getCurrentUser();
            LocalDateTime inicio = LocalDateTime.of(ano, mes, 1, 0, 0, 0);
            LocalDateTime fim = LocalDateTime.of(ano, mes, 1, 23, 59, 59)
                    .plusMonths(1)
                    .minusDays(1);

            List<Venda> vendas = vendaRepository.findByDataBetweenAndUser(inicio, fim, currentUser);

            // ✅ CONVERTER PARA DTO
            List<VendaDTO> vendasDTO = vendas.stream()
                    .map(VendaDTO::new)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(vendasDTO);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao buscar resumo mensal: " + e.getMessage());
        }
    }

    // ✅ ATUALIZADO: GET - Buscar venda por ID do pedido DO USUÁRIO - AGORA COM DTO
    @GetMapping("/pedido/{idPedido}")
    public ResponseEntity<?> buscarPorIdPedido(@PathVariable String idPedido) {
        try {
            User currentUser = getCurrentUser();
            Optional<Venda> venda = vendaRepository.findByIdPedidoAndUser(idPedido, currentUser);
            return venda.map(v -> ResponseEntity.ok(new VendaDTO(v)))
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao buscar venda por ID do pedido: " + e.getMessage());
        }
    }
}