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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/vendas")
@SuppressWarnings("unchecked")
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

    // ✅✅✅ CORRIGIDO COMPLETAMENTE: Método para extrair data
    private LocalDateTime extrairData(Object dataObj) {
        if (dataObj == null || dataObj.toString().isEmpty()) {
            return null;
        }

        String dataString = dataObj.toString().trim();
        System.out.println("📅 [DEBUG-v46.9] DEBUG DATA - String recebida: '" + dataString + "'");

        try {
            // ✅ FORMATO ESPERADO: "YYYY-MM-DD" (apenas data, sem hora)
            LocalDate dataApenas = LocalDate.parse(dataString);
            return dataApenas.atStartOfDay();

        } catch (Exception e1) {
            try {
                if (dataString.contains("T")) {
                    return LocalDateTime.parse(dataString);
                } else {
                    LocalDate dataApenas = LocalDate.parse(dataString, DateTimeFormatter.ISO_LOCAL_DATE);
                    return dataApenas.atStartOfDay();
                }
            } catch (Exception e2) {
                try {
                    String comT = dataString.replace(" ", "T");
                    return LocalDateTime.parse(comT);
                } catch (Exception e3) {
                    System.out.println("❌ [DEBUG-v46.9] ERRO DATA - Formato inválido: " + dataString +
                            ". Esperado: YYYY-MM-DD (apenas data)");
                    return null;
                }
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

    // ✅✅✅ ATUALIZADO v46.9: POST - CRIAR VENDA
    @PostMapping
    public ResponseEntity<?> criarVenda(@RequestBody Map<String, Object> vendaData) {
        try {
            System.out.println("🔍 [DEBUG-v46.9] INICIAL - Criar venda - Dados recebidos: " + vendaData);

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

            LocalDateTime dataVenda = extrairData(vendaData.get("data"));
            if (dataVenda == null) {
                return ResponseEntity.badRequest()
                        .body("Data da venda é obrigatória. Formato esperado: YYYY-MM-DD (apenas data)");
            }

            // ✅ EXTRAIR DADOS OPCIONAIS
            Double fretePagoPeloCliente = getDoubleValue(vendaData.get("fretePagoPeloCliente"), 0.0);
            Double custoEnvio = getDoubleValue(vendaData.get("custoEnvio"), 0.0);
            Double tarifaPlataforma = getDoubleValue(vendaData.get("tarifaPlataforma"), 0.0);
            Double despesasOperacionais = getDoubleValue(vendaData.get("despesasOperacionais"), 0.0);

            // ✅ 3️⃣ VALIDAR E EXTRAIR ITENS DA VENDA
            List<Map<String, Object>> itensData = (List<Map<String, Object>>) vendaData.get("itens");

            if (itensData == null || itensData.isEmpty()) {
                return ResponseEntity.badRequest().body("A venda deve conter pelo menos um produto");
            }

            // ✅ DEBUG DETALHADO DE CADA ITEM
            for (int i = 0; i < itensData.size(); i++) {
                Map<String, Object> item = itensData.get(i);
                System.out.println("🔍 [DEBUG-v46.9] ITEM " + i + ": " + item);

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

            // ✅✅✅ CORREÇÃO: CRIAR ITENS COM VALORES PADRÃO
            List<ItemVenda> itensPreliminares = new ArrayList<>();
            for (Map<String, Object> itemData : itensData) {
                // Extrair dados
                Long produtoId;
                Integer quantidade;

                try {
                    produtoId = Long.valueOf(itemData.get("produtoId").toString());
                    quantidade = Integer.valueOf(itemData.get("quantidade").toString());
                } catch (Exception e) {
                    return ResponseEntity.badRequest().body("Erro nos dados do item: " + e.getMessage());
                }

                // Extrair precoUnitarioVenda
                BigDecimal precoUnitario = BigDecimal.ZERO;
                if (itemData.containsKey("precoUnitarioVenda") && itemData.get("precoUnitarioVenda") != null) {
                    try {
                        precoUnitario = new BigDecimal(itemData.get("precoUnitarioVenda").toString());
                        System.out.println("💰 [DEBUG-v46.9] precoUnitarioVenda extraído: " + precoUnitario);
                    } catch (Exception e) {
                        System.out.println("⚠️ [DEBUG-v46.9] precoUnitarioVenda inválido, usando 0: " + e.getMessage());
                    }
                }

                // Buscar produto
                Optional<Produto> produtoOpt = produtoRepository.findByIdAndUser(produtoId, currentUser);
                if (!produtoOpt.isPresent()) {
                    return ResponseEntity.badRequest()
                            .body("Produto não encontrado ou não pertence ao usuário: ID " + produtoId);
                }
                Produto produto = produtoOpt.get();

                // ✅✅✅ VERIFICAR ESTOQUE
                if (!estoqueService.verificarEstoqueSuficiente(produto, quantidade)) {
                    Integer saldoDisponivel = estoqueService.verificarSaldoTotal(produto);
                    return ResponseEntity.badRequest()
                            .body("Estoque insuficiente para produto: " + produto.getNome() +
                                    ". Disponível: " + saldoDisponivel + ", Necessário: " + quantidade);
                }

                // ✅✅✅ CORREÇÃO: Criar ItemVenda com valores padrão
                ItemVenda itemPreliminar = new ItemVenda();
                itemPreliminar.setVenda(venda);
                itemPreliminar.setProduto(produto);
                itemPreliminar.setQuantidade(quantidade);
                itemPreliminar.setPrecoUnitario(precoUnitario);
                itemPreliminar.setUser(currentUser);
                itemPreliminar.setCustoUnitario(BigDecimal.ZERO);
                itemPreliminar.setProcessadoPeps(false);

                itensPreliminares.add(itemPreliminar);
                System.out.println("✅ [DEBUG-v46.9] Item criado: " + produto.getNome() + " x" + quantidade);
            }

            // ✅ 6️⃣ ADICIONAR ITENS À VENDA
            venda.setItens(itensPreliminares);

            // ✅ 7️⃣ PROCESSAR VENDA COM PEPS CORRETO
            System.out.println("🔄 [DEBUG-v46.9] Processando venda com PEPS...");
            try {
                estoqueService.processarVendaComPeps(venda);
            } catch (Exception e) {
                System.out.println("❌ [DEBUG-v46.9] Erro no processamento PEPS: " + e.getMessage());
                throw e;
            }

            // ✅ 8️⃣ BUSCAR VENDA COMPLETA
            Optional<Venda> vendaCompleta = vendaRepository.findByIdPedidoAndUser(idPedido, currentUser);
            if (!vendaCompleta.isPresent()) {
                throw new RuntimeException("Erro ao recuperar venda criada: " + idPedido);
            }

            System.out.println("✅✅✅ [DEBUG-v46.9] Venda criada com sucesso: " + vendaCompleta.get().getIdPedido());

            return ResponseEntity.ok(new VendaDTO(vendaCompleta.get()));

        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body("Erro de formato numérico: " + e.getMessage());
        } catch (ClassCastException e) {
            return ResponseEntity.badRequest().body("Erro de tipo de dados: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("❌ [DEBUG-v46.9] Erro ao criar venda: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Erro ao criar venda: " + e.getMessage());
        }
    }

    // ✅✅✅ CORREÇÃO CRÍTICA v46.9: PUT - ATUALIZAR VENDA INTELIGENTE
    @PutMapping("/{id}")
    public ResponseEntity<?> atualizarVenda(@PathVariable Long id, @RequestBody Map<String, Object> vendaData) {
        try {
            System.out.println("🔄 [DEBUG-v46.9] ATUALIZAR-VENDA - Iniciando atualização da venda ID: " + id);
            System.out.println("🔍 [DEBUG-v46.9] Dados recebidos: " + vendaData.keySet());

            User currentUser = getCurrentUser();

            // ✅ 1️⃣ BUSCAR VENDA EXISTENTE
            Optional<Venda> vendaExistenteOpt = vendaRepository.findByIdAndUser(id, currentUser);
            if (!vendaExistenteOpt.isPresent()) {
                System.out.println("❌ [DEBUG-v46.9] Venda não encontrada: " + id);
                return ResponseEntity.notFound().build();
            }

            Venda vendaExistente = vendaExistenteOpt.get();
            System.out.println("✅ [DEBUG-v46.9] Venda encontrada: " + vendaExistente.getIdPedido());
            System.out.println("📊 [DEBUG-v46.9] Itens atuais: " + vendaExistente.getItens().size() + " lotes");

            // ✅✅✅ 2️⃣ DETECTAR SE ITENS FORAM ENVIADOS
            boolean itensEnviados = vendaData.containsKey("itens") && vendaData.get("itens") != null;

            if (!itensEnviados) {
                System.out.println("ℹ️ [DEBUG-v46.9] ATENÇÃO: Itens NÃO enviados na atualização!");
                System.out.println("ℹ️ [DEBUG-v46.9] Atualizando apenas campos básicos...");

                // ✅ APENAS ATUALIZAR CAMPOS BÁSICOS - NÃO REPROCESSAR PEPS
                boolean camposModificados = false;

                if (vendaData.containsKey("idPedido")) {
                    String novoIdPedido = vendaData.get("idPedido").toString();
                    Optional<Venda> vendaComMesmoPedido = vendaRepository.findByIdPedidoAndUser(novoIdPedido, currentUser);
                    if (vendaComMesmoPedido.isPresent() && !vendaComMesmoPedido.get().getId().equals(id)) {
                        System.out.println("❌ [DEBUG-v46.9] ID do pedido já existe: " + novoIdPedido);
                        return ResponseEntity.badRequest().body("Já existe outra venda com este ID do pedido");
                    }
                    vendaExistente.setIdPedido(novoIdPedido);
                    camposModificados = true;
                }

                if (vendaData.containsKey("plataforma")) {
                    vendaExistente.setPlataforma(vendaData.get("plataforma").toString());
                    camposModificados = true;
                }

                if (vendaData.containsKey("data")) {
                    LocalDateTime novaData = extrairData(vendaData.get("data"));
                    if (novaData == null) {
                        return ResponseEntity.badRequest()
                                .body("Formato de data inválido. Esperado: YYYY-MM-DD");
                    }
                    vendaExistente.setData(novaData);
                    camposModificados = true;
                }

                if (vendaData.containsKey("precoVenda")) {
                    vendaExistente.setPrecoVenda(Double.valueOf(vendaData.get("precoVenda").toString()));
                    camposModificados = true;
                }

                if (vendaData.containsKey("fretePagoPeloCliente")) {
                    vendaExistente.setFretePagoPeloCliente(getDoubleValue(vendaData.get("fretePagoPeloCliente"), 0.0));
                    camposModificados = true;
                }

                if (vendaData.containsKey("custoEnvio")) {
                    vendaExistente.setCustoEnvio(getDoubleValue(vendaData.get("custoEnvio"), 0.0));
                    camposModificados = true;
                }

                if (vendaData.containsKey("tarifaPlataforma")) {
                    vendaExistente.setTarifaPlataforma(getDoubleValue(vendaData.get("tarifaPlataforma"), 0.0));
                    camposModificados = true;
                }

                if (vendaData.containsKey("despesasOperacionais")) {
                    vendaExistente.setDespesasOperacionais(getDoubleValue(vendaData.get("despesasOperacionais"), 0.0));
                    camposModificados = true;
                }

                if (!camposModificados) {
                    System.out.println("⚠️ [DEBUG-v46.9] Nenhum campo modificado, retornando venda atual");
                    return ResponseEntity.ok(new VendaDTO(vendaExistente));
                }

                // ✅ SALVAR VENDA ATUALIZADA (SEM REPROCESSAR PEPS)
                Venda vendaSalva = vendaRepository.save(vendaExistente);
                System.out.println("✅✅✅ [DEBUG-v46.9] VENDA ATUALIZADA SEM REPROCESSAR PEPS!");
                System.out.println("📊 [DEBUG-v46.9] Lotes mantidos: " + vendaSalva.getItens().size());

                return ResponseEntity.ok(new VendaDTO(vendaSalva));
            }

            // ✅✅✅ 3️⃣ ITENS ENVIADOS - VERIFICAR SE QUANTIDADES MUDARAM
            List<Map<String, Object>> itensData = (List<Map<String, Object>>) vendaData.get("itens");
            System.out.println("🔍 [DEBUG-v46.9] Itens recebidos: " + itensData.size());

            // Mapear quantidades originais por produto
            Map<Long, Integer> quantidadesOriginais = new HashMap<>();
            Map<Long, ItemVenda> itensPorProduto = new HashMap<>();

            for (ItemVenda itemOriginal : vendaExistente.getItens()) {
                if (itemOriginal.getProduto() != null) {
                    Long produtoId = itemOriginal.getProduto().getId();
                    quantidadesOriginais.merge(produtoId, itemOriginal.getQuantidade(), Integer::sum);
                    itensPorProduto.putIfAbsent(produtoId, itemOriginal);
                }
            }

            // Mapear novas quantidades
            Map<Long, Integer> quantidadesNovas = new HashMap<>();
            boolean quantidadeMudou = false;

            for (Map<String, Object> itemData : itensData) {
                Long produtoId = Long.valueOf(itemData.get("produtoId").toString());
                Integer quantidade = Integer.valueOf(itemData.get("quantidade").toString());
                quantidadesNovas.merge(produtoId, quantidade, Integer::sum);
            }

            // Comparar quantidades
            for (Long produtoId : quantidadesNovas.keySet()) {
                Integer quantidadeOriginal = quantidadesOriginais.getOrDefault(produtoId, 0);
                Integer quantidadeNova = quantidadesNovas.get(produtoId);

                if (!quantidadeOriginal.equals(quantidadeNova)) {
                    quantidadeMudou = true;
                    System.out.println("⚠️ [DEBUG-v46.9] Quantidade alterada! Produto " + produtoId +
                            ": " + quantidadeOriginal + " → " + quantidadeNova);
                    break;
                }
            }

            // ✅✅✅ 4️⃣ DECISÃO: REPROCESSAR PEPS OU NÃO?
            if (!quantidadeMudou) {
                System.out.println("✅✅✅ [DEBUG-v46.9] ATENÇÃO: Quantidades NÃO alteradas!");
                System.out.println("✅✅✅ [DEBUG-v46.9] NÃO reaplicando PEPS - mantendo lotes originais!");

                // ✅ ATUALIZAR CAMPOS BÁSICOS (como no caso sem itens)
                boolean camposModificados = false;

                if (vendaData.containsKey("idPedido")) {
                    String novoIdPedido = vendaData.get("idPedido").toString();
                    Optional<Venda> vendaComMesmoPedido = vendaRepository.findByIdPedidoAndUser(novoIdPedido, currentUser);
                    if (vendaComMesmoPedido.isPresent() && !vendaComMesmoPedido.get().getId().equals(id)) {
                        return ResponseEntity.badRequest().body("Já existe outra venda com este ID do pedido");
                    }
                    vendaExistente.setIdPedido(novoIdPedido);
                    camposModificados = true;
                }

                if (vendaData.containsKey("plataforma")) {
                    vendaExistente.setPlataforma(vendaData.get("plataforma").toString());
                    camposModificados = true;
                }

                if (vendaData.containsKey("data")) {
                    LocalDateTime novaData = extrairData(vendaData.get("data"));
                    if (novaData != null) {
                        vendaExistente.setData(novaData);
                        camposModificados = true;
                    }
                }

                if (vendaData.containsKey("precoVenda")) {
                    vendaExistente.setPrecoVenda(Double.valueOf(vendaData.get("precoVenda").toString()));
                    camposModificados = true;
                }

                // ✅ ATUALIZAR PREÇOS UNITÁRIOS SE FORNECIDOS
                for (Map<String, Object> itemData : itensData) {
                    if (itemData.containsKey("precoUnitarioVenda") && itemData.get("precoUnitarioVenda") != null) {
                        try {
                            Long produtoId = Long.valueOf(itemData.get("produtoId").toString());
                            BigDecimal novoPreco = new BigDecimal(itemData.get("precoUnitarioVenda").toString());

                            // Atualizar todos os itens deste produto
                            for (ItemVenda item : vendaExistente.getItens()) {
                                if (item.getProduto() != null && item.getProduto().getId().equals(produtoId)) {
                                    item.setPrecoUnitario(novoPreco);
                                    camposModificados = true;
                                }
                            }
                        } catch (Exception e) {
                            System.out.println("⚠️ [DEBUG-v46.9] Erro ao atualizar preço: " + e.getMessage());
                        }
                    }
                }

                if (!camposModificados) {
                    System.out.println("⚠️ [DEBUG-v46.9] Nenhum campo modificado");
                    return ResponseEntity.ok(new VendaDTO(vendaExistente));
                }

                Venda vendaSalva = vendaRepository.save(vendaExistente);
                System.out.println("✅✅✅ [DEBUG-v46.9] VENDA ATUALIZADA SEM REPROCESSAR PEPS!");
                System.out.println("📊 [DEBUG-v46.9] Lotes mantidos: " + vendaSalva.getItens().size());

                return ResponseEntity.ok(new VendaDTO(vendaSalva));
            }

            // ✅✅✅ 5️⃣ QUANTIDADE MUDOU - PRECISA REPROCESSAR PEPS COMPLETO
            System.out.println("⚠️ [DEBUG-v46.9] Quantidades alteradas - reprocessando PEPS...");

            // ✅ VERIFICAR SE JÁ EXISTE OUTRA VENDA COM MESMO ID DO PEDIDO
            if (vendaData.containsKey("idPedido")) {
                String novoIdPedido = vendaData.get("idPedido").toString();
                Optional<Venda> vendaComMesmoPedido = vendaRepository.findByIdPedidoAndUser(novoIdPedido, currentUser);
                if (vendaComMesmoPedido.isPresent() && !vendaComMesmoPedido.get().getId().equals(id)) {
                    return ResponseEntity.badRequest().body("Já existe outra venda com este ID do pedido");
                }
                vendaExistente.setIdPedido(novoIdPedido);
            }

            // ✅ ATUALIZAR CAMPOS BÁSICOS
            if (vendaData.containsKey("plataforma")) {
                vendaExistente.setPlataforma(vendaData.get("plataforma").toString());
            }

            if (vendaData.containsKey("data")) {
                LocalDateTime novaData = extrairData(vendaData.get("data"));
                if (novaData != null) {
                    vendaExistente.setData(novaData);
                }
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

            // ✅ REMOVER ITENS ANTIGOS E REVERTER ESTOQUE
            System.out.println("🔄 [DEBUG-v46.9] Revertendo estoque dos itens antigos...");
            estoqueService.reverterEstoqueVenda(vendaExistente);
            vendaExistente.getItens().clear();

            // ✅ CRIAR NOVOS ITENS PRELIMINARES
            List<ItemVenda> novosItensPreliminares = new ArrayList<>();

            for (Map<String, Object> itemData : itensData) {
                Long produtoId;
                Integer quantidade;

                try {
                    produtoId = Long.valueOf(itemData.get("produtoId").toString());
                    quantidade = Integer.valueOf(itemData.get("quantidade").toString());
                } catch (Exception e) {
                    return ResponseEntity.badRequest()
                            .body("Erro nos dados do produto: " + e.getMessage());
                }

                // Extrair precoUnitarioVenda
                BigDecimal precoUnitario = BigDecimal.ZERO;
                if (itemData.containsKey("precoUnitarioVenda") && itemData.get("precoUnitarioVenda") != null) {
                    try {
                        precoUnitario = new BigDecimal(itemData.get("precoUnitarioVenda").toString());
                    } catch (Exception e) {
                        System.out.println("⚠️ [DEBUG-v46.9] precoUnitarioVenda inválido, usando 0: " + e.getMessage());
                    }
                }

                // Buscar produto
                Optional<Produto> produtoOpt = produtoRepository.findByIdAndUser(produtoId, currentUser);
                if (!produtoOpt.isPresent()) {
                    return ResponseEntity.badRequest()
                            .body("Produto não encontrado: ID " + produtoId);
                }

                Produto produto = produtoOpt.get();

                // ✅ VERIFICAR ESTOQUE
                if (!estoqueService.verificarEstoqueSuficiente(produto, quantidade)) {
                    Integer saldoDisponivel = estoqueService.verificarSaldoTotal(produto);
                    return ResponseEntity.badRequest()
                            .body("Estoque insuficiente para produto: " + produto.getNome() +
                                    ". Disponível: " + saldoDisponivel + ", Necessário: " + quantidade);
                }

                // ✅ CRIAR NOVO ITEM PRELIMINAR
                ItemVenda novoItem = new ItemVenda();
                novoItem.setVenda(vendaExistente);
                novoItem.setProduto(produto);
                novoItem.setQuantidade(quantidade);
                novoItem.setPrecoUnitario(precoUnitario);
                novoItem.setUser(currentUser);
                novoItem.setCustoUnitario(BigDecimal.ZERO);
                novoItem.setProcessadoPeps(false);

                novosItensPreliminares.add(novoItem);
            }

            // ✅ ADICIONAR NOVOS ITENS À VENDA
            vendaExistente.setItens(novosItensPreliminares);

            // ✅ RECALCULAR CUSTOS E ATUALIZAR ESTOQUE
            System.out.println("🔄 [DEBUG-v46.9] Recalculando custos PEPS...");
            estoqueService.processarVendaComPeps(vendaExistente);

            // ✅ SALVAR VENDA ATUALIZADA
            Venda vendaSalva = vendaRepository.save(vendaExistente);

            System.out.println("✅✅✅ [DEBUG-v46.9] VENDA ATUALIZADA COM SUCESSO: " + vendaSalva.getIdPedido());
            System.out.println("📊 [DEBUG-v46.9] Novos lotes criados: " + vendaSalva.getItens().size());

            return ResponseEntity.ok(new VendaDTO(vendaSalva));

        } catch (Exception e) {
            System.out.println("❌ [DEBUG-v46.9] Erro ao atualizar venda: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Erro ao atualizar venda: " + e.getMessage());
        }
    }

    // ✅ ATUALIZADO v46.9: DELETE - Excluir venda com reversão de estoque
    @DeleteMapping("/{id}")
    public ResponseEntity<?> excluirVenda(@PathVariable Long id) {
        try {
            User currentUser = getCurrentUser();

            Optional<Venda> vendaOpt = vendaRepository.findByIdAndUser(id, currentUser);
            if (!vendaOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            Venda venda = vendaOpt.get();

            // ✅ REVERTER ESTOQUE
            estoqueService.reverterEstoqueVenda(venda);

            // Excluir a venda
            vendaRepository.deleteById(id);

            System.out.println("✅ [DEBUG-v46.9] Venda excluída e estoque revertido: " + venda.getIdPedido());
            return ResponseEntity.noContent().build();

        } catch (Exception e) {
            System.out.println("❌ [DEBUG-v46.9] Erro ao excluir venda: " + e.getMessage());
            return ResponseEntity.badRequest().body("Erro ao excluir venda: " + e.getMessage());
        }
    }

    // ========== RESTANTE DO CÓDIGO (MÉTODOS GET E DASHBOARD) ==========

    @GetMapping
    public ResponseEntity<List<VendaDTO>> listarTodas() {
        try {
            User currentUser = getCurrentUser();
            List<Venda> vendas = vendaRepository.findByUserWithProduto(currentUser);
            List<VendaDTO> vendasDTO = vendas.stream()
                    .map(VendaDTO::new)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(vendasDTO);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<VendaDTO> buscarPorId(@PathVariable Long id) {
        try {
            User currentUser = getCurrentUser();
            Optional<Venda> venda = vendaRepository.findByIdAndUser(id, currentUser);
            return venda.map(v -> ResponseEntity.ok(new VendaDTO(v)))
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/plataforma/{plataforma}")
    public ResponseEntity<List<VendaDTO>> buscarPorPlataforma(@PathVariable String plataforma) {
        try {
            User currentUser = getCurrentUser();
            List<Venda> vendas = vendaRepository.findByPlataformaAndUser(plataforma, currentUser);
            List<VendaDTO> vendasDTO = vendas.stream()
                    .map(VendaDTO::new)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(vendasDTO);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/periodo")
    public ResponseEntity<List<VendaDTO>> buscarPorPeriodo(
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
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/produto/{nome}")
    public ResponseEntity<List<VendaDTO>> buscarPorNomeProduto(@PathVariable String nome) {
        try {
            User currentUser = getCurrentUser();
            List<Produto> produtos = produtoRepository.findByNomeContainingAndUser(nome, currentUser);

            if (produtos.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            List<Venda> vendasEncontradas = new ArrayList<>();
            for (Produto produto : produtos) {
                vendasEncontradas.addAll(vendaRepository.findByProdutoInItens(produto, currentUser));
            }

            List<Venda> vendasUnicas = vendasEncontradas.stream()
                    .distinct()
                    .collect(Collectors.toList());

            List<VendaDTO> vendasDTO = vendasUnicas.stream()
                    .map(VendaDTO::new)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(vendasDTO);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
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
    public ResponseEntity<List<VendaDTO>> resumoMensal(@RequestParam int mes, @RequestParam int ano) {
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
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/pedido/{idPedido}")
    public ResponseEntity<VendaDTO> buscarPorIdPedido(@PathVariable String idPedido) {
        try {
            User currentUser = getCurrentUser();
            Optional<Venda> venda = vendaRepository.findByIdPedidoAndUser(idPedido, currentUser);
            return venda.map(v -> ResponseEntity.ok(new VendaDTO(v)))
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    // ========== MÉTODOS DO DASHBOARD ==========
    private Map<String, Double> calcularFaturamentoPorPlataforma(User user) {
        List<Venda> vendasUsuario = vendaRepository.findByUserWithProduto(user);
        Map<String, Double> faturamentoPorPlataforma = new HashMap<>();

        faturamentoPorPlataforma.put("AMAZON", 0.0);
        faturamentoPorPlataforma.put("MERCADO_LIVRE", 0.0);
        faturamentoPorPlataforma.put("SHOPEE", 0.0);

        for (Venda venda : vendasUsuario) {
            String plataforma = venda.getPlataforma();
            double faturamentoVenda = venda.calcularFaturamento();
            faturamentoPorPlataforma.put(plataforma,
                    faturamentoPorPlataforma.getOrDefault(plataforma, 0.0) + faturamentoVenda);
        }

        return faturamentoPorPlataforma;
    }

    private List<Map<String, Object>> calcularProdutosMaisVendidos(User user) {
        List<Venda> vendasUsuario = vendaRepository.findByUserWithProduto(user);
        Map<Long, Map<String, Object>> produtosMap = new HashMap<>();

        for (Venda venda : vendasUsuario) {
            if (venda.getItens() != null) {
                for (ItemVenda item : venda.getItens()) {
                    Produto produto = item.getProduto();
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

                    double proporcaoItem = item.getCustoTotal().doubleValue() / venda.getCustoProdutoVendido();
                    double faturamentoItem = venda.calcularFaturamento() * proporcaoItem;
                    double lucroItem = venda.calcularLucroLiquido() * proporcaoItem;

                    produtoInfo.put("quantidadeVendida", quantidadeAtual + item.getQuantidade());
                    produtoInfo.put("faturamento", faturamentoAtual + faturamentoItem);
                    produtoInfo.put("lucroLiquido", lucroLiquidoAtual + lucroItem);
                }
            }
        }

        List<Map<String, Object>> produtosMaisVendidos = new ArrayList<>(produtosMap.values());
        produtosMaisVendidos.sort((a, b) -> {
            int quantidadeA = (int) a.get("quantidadeVendida");
            int quantidadeB = (int) b.get("quantidadeVendida");
            return Integer.compare(quantidadeB, quantidadeA);
        });

        return produtosMaisVendidos.size() > 5 ? produtosMaisVendidos.subList(0, 5) : produtosMaisVendidos;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard() {
        try {
            User currentUser = getCurrentUser();
            Map<String, Object> dashboard = new HashMap<>();

            List<Venda> vendasUsuario = vendaRepository.findByUserWithProduto(currentUser);

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

            dashboard.put("faturamentoTotal", faturamentoTotal);
            dashboard.put("custoEfetivoTotal", custoEfetivoTotal);
            dashboard.put("lucroBrutoTotal", lucroBrutoTotal);
            dashboard.put("lucroLiquidoTotal", lucroLiquidoTotal);
            dashboard.put("despesasOperacionaisTotal", despesasOperacionaisTotal);

            double roiTotal = (custoEfetivoTotal > 0) ? (lucroLiquidoTotal / custoEfetivoTotal) * 100 : 0;
            dashboard.put("roiTotal", roiTotal);

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

    @GetMapping("/faturamento-ano-atual")
    public ResponseEntity<?> getFaturamentoAnoAtual() {
        try {
            User currentUser = getCurrentUser();
            Double faturamento = vendaRepository.calcularFaturamentoAnoAtual(currentUser);
            return ResponseEntity.ok(faturamento != null ? faturamento : 0.0);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao calcular faturamento do ano atual: " + e.getMessage());
        }
    }

    @GetMapping("/custo-efetivo-ano-atual")
    public ResponseEntity<?> getCustoEfetivoAnoAtual() {
        try {
            User currentUser = getCurrentUser();
            Double custoEfetivo = vendaRepository.calcularCustoEfetivoAnoAtual(currentUser);
            return ResponseEntity.ok(custoEfetivo != null ? custoEfetivo : 0.0);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao calcular custo efetivo do ano atual: " + e.getMessage());
        }
    }

    @GetMapping("/lucro-bruto-ano-atual")
    public ResponseEntity<?> getLucroBrutoAnoAtual() {
        try {
            User currentUser = getCurrentUser();
            Double lucroBruto = vendaRepository.calcularLucroBrutoAnoAtual(currentUser);
            return ResponseEntity.ok(lucroBruto != null ? lucroBruto : 0.0);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao calcular lucro bruto do ano atual: " + e.getMessage());
        }
    }
}