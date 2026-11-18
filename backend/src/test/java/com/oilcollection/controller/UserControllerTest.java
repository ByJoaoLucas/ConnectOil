package com.oilcollection.controller;

import com.oilcollection.dto.UserDTO;
import com.oilcollection.model.User;
import com.oilcollection.service.UserService;
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
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private HttpServletRequest httpRequest;

    @InjectMocks
    private UserController userController;

    private User adminUser;
    private UserDTO userDTO;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        lenient().when(httpRequest.getHeader("User-Agent")).thenReturn("test-agent");
        lenient().when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        lenient().when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        lenient().when(httpRequest.getHeader("X-Real-IP")).thenReturn(null);

        UUID adminId = UUID.randomUUID();

        adminUser = User.builder()
                .id(adminId)
                .name("Admin User")
                .email("admin@example.com")
                .password("encodedPassword")
                .cpf("11111111111")
                .role(User.UserRole.ADMIN)
                .isActive(true)
                .build();

        userDTO = new UserDTO();
        userDTO.setId(UUID.randomUUID());
        userDTO.setName("Test User");
        userDTO.setEmail("test@example.com");
        userDTO.setCpf("12345678901");
        userDTO.setRole(User.UserRole.COLLECTOR);
        userDTO.setIsActive(true);

        pageable = PageRequest.of(0, 20);
    }

    // ===== GET ALL USERS TESTS =====

    @Test
    void getAllUsers_ReturnsPage() {
        Page<UserDTO> userPage = new PageImpl<>(List.of(userDTO), pageable, 1);
        when(userService.getAllUsers(pageable)).thenReturn(userPage);

        ResponseEntity<Page<UserDTO>> response = userController.getAllUsers(pageable);

        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getTotalElements());
        assertEquals("test@example.com", response.getBody().getContent().get(0).getEmail());

        verify(userService).getAllUsers(pageable);
    }

    @Test
    void getAllUsersList_ReturnsList() {
        when(userService.getAllUsers()).thenReturn(List.of(userDTO));

        ResponseEntity<List<UserDTO>> response = userController.getAllUsersList();

        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("test@example.com", response.getBody().get(0).getEmail());

        verify(userService).getAllUsers();
    }

    // ===== GET USER BY ID TESTS =====

    @Test
    void getUserById_Success() {
        when(userService.getUserById(userDTO.getId())).thenReturn(userDTO);

        ResponseEntity<UserDTO> response = userController.getUserById(userDTO.getId());

        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals("test@example.com", response.getBody().getEmail());

        verify(userService).getUserById(userDTO.getId());
    }

    // ===== GET USERS BY ROLE TESTS =====

    @Test
    void getUsersByRole_ReturnsPage() {
        Page<UserDTO> userPage = new PageImpl<>(List.of(userDTO), pageable, 1);
        when(userService.getUsersByRole(User.UserRole.COLLECTOR, pageable)).thenReturn(userPage);

        ResponseEntity<Page<UserDTO>> response = userController.getUsersByRole(User.UserRole.COLLECTOR, pageable);

        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getTotalElements());
        assertEquals(User.UserRole.COLLECTOR, response.getBody().getContent().get(0).getRole());

        verify(userService).getUsersByRole(eq(User.UserRole.COLLECTOR), eq(pageable));
    }

    // ===== GET ACTIVE COLLECTORS TESTS =====

    @Test
    void getActiveCollectors_ReturnsList() {
        when(userService.getActiveCollectors()).thenReturn(List.of(userDTO));

        ResponseEntity<List<UserDTO>> response = userController.getActiveCollectors();

        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(User.UserRole.COLLECTOR, response.getBody().get(0).getRole());

        verify(userService).getActiveCollectors();
    }

    // ===== CREATE USER TESTS =====

    @Test
    void createUser_Success() {
        UserDTO createRequest = new UserDTO();
        createRequest.setName("New User");
        createRequest.setEmail("new@example.com");
        createRequest.setPassword("password123");
        createRequest.setCpf("98765432109");
        createRequest.setRole(User.UserRole.COLLECTOR);
        createRequest.setIsActive(true);

        UserDTO createdUser = new UserDTO();
        createdUser.setId(UUID.randomUUID());
        createdUser.setName("New User");
        createdUser.setEmail("new@example.com");
        createdUser.setCpf("98765432109");
        createdUser.setRole(User.UserRole.COLLECTOR);
        createdUser.setIsActive(true);

        when(userService.createUser(any(UserDTO.class), eq(adminUser), anyString(), anyString()))
                .thenReturn(createdUser);

        ResponseEntity<UserDTO> response = userController.createUser(createRequest, adminUser, httpRequest);

        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals("new@example.com", response.getBody().getEmail());

        verify(userService).createUser(any(UserDTO.class), eq(adminUser), anyString(), anyString());
    }

    // ===== UPDATE USER TESTS =====

    @Test
    void updateUser_Success() {
        UserDTO updateRequest = new UserDTO();
        updateRequest.setName("Updated User");
        updateRequest.setEmail("updated@example.com");
        updateRequest.setCpf("12345678901");
        updateRequest.setRole(User.UserRole.COLLECTOR);
        updateRequest.setIsActive(true);

        UserDTO updatedUser = new UserDTO();
        updatedUser.setId(userDTO.getId());
        updatedUser.setName("Updated User");
        updatedUser.setEmail("updated@example.com");
        updatedUser.setCpf("12345678901");
        updatedUser.setRole(User.UserRole.COLLECTOR);
        updatedUser.setIsActive(true);

        when(userService.updateUser(eq(userDTO.getId()), any(UserDTO.class), eq(adminUser), anyString(), anyString()))
                .thenReturn(updatedUser);

        ResponseEntity<UserDTO> response = userController.updateUser(userDTO.getId(), updateRequest, adminUser, httpRequest);

        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals("updated@example.com", response.getBody().getEmail());

        verify(userService).updateUser(eq(userDTO.getId()), any(UserDTO.class), eq(adminUser), anyString(), anyString());
    }

    // ===== DELETE USER TESTS =====

    @Test
    void deleteUser_Success() {
        doNothing().when(userService).deleteUser(eq(userDTO.getId()), eq(adminUser), anyString(), anyString());

        ResponseEntity<Void> response = userController.deleteUser(userDTO.getId(), adminUser, httpRequest);

        assertEquals(204, response.getStatusCodeValue());

        verify(userService).deleteUser(eq(userDTO.getId()), eq(adminUser), anyString(), anyString());
    }

    // ===== ACTIVATE USER TESTS =====

    @Test
    void activateUser_Success() {
        UserDTO activatedUser = new UserDTO();
        activatedUser.setId(userDTO.getId());
        activatedUser.setName("Test User");
        activatedUser.setEmail("test@example.com");
        activatedUser.setCpf("12345678901");
        activatedUser.setRole(User.UserRole.COLLECTOR);
        activatedUser.setIsActive(true);

        when(userService.activateUser(eq(userDTO.getId()), eq(adminUser), anyString(), anyString()))
                .thenReturn(activatedUser);

        ResponseEntity<UserDTO> response = userController.activateUser(userDTO.getId(), adminUser, httpRequest);

        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getIsActive());

        verify(userService).activateUser(eq(userDTO.getId()), eq(adminUser), anyString(), anyString());
    }

    // ===== ASSIGN COLLECTOR ROLE TESTS =====

    @Test
    void assignCollectorRole_Success() {
        UserDTO collectorUser = new UserDTO();
        collectorUser.setId(userDTO.getId());
        collectorUser.setName("Test User");
        collectorUser.setEmail("test@example.com");
        collectorUser.setCpf("12345678901");
        collectorUser.setRole(User.UserRole.COLLECTOR);
        collectorUser.setIsActive(true);

        when(userService.assignCollectorRole(eq(userDTO.getId()), eq(adminUser), anyString(), anyString()))
                .thenReturn(collectorUser);

        ResponseEntity<UserDTO> response = userController.assignCollectorRole(userDTO.getId(), adminUser, httpRequest);

        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(User.UserRole.COLLECTOR, response.getBody().getRole());

        verify(userService).assignCollectorRole(eq(userDTO.getId()), eq(adminUser), anyString(), anyString());
    }
}