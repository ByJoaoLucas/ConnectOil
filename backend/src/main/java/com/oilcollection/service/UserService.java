package com.oilcollection.service;

import com.oilcollection.dto.UserDTO;
import com.oilcollection.exception.BusinessException;
import com.oilcollection.exception.ResourceNotFoundException;
import com.oilcollection.model.User;
import com.oilcollection.repository.UserRepository;
import com.oilcollection.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    private static final String LOG_PREFIX = "[UserService]";

    @Transactional(readOnly = true)
    public Page<UserDTO> getAllUsers(Pageable pageable) {
        log.debug("{} Buscando todos os usuários com paginação: {}", LOG_PREFIX, pageable);
        return userRepository.findAll(pageable).map(UserDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<UserDTO> getAllUsers() {
        log.debug("{} Buscando todos os usuários como lista", LOG_PREFIX);
        return userRepository.findAll().stream().map(UserDTO::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public UserDTO getUserById(UUID id) {
        log.debug("{} Buscando usuário por ID: {}", LOG_PREFIX, id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("{} Usuário não encontrado: {}", LOG_PREFIX, id);
                    return new ResourceNotFoundException("User", "id", id);
                });
        return UserDTO.fromEntity(user);
    }

    @Transactional(readOnly = true)
    public Page<UserDTO> getUsersByRole(User.UserRole role, Pageable pageable) {
        log.debug("{} Buscando usuários por role: {} com paginação: {}", LOG_PREFIX, role, pageable);
        return userRepository.findByRole(role, pageable).map(UserDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<UserDTO> getActiveCollectors() {
        log.debug("{} Buscando coletores ativos", LOG_PREFIX);
        return userRepository.findByIsActiveTrueAndRole(User.UserRole.COLLECTOR)
                .stream().map(UserDTO::fromEntity).toList();
    }

    @Transactional
    public UserDTO createUser(UserDTO request, User createdBy, String ipAddress, String userAgent) {
        log.info("{} Criando usuário: {} (role: {}) por usuário: {}", LOG_PREFIX, request.getEmail(), request.getRole(), createdBy.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("{} Tentativa de criar usuário com email já existente: {}", LOG_PREFIX, request.getEmail());
            throw new BusinessException("Email já cadastrado", "EMAIL_ALREADY_EXISTS");
        }

        if (request.getCpf() != null && !request.getCpf().isBlank()
                && userRepository.existsByCpf(request.getCpf())) {
            log.warn("{} Tentativa de criar usuário com CPF já existente: {}", LOG_PREFIX, request.getCpf());
            throw new BusinessException("CPF já cadastrado", "CPF_ALREADY_EXISTS");
        }

        if (request.getCnpj() != null && !request.getCnpj().isBlank()
                && userRepository.existsByCnpj(request.getCnpj())) {
            log.warn("{} Tentativa de criar usuário com CNPJ já existente: {}", LOG_PREFIX, request.getCnpj());
            throw new BusinessException("CNPJ já cadastrado", "CNPJ_ALREADY_EXISTS");
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .cpf(request.getCpf())
                .cnpj(request.getCnpj())
                .phoneNumber(request.getPhoneNumber())
                .establishmentName(request.getEstablishmentName())
                .establishmentAddress(request.getEstablishmentAddress())
                .establishmentCity(request.getEstablishmentCity())
                .establishmentState(request.getEstablishmentState())
                .establishmentZipCode(request.getEstablishmentZipCode())
                .role(request.getRole() != null ? request.getRole() : User.UserRole.COLLECTOR)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .passwordChangedAt(java.time.LocalDateTime.now())
                .build();

        User savedUser = userRepository.save(user);

        // Log audit
        auditLogService.logCreate("USER", savedUser.getId(), createdBy,
                "{\"email\":\"" + savedUser.getEmail() + "\",\"role\":\"" + savedUser.getRole() + "\"}", ipAddress, userAgent);

        log.info("{} Usuário criado: ID={}, Email={}, Nome={}, Role={}, Ativo={}, por={}", LOG_PREFIX, savedUser.getId(), savedUser.getEmail(), savedUser.getName(), savedUser.getRole(), savedUser.getIsActive(), createdBy.getEmail());
        return UserDTO.fromEntity(savedUser);
    }

    @Transactional
    public UserDTO updateUser(UUID id, UserDTO request, User updatedBy, String ipAddress, String userAgent) {
        log.info("{} Atualizando usuário ID: {} por usuário: {}", LOG_PREFIX, id, updatedBy.getEmail());

        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("{} Usuário não encontrado para atualização: {}", LOG_PREFIX, id);
                    return new ResourceNotFoundException("User", "id", id);
                });

        String oldValues = toJson(user);

        if (!user.getEmail().equals(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            log.warn("{} Tentativa de atualizar usuário para email já existente: {}", LOG_PREFIX, request.getEmail());
            throw new BusinessException("Email já cadastrado", "EMAIL_ALREADY_EXISTS");
        }

        if (request.getCpf() != null && !request.getCpf().isBlank()
                && !request.getCpf().equals(user.getCpf())
                && userRepository.existsByCpf(request.getCpf())) {
            log.warn("{} Tentativa de atualizar usuário para CPF já existente: {}", LOG_PREFIX, request.getCpf());
            throw new BusinessException("CPF já cadastrado", "CPF_ALREADY_EXISTS");
        }

        if (request.getCnpj() != null && !request.getCnpj().isBlank()
                && !request.getCnpj().equals(user.getCnpj())
                && userRepository.existsByCnpj(request.getCnpj())) {
            log.warn("{} Tentativa de atualizar usuário para CNPJ já existente: {}", LOG_PREFIX, request.getCnpj());
            throw new BusinessException("CNPJ já cadastrado", "CNPJ_ALREADY_EXISTS");
        }

        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setCpf(request.getCpf());
        user.setCnpj(request.getCnpj());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setEstablishmentName(request.getEstablishmentName());
        user.setEstablishmentAddress(request.getEstablishmentAddress());
        user.setEstablishmentCity(request.getEstablishmentCity());
        user.setEstablishmentState(request.getEstablishmentState());
        user.setEstablishmentZipCode(request.getEstablishmentZipCode());
        user.setRole(request.getRole());
        user.setIsActive(request.getIsActive());

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setPasswordChangedAt(java.time.LocalDateTime.now());
            log.debug("{} Senha do usuário atualizada", LOG_PREFIX);
        }

        User updatedUser = userRepository.save(user);

        // Log audit
        auditLogService.logUpdate("USER", updatedUser.getId(), updatedBy,
                oldValues, toJson(updatedUser), ipAddress, userAgent);

        log.info("{} Usuário atualizado: ID={}, Email={}, Nome={}, Role={}, Ativo={}, por={}", LOG_PREFIX, updatedUser.getId(), updatedUser.getEmail(), updatedUser.getName(), updatedUser.getRole(), updatedUser.getIsActive(), updatedBy.getEmail());
        return UserDTO.fromEntity(updatedUser);
    }

    @Transactional
    public void deleteUser(UUID id, User deletedBy, String ipAddress, String userAgent) {
        log.info("{} Desativando usuário ID: {} por usuário: {}", LOG_PREFIX, id, deletedBy.getEmail());

        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("{} Usuário não encontrado para desativação: {}", LOG_PREFIX, id);
                    return new ResourceNotFoundException("User", "id", id);
                });

        String oldValues = toJson(user);

        user.setIsActive(false);
        userRepository.save(user);

        // Log audit
        auditLogService.logUpdate("USER", user.getId(), deletedBy,
                oldValues, "{\"isActive\":false}", ipAddress, userAgent);

        log.info("{} Usuário desativado: ID={}, Email={}, por={}", LOG_PREFIX, id, user.getEmail(), deletedBy.getEmail());
    }

    @Transactional
    public UserDTO activateUser(UUID id, User activatedBy, String ipAddress, String userAgent) {
        log.info("{} Ativando usuário ID: {} por usuário: {}", LOG_PREFIX, id, activatedBy.getEmail());

        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("{} Usuário não encontrado para ativação: {}", LOG_PREFIX, id);
                    return new ResourceNotFoundException("User", "id", id);
                });

        String oldValues = toJson(user);

        user.setIsActive(true);
        User activatedUser = userRepository.save(user);

        // Log audit
        auditLogService.logUpdate("USER", activatedUser.getId(), activatedBy,
                oldValues, "{\"isActive\":true}", ipAddress, userAgent);

        log.info("{} Usuário ativado: ID={}, Email={}, por={}", LOG_PREFIX, activatedUser.getId(), activatedUser.getEmail(), activatedBy.getEmail());
        return UserDTO.fromEntity(activatedUser);
    }

    @Transactional
    public UserDTO assignCollectorRole(UUID id, User assignedBy, String ipAddress, String userAgent) {
        log.info("{} Atribuindo papel de COLETOR ao usuário ID: {} por usuário: {}", LOG_PREFIX, id, assignedBy.getEmail());

        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("{} Usuário não encontrado para atribuição de papel: {}", LOG_PREFIX, id);
                    return new ResourceNotFoundException("User", "id", id);
                });

        String oldValues = toJson(user);

        user.setRole(User.UserRole.COLLECTOR);
        User updatedUser = userRepository.save(user);

        // Log audit
        auditLogService.logUpdate("USER", updatedUser.getId(), assignedBy,
                oldValues, toJson(updatedUser), ipAddress, userAgent);

        log.info("{} Papel de COLETOR atribuído ao usuário: ID={}, Email={}, por={}", LOG_PREFIX, updatedUser.getId(), updatedUser.getEmail(), assignedBy.getEmail());
        return UserDTO.fromEntity(updatedUser);
    }

    private String toJson(User user) {
        return String.format(
                "{\"id\":\"%s\",\"email\":\"%s\",\"name\":\"%s\",\"role\":\"%s\",\"isActive\":%b}",
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole(),
                user.getIsActive()
        );
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isEmpty()) {
            return xfHeader.split(",")[0].trim();
        }
        String xrHeader = request.getHeader("X-Real-IP");
        if (xrHeader != null && !xrHeader.isEmpty()) {
            return xrHeader;
        }
        return request.getRemoteAddr();
    }
}