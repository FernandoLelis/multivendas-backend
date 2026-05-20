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

    // ========== MÉTODOS AUXILIARES ==========
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            return (User) authentication.getPrincipal();
        }
        throw new RuntimeException("Usuário não autenticado");
    }

    private LocalDateTime extrairData(Object dataObj) {
        if (dataObj == null || dataObj.toString().isEmpty()) return null;
        String dataString = dataObj.toString().trim();
        try {
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
                    System.out.println("❌ [DEBUG] DATA inválida: " + dataString);
                    return null;
                }
            }
        }
    }

    private Double getDoubleValue(Object value, Double defaultValue) {
        if (value == null) return defaultValue;
        try {
            return Double.valueOf(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    // ========== ENDPOINTS CRUD ==========
    @PostMapping
    public ResponseEntity<?> criarVenda(@RequestBody Map<String, Object> vendaData) {
        try {
            System.out.println("🔍 [DEBUG] Criar venda - Dados recebidos: " + vendaData);
            User currentUser = getCurrentUser();

            if (!vendaData.containsKey("idPedido") || !vendaData.containsKey("plataforma") ||
                    !vendaData.containsKey("precoVenda") || !vendaData.containsKey("itens")) {
                return ResponseEntity.badRequest().body("Dados incompletos.");
            }

            String idPedido = vendaData.get("idPedido").toString();
            String plataforma = vendaData.get("plataforma").toString();
            Double precoVenda = Double.valueOf(vendaData.get("precoVenda").toString());

            LocalDateTime dataVenda = extrairData(vendaData.get("data"));
            if (dataVenda == null) {
                return ResponseEntity.badRequest().body("Data da venda é obrigatória.");
            }

            Double fretePagoPeloCliente = getDoubleValue(vendaData.get("fretePagoPeloCliente"), 0.0);
            Double custoEnvio = getDoubleValue(vendaData.get("custoEnvio"), 0.0);
            Double tarifaPlataforma = getDoubleValue(vendaData.get("tarifaPlataforma"), 0.0);
            Double despesasOperacionais = getDoubleValue(vendaData.get("despesasOperacionais"), 0.0);

            List<Map<String, Object>> itensData = (List<Map<String, Object>>) vendaData.get("itens");
            if (itensData == null || itensData.isEmpty()) {
                return ResponseEntity.badRequest().body("A venda deve conter pelo menos um produto");
            }

            // Verificar duplicidade de ID do pedido
            if (vendaRepository.findByIdPedidoAndUser(idPedido, currentUser).isPresent()) {
                return ResponseEntity.badRequest().body("Já existe uma venda com este ID do pedido");
            }

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

            List<ItemVenda> itensPreliminares = new ArrayList<>();
            for (Map<String, Object> itemData : itensData) {
                Long produtoId = Long.valueOf(itemData.get("produtoId").toString());
                Integer quantidade = Integer.valueOf(itemData.get("quantidade").toString());

                BigDecimal precoUnitario = BigDecimal.ZERO;
                if (itemData.containsKey("precoUnitarioVenda") && itemData.get("precoUnitarioVenda") != null) {
                    precoUnitario = new BigDecimal(itemData.get("precoUnitarioVenda").toString());
                }

                Optional<Produto> produtoOpt = produtoRepository.findByIdAndUser(produtoId, currentUser);
                if (!produtoOpt.isPresent()) {
                    return ResponseEntity.badRequest().body("Produto não encontrado: ID " + produtoId);
                }
                Produto produto = produtoOpt.get();

                if (!estoqueService.verificarEstoqueSuficiente(produto, quantidade)) {
                    Integer saldo = estoqueService.verificarSaldoTotal(produto);
                    return ResponseEntity.badRequest().body("Estoque insuficiente para: " + produto.getNome() +
                            ". Disponível: " + saldo + ", Necessário: " + quantidade);
                }

                ItemVenda item = new ItemVenda();
                item.setVenda(venda);
                item.setProduto(produto);
                item.setQuantidade(quantidade);
                item.setPrecoUnitario(precoUnitario);
                item.setUser(currentUser);
                item.setCustoUnitario(BigDecimal.ZERO);
                item.setProcessadoPeps(false);
                itensPreliminares.add(item);
            }

            venda.setItens(itensPreliminares);
            estoqueService.processarVendaComPeps(venda);

            Optional<Venda> vendaCompleta = vendaRepository.findByIdPedidoAndUser(idPedido, currentUser);
            return ResponseEntity.ok(new VendaDTO(vendaCompleta.get()));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Erro ao criar venda: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> atualizarVenda(@PathVariable Long id, @RequestBody Map<String, Object> vendaData) {
        try {
            System.out.println("🔄 [DEBUG] ATUALIZAR-VENDA - Iniciando atualização da venda ID: " + id);
            System.out.println("🔍 [DEBUG] Dados recebidos: " + vendaData.keySet());

            User currentUser = getCurrentUser();

            Optional<Venda> vendaExistenteOpt = vendaRepository.findByIdAndUser(id, currentUser);
            if (!vendaExistenteOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            Venda vendaExistente = vendaExistenteOpt.get();
            System.out.println("✅ [DEBUG] Venda encontrada: " + vendaExistente.getIdPedido());
            System.out.println("📊 [DEBUG] Itens atuais: " + vendaExistente.getItens().size() + " lotes");

            boolean itensEnviados = vendaData.containsKey("itens") && vendaData.get("itens") != null;

            if (!itensEnviados) {
                // Atualizar apenas campos básicos
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
                    if (novaData == null) {
                        return ResponseEntity.badRequest().body("Formato de data inválido.");
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
                    return ResponseEntity.ok(new VendaDTO(vendaExistente));
                }

                Venda vendaSalva = vendaRepository.save(vendaExistente);
                return ResponseEntity.ok(new VendaDTO(vendaSalva));
            }

            // Se itens foram enviados, processar com PEPS (lógica completa mantida)
            List<Map<String, Object>> itensData = (List<Map<String, Object>>) vendaData.get("itens");
            System.out.println("🔍 [DEBUG] Itens recebidos: " + itensData.size());

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

            // ✅ CORREÇÃO: Detectar se a lista de produtos mudou (adição/remoção)
            Set<Long> idsProdutosNovos = quantidadesNovas.keySet();
            Set<Long> idsProdutosAtuais = quantidadesOriginais.keySet();
            boolean produtosMudaram = !idsProdutosNovos.equals(idsProdutosAtuais);

            if (produtosMudaram) {
                quantidadeMudou = true;
                System.out.println("⚠️ [DEBUG] Lista de produtos alterada! Produtos removidos ou adicionados.");
            }

            // Comparar quantidades dos produtos que existem em ambos
            if (!quantidadeMudou) {
                for (Long produtoId : quantidadesNovas.keySet()) {
                    Integer quantidadeOriginal = quantidadesOriginais.getOrDefault(produtoId, 0);
                    Integer quantidadeNova = quantidadesNovas.get(produtoId);
                    if (!quantidadeOriginal.equals(quantidadeNova)) {
                        quantidadeMudou = true;
                        System.out.println("⚠️ [DEBUG] Quantidade alterada! Produto " + produtoId +
                                ": " + quantidadeOriginal + " → " + quantidadeNova);
                        break;
                    }
                }
            }

            if (!quantidadeMudou) {
                System.out.println("✅ [DEBUG] Quantidades NÃO alteradas! Mantendo lotes originais.");
                // Atualizar campos básicos e financeiros
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

                // Atualizar preços unitários se fornecidos
                for (Map<String, Object> itemData : itensData) {
                    if (itemData.containsKey("precoUnitarioVenda") && itemData.get("precoUnitarioVenda") != null) {
                        try {
                            Long produtoId = Long.valueOf(itemData.get("produtoId").toString());
                            BigDecimal novoPreco = new BigDecimal(itemData.get("precoUnitarioVenda").toString());

                            for (ItemVenda item : vendaExistente.getItens()) {
                                if (item.getProduto() != null && item.getProduto().getId().equals(produtoId)) {
                                    item.setPrecoUnitario(novoPreco);
                                    camposModificados = true;
                                }
                            }
                        } catch (Exception e) {
                            System.out.println("⚠️ [DEBUG] Erro ao atualizar preço: " + e.getMessage());
                        }
                    }
                }

                if (!camposModificados) {
                    System.out.println("⚠️ [DEBUG] Nenhum campo modificado");
                    return ResponseEntity.ok(new VendaDTO(vendaExistente));
                }

                Venda vendaSalva = vendaRepository.save(vendaExistente);
                return ResponseEntity.ok(new VendaDTO(vendaSalva));
            }

            // ✅ Quantidade mudou OU lista de produtos mudou - reprocessar PEPS completo
            System.out.println("⚠️ [DEBUG] Quantidades ou lista de produtos alteradas - reprocessando PEPS...");

            // Verificar duplicidade de ID do pedido
            if (vendaData.containsKey("idPedido")) {
                String novoIdPedido = vendaData.get("idPedido").toString();
                Optional<Venda> vendaComMesmoPedido = vendaRepository.findByIdPedidoAndUser(novoIdPedido, currentUser);
                if (vendaComMesmoPedido.isPresent() && !vendaComMesmoPedido.get().getId().equals(id)) {
                    return ResponseEntity.badRequest().body("Já existe outra venda com este ID do pedido");
                }
                vendaExistente.setIdPedido(novoIdPedido);
            }

            // Atualizar campos básicos
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

            // Reverter estoque dos itens antigos e limpar
            estoqueService.reverterEstoqueVenda(vendaExistente);
            vendaExistente.getItens().clear();

            // Criar novos itens preliminares
            List<ItemVenda> novosItensPreliminares = new ArrayList<>();
            for (Map<String, Object> itemData : itensData) {
                Long produtoId = Long.valueOf(itemData.get("produtoId").toString());
                Integer quantidade = Integer.valueOf(itemData.get("quantidade").toString());

                BigDecimal precoUnitario = BigDecimal.ZERO;
                if (itemData.containsKey("precoUnitarioVenda") && itemData.get("precoUnitarioVenda") != null) {
                    precoUnitario = new BigDecimal(itemData.get("precoUnitarioVenda").toString());
                }

                Optional<Produto> produtoOpt = produtoRepository.findByIdAndUser(produtoId, currentUser);
                if (!produtoOpt.isPresent()) {
                    return ResponseEntity.badRequest().body("Produto não encontrado: ID " + produtoId);
                }
                Produto produto = produtoOpt.get();

                if (!estoqueService.verificarEstoqueSuficiente(produto, quantidade)) {
                    Integer saldo = estoqueService.verificarSaldoTotal(produto);
                    return ResponseEntity.badRequest().body("Estoque insuficiente para: " + produto.getNome() +
                            ". Disponível: " + saldo + ", Necessário: " + quantidade);
                }

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

            vendaExistente.setItens(novosItensPreliminares);
            estoqueService.processarVendaComPeps(vendaExistente);
            Venda vendaSalva = vendaRepository.save(vendaExistente);

            return ResponseEntity.ok(new VendaDTO(vendaSalva));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Erro ao atualizar venda: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> excluirVenda(@PathVariable Long id) {
        try {
            User currentUser = getCurrentUser();

            Optional<Venda> vendaOpt = vendaRepository.findByIdAndUser(id, currentUser);
            if (!vendaOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            Venda venda = vendaOpt.get();

            // Reverter estoque conforme status (ativa ou cancelada)
            estoqueService.reverterEstoqueExclusaoVenda(venda);

            // Excluir a venda
            vendaRepository.deleteById(id);

            System.out.println("✅ [DEBUG] Venda excluída e estoque ajustado: " + venda.getIdPedido());
            return ResponseEntity.noContent().build();

        } catch (Exception e) {
            System.out.println("❌ [DEBUG] Erro ao excluir venda: " + e.getMessage());
            return ResponseEntity.badRequest().body("Erro ao excluir venda: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/cancelar")
    public ResponseEntity<?> cancelarVenda(
            @PathVariable Long id,
            @RequestBody Map<String, Object> cancelamentoData) {
        try {
            User currentUser = getCurrentUser();

            Optional<Venda> vendaOpt = vendaRepository.findByIdAndUser(id, currentUser);
            if (!vendaOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            Venda venda = vendaOpt.get();

            String motivo = (String) cancelamentoData.getOrDefault("motivo", "");
            Double custoRetorno = getDoubleValue(cancelamentoData.get("custoRetorno"), 0.0);
            Boolean retornarEstoque = (Boolean) cancelamentoData.getOrDefault("retornouEstoque", false);

            estoqueService.cancelarVenda(venda, motivo, custoRetorno, retornarEstoque);

            return ResponseEntity.ok(new VendaDTO(venda));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao cancelar venda: " + e.getMessage());
        }
    }

    // ========== NOVO ENDPOINT: REATIVAR VENDA CANCELADA ==========
    @PutMapping("/{id}/reativar")
    public ResponseEntity<?> reativarVenda(@PathVariable Long id) {
        try {
            User currentUser = getCurrentUser();

            Optional<Venda> vendaOpt = vendaRepository.findByIdAndUser(id, currentUser);
            if (!vendaOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            Venda venda = vendaOpt.get();

            if (venda.getStatus() == Venda.StatusVenda.ATIVA) {
                return ResponseEntity.badRequest().body("Venda já está ativa.");
            }

            if (venda.getStatus() != Venda.StatusVenda.CANCELADA) {
                return ResponseEntity.badRequest().body("Apenas vendas canceladas podem ser reativadas.");
            }

            // Se no cancelamento os produtos foram devolvidos ao estoque, precisamos retirá-los novamente
            if (venda.getRetornouEstoque() != null && venda.getRetornouEstoque()) {
                // Reaplicar o consumo de estoque usando PEPS (vai recriar os itens com lotes atuais)
                estoqueService.processarVendaComPeps(venda);
            } // Senão, não mexer no estoque

            // Atualizar campos de cancelamento
            venda.setStatus(Venda.StatusVenda.ATIVA);
            venda.setMotivoCancelamento(null);
            venda.setCustoRetorno(0.0);
            venda.setRetornouEstoque(false);

            // Salvar venda reativada
            Venda vendaSalva = vendaRepository.save(venda);

            return ResponseEntity.ok(new VendaDTO(vendaSalva));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao reativar venda: " + e.getMessage());
        }
    }

    // ========== MÉTODOS DE CONSULTA ==========
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
            return ResponseEntity.badRequest().body("Erro ao calcular métricas: " + e.getMessage());
        }
    }

    @GetMapping("/resumo-mensal")
    public ResponseEntity<List<VendaDTO>> resumoMensal(@RequestParam int mes, @RequestParam int ano) {
        try {
            User currentUser = getCurrentUser();
            LocalDateTime inicio = LocalDateTime.of(ano, mes, 1, 0, 0, 0);
            LocalDateTime fim = inicio.plusMonths(1).minusNanos(1);
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

    // ========== MÉTODOS AUXILIARES PARA DASHBOARD ==========
    private Map<String, Double> calcularFaturamentoPorPlataforma(List<Venda> vendas) {
        Map<String, Double> faturamentoPorPlataforma = new HashMap<>();
        for (Venda venda : vendas) {
            String plataforma = venda.getPlataforma();
            double faturamentoVenda = venda.calcularFaturamento();
            faturamentoPorPlataforma.merge(plataforma, faturamentoVenda, Double::sum);
        }
        return faturamentoPorPlataforma;
    }

    private List<Map<String, Object>> calcularProdutosMaisVendidos(List<Venda> vendas) {
        Map<Long, Map<String, Object>> produtosMap = new HashMap<>();
        for (Venda venda : vendas) {
            if (venda.getItens() != null) {
                for (ItemVenda item : venda.getItens()) {
                    Produto produto = item.getProduto();
                    Long produtoId = produto.getId();

                    produtosMap.computeIfAbsent(produtoId, id -> {
                        Map<String, Object> info = new HashMap<>();
                        info.put("produtoId", id);
                        info.put("produtoNome", produto.getNome());
                        info.put("quantidadeVendida", 0);
                        info.put("faturamento", 0.0);
                        info.put("lucroLiquido", 0.0);
                        return info;
                    });

                    Map<String, Object> produtoInfo = produtosMap.get(produtoId);
                    double proporcaoItem = item.getCustoTotal().doubleValue() / venda.getCustoProdutoVendido();
                    double faturamentoItem = venda.calcularFaturamento() * proporcaoItem;
                    double lucroItem = venda.calcularLucroLiquido() * proporcaoItem;

                    produtoInfo.put("quantidadeVendida", (int) produtoInfo.get("quantidadeVendida") + item.getQuantidade());
                    produtoInfo.put("faturamento", (double) produtoInfo.get("faturamento") + faturamentoItem);
                    produtoInfo.put("lucroLiquido", (double) produtoInfo.get("lucroLiquido") + lucroItem);
                }
            }
        }

        List<Map<String, Object>> produtosMaisVendidos = new ArrayList<>(produtosMap.values());
        produtosMaisVendidos.sort((a, b) -> Integer.compare((int) b.get("quantidadeVendida"), (int) a.get("quantidadeVendida")));
        return produtosMaisVendidos.size() > 5 ? produtosMaisVendidos.subList(0, 5) : produtosMaisVendidos;
    }

    // ========== ENDPOINTS DO DASHBOARD ==========
    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard() {
        try {
            User currentUser = getCurrentUser();
            LocalDateTime agora = LocalDateTime.now();
            LocalDateTime inicioMesAtual = agora.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime inicioAno = agora.withDayOfYear(1).withHour(0).withMinute(0).withSecond(0).withNano(0);

            // Métricas do mês atual
            List<Object[]> metricasMesList = vendaRepository.getMetricasAgregadasNative(currentUser.getId(), inicioMesAtual, agora);
            Object[] metricasMes = metricasMesList.isEmpty() ? new Object[]{0, 0, 0} : metricasMesList.get(0);
            System.out.println("🔍 [DASHBOARD] metricasMes: " + Arrays.toString(metricasMes));
            Double faturamentoMes = ((Number) metricasMes[0]).doubleValue();
            Double custoEfetivoMes = ((Number) metricasMes[1]).doubleValue();
            Double despesasOperacionaisMes = ((Number) metricasMes[2]).doubleValue();
            Double lucroBrutoMes = faturamentoMes - custoEfetivoMes;
            Double lucroLiquidoMes = lucroBrutoMes - despesasOperacionaisMes;
            Double roiMes = (custoEfetivoMes > 0) ? (lucroLiquidoMes / custoEfetivoMes) * 100 : 0;

            // Métricas do ano atual
            List<Object[]> metricasAnoList = vendaRepository.getMetricasAgregadasNative(currentUser.getId(), inicioAno, agora);
            Object[] metricasAno = metricasAnoList.isEmpty() ? new Object[]{0, 0, 0} : metricasAnoList.get(0);
            System.out.println("🔍 [DASHBOARD] metricasAno: " + Arrays.toString(metricasAno));
            Double faturamentoAno = ((Number) metricasAno[0]).doubleValue();
            Double custoEfetivoAno = ((Number) metricasAno[1]).doubleValue();
            Double despesasOperacionaisAno = ((Number) metricasAno[2]).doubleValue();
            Double lucroBrutoAno = faturamentoAno - custoEfetivoAno;
            Double lucroLiquidoAno = lucroBrutoAno - despesasOperacionaisAno;
            Double roiAno = (custoEfetivoAno > 0) ? (lucroLiquidoAno / custoEfetivoAno) * 100 : 0;

            // Para métricas que dependem de itens (plataforma, produtos mais vendidos), pegamos apenas as vendas ativas
            List<Venda> todasVendas = vendaRepository.findByUserWithProduto(currentUser);
            List<Venda> vendasAtivas = todasVendas.stream()
                    .filter(v -> v.getStatus() == Venda.StatusVenda.ATIVA)
                    .collect(Collectors.toList());

            Map<String, Double> faturamentoPorPlataforma = calcularFaturamentoPorPlataforma(vendasAtivas);
            List<Map<String, Object>> produtosMaisVendidos = calcularProdutosMaisVendidos(vendasAtivas);

            long totalVendasAtivas = vendasAtivas.size();
            long vendasMesAtual = vendasAtivas.stream()
                    .filter(v -> v.getData().getMonthValue() == agora.getMonthValue()
                            && v.getData().getYear() == agora.getYear())
                    .count();

            Map<String, Object> dashboard = new HashMap<>();
            dashboard.put("faturamentoTotal", faturamentoAno);
            dashboard.put("custoEfetivoTotal", custoEfetivoAno);
            dashboard.put("lucroBrutoTotal", lucroBrutoAno);
            dashboard.put("lucroLiquidoTotal", lucroLiquidoAno);
            dashboard.put("roiTotal", roiAno);
            dashboard.put("faturamentoPorPlataforma", faturamentoPorPlataforma);
            dashboard.put("totalVendas", totalVendasAtivas);
            dashboard.put("vendasMesAtual", vendasMesAtual);
            dashboard.put("produtosMaisVendidos", produtosMaisVendidos);
            dashboard.put("faturamentoMes", faturamentoMes);
            dashboard.put("custoEfetivoMes", custoEfetivoMes);
            dashboard.put("lucroBrutoMes", lucroBrutoMes);
            dashboard.put("lucroLiquidoMes", lucroLiquidoMes);
            dashboard.put("roiMes", roiMes);

            return ResponseEntity.ok(dashboard);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Erro ao carregar dashboard: " + e.getMessage());
        }
    }

    @GetMapping("/faturamento-mes-atual")
    public ResponseEntity<?> getFaturamentoMesAtual() {
        try {
            User currentUser = getCurrentUser();
            LocalDateTime agora = LocalDateTime.now();
            LocalDateTime inicioMes = agora.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
            List<Object[]> metricasList = vendaRepository.getMetricasAgregadasNative(currentUser.getId(), inicioMes, agora);
            Object[] metricas = metricasList.isEmpty() ? new Object[]{0, 0, 0} : metricasList.get(0);
            Double faturamento = ((Number) metricas[0]).doubleValue();
            return ResponseEntity.ok(faturamento);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Erro ao calcular faturamento: " + e.getMessage());
        }
    }

    @GetMapping("/custo-efetivo-mes-atual")
    public ResponseEntity<?> getCustoEfetivoMesAtual() {
        try {
            User currentUser = getCurrentUser();
            LocalDateTime agora = LocalDateTime.now();
            LocalDateTime inicioMes = agora.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
            List<Object[]> metricasList = vendaRepository.getMetricasAgregadasNative(currentUser.getId(), inicioMes, agora);
            Object[] metricas = metricasList.isEmpty() ? new Object[]{0, 0, 0} : metricasList.get(0);
            Double custoEfetivo = ((Number) metricas[1]).doubleValue();
            return ResponseEntity.ok(custoEfetivo);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Erro ao calcular custo efetivo: " + e.getMessage());
        }
    }

    @GetMapping("/lucro-bruto-mes-atual")
    public ResponseEntity<?> getLucroBrutoMesAtual() {
        try {
            User currentUser = getCurrentUser();
            LocalDateTime agora = LocalDateTime.now();
            LocalDateTime inicioMes = agora.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
            List<Object[]> metricasList = vendaRepository.getMetricasAgregadasNative(currentUser.getId(), inicioMes, agora);
            Object[] metricas = metricasList.isEmpty() ? new Object[]{0, 0, 0} : metricasList.get(0);
            Double faturamento = ((Number) metricas[0]).doubleValue();
            Double custoEfetivo = ((Number) metricas[1]).doubleValue();
            Double lucroBruto = faturamento - custoEfetivo;
            return ResponseEntity.ok(lucroBruto);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Erro ao calcular lucro bruto: " + e.getMessage());
        }
    }

    @GetMapping("/lucro-liquido-mes-atual")
    public ResponseEntity<?> getLucroLiquidoMesAtual() {
        try {
            User currentUser = getCurrentUser();
            LocalDateTime agora = LocalDateTime.now();
            LocalDateTime inicioMes = agora.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
            List<Object[]> metricasList = vendaRepository.getMetricasAgregadasNative(currentUser.getId(), inicioMes, agora);
            Object[] metricas = metricasList.isEmpty() ? new Object[]{0, 0, 0} : metricasList.get(0);
            Double faturamento = ((Number) metricas[0]).doubleValue();
            Double custoEfetivo = ((Number) metricas[1]).doubleValue();
            Double despesas = ((Number) metricas[2]).doubleValue();
            Double lucroLiquido = (faturamento - custoEfetivo) - despesas;
            return ResponseEntity.ok(lucroLiquido);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Erro ao calcular lucro líquido: " + e.getMessage());
        }
    }

    @GetMapping("/quantidade-vendas")
    public ResponseEntity<?> getQuantidadeVendas() {
        try {
            User currentUser = getCurrentUser();
            LocalDateTime agora = LocalDateTime.now();
            LocalDateTime inicioMesAtual = agora.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime inicioMesAnterior = inicioMesAtual.minusMonths(1);
            LocalDateTime limiteMesAnterior = agora.minusMonths(1);
            LocalDateTime inicioAno = agora.withDayOfYear(1).withHour(0).withMinute(0).withSecond(0).withNano(0);

            long vendasMesAtual = vendaRepository.findByDataBetweenAndUser(inicioMesAtual, agora, currentUser).size();
            long vendasMesAnterior = vendaRepository.findByDataBetweenAndUser(inicioMesAnterior, limiteMesAnterior, currentUser).size();
            long vendasAnoAtual = vendaRepository.findByDataBetweenAndUser(inicioAno, agora, currentUser).size();

            double variacao = 0.0;
            if (vendasMesAnterior > 0) {
                variacao = ((double) (vendasMesAtual - vendasMesAnterior) / vendasMesAnterior) * 100;
            } else if (vendasMesAtual > 0) {
                variacao = 100.0;
            }

            Map<String, Object> response = new HashMap<>();
            response.put("mesAtual", vendasMesAtual);
            response.put("anoAtual", vendasAnoAtual);
            response.put("variacao", variacao);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Erro ao buscar quantidade de vendas: " + e.getMessage());
        }
    }

    @GetMapping("/vendas-por-dia")
    public ResponseEntity<?> getVendasPorDia(
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer ano) {
        try {
            User currentUser = getCurrentUser();
            if (mes == null) mes = LocalDate.now().getMonthValue();
            if (ano == null) ano = LocalDate.now().getYear();

            LocalDate dataInicio = LocalDate.of(ano, mes, 1);
            int ultimoDia = dataInicio.lengthOfMonth();
            LocalDateTime inicioMes = dataInicio.atStartOfDay();
            LocalDateTime fimMes = dataInicio.withDayOfMonth(ultimoDia).atTime(23, 59, 59);
            List<Venda> vendasDoMes = vendaRepository.findByDataBetweenAndUser(inicioMes, fimMes, currentUser);

            Map<Integer, Integer> pedidosPorDia = new HashMap<>();
            for (Venda venda : vendasDoMes) {
                int diaDaVenda = venda.getData().getDayOfMonth();
                pedidosPorDia.put(diaDaVenda, pedidosPorDia.getOrDefault(diaDaVenda, 0) + 1);
            }

            Map<String, Integer> retornoAcumulado = new LinkedHashMap<>();
            int totalAcumulado = 0;
            LocalDate hoje = LocalDate.now();
            boolean ehMesAtual = (ano == hoje.getYear() && mes == hoje.getMonthValue());
            int diaLimite = ehMesAtual ? hoje.getDayOfMonth() : ultimoDia;

            for (int dia = 1; dia <= diaLimite; dia++) {
                int vendasHoje = pedidosPorDia.getOrDefault(dia, 0);
                totalAcumulado += vendasHoje;
                String dataStr = String.format("%04d-%02d-%02d", ano, mes, dia);
                retornoAcumulado.put(dataStr, totalAcumulado);
            }
            return ResponseEntity.ok(retornoAcumulado);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Erro ao buscar vendas por dia: " + e.getMessage());
        }
    }

    @GetMapping("/faturamento-ano-atual")
    public ResponseEntity<?> getFaturamentoAnoAtual() {
        try {
            User currentUser = getCurrentUser();
            LocalDateTime inicioAno = LocalDateTime.now().withDayOfYear(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
            List<Object[]> metricasList = vendaRepository.getMetricasAgregadasNative(currentUser.getId(), inicioAno, LocalDateTime.now());
            Object[] metricas = metricasList.isEmpty() ? new Object[]{0, 0, 0} : metricasList.get(0);
            Double faturamento = ((Number) metricas[0]).doubleValue();
            return ResponseEntity.ok(faturamento);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Erro ao calcular faturamento anual: " + e.getMessage());
        }
    }

    @GetMapping("/custo-efetivo-ano-atual")
    public ResponseEntity<?> getCustoEfetivoAnoAtual() {
        try {
            User currentUser = getCurrentUser();
            LocalDateTime inicioAno = LocalDateTime.now().withDayOfYear(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
            List<Object[]> metricasList = vendaRepository.getMetricasAgregadasNative(currentUser.getId(), inicioAno, LocalDateTime.now());
            Object[] metricas = metricasList.isEmpty() ? new Object[]{0, 0, 0} : metricasList.get(0);
            Double custoEfetivo = ((Number) metricas[1]).doubleValue();
            return ResponseEntity.ok(custoEfetivo);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Erro ao calcular custo efetivo anual: " + e.getMessage());
        }
    }

    @GetMapping("/lucro-bruto-ano-atual")
    public ResponseEntity<?> getLucroBrutoAnoAtual() {
        try {
            User currentUser = getCurrentUser();
            LocalDateTime inicioAno = LocalDateTime.now().withDayOfYear(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
            List<Object[]> metricasList = vendaRepository.getMetricasAgregadasNative(currentUser.getId(), inicioAno, LocalDateTime.now());
            Object[] metricas = metricasList.isEmpty() ? new Object[]{0, 0, 0} : metricasList.get(0);
            Double faturamento = ((Number) metricas[0]).doubleValue();
            Double custoEfetivo = ((Number) metricas[1]).doubleValue();
            Double lucroBruto = faturamento - custoEfetivo;
            return ResponseEntity.ok(lucroBruto);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Erro ao calcular lucro bruto anual: " + e.getMessage());
        }
    }

    @GetMapping("/metricas-mes-anterior")
    public ResponseEntity<?> getMetricasMesAnterior() {
        try {
            User currentUser = getCurrentUser();
            LocalDateTime agora = LocalDateTime.now();
            LocalDateTime inicioMesAnterior = agora.minusMonths(1).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime limiteMesAnterior = agora.minusMonths(1);

            List<Object[]> metricasList = vendaRepository.getMetricasAgregadasNative(currentUser.getId(), inicioMesAnterior, limiteMesAnterior);
            Object[] metricas = metricasList.isEmpty() ? new Object[]{0, 0, 0} : metricasList.get(0);
            Double faturamento = ((Number) metricas[0]).doubleValue();
            Double custoEfetivo = ((Number) metricas[1]).doubleValue();
            Double despesas = ((Number) metricas[2]).doubleValue();
            Double lucroBruto = faturamento - custoEfetivo;
            Double lucroLiquido = lucroBruto - despesas;
            Double roi = (custoEfetivo > 0) ? (lucroLiquido / custoEfetivo) * 100 : 0;

            Map<String, Object> response = new HashMap<>();
            response.put("faturamento", faturamento);
            response.put("custoEfetivo", custoEfetivo);
            response.put("lucroBruto", lucroBruto);
            response.put("lucroLiquido", lucroLiquido);
            response.put("roi", roi);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Erro ao calcular métricas do mês anterior: " + e.getMessage());
        }
    }
}