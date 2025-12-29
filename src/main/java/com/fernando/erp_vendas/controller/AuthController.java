package com.fernando.erp_vendas.controller;

import com.fernando.erp_vendas.model.User;
import com.fernando.erp_vendas.repository.UserRepository;
import com.fernando.erp_vendas.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${admin.reset.token}")
    private String adminResetToken;

    // Endpoint de Login
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            System.out.println("🔐 Tentando login para: " + loginRequest.getEmail());

            // Buscar usuário por email
            Optional<User> userOptional = userRepository.findByEmail(loginRequest.getEmail());

            if (userOptional.isEmpty()) {
                System.out.println("❌ Usuário não encontrado: " + loginRequest.getEmail());
                return ResponseEntity.status(401).body(createErrorResponse("Usuário não encontrado"));
            }

            User user = userOptional.get();
            System.out.println("✅ Usuário encontrado: " + user.getEmail());

            // Verificar senha
            if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
                System.out.println("❌ Senha incorreta para: " + loginRequest.getEmail());
                return ResponseEntity.status(401).body(createErrorResponse("Senha incorreta"));
            }

            // Verificar se usuário está ativo
            if (!user.isAtivo()) {
                System.out.println("❌ Usuário desativado: " + loginRequest.getEmail());
                return ResponseEntity.status(401).body(createErrorResponse("Usuário desativado"));
            }

            // Gerar token JWT
            String token = jwtService.generateToken(user.getEmail());
            System.out.println("✅ Token gerado para: " + loginRequest.getEmail());

            // Retornar resposta de sucesso
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("user", createUserResponse(user));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.out.println("❌ Erro no login: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(createErrorResponse("Erro interno no servidor: " + e.getMessage()));
        }
    }

    // Endpoint de Registro
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest registerRequest) {
        try {
            System.out.println("📝 Tentando registrar: " + registerRequest.getEmail());

            // Verificar se email já existe
            if (userRepository.existsByEmail(registerRequest.getEmail())) {
                System.out.println("❌ Email já existe: " + registerRequest.getEmail());
                return ResponseEntity.status(400).body(createErrorResponse("Email já cadastrado"));
            }

            // Criar novo usuário
            User user = new User();
            user.setEmail(registerRequest.getEmail());
            user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
            user.setNome(registerRequest.getNome());
            user.setAtivo(true);

            System.out.println("✅ Usuário criado, salvando no banco...");

            // Salvar usuário
            User savedUser = userRepository.save(user);
            System.out.println("✅ Usuário salvo com ID: " + savedUser.getId());

            // Gerar token JWT
            String token = jwtService.generateToken(savedUser.getEmail());
            System.out.println("✅ Token gerado para novo usuário");

            // Retornar resposta de sucesso
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("user", createUserResponse(savedUser));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.out.println("❌ Erro no registro: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(createErrorResponse("Erro ao criar usuário: " + e.getMessage()));
        }
    }

    /**
     * 🚨 ENDPOINT ADMINISTRATIVO TEMPORÁRIO 🚨
     * Apenas para desenvolvimento - RESETAR SENHAS COM HASH INCOMPATÍVEL
     * REQUER: Header "X-Admin-Token" com valor configurado em admin.reset.token
     *
     * USO: Resetar senhas de usuários antigos migrados com hash incompatível
     * REMOVER ANTES DO DEPLOY EM PRODUÇÃO
     */
    @PostMapping("/admin-reset-password")
    public ResponseEntity<?> adminResetPassword(
            @RequestHeader("X-Admin-Token") String adminToken,
            @RequestBody AdminResetRequest request) {

        try {
            System.out.println("🔧 ADMIN RESET: Solicitado para '" + request.getEmail() + "'");
            System.out.println("🔧 Token recebido: " + adminToken);
            System.out.println("🔧 Token esperado: " + adminResetToken);

            // 1. Verificar token administrativo
            if (!adminResetToken.equals(adminToken)) {
                System.out.println("❌ ADMIN RESET: Token inválido recebido");
                return ResponseEntity.status(401)
                        .body(createErrorResponse("Token administrativo inválido"));
            }

            System.out.println("✅ Token válido. Buscando usuário...");

            // 2. DEBUG: Listar TODOS os usuários primeiro
            System.out.println("📋 LISTA COMPLETA DE USUÁRIOS NO BANCO:");
            List<User> allUsers = userRepository.findAll();
            for (User u : allUsers) {
                System.out.println("   ID: " + u.getId() +
                        " | Email: '" + u.getEmail() + "'" +
                        " | Email solicitado: '" + request.getEmail() + "'" +
                        " | Iguais? " + u.getEmail().equals(request.getEmail()) +
                        " | Iguais (ignore case)? " + u.getEmail().equalsIgnoreCase(request.getEmail()));
            }

            // 3. Buscar usuário pelo método do repository
            System.out.println("🔍 Buscando com findByEmail('" + request.getEmail() + "')...");
            Optional<User> userOptional = userRepository.findByEmail(request.getEmail());

            if (userOptional.isPresent()) {
                System.out.println("✅ Usuário encontrado via findByEmail");
            } else {
                System.out.println("⚠️  findByEmail NÃO encontrou. Tentando busca manual...");

                // Busca manual case-insensitive
                User foundUser = null;
                for (User user : allUsers) {
                    if (user.getEmail().equalsIgnoreCase(request.getEmail())) {
                        foundUser = user;
                        break;
                    }
                }

                if (foundUser != null) {
                    userOptional = Optional.of(foundUser);
                    System.out.println("✅ Usuário encontrado manualmente (case-insensitive)");
                } else {
                    System.out.println("❌ Usuário NÃO encontrado nem manualmente");
                    return ResponseEntity.status(404)
                            .body(createErrorResponse("Usuário não encontrado. Email: " + request.getEmail()));
                }
            }

            User user = userOptional.get();
            System.out.println("✅ ADMIN RESET: Usuário confirmado - " +
                    user.getEmail() + " (ID: " + user.getId() + ")");

            // 4. DEBUG: Verificar senha atual (apenas para log)
            System.out.println("🔐 Hash atual no banco: " + user.getPassword());
            System.out.println("🔐 Hash length: " + user.getPassword().length());

            // 5. Atualizar senha com hash atual do BCrypt
            String novaSenhaHash = passwordEncoder.encode(request.getNewPassword());
            System.out.println("🔐 Novo hash gerado: " + novaSenhaHash);
            System.out.println("🔐 Novo hash length: " + novaSenhaHash.length());

            user.setPassword(novaSenhaHash);
            userRepository.save(user);

            // 6. Log de auditoria (CRÍTICO!)
            System.out.println("🔄 ADMIN RESET: Senha resetada para " + user.getEmail() +
                    " em " + LocalDateTime.now() +
                    " por endpoint administrativo");

            // 7. Verificar se a nova senha funciona
            System.out.println("🧪 Verificando nova senha com passwordEncoder.matches()...");
            boolean senhaFunciona = passwordEncoder.matches(request.getNewPassword(), novaSenhaHash);
            System.out.println("🧪 Senha funciona após reset? " + senhaFunciona);

            // 8. Retornar sucesso (sem token JWT por segurança)
            Map<String, String> response = new HashMap<>();
            response.put("message", "Senha resetada com sucesso para: " + user.getEmail());
            response.put("timestamp", LocalDateTime.now().toString());
            response.put("userId", user.getId().toString());
            response.put("warning", "Este endpoint é temporário e deve ser removido em produção");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.out.println("❌ ADMIN RESET ERRO: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(createErrorResponse("Erro ao resetar senha: " + e.getMessage()));
        }
    }

    /**
     * Endpoint temporário para debug - listar todos usuários (APENAS DEV)
     */
    @GetMapping("/admin/debug-users")
    public ResponseEntity<?> debugUsers(@RequestHeader("X-Admin-Token") String adminToken) {
        try {
            // Verificar token
            if (!adminResetToken.equals(adminToken)) {
                return ResponseEntity.status(401).body("Token inválido");
            }

            List<User> users = userRepository.findAll();
            List<Map<String, Object>> userList = new ArrayList<>();

            for (User user : users) {
                Map<String, Object> userData = new HashMap<>();
                userData.put("id", user.getId());
                userData.put("email", user.getEmail());
                userData.put("nome", user.getNome());
                userData.put("ativo", user.isAtivo());
                userData.put("dataCriacao", user.getDataCriacao());
                userList.add(userData);
            }

            return ResponseEntity.ok(userList);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erro: " + e.getMessage());
        }
    }

    // Métodos auxiliares
    private Map<String, Object> createUserResponse(User user) {
        Map<String, Object> userResponse = new HashMap<>();
        userResponse.put("id", user.getId());
        userResponse.put("email", user.getEmail());
        userResponse.put("nome", user.getNome());
        return userResponse;
    }

    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", message);
        return errorResponse;
    }

    // ✅ CLASSES INTERNAS CORRETAS
    public static class LoginRequest {
        private String email;
        private String password;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class RegisterRequest {
        private String email;
        private String password;
        private String nome;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getNome() { return nome; }
        public void setNome(String nome) { this.nome = nome; }
    }

    public static class AdminResetRequest {
        private String email;
        private String newPassword;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }
}