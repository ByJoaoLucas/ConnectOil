package com.oilcollection.controller;

import com.oilcollection.dto.AuthRequestDTO;
import com.oilcollection.dto.AuthResponseDTO;
import com.oilcollection.dto.UserDTO;
import com.oilcollection.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.oilcollection.model.User;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication endpoints")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "User login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody AuthRequestDTO request, HttpServletRequest httpRequest) {
        AuthResponseDTO response = authService.login(request, httpRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    @Operation(summary = "User registration")
    public ResponseEntity<AuthResponseDTO> register(@Valid @RequestBody UserDTO request, HttpServletRequest httpRequest) {
        AuthResponseDTO response = authService.register(request, httpRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token")
    public ResponseEntity<AuthResponseDTO> refreshToken(@RequestParam String refreshToken, HttpServletRequest httpRequest) {
        AuthResponseDTO response = authService.refreshToken(refreshToken, httpRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user info")
    public ResponseEntity<AuthResponseDTO.UserInfoDTO> getCurrentUser(@AuthenticationPrincipal User user) {
        AuthResponseDTO.UserInfoDTO userInfo = authService.getCurrentUser(user.getEmail());
        return ResponseEntity.ok(userInfo);
    }

    @PutMapping("/password")
    @Operation(summary = "Change password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal User user,
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            HttpServletRequest httpRequest) {
        authService.changePassword(user.getId(), currentPassword, newPassword, httpRequest);
        return ResponseEntity.ok().build();
    }
}