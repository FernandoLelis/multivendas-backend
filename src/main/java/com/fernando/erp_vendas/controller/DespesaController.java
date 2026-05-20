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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/despesas")
public class DespesaController {

    @Autowired
    private DespesaRepository despesaRepository;

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            return (User) authentication.getPrincipal();
        }
        throw new RuntimeException("Usuário não autenticado");
    }

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

    @GetMapping("/{id}")
    public ResponseEntity<?> buscarPorId(@PathVariable Long id) {
        try {
            User currentUser = getCurrentUser();
            Optional<Despesa> despesa = despesaRepository.findByIdAndUser(id, currentUser);
            return despesa.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao buscar despesa: " + e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> criarDespesa(@RequestBody Despesa despesa) {
        try {
            User currentUser = getCurrentUser();
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
            despesa.setUser(currentUser);
            Despesa despesaSalva = despesaRepository.save(despesa);
            return ResponseEntity.ok(despesaSalva);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao criar despesa: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> atualizarDespesa(@PathVariable Long id, @RequestBody Despesa despesaAtualizada) {
        try {
            User currentUser = getCurrentUser();
            Optional<Despesa> despesaExistenteOpt = despesaRepository.findByIdAndUser(id, currentUser);
            if (!despesaExistenteOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            Despesa despesaExistente = despesaExistenteOpt.get();
            if (despesaAtualizada.getDescricao() == null || despesaAtualizada.getDescricao().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Descrição é obrigatória");
            }
            if (despesaAtualizada.getValor() == null || despesaAtualizada.getValor().compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.badRequest().body("Valor deve ser maior que zero");
            }
            if (despesaAtualizada.getCategoria() == null || despesaAtualizada.getCategoria().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Categoria é obrigatória");
            }
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

    @DeleteMapping("/{id}")
    public ResponseEntity<?> excluirDespesa(@PathVariable Long id) {
        try {
            User currentUser = getCurrentUser();
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

    // ✅ TOTAL (ano atual) - mantido igual
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

    @GetMapping("/periodo")
    public ResponseEntity<?> buscarPorPeriodo(@RequestParam LocalDate inicio, @RequestParam LocalDate fim) {
        try {
            User currentUser = getCurrentUser();
            List<Despesa> despesas = despesaRepository.findByDataBetweenAndUserOrderByDataDesc(inicio, fim, currentUser);
            return ResponseEntity.ok(despesas);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao buscar despesas por período: " + e.getMessage());
        }
    }

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

    // ✅ CORRIGIDO: total-mes-atual com período pro-rata (do primeiro dia até agora)
    @GetMapping("/total-mes-atual")
    public ResponseEntity<?> calcularTotalMesAtual() {
        try {
            User currentUser = getCurrentUser();
            LocalDate agora = LocalDate.now();
            LocalDate inicioMes = agora.withDayOfMonth(1);
            BigDecimal total = despesaRepository.calcularTotalDespesasPorPeriodoDinamico(currentUser, inicioMes, agora);
            return ResponseEntity.ok(total != null ? total : BigDecimal.ZERO);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao calcular total do mês atual: " + e.getMessage());
        }
    }

    // ✅ CORRIGIDO: total-mes-anterior com período pro-rata (primeiro dia do mês anterior até mesmo dia/hora do mês atual)
    @GetMapping("/total-mes-anterior")
    public ResponseEntity<?> calcularTotalMesAnterior() {
        try {
            User currentUser = getCurrentUser();
            LocalDate agora = LocalDate.now();
            LocalDate inicioMesAnterior = agora.minusMonths(1).withDayOfMonth(1);
            LocalDate fimMesAnterior = agora.minusMonths(1);
            BigDecimal total = despesaRepository.calcularTotalDespesasPorPeriodoDinamico(currentUser, inicioMesAnterior, fimMesAnterior);
            return ResponseEntity.ok(total != null ? total : BigDecimal.ZERO);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao calcular total do mês anterior: " + e.getMessage());
        }
    }

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

    @GetMapping("/media-mensal")
    public ResponseEntity<?> calcularMediaMensal(@RequestParam(required = false) LocalDate inicio) {
        try {
            User currentUser = getCurrentUser();
            if (inicio == null) inicio = LocalDate.now().withDayOfYear(1);
            BigDecimal media = despesaRepository.calcularMediaMensalDespesas(currentUser, inicio);
            return ResponseEntity.ok(media != null ? media : BigDecimal.ZERO);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao calcular média mensal: " + e.getMessage());
        }
    }
}