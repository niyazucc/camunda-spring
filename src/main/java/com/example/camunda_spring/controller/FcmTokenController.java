package com.example.camunda_spring.controller;

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

    @PostMapping("/save-fcm-token")
    public ResponseEntity<?> saveFcmToken(@RequestBody FcmTokenRequest request) {
        String username = request.getUserId();
        String token = request.getToken();

        try {
            // 1. Check if the user actually exists in your Camunda/H2 database
            if (identityService.createUserQuery().userId(username).singleResult() != null) {
                
                // 2. Save it directly to Camunda's built-in internal H2 storage!
                // This saves a key named "fcm_token" linked directly to that username string
                identityService.setUserInfo(username, "fcm_token", token);
                
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
}