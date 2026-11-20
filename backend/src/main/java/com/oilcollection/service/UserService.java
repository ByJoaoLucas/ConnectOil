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

    @Transactional(readOnly = true)
    public Page<UserDTO> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(UserDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream().map(UserDTO::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public UserDTO getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return UserDTO.fromEntity(user);
    }

    @Transactional(readOnly = true)
    public Page<UserDTO> getUsersByRole(User.UserRole role, Pageable pageable) {
        return userRepository.findByRole(role, pageable).map(UserDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<UserDTO> getActiveCollectors() {
        return userRepository.findByIsActiveTrueAndRole(User.UserRole.COLLECTOR)
                .stream().map(UserDTO::fromEntity).toList();
    }

    @Transactional
    public UserDTO createUser(UserDTO request, User createdBy, String ipAddress, String userAgent) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email already registered", "EMAIL_ALREADY_EXISTS");
        }

        if (request.getCpf() != null && !request.getCpf().isBlank()
                && userRepository.existsByCpf(request.getCpf())) {
            throw new BusinessException("CPF already registered", "CPF_ALREADY_EXISTS");
        }

        if (request.getCnpj() != null && !request.getCnpj().isBlank()
                && userRepository.existsByCnpj(request.getCnpj())) {
            throw new BusinessException("CNPJ already registered", "CNPJ_ALREADY_EXISTS");
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

        log.info("User created: {} by {}", savedUser.getEmail(), createdBy.getEmail());
        return UserDTO.fromEntity(savedUser);
    }

    @Transactional
    public UserDTO updateUser(UUID id, UserDTO request, User updatedBy, String ipAddress, String userAgent) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        String oldValues = toJson(user);

        if (!user.getEmail().equals(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email already registered", "EMAIL_ALREADY_EXISTS");
        }

        if (request.getCpf() != null && !request.getCpf().isBlank()
                && !request.getCpf().equals(user.getCpf())
                && userRepository.existsByCpf(request.getCpf())) {
            throw new BusinessException("CPF already registered", "CPF_ALREADY_EXISTS");
        }

        if (request.getCnpj() != null && !request.getCnpj().isBlank()
                && !request.getCnpj().equals(user.getCnpj())
                && userRepository.existsByCnpj(request.getCnpj())) {
            throw new BusinessException("CNPJ already registered", "CNPJ_ALREADY_EXISTS");
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
        }

        User updatedUser = userRepository.save(user);

        // Log audit
        auditLogService.logUpdate("USER", updatedUser.getId(), updatedBy,
                oldValues, toJson(updatedUser), ipAddress, userAgent);

        log.info("User updated: {} by {}", updatedUser.getEmail(), updatedBy.getEmail());
        return UserDTO.fromEntity(updatedUser);
    }

    @Transactional
    public void deleteUser(UUID id, User deletedBy, String ipAddress, String userAgent) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        String oldValues = toJson(user);

        user.setIsActive(false);
        userRepository.save(user);

        // Log audit
        auditLogService.logUpdate("USER", user.getId(), deletedBy,
                oldValues, "{\"isActive\":false}", ipAddress, userAgent);

        log.info("User deactivated: {} by {}", user.getEmail(), deletedBy.getEmail());
    }

    @Transactional
    public UserDTO activateUser(UUID id, User activatedBy, String ipAddress, String userAgent) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        String oldValues = toJson(user);

        user.setIsActive(true);
        User activatedUser = userRepository.save(user);

        // Log audit
        auditLogService.logUpdate("USER", activatedUser.getId(), activatedBy,
                oldValues, "{\"isActive\":true}", ipAddress, userAgent);

        log.info("User activated: {} by {}", activatedUser.getEmail(), activatedBy.getEmail());
        return UserDTO.fromEntity(activatedUser);
    }

    @Transactional
    public UserDTO assignCollectorRole(UUID id, User assignedBy, String ipAddress, String userAgent) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        String oldValues = toJson(user);

        user.setRole(User.UserRole.COLLECTOR);
        User updatedUser = userRepository.save(user);

        // Log audit
        auditLogService.logUpdate("USER", updatedUser.getId(), assignedBy,
                oldValues, toJson(updatedUser), ipAddress, userAgent);

        log.info("User assigned as collector: {} by {}", updatedUser.getEmail(), assignedBy.getEmail());
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