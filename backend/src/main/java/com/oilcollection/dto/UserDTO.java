package com.oilcollection.dto;

import com.oilcollection.model.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserDTO {
    private java.util.UUID id;

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @NotBlank(message = "CPF is required")
    @Pattern(regexp = "^\\d{11}$", message = "CPF must contain exactly 11 digits")
    private String cpf;

    @Pattern(regexp = "^\\d{14}$", message = "CNPJ must contain exactly 14 digits")
    private String cnpj;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    private String phoneNumber;

    private String establishmentName;

    private String establishmentAddress;

    private String establishmentCity;

    private String establishmentState;

    private String establishmentZipCode;

    private User.UserRole role = User.UserRole.COLLECTOR;

    private Boolean isActive = true;

    public static UserDTO fromEntity(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setCpf(user.getCpf());
        dto.setCnpj(user.getCnpj());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setEstablishmentName(user.getEstablishmentName());
        dto.setEstablishmentAddress(user.getEstablishmentAddress());
        dto.setEstablishmentCity(user.getEstablishmentCity());
        dto.setEstablishmentState(user.getEstablishmentState());
        dto.setEstablishmentZipCode(user.getEstablishmentZipCode());
        dto.setRole(user.getRole());
        dto.setIsActive(user.getIsActive());
        return dto;
    }
}