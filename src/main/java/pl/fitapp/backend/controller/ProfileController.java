package pl.fitapp.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pl.fitapp.backend.config.CustomUserDetails;
import pl.fitapp.backend.dto.ChangePasswordRequest;
import pl.fitapp.backend.dto.UpdateProfileRequest;
import pl.fitapp.backend.dto.UserProfileResponse;
import pl.fitapp.backend.service.UserService;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<UserProfileResponse> getProfile(@AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID userId = userDetails.getId();
        UserProfileResponse profile = userService.getProfile(userId);
        return ResponseEntity.ok(profile);
    }

    @PutMapping
    public ResponseEntity<UserProfileResponse> updateProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest request) {
        UUID userId = userDetails.getId();
        UserProfileResponse profile = userService.updateProfile(userId, request);
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/password")
    public ResponseEntity<Map<String, String>> changePassword(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request) {
        UUID userId = userDetails.getId();
        userService.changePassword(userId, request);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteAccount(@AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID userId = userDetails.getId();
        userService.deleteAccount(userId);
        return ResponseEntity.noContent().build();
    }
}
