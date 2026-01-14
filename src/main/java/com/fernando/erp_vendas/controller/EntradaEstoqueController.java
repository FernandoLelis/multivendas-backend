package com.fernando.erp_vendas.controller;

import com.fernando.erp_vendas.dto.CompraDTO;
import com.fernando.erp_vendas.model.*;
import com.fernando.erp_vendas.repository.EntradaEstoqueRepository;
import com.fernando.erp_vendas.repository.ProdutoRepository;
import com.fernando.erp_vendas.repository.CompraRepository;
import com.fernando.erp_vendas.repository.ItemCompraRepository;
import com.fernando.erp_vendas.service.EstoqueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/estoque")
public class EntradaEstoqueController {

    @Autowired
    private EntradaEstoqueRepository entradaEstoqueRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private CompraRepository compraRepository;

    @Autowired
    private ItemCompraRepository itemCompraRepository;

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

    // ============================================================
    // ✅✅✅ NOVA VERSÃO: COMPRAS COM MÚLTIPLOS PRODUTOS (v46.6)
    // ============================================================

    // ✅✅✅ ATUALIZADO: POST - Criar nova COMPRA com múltiplos produtos (v46.6)
    @PostMapping("/compra")
    public ResponseEntity<?> criarCompra(@RequestBody Map<String, Object> compraData) {
        try {
            System.out.println("🔍 [DEBUG-v46.6] COMPRA - Dados recebidos do frontend: " + compraData);

            User currentUser = getCurrentUser();

            // ✅ 1️⃣ VALIDAR DADOS OBRIGATÓRIOS
            if (!compraData.containsKey("idPedidoCompra") || !compraData.containsKey("fornecedor") ||
                    !compraData.containsKey("itens")) {
                return ResponseEntity.badRequest().body("Dados incompletos. Campos obrigatórios: idPedidoCompra, fornecedor, itens");
            }

            // ✅ 2️⃣ EXTRAIR DADOS BÁSICOS DA COMPRA
            String idPedidoCompra = compraData.get("idPedidoCompra").toString();
            String fornecedor = compraData.get("fornecedor").toString();
            String observacoes = compraData.containsKey("observacoes") ? compraData.get("observacoes").toString() : "";

            // ✅✅✅ EXTRAIR E VALIDAR DATA
            Object dataObj = compraData.get("data");
            LocalDateTime dataCompra = extrairDataMelhorado(dataObj);

            if (dataCompra == null) {
                System.out.println("⚠️ [DEBUG-v46.6] Data nula, usando data atual");
                dataCompra = LocalDateTime.now();
            }

            // ✅ 3️⃣ VALIDAR E EXTRAIR ITENS DA COMPRA
            List<Map<String, Object>> itensData = (List<Map<String, Object>>) compraData.get("itens");

            if (itensData == null || itensData.isEmpty()) {
                return ResponseEntity.badRequest().body("A compra deve conter pelo menos um produto");
            }

            // ✅ 4️⃣ VERIFICAR SE JÁ EXISTE COMPRA COM MESMO ID DO PEDIDO
            Optional<Compra> compraExistente = compraRepository.findByIdPedidoCompraAndUser(idPedidoCompra, currentUser);
            if (compraExistente.isPresent()) {
                return ResponseEntity.badRequest().body("Já existe uma compra com este ID do pedido: " + idPedidoCompra);
            }

            // ✅✅✅ 5️⃣ CRIAR A COMPRA COM DATA CORRETA
            Compra compra = new Compra();
            compra.setData(dataCompra);
            compra.setIdPedidoCompra(idPedidoCompra);
            compra.setFornecedor(fornecedor);
            compra.setObservacoes(observacoes);
            compra.setUser(currentUser);

            // ✅ 6️⃣ CRIAR ITENS DA COMPRA E VERIFICAR PRODUTOS
            List<ItemCompra> itens = new ArrayList<>();
            for (Map<String, Object> itemData : itensData) {
                Long produtoId = Long.valueOf(itemData.get("produtoId").toString());
                Integer quantidade = Integer.valueOf(itemData.get("quantidade").toString());
                BigDecimal custoUnitario = new BigDecimal(itemData.get("custoUnitario").toString());

                // ✅ VERIFICAR SE O PRODUTO EXISTE E PERTENCE AO USUÁRIO
                Optional<Produto> produtoOpt = produtoRepository.findByIdAndUser(produtoId, currentUser);
                if (!produtoOpt.isPresent()) {
                    return ResponseEntity.badRequest()
                            .body("Produto não encontrado ou não pertence ao usuário: ID " + produtoId);
                }
                Produto produto = produtoOpt.get();

                // ✅ CRIAR ITEM DA COMPRA
                ItemCompra item = new ItemCompra();
                item.setCompra(compra);
                item.setProduto(produto);
                item.setQuantidade(quantidade);
                item.setCustoUnitario(custoUnitario);
                item.setUser(currentUser);

                // ✅ CRIAR O LOTE (EntradaEstoque)
                EntradaEstoque lote = new EntradaEstoque();
                lote.setProduto(produto);
                lote.setQuantidade(quantidade);
                lote.setCustoUnitario(custoUnitario);
                lote.setCustoTotal(custoUnitario.multiply(BigDecimal.valueOf(quantidade)));
                lote.setDataEntrada(dataCompra);
                lote.setFornecedor(fornecedor);
                lote.setIdPedidoCompra(idPedidoCompra);
                lote.setCategoria("COMPRA");
                lote.setObservacoes(observacoes);
                lote.setUser(currentUser);
                lote.setSaldo(quantidade);

                // Salvar o lote
                entradaEstoqueRepository.save(lote);

                // Associar lote ao item
                item.setLote(lote);

                itens.add(item);
            }

            // ✅ 7️⃣ ADICIONAR ITENS À COMPRA
            compra.setItens(itens);

            // ✅ 8️⃣ SALVAR COMPRA
            Compra compraSalva = compraRepository.save(compra);

            // ✅ 9️⃣ BUSCAR COMPRA COMPLETA
            Optional<Compra> compraCompleta = compraRepository.findByIdPedidoCompraAndUser(idPedidoCompra, currentUser);
            if (!compraCompleta.isPresent()) {
                throw new RuntimeException("Erro ao recuperar compra criada: " + idPedidoCompra);
            }

            System.out.println("✅ [DEBUG-v46.6] Compra criada: " + compraCompleta.get().getIdPedidoCompra() +
                    ", Itens: " + compraCompleta.get().getItens().size());

            return ResponseEntity.ok(new CompraDTO(compraCompleta.get()));

        } catch (Exception e) {
            System.out.println("❌ [DEBUG-v46.6] Erro ao criar compra: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Erro ao criar compra: " + e.getMessage());
        }
    }

