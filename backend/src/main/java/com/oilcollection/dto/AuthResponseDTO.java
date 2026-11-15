package com.oilcollection.dto;

import com.oilcollection.model.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDTO {
    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private Long expiresIn;
    private UserInfoDTO user;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfoDTO {
        private UUID id;
        private String name;
        private String email;
        private String cpf;
        private String phoneNumber;
        private User.UserRole role;
        private Boolean isActive;

        public static UserInfoDTO fromUser(User user) {
            return new UserInfoDTO(
                    user.getId(),
                    user.getName(),
                    user.getEmail(),
                    user.getCpf(),
                    user.getPhoneNumber(),
                    user.getRole(),
                    user.getIsActive()
            );
        }
    }
}