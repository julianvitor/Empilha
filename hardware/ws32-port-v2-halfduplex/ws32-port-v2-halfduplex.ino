#include <Arduino.h>
#include <ETH.h>
#include <WiFi.h>
#include <PN532_I2C.h>
#include <PN532.h>
#include <WebSocketsServer.h>

// Reles e RFID
#define RELE_PIN 2
#define RELE_PIN2 4

// I2C
#define I2C_SCL 32
#define I2C_SDA 33

// Ethernet
#define ETH_ADDR        1
#define ETH_POWER_PIN   16
#define ETH_POWER_PIN_ALTERNATIVE 16
#define ETH_MDC_PIN    23
#define ETH_MDIO_PIN   18
#define ETH_TYPE       ETH_PHY_LAN8720
#define ETH_CLK_MODE   ETH_CLOCK_GPIO17_OUT

unsigned long previousMillis = 0;
const long interval = 9000;

PN532_I2C pn532_i2c(Wire);
PN532 nfc(pn532_i2c);

WebSocketsServer webSocket = WebSocketsServer(81);

static bool eth_connected = false;


String lastUID = "";
String currentUID = "";

bool releState = false;

void WiFiEvent(WiFiEvent_t event) {
  switch (event) {
    case SYSTEM_EVENT_ETH_START:
      //Serial.println("ETH Started");
      ETH.setHostname("esp32-ethernet"); // Set ETH hostname
      break;
    case SYSTEM_EVENT_ETH_CONNECTED:
      //Serial.println("ETH Connected");
      break;
    case SYSTEM_EVENT_ETH_GOT_IP:
      //Serial.print("ETH MAC: ");
      //Serial.print(ETH.macAddress());
      //Serial.print(", IPv4: ");
      //Serial.print(ETH.localIP());
      if (ETH.fullDuplex()) {
        //Serial.print(", FULL_DUPLEX");
      }
      //Serial.print(", ");
      //Serial.print(ETH.linkSpeed());
      //Serial.println("Mbps");
      eth_connected = true;
      break;
    case SYSTEM_EVENT_ETH_DISCONNECTED:
      //Serial.println("ETH Disconnected");
      eth_connected = false;
      break;
    case SYSTEM_EVENT_ETH_STOP:
      //Serial.println("ETH Stopped");
      eth_connected = false;
      break;
    default:
      break;
  }
}

bool transmittingData = false; // Flag para indicar se está transmitindo dados

void webSocketEvent(uint8_t num, WStype_t type, uint8_t * payload, size_t length) {
    switch(type) {
      case WStype_TEXT:
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


void acionarRele(int pin) {
  digitalWrite(pin, LOW);
  releState = true;
  delay(15000); // Manter o relé ativado por 20 segundos
  digitalWrite(pin, HIGH);
  releState = false;
}
void setup() {
  pinMode(ETH_POWER_PIN_ALTERNATIVE, OUTPUT);
  digitalWrite(ETH_POWER_PIN_ALTERNATIVE, HIGH);

  Serial.begin(115200);
  
  Wire.begin(I2C_SDA, I2C_SCL);
  nfc.begin();
  
  pinMode(RELE_PIN, OUTPUT);
  pinMode(RELE_PIN2, OUTPUT);
  digitalWrite(RELE_PIN, HIGH);
  digitalWrite(RELE_PIN2, HIGH);

  uint32_t versiondata = nfc.getFirmwareVersion();
  if (!versiondata) {
    //Serial.println("Não foi possível encontrar o PN53x. Certifique-se de que está conectado corretamente.");
  } else {
    nfc.SAMConfig();
    //Serial.println("ESP32 PN532 Iniciado!");
    //Serial.println(versiondata);
  }
  btStop();
  
  WiFi.onEvent(WiFiEvent);
  ETH.begin(ETH_ADDR, ETH_POWER_PIN, ETH_MDC_PIN, ETH_MDIO_PIN, ETH_TYPE, ETH_CLK_MODE);
  webSocket.begin();
  webSocket.onEvent(webSocketEvent);
}


void loop() {
  webSocket.loop();
  unsigned long currentMillis = millis();

  if (!transmittingData && currentMillis - previousMillis >= interval) {
      previousMillis = currentMillis;

      uint8_t success;
      uint8_t uid[] = { 0, 0, 0, 0, 0, 0, 0 };
      uint8_t uidLength;
      
      // Desativa a leitura do cartão RFID durante a transmissão de dados
      transmittingData = true;
      success = nfc.readPassiveTargetID(PN532_MIFARE_ISO14443A, uid, &uidLength, 500);
      transmittingData = false;

      if (success) {
        currentUID = "";
        for (uint8_t i=0; i < uidLength; i++) {
          currentUID += String(uid[i], HEX);
        }

        if (currentUID != lastUID || lastUID == "") {
          webSocket.broadcastTXT("inserido:"+currentUID);
          lastUID = currentUID;
        }
      } else {
        if (lastUID != "") {
          webSocket.broadcastTXT("removido:"+lastUID);
          lastUID = "";
        }
      }
    }
}
