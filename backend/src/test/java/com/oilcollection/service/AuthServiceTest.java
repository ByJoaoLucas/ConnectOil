package com.oilcollection.service;

import com.oilcollection.dto.AuthRequestDTO;
import com.oilcollection.dto.AuthResponseDTO;
import com.oilcollection.dto.UserDTO;
import com.oilcollection.exception.BusinessException;
import com.oilcollection.model.User;
import com.oilcollection.repository.UserRepository;
import com.oilcollection.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private SystemConfigService systemConfigService;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private HttpServletRequest httpRequest;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private AuthRequestDTO loginRequest;
    private UserDTO collectorRegisterRequest;
    private UserDTO establishmentRegisterRequest;

    @BeforeEach
    void setUp() {
        lenient().when(httpRequest.getHeader("User-Agent")).thenReturn("test-agent");
        lenient().when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        lenient().when(systemConfigService.getPasswordMinLength()).thenReturn(8);
        lenient().when(systemConfigService.getPasswordRequireUppercase()).thenReturn(false);
        lenient().when(systemConfigService.getPasswordRequireLowercase()).thenReturn(false);
        lenient().when(systemConfigService.getPasswordRequireNumber()).thenReturn(false);
        lenient().when(systemConfigService.getPasswordRequireSpecial()).thenReturn(false);

        testUser = User.builder()
                .id(UUID.randomUUID())
                .name("Test User")
                .email("test@example.com")
                .password("encodedPassword")
                .cpf("12345678901")
                .role(User.UserRole.COLLECTOR)
                .isActive(true)
                .build();

        loginRequest = new AuthRequestDTO();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password123");

        collectorRegisterRequest = new UserDTO();
        collectorRegisterRequest.setName("New Collector");
        collectorRegisterRequest.setEmail("collector@example.com");
        collectorRegisterRequest.setPassword("password123");
        collectorRegisterRequest.setCpf("98765432109");
        collectorRegisterRequest.setRole(User.UserRole.COLLECTOR);

        establishmentRegisterRequest = new UserDTO();
        establishmentRegisterRequest.setName("New Establishment");
        establishmentRegisterRequest.setEmail("establishment@example.com");
        establishmentRegisterRequest.setPassword("password123");
        establishmentRegisterRequest.setCnpj("12345678000199");
        establishmentRegisterRequest.setRole(User.UserRole.ESTABLISHMENT);
        establishmentRegisterRequest.setEstablishmentName("Test Establishment");
        establishmentRegisterRequest.setEstablishmentAddress("Rua Teste, 123");
        establishmentRegisterRequest.setEstablishmentCity("São Paulo");
        establishmentRegisterRequest.setEstablishmentState("SP");
        establishmentRegisterRequest.setEstablishmentZipCode("01234567");
    }

    @Test
    void login_Success() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(testUser, null, testUser.getAuthorities());
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(testUser));
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtTokenProvider.generateToken(authentication)).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(testUser.getEmail())).thenReturn("refresh-token");
        when(jwtTokenProvider.getJwtProperties()).thenReturn(new com.oilcollection.security.JwtProperties());

        AuthResponseDTO response = authService.login(loginRequest, httpRequest);

        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        verify(userRepository).save(testUser);
    }

    @Test
    void register_Collector_Success() {
        when(userRepository.existsByEmail(collectorRegisterRequest.getEmail())).thenReturn(false);
        when(userRepository.existsByCpf(collectorRegisterRequest.getCpf())).thenReturn(false);
        when(passwordEncoder.encode(collectorRegisterRequest.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtTokenProvider.generateToken(any())).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(collectorRegisterRequest.getEmail())).thenReturn("refresh-token");
        when(jwtTokenProvider.getJwtProperties()).thenReturn(new com.oilcollection.security.JwtProperties());

        AuthResponseDTO response = authService.register(collectorRegisterRequest, httpRequest);

        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
        verify(userRepository).save(any());
    }

    @Test
    void register_Establishment_Success() {
        when(userRepository.existsByEmail(establishmentRegisterRequest.getEmail())).thenReturn(false);
        when(userRepository.existsByCnpj(establishmentRegisterRequest.getCnpj())).thenReturn(false);
        when(passwordEncoder.encode(establishmentRegisterRequest.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtTokenProvider.generateToken(any())).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(establishmentRegisterRequest.getEmail())).thenReturn("refresh-token");
        when(jwtTokenProvider.getJwtProperties()).thenReturn(new com.oilcollection.security.JwtProperties());

        AuthResponseDTO response = authService.register(establishmentRegisterRequest, httpRequest);

        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
        verify(userRepository).save(any());
    }

    @Test
    void register_EmailAlreadyExists_ThrowsException() {
        when(userRepository.existsByEmail(collectorRegisterRequest.getEmail())).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.register(collectorRegisterRequest, httpRequest));

        assertEquals("EMAIL_ALREADY_EXISTS", exception.getErrorCode());
    }

    @Test
    void register_CpfAlreadyExists_ThrowsException() {
        when(userRepository.existsByEmail(collectorRegisterRequest.getEmail())).thenReturn(false);
        when(userRepository.existsByCpf(collectorRegisterRequest.getCpf())).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.register(collectorRegisterRequest, httpRequest));

        assertEquals("CPF_ALREADY_EXISTS", exception.getErrorCode());
    }

    @Test
    void register_CnpjAlreadyExists_ThrowsException() {
        when(userRepository.existsByEmail(establishmentRegisterRequest.getEmail())).thenReturn(false);
        when(userRepository.existsByCnpj(establishmentRegisterRequest.getCnpj())).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.register(establishmentRegisterRequest, httpRequest));

        assertEquals("CNPJ_ALREADY_EXISTS", exception.getErrorCode());
    }

    @Test
    void register_EstablishmentWithoutCnpj_ThrowsException() {
        establishmentRegisterRequest.setCnpj(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.register(establishmentRegisterRequest, httpRequest));

        assertEquals("CNPJ_REQUIRED", exception.getErrorCode());
    }

    @Test
    void register_CollectorWithoutCpf_ThrowsException() {
        collectorRegisterRequest.setCpf(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.register(collectorRegisterRequest, httpRequest));

        assertEquals("CPF_REQUIRED", exception.getErrorCode());
    }

    @Test
    void register_EstablishmentWithInvalidCnpjFormat_ThrowsException() {
        establishmentRegisterRequest.setCnpj("123"); // invalid format

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.register(establishmentRegisterRequest, httpRequest));

        assertEquals("INVALID_CNPJ", exception.getErrorCode());
    }

    @Test
    void register_CollectorWithInvalidCpfFormat_ThrowsException() {
        collectorRegisterRequest.setCpf("123"); // invalid format

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.register(collectorRegisterRequest, httpRequest));

        assertEquals("INVALID_CPF", exception.getErrorCode());
    }

    @Test
    void refreshToken_Success() {
        String refreshToken = "valid-refresh-token";
        when(jwtTokenProvider.validateToken(refreshToken)).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromToken(refreshToken)).thenReturn(testUser.getEmail());
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(jwtTokenProvider.generateToken(any())).thenReturn("new-access-token");
        when(jwtTokenProvider.generateRefreshToken(testUser.getEmail())).thenReturn("new-refresh-token");
        when(jwtTokenProvider.getJwtProperties()).thenReturn(new com.oilcollection.security.JwtProperties());

        AuthResponseDTO response = authService.refreshToken(refreshToken, httpRequest);

        assertNotNull(response);
        assertEquals("new-access-token", response.getAccessToken());
        assertEquals("new-refresh-token", response.getRefreshToken());
    }

    @Test
    void refreshToken_InvalidToken_ThrowsException() {
        String refreshToken = "invalid-refresh-token";
        when(jwtTokenProvider.validateToken(refreshToken)).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.refreshToken(refreshToken, httpRequest));

        assertEquals("INVALID_REFRESH_TOKEN", exception.getErrorCode());
    }

    @Test
    void changePassword_Success() {
        UUID userId = testUser.getId();
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("currentPassword", testUser.getPassword())).thenReturn(true);
        when(passwordEncoder.encode("newPassword")).thenReturn("newEncodedPassword");

        authService.changePassword(userId, "currentPassword", "newPassword", httpRequest);

        verify(userRepository).save(testUser);
        assertEquals("newEncodedPassword", testUser.getPassword());
    }

    @Test
    void changePassword_InvalidCurrentPassword_ThrowsException() {
        UUID userId = testUser.getId();
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongPassword", testUser.getPassword())).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.changePassword(userId, "wrongPassword", "newPassword", httpRequest));

        assertEquals("INVALID_CURRENT_PASSWORD", exception.getErrorCode());
    }

    // ===== ROLE VALIDATION TESTS =====

    @Test
    void register_AdminRole_Accepted() {
        // TODO: remover junto com o ADMIN do cadastro público
        UserDTO adminRequest = new UserDTO();
        adminRequest.setName("Admin User");
        adminRequest.setEmail("admin@example.com");
        adminRequest.setPassword("password123");
        adminRequest.setRole(User.UserRole.ADMIN);
        // No CPF/CNPJ required for ADMIN

        when(userRepository.existsByEmail(adminRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(adminRequest.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtTokenProvider.generateToken(any())).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(adminRequest.getEmail())).thenReturn("refresh-token");
        when(jwtTokenProvider.getJwtProperties()).thenReturn(new com.oilcollection.security.JwtProperties());

        AuthResponseDTO response = authService.register(adminRequest, httpRequest);

        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
        verify(userRepository).save(any());
    }

    @Test
    void register_ClientRole_ThrowsInvalidRole() {
        UserDTO request = new UserDTO();
        request.setName("Client User");
        request.setEmail("client@example.com");
        request.setPassword("password123");
        request.setRole(User.UserRole.CLIENT);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.register(request, httpRequest));

        assertEquals("INVALID_ROLE", exception.getErrorCode());
    }

    @Test
    void register_NullRole_ThrowsInvalidRole() {
        UserDTO request = new UserDTO();
        request.setName("No Role User");
        request.setEmail("norole@example.com");
        request.setPassword("password123");
        request.setRole(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.register(request, httpRequest));

        assertEquals("INVALID_ROLE", exception.getErrorCode());
    }

    // ===== ESTABLISHMENT SPECIFIC FIELD VALIDATION TESTS =====

    @Test
    void register_EstablishmentWithoutName_ThrowsException() {
        establishmentRegisterRequest.setEstablishmentName(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.register(establishmentRegisterRequest, httpRequest));

        assertEquals("ESTABLISHMENT_NAME_REQUIRED", exception.getErrorCode());
    }

    @Test
    void register_EstablishmentWithoutAddress_ThrowsException() {
        establishmentRegisterRequest.setEstablishmentAddress(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.register(establishmentRegisterRequest, httpRequest));

        assertEquals("ESTABLISHMENT_ADDRESS_REQUIRED", exception.getErrorCode());
    }

    @Test
    void register_EstablishmentWithBlankName_ThrowsException() {
        establishmentRegisterRequest.setEstablishmentName("   ");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.register(establishmentRegisterRequest, httpRequest));

        assertEquals("ESTABLISHMENT_NAME_REQUIRED", exception.getErrorCode());
    }

    @Test
    void register_EstablishmentWithBlankAddress_ThrowsException() {
        establishmentRegisterRequest.setEstablishmentAddress("   ");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.register(establishmentRegisterRequest, httpRequest));

        assertEquals("ESTABLISHMENT_ADDRESS_REQUIRED", exception.getErrorCode());
    }

    // ===== PASSWORD COMPLEXITY TESTS =====

    @Test
    void register_WeakPassword_NoUppercase_ThrowsException() {
        lenient().when(systemConfigService.getPasswordRequireUppercase()).thenReturn(true);
        lenient().when(systemConfigService.getPasswordRequireLowercase()).thenReturn(false);
        lenient().when(systemConfigService.getPasswordRequireNumber()).thenReturn(false);
        lenient().when(systemConfigService.getPasswordRequireSpecial()).thenReturn(false);

        collectorRegisterRequest.setPassword("password123"); // no uppercase

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.register(collectorRegisterRequest, httpRequest));

        assertEquals("WEAK_PASSWORD", exception.getErrorCode());
    }

    @Test
    void register_WeakPassword_NoLowercase_ThrowsException() {
        lenient().when(systemConfigService.getPasswordRequireUppercase()).thenReturn(false);
        lenient().when(systemConfigService.getPasswordRequireLowercase()).thenReturn(true);
        lenient().when(systemConfigService.getPasswordRequireNumber()).thenReturn(false);
        lenient().when(systemConfigService.getPasswordRequireSpecial()).thenReturn(false);

        collectorRegisterRequest.setPassword("PASSWORD123"); // no lowercase

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.register(collectorRegisterRequest, httpRequest));

        assertEquals("WEAK_PASSWORD", exception.getErrorCode());
    }

    @Test
    void register_WeakPassword_NoNumber_ThrowsException() {
        lenient().when(systemConfigService.getPasswordRequireUppercase()).thenReturn(false);
        lenient().when(systemConfigService.getPasswordRequireLowercase()).thenReturn(false);
        lenient().when(systemConfigService.getPasswordRequireNumber()).thenReturn(true);
        lenient().when(systemConfigService.getPasswordRequireSpecial()).thenReturn(false);

        collectorRegisterRequest.setPassword("Password"); // no number

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.register(collectorRegisterRequest, httpRequest));

        assertEquals("WEAK_PASSWORD", exception.getErrorCode());
    }

    @Test
    void register_WeakPassword_NoSpecialChar_ThrowsException() {
        lenient().when(systemConfigService.getPasswordRequireUppercase()).thenReturn(false);
        lenient().when(systemConfigService.getPasswordRequireLowercase()).thenReturn(false);
        lenient().when(systemConfigService.getPasswordRequireNumber()).thenReturn(false);
        lenient().when(systemConfigService.getPasswordRequireSpecial()).thenReturn(true);

        collectorRegisterRequest.setPassword("Password123"); // no special char

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.register(collectorRegisterRequest, httpRequest));

        assertEquals("WEAK_PASSWORD", exception.getErrorCode());
    }

    @Test
    void register_WeakPassword_TooShort_ThrowsException() {
        lenient().when(systemConfigService.getPasswordMinLength()).thenReturn(8);
        lenient().when(systemConfigService.getPasswordRequireUppercase()).thenReturn(false);
        lenient().when(systemConfigService.getPasswordRequireLowercase()).thenReturn(false);
        lenient().when(systemConfigService.getPasswordRequireNumber()).thenReturn(false);
        lenient().when(systemConfigService.getPasswordRequireSpecial()).thenReturn(false);

        collectorRegisterRequest.setPassword("Pass1"); // less than 8 chars

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.register(collectorRegisterRequest, httpRequest));

        assertEquals("WEAK_PASSWORD", exception.getErrorCode());
    }
}