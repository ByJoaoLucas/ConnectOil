package com.oilcollection.service;

import com.oilcollection.exception.BusinessException;
import com.oilcollection.exception.ResourceNotFoundException;
import com.oilcollection.model.User;
import com.oilcollection.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceAccountDeletionTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private HttpServletRequest httpRequest;

    @InjectMocks
    private AuthService authService;

    @Test
    void collectorCanDeleteOwnAccountAndAccountIsAudited() {
        assertAccountIsDeactivatedAndAudited(User.UserRole.COLLECTOR);
    }

    @Test
    void establishmentCanDeleteOwnAccountAndAccountIsAudited() {
        assertAccountIsDeactivatedAndAudited(User.UserRole.ESTABLISHMENT);
    }

    @Test
    void adminCannotUseSelfServiceAccountDeletion() {
        User admin = activeUser(User.UserRole.ADMIN);
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.deleteOwnAccount(admin, httpRequest));

        assertEquals("ACCOUNT_DELETION_NOT_ALLOWED", exception.getErrorCode());
        assertTrue(admin.getIsActive());
        verify(userRepository, never()).save(admin);
        verify(auditLogService, never()).logDelete(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void alreadyDeactivatedAccountCannotBeDeletedAgain() {
        User collector = activeUser(User.UserRole.COLLECTOR);
        collector.setIsActive(false);
        when(userRepository.findById(collector.getId())).thenReturn(Optional.of(collector));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.deleteOwnAccount(collector, httpRequest));

        assertEquals("ACCOUNT_ALREADY_DELETED", exception.getErrorCode());
        verify(userRepository, never()).save(collector);
        verify(auditLogService, never()).logDelete(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void missingAccountIsNotFound() {
        User collector = activeUser(User.UserRole.COLLECTOR);
        when(userRepository.findById(collector.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> authService.deleteOwnAccount(collector, httpRequest));

        verify(userRepository, never()).save(collector);
        verify(auditLogService, never()).logDelete(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
    }

    private void assertAccountIsDeactivatedAndAudited(User.UserRole role) {
        User user = activeUser(role);
        lenient().when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        lenient().when(httpRequest.getHeader("User-Agent")).thenReturn("account-deletion-test");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        authService.deleteOwnAccount(user, httpRequest);

        assertFalse(user.getIsActive());
        verify(userRepository).save(user);
        verify(auditLogService).logDelete(
                "USER",
                user.getId(),
                user,
                "{\"email\":\"" + user.getEmail() + "\",\"role\":\"" + role + "\"}",
                "127.0.0.1",
                "account-deletion-test");
    }

    private User activeUser(User.UserRole role) {
        return User.builder()
                .id(UUID.randomUUID())
                .name("Account deletion test")
                .email("delete-" + role.name().toLowerCase() + "@example.test")
                .cpf("12345678901")
                .password("encoded-password")
                .role(role)
                .isActive(true)
                .build();
    }
}