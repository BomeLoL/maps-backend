package com.maps.backend.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maps.backend.model.BusPosition;
import com.maps.backend.service.RouteService; // 👈 Importamos el servicio
import com.maps.backend.websocket.InternalBusWebSocketHandler;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class ExternalBusWebSocketClient extends WebSocketClient {

    private final InternalBusWebSocketHandler internalHandler;
    private final RouteService routeService; // 👈 Referencia al servicio de ArcGIS
    private final ObjectMapper mapper = new ObjectMapper();

    // 🧠 MEMORIA: Guardamos la última posición conocida de cada bus (Key: busId)
    private final Map<Integer, BusPosition> lastPositions = new HashMap<>();

    // Constructor actualizado para recibir RouteService
    public ExternalBusWebSocketClient(URI serverUri, InternalBusWebSocketHandler handler, RouteService routeService) {
        super(serverUri);
        this.internalHandler = handler;
        this.routeService = routeService;
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        System.out.println("🟢 Conectado a WS Externo de Empleados");
    }

    @Override
    public void onMessage(String message) {
        try {
            BusPosition currentRawPosition = parseBusPosition(message);

            if (currentRawPosition != null) {
                String jsonToSend;
                BusPosition lastPosition = lastPositions.get(currentRawPosition.getBusId());

                if (lastPosition != null) {
                    // Calculamos la ruta "pegada a la calle"
                    String matchedJson = routeService.getMatchedRoute(lastPosition, currentRawPosition);

                    if (matchedJson != null) {
                        jsonToSend = matchedJson;
                    } else {
                        // Fallback: Si falla ArcGIS, enviamos punto simple
                        jsonToSend = createSimplePointJson(currentRawPosition);
                    }
                } else {
                    // Primer punto conocido
                    jsonToSend = createSimplePointJson(currentRawPosition);
                }

                lastPositions.put(currentRawPosition.getBusId(), currentRawPosition);
                internalHandler.broadcastToFrontend(jsonToSend);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Helper para crear el JSON simple cuando no hay ruta calculada
    private String createSimplePointJson(BusPosition pos) {
        try {
            // { "busId": 123, "type": "point", "path": [[lon, lat]], "lastLat": ..., "lastLon": ... }
            var node = mapper.createObjectNode();
            node.put("busId", pos.getBusId());
            node.put("type", "point");

            var pathArray = mapper.createArrayNode();
            var pointArray = mapper.createArrayNode();
            pointArray.add(pos.getLon());
            pointArray.add(pos.getLat());
            pathArray.add(pointArray);

            node.set("path", pathArray);
            node.put("lastLat", pos.getLat());
            node.put("lastLon", pos.getLon());
            return mapper.writeValueAsString(node);
        } catch (Exception e) { return null; }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("🔴 WS Externo cerrado: " + reason);
    }

    @Override
    public void onError(Exception ex) {
        System.err.println("❌ Error en WS: " + ex.getMessage());
    }

    /**
     * Extrae el BusPosition del JSON complejo de EmployeeResponse
     */
    private BusPosition parseBusPosition(String json) {
        try {
            JsonNode root = mapper.readTree(json);
            JsonNode busNode = root.at("/cities/0/routes/0/buses/0");

            if (busNode.isMissingNode()) return null;
            if (busNode.get("busId") == null) return null;

            return new BusPosition(
                    busNode.get("busId").asInt(),
                    busNode.get("latitude").asDouble(),
                    busNode.get("longitude").asDouble()
            );
        } catch (Exception e) {
            return null;
        }
    }
}