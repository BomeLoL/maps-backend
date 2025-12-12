package com.maps.backend.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.maps.backend.model.BusPosition;
import com.maps.backend.service.RouteService;
import com.maps.backend.websocket.InternalBusWebSocketHandler;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class ExternalBusWebSocketClient extends WebSocketClient {

    private final InternalBusWebSocketHandler internalHandler;
    private final RouteService routeService;
    private final ObjectMapper mapper = new ObjectMapper();

    // Memoria de la última posición reportada de cada bus
    private final Map<Integer, BusPosition> lastPositions = new HashMap<>();

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
            JsonNode root = mapper.readTree(message);
            JsonNode citiesNode = root.get("cities");

            // 1. Recorrer jerarquía completa para extraer Datos de Ciudad
            if (citiesNode != null && citiesNode.isArray()) {
                for (JsonNode city : citiesNode) {
                    // Extraemos ID y Nombre de la ciudad
                    int citId = city.get("citId").asInt();
                    String citName = city.get("citName").asText();

                    JsonNode routes = city.get("routes");
                    if (routes != null && routes.isArray()) {
                        for (JsonNode route : routes) {
                            JsonNode buses = route.get("buses");
                            if (buses != null && buses.isArray()) {
                                for (JsonNode bus : buses) {
                                    // Procesar cada bus pasando los datos de su ciudad
                                    processBus(bus, citId, citName);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ Error parseando JSON: " + e.getMessage());
        }
    }

    /**
     * Lógica principal de filtrado y optimización
     */
    private void processBus(JsonNode busNode, int citId, String citName) {
        try {

            int busId = busNode.get("busId").asInt();

            // 🧪 TRAMPA PARA TESTING RECORDAR BORRAR🧪
            // Si es el bus 301, le cambiamos la ciudad a la fuerza
            if (busId == 301) {
                citId = 2;              // ID Nuevo
                citName = "Valencia";   // Nombre Nuevo
            }
            // -------------------------

            BusPosition currentPos = new BusPosition(
                    busNode.get("busId").asInt(),
                    busNode.get("latitude").asDouble(),
                    busNode.get("longitude").asDouble(),
                    citId,
                    citName
            );

            String jsonToSend = null;
            BusPosition lastPos = lastPositions.get(currentPos.getBusId());

            if (lastPos != null) {
                // Calcular distancia
                double dist = calculateDistance(
                        lastPos.getLat(), lastPos.getLon(),
                        currentPos.getLat(), currentPos.getLon()
                );

                // 🛑 NIVEL 1: ZONA MUERTA (Silencio Total)
                if (dist < 3.0) {
                    return; // Si no se mueve, NO envía nada.
                }

                // 🚀 NIVEL 2: RUTA OSRM (> 30m)
                if (dist > 30.0) {
                    String matchedJson = routeService.getMatchedRoute(lastPos, currentPos);

                    if (matchedJson != null) {
                        jsonToSend = enrichJsonWithCityData(matchedJson, currentPos);
                    } else {
                        jsonToSend = createSimplePointJson(currentPos);
                    }
                    // Actualizamos memoria
                    lastPositions.put(currentPos.getBusId(), currentPos);
                }
                // 🚶 NIVEL 3: MOVIMIENTO LIGERO (Entre 3m y 30m)
                else {
                    jsonToSend = createSimplePointJson(currentPos);

                    // 🔥 CORRECCIÓN AQUÍ 🔥
                    // Debemos actualizar la posición para que, si el bus se para en el siguiente segundo,
                    // la distancia sea 0 y entre en el Nivel 1 (Silencio).
                    lastPositions.put(currentPos.getBusId(), currentPos);
                }

            } else {
                // Primera vez
                jsonToSend = createSimplePointJson(currentPos);
                lastPositions.put(currentPos.getBusId(), currentPos);
            }

            if (jsonToSend != null) {
                internalHandler.broadcastToFrontend(jsonToSend);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- MÉTODOS AUXILIARES ---

    // Fórmula de Haversine para distancia en metros
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radio de la Tierra en km
        double latDist = Math.toRadians(lat2 - lat1);
        double lonDist = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDist / 2) * Math.sin(latDist / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDist / 2) * Math.sin(lonDist / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c * 1000; // Convertir a metros
    }

    // Crea el JSON simple (tipo "point") incluyendo datos de ciudad
    private String createSimplePointJson(BusPosition pos) {
        try {
            ObjectNode node = mapper.createObjectNode();
            node.put("busId", pos.getBusId());
            node.put("citId", pos.getCitId());      // ✅ ID Ciudad
            node.put("citName", pos.getCitName());  // ✅ Nombre Ciudad
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

    // Inyecta los datos de ciudad en el JSON que devuelve OSRM
    private String enrichJsonWithCityData(String jsonString, BusPosition pos) {
        try {
            ObjectNode node = (ObjectNode) mapper.readTree(jsonString);
            node.put("citId", pos.getCitId());
            node.put("citName", pos.getCitName());
            return mapper.writeValueAsString(node);
        } catch (Exception e) { return jsonString; }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("🔴 WS Externo cerrado: " + reason);
    }

    @Override
    public void onError(Exception ex) {
        System.err.println("❌ Error en WS: " + ex.getMessage());
    }
}