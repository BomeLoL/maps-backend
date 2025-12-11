package com.maps.backend.websocket;

import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class InternalBusWebSocketHandler extends TextWebSocketHandler {

    private static final Set<WebSocketSession> sessions = new CopyOnWriteArraySet<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        System.out.println("🟢 Cliente conectado: " + session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        System.out.println("🔴 Cliente desconectado: " + session.getId());
    }

    public void broadcastToFrontend(String json) {

         System.out.println("📤 ENVIANDO AL FRONTEND → " + json);

        for (WebSocketSession session : sessions) {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(json));
                } else {
                    sessions.remove(session); // Limpieza automática
                }
            } catch (Exception e) {
                sessions.remove(session);
                e.printStackTrace();
            }
        }
    }
}
