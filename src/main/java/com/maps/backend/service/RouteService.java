package com.maps.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.maps.backend.model.BusPosition;
import com.maps.backend.utils.HttpClientUtil;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class RouteService {

    private final ObjectMapper mapper = new ObjectMapper();

    public String getMatchedRoute(BusPosition p1, BusPosition p2) {
        try {
            // Usamos OSRM (Open Source Routing Machine) - Servicio público gratuito
            // Formato: /route/v1/driving/{lon1},{lat1};{lon2},{lat2}?overview=full&geometries=geojson

            String url = String.format(Locale.US,
                    "http://router.project-osrm.org/route/v1/driving/%f,%f;%f,%f?overview=full&geometries=geojson",
                    p1.getLon(), p1.getLat(), p2.getLon(), p2.getLat());

            // Hacemos GET en lugar de POST (HttpClientUtil.get no lo tienes, usa un simple GET)
            // Si no tienes un método GET, usa el POST pero OSRM prefiere GET.
            // Para simplificar, asumiremos que puedes implementar un GET simple o usar Java nativo.

            // ⚠️ IMPORTANTE: OSRM usa GET. Tienes que añadir un método GET a tu HttpClientUtil
            // O usar esta implementación rápida con java.net.http:

            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(url))
                    .GET()
                    .build();

            java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.err.println("❌ Error OSRM: " + response.statusCode());
                return null;
            }

            JsonNode root = mapper.readTree(response.body());
            JsonNode coordinates = root.at("/routes/0/geometry/coordinates");

            if (coordinates.isMissingNode() || !coordinates.isArray()) {
                return null;
            }

            ObjectNode resultJson = mapper.createObjectNode();
            resultJson.put("busId", p2.getBusId());
            resultJson.put("type", "segment");
            resultJson.set("path", coordinates); // OSRM ya devuelve [[lon, lat], ...] igual que queríamos

            JsonNode lastPoint = coordinates.get(coordinates.size() - 1);
            resultJson.put("lastLat", lastPoint.get(1).asDouble());
            resultJson.put("lastLon", lastPoint.get(0).asDouble());

            return mapper.writeValueAsString(resultJson);

        } catch (Exception e) {
            // e.printStackTrace();
            return null;
        }
    }
}