package com.oilcollection.repository;

import com.oilcollection.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByCpf(String cpf);

    boolean existsByEmail(String email);

    boolean existsByCpf(String cpf);

    boolean existsByCnpj(String cnpj);

    List<User> findByRole(User.UserRole role);

    Page<User> findByRole(User.UserRole role, Pageable pageable);

    List<User> findByIsActiveTrueAndRole(User.UserRole role);

    @Query("SELECT u FROM User u WHERE u.isActive = true AND u.role IN :roles")
    List<User> findActiveUsersByRoles(@Param("roles") List<User.UserRole> roles);

    @Query("SELECT u FROM User u WHERE u.lastLogin < :date AND u.isActive = true")
    List<User> findInactiveUsersSince(@Param("date") java.time.LocalDateTime date);

    List<User> findByCollectionPointsIsNotNull();

    // Dashboard queries
    long countByRole(User.UserRole role);

    long countByRoleAndIsActive(User.UserRole role, boolean isActive);
}