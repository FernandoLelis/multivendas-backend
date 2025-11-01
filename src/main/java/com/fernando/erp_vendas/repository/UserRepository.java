package com.fernando.erp_vendas.repository;

import com.fernando.erp_vendas.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Buscar usuário por email
    Optional<User> findByEmail(String email);

    // Verificar se email já existe
    boolean existsByEmail(String email);
}