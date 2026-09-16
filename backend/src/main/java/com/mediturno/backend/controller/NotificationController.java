package com.mediturno.backend.controller;

import com.mediturno.backend.model.DeviceToken;
import com.mediturno.backend.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/notificar")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> registerToken(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Token es requerido"));
        }

        DeviceToken deviceToken = notificationService.registerToken(token);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Token registrado correctamente",
            "id", deviceToken.getId()
        ));
    }

    @PostMapping("/deactivate")
    public ResponseEntity<Map<String, Object>> deactivateToken(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Token es requerido"));
        }

        notificationService.deactivateToken(token);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Token desactivado correctamente"
        ));
    }

    @GetMapping("/tokens")
    public ResponseEntity<List<DeviceToken>> listTokens() {
        return ResponseEntity.ok(notificationService.getAllActiveTokens());
    }

    @PostMapping("/send")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> sendNotification(@RequestBody Map<String, Object> body) {
        String title = (String) body.get("title");
        String msgBody = (String) body.get("body");
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) body.get("data");

        if (title == null || title.isBlank() || msgBody == null || msgBody.isBlank()) {
            CompletableFuture<ResponseEntity<Map<String, Object>>> future = new CompletableFuture<>();
            future.complete(ResponseEntity.badRequest().body(Map.of("error", "title y body son requeridos")));
            return future;
        }

        return notificationService.sendPushNotification(title, msgBody, data)
            .thenApply(result -> ResponseEntity.ok(result))
            .exceptionally(ex -> ResponseEntity.internalServerError().body(Map.of("error", ex.getMessage())));
    }

    @PostMapping("/send-to")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> sendToToken(@RequestBody Map<String, Object> body) {
        String token = (String) body.get("token");
        String title = (String) body.get("title");
        String msgBody = (String) body.get("body");
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) body.get("data");

        if (token == null || token.isBlank() || title == null || title.isBlank() || msgBody == null || msgBody.isBlank()) {
            CompletableFuture<ResponseEntity<Map<String, Object>>> future = new CompletableFuture<>();
            future.complete(ResponseEntity.badRequest().body(Map.of("error", "token, title y body son requeridos")));
            return future;
        }

        return notificationService.sendToToken(token, title, msgBody, data)
            .thenApply(result -> ResponseEntity.ok(result))
            .exceptionally(ex -> ResponseEntity.internalServerError().body(Map.of("error", ex.getMessage())));
    }
}
