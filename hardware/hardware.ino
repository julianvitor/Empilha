#include <WiFi.h>
#include <WebSocketsServer.h>

const char* ssid = "ESP32AP";
const char* password = "senha123";
WebSocketsServer webSocket = WebSocketsServer(81); // Porta WebSocket

void webSocketEvent(uint8_t num, WStype_t type, uint8_t * payload, size_t length) {}

void setup() {
  Serial.begin(9600);

  // Cria uma rede WiFi
  WiFi.softAP(ssid, password);
  IPAddress IP = WiFi.softAPIP();
  Serial.print("Endereço IP do AP: ");
  Serial.println(IP);

  // Inicia o servidor WebSocket
  webSocket.begin();
  webSocket.onEvent(webSocketEvent);
}

void loop() {
  webSocket.loop();

  // Envia mensagem "Hello, World!" para todos os clientes conectados a cada 5 segundos
  static unsigned long lastMillis = 0;
  if (millis() - lastMillis > 5000) {
    webSocket.broadcastTXT("Hello, World!");
    lastMillis = millis();
  }
}
