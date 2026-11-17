package com.oilcollection.controller;

import com.oilcollection.model.User;
import com.oilcollection.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;

import java.lang.reflect.Method;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AuthControllerAccountDeletionTest {

    @Test
    void deleteAccountEndpointReturnsNoContentAndUsesAuthenticatedUser() {
        AuthService authService = mock(AuthService.class);
        AuthController controller = new AuthController(authService);
        User authenticatedUser = User.builder()
                .id(UUID.randomUUID())
                .email("collector@example.test")
                .role(User.UserRole.COLLECTOR)
                .isActive(true)
                .build();
        HttpServletRequest request = mock(HttpServletRequest.class);

        var response = controller.deleteOwnAccount(authenticatedUser, request);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(authService).deleteOwnAccount(authenticatedUser, request);
    }

    @Test
    void endpointIsMappedForOnlyCollectorAndEstablishmentRoles() throws NoSuchMethodException {
        Method endpoint = AuthController.class.getMethod(
                "deleteOwnAccount", User.class, HttpServletRequest.class);

        assertEquals("/account", endpoint.getAnnotation(DeleteMapping.class).value()[0]);
        PreAuthorize authorization = endpoint.getAnnotation(PreAuthorize.class);
        assertNotNull(authorization);
        assertEquals("hasAnyRole('COLLECTOR', 'ESTABLISHMENT')", authorization.value());
    }
}