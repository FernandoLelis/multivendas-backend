package com.fernando.erp_vendas.controller;

import com.fernando.erp_vendas.dto.VendaDTO;
import com.fernando.erp_vendas.model.*;
import com.fernando.erp_vendas.repository.EntradaEstoqueRepository;
import com.fernando.erp_vendas.repository.ProdutoRepository;
import com.fernando.erp_vendas.repository.VendaRepository;
import com.fernando.erp_vendas.service.EstoqueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;
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

    @Autowired
    private EntradaEstoqueRepository entradaEstoqueRepository;

    // 🆕 MÉTODO PARA OBTER USUÁRIO LOGADO
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            return (User) authentication.getPrincipal();
        }
        throw new RuntimeException("Usuário não autenticado");
    }

    // ✅ ATUALIZADO: Método para calcular faturamento por plataforma DO USUÁRIO (com múltiplos produtos)
    private Map<String, Double> calcularFaturamentoPorPlataforma(User user) {
        List<Venda> vendasUsuario = vendaRepository.findByUserWithProduto(user);
        Map<String, Double> faturamentoPorPlataforma = new HashMap<>();

        // Inicializar plataformas conhecidas
        faturamentoPorPlataforma.put("AMAZON", 0.0);
        faturamentoPorPlataforma.put("MERCADO_LIVRE", 0.0);
        faturamentoPorPlataforma.put("SHOPEE", 0.0);

        // Calcular faturamento usando o novo método (já soma todos os produtos)
        for (Venda venda : vendasUsuario) {
            String plataforma = venda.getPlataforma();
            double faturamentoVenda = venda.calcularFaturamento();

            faturamentoPorPlataforma.put(plataforma,
                    faturamentoPorPlataforma.getOrDefault(plataforma, 0.0) + faturamentoVenda);
        }

        return faturamentoPorPlataforma;
    }

    // ✅ ATUALIZADO: Método para produtos mais vendidos com faturamento DO USUÁRIO (com múltiplos produtos)
    private List<Map<String, Object>> calcularProdutosMaisVendidos(User user) {
        List<Venda> vendasUsuario = vendaRepository.findByUserWithProduto(user);
        Map<Long, Map<String, Object>> produtosMap = new HashMap<>();

        // Agrupar por produto (usando ID para evitar duplicatas por nome)
        for (Venda venda : vendasUsuario) {
            // ✅ NOVO: Processar cada item da venda
            if (venda.getItens() != null) {
                for (ItemVenda item : venda.getItens()) {
                    Produto produto = item.getLote().getProduto();
                    Long produtoId = produto.getId();

                    if (!produtosMap.containsKey(produtoId)) {
                        Map<String, Object> produtoInfo = new HashMap<>();
                        produtoInfo.put("produtoId", produtoId);
                        produtoInfo.put("produtoNome", produto.getNome());
                        produtoInfo.put("quantidadeVendida", 0);
                        produtoInfo.put("faturamento", 0.0);
                        produtoInfo.put("lucroLiquido", 0.0);
                        produtosMap.put(produtoId, produtoInfo);
                    }

                    Map<String, Object> produtoInfo = produtosMap.get(produtoId);
                    int quantidadeAtual = (int) produtoInfo.get("quantidadeVendida");
                    double faturamentoAtual = (double) produtoInfo.get("faturamento");
                    double lucroLiquidoAtual = (double) produtoInfo.get("lucroLiquido");

                    // ✅ Calcular contribuição deste item para o produto
                    // Nota: O faturamento e lucro são distribuídos proporcionalmente ao custo do item
                    double proporcaoItem = item.getCustoTotal().doubleValue() / venda.getCustoProdutoVendido();
                    double faturamentoItem = venda.calcularFaturamento() * proporcaoItem;
                    double lucroItem = venda.calcularLucroLiquido() * proporcaoItem;

                    produtoInfo.put("quantidadeVendida", quantidadeAtual + item.getQuantidade());
                    produtoInfo.put("faturamento", faturamentoAtual + faturamentoItem);
                    produtoInfo.put("lucroLiquido", lucroLiquidoAtual + lucroItem);
                }
            }
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

    // ✅ ATUALIZADO: ENDPOINT DASHBOARD - VERSÃO FUNCIONAL COM MULTI-TENANCY E MÚLTIPLOS PRODUTOS
    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard() {
        try {
            User currentUser = getCurrentUser();
            Map<String, Object> dashboard = new HashMap<>();

            // Buscar todas as vendas DO USUÁRIO para calcular em tempo real
            List<Venda> vendasUsuario = vendaRepository.findByUserWithProduto(currentUser);

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

    // Métodos de cálculo mensal (mantidos - precisarão ser ajustados depois)
    @GetMapping("/faturamento-mes-atual")
    public ResponseEntity<?> getFaturamentoMesAtual() {
        try {
            User currentUser = getCurrentUser();
            Double faturamento = vendaRepository.calcularFaturamentoMesAtual(currentUser);
            return ResponseEntity.ok(faturamento != null ? faturamento : 0.0);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao calcular faturamento do mês atual: " + e.getMessage());
        }
    }

    @GetMapping("/custo-efetivo-mes-atual")
    public ResponseEntity<?> getCustoEfetivoMesAtual() {
        try {
            User currentUser = getCurrentUser();
            Double custoEfetivo = vendaRepository.calcularCustoEfetivoMesAtual(currentUser);
            return ResponseEntity.ok(custoEfetivo != null ? custoEfetivo : 0.0);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao calcular custo efetivo do mês atual: " + e.getMessage());
        }
    }

    @GetMapping("/lucro-bruto-mes-atual")
    public ResponseEntity<?> getLucroBrutoMesAtual() {
        try {
            User currentUser = getCurrentUser();
            Double lucroBruto = vendaRepository.calcularLucroBrutoMesAtual(currentUser);
            return ResponseEntity.ok(lucroBruto != null ? lucroBruto : 0.0);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao calcular lucro bruto do mês atual: " + e.getMessage());
        }
    }

    @GetMapping("/lucro-liquido-mes-atual")
    public ResponseEntity<?> getLucroLiquidoMesAtual() {
        try {
            User currentUser = getCurrentUser();
            Double lucroLiquido = vendaRepository.calcularLucroLiquidoMesAtual(currentUser);
            return ResponseEntity.ok(lucroLiquido != null ? lucroLiquido : 0.0);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao calcular lucro líquido do mês atual: " + e.getMessage());
        }
    }

    @GetMapping("/quantidade-vendas")
    public ResponseEntity<?> getQuantidadeVendas() {
        try {
            User currentUser = getCurrentUser();
            Long userId = currentUser.getId();

            Long vendasMesAtual = vendaRepository.countVendasMesAtual(userId);
            Long vendasMesAnterior = vendaRepository.countVendasMesAnterior(userId);
            Long vendasAnoAtual = vendaRepository.countVendasAnoAtual(userId);

            double variacao = 0.0;
            if (vendasMesAnterior != null && vendasMesAnterior > 0) {
                variacao = ((vendasMesAtual.doubleValue() - vendasMesAnterior.doubleValue())
                        / vendasMesAnterior.doubleValue()) * 100;
            }

            Map<String, Object> response = new HashMap<>();
            response.put("mesAtual", vendasMesAtual != null ? vendasMesAtual : 0);
            response.put("anoAtual", vendasAnoAtual != null ? vendasAnoAtual : 0);
            response.put("variacao", variacao);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("Erro ao buscar quantidade de vendas: " + e.getMessage());
        }
    }

    @GetMapping("/vendas-por-dia")
    public ResponseEntity<?> getVendasPorDia(
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer ano) {

        try {
            User currentUser = getCurrentUser();
            List<Object[]> resultados;
            Map<String, Integer> vendasPorDia = new HashMap<>();

            if (mes != null && ano != null) {
                resultados = vendaRepository.findVendasPorDiaDoMes(currentUser, mes, ano);
            } else {
                resultados = vendaRepository.findVendasPorDia(currentUser);
            }

            for (Object[] resultado : resultados) {
                Date data = (Date) resultado[0];
                Long quantidade = (Long) resultado[1];

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
            System.out.println("🔍 DEBUG VENDAS - Buscando vendas para usuário: " + currentUser.getEmail());

            List<Venda> vendas = vendaRepository.findByUserWithProduto(currentUser);
            System.out.println("📊 DEBUG VENDAS - Total de vendas encontradas: " + vendas.size());

            // ✅ CONVERTER PARA DTO
            List<VendaDTO> vendasDTO = vendas.stream()
                    .map(VendaDTO::new)
                    .collect(Collectors.toList());

            System.out.println("✅ Vendas convertidas para DTO: " + vendasDTO.size());
            return ResponseEntity.ok(vendasDTO);
        } catch (Exception e) {
            System.out.println("❌ ERRO CRÍTICO em listarTodas: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Erro ao listar vendas: " + e.getMessage());
        }
    }

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

    // ✅✅✅ CORRIGIDO COMPLETAMENTE: POST - Criar nova venda com MÚLTIPLOS PRODUTOS
    @PostMapping
    public ResponseEntity<?> criarVenda(@RequestBody Map<String, Object> vendaData) {
        try {
            System.out.println("🔍 DEBUG INICIAL - Dados recebidos: " + vendaData);

            User currentUser = getCurrentUser();

            // ✅ 1️⃣ VALIDAR DADOS OBRIGATÓRIOS
            if (!vendaData.containsKey("idPedido") || !vendaData.containsKey("plataforma") ||
                    !vendaData.containsKey("precoVenda") || !vendaData.containsKey("itens")) {
                return ResponseEntity.badRequest().body("Dados incompletos. Campos obrigatórios: idPedido, plataforma, precoVenda, itens");
            }

            // ✅ 2️⃣ EXTRAIR DADOS BÁSICOS DA VENDA
            String idPedido = vendaData.get("idPedido").toString();
            String plataforma = vendaData.get("plataforma").toString();
            Double precoVenda = Double.valueOf(vendaData.get("precoVenda").toString());

            // ✅ EXTRAIR DADOS OPCIONAIS
            LocalDateTime dataVenda = extrairData(vendaData.get("data"));
            Double fretePagoPeloCliente = getDoubleValue(vendaData.get("fretePagoPeloCliente"), 0.0);
            Double custoEnvio = getDoubleValue(vendaData.get("custoEnvio"), 0.0);
            Double tarifaPlataforma = getDoubleValue(vendaData.get("tarifaPlataforma"), 0.0);
            Double despesasOperacionais = getDoubleValue(vendaData.get("despesasOperacionais"), 0.0);

            // ✅ 3️⃣ VALIDAR E EXTRAIR ITENS DA VENDA
            List<Map<String, Object>> itensData = (List<Map<String, Object>>) vendaData.get("itens");

            // ✅✅✅ ADICIONAR LOGS CRÍTICOS AQUI
            System.out.println("🔍 DEBUG ITENS - Tipo: " + (itensData != null ? itensData.getClass().getName() : "null"));
            System.out.println("🔍 DEBUG ITENS - Tamanho: " + (itensData != null ? itensData.size() : "null"));

            if (itensData == null || itensData.isEmpty()) {
                return ResponseEntity.badRequest().body("A venda deve conter pelo menos um produto");
            }

            // ✅ DEBUG DETALHADO DE CADA ITEM
            for (int i = 0; i < itensData.size(); i++) {
                Map<String, Object> item = itensData.get(i);
                System.out.println("🔍 DEBUG ITEM " + i + ": " + item);
                System.out.println("   produtoId: " + item.get("produtoId"));
                System.out.println("   quantidade: " + item.get("quantidade"));

                // ✅ VALIDAR CAMPOS OBRIGATÓRIOS NO ITEM
                if (!item.containsKey("produtoId") || item.get("produtoId") == null) {
                    return ResponseEntity.badRequest().body("Item " + i + " não contém produtoId");
                }
                if (!item.containsKey("quantidade") || item.get("quantidade") == null) {
                    return ResponseEntity.badRequest().body("Item " + i + " não contém quantidade");
                }
            }

            // ✅ 4️⃣ VERIFICAR SE JÁ EXISTE VENDA COM MESMO ID DO PEDIDO
            if (vendaRepository.findByIdPedidoAndUser(idPedido, currentUser).isPresent()) {
                return ResponseEntity.badRequest().body("Já existe uma venda com este ID do pedido");
            }

            // ✅ 5️⃣ CRIAR A VENDA (SEM PRODUTOS AINDA)
            Venda venda = new Venda();
            venda.setData(dataVenda);
            venda.setIdPedido(idPedido);
            venda.setPlataforma(plataforma);
            venda.setPrecoVenda(precoVenda);
            venda.setFretePagoPeloCliente(fretePagoPeloCliente);
            venda.setCustoEnvio(custoEnvio);
            venda.setTarifaPlataforma(tarifaPlataforma);
            venda.setDespesasOperacionais(despesasOperacionais);
            venda.setUser(currentUser);

            // ✅ 6️⃣ CRIAR LISTA DE ITENS PRELIMINARES (APENAS COM PRODUTO E QUANTIDADE)
            List<ItemVenda> itensPreliminares = new ArrayList<>();
            for (Map<String, Object> itemData : itensData) {
                // ✅✅✅ EXTRAÇÃO SEGURA COM TRY-CATCH
                Long produtoId;
                Integer quantidade;

                try {
                    produtoId = Long.valueOf(itemData.get("produtoId").toString());
                } catch (Exception e) {
                    return ResponseEntity.badRequest().body("Erro no produtoId: " + e.getMessage());
                }

                try {
                    quantidade = Integer.valueOf(itemData.get("quantidade").toString());
                } catch (Exception e) {
                    return ResponseEntity.badRequest().body("Erro na quantidade: " + e.getMessage());
                }

                // ✅ VERIFICAR SE O PRODUTO EXISTE E PERTENCE AO USUÁRIO
                Optional<Produto> produtoOpt = produtoRepository.findByIdAndUser(produtoId, currentUser);
                if (!produtoOpt.isPresent()) {
                    return ResponseEntity.badRequest()
                            .body("Produto não encontrado ou não pertence ao usuário: ID " + produtoId);
                }
                Produto produto = produtoOpt.get();

                // ✅ VERIFICAR ESTOQUE PARA ESTE PRODUTO
                Integer saldoDisponivel = estoqueService.verificarSaldoTotal(produto);
                if (saldoDisponivel < quantidade) {
                    return ResponseEntity.badRequest()
                            .body("Estoque insuficiente para produto: " + produto.getNome() +
                                    ". Disponível: " + saldoDisponivel + ", Necessário: " + quantidade);
                }

                // ✅ ENCONTRAR O PRÓXIMO LOTE DISPONÍVEL (PEPS)
                EntradaEstoque lotePreliminar = entradaEstoqueRepository
                        .findByProdutoAndUserAndSaldoGreaterThanOrderByDataEntradaAsc(produto, currentUser, 0)
                        .stream()
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Produto sem estoque: " + produto.getNome()));

                // ✅ CRIAR ITEM PRELIMINAR
                ItemVenda itemPreliminar = new ItemVenda();
                itemPreliminar.setVenda(venda);
                itemPreliminar.setLote(lotePreliminar);
                itemPreliminar.setQuantidade(quantidade);
                itemPreliminar.setCustoUnitario(lotePreliminar.getCustoUnitario());
                itemPreliminar.setUser(currentUser);

                itensPreliminares.add(itemPreliminar);
            }

            // ✅ 7️⃣ ADICIONAR ITENS À VENDA
            venda.setItens(itensPreliminares);

            // ✅ 8️⃣ SALVAR VENDA E PROCESSAR PEPS
            estoqueService.calcularCustoVendaERegistrarItens(venda);

            // ✅ 9️⃣ BUSCAR VENDA COMPLETA (COM ITENS PERSISTIDOS)
            Optional<Venda> vendaCompleta = vendaRepository.findByIdPedidoAndUser(idPedido, currentUser);
            if (!vendaCompleta.isPresent()) {
                throw new RuntimeException("Erro ao recuperar venda criada: " + idPedido);
            }

            System.out.println("✅ Venda criada com sucesso: " + vendaCompleta.get().getIdPedido() +
                    ", Total produtos: " + vendaCompleta.get().getItens().size());

            return ResponseEntity.ok(new VendaDTO(vendaCompleta.get()));

        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body("Erro de formato numérico: " + e.getMessage());
        } catch (ClassCastException e) {
            return ResponseEntity.badRequest().body("Erro de tipo de dados: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("❌ Erro ao criar venda: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Erro ao criar venda: " + e.getMessage());
        }
    }

    // ✅ MÉTODO AUXILIAR: Extrair data do Map
    private LocalDateTime extrairData(Object dataObj) {
        if (dataObj == null) {
            return LocalDateTime.now();
        }

        String dataString = dataObj.toString();
        System.out.println("📅 DEBUG DATA - String recebida: '" + dataString + "'");

        try {
            return LocalDateTime.parse(dataString);
        } catch (Exception e1) {
            try {
                return LocalDateTime.parse(dataString + ":00");
            } catch (Exception e2) {
                System.out.println("⚠️ DEBUG DATA - Erro no parse, usando data atual");
                return LocalDateTime.now();
            }
        }
    }

    // ✅ MÉTODO AUXILIAR: Extrair Double com valor padrão
    private Double getDoubleValue(Object value, Double defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            return Double.valueOf(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    // ✅✅✅ ATUALIZADO: PUT - Atualizar venda existente DO USUÁRIO (SIMPLIFICADO)
    @PutMapping("/{id}")
    public ResponseEntity<?> atualizarVenda(@PathVariable Long id, @RequestBody Map<String, Object> vendaData) {
        try {
            User currentUser = getCurrentUser();

            // ✅ 1️⃣ BUSCAR VENDA EXISTENTE
            Optional<Venda> vendaExistenteOpt = vendaRepository.findByIdAndUser(id, currentUser);
            if (!vendaExistenteOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            Venda vendaExistente = vendaExistenteOpt.get();

            // ✅ 2️⃣ VERIFICAR SE JÁ EXISTE OUTRA VENDA COM MESMO ID DO PEDIDO
            if (vendaData.containsKey("idPedido")) {
                String novoIdPedido = vendaData.get("idPedido").toString();
                Optional<Venda> vendaComMesmoPedido = vendaRepository.findByIdPedidoAndUser(novoIdPedido, currentUser);
                if (vendaComMesmoPedido.isPresent() && !vendaComMesmoPedido.get().getId().equals(id)) {
                    return ResponseEntity.badRequest().body("Já existe outra venda com este ID do pedido");
                }
                vendaExistente.setIdPedido(novoIdPedido);
            }

            // ✅ 3️⃣ ATUALIZAR APENAS CAMPOS PERMITIDOS (NÃO PERMITE ALTERAR PRODUTOS)
            if (vendaData.containsKey("data")) {
                vendaExistente.setData(extrairData(vendaData.get("data")));
            }
            if (vendaData.containsKey("plataforma")) {
                vendaExistente.setPlataforma(vendaData.get("plataforma").toString());
            }
            if (vendaData.containsKey("precoVenda")) {
                vendaExistente.setPrecoVenda(Double.valueOf(vendaData.get("precoVenda").toString()));
            }
            if (vendaData.containsKey("fretePagoPeloCliente")) {
                vendaExistente.setFretePagoPeloCliente(getDoubleValue(vendaData.get("fretePagoPeloCliente"), 0.0));
            }
            if (vendaData.containsKey("custoEnvio")) {
                vendaExistente.setCustoEnvio(getDoubleValue(vendaData.get("custoEnvio"), 0.0));
            }
            if (vendaData.containsKey("tarifaPlataforma")) {
                vendaExistente.setTarifaPlataforma(getDoubleValue(vendaData.get("tarifaPlataforma"), 0.0));
            }
            if (vendaData.containsKey("despesasOperacionais")) {
                vendaExistente.setDespesasOperacionais(getDoubleValue(vendaData.get("despesasOperacionais"), 0.0));
            }

            // ✅ 4️⃣ SALVAR VENDA ATUALIZADA
            Venda vendaSalva = vendaRepository.save(vendaExistente);

            return ResponseEntity.ok(new VendaDTO(vendaSalva));

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

            // ✅ REVERTER ESTOQUE BASEADO NOS ITENS RASTREADOS
            estoqueService.reverterEstoqueVenda(venda);

            // Excluir a venda
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

            List<VendaDTO> vendasDTO = vendas.stream()
                    .map(VendaDTO::new)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(vendasDTO);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao buscar vendas por plataforma: " + e.getMessage());
        }
    }

    @GetMapping("/periodo")
    public ResponseEntity<?> buscarPorPeriodo(
            @RequestParam LocalDateTime inicio,
            @RequestParam LocalDateTime fim) {
        try {
            User currentUser = getCurrentUser();
            List<Venda> vendas = vendaRepository.findByDataBetweenAndUser(inicio, fim, currentUser);

            List<VendaDTO> vendasDTO = vendas.stream()
                    .map(VendaDTO::new)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(vendasDTO);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao buscar vendas por período: " + e.getMessage());
        }
    }

    // ✅✅✅ ATUALIZADO: GET - Buscar venda por nome DO USUÁRIO - AGORA USANDO O NOVO MÉTODO
    @GetMapping("/produto/{nome}")
    public ResponseEntity<?> buscarPorNomeProduto(@PathVariable String nome) {
        try {
            User currentUser = getCurrentUser();

            // 1. Buscar produtos com nome similar
            List<Produto> produtos = produtoRepository.findByNomeContainingAndUser(nome, currentUser);

            if (produtos.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            // ✅ 2. USANDO O NOVO MÉTODO: Buscar vendas que contenham algum desses produtos
            List<Venda> vendasEncontradas = new ArrayList<>();
            for (Produto produto : produtos) {
                // ✅ USA O NOVO MÉTODO findByProdutoInItens() do VendaRepository
                vendasEncontradas.addAll(vendaRepository.findByProdutoInItens(produto, currentUser));
            }

            // Remover duplicatas (uma venda pode conter múltiplos produtos)
            List<Venda> vendasUnicas = vendasEncontradas.stream()
                    .distinct()
                    .collect(Collectors.toList());

            List<VendaDTO> vendasDTO = vendasUnicas.stream()
                    .map(VendaDTO::new)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(vendasDTO);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao buscar vendas por nome do produto: " + e.getMessage());
        }
    }

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

    @GetMapping("/resumo-mensal")
    public ResponseEntity<?> resumoMensal(@RequestParam int mes, @RequestParam int ano) {
        try {
            User currentUser = getCurrentUser();
            LocalDateTime inicio = LocalDateTime.of(ano, mes, 1, 0, 0, 0);
            LocalDateTime fim = LocalDateTime.of(ano, mes, 1, 23, 59, 59)
                    .plusMonths(1)
                    .minusDays(1);

            List<Venda> vendas = vendaRepository.findByDataBetweenAndUser(inicio, fim, currentUser);

            List<VendaDTO> vendasDTO = vendas.stream()
                    .map(VendaDTO::new)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(vendasDTO);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao buscar resumo mensal: " + e.getMessage());
        }
    }

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