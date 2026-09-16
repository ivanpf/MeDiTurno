package com.mediturno.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mediturno.backend.model.DeviceToken;
import com.mediturno.backend.repository.DeviceTokenRepository;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    private static final String EXPO_PUSH_URL = "https://exp.host/--/api/v2/push/send";

    @Autowired
    private DeviceTokenRepository deviceTokenRepository;

    private final OkHttpClient httpClient = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DeviceToken registerToken(String token) {
        Optional<DeviceToken> existing = deviceTokenRepository.findByToken(token);
        if (existing.isPresent()) {
            DeviceToken deviceToken = existing.get();
            deviceToken.setActive(true);
            return deviceTokenRepository.save(deviceToken);
        }
        DeviceToken deviceToken = new DeviceToken(token);
        return deviceTokenRepository.save(deviceToken);
    }

    public void deactivateToken(String token) {
        deviceTokenRepository.findByToken(token).ifPresent(deviceToken -> {
            deviceToken.setActive(false);
            deviceTokenRepository.save(deviceToken);
        });
    }

    public List<DeviceToken> getAllActiveTokens() {
        return deviceTokenRepository.findByActiveTrue();
    }

    public CompletableFuture<Map<String, Object>> sendPushNotification(String title, String body, Map<String, Object> data) {
        List<DeviceToken> activeTokens = getAllActiveTokens();

        if (activeTokens.isEmpty()) {
            logger.warn("No hay tokens activos para enviar notificaciones");
            CompletableFuture<Map<String, Object>> future = new CompletableFuture<>();
            future.complete(Map.of("success", false, "message", "No hay tokens registrados"));
            return future;
        }

        List<Map<String, Object>> messages = new ArrayList<>();
        for (DeviceToken deviceToken : activeTokens) {
            Map<String, Object> message = new HashMap<>();
            message.put("to", deviceToken.getToken());
            message.put("title", title);
            message.put("body", body);
            if (data != null && !data.isEmpty()) {
                message.put("data", data);
            }
            message.put("sound", "default");
            messages.add(message);
        }

        return sendBatch(messages);
    }

    public CompletableFuture<Map<String, Object>> sendToToken(String token, String title, String body, Map<String, Object> data) {
        Map<String, Object> message = new HashMap<>();
        message.put("to", token);
        message.put("title", title);
        message.put("body", body);
        if (data != null && !data.isEmpty()) {
            message.put("data", data);
        }
        message.put("sound", "default");

        return sendBatch(List.of(message));
    }

    private CompletableFuture<Map<String, Object>> sendBatch(List<Map<String, Object>> messages) {
        try {
            String jsonBody = objectMapper.writeValueAsString(messages);

            RequestBody body = RequestBody.create(
                jsonBody,
                MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                .url(EXPO_PUSH_URL)
                .post(body)
                .addHeader("Accept", "application/json")
                .addHeader("Accept-encoding", "gzip, deflate")
                .addHeader("Content-Type", "application/json")
                .build();

            CompletableFuture<Map<String, Object>> future = new CompletableFuture<>();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    logger.error("Error enviando notificación push", e);
                    future.completeExceptionally(e);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        String responseBody = response.body().string();
                        Map<String, Object> result = objectMapper.readValue(responseBody, Map.class);
                        logger.info("Respuesta Expo Push: {}", responseBody);
                        future.complete(result);
                    } catch (Exception e) {
                        logger.error("Error parseando respuesta de Expo Push", e);
                        future.completeExceptionally(e);
                    } finally {
                        response.close();
                    }
                }
            });

            return future;
        } catch (Exception e) {
            logger.error("Error serializando mensaje push", e);
            CompletableFuture<Map<String, Object>> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }
}