    // ✅✅✅✅✅ CORREÇÃO v46.6: PUT - Atualizar compra existente COM PROCESSAMENTO DE ITENS
    @PutMapping("/compra/{id}")
    public ResponseEntity<?> atualizarCompra(@PathVariable Long id, @RequestBody Map<String, Object> compraData) {
        try {
            System.out.println("🔄 [DEBUG-v46.6] ATUALIZAR-COMPRA - Iniciando atualização da compra ID: " + id);
            System.out.println("🔄 [DEBUG-v46.6] Dados recebidos: " + compraData.keySet());

            User currentUser = getCurrentUser();

            // ✅ 1️⃣ BUSCAR COMPRA EXISTENTE
            Optional<Compra> compraExistenteOpt = compraRepository.findByIdAndUser(id, currentUser);
            if (!compraExistenteOpt.isPresent()) {
                System.out.println("❌ [DEBUG-v46.6] Compra não encontrada: " + id);
                return ResponseEntity.notFound().build();
            }

            Compra compraExistente = compraExistenteOpt.get();
            System.out.println("✅ [DEBUG-v46.6] Compra encontrada: " + compraExistente.getIdPedidoCompra());

            // ✅ 2️⃣ VERIFICAR SE JÁ EXISTE OUTRA COMPRA COM MESMO ID DO PEDIDO
            if (compraData.containsKey("idPedidoCompra")) {
                String novoIdPedidoCompra = compraData.get("idPedidoCompra").toString();
                Optional<Compra> compraComMesmoPedido = compraRepository.findByIdPedidoCompraAndUser(novoIdPedidoCompra, currentUser);
                if (compraComMesmoPedido.isPresent() && !compraComMesmoPedido.get().getId().equals(id)) {
                    System.out.println("❌ [DEBUG-v46.6] ID do pedido já existe: " + novoIdPedidoCompra);
                    return ResponseEntity.badRequest().body("Já existe outra compra com este ID do pedido");
                }
                compraExistente.setIdPedidoCompra(novoIdPedidoCompra);
                System.out.println("📝 [DEBUG-v46.6] ID do pedido atualizado: " + novoIdPedidoCompra);
            }

            // ✅ 3️⃣ ATUALIZAR CAMPOS BÁSICOS (INCLUINDO DATA)
            if (compraData.containsKey("data")) {
                LocalDateTime novaData = extrairDataMelhorado(compraData.get("data"));
                if (novaData != null) {
                    compraExistente.setData(novaData);
                    System.out.println("📝 [DEBUG-v46.6] Data atualizada: " + novaData);
                }
            }

            if (compraData.containsKey("fornecedor")) {
                compraExistente.setFornecedor(compraData.get("fornecedor").toString());
                System.out.println("📝 [DEBUG-v46.6] Fornecedor atualizado: " + compraData.get("fornecedor"));
            }

            if (compraData.containsKey("observacoes")) {
                compraExistente.setObservacoes(compraData.get("observacoes").toString());
                System.out.println("📝 [DEBUG-v46.6] Observações atualizadas");
            }

            // ✅✅✅ 4️⃣ PROCESSAR ITENS MODIFICADOS (CORREÇÃO CRÍTICA v46.6)
            if (compraData.containsKey("itens")) {
                List<Map<String, Object>> itensData = (List<Map<String, Object>>) compraData.get("itens");

                System.out.println("🔄 [DEBUG-v46.6] PROCESSANDO ITENS - Total recebido: " +
                        (itensData != null ? itensData.size() : 0) + " itens");

                if (itensData == null || itensData.isEmpty()) {
                    System.out.println("❌ [DEBUG-v46.6] ERRO: Lista de itens vazia");
                    return ResponseEntity.badRequest().body("A compra deve conter pelo menos um produto");
                }

                // ✅ REMOVER ITENS ANTIGOS E SEUS LOTES
                System.out.println("🔄 [DEBUG-v46.6] Removendo itens e lotes antigos...");

                // Remover lotes associados aos itens antigos
                for (ItemCompra itemAntigo : compraExistente.getItens()) {
                    if (itemAntigo.getLote() != null) {
                        entradaEstoqueRepository.delete(itemAntigo.getLote());
                    }
                }

                // Limpar lista de itens
                compraExistente.getItens().clear();
                System.out.println("✅ [DEBUG-v46.6] Itens antigos removidos");

                // ✅ CRIAR NOVOS ITENS
                System.out.println("🔄 [DEBUG-v46.6] Criando novos itens...");
                for (Map<String, Object> itemData : itensData) {
                    // Extrair dados do item
                    Long produtoId = Long.valueOf(itemData.get("produtoId").toString());
                    Integer quantidade = Integer.valueOf(itemData.get("quantidade").toString());
                    BigDecimal custoUnitario = new BigDecimal(itemData.get("custoUnitario").toString());

                    // Buscar produto
                    Optional<Produto> produtoOpt = produtoRepository.findByIdAndUser(produtoId, currentUser);
                    if (!produtoOpt.isPresent()) {
                        System.out.println("❌ [DEBUG-v46.6] Produto não encontrado: ID " + produtoId);
                        return ResponseEntity.badRequest()
                                .body("Produto não encontrado: ID " + produtoId);
                    }

                    Produto produto = produtoOpt.get();
                    System.out.println("✅ [DEBUG-v46.6] Produto encontrado: " + produto.getNome());

                    // ✅ CRIAR NOVO ITEM DA COMPRA
                    ItemCompra novoItem = new ItemCompra();
                    novoItem.setCompra(compraExistente);
                    novoItem.setProduto(produto);
                    novoItem.setQuantidade(quantidade);
                    novoItem.setCustoUnitario(custoUnitario);
                    novoItem.setUser(currentUser);

                    // ✅ CRIAR NOVO LOTE
                    EntradaEstoque novoLote = new EntradaEstoque();
                    novoLote.setProduto(produto);
                    novoLote.setQuantidade(quantidade);
                    novoLote.setCustoUnitario(custoUnitario);
                    novoLote.setCustoTotal(custoUnitario.multiply(BigDecimal.valueOf(quantidade)));
                    novoLote.setDataEntrada(compraExistente.getData());
                    novoLote.setFornecedor(compraExistente.getFornecedor());
                    novoLote.setIdPedidoCompra(compraExistente.getIdPedidoCompra());
                    novoLote.setCategoria("COMPRA");
                    novoLote.setObservacoes(compraExistente.getObservacoes());
                    novoLote.setUser(currentUser);
                    novoLote.setSaldo(quantidade);

                    // Salvar o lote
                    EntradaEstoque loteSalvo = entradaEstoqueRepository.save(novoLote);
                    System.out.println("✅ [DEBUG-v46.6] Lote criado: ID " + loteSalvo.getId());

                    // Associar lote ao item
                    novoItem.setLote(loteSalvo);

                    // Adicionar item à compra
                    compraExistente.getItens().add(novoItem);
                    System.out.println("✅ [DEBUG-v46.6] Item criado para produto: " + produto.getNome());
                }
            } else {
                System.out.println("❌ [DEBUG-v46.6] ERRO: Campo 'itens' não encontrado");
                return ResponseEntity.badRequest().body("Dados de itens não fornecidos");
            }

            // ✅ 5️⃣ SALVAR COMPRA ATUALIZADA
            System.out.println("🔄 [DEBUG-v46.6] Salvando compra atualizada...");
            Compra compraSalva = compraRepository.save(compraExistente);

            System.out.println("✅✅✅ [DEBUG-v46.6] COMPRA ATUALIZADA COM SUCESSO: " + compraSalva.getIdPedidoCompra());

            return ResponseEntity.ok(new CompraDTO(compraSalva));

        } catch (Exception e) {
            System.out.println("❌ [DEBUG-v46.6] Erro ao atualizar compra: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Erro ao atualizar compra: " + e.getMessage());
        }
    }

