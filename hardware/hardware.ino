#include <WiFi.h>
#include <WebSocketsServer.h>
#include <Wire.h>
#include <Adafruit_PN532.h>

const char* ssid = "ESP32AP";
const char* password = "senha123";
WebSocketsServer webSocket = WebSocketsServer(81); // Porta WebSocket

#define PINO_SDA 21
#define PINO_SCL 22
#define RELE_PIN 2
Adafruit_PN532 nfc(PINO_SDA, PINO_SCL);

unsigned long lastRFIDReadTime = 0;
const unsigned long RFIDReadInterval = 2000; // Intervalo de 2 segundos entre as leituras de RFID

void webSocketEvent(uint8_t num, WStype_t type, uint8_t * payload, size_t length) {
  switch(type) {
    case WStype_TEXT:
      Serial.printf("[%u] Mensagem: %s\n", num, payload);

      // Verifica se a mensagem recebida é "ativar 1"
      if (strcmp((char*)payload, "ativar 1") == 0) {
        // Ativa o relé
        digitalWrite(RELE_PIN, LOW);
        Serial.println("Relé ativado");

        // Espera 2 segundos
        delay(2000);

        // Desativa o relé
        digitalWrite(RELE_PIN, HIGH);
        Serial.println("Relé desativado");
      }
      break;
  }
}

void setup() {
  Serial.begin(115200);

  // Cria uma rede WiFi
  WiFi.softAP(ssid, password);
  IPAddress IP = WiFi.softAPIP();
  Serial.print("Endereço IP do AP: ");
  Serial.println(IP);

  // Inicia o servidor WebSocket
  webSocket.begin();
  webSocket.onEvent(webSocketEvent);

  // Inicia o relé
  pinMode(RELE_PIN, OUTPUT);
  digitalWrite(RELE_PIN, HIGH); // Desliga o relé inicialmente

  // Inicia o módulo NFC
  nfc.begin();
  nfc.SAMConfig(); // Configura o PN532 para aguardar por um cartão RFID
}

void loop() {
  webSocket.loop(); // Processa eventos WebSocket

  // Verifica se é hora de fazer uma nova leitura de RFID
  unsigned long currentMillis = millis();
  if (currentMillis - lastRFIDReadTime >= RFIDReadInterval) {
    // Atualiza o tempo da última leitura de RFID
    lastRFIDReadTime = currentMillis;

    // Verifica se há um cartão RFID
    uint8_t success;
    uint8_t uid[] = { 0, 0, 0, 0, 0, 0, 0 };  // Buffer para armazenar o UID lido
    uint8_t uidLength;                        // Comprimento do UID (4 ou 7 bytes dependendo do tipo de cartão)
    success = nfc.readPassiveTargetID(PN532_MIFARE_ISO14443A, uid, &uidLength, 500);
    
    if (success) {
      // Se um cartão foi encontrado, converte o UID para uma string
      String uidString = "";
      for (uint8_t i=0; i < uidLength; i++) {
        uidString += String(uid[i], HEX);
      }

      // Envia o UID para todos os clientes conectados
      webSocket.broadcastTXT(uidString);

      // Imprime o UID no Serial Monitor
      Serial.print("UID do cartão RFID: ");
      Serial.println(uidString);
    }
  }
}
