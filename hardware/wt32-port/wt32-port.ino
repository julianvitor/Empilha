#include <ETH.h> // quote to use ETH
#include <WiFi.h>
#include <PN532_I2C.h>
#include <PN532.h>
#include <WebServer.h> // Introduce corresponding libraries
#include <WebSocketsServer.h>
//rfid
#include <Wire.h>

//ethernet e websocket
#include <Arduino.h>

//reles e rfid
#define RELE_PIN 5
#define RELE_PIN2 4

#define I2C_SCL 32    // WT32-ETH01 CFG    = Gpio 32      non standard i2c adress 
#define I2C_SDA 33    // WT32-ETH01 485_EN = Gpio 33      non standard i2c adress
 
//ethernet
#define ETH_ADDR        1
#define ETH_POWER_PIN   16//-1 //16 // Do not use it, it can cause conflict during the software reset.
#define ETH_POWER_PIN_ALTERNATIVE 16 //17
#define ETH_MDC_PIN    23
#define ETH_MDIO_PIN   18
#define ETH_TYPE       ETH_PHY_LAN8720
#define ETH_CLK_MODE    ETH_CLOCK_GPIO17_OUT // ETH_CLOCK_GPIO0_IN

PN532_I2C pn532_i2c(Wire);
PN532 nfc(pn532_i2c);

String lastUID = ""; // Variável para armazenar o UID anterior lido
String currentUID = ""; // Variável para armazenar o UID atual

unsigned long previousMillis = 0;
const long interval = 6000; // Intervalo de tempo em milissegundos entre as leituras do cartão RFID
const unsigned long releDuration = 20000; // Tempo em milissegundos que o relé ficará acionado

bool releState = false; // Estado atual do relé
unsigned long releStartTime = 0; // Tempo de início da ativação do relé

WebSocketsServer webSocket = WebSocketsServer(81); // WebSocket server on port 81
static bool eth_connected = false;

void acionarRele(int pin) {
  digitalWrite(pin, LOW);
  releState = true;
  releStartTime = millis();
}
void atualizarRele() {
  if (releState && millis() - releStartTime >= releDuration) {
    digitalWrite(RELE_PIN, HIGH);
    digitalWrite(RELE_PIN2, HIGH);
    releState = false;
  }
}

void WiFiEvent(WiFiEvent_t event) {
  switch (event) {
    case SYSTEM_EVENT_ETH_START:
      Serial.println("ETH Started");
      //set eth hostname here
      ETH.setHostname("esp32-ethernet");
      break;
    case SYSTEM_EVENT_ETH_CONNECTED:
      Serial.println("ETH Connected");
      break;
    case SYSTEM_EVENT_ETH_GOT_IP:
      Serial.print("ETH MAC: ");
      Serial.print(ETH.macAddress());
      Serial.print(", IPv4: ");
      Serial.print(ETH.localIP());
      if (ETH.fullDuplex()) {
        Serial.print(", FULL_DUPLEX");
      }
      Serial.print(", ");
      Serial.print(ETH.linkSpeed());
      Serial.println("Mbps");
      eth_connected = true;
      break;
    case SYSTEM_EVENT_ETH_DISCONNECTED:
      Serial.println("ETH Disconnected");
      eth_connected = false;
      break;
    case SYSTEM_EVENT_ETH_STOP:
      Serial.println("ETH Stopped");
      eth_connected = false;
      break;
    default:
      break;
  }
}

void webSocketEvent(uint8_t num, WStype_t type, uint8_t * payload, size_t length) {
  switch(type) {
    case WStype_TEXT:
      Serial.printf("[%u] Text: %s\n", num, payload);
      webSocket.sendTXT(num, payload);
      if (strcmp((char*)payload, "ativar 1") == 0) {
        acionarRele(RELE_PIN);
      } 
      else if (strcmp((char*)payload, "ativar 2") == 0) {
        acionarRele(RELE_PIN2);
      } 
      break;
    default:
      break;
  }
}

void setup() {
  pinMode(ETH_POWER_PIN_ALTERNATIVE, OUTPUT);
  digitalWrite(ETH_POWER_PIN_ALTERNATIVE, HIGH);

  Serial.begin(115200);
  Serial.println("Apos o serial begin");

  pinMode(RELE_PIN, OUTPUT);
  pinMode(RELE_PIN2, OUTPUT);
  digitalWrite(RELE_PIN, HIGH);
  digitalWrite(RELE_PIN2, HIGH);
                                // start i2c on non stndard i2c pins
  Wire.begin(I2C_SDA, I2C_SCL);                               // start i2c on non stndard i2c pins
  nfc.begin();
  Serial.println("depois do nfc.begin");

  Serial.println("antes do wifievent");

  WiFi.onEvent(WiFiEvent);
  ETH.begin(ETH_ADDR, ETH_POWER_PIN, ETH_MDC_PIN, ETH_MDIO_PIN, ETH_TYPE, ETH_CLK_MODE);
  
  webSocket.begin();
  webSocket.onEvent(webSocketEvent);
  Serial.println("antes do nfc begin com delay");
  delay(3000);



  uint32_t versiondata = nfc.getFirmwareVersion();
  if (!versiondata) {
    Serial.println("Não foi possível encontrar o PN53x. Certifique-se de que está conectado corretamente.");
  } else {
    // Configuração do PN532 para aguardar tags por até 20 centímetros (esse valor pode variar dependendo do tipo de tag)
    nfc.SAMConfig();
    Serial.println("ESP32 PN532 Iniciado!");
    Serial.println(versiondata);
  }
  // Desativar Bluetooth
  btStop();
  Serial.println("fim do setup");
}

void loop() {
  webSocket.loop();
  //atualizarRele();
  Serial.println("antes do current millis");
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
