package com.fernando.erp_vendas.controller;

import com.fernando.erp_vendas.model.Despesa;
import com.fernando.erp_vendas.model.User;
import com.fernando.erp_vendas.repository.DespesaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/despesas")
public class DespesaController {

    @Autowired
    private DespesaRepository despesaRepository;

    // 🆕 MÉTODO PARA OBTER USUÁRIO LOGADO
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            return (User) authentication.getPrincipal();
        }
        throw new RuntimeException("Usuário não autenticado");
    }

    // ✅ ATUALIZADO: GET - Listar todas as despesas DO USUÁRIO
    @GetMapping
    public ResponseEntity<?> listarTodas() {
        try {
            User currentUser = getCurrentUser();
            List<Despesa> despesas = despesaRepository.findByUserOrderByDataDesc(currentUser);
            return ResponseEntity.ok(despesas);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao listar despesas: " + e.getMessage());
        }
    }

    // ✅ ATUALIZADO: GET - Buscar despesa por ID DO USUÁRIO
    @GetMapping("/{id}")
    public ResponseEntity<?> buscarPorId(@PathVariable Long id) {
        try {
            User currentUser = getCurrentUser();
            Optional<Despesa> despesa = despesaRepository.findByIdAndUser(id, currentUser);
            return despesa.map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao buscar despesa: " + e.getMessage());
        }
    }

    // ✅ ATUALIZADO: POST - Criar nova despesa PARA O USUÁRIO
    @PostMapping
    public ResponseEntity<?> criarDespesa(@RequestBody Despesa despesa) {
        try {
            User currentUser = getCurrentUser();

            // Validações básicas
            if (despesa.getDescricao() == null || despesa.getDescricao().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Descrição é obrigatória");
            }
            if (despesa.getValor() == null || despesa.getValor().compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.badRequest().body("Valor deve ser maior que zero");
            }
            if (despesa.getData() == null) {
                despesa.setData(LocalDate.now());
            }
            if (despesa.getCategoria() == null || despesa.getCategoria().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Categoria é obrigatória");
            }

            // 🆕 ASSOCIAR USUÁRIO À DESPESA
            despesa.setUser(currentUser);

            Despesa despesaSalva = despesaRepository.save(despesa);
            return ResponseEntity.ok(despesaSalva);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao criar despesa: " + e.getMessage());
        }
    }

    // ✅ ATUALIZADO: PUT - Atualizar despesa existente DO USUÁRIO
    @PutMapping("/{id}")
    public ResponseEntity<?> atualizarDespesa(@PathVariable Long id, @RequestBody Despesa despesaAtualizada) {
        try {
            User currentUser = getCurrentUser();

            Optional<Despesa> despesaExistenteOpt = despesaRepository.findByIdAndUser(id, currentUser);
            if (!despesaExistenteOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            Despesa despesaExistente = despesaExistenteOpt.get();

            // Validações
            if (despesaAtualizada.getDescricao() == null || despesaAtualizada.getDescricao().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Descrição é obrigatória");
            }
            if (despesaAtualizada.getValor() == null || despesaAtualizada.getValor().compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.badRequest().body("Valor deve ser maior que zero");
            }
            if (despesaAtualizada.getCategoria() == null || despesaAtualizada.getCategoria().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Categoria é obrigatória");
            }

            // Atualizar campos
            despesaExistente.setDescricao(despesaAtualizada.getDescricao());
            despesaExistente.setValor(despesaAtualizada.getValor());
            despesaExistente.setData(despesaAtualizada.getData());
            despesaExistente.setCategoria(despesaAtualizada.getCategoria());
            despesaExistente.setObservacoes(despesaAtualizada.getObservacoes());
            despesaExistente.setRecorrente(despesaAtualizada.isRecorrente());

            Despesa despesaSalva = despesaRepository.save(despesaExistente);
            return ResponseEntity.ok(despesaSalva);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao atualizar despesa: " + e.getMessage());
        }
    }

    // ✅ ATUALIZADO: DELETE - Excluir despesa DO USUÁRIO
    @DeleteMapping("/{id}")
    public ResponseEntity<?> excluirDespesa(@PathVariable Long id) {
        try {
            User currentUser = getCurrentUser();

            // 🆕 VERIFICAR SE DESPESA EXISTE E PERTENCE AO USUÁRIO
            Optional<Despesa> despesa = despesaRepository.findByIdAndUser(id, currentUser);
            if (!despesa.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            despesaRepository.deleteById(id);
            return ResponseEntity.noContent().build();

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao excluir despesa: " + e.getMessage());
        }
    }

    // ✅ ATUALIZADO: GET - Listar categorias distintas DO USUÁRIO
    @GetMapping("/categorias")
    public ResponseEntity<?> listarCategorias() {
        try {
            User currentUser = getCurrentUser();
            List<String> categorias = despesaRepository.findCategoriasDistintas(currentUser);
            return ResponseEntity.ok(categorias);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao listar categorias: " + e.getMessage());
        }
    }

    // ✅ ATUALIZADO: GET - Calcular total de despesas DO USUÁRIO (para dashboard)
    @GetMapping("/total")
    public ResponseEntity<?> calcularTotalDespesas() {
        try {
            User currentUser = getCurrentUser();
            LocalDate inicioMes = LocalDate.now().withDayOfMonth(1);
            LocalDate fimMes = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());

            BigDecimal total = despesaRepository.calcularTotalDespesasPorPeriodo(currentUser, inicioMes, fimMes);
            return ResponseEntity.ok(total != null ? total : BigDecimal.ZERO);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao calcular total de despesas: " + e.getMessage());
        }
    }

    // ✅ ATUALIZADO: GET - Buscar despesas por categoria DO USUÁRIO
    @GetMapping("/categoria/{categoria}")
    public ResponseEntity<?> buscarPorCategoria(@PathVariable String categoria) {
        try {
            User currentUser = getCurrentUser();
            List<Despesa> despesas = despesaRepository.findByCategoriaAndUserOrderByDataDesc(categoria, currentUser);
            return ResponseEntity.ok(despesas);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao buscar despesas por categoria: " + e.getMessage());
        }
    }

    // ✅ ATUALIZADO: GET - Buscar despesas por período DO USUÁRIO
    @GetMapping("/periodo")
    public ResponseEntity<?> buscarPorPeriodo(
            @RequestParam LocalDate inicio,
            @RequestParam LocalDate fim) {
        try {
            User currentUser = getCurrentUser();
            List<Despesa> despesas = despesaRepository.findByDataBetweenAndUserOrderByDataDesc(inicio, fim, currentUser);
            return ResponseEntity.ok(despesas);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao buscar despesas por período: " + e.getMessage());
        }
    }

    // 🆕 GET - Buscar despesas recorrentes DO USUÁRIO
    @GetMapping("/recorrentes")
    public ResponseEntity<?> buscarRecorrentes() {
        try {
            User currentUser = getCurrentUser();
            List<Despesa> despesas = despesaRepository.findByRecorrenteTrueAndUserOrderByDataDesc(currentUser);
            return ResponseEntity.ok(despesas);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao buscar despesas recorrentes: " + e.getMessage());
        }
    }

    // 🆕 GET - Buscar despesas do mês atual DO USUÁRIO
    @GetMapping("/mes-atual")
    public ResponseEntity<?> buscarDespesasMesAtual() {
        try {
            User currentUser = getCurrentUser();
            List<Despesa> despesas = despesaRepository.findDespesasDoMesAtual(currentUser);
            return ResponseEntity.ok(despesas);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao buscar despesas do mês atual: " + e.getMessage());
        }
    }

    // 🆕 GET - Calcular total de despesas do mês atual DO USUÁRIO
    @GetMapping("/total-mes-atual")
    public ResponseEntity<?> calcularTotalMesAtual() {
        try {
            User currentUser = getCurrentUser();
            BigDecimal total = despesaRepository.calcularTotalDespesasMesAtual(currentUser);
            return ResponseEntity.ok(total != null ? total : BigDecimal.ZERO);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao calcular total do mês atual: " + e.getMessage());
        }
    }

    // 🆕 GET - Calcular total de despesas do mês ANTERIOR DO USUÁRIO (Novo Endpoint)
    @GetMapping("/total-mes-anterior")
    public ResponseEntity<?> calcularTotalMesAnterior() {
        try {
            User currentUser = getCurrentUser();

            // Retrocede 1 mês a partir da data atual
            LocalDate dataMesAnterior = LocalDate.now().minusMonths(1);
            LocalDate inicioMes = dataMesAnterior.withDayOfMonth(1);
            LocalDate fimMes = dataMesAnterior.withDayOfMonth(dataMesAnterior.lengthOfMonth());

            BigDecimal total = despesaRepository.calcularTotalDespesasPorPeriodo(currentUser, inicioMes, fimMes);
            return ResponseEntity.ok(total != null ? total : BigDecimal.ZERO);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao calcular total do mês anterior: " + e.getMessage());
        }
    }

    // ✅ ATUALIZADO: GET - Calcular total de despesas do ANO ATUAL DO USUÁRIO
    @GetMapping("/total-ano-atual")
    public ResponseEntity<?> calcularTotalDespesasAnoAtual() {
        try {
            User currentUser = getCurrentUser();
            BigDecimal total = despesaRepository.calcularTotalDespesasAnoAtual(currentUser);
            return ResponseEntity.ok(total != null ? total : BigDecimal.ZERO);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao calcular total de despesas do ano atual: " + e.getMessage());
        }
    }

    // 🆕 GET - Top 5 categorias com maior gasto DO USUÁRIO
    @GetMapping("/top-categorias")
    public ResponseEntity<?> getTopCategorias() {
        try {
            User currentUser = getCurrentUser();
            List<Object[]> topCategorias = despesaRepository.findTopCategoriasComMaiorGasto(currentUser);
            return ResponseEntity.ok(topCategorias);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao buscar top categorias: " + e.getMessage());
        }
    }

    // 🆕 GET - Buscar despesas por descrição (busca parcial) DO USUÁRIO
    @GetMapping("/buscar")
    public ResponseEntity<?> buscarPorDescricao(@RequestParam String descricao) {
        try {
            User currentUser = getCurrentUser();
            List<Despesa> despesas = despesaRepository.findByDescricaoContainingAndUser(descricao, currentUser);
            return ResponseEntity.ok(despesas);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao buscar despesas: " + e.getMessage());
        }
    }

    // 🆕 GET - Calcular média mensal de despesas DO USUÁRIO
    @GetMapping("/media-mensal")
    public ResponseEntity<?> calcularMediaMensal(@RequestParam(required = false) LocalDate inicio) {
        try {
            User currentUser = getCurrentUser();

            // Se não for especificado, usar início do ano atual
            if (inicio == null) {
                inicio = LocalDate.now().withDayOfYear(1);
            }

            BigDecimal media = despesaRepository.calcularMediaMensalDespesas(currentUser, inicio);
            return ResponseEntity.ok(media != null ? media : BigDecimal.ZERO);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao calcular média mensal: " + e.getMessage());
        }
    }
}