    // ============================================================
    // ✅✅✅ MÉTODOS AUXILIARES (MANTIDOS)
    // ============================================================

    // ✅✅✅ NOVO: Método para extrair data CORRIGIDO (v46.6)
    private LocalDateTime extrairDataMelhorado(Object dataObj) {
        if (dataObj == null || dataObj.toString().isEmpty()) {
            return null;
        }

        String dataString = dataObj.toString().trim();

        try {
            // ✅ CASO 1: Formato "YYYY-MM-DDTHH:mm:ss" com offset
            if (dataString.contains("T")) {
                if (dataString.contains("+") || dataString.contains("-")) {
                    int timezoneIndex = Math.max(dataString.lastIndexOf('+'), dataString.lastIndexOf('-'));
                    if (timezoneIndex > dataString.indexOf('T')) {
                        OffsetDateTime odt = OffsetDateTime.parse(dataString);
                        Instant instant = odt.toInstant();
                        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
                    }
                }

                if (dataString.endsWith("Z")) {
                    String semZ = dataString.substring(0, dataString.length() - 1);
                    return LocalDateTime.parse(semZ);
                }

                return LocalDateTime.parse(dataString);
            }

            // ✅ CASO 2: Formato "YYYY-MM-DD" (apenas data)
            java.time.LocalDate dataApenas = java.time.LocalDate.parse(dataString);
            return dataApenas.atStartOfDay();

        } catch (Exception e) {
            System.out.println("❌ [DEBUG-v46.6] Erro ao extrair data: " + e.getMessage());
            return null;
        }
    }

