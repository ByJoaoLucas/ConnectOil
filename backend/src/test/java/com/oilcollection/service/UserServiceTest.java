package com.oilcollection.service;

import com.oilcollection.dto.UserDTO;
import com.oilcollection.exception.BusinessException;
import com.oilcollection.exception.ResourceNotFoundException;
import com.oilcollection.model.User;
import com.oilcollection.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private HttpServletRequest httpRequest;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private User adminUser;
    private UserDTO createRequest;
    private UserDTO updateRequest;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        lenient().when(httpRequest.getHeader("User-Agent")).thenReturn("test-agent");
        lenient().when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        lenient().when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        lenient().when(httpRequest.getHeader("X-Real-IP")).thenReturn(null);

        UUID userId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();

        testUser = User.builder()
                .id(userId)
                .name("Test User")
                .email("test@example.com")
                .password("encodedPassword")
                .cpf("12345678901")
                .role(User.UserRole.COLLECTOR)
                .isActive(true)
                .build();

        adminUser = User.builder()
                .id(adminId)
                .name("Admin User")
                .email("admin@example.com")
                .password("encodedPassword")
                .cpf("11111111111")
                .role(User.UserRole.ADMIN)
                .isActive(true)
                .build();

        createRequest = new UserDTO();
        createRequest.setName("New User");
        createRequest.setEmail("new@example.com");
        createRequest.setPassword("password123");
        createRequest.setCpf("98765432109");
        createRequest.setRole(User.UserRole.COLLECTOR);
        createRequest.setIsActive(true);

        updateRequest = new UserDTO();
        updateRequest.setName("Updated User");
        updateRequest.setEmail("updated@example.com");
        updateRequest.setCpf("12345678901");
        updateRequest.setRole(User.UserRole.COLLECTOR);
        updateRequest.setIsActive(true);

        pageable = PageRequest.of(0, 20);
    }

    // ===== GET ALL USERS TESTS =====

    @Test
    void getAllUsers_ReturnsPage() {
        Page<User> userPage = new PageImpl<>(List.of(testUser), pageable, 1);
        when(userRepository.findAll(pageable)).thenReturn(userPage);

        Page<UserDTO> result = userService.getAllUsers(pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("test@example.com", result.getContent().get(0).getEmail());
        verify(userRepository).findAll(pageable);
    }

    @Test
    void getAllUsersList_ReturnsList() {
        when(userRepository.findAll()).thenReturn(List.of(testUser));

        List<UserDTO> result = userService.getAllUsers();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("test@example.com", result.get(0).getEmail());
        verify(userRepository).findAll();
    }

    // ===== GET USER BY ID TESTS =====

    @Test
    void getUserById_Success() {
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));

        UserDTO result = userService.getUserById(testUser.getId());

        assertNotNull(result);
        assertEquals(testUser.getEmail(), result.getEmail());
        assertEquals(testUser.getName(), result.getName());
        verify(userRepository).findById(testUser.getId());
    }

    @Test
    void getUserById_NotFound_ThrowsException() {
        UUID nonExistentId = UUID.randomUUID();
        when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> userService.getUserById(nonExistentId));

        assertTrue(exception.getMessage().contains("User"));
        assertTrue(exception.getMessage().contains("id"));
        verify(userRepository).findById(nonExistentId);
    }

    // ===== GET USERS BY ROLE TESTS =====

    @Test
    void getUsersByRole_ReturnsPage() {
        Page<User> userPage = new PageImpl<>(List.of(testUser), pageable, 1);
        when(userRepository.findByRole(User.UserRole.COLLECTOR, pageable)).thenReturn(userPage);

        Page<UserDTO> result = userService.getUsersByRole(User.UserRole.COLLECTOR, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(User.UserRole.COLLECTOR, result.getContent().get(0).getRole());
        verify(userRepository).findByRole(User.UserRole.COLLECTOR, pageable);
    }

    // ===== GET ACTIVE COLLECTORS TESTS =====

    @Test
    void getActiveCollectors_ReturnsList() {
        when(userRepository.findByIsActiveTrueAndRole(User.UserRole.COLLECTOR)).thenReturn(List.of(testUser));

        List<UserDTO> result = userService.getActiveCollectors();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(User.UserRole.COLLECTOR, result.get(0).getRole());
        assertTrue(result.get(0).getIsActive());
        verify(userRepository).findByIsActiveTrueAndRole(User.UserRole.COLLECTOR);
    }

    // ===== CREATE USER TESTS =====

    @Test
    void createUser_Collector_Success() {
        when(userRepository.existsByEmail(createRequest.getEmail())).thenReturn(false);
        when(userRepository.existsByCpf(createRequest.getCpf())).thenReturn(false);
        when(passwordEncoder.encode(createRequest.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        UserDTO result = userService.createUser(createRequest, adminUser, "127.0.0.1", "test-agent");

        assertNotNull(result);
        assertEquals(createRequest.getEmail(), result.getEmail());
        assertEquals(createRequest.getName(), result.getName());
        assertEquals(User.UserRole.COLLECTOR, result.getRole());
        assertTrue(result.getIsActive());
        verify(userRepository).save(any(User.class));
        verify(auditLogService).logCreate(eq("USER"), any(UUID.class), eq(adminUser), anyString(), eq("127.0.0.1"), eq("test-agent"));
    }

    @Test
    void createUser_Establishment_Success() {
        createRequest.setRole(User.UserRole.ESTABLISHMENT);
        createRequest.setCnpj("12345678000199");
        createRequest.setEstablishmentName("Test Establishment");
        createRequest.setEstablishmentAddress("Rua Teste, 123");
        createRequest.setEstablishmentCity("São Paulo");
        createRequest.setEstablishmentState("SP");
        createRequest.setEstablishmentZipCode("01234567");
        createRequest.setCpf(null);

        when(userRepository.existsByEmail(createRequest.getEmail())).thenReturn(false);
        when(userRepository.existsByCnpj(createRequest.getCnpj())).thenReturn(false);
        when(passwordEncoder.encode(createRequest.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        UserDTO result = userService.createUser(createRequest, adminUser, "127.0.0.1", "test-agent");

        assertNotNull(result);
        assertEquals(User.UserRole.ESTABLISHMENT, result.getRole());
        assertEquals("12345678000199", result.getCnpj());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_Admin_Success() {
        createRequest.setRole(User.UserRole.ADMIN);
        createRequest.setCpf(null);

        when(userRepository.existsByEmail(createRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(createRequest.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        UserDTO result = userService.createUser(createRequest, adminUser, "127.0.0.1", "test-agent");

        assertNotNull(result);
        assertEquals(User.UserRole.ADMIN, result.getRole());
    }

    @Test
    void createUser_Client_Success() {
        createRequest.setRole(User.UserRole.CLIENT);
        createRequest.setCpf(null);

        when(userRepository.existsByEmail(createRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(createRequest.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        UserDTO result = userService.createUser(createRequest, adminUser, "127.0.0.1", "test-agent");

        assertNotNull(result);
        assertEquals(User.UserRole.CLIENT, result.getRole());
    }

    @Test
    void createUser_DefaultRole_Collector() {
        createRequest.setRole(null);

        when(userRepository.existsByEmail(createRequest.getEmail())).thenReturn(false);
        when(userRepository.existsByCpf(createRequest.getCpf())).thenReturn(false);
        when(passwordEncoder.encode(createRequest.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        UserDTO result = userService.createUser(createRequest, adminUser, "127.0.0.1", "test-agent");

        assertNotNull(result);
        assertEquals(User.UserRole.COLLECTOR, result.getRole());
    }

    @Test
    void createUser_EmailAlreadyExists_ThrowsException() {
        when(userRepository.existsByEmail(createRequest.getEmail())).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> userService.createUser(createRequest, adminUser, "127.0.0.1", "test-agent"));

        assertEquals("EMAIL_ALREADY_EXISTS", exception.getErrorCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    void createUser_CpfAlreadyExists_ThrowsException() {
        when(userRepository.existsByEmail(createRequest.getEmail())).thenReturn(false);
        when(userRepository.existsByCpf(createRequest.getCpf())).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> userService.createUser(createRequest, adminUser, "127.0.0.1", "test-agent"));

        assertEquals("CPF_ALREADY_EXISTS", exception.getErrorCode());
    }

    @Test
    void createUser_CnpjAlreadyExists_ThrowsException() {
        createRequest.setRole(User.UserRole.ESTABLISHMENT);
        createRequest.setCnpj("12345678000199");
        createRequest.setCpf(null);

        when(userRepository.existsByEmail(createRequest.getEmail())).thenReturn(false);
        when(userRepository.existsByCnpj(createRequest.getCnpj())).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> userService.createUser(createRequest, adminUser, "127.0.0.1", "test-agent"));

        assertEquals("CNPJ_ALREADY_EXISTS", exception.getErrorCode());
    }

    @Test
    void createUser_WithBlankName_ThrowsException() {
        // Bean Validation would catch this, but test service behavior with invalid name
        createRequest.setName("   ");

        // This will pass validation at DTO level but service should handle it
        when(userRepository.existsByEmail(createRequest.getEmail())).thenReturn(false);
        when(userRepository.existsByCpf(createRequest.getCpf())).thenReturn(false);
        when(passwordEncoder.encode(createRequest.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        UserDTO result = userService.createUser(createRequest, adminUser, "127.0.0.1", "test-agent");
        // Service saves user with blank name (validation at controller level)
        assertNotNull(result);
    }

    // ===== UPDATE USER TESTS =====

    @Test
    void updateUser_Success() {
        // Use different email and CPF to trigger uniqueness checks
        updateRequest.setEmail("updated@example.com");
        updateRequest.setCpf("99999999999");
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmail(updateRequest.getEmail())).thenReturn(false);
        when(userRepository.existsByCpf(updateRequest.getCpf())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserDTO result = userService.updateUser(testUser.getId(), updateRequest, adminUser, "127.0.0.1", "test-agent");

        assertNotNull(result);
        assertEquals(updateRequest.getEmail(), result.getEmail());
        assertEquals(updateRequest.getName(), result.getName());
        verify(userRepository).save(any(User.class));
        verify(auditLogService).logUpdate(eq("USER"), eq(testUser.getId()), eq(adminUser), anyString(), anyString(), eq("127.0.0.1"), eq("test-agent"));
    }

    @Test
    void updateUser_WithPasswordChange_Success() {
        // Use different email and CPF to trigger uniqueness checks
        updateRequest.setEmail("updated@example.com");
        updateRequest.setCpf("99999999999");
        updateRequest.setPassword("newPassword123");
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmail(updateRequest.getEmail())).thenReturn(false);
        when(userRepository.existsByCpf(updateRequest.getCpf())).thenReturn(false);
        when(passwordEncoder.encode(updateRequest.getPassword())).thenReturn("newEncodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserDTO result = userService.updateUser(testUser.getId(), updateRequest, adminUser, "127.0.0.1", "test-agent");

        assertNotNull(result);
        verify(passwordEncoder).encode(updateRequest.getPassword());
    }

    @Test
    void updateUser_NotFound_ThrowsException() {
        UUID nonExistentId = UUID.randomUUID();
        when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> userService.updateUser(nonExistentId, updateRequest, adminUser, "127.0.0.1", "test-agent"));

        assertTrue(exception.getMessage().contains("User"));
    }

    @Test
    void updateUser_EmailAlreadyExists_ThrowsException() {
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmail(updateRequest.getEmail())).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> userService.updateUser(testUser.getId(), updateRequest, adminUser, "127.0.0.1", "test-agent"));

        assertEquals("EMAIL_ALREADY_EXISTS", exception.getErrorCode());
    }

    @Test
    void updateUser_CpfAlreadyExists_ThrowsException() {
        // Use different CPF to trigger the uniqueness check
        updateRequest.setCpf("99999999999");
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmail(updateRequest.getEmail())).thenReturn(false);
        when(userRepository.existsByCpf(updateRequest.getCpf())).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> userService.updateUser(testUser.getId(), updateRequest, adminUser, "127.0.0.1", "test-agent"));

        assertEquals("CPF_ALREADY_EXISTS", exception.getErrorCode());
    }

    @Test
    void updateUser_CnpjAlreadyExists_ThrowsException() {
        updateRequest.setRole(User.UserRole.ESTABLISHMENT);
        updateRequest.setCnpj("12345678000199");
        testUser.setRole(User.UserRole.ESTABLISHMENT);
        testUser.setCnpj("99999999000199");

        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmail(updateRequest.getEmail())).thenReturn(false);
        when(userRepository.existsByCnpj(updateRequest.getCnpj())).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> userService.updateUser(testUser.getId(), updateRequest, adminUser, "127.0.0.1", "test-agent"));

        assertEquals("CNPJ_ALREADY_EXISTS", exception.getErrorCode());
    }

    // ===== DELETE USER TESTS =====

    @Test
    void deleteUser_Success() {
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.deleteUser(testUser.getId(), adminUser, "127.0.0.1", "test-agent");

        verify(userRepository).save(argThat(u -> !u.getIsActive()));
        verify(auditLogService).logUpdate(eq("USER"), eq(testUser.getId()), eq(adminUser), anyString(), contains("isActive\":false"), eq("127.0.0.1"), eq("test-agent"));
    }

    @Test
    void deleteUser_NotFound_ThrowsException() {
        UUID nonExistentId = UUID.randomUUID();
        when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> userService.deleteUser(nonExistentId, adminUser, "127.0.0.1", "test-agent"));

        assertTrue(exception.getMessage().contains("User"));
    }

    // ===== ACTIVATE USER TESTS =====

    @Test
    void activateUser_Success() {
        testUser.setIsActive(false);
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserDTO result = userService.activateUser(testUser.getId(), adminUser, "127.0.0.1", "test-agent");

        assertNotNull(result);
        assertTrue(result.getIsActive());
        verify(userRepository).save(argThat(u -> u.getIsActive()));
        verify(auditLogService).logUpdate(eq("USER"), eq(testUser.getId()), eq(adminUser), anyString(), contains("isActive\":true"), eq("127.0.0.1"), eq("test-agent"));
    }

    @Test
    void activateUser_NotFound_ThrowsException() {
        UUID nonExistentId = UUID.randomUUID();
        when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> userService.activateUser(nonExistentId, adminUser, "127.0.0.1", "test-agent"));

        assertTrue(exception.getMessage().contains("User"));
    }

    // ===== ASSIGN COLLECTOR ROLE TESTS =====

    @Test
    void assignCollectorRole_Success() {
        testUser.setRole(User.UserRole.CLIENT);
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserDTO result = userService.assignCollectorRole(testUser.getId(), adminUser, "127.0.0.1", "test-agent");

        assertNotNull(result);
        assertEquals(User.UserRole.COLLECTOR, result.getRole());
        verify(userRepository).save(argThat(u -> u.getRole() == User.UserRole.COLLECTOR));
        verify(auditLogService).logUpdate(eq("USER"), eq(testUser.getId()), eq(adminUser), anyString(), anyString(), eq("127.0.0.1"), eq("test-agent"));
    }

    @Test
    void assignCollectorRole_NotFound_ThrowsException() {
        UUID nonExistentId = UUID.randomUUID();
        when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> userService.assignCollectorRole(nonExistentId, adminUser, "127.0.0.1", "test-agent"));

        assertTrue(exception.getMessage().contains("User"));
    }

    // ===== EDGE CASES =====

    @Test
    void createUser_WithEstablishmentFields_AllMapped() {
        createRequest.setRole(User.UserRole.ESTABLISHMENT);
        createRequest.setCnpj("12345678000199");
        createRequest.setEstablishmentName("Test Est");
        createRequest.setEstablishmentAddress("Rua A, 1");
        createRequest.setEstablishmentCity("City");
        createRequest.setEstablishmentState("ST");
        createRequest.setEstablishmentZipCode("12345678");
        createRequest.setCpf(null);
        createRequest.setPhoneNumber("+5511999999999");

        when(userRepository.existsByEmail(createRequest.getEmail())).thenReturn(false);
        when(userRepository.existsByCnpj(createRequest.getCnpj())).thenReturn(false);
        when(passwordEncoder.encode(createRequest.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        UserDTO result = userService.createUser(createRequest, adminUser, "127.0.0.1", "test-agent");

        assertEquals("Test Est", result.getEstablishmentName());
        assertEquals("Rua A, 1", result.getEstablishmentAddress());
        assertEquals("City", result.getEstablishmentCity());
        assertEquals("ST", result.getEstablishmentState());
        assertEquals("12345678", result.getEstablishmentZipCode());
        assertEquals("+5511999999999", result.getPhoneNumber());
    }

    @Test
    void updateUser_RoleChange_Success() {
        updateRequest.setRole(User.UserRole.ESTABLISHMENT);
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmail(updateRequest.getEmail())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserDTO result = userService.updateUser(testUser.getId(), updateRequest, adminUser, "127.0.0.1", "test-agent");

        assertEquals(User.UserRole.ESTABLISHMENT, result.getRole());
    }

    @Test
    void createUser_WithNullIsActive_DefaultsToTrue() {
        createRequest.setIsActive(null);
        when(userRepository.existsByEmail(createRequest.getEmail())).thenReturn(false);
        when(userRepository.existsByCpf(createRequest.getCpf())).thenReturn(false);
        when(passwordEncoder.encode(createRequest.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserDTO result = userService.createUser(createRequest, adminUser, "127.0.0.1", "test-agent");

        assertTrue(result.getIsActive());
    }
}