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

    // TODO: remover ADMIN do cadastro público antes da entrega final
    private static final Set<User.UserRole> PUBLIC_REGISTRATION_ROLES = Set.of(
            User.UserRole.ESTABLISHMENT,
            User.UserRole.COLLECTOR,
            User.UserRole.ADMIN
    );

    @Transactional
    public AuthResponseDTO login(AuthRequestDTO request, HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        // Check account lockout
        if (user.isLocked()) {
            auditLogService.log("USER_LOGIN", user.getId(), AuditLog.Action.LOGIN_FAILED.name(), user,
                    null, "{\"reason\":\"account_locked\",\"lockedUntil\":\"" + user.getLockedUntil() + "\"}", ipAddress, userAgent);
            throw new BusinessException("Account temporarily locked. Try again later.", "ACCOUNT_LOCKED");
        }

        if (!user.getIsActive()) {
            auditLogService.log("USER_LOGIN", user.getId(), AuditLog.Action.LOGIN_FAILED.name(), user,
                    null, "{\"reason\":\"account_inactive\"}", ipAddress, userAgent);
            throw new BusinessException("Account is deactivated", "ACCOUNT_INACTIVE");
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

            // Log failed login
            auditLogService.log("USER_LOGIN", user.getId(), AuditLog.Action.LOGIN_FAILED.name(), user,
                    null, "{\"reason\":\"bad_credentials\",\"failedAttempts\":" + user.getFailedLoginAttempts() + "}", ipAddress, userAgent);

            throw new BusinessException("Invalid credentials", "INVALID_CREDENTIALS");
        }
    }

    @Transactional
    public AuthResponseDTO register(UserDTO request, HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        User.UserRole role = request.getRole();

        // Validate role: only ESTABLISHMENT, COLLECTOR, ADMIN allowed for public registration
        if (role == null || !PUBLIC_REGISTRATION_ROLES.contains(role)) {
            throw new BusinessException("Invalid role for public registration", "INVALID_ROLE");
        }

        // Validate role-specific document and field requirements
        if (role == User.UserRole.ESTABLISHMENT) {
            if (request.getCnpj() == null || request.getCnpj().isBlank()) {
                throw new BusinessException("CNPJ is required for establishment registration", "CNPJ_REQUIRED");
            }
            // Validate CNPJ format (basic check - 14 digits)
            if (!request.getCnpj().matches("^\\d{14}$")) {
                throw new BusinessException("CNPJ must contain exactly 14 digits", "INVALID_CNPJ");
            }
            // Require establishment name
            if (request.getEstablishmentName() == null || request.getEstablishmentName().isBlank()) {
                throw new BusinessException("Establishment name is required", "ESTABLISHMENT_NAME_REQUIRED");
            }
            // Require establishment address
            if (request.getEstablishmentAddress() == null || request.getEstablishmentAddress().isBlank()) {
                throw new BusinessException("Establishment address is required", "ESTABLISHMENT_ADDRESS_REQUIRED");
            }
        } else if (role == User.UserRole.COLLECTOR) {
            if (request.getCpf() == null || request.getCpf().isBlank()) {
                throw new BusinessException("CPF is required for collector registration", "CPF_REQUIRED");
            }
            // Validate CPF format (basic check - 11 digits)
            if (!request.getCpf().matches("^\\d{11}$")) {
                throw new BusinessException("CPF must contain exactly 11 digits", "INVALID_CPF");
            }
        }
        // ADMIN: no CPF/CNPJ requirement

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email already registered", "EMAIL_ALREADY_EXISTS");
        }

        // Check CPF duplicate if provided
        if (request.getCpf() != null && !request.getCpf().isBlank() && userRepository.existsByCpf(request.getCpf())) {
            throw new BusinessException("CPF already registered", "CPF_ALREADY_EXISTS");
        }

        // Check CNPJ duplicate if provided
        if (request.getCnpj() != null && !request.getCnpj().isBlank() && userRepository.existsByCnpj(request.getCnpj())) {
            throw new BusinessException("CNPJ already registered", "CNPJ_ALREADY_EXISTS");
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
        log.info("New user registered: {}", savedUser.getEmail());

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
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        return AuthResponseDTO.UserInfoDTO.fromUser(user);
    }

    @Transactional
    public void deleteOwnAccount(User authenticatedUser, HttpServletRequest httpRequest) {
        User user = userRepository.findById(authenticatedUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", authenticatedUser.getId()));

        if (user.getRole() != User.UserRole.COLLECTOR && user.getRole() != User.UserRole.ESTABLISHMENT) {
            throw new BusinessException("Only collector and establishment accounts can be deleted here", "ACCOUNT_DELETION_NOT_ALLOWED");
        }
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new BusinessException("Account is already deactivated", "ACCOUNT_ALREADY_DELETED");
        }

        // Keep historical collection records and their foreign keys intact while revoking access.
        user.setIsActive(false);
        userRepository.save(user);
        auditLogService.logDelete("USER", user.getId(), user,
                "{\"email\":\"" + user.getEmail() + "\",\"role\":\"" + user.getRole() + "\"}",
                getClientIp(httpRequest), httpRequest.getHeader("User-Agent"));
        log.info("User account deactivated by owner: {}", user.getEmail());
    }

    @Transactional
    public AuthResponseDTO refreshToken(String refreshToken, HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException("Invalid refresh token", "INVALID_REFRESH_TOKEN");
        }

        String email = jwtTokenProvider.getUsernameFromToken(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        if (!user.getIsActive() || user.isLocked()) {
            throw new BusinessException("Account is not active or locked", "ACCOUNT_LOCKED");
        }

        // Log token refresh
        auditLogService.log("TOKEN_REFRESH", user.getId(), AuditLog.Action.TOKEN_REFRESH.name(), user,
                null, "{\"ip\":\"" + ipAddress + "\"}", ipAddress, userAgent);

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

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            auditLogService.log("PASSWORD_CHANGE", user.getId(), AuditLog.Action.PASSWORD_CHANGE_FAILED.name(), user,
                    null, "{\"reason\":\"invalid_current_password\"}", ipAddress, userAgent);
            throw new BusinessException("Current password is incorrect", "INVALID_CURRENT_PASSWORD");
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

        log.info("Password changed for user: {}", user.getEmail());
    }

    private void validatePasswordComplexity(String password) {
        int minLength = systemConfigService.getPasswordMinLength();
        boolean requireUppercase = systemConfigService.getPasswordRequireUppercase();
        boolean requireLowercase = systemConfigService.getPasswordRequireLowercase();
        boolean requireNumber = systemConfigService.getPasswordRequireNumber();
        boolean requireSpecial = systemConfigService.getPasswordRequireSpecial();

        if (password.length() < minLength) {
            throw new BusinessException("Password must be at least " + minLength + " characters", "WEAK_PASSWORD");
        }

        if (requireUppercase && !UPPERCASE_PATTERN.matcher(password).find()) {
            throw new BusinessException("Password must contain at least one uppercase letter", "WEAK_PASSWORD");
        }

        if (requireLowercase && !LOWERCASE_PATTERN.matcher(password).find()) {
            throw new BusinessException("Password must contain at least one lowercase letter", "WEAK_PASSWORD");
        }

        if (requireNumber && !NUMBER_PATTERN.matcher(password).find()) {
            throw new BusinessException("Password must contain at least one number", "WEAK_PASSWORD");
        }

        if (requireSpecial && !SPECIAL_PATTERN.matcher(password).find()) {
            throw new BusinessException("Password must contain at least one special character", "WEAK_PASSWORD");
        }
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