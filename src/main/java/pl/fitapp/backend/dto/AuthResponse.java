package pl.fitapp.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private UUID userId;
    private String email;
    private String firstName;
    private String lastName;
    private Set<String> roles;
    private String accessToken;
    private String refreshToken;
    private String message;
}
