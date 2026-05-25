package com.example.camunda_spring.controller;

import com.example.camunda_spring.service.FcmTokenStore;
import com.example.camunda_spring.dto.*;
import org.camunda.bpm.engine.IdentityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/user")
@CrossOrigin(origins = "http://localhost:3000")
public class FcmTokenController {

    @Autowired
    private IdentityService identityService;

    @Autowired
    private FcmTokenStore fcmTokenStore;

    @PostMapping("/save-fcm-token")
    public ResponseEntity<?> saveFcmToken(@RequestBody FcmTokenRequest request) {
        String username = request.getUserId();
        String token = request.getToken();

        try {
            // 1. Check if the user actually exists in your Camunda/H2 database
            if (identityService.createUserQuery().userId(username).singleResult() != null) {
                
                // 2. Save the latest token after removing any duplicate stale rows.
                fcmTokenStore.saveToken(username, token);
                
                System.out.println("🚀 Saved token for " + username + " inside Camunda's H2 storage.");
                return ResponseEntity.ok(Map.of("status", "success", "message", "Token linked to Camunda user!"));
            } else {
                System.out.println("❌ User " + username + " not found in Camunda database.");
                return ResponseEntity.status(404).body(Map.of("status", "error", "message", "User not found"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    @PostMapping("/delete-fcm-token")
    public ResponseEntity<?> deleteFcmToken(@RequestBody FcmTokenRequest request) {
        String username = request.getUserId();

        try {
            if (username == null || username.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Missing userId"));
            }

            fcmTokenStore.deleteToken(username);
            System.out.println("Deleted FCM token for logged-out user: " + username);
            return ResponseEntity.ok(Map.of("status", "success", "message", "Token removed"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("status", "error", "message", e.getMessage()));
        }
    }
}
