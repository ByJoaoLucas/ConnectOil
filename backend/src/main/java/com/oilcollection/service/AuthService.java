package com.oilcollection.service;

import com.oilcollection.dto.AuthRequestDTO;
import com.oilcollection.dto.AuthResponseDTO;
import com.oilcollection.dto.UserDTO;
import com.oilcollection.exception.BusinessException;
import com.oilcollection.exception.ResourceNotFoundException;
import com.oilcollection.model.User;
import com.oilcollection.repository.UserRepository;
import com.oilcollection.security.JwtTokenProvider;
import com.oilcollection.model.AuditLog;
import com.oilcollection.service.AuditLogService;
import com.oilcollection.service.SystemConfigService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final SystemConfigService systemConfigService;
    private final AuditLogService auditLogService;

    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d");
    private static final Pattern SPECIAL_PATTERN = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]");

    private static final Set<User.UserRole> PUBLIC_REGISTRATION_ROLES = Set.of(
            User.UserRole.ESTABLISHMENT,
            User.UserRole.COLLECTOR
    );

    @Transactional
    public AuthResponseDTO login(AuthRequestDTO request, HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        log.debug("Tentativa de login para email: {} de IP: {}", request.getEmail(), ipAddress);

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("Tentativa de login com email inexistente: {} de IP: {}", request.getEmail(), ipAddress);
                    return new BadCredentialsException("Credenciais inválidas");
                });

        // Check account lockout
        if (user.isLocked()) {
            log.warn("Tentativa de login em conta bloqueada: {} (bloqueada até: {})", user.getEmail(), user.getLockedUntil());
            auditLogService.log("USER_LOGIN", user.getId(), AuditLog.Action.LOGIN_FAILED.name(), user,
                    null, "{\"reason\":\"account_locked\",\"lockedUntil\":\"" + user.getLockedUntil() + "\"}", ipAddress, userAgent);
            throw new BusinessException("Conta temporariamente bloqueada. Tente novamente mais tarde.", "ACCOUNT_LOCKED");
        }

        if (!user.getIsActive()) {
            log.warn("Tentativa de login em conta inativa: {}", user.getEmail());
            auditLogService.log("USER_LOGIN", user.getId(), AuditLog.Action.LOGIN_FAILED.name(), user,
                    null, "{\"reason\":\"account_inactive\"}", ipAddress, userAgent);
            throw new BusinessException("Conta desativada", "ACCOUNT_INACTIVE");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            // Reset failed login attempts on successful login
            user.resetFailedLoginAttempts();
            user.setLastLogin(java.time.LocalDateTime.now());
            userRepository.save(user);

            String accessToken = jwtTokenProvider.generateToken(authentication);
            String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());

            // Log successful login
            auditLogService.log("USER_LOGIN", user.getId(), AuditLog.Action.LOGIN.name(), user,
                    null, "{\"ip\":\"" + ipAddress + "\"}", ipAddress, userAgent);

            log.info("Login realizado com sucesso para usuário: {} (ID: {})", user.getEmail(), user.getId());
            return new AuthResponseDTO(
                    accessToken,
                    refreshToken,
                    "Bearer",
                    jwtTokenProvider.getJwtProperties().getExpirationMs(),
                    AuthResponseDTO.UserInfoDTO.fromUser(user)
            );
        } catch (BadCredentialsException e) {
            // Increment failed login attempts
            int maxAttempts = systemConfigService.getAccountLockoutMaxAttempts();
            int lockoutMinutes = systemConfigService.getAccountLockoutDurationMinutes();
            user.incrementFailedLoginAttempts(maxAttempts, lockoutMinutes);
            userRepository.save(user);

            log.warn("Credenciais inválidas para usuário: {} (tentativa {} de {})", user.getEmail(), user.getFailedLoginAttempts(), maxAttempts);

            // Log failed login
            auditLogService.log("USER_LOGIN", user.getId(), AuditLog.Action.LOGIN_FAILED.name(), user,
                    null, "{\"reason\":\"bad_credentials\",\"failedAttempts\":" + user.getFailedLoginAttempts() + "}", ipAddress, userAgent);

            throw new BusinessException("Credenciais inválidas", "INVALID_CREDENTIALS");
        }
    }

    @Transactional
    public AuthResponseDTO register(UserDTO request, HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        log.debug("Tentativa de registro para email: {} com role: {} de IP: {}", request.getEmail(), request.getRole(), ipAddress);

        User.UserRole role = request.getRole();

        // Validate role: only ESTABLISHMENT and COLLECTOR are allowed for public registration
        if (role == null || !PUBLIC_REGISTRATION_ROLES.contains(role)) {
            log.warn("Tentativa de registro com role inválida: {}", request.getRole());
            throw new BusinessException("Role inválida para registro público", "INVALID_ROLE");
        }

        // Validate role-specific document and field requirements
        if (role == User.UserRole.ESTABLISHMENT) {
            if (request.getCnpj() == null || request.getCnpj().isBlank()) {
                log.warn("Tentativa de registro de estabelecimento sem CNPJ: {}", request.getEmail());
                throw new BusinessException("CNPJ é obrigatório para registro de estabelecimento", "CNPJ_REQUIRED");
            }
            // Validate CNPJ format (12 alphanumeric + 2 digits = 14 chars)
            if (!request.getCnpj().matches("^[A-Z0-9]{12}[0-9]{2}$")) {
                log.warn("CNPJ inválido no registro: {}", request.getCnpj());
                throw new BusinessException("CNPJ deve conter 12 caracteres alfanuméricos seguidos de 2 dígitos", "INVALID_CNPJ");
            }
            // Require establishment name
            if (request.getEstablishmentName() == null || request.getEstablishmentName().isBlank()) {
                log.warn("Tentativa de registro de estabelecimento sem nome: {}", request.getEmail());
                throw new BusinessException("Nome do estabelecimento é obrigatório", "ESTABLISHMENT_NAME_REQUIRED");
            }
            // Require establishment address
            if (request.getEstablishmentAddress() == null || request.getEstablishmentAddress().isBlank()) {
                log.warn("Tentativa de registro de estabelecimento sem endereço: {}", request.getEmail());
                throw new BusinessException("Endereço do estabelecimento é obrigatório", "ESTABLISHMENT_ADDRESS_REQUIRED");
            }
        } else if (role == User.UserRole.COLLECTOR) {
            if (request.getCpf() == null || request.getCpf().isBlank()) {
                log.warn("Tentativa de registro de coletor sem CPF: {}", request.getEmail());
                throw new BusinessException("CPF é obrigatório para registro de coletor", "CPF_REQUIRED");
            }
            // Validate CPF format (basic check - 11 digits)
            if (!request.getCpf().matches("^\\d{11}$")) {
                log.warn("CPF inválido no registro: {}", request.getCpf());
                throw new BusinessException("CPF deve conter exatamente 11 dígitos", "INVALID_CPF");
            }
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Tentativa de registro com email já existente: {}", request.getEmail());
            throw new BusinessException("Email já cadastrado", "EMAIL_ALREADY_EXISTS");
        }

        // Check CPF duplicate if provided
        if (request.getCpf() != null && !request.getCpf().isBlank() && userRepository.existsByCpf(request.getCpf())) {
            log.warn("Tentativa de registro com CPF já existente: {}", request.getCpf());
            throw new BusinessException("CPF já cadastrado", "CPF_ALREADY_EXISTS");
        }

        // Check CNPJ duplicate if provided
        if (request.getCnpj() != null && !request.getCnpj().isBlank() && userRepository.existsByCnpj(request.getCnpj())) {
            log.warn("Tentativa de registro com CNPJ já existente: {}", request.getCnpj());
            throw new BusinessException("CNPJ já cadastrado", "CNPJ_ALREADY_EXISTS");
        }

        // Validate password complexity
        validatePasswordComplexity(request.getPassword());

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
                .role(role)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .passwordChangedAt(java.time.LocalDateTime.now())
                .build();

        User savedUser = userRepository.save(user);
        log.info("Novo usuário registrado: {} (ID: {}, Role: {})", savedUser.getEmail(), savedUser.getId(), savedUser.getRole());

        // Log registration
        auditLogService.logCreate("USER", savedUser.getId(), savedUser,
                "{\"email\":\"" + savedUser.getEmail() + "\",\"role\":\"" + savedUser.getRole() + "\"}", ipAddress, userAgent);

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                savedUser, null, savedUser.getAuthorities()
        );

        String accessToken = jwtTokenProvider.generateToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(savedUser.getEmail());

        return new AuthResponseDTO(
                accessToken,
                refreshToken,
                "Bearer",
                jwtTokenProvider.getJwtProperties().getExpirationMs(),
                AuthResponseDTO.UserInfoDTO.fromUser(savedUser)
        );
    }

    @Transactional(readOnly = true)
    public AuthResponseDTO.UserInfoDTO getCurrentUser(String email) {
        log.debug("Buscando usuário atual para email: {}", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Usuário não encontrado para email: {}", email);
                    return new ResourceNotFoundException("User", "email", email);
                });
        return AuthResponseDTO.UserInfoDTO.fromUser(user);
    }

    @Transactional
    public void deleteOwnAccount(User authenticatedUser, HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        log.debug("Solicitação de exclusão de conta própria por usuário: {} (ID: {})", authenticatedUser.getEmail(), authenticatedUser.getId());

        User user = userRepository.findById(authenticatedUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", authenticatedUser.getId()));

        if (user.getRole() != User.UserRole.COLLECTOR && user.getRole() != User.UserRole.ESTABLISHMENT) {
            log.warn("Tentativa de exclusão de conta por role não permitida: {} (role: {})", user.getEmail(), user.getRole());
            throw new BusinessException("Apenas contas de coletor e estabelecimento podem ser excluídas aqui", "ACCOUNT_DELETION_NOT_ALLOWED");
        }
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            log.warn("Tentativa de exclusão de conta já desativada: {}", user.getEmail());
            throw new BusinessException("Conta já está desativada", "ACCOUNT_ALREADY_DELETED");
        }

        // Keep historical collection records and their foreign keys intact while revoking access.
        user.setIsActive(false);
        userRepository.save(user);
        auditLogService.logDelete("USER", user.getId(), user,
                "{\"email\":\"" + user.getEmail() + "\",\"role\":\"" + user.getRole() + "\"}",
                ipAddress, userAgent);
        log.info("Conta desativada pelo próprio usuário: {} (ID: {})", user.getEmail(), user.getId());
    }

    @Transactional
    public AuthResponseDTO refreshToken(String refreshToken, HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        log.debug("Tentativa de renovação de token de IP: {}", ipAddress);

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            log.warn("Tentativa de renovação com refresh token inválido de IP: {}", ipAddress);
            throw new BusinessException("Refresh token inválido", "INVALID_REFRESH_TOKEN");
        }

        String email = jwtTokenProvider.getUsernameFromToken(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Usuário não encontrado para email no refresh token: {}", email);
                    return new ResourceNotFoundException("User", "email", email);
                });

        if (!user.getIsActive() || user.isLocked()) {
            log.warn("Tentativa de renovação de token para conta inativa ou bloqueada: {}", user.getEmail());
            throw new BusinessException("Conta não está ativa ou está bloqueada", "ACCOUNT_LOCKED");
        }

        // Log token refresh
        auditLogService.log("TOKEN_REFRESH", user.getId(), AuditLog.Action.TOKEN_REFRESH.name(), user,
                null, "{\"ip\":\"" + ipAddress + "\"}", ipAddress, userAgent);

        log.debug("Token renovado com sucesso para usuário: {}", user.getEmail());

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user, null, user.getAuthorities()
        );

        String newAccessToken = jwtTokenProvider.generateToken(authentication);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(email);

        return new AuthResponseDTO(
                newAccessToken,
                newRefreshToken,
                "Bearer",
                jwtTokenProvider.getJwtProperties().getExpirationMs(),
                AuthResponseDTO.UserInfoDTO.fromUser(user)
        );
    }

    @Transactional
    public void changePassword(UUID userId, String currentPassword, String newPassword, HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        log.debug("Tentativa de alteração de senha para usuário ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            log.warn("Senha atual incorreta para usuário: {}", user.getEmail());
            auditLogService.log("PASSWORD_CHANGE", user.getId(), AuditLog.Action.PASSWORD_CHANGE_FAILED.name(), user,
                    null, "{\"reason\":\"invalid_current_password\"}", ipAddress, userAgent);
            throw new BusinessException("Senha atual incorreta", "INVALID_CURRENT_PASSWORD");
        }

        // Validate new password complexity
        validatePasswordComplexity(newPassword);

        // Check password history (optional - not implemented)
        // if (passwordEncoder.matches(newPassword, user.getPassword())) { ... }

        String oldValues = "{\"passwordChangedAt\":\"" + user.getPasswordChangedAt() + "\"}";

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(java.time.LocalDateTime.now());
        userRepository.save(user);

        // Log password change
        auditLogService.logUpdate("USER", user.getId(), user,
                oldValues, "{\"passwordChangedAt\":\"" + user.getPasswordChangedAt() + "\"}", ipAddress, userAgent);

        log.info("Senha alterada para usuário: {} (ID: {})", user.getEmail(), user.getId());
    }

    private void validatePasswordComplexity(String password) {
        int minLength = systemConfigService.getPasswordMinLength();
        boolean requireUppercase = systemConfigService.getPasswordRequireUppercase();
        boolean requireLowercase = systemConfigService.getPasswordRequireLowercase();
        boolean requireNumber = systemConfigService.getPasswordRequireNumber();
        boolean requireSpecial = systemConfigService.getPasswordRequireSpecial();

        log.debug("Validando complexidade da senha (minLength: {}, requireUppercase: {}, requireLowercase: {}, requireNumber: {}, requireSpecial: {})",
                minLength, requireUppercase, requireLowercase, requireNumber, requireSpecial);

        if (password.length() < minLength) {
            log.warn("Senha muito curta: {} caracteres (mínimo: {})", password.length(), minLength);
            throw new BusinessException("A senha deve ter pelo menos " + minLength + " caracteres", "WEAK_PASSWORD");
        }

        if (requireUppercase && !UPPERCASE_PATTERN.matcher(password).find()) {
            log.warn("Senha não contém letra maiúscula");
            throw new BusinessException("A senha deve conter pelo menos uma letra maiúscula", "WEAK_PASSWORD");
        }

        if (requireLowercase && !LOWERCASE_PATTERN.matcher(password).find()) {
            log.warn("Senha não contém letra minúscula");
            throw new BusinessException("A senha deve conter pelo menos uma letra minúscula", "WEAK_PASSWORD");
        }

        if (requireNumber && !NUMBER_PATTERN.matcher(password).find()) {
            log.warn("Senha não contém número");
            throw new BusinessException("A senha deve conter pelo menos um número", "WEAK_PASSWORD");
        }

        if (requireSpecial && !SPECIAL_PATTERN.matcher(password).find()) {
            log.warn("Senha não contém caractere especial");
            throw new BusinessException("A senha deve conter pelo menos um caractere especial", "WEAK_PASSWORD");
        }

        log.debug("Validação de complexidade da senha aprovada");
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