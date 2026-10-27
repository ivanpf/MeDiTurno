package com.mediturno.backend.notificaciones;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.mediturno.backend.notificaciones.repository.TokenDispositivoRepository;

/**
 * Registro de dispositivos y envío best-effort de notificaciones push mediante Expo.
 */
@Service
public class PushNotificationService {

    private static final Logger log = LoggerFactory.getLogger(PushNotificationService.class);
    private final TokenDispositivoRepository tokenDispositivoRepository;
    private final ObjectMapper objectMapper;
    private final Clock reloj;
    private final String expoAccessToken;
    private final URI expoPushUrl;
    private final HttpClient httpClient;

    public PushNotificationService(TokenDispositivoRepository tokenDispositivoRepository, ObjectMapper objectMapper,
            Clock reloj, @Value("${mediturno.push.access-token:}") String expoAccessToken,
            @Value("${mediturno.push.url:https://exp.host/--/api/v2/push/send}") URI expoPushUrl) {
        this.tokenDispositivoRepository = tokenDispositivoRepository;
        this.objectMapper = objectMapper;
        this.reloj = reloj;
        this.expoAccessToken = expoAccessToken;
        this.expoPushUrl = expoPushUrl;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    /** CU-6. El endpoint no aparece en los diagramas; es necesario para que la app móvil registre su token. */
    @Transactional
    public void registrar(Long idUsuario, String token) {
        tokenDispositivoRepository.registrarOActualizar(idUsuario, token, LocalDateTime.now(reloj));
    }

    /** CU-6. El endpoint no aparece en los diagramas; es necesario para que la app móvil elimine su token. */
    @Transactional
    public void eliminar(Long idUsuario) {
        tokenDispositivoRepository.deleteByUsuarioIdUsuario(idUsuario);
    }

    /** CU-8, CU-13 y CU-23. Uso interno. Nunca propaga fallos del servicio externo. */
    public void enviarPush(Long idUsuario, String titulo, String mensaje) {
        enviarPush(idUsuario, titulo, mensaje, Map.of());
    }

    /** CU-8, CU-13 y CU-23. Uso interno; {@code datos} se agrega al campo data del mensaje Expo. */
    public void enviarPush(Long idUsuario, String titulo, String mensaje, Map<String, Object> datos) {
        try {
            List<String> tokens = tokenDispositivoRepository.buscarTokensPorUsuario(idUsuario);
            if (tokens.isEmpty()) {
                log.debug("No hay tokens push registrados para el usuario {}", idUsuario);
                return;
            }

            List<Map<String, Object>> mensajes = new ArrayList<>(tokens.size());
            for (String token : tokens) {
                Map<String, Object> push = new LinkedHashMap<>();
                push.put("to", token);
                push.put("title", titulo);
                push.put("body", mensaje);
                push.put("sound", "default");
                push.put("data", datos == null ? Map.of() : datos);
                mensajes.add(push);
            }

            HttpRequest.Builder request = HttpRequest.newBuilder(expoPushUrl)
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/json")
                .header("Accept-encoding", "gzip, deflate")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(mensajes)));
            if (!expoAccessToken.isBlank()) {
                request.header("Authorization", "Bearer " + expoAccessToken);
            }

            HttpResponse<byte[]> response = httpClient.send(request.build(), HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Expo push respondió HTTP {} para el usuario {}", response.statusCode(), idUsuario);
                return;
            }

            String cuerpo = decodificarCuerpo(response);
            JsonNode tickets = objectMapper.readTree(cuerpo).path("data");
            if (!tickets.isArray()) {
                log.warn("Expo push devolvió una respuesta sin tickets válidos para el usuario {}", idUsuario);
                return;
            }

            List<String> tokensNoRegistrados = new ArrayList<>();
            int procesados = Math.min(tickets.size(), tokens.size());
            for (int i = 0; i < procesados; i++) {
                JsonNode ticket = tickets.get(i);
                if ("DeviceNotRegistered".equals(ticket.path("details").path("error").asText())) {
                    tokensNoRegistrados.add(tokens.get(i));
                }
            }
            if (!tokensNoRegistrados.isEmpty()) {
                tokenDispositivoRepository.deleteByTokenIn(tokensNoRegistrados);
                log.info("Expo push: se eliminaron {} tokens de dispositivos no registrados", tokensNoRegistrados.size());
            }
            log.info("Expo push: respuesta HTTP {}, {} de {} tickets recibidos para el usuario {}",
                response.statusCode(), procesados, tokens.size(), idUsuario);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Se interrumpió el envío push al usuario {}: {}", idUsuario, e.getMessage());
        } catch (IOException | RuntimeException e) {
            log.warn("No se pudo completar el envío push al usuario {}: {}", idUsuario, e.getMessage());
        }
    }

    private static String decodificarCuerpo(HttpResponse<byte[]> response) throws IOException {
        String codificacion = response.headers().firstValue("Content-Encoding").orElse("").toLowerCase();
        InputStream entrada = new ByteArrayInputStream(response.body());
        if (codificacion.contains("gzip")) {
            entrada = new GZIPInputStream(entrada);
        } else if (codificacion.contains("deflate")) {
            entrada = new InflaterInputStream(entrada);
        }
        try (InputStream cuerpo = entrada; ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            cuerpo.transferTo(salida);
            return salida.toString(java.nio.charset.StandardCharsets.UTF_8);
        }
    }
}
