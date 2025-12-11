package com.maps.backend.service;

import com.maps.backend.client.EmployeeAuthClient;
import com.maps.backend.client.ExternalBusWebSocketClient;
import com.maps.backend.websocket.InternalBusWebSocketHandler;
import org.java_websocket.enums.ReadyState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class WebSocketConnectionManager {

    private static final String AUTH_HEADER = "Auth";

    @Value("${bus.external.ws}")
    private String externalWsUrl;

    @Autowired
    private EmployeeAuthClient authClient;

    @Autowired
    private InternalBusWebSocketHandler internalHandler;

    // 👇 INYECTAR EL SERVICIO DE RUTAS
    @Autowired
    private RouteService routeService;

    private ExternalBusWebSocketClient wsClient;

    @Scheduled(fixedDelay = 5000)
    public void checkAndConnect() {
        try {
            if (wsClient != null && (wsClient.isOpen() || wsClient.getReadyState() == ReadyState.NOT_YET_CONNECTED)) {
                return;
            }

            System.out.println("🔄 Iniciando conexión...");

            String token = authClient.getAccessToken();
            if (token == null || token.isEmpty()) {
                System.err.println("⛔ Fallo al obtener token.");
                return;
            }

            String finalUrl = buildWebSocketUri(externalWsUrl, token);

            // 👇 PASAR routeService AL CONSTRUCTOR
            wsClient = new ExternalBusWebSocketClient(new URI(finalUrl), internalHandler, routeService);
            wsClient.connect();

            System.out.println("⏳ Conectando a: " + externalWsUrl);

        } catch (Exception e) {
            System.err.println("❌ Error conexión: " + e.getMessage());
        }
    }

    private String buildWebSocketUri(String baseUrl, String token) {
        try {
            String encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8.name());
            String separator = baseUrl.contains("?") ? "&" : "?";
            return baseUrl + separator + AUTH_HEADER + "=" + encodedToken;
        } catch (Exception e) {
            return baseUrl;
        }
    }
}