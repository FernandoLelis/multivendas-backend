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
    // ✅✅✅ NOVA VERSÃO: COMPRAS COM MÚLTIPLOS PRODUTOS
    // ============================================================

    // ✅✅✅ NOVO: POST - Criar nova COMPRA com múltiplos produtos (COM DEBUG DETALHADO)
    @PostMapping("/compra")
    public ResponseEntity<?> criarCompra(@RequestBody Map<String, Object> compraData) {
        try {
            System.out.println("🔍 DEBUG COMPRA - Dados recebidos do frontend: " + compraData);
            System.out.println("🔍 DEBUG COMPRA - Chaves presentes: " + compraData.keySet());

            User currentUser = getCurrentUser();

            // ✅ 1️⃣ VALIDAR DADOS OBRIGATÓRIOS
            if (!compraData.containsKey("idPedidoCompra") || !compraData.containsKey("fornecedor") ||
                    !compraData.containsKey("itens")) {
                System.out.println("❌ DADOS INCOMPLETOS - Campos faltando:");
                System.out.println("   - idPedidoCompra: " + compraData.containsKey("idPedidoCompra"));
                System.out.println("   - fornecedor: " + compraData.containsKey("fornecedor"));
                System.out.println("   - itens: " + compraData.containsKey("itens"));
                return ResponseEntity.badRequest().body("Dados incompletos. Campos obrigatórios: idPedidoCompra, fornecedor, itens");
            }

            // ✅ 2️⃣ EXTRAIR DADOS BÁSICOS DA COMPRA
            String idPedidoCompra = compraData.get("idPedidoCompra").toString();
            String fornecedor = compraData.get("fornecedor").toString();
            String observacoes = compraData.containsKey("observacoes") ? compraData.get("observacoes").toString() : "";

            // ✅✅✅ DEBUG CRÍTICO: EXTRAIR E VALIDAR DATA
            System.out.println("📅 DEBUG CRÍTICO - Verificando campo 'data' no JSON...");
            Object dataObj = compraData.get("data");
            System.out.println("📅 DEBUG CRÍTICO - Objeto data recebido: " + dataObj);
            System.out.println("📅 DEBUG CRÍTICO - Tipo do objeto: " + (dataObj != null ? dataObj.getClass().getName() : "null"));

            if (dataObj != null) {
                System.out.println("📅 DEBUG CRÍTICO - String data: '" + dataObj.toString() + "'");
                System.out.println("📅 DEBUG CRÍTICO - Tamanho string: " + dataObj.toString().length());
                System.out.println("📅 DEBUG CRÍTICO - Contém Z: " + dataObj.toString().contains("Z"));
                System.out.println("📅 DEBUG CRÍTICO - Contém T: " + dataObj.toString().contains("T"));
                System.out.println("📅 DEBUG CRÍTICO - Contém +: " + dataObj.toString().contains("+"));
                System.out.println("📅 DEBUG CRÍTICO - Contém -: " + dataObj.toString().contains("-"));
            }

            LocalDateTime dataCompra = extrairDataMelhorado(dataObj);
            System.out.println("📅 DEBUG CRÍTICO - Data extraída pelo método: " + dataCompra);

            if (dataCompra == null) {
                System.out.println("⚠️ DEBUG CRÍTICO - Data extraída é NULL, verificando outros campos...");

                // Tentar campo dataCompra (alternativo)
                if (compraData.containsKey("dataCompra")) {
                    Object dataCompraObj = compraData.get("dataCompra");
                    System.out.println("📅 DEBUG CRÍTICO - Tentando campo dataCompra: " + dataCompraObj);
                    dataCompra = extrairDataMelhorado(dataCompraObj);
                    System.out.println("📅 DEBUG CRÍTICO - Data extraída de dataCompra: " + dataCompra);
                }

                // Tentar campo dataEntrada (legacy)
                if (dataCompra == null && compraData.containsKey("dataEntrada")) {
                    Object dataEntradaObj = compraData.get("dataEntrada");
                    System.out.println("📅 DEBUG CRÍTICO - Tentando campo dataEntrada: " + dataEntradaObj);
                    dataCompra = extrairDataMelhorado(dataEntradaObj);
                    System.out.println("📅 DEBUG CRÍTICO - Data extraída de dataEntrada: " + dataCompra);
                }

                // Se ainda nulo, usar hoje
                if (dataCompra == null) {
                    System.out.println("⚠️ DEBUG CRÍTICO - Nenhum campo de data válido encontrado, usando data atual");
                    dataCompra = LocalDateTime.now();
                }
            }

            System.out.println("📅 DEBUG FINAL - Data da compra que será usada: " + dataCompra);
            System.out.println("📅 DEBUG FINAL - Data formatada (yyyy-MM-dd): " + dataCompra.toLocalDate());

            // ✅ 3️⃣ VALIDAR E EXTRAIR ITENS DA COMPRA
            List<Map<String, Object>> itensData = (List<Map<String, Object>>) compraData.get("itens");

            System.out.println("🔍 DEBUG ITENS COMPRA - Tamanho: " + (itensData != null ? itensData.size() : "null"));

            if (itensData == null || itensData.isEmpty()) {
                return ResponseEntity.badRequest().body("A compra deve conter pelo menos um produto");
            }

            // ✅ DEBUG DETALHADO DE CADA ITEM
            for (int i = 0; i < itensData.size(); i++) {
                Map<String, Object> item = itensData.get(i);
                System.out.println("🔍 DEBUG ITEM COMPRA " + i + ": " + item);
            }

            // ✅ 4️⃣ VERIFICAR SE JÁ EXISTE COMPRA COM MESMO ID DO PEDIDO
            if (compraRepository.findByIdPedidoCompraAndUser(idPedidoCompra, currentUser).isPresent()) {
                return ResponseEntity.badRequest().body("Já existe uma compra com este ID do pedido: " + idPedidoCompra);
            }

            // ✅✅✅ 5️⃣ CRIAR A COMPRA COM DATA CORRETA
            Compra compra = new Compra(dataCompra, idPedidoCompra, fornecedor, observacoes, currentUser);

            System.out.println("✅ Compra criada com data: " + compra.getData());

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
                ItemCompra item = new ItemCompra(compra, produto, quantidade, custoUnitario, currentUser);

                // ✅ CRIAR O LOTE (EntradaEstoque) automaticamente
                item.criarLote();

                itens.add(item);
            }

            // ✅ 7️⃣ ADICIONAR ITENS À COMPRA
            compra.setItens(itens);

            // ✅ 8️⃣ SALVAR COMPRA (cascade salvará os itens e lotes)
            Compra compraSalva = compraRepository.save(compra);

            // ✅ 9️⃣ BUSCAR COMPRA COMPLETA (COM ITENS E LOTES PERSISTIDOS)
            Optional<Compra> compraCompleta = compraRepository.findByIdPedidoCompraAndUser(idPedidoCompra, currentUser);
            if (!compraCompleta.isPresent()) {
                throw new RuntimeException("Erro ao recuperar compra criada: " + idPedidoCompra);
            }

            System.out.println("✅ Compra criada com sucesso: " + compraCompleta.get().getIdPedidoCompra() +
                    ", Data final: " + compraCompleta.get().getData() +
                    ", Total produtos: " + compraCompleta.get().getItens().size());

            return ResponseEntity.ok(new CompraDTO(compraCompleta.get()));

        } catch (NumberFormatException e) {
            System.out.println("❌ NumberFormatException: " + e.getMessage());
            return ResponseEntity.badRequest().body("Erro de formato numérico: " + e.getMessage());
        } catch (ClassCastException e) {
            System.out.println("❌ ClassCastException: " + e.getMessage());
            return ResponseEntity.badRequest().body("Erro de tipo de dados: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("❌ Erro ao criar compra: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Erro ao criar compra: " + e.getMessage());
        }
    }

    // ✅✅✅ NOVO: Método para extrair data CORRIGIDO (resolve timezone - AGORA USA UTC)
    private LocalDateTime extrairDataMelhorado(Object dataObj) {
        if (dataObj == null || dataObj.toString().isEmpty()) {
            System.out.println("📅 DEBUG EXTRACAO - Data nula ou vazia");
            return null;
        }

        String dataString = dataObj.toString().trim();
        System.out.println("📅 DEBUG EXTRACAO - String recebida: '" + dataString + "'");
        System.out.println("📅 DEBUG EXTRACAO - Tamanho string: " + dataString.length());

        try {
            // ✅ CASO 1: Formato "YYYY-MM-DDTHH:mm:ss" (ISO completo) - COM OFFSET
            if (dataString.contains("T")) {
                System.out.println("📅 DEBUG EXTRACAO - Contém T, processando como ISO...");
                try {
                    // ✅ CORREÇÃO: Se tem offset (+ ou -), usar OffsetDateTime e converter para UTC
                    if (dataString.contains("+") || dataString.contains("-")) {
                        // Verificar se é realmente um offset (depois do T)
                        int timezoneIndex = Math.max(dataString.lastIndexOf('+'), dataString.lastIndexOf('-'));
                        if (timezoneIndex > dataString.indexOf('T')) {
                            System.out.println("📅 DEBUG EXTRACAO - Tem offset, usando OffsetDateTime...");
                            // ✅ USAR OffsetDateTime para preservar o offset
                            OffsetDateTime odt = OffsetDateTime.parse(dataString);
                            System.out.println("📅 DEBUG EXTRACAO - OffsetDateTime parseado: " + odt);

                            // Converter para UTC (Instant) e depois para LocalDateTime
                            Instant instant = odt.toInstant();
                            LocalDateTime dataUTC = LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
                            System.out.println("📅 DEBUG EXTRACAO - Convertido para UTC: " + dataUTC);

                            return dataUTC;
                        }
                    }

                    // Se não tem offset, pode ter Z (UTC)
                    if (dataString.endsWith("Z")) {
                        System.out.println("📅 DEBUG EXTRACAO - Termina com Z, tratando como UTC...");
                        // Remover Z e parsear
                        String semZ = dataString.substring(0, dataString.length() - 1);
                        LocalDateTime dataCompleta = LocalDateTime.parse(semZ);
                        System.out.println("📅 DEBUG EXTRACAO - Data parseada (com Z): " + dataCompleta);
                        return dataCompleta;
                    }

                    // Se não tem offset nem Z, parse normalmente
                    System.out.println("📅 DEBUG EXTRACAO - Sem offset nem Z, parse normal...");
                    LocalDateTime dataCompleta = LocalDateTime.parse(dataString);
                    System.out.println("📅 DEBUG EXTRACAO - Data parseada (sem timezone): " + dataCompleta);
                    return dataCompleta;

                } catch (Exception e) {
                    System.out.println("❌ DEBUG EXTRACAO - Erro no parse com offset: " + e.getMessage());
                    // Tentar fallback para parse normal
                    try {
                        LocalDateTime dataCompleta = LocalDateTime.parse(dataString);
                        return dataCompleta;
                    } catch (Exception e2) {
                        System.out.println("❌ DEBUG EXTRACAO - Erro em fallback: " + e2.getMessage());
                        return null;
                    }
                }
            }

            // ✅ CASO 2: Formato "YYYY-MM-DD" (apenas data)
            System.out.println("📅 DEBUG EXTRACAO - Não contém T, tentando como LocalDate...");
            try {
                java.time.LocalDate dataApenas = java.time.LocalDate.parse(dataString);
                System.out.println("📅 DEBUG EXTRACAO - Data parseada (LocalDate): " + dataApenas);

                // Converter para LocalDateTime no início do dia em UTC
                LocalDateTime dataInicioDia = dataApenas.atStartOfDay();
                System.out.println("📅 DEBUG EXTRACAO - Convertido para início do dia: " + dataInicioDia);

                return dataInicioDia;
            } catch (Exception e) {
                System.out.println("❌ DEBUG EXTRACAO - Erro parse LocalDate: " + e.getMessage());
            }

        } catch (Exception e1) {
            System.out.println("❌ DEBUG EXTRACAO - Erro no parse principal: " + e1.getMessage());
        }

        System.out.println("❌ DEBUG EXTRACAO - Nenhum formato reconhecido: " + dataString);
        return null;
    }

    // ✅✅✅ NOVO: PUT - Atualizar compra existente DO USUÁRIO (ATUALIZADO)
    @PutMapping("/compra/{id}")
    public ResponseEntity<?> atualizarCompra(@PathVariable Long id, @RequestBody Map<String, Object> compraData) {
        try {
            User currentUser = getCurrentUser();

            // ✅ 1️⃣ BUSCAR COMPRA EXISTENTE
            Optional<Compra> compraExistenteOpt = compraRepository.findByIdAndUser(id, currentUser);
            if (!compraExistenteOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            Compra compraExistente = compraExistenteOpt.get();

            // ✅ 2️⃣ VERIFICAR SE JÁ EXISTE OUTRA COMPRA COM MESMO ID DO PEDIDO
            if (compraData.containsKey("idPedidoCompra")) {
                String novoIdPedidoCompra = compraData.get("idPedidoCompra").toString();
                Optional<Compra> compraComMesmoPedido = compraRepository.findByIdPedidoCompraAndUser(novoIdPedidoCompra, currentUser);
                if (compraComMesmoPedido.isPresent() && !compraComMesmoPedido.get().getId().equals(id)) {
                    return ResponseEntity.badRequest().body("Já existe outra compra com este ID do pedido");
                }
                compraExistente.setIdPedidoCompra(novoIdPedidoCompra);
            }

            // ✅ 3️⃣ ATUALIZAR APENAS CAMPOS PERMITIDOS (INCLUINDO DATA)
            if (compraData.containsKey("data")) {
                LocalDateTime novaData = extrairDataMelhorado(compraData.get("data"));
                if (novaData != null) {
                    compraExistente.setData(novaData);
                    System.out.println("📅 DEBUG - Data atualizada para (UTC): " + novaData);
                }
            }
            if (compraData.containsKey("fornecedor")) {
                compraExistente.setFornecedor(compraData.get("fornecedor").toString());
            }
            if (compraData.containsKey("observacoes")) {
                compraExistente.setObservacoes(compraData.get("observacoes").toString());
            }

            // ✅ 4️⃣ SALVAR COMPRA ATUALIZADA
            Compra compraSalva = compraRepository.save(compraExistente);

            return ResponseEntity.ok(new CompraDTO(compraSalva));

        } catch (Exception e) {
            System.out.println("❌ Erro ao atualizar compra: " + e.getMessage());
            return ResponseEntity.badRequest().body("Erro ao atualizar compra: " + e.getMessage());
        }
    }

    // ✅✅✅ NOVO: DELETE - Excluir compra com validação DO USUÁRIO
    @DeleteMapping("/compra/{id}")
    public ResponseEntity<?> excluirCompra(@PathVariable Long id) {
        try {
            User currentUser = getCurrentUser();

            Optional<Compra> compraOpt = compraRepository.findByIdAndUser(id, currentUser);
            if (!compraOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            Compra compra = compraOpt.get();

            // ✅ VERIFICAR SE ALGUM LOTE FOI CONSUMIDO (não pode excluir se já vendeu)
            for (ItemCompra item : compra.getItens()) {
                if (item.getLote() != null && item.getLote().getSaldo() < item.getLote().getQuantidade()) {
                    return ResponseEntity.badRequest()
                            .body("Não é possível excluir compra: lote " + item.getLote().getId() +
                                    " já foi parcialmente consumido. Saldo atual: " +
                                    item.getLote().getSaldo() + "/" + item.getLote().getQuantidade());
                }
            }

            // ✅ EXCLUIR COMPRA (cascade excluirá itens e lotes)
            compraRepository.delete(compra);

            System.out.println("✅ Compra excluída: " + compra.getIdPedidoCompra());
            return ResponseEntity.noContent().build();

        } catch (Exception e) {
            System.out.println("❌ Erro ao excluir compra: " + e.getMessage());
            return ResponseEntity.badRequest().body("Erro ao excluir compra: " + e.getMessage());
        }
    }

    // ✅✅✅ NOVO: GET - Listar todas as compras DO USUÁRIO
    @GetMapping("/compras")
    @SuppressWarnings("unchecked")
    public ResponseEntity<?> listarTodasCompras() {
        try {
            User currentUser = getCurrentUser();
            System.out.println("🔍 DEBUG COMPRAS - Buscando compras para usuário: " + currentUser.getEmail());

            List<Compra> compras = compraRepository.findByUserOrderByDataDesc(currentUser);
            System.out.println("📊 DEBUG COMPRAS - Total de compras encontradas: " + compras.size());

            // ✅ CONVERTER PARA DTO
            List<CompraDTO> comprasDTO = compras.stream()
                    .map(CompraDTO::new)
                    .collect(Collectors.toList());

            System.out.println("✅ Compras convertidas para DTO: " + comprasDTO.size());
            return ResponseEntity.ok(comprasDTO);
        } catch (Exception e) {
            System.out.println("❌ ERRO CRÍTICO em listarTodasCompras: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Erro ao listar compras: " + e.getMessage());
        }
    }

    // ✅✅✅ NOVO: GET - Buscar compra por ID DO USUÁRIO
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

    // ✅✅✅ NOVO: GET - Buscar compra por ID do Pedido DO USUÁRIO
    @GetMapping("/compra/pedido/{idPedidoCompra}")
    public ResponseEntity<?> buscarCompraPorIdPedido(@PathVariable String idPedidoCompra) {
        try {
            User currentUser = getCurrentUser();
            Optional<Compra> compra = compraRepository.findByIdPedidoCompraAndUser(idPedidoCompra, currentUser);
            return compra.map(c -> ResponseEntity.ok(new CompraDTO(c)))
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao buscar compra por ID do pedido: " + e.getMessage());
        }
    }

    // ✅✅✅ NOVO: GET - Buscar compras por fornecedor DO USUÁRIO
    @GetMapping("/compras/fornecedor/{fornecedor}")
    public ResponseEntity<?> buscarComprasPorFornecedor(@PathVariable String fornecedor) {
        try {
            User currentUser = getCurrentUser();
            List<Compra> compras = compraRepository.findByFornecedorContainingAndUser(fornecedor, currentUser);

            List<CompraDTO> comprasDTO = compras.stream()
                    .map(CompraDTO::new)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(comprasDTO);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao buscar compras por fornecedor: " + e.getMessage());
        }
    }

    // ✅✅✅ NOVO: GET - Listar TODAS as compras (sistema novo + sistema antigo) DO USUÁRIO (CORRIGIDO)
    @GetMapping("/compras-unificadas")
    public ResponseEntity<?> listarTodasComprasUnificadas() {
        try {
            User currentUser = getCurrentUser();
            System.out.println("🔍 DEBUG COMPRAS UNIFICADAS - Buscando compras para usuário: " + currentUser.getEmail());

            List<CompraDTO> todasCompras = new ArrayList<>();

            // ✅ 1️⃣ BUSCAR COMPRAS DO SISTEMA NOVO (tabela compra)
            List<Compra> comprasNovas = compraRepository.findByUserOrderByDataDesc(currentUser);
            System.out.println("📊 DEBUG - Compras sistema novo: " + comprasNovas.size());

            for (Compra compra : comprasNovas) {
                todasCompras.add(new CompraDTO(compra));
            }

            // ✅ 2️⃣ BUSCAR COMPRAS DO SISTEMA ANTIGO (entrada_estoque que não são de ItemCompra)
            // Buscar todas as entradas do usuário
            List<EntradaEstoque> entradas = entradaEstoqueRepository.findByUserOrderByDataEntradaDesc(currentUser);
            System.out.println("📊 DEBUG - Total de entradas: " + entradas.size());

            // Filtrar apenas as entradas que NÃO têm item_compra_id
            List<EntradaEstoque> entradasAntigas = entradas.stream()
                    .filter(entrada -> entrada.getItemCompra() == null) // ✅ CORREÇÃO: usar getItemCompra() em vez de verificar ID
                    .collect(Collectors.toList());

            System.out.println("📊 DEBUG - Compras sistema antigo (entradas sem item_compra): " + entradasAntigas.size());

            // ✅ 3️⃣ CONVERTER ENTRADAS ANTIGAS PARA CompraDTO (compra "fake" com 1 item)
            for (EntradaEstoque entrada : entradasAntigas) {
                todasCompras.add(converterEntradaParaCompraDTO(entrada));
            }

            // ✅ 4️⃣ ORDENAR TODAS POR DATA (mais recente primeiro)
            todasCompras.sort((c1, c2) -> {
                if (c1.getData() == null && c2.getData() == null) return 0;
                if (c1.getData() == null) return 1;
                if (c2.getData() == null) return -1;
                return c2.getData().compareTo(c1.getData());
            });

            System.out.println("✅ Compras unificadas: " + todasCompras.size());
            return ResponseEntity.ok(todasCompras);
        } catch (Exception e) {
            System.out.println("❌ ERRO CRÍTICO em listarTodasComprasUnificadas: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Erro ao listar compras: " + e.getMessage());
        }
    }

    // ✅ MÉTODO AUXILIAR: Converter EntradaEstoque (sistema antigo) para CompraDTO
    private CompraDTO converterEntradaParaCompraDTO(EntradaEstoque entrada) {
        CompraDTO dto = new CompraDTO();

        // Definir ID negativo para indicar que é do sistema antigo
        dto.setId(-entrada.getId()); // ID negativo para diferenciar

        // Usar os campos da entrada
        dto.setData(entrada.getDataEntrada());
        dto.setFornecedor(entrada.getFornecedor());
        dto.setObservacoes(entrada.getObservacoes());
        dto.setIdPedidoCompra(entrada.getIdPedidoCompra());

        // Calcular total da compra (custo total)
        dto.setTotalCompra(entrada.getCustoTotal());

        // ✅ IMPORTANTE: Marcar que é do sistema antigo
        dto.setSistemaAntigo(true);

        // ✅ CRIAR UM ITEM SIMULADO PARA A COMPRA
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
    // ✅✅✅ MÉTODOS LEGACY (MANTIDOS PARA COMPATIBILIDADE)
    // ============================================================

    // ✅ CORRIGIDO: Registrar nova entrada de estoque (COMPRA) PARA O USUÁRIO COM DATA
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

            // 🆕 VERIFICAR SE JÁ EXISTE COMPRA COM MESMO ID PEDIDO PARA ESTE USUÁRIO
            if (entradaEstoqueRepository.findByIdPedidoCompraAndUser(idPedidoCompra, currentUser).isPresent()) {
                return ResponseEntity.badRequest()
                        .body("Já existe uma compra cadastrada com este ID do Pedido: " + idPedidoCompra);
            }

            // ✅ CORREÇÃO: Cria entrada com cálculo automático de custo unitário e saldo
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

            // ✅ NOVO: DEFINIR DATA PERSONALIZADA SE FORNECIDA (AGORA EM UTC)
            if (dataEntrada != null && !dataEntrada.trim().isEmpty()) {
                try {
                    // Usar o mesmo método para extrair data (que agora converte para UTC)
                    LocalDateTime dataCustomizada = extrairDataMelhorado(dataEntrada);
                    if (dataCustomizada != null) {
                        entrada.setDataEntrada(dataCustomizada);
                    }
                } catch (Exception e) {
                    // Se falhar, mantém a data atual (comportamento original)
                    System.out.println("⚠️ Data inválida, usando data atual: " + dataEntrada);
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

    // ✅ ATUALIZADO: Atualizar entrada de estoque (COMPRA) DO USUÁRIO COM DATA
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

            // ✅ NOVO: ATUALIZAR DATA SE FORNECIDA (AGORA EM UTC)
            if (dataEntrada != null && !dataEntrada.trim().isEmpty()) {
                try {
                    LocalDateTime dataCustomizada = extrairDataMelhorado(dataEntrada);
                    if (dataCustomizada != null) {
                        entradaExistente.setDataEntrada(dataCustomizada);
                    }
                } catch (Exception e) {
                    System.out.println("⚠️ Data inválida na atualização, mantendo data original: " + dataEntrada);
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

    // ✅ ATUALIZADO: Listar todas as entradas de estoque (COMPRAS) DO USUÁRIO
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

    // ✅ ATUALIZADO: Listar entradas de estoque de um produto (HISTÓRICO DE COMPRAS) DO USUÁRIO
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

            entradaEstoqueRepository.delete(entrada);
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao excluir compra: " + e.getMessage());
        }
    }

    // 🆕 GET - Buscar entrada por ID DO USUÁRIO
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

    // 🆕 GET - Buscar entradas por categoria DO USUÁRIO
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

    // 🆕 GET - Buscar entradas por fornecedor DO USUÁRIO
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

    // 🆕 GET - Entradas com saldo baixo DO USUÁRIO
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

    // 🆕 POST - Limpar dados inconsistentes (mantido para compatibilidade)
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