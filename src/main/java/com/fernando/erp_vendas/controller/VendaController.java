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
@SuppressWarnings("unchecked") // ✅ ADICIONADO PARA EVITAR WARNINGS
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

    // ✅✅✅ CORRIGIDO COMPLETAMENTE: Método para extrair data (agora aceita apenas data sem hora)
    private LocalDateTime extrairData(Object dataObj) {
        if (dataObj == null || dataObj.toString().isEmpty()) {
            return null; // ⚠️ IMPORTANTE: não usar LocalDateTime.now()
        }

        String dataString = dataObj.toString().trim();
        System.out.println("📅 [DEBUG-v46.6] DEBUG DATA - String recebida: '" + dataString + "'");

        try {
            // ✅ FORMATO ESPERADO: "YYYY-MM-DD" (apenas data, sem hora)
            // Converter para LocalDateTime com hora 00:00:00
            LocalDate dataApenas = LocalDate.parse(dataString);
            return dataApenas.atStartOfDay(); // 00:00:00

        } catch (Exception e1) {
            try {
                // ✅ Se vier com formato ISO (backward compatibility): "YYYY-MM-DDTHH:mm:ss"
                if (dataString.contains("T")) {
                    return LocalDateTime.parse(dataString);
                } else {
                    // Tentar outros formatos de data
                    LocalDate dataApenas = LocalDate.parse(dataString, DateTimeFormatter.ISO_LOCAL_DATE);
                    return dataApenas.atStartOfDay();
                }
            } catch (Exception e2) {
                try {
                    // ✅ Tentar formato com espaço: "YYYY-MM-DD HH:mm:ss"
                    String comT = dataString.replace(" ", "T");
                    return LocalDateTime.parse(comT);
                } catch (Exception e3) {
                    System.out.println("❌ [DEBUG-v46.6] ERRO DATA - Formato inválido: " + dataString +
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

    // ✅✅✅ CORRIGIDO COMPLETAMENTE: POST - Criar nova venda com MÚLTIPLOS PRODUTOS E DATA CORRETA
    @PostMapping
    public ResponseEntity<?> criarVenda(@RequestBody Map<String, Object> vendaData) {
        try {
            System.out.println("🔍 [DEBUG-v46.6] INICIAL - Criar venda - Dados recebidos: " + vendaData);

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

            // ✅✅✅ CORREÇÃO CRÍTICA: EXTRAIR E VALIDAR DATA
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

            // ✅✅✅ ADICIONAR LOGS CRÍTICOS AQUI
            System.out.println("🔍 [DEBUG-v46.6] ITENS - Tipo: " + (itensData != null ? itensData.getClass().getName() : "null"));
            System.out.println("🔍 [DEBUG-v46.6] ITENS - Tamanho: " + (itensData != null ? itensData.size() : "null"));

            if (itensData == null || itensData.isEmpty()) {
                return ResponseEntity.badRequest().body("A venda deve conter pelo menos um produto");
            }

            // ✅ DEBUG DETALHADO DE CADA ITEM
            for (int i = 0; i < itensData.size(); i++) {
                Map<String, Object> item = itensData.get(i);
                System.out.println("🔍 [DEBUG-v46.6] ITEM " + i + ": " + item);
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
            venda.setData(dataVenda); // ✅ AGORA COM DATA CORRETA
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

                // ✅✅✅ CORREÇÃO v46.6: Extrair precoUnitarioVenda do frontend
                BigDecimal precoUnitario = BigDecimal.ZERO;
                if (itemData.containsKey("precoUnitarioVenda") && itemData.get("precoUnitarioVenda") != null) {
                    try {
                        precoUnitario = new BigDecimal(itemData.get("precoUnitarioVenda").toString());
                        System.out.println("💰 [DEBUG-v46.6] precoUnitarioVenda extraído: " + precoUnitario);
                    } catch (Exception e) {
                        System.out.println("⚠️ [DEBUG-v46.6] precoUnitarioVenda inválido, usando 0: " + e.getMessage());
                    }
                } else {
                    System.out.println("⚠️ [DEBUG-v46.6] precoUnitarioVenda não encontrado no item");
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

                // ✅ CRIAR ITEM PRELIMINAR COM precoUnitario
                ItemVenda itemPreliminar = new ItemVenda();
                itemPreliminar.setVenda(venda);
                itemPreliminar.setLote(lotePreliminar);
                itemPreliminar.setQuantidade(quantidade);
                itemPreliminar.setCustoUnitario(lotePreliminar.getCustoUnitario());
                itemPreliminar.setPrecoUnitario(precoUnitario); // ✅✅✅ ADICIONADO
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

            System.out.println("✅ [DEBUG-v46.6] Venda criada com sucesso: " + vendaCompleta.get().getIdPedido() +
                    ", Data: " + vendaCompleta.get().getData() +
                    ", Total produtos: " + vendaCompleta.get().getItens().size());

            return ResponseEntity.ok(new VendaDTO(vendaCompleta.get()));

        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body("Erro de formato numérico: " + e.getMessage());
        } catch (ClassCastException e) {
            return ResponseEntity.badRequest().body("Erro de tipo de dados: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("❌ [DEBUG-v46.6] Erro ao criar venda: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Erro ao criar venda: " + e.getMessage());
        }
    }

    // ✅✅✅✅✅ CORREÇÃO v46.6: PUT - Atualizar venda existente COM PROCESSAMENTO DE ITENS
    @PutMapping("/{id}")
    public ResponseEntity<?> atualizarVenda(@PathVariable Long id, @RequestBody Map<String, Object> vendaData) {
        try {
            System.out.println("🔄 [DEBUG-v46.6] ATUALIZAR-VENDA - Iniciando atualização da venda ID: " + id);
            System.out.println("🔄 [DEBUG-v46.6] Dados recebidos do frontend: " + vendaData);

            User currentUser = getCurrentUser();

            // ✅ 1️⃣ BUSCAR VENDA EXISTENTE
            Optional<Venda> vendaExistenteOpt = vendaRepository.findByIdAndUser(id, currentUser);
            if (!vendaExistenteOpt.isPresent()) {
                System.out.println("❌ [DEBUG-v46.6] Venda não encontrada: " + id);
                return ResponseEntity.notFound().build();
            }

            Venda vendaExistente = vendaExistenteOpt.get();
            System.out.println("✅ [DEBUG-v46.6] Venda encontrada: " + vendaExistente.getIdPedido());

            // ✅ 2️⃣ VERIFICAR SE JÁ EXISTE OUTRA VENDA COM MESMO ID DO PEDIDO
            if (vendaData.containsKey("idPedido")) {
                String novoIdPedido = vendaData.get("idPedido").toString();
                Optional<Venda> vendaComMesmoPedido = vendaRepository.findByIdPedidoAndUser(novoIdPedido, currentUser);
                if (vendaComMesmoPedido.isPresent() && !vendaComMesmoPedido.get().getId().equals(id)) {
                    System.out.println("❌ [DEBUG-v46.6] ID do pedido já existe: " + novoIdPedido);
                    return ResponseEntity.badRequest().body("Já existe outra venda com este ID do pedido");
                }
                vendaExistente.setIdPedido(novoIdPedido);
                System.out.println("📝 [DEBUG-v46.6] ID do pedido atualizado: " + novoIdPedido);
            }

            // ✅ 3️⃣ ATUALIZAR APENAS CAMPOS PERMITIDOS (INCLUINDO DATA)
            if (vendaData.containsKey("data")) {
                LocalDateTime novaData = extrairData(vendaData.get("data"));
                if (novaData == null) {
                    return ResponseEntity.badRequest()
                            .body("Formato de data inválido. Esperado: YYYY-MM-DD");
                }
                vendaExistente.setData(novaData);
                System.out.println("📝 [DEBUG-v46.6] Data atualizada: " + novaData);
            }

            if (vendaData.containsKey("plataforma")) {
                vendaExistente.setPlataforma(vendaData.get("plataforma").toString());
                System.out.println("📝 [DEBUG-v46.6] Plataforma atualizada: " + vendaData.get("plataforma"));
            }

            if (vendaData.containsKey("precoVenda")) {
                vendaExistente.setPrecoVenda(Double.valueOf(vendaData.get("precoVenda").toString()));
                System.out.println("📝 [DEBUG-v46.6] Preço venda atualizado: " + vendaData.get("precoVenda"));
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

            // ✅✅✅ 4️⃣ PROCESSAR ITENS MODIFICADOS (CORREÇÃO CRÍTICA v46.6)
            if (vendaData.containsKey("itens")) {
                List<Map<String, Object>> itensData = (List<Map<String, Object>>) vendaData.get("itens");

                System.out.println("🔄 [DEBUG-v46.6] PROCESSANDO ITENS - Total recebido: " +
                        (itensData != null ? itensData.size() : 0) + " itens");

                if (itensData == null || itensData.isEmpty()) {
                    System.out.println("❌ [DEBUG-v46.6] ERRO: Lista de itens vazia ou nula");
                    return ResponseEntity.badRequest().body("A venda deve conter pelo menos um produto");
                }

                // ✅ DEBUG DETALHADO DOS ITENS RECEBIDOS
                for (int i = 0; i < itensData.size(); i++) {
                    Map<String, Object> item = itensData.get(i);
                    System.out.println("🔍 [DEBUG-v46.6] ITEM " + i + " recebido: " + item);
                }

                // ✅ REMOVER ITENS ANTIGOS E REVERTER ESTOQUE
                System.out.println("🔄 [DEBUG-v46.6] Revertendo estoque dos itens antigos...");
                estoqueService.reverterEstoqueVenda(vendaExistente);
                vendaExistente.getItens().clear();
                System.out.println("✅ [DEBUG-v46.6] Itens antigos removidos e estoque revertido");

                // ✅ CRIAR NOVOS ITENS
                System.out.println("🔄 [DEBUG-v46.6] Criando novos itens...");
                for (Map<String, Object> itemData : itensData) {
                    // Extrair dados do item com validação
                    Long produtoId;
                    Integer quantidade;

                    try {
                        produtoId = Long.valueOf(itemData.get("produtoId").toString());
                        quantidade = Integer.valueOf(itemData.get("quantidade").toString());
                    } catch (Exception e) {
                        System.out.println("❌ [DEBUG-v46.6] Erro ao extrair dados do item: " + e.getMessage());
                        return ResponseEntity.badRequest()
                                .body("Erro nos dados do produto: " + e.getMessage());
                    }

                    // ✅✅✅ CORREÇÃO v46.6: Extrair precoUnitarioVenda do frontend
                    BigDecimal precoUnitario = BigDecimal.ZERO;
                    if (itemData.containsKey("precoUnitarioVenda") && itemData.get("precoUnitarioVenda") != null) {
                        try {
                            precoUnitario = new BigDecimal(itemData.get("precoUnitarioVenda").toString());
                            System.out.println("💰 [DEBUG-v46.6] precoUnitarioVenda extraído: " + precoUnitario);
                        } catch (Exception e) {
                            System.out.println("⚠️ [DEBUG-v46.6] precoUnitarioVenda inválido, usando 0: " + e.getMessage());
                        }
                    } else {
                        System.out.println("⚠️ [DEBUG-v46.6] precoUnitarioVenda não encontrado no item");
                    }

                    // Buscar produto
                    Optional<Produto> produtoOpt = produtoRepository.findByIdAndUser(produtoId, currentUser);
                    if (!produtoOpt.isPresent()) {
                        System.out.println("❌ [DEBUG-v46.6] Produto não encontrado: ID " + produtoId);
                        return ResponseEntity.badRequest()
                                .body("Produto não encontrado: ID " + produtoId);
                    }

                    Produto produto = produtoOpt.get();
                    System.out.println("✅ [DEBUG-v46.6] Produto encontrado: " + produto.getNome());

                    // Verificar estoque
                    Integer saldoDisponivel = estoqueService.verificarSaldoTotal(produto);
                    if (saldoDisponivel < quantidade) {
                        System.out.println("❌ [DEBUG-v46.6] Estoque insuficiente: " + produto.getNome() +
                                " (Disponível: " + saldoDisponivel + ", Necessário: " + quantidade + ")");
                        return ResponseEntity.badRequest()
                                .body("Estoque insuficiente para produto: " + produto.getNome() +
                                        ". Disponível: " + saldoDisponivel + ", Necessário: " + quantidade);
                    }

                    // Encontrar lote PEPS
                    EntradaEstoque lote = entradaEstoqueRepository
                            .findByProdutoAndUserAndSaldoGreaterThanOrderByDataEntradaAsc(produto, currentUser, 0)
                            .stream()
                            .findFirst()
                            .orElseThrow(() -> {
                                System.out.println("❌ [DEBUG-v46.6] Produto sem estoque: " + produto.getNome());
                                return new RuntimeException("Produto sem estoque: " + produto.getNome());
                            });

                    System.out.println("✅ [DEBUG-v46.6] Lote PEPS encontrado: ID " + lote.getId());

                    // Criar novo item COM precoUnitario
                    ItemVenda novoItem = new ItemVenda();
                    novoItem.setVenda(vendaExistente);
                    novoItem.setLote(lote);
                    novoItem.setQuantidade(quantidade);
                    novoItem.setCustoUnitario(lote.getCustoUnitario());
                    novoItem.setPrecoUnitario(precoUnitario); // ✅✅✅ ADICIONADO
                    novoItem.setUser(currentUser);

                    vendaExistente.getItens().add(novoItem);
                    System.out.println("✅ [DEBUG-v46.6] Item criado para produto: " + produto.getNome());
                }

                // ✅ RECALCULAR CUSTOS E ATUALIZAR ESTOQUE
                System.out.println("🔄 [DEBUG-v46.6] Recalculando custos PEPS...");
                estoqueService.calcularCustoVendaERegistrarItens(vendaExistente);
                System.out.println("✅ [DEBUG-v46.6] Custo PEPS recalculado");
            } else {
                System.out.println("❌ [DEBUG-v46.6] ERRO: Campo 'itens' não encontrado nos dados");
                return ResponseEntity.badRequest().body("Dados de itens não fornecidos");
            }

            // ✅ 5️⃣ SALVAR VENDA ATUALIZADA
            System.out.println("🔄 [DEBUG-v46.6] Salvando venda atualizada no banco...");
            Venda vendaSalva = vendaRepository.save(vendaExistente);

            System.out.println("✅✅✅ [DEBUG-v46.6] VENDA ATUALIZADA COM SUCESSO: " + vendaSalva.getIdPedido());
            System.out.println("📊 [DEBUG-v46.6] Total de itens na venda: " + vendaSalva.getItens().size());
            System.out.println("📊 [DEBUG-v46.6] Data da venda: " + vendaSalva.getData());

            return ResponseEntity.ok(new VendaDTO(vendaSalva));

        } catch (NumberFormatException e) {
            System.out.println("❌ [DEBUG-v46.6] Erro de formato numérico: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Erro de formato numérico: " + e.getMessage());
        } catch (ClassCastException e) {
            System.out.println("❌ [DEBUG-v46.6] Erro de tipo de dados: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Erro de tipo de dados: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("❌ [DEBUG-v46.6] Erro ao atualizar venda: " + e.getMessage());
            e.printStackTrace();
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

            System.out.println("✅ [DEBUG-v46.6] Venda excluída e estoque revertido: " + venda.getIdPedido());
            return ResponseEntity.noContent().build();

        } catch (Exception e) {
            System.out.println("❌ [DEBUG-v46.6] Erro ao reverter estoque na exclusão da venda: " + e.getMessage());
            return ResponseEntity.badRequest().body("Erro ao excluir venda: " + e.getMessage());
        }
    }

    // ========== RESTANTE DO CÓDIGO (MÉTODOS GET E DASHBOARD) ==========

    // ✅ ATUALIZADO: GET - Listar todas as vendas DO USUÁRIO - AGORA COM DTO
    @GetMapping
    public ResponseEntity<List<VendaDTO>> listarTodas() {
        try {
            User currentUser = getCurrentUser();
            System.out.println("🔍 [DEBUG-v46.6] Buscando vendas para usuário: " + currentUser.getEmail());

            List<Venda> vendas = vendaRepository.findByUserWithProduto(currentUser);
            System.out.println("📊 [DEBUG-v46.6] Total de vendas encontradas: " + vendas.size());

            // ✅ CONVERTER PARA DTO
            List<VendaDTO> vendasDTO = vendas.stream()
                    .map(VendaDTO::new)
                    .collect(Collectors.toList());

            System.out.println("✅ [DEBUG-v46.6] Vendas convertidas para DTO: " + vendasDTO.size());
            return ResponseEntity.ok(vendasDTO);
        } catch (Exception e) {
            System.out.println("❌ [DEBUG-v46.6] ERRO CRÍTICO em listarTodas: " + e.getMessage());
            e.printStackTrace();
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

    // ✅✅✅ ATUALIZADO: GET - Buscar venda por nome DO USUÁRIO
    @GetMapping("/produto/{nome}")
    public ResponseEntity<List<VendaDTO>> buscarPorNomeProduto(@PathVariable String nome) {
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

    // ========== MÉTODOS DO DASHBOARD (MANTIDOS) ==========
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

    // Métodos de cálculo mensal
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

    // 🆕 ADICIONADO: Endpoints para dados do ANO ATUAL
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