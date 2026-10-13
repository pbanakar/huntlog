package com.pbanakar.huntlog.controller;

import com.pbanakar.huntlog.dto.request.UpdatePreferencesRequest;
import com.pbanakar.huntlog.dto.response.UserPreferencesResponse;
import com.pbanakar.huntlog.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User Preferences",
        description = "Manage user account settings")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me/preferences")
    @Operation(summary = "Get user preferences",
            description = "Retrieve current notification and account preferences.")
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponse(responseCode = "200", description = "Preferences retrieved successfully")
    public ResponseEntity<UserPreferencesResponse> getPreferences() {
        return ResponseEntity.ok(userService.getPreferences());
    }

    @PutMapping("/me/preferences")
    @Operation(summary = "Update email preferences",
            description = "Enable or disable automated follow-up email reminders for stale applications.")
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponse(responseCode = "200", description = "Preferences updated successfully")
    public ResponseEntity<UserPreferencesResponse> updatePreferences(
            @Valid @RequestBody UpdatePreferencesRequest request) {
        return ResponseEntity.ok(userService.updatePreferences(request));
    }

    @DeleteMapping("/me")
    @Operation(summary = "Delete user account",
            description = "Permanently delete user account and all associated applications.")
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponse(responseCode = "204", description = "Account deleted successfully")
    public ResponseEntity<Void> deleteAccount() {
        userService.deleteCurrentUser();
        return ResponseEntity.noContent().build();
    }
}