    // ============================================================
    // ✅✅✅ MÉTODOS LEGACY (MANTIDOS PARA COMPATIBILIDADE)
    // ============================================================

    @GetMapping("/compras")
    public ResponseEntity<?> listarTodasCompras() {
        try {
            User currentUser = getCurrentUser();
            List<Compra> compras = compraRepository.findByUserOrderByDataDesc(currentUser);
            List<CompraDTO> comprasDTO = compras.stream()
                    .map(CompraDTO::new)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(comprasDTO);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao listar compras: " + e.getMessage());
        }
    }

    @GetMapping("/compra/{id}")
    public ResponseEntity<?> buscarCompraPorId(@PathVariable Long id) {
        try {
            User currentUser = getCurrentUser();
            Optional<Compra> compra = compraRepository.findByIdAndUser(id, currentUser);
            return compra.map(c -> ResponseEntity.ok(new CompraDTO(c)))
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao buscar compra: " + e.getMessage());
        }
    }

    @DeleteMapping("/compra/{id}")
    public ResponseEntity<?> excluirCompra(@PathVariable Long id) {
        try {
            User currentUser = getCurrentUser();
            Optional<Compra> compraOpt = compraRepository.findByIdAndUser(id, currentUser);
            if (!compraOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            Compra compra = compraOpt.get();

            // ✅ VERIFICAR SE ALGUM LOTE FOI CONSUMIDO
            for (ItemCompra item : compra.getItens()) {
                if (item.getLote() != null && item.getLote().getSaldo() < item.getLote().getQuantidade()) {
                    return ResponseEntity.badRequest()
                            .body("Não é possível excluir compra: lote já foi parcialmente consumido");
                }
            }

            compraRepository.delete(compra);
            return ResponseEntity.noContent().build();

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao excluir compra: " + e.getMessage());
        }
    }

    @GetMapping("/compras-unificadas")
    public ResponseEntity<?> listarTodasComprasUnificadas() {
        try {
            User currentUser = getCurrentUser();
            System.out.println("🔍 [DEBUG-v46.6] Buscando compras unificadas para usuário: " + currentUser.getEmail());

            List<CompraDTO> todasCompras = new ArrayList<>();

            // ✅ 1️⃣ BUSCAR COMPRAS DO SISTEMA NOVO
            List<Compra> comprasNovas = compraRepository.findByUserOrderByDataDesc(currentUser);
            System.out.println("📊 [DEBUG-v46.6] Compras sistema novo: " + comprasNovas.size());

            for (Compra compra : comprasNovas) {
                todasCompras.add(new CompraDTO(compra));
            }

            // ✅ 2️⃣ BUSCAR COMPRAS DO SISTEMA ANTIGO
            List<EntradaEstoque> entradas = entradaEstoqueRepository.findByUserOrderByDataEntradaDesc(currentUser);
            System.out.println("📊 [DEBUG-v46.6] Total de entradas: " + entradas.size());

            // Filtrar apenas as entradas que NÃO têm item_compra_id
            List<EntradaEstoque> entradasAntigas = entradas.stream()
                    .filter(entrada -> entrada.getItemCompra() == null)
                    .collect(Collectors.toList());

            System.out.println("📊 [DEBUG-v46.6] Compras sistema antigo: " + entradasAntigas.size());

            // ✅ 3️⃣ CONVERTER ENTRADAS ANTIGAS PARA CompraDTO
            for (EntradaEstoque entrada : entradasAntigas) {
                todasCompras.add(converterEntradaParaCompraDTO(entrada));
            }

            // ✅ 4️⃣ ORDENAR TODAS POR DATA
            todasCompras.sort((c1, c2) -> {
                if (c1.getData() == null && c2.getData() == null) return 0;
                if (c1.getData() == null) return 1;
                if (c2.getData() == null) return -1;
                return c2.getData().compareTo(c1.getData());
            });

            System.out.println("✅ [DEBUG-v46.6] Compras unificadas: " + todasCompras.size());
            return ResponseEntity.ok(todasCompras);
        } catch (Exception e) {
            System.out.println("❌ [DEBUG-v46.6] Erro em listarTodasComprasUnificadas: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Erro ao listar compras: " + e.getMessage());
        }
    }

    // ✅ MÉTODO AUXILIAR: Converter EntradaEstoque para CompraDTO
    private CompraDTO converterEntradaParaCompraDTO(EntradaEstoque entrada) {
        CompraDTO dto = new CompraDTO();

        // Definir ID negativo para indicar que é do sistema antigo
        dto.setId(-entrada.getId());

        // Usar os campos da entrada
        dto.setData(entrada.getDataEntrada());
        dto.setFornecedor(entrada.getFornecedor());
        dto.setObservacoes(entrada.getObservacoes());
        dto.setIdPedidoCompra(entrada.getIdPedidoCompra());

        // Calcular total da compra
        dto.setTotalCompra(entrada.getCustoTotal());

        // ✅ IMPORTANTE: Marcar que é do sistema antigo
        dto.setSistemaAntigo(true);

        // ✅ CRIAR UM ITEM SIMULADO
        CompraDTO.ItemCompraDTO item = new CompraDTO.ItemCompraDTO();
        if (entrada.getProduto() != null) {
            item.setProdutoId(entrada.getProduto().getId());
            item.setProdutoNome(entrada.getProduto().getNome());
        } else {
            item.setProdutoId(0L);
            item.setProdutoNome("Produto não encontrado");
        }
        item.setQuantidade(entrada.getQuantidade());
        item.setCustoUnitario(entrada.getCustoUnitario());
        item.setTotal(entrada.getCustoTotal());

        // Adicionar o item à lista
        List<CompraDTO.ItemCompraDTO> itens = new ArrayList<>();
        itens.add(item);
        dto.setItens(itens);

        return dto;
    }

    // ============================================================
    // ✅✅✅ MÉTODOS LEGACY PARA COMPATIBILIDADE
    // ============================================================

    @PostMapping("/entrada")
    public ResponseEntity<?> registrarEntrada(
            @RequestParam Long produtoId,
            @RequestParam Integer quantidade,
            @RequestParam BigDecimal custoTotal,
            @RequestParam(required = false) String fornecedor,
            @RequestParam String idPedidoCompra,
            @RequestParam String categoria,
            @RequestParam(required = false) String observacoes,
            @RequestParam(required = false) String dataEntrada) {

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

            // 🆕 VERIFICAR SE JÁ EXISTE COMPRA COM MESMO ID PEDIDO
            if (entradaEstoqueRepository.findByIdPedidoCompraAndUser(idPedidoCompra, currentUser).isPresent()) {
                return ResponseEntity.badRequest()
                        .body("Já existe uma compra cadastrada com este ID do Pedido: " + idPedidoCompra);
            }

            // ✅ Cria entrada com cálculo automático de custo unitário e saldo
            EntradaEstoque entrada = new EntradaEstoque(
                    produto,
                    quantidade,
                    custoTotal,
                    fornecedor != null ? fornecedor : "",
                    idPedidoCompra,
                    categoria,
                    observacoes != null ? observacoes : "",
                    currentUser
            );

            // ✅ DEFINIR DATA PERSONALIZADA SE FORNECIDA
            if (dataEntrada != null && !dataEntrada.trim().isEmpty()) {
                try {
                    LocalDateTime dataCustomizada = extrairDataMelhorado(dataEntrada);
                    if (dataCustomizada != null) {
                        entrada.setDataEntrada(dataCustomizada);
                    }
                } catch (Exception e) {
                    System.out.println("⚠️ [DEBUG-v46.6] Data inválida, usando data atual: " + dataEntrada);
                }
            }

            EntradaEstoque entradaSalva = entradaEstoqueRepository.save(entrada);
            return ResponseEntity.ok(entradaSalva);

        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.badRequest()
                    .body("Erro de integridade de dados: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao registrar entrada: " + e.getMessage());
        }
    }

