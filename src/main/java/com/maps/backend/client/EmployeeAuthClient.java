package com.maps.backend.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maps.backend.utils.HttpClientUtil;
import org.springframework.stereotype.Component;

@Component
public class EmployeeAuthClient {

    private final ObjectMapper mapper = new ObjectMapper();
    // ✅ URL de LOGIN desplegada
    private static final String LOGIN_URL = "https://api-dev.enlaparadave.com/api/v2/auth/login/signin";
    // Credenciales para el rol EMPLOYEE
    private static final String LOGIN_BODY = "{\"email\":\"employee-enlaparada@yopmail.com\", \"password\":\"amngt1994\"}";

    /**
     * Realiza el login en el servidor desplegado y devuelve el accessToken.
     */
    public String getAccessToken() {
        try {
            String response = HttpClientUtil.post(LOGIN_URL, LOGIN_BODY);

            JsonNode root = mapper.readTree(response);
            JsonNode tokenNode = root.get("accessToken");

            if (tokenNode == null) {
                // Si no hay token, el login falló (ej. credenciales inválidas)
                System.err.println("⛔ Login fallido. Respuesta no contiene 'accessToken'.");

                // Si el servidor devuelve un campo 'error', lo mostramos
                JsonNode errorNode = root.get("error");
                if (errorNode != null) {
                    System.err.println("⛔ MENSAJE DE ERROR DEL SERVIDOR: " + errorNode.asText());
                }
                return null;
            }

            String token = tokenNode.asText();
            System.out.println("🔑 Login de Empleado exitoso. Token obtenido (longitud: " + token.length() + ")");
            return token;

        } catch (Exception e) {
            System.err.println("❌ Error de red o serialización al intentar el login desplegado: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}