package com.pbanakar.huntlog.controller;

import com.pbanakar.huntlog.dto.request.UpdatePreferencesRequest;
import com.pbanakar.huntlog.dto.response.UserPreferencesResponse;
import com.pbanakar.huntlog.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me/preferences")
    public ResponseEntity<UserPreferencesResponse> getPreferences() {
        return ResponseEntity.ok(userService.getPreferences());
    }

    @PutMapping("/me/preferences")
    public ResponseEntity<UserPreferencesResponse> updatePreferences(
            @Valid @RequestBody UpdatePreferencesRequest request) {
        return ResponseEntity.ok(userService.updatePreferences(request));
    }
}
