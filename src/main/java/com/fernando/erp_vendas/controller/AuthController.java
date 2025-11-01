package com.fernando.erp_vendas.controller;

import com.fernando.erp_vendas.model.User;
import com.fernando.erp_vendas.repository.UserRepository;
import com.fernando.erp_vendas.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:4200")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

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
}

// ✅ MOVER AS CLASSES PARA FORA DA CLASSE PRINCIPAL
class LoginRequest {
    private String email;
    private String password;

    // Getters e Setters
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}

class RegisterRequest {
    private String email;
    private String password;
    private String nome;

    // Getters e Setters
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
}