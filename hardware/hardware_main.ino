#include <WiFi.h>
#include <WebSocketsServer.h>
#include <Wire.h>
#include <Adafruit_PN532.h>

const char* ssid = "CONTROLADORA";
const char* password = "senha123";
const char* totem_ssid = "totem";
const char* totem_password = "totem123";
WebSocketsServer webSocket = WebSocketsServer(81); // Porta WebSocket

#define RELE_PIN 2
#define RELE_PIN2 4
#define RELE_PIN3 5
#define RELE_PIN4 18

#define PINO_SDA 21
#define PINO_SCL 22

Adafruit_PN532 nfc(PINO_SDA, PINO_SCL);
String lastUID = ""; // Variável para armazenar o UID anterior lido
String currentUID = ""; // Variável para armazenar o UID atual

unsigned long previousMillis = 0;
const long interval = 6000; // Intervalo de tempo em milissegundos entre as leituras do cartão RFID

void acionarRele(int pin) {
  digitalWrite(pin, LOW);
  delay(30000);
  digitalWrite(pin, HIGH);
}

void webSocketEvent(uint8_t num, WStype_t type, uint8_t * payload, size_t length) {
  switch(type) {
    case WStype_TEXT:
      Serial.printf("[%u] Text: %s\n", num, payload);
      if (strcmp((char*)payload, "ativar 1") == 0) {
        acionarRele(RELE_PIN);
      } else if (strcmp((char*)payload, "ativar 2") == 0) {
        acionarRele(RELE_PIN2);
      } else if (strcmp((char*)payload, "ativar 3") == 0) {
        acionarRele(RELE_PIN3);
      } else if (strcmp((char*)payload, "ativar 4") == 0) {
        acionarRele(RELE_PIN4);
      }
      break;
    default:
      break;
  }
}

void setup() {
  Serial.begin(115200);

  pinMode(RELE_PIN, OUTPUT);
  pinMode(RELE_PIN2, OUTPUT);
  pinMode(RELE_PIN3, OUTPUT);
  pinMode(RELE_PIN4, OUTPUT);

  digitalWrite(RELE_PIN, HIGH);
  digitalWrite(RELE_PIN2, HIGH);
  digitalWrite(RELE_PIN3, HIGH);
  digitalWrite(RELE_PIN4, HIGH);

  // Conectar-se à rede totem
  WiFi.begin(totem_ssid, totem_password);
  while (WiFi.status() != WL_CONNECTED) {
    delay(1000);
    Serial.println("Conectando à rede totem...");
  }

  Serial.println("Conectado à rede totem");

  IPAddress IP = WiFi.localIP();
  Serial.print("Endereço IP na rede totem: ");
  Serial.println(IP);

  // Configurar o ponto de acesso
  WiFi.softAP(ssid, password);
  IPAddress AP_IP = WiFi.softAPIP();
  Serial.print("Endereço IP do AP: ");
  Serial.println(AP_IP);

  webSocket.begin();
  webSocket.onEvent(webSocketEvent);

  nfc.begin();
  nfc.SAMConfig(); // Configura o PN532 para aguardar por um cartão RFID
}

void loop() {
  webSocket.loop();

  unsigned long currentMillis = millis();

  if (currentMillis - previousMillis >= interval) {
    // Salva o último tempo de leitura
    previousMillis = currentMillis;

    // Verifica se há um cartão RFID
    uint8_t success;
    uint8_t uid[] = { 0, 0, 0, 0, 0, 0, 0 };  // Buffer para armazenar o UID lido
    uint8_t uidLength;                        // Comprimento do UID (4 ou 7 bytes dependendo do tipo de cartão)
    success = nfc.readPassiveTargetID(PN532_MIFARE_ISO14443A, uid, &uidLength, 500);
    
    if (success) {
      // Se um cartão foi encontrado, converte o UID para uma string
      currentUID = "";
      for (uint8_t i=0; i < uidLength; i++) {
        currentUID += String(uid[i], HEX);
      }

      // Verifica se o UID atual é diferente do UID anterior ou se é a primeira leitura
      if (currentUID != lastUID || lastUID == "") {
        // Envia o UID para todos os clientes conectados via WebSocket
        webSocket.broadcastTXT("inserido:"+currentUID);

        // Imprime o UID no Serial Monitor
        Serial.print("inserido:");
        Serial.println(currentUID);

        // Atualiza o último UID lido
        lastUID = currentUID;
      }
    } else {
      // Se não foi encontrado um cartão RFID, verifica se o último UID lido foi diferente de vazio
      if (lastUID != "") {
        // Envia uma mensagem indicando que o cartão foi removido para todos os clientes conectados via WebSocket
        webSocket.broadcastTXT("removido:"+lastUID);

        // Imprime no Serial Monitor que o cartão foi removido
        Serial.println("removido:"+lastUID);

        // Limpa o último UID lido
        lastUID = "";
      }
    }
  }
}
