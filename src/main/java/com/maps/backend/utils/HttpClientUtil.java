package com.maps.backend.utils;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.StringEntity;

public class HttpClientUtil {

    public static String post(String url, String body) throws Exception {

        try (CloseableHttpClient client = HttpClients.createDefault()) {

            HttpPost post = new HttpPost(url);
            post.setEntity(new StringEntity(body));

            // 🔴 ANTES (ERROR): Le decías al server que era un formulario
            // post.setHeader("Content-Type", "application/x-www-form-urlencoded");

            // 🟢 AHORA (CORRECTO): Le dices que es JSON, igual que en Postman
            post.setHeader("Content-Type", "application/json");

            return client.execute(post, httpResponse ->
                    new String(httpResponse.getEntity().getContent().readAllBytes())
            );
        }
    }
}