    @PutMapping("/entrada/{id}")
    public ResponseEntity<?> atualizarEntrada(
            @PathVariable Long id,
            @RequestParam Long produtoId,
            @RequestParam Integer quantidade,
            @RequestParam BigDecimal custoTotal,
            @RequestParam(required = false) String fornecedor,
            @RequestParam String idPedidoCompra,
            @RequestParam String categoria,
            @RequestParam(required = false) String observacoes,
            @RequestParam(required = false) String dataEntrada) {

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

            // ✅ ATUALIZAR DATA SE FORNECIDA
            if (dataEntrada != null && !dataEntrada.trim().isEmpty()) {
                try {
                    LocalDateTime dataCustomizada = extrairDataMelhorado(dataEntrada);
                    if (dataCustomizada != null) {
                        entradaExistente.setDataEntrada(dataCustomizada);
                    }
                } catch (Exception e) {
                    System.out.println("⚠️ [DEBUG-v46.6] Data inválida na atualização, mantendo data original: " + dataEntrada);
                }
            }

            // Atualiza os campos da entrada existente
            entradaExistente.setProduto(produto);
            entradaExistente.setQuantidade(quantidade);
            entradaExistente.setCustoTotal(custoTotal);
            entradaExistente.setFornecedor(fornecedor != null ? fornecedor : "");
            entradaExistente.setIdPedidoCompra(idPedidoCompra);
            entradaExistente.setCategoria(categoria);
            entradaExistente.setObservacoes(observacoes != null ? observacoes : "");

            // ✅ CORREÇÃO: Recalcula o custo unitário
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

    @GetMapping("/entradas")
    public ResponseEntity<?> listarTodasEntradas() {
        try {
            User currentUser = getCurrentUser();
            List<EntradaEstoque> entradas = entradaEstoqueRepository.findByUserOrderByDataEntradaDesc(currentUser);
            return ResponseEntity.ok(entradas);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao listar compras: " + e.getMessage());
        }
    }

    @GetMapping("/produto/{produtoId}")
    public ResponseEntity<?> listarEntradasPorProduto(@PathVariable Long produtoId) {
        try {
            User currentUser = getCurrentUser();

            // Buscar produto DO USUÁRIO
            Produto produto = produtoRepository.findByIdAndUser(produtoId, currentUser)
                    .orElseThrow(() -> new RuntimeException("Produto não encontrado ou não pertence ao usuário"));

            List<EntradaEstoque> entradas = entradaEstoqueRepository.findByProdutoAndUserOrderByDataEntradaAsc(produto, currentUser);
            return ResponseEntity.ok(entradas);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao listar compras do produto: " + e.getMessage());
        }
    }

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

    @DeleteMapping("/entrada/{id}")
    public ResponseEntity<?> excluirEntrada(@PathVariable Long id) {
        try {
            User currentUser = getCurrentUser();

            EntradaEstoque entrada = entradaEstoqueRepository.findByIdAndUser(id, currentUser)
                    .orElseThrow(() -> new RuntimeException("Compra não encontrada ou não pertence ao usuário"));

            // ✅ VALIDAÇÃO PEPS - Não permite excluir lote parcialmente consumido
            Integer saldoAtual = entrada.getSaldo();
            Integer quantidadeOriginal = entrada.getQuantidade();

            // Se o saldo atual é menor que a quantidade original, significa que parte foi vendida
            if (saldoAtual != null && saldoAtual < quantidadeOriginal) {
                return ResponseEntity.badRequest()
                        .body("Não é possível excluir um lote que já foi parcialmente consumido. " +
                                "Saldo atual: " + saldoAtual + ", Quantidade original: " + quantidadeOriginal + ". " +
                                "Exclua as VENDAS que utilizaram este lote primeiro para liberar a exclusão.");
            }

            entradaEstoqueRepository.delete(entrada);
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao excluir compra: " + e.getMessage());
        }
    }

    @GetMapping("/entrada/{id}")
    public ResponseEntity<?> buscarEntradaPorId(@PathVariable Long id) {
        try {
            User currentUser = getCurrentUser();
            Optional<EntradaEstoque> entrada = entradaEstoqueRepository.findByIdAndUser(id, currentUser);
            return entrada.map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao buscar compra: " + e.getMessage());
        }
    }

    @GetMapping("/categoria/{categoria}")
    public ResponseEntity<?> buscarPorCategoria(@PathVariable String categoria) {
        try {
            User currentUser = getCurrentUser();
            List<EntradaEstoque> entradas = entradaEstoqueRepository.findByCategoriaAndUser(categoria, currentUser);
            return ResponseEntity.ok(entradas);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao buscar compras por categoria: " + e.getMessage());
        }
    }

    @GetMapping("/fornecedor/{fornecedor}")
    public ResponseEntity<?> buscarPorFornecedor(@PathVariable String fornecedor) {
        try {
            User currentUser = getCurrentUser();
            List<EntradaEstoque> entradas = entradaEstoqueRepository.findByFornecedorContainingAndUser(fornecedor, currentUser);
            return ResponseEntity.ok(entradas);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao buscar compras por fornecedor: " + e.getMessage());
        }
    }

    @GetMapping("/saldo-baixo")
    public ResponseEntity<?> getEntradasComSaldoBaixo() {
        try {
            User currentUser = getCurrentUser();
            List<EntradaEstoque> entradas = entradaEstoqueRepository.findEntradasComSaldoBaixo(currentUser);
            return ResponseEntity.ok(entradas);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao buscar compras com saldo baixo: " + e.getMessage());
        }
    }

    @PostMapping("/limpar-dados-inconsistentes")
    public ResponseEntity<?> limparDadosInconsistentes() {
        try {
            User currentUser = getCurrentUser();

            List<EntradaEstoque> todasEntradas = entradaEstoqueRepository.findAll();
            List<EntradaEstoque> entradasSemUsuario = todasEntradas.stream()
                    .filter(e -> e.getUser() == null)
                    .collect(Collectors.toList());

            int entradasDeletadas = 0;
            for (EntradaEstoque entrada : entradasSemUsuario) {
                if (entrada.getItensVenda() == null || entrada.getItensVenda().isEmpty()) {
                    entradaEstoqueRepository.delete(entrada);
                    entradasDeletadas++;
                }
            }

            return ResponseEntity.ok(String.format(
                    "🔧 LIMPEZA DE DADOS CONCLUÍDA:\n" +
                            "• Total de entradas no sistema: %d\n" +
                            "• Entradas sem usuário: %d\n" +
                            "• Entradas deletadas (sem usuário e sem vendas): %d",
                    todasEntradas.size(),
                    entradasSemUsuario.size(),
                    entradasDeletadas
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("❌ Erro na limpeza: " + e.getMessage());
        }
    }
}