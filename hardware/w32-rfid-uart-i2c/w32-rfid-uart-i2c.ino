#include <Arduino.h>
#include <ETH.h>
#include <WiFi.h>
#include <PN532_I2C.h>
#include <PN532_HSU.h>
#include <PN532.h>
#include <WebSocketsServer.h>
#include <Wire.h>

// Pinos dos relés
#define RELE_PIN 2
#define RELE_PIN2 4

// Pinos I2C
#define I2C_SCL 32
#define I2C_SDA 33

// Pinos UART para PN532
#define UART_RX_PIN 35
#define UART_TX_PIN 17

// Ethernet
#define ETH_ADDR        1
#define ETH_POWER_PIN_ALTERNATIVE 16
#define ETH_MDC_PIN    23
#define ETH_MDIO_PIN   18
#define ETH_TYPE       ETH_PHY_LAN8720
#define ETH_CLK_MODE   ETH_CLOCK_GPIO17_OUT

unsigned long previousMillisRFID_I2C = 0;
unsigned long previousMillisRFID_UART = 0;
const long intervalRFID_I2C = 5000;
const long intervalRFID_UART = 5000;
const long releDuration = 20000; // Tempo de ativação do relé em milissegundos

// Inicializa a comunicação I2C para o PN532
PN532_I2C pn532_i2c(Wire);
PN532 nfc_i2c(pn532_i2c);

// Inicializa a comunicação UART para o PN532
PN532_HSU pn532_hsu(Serial2);
PN532 nfc_uart(pn532_hsu);

WebSocketsServer webSocket = WebSocketsServer(81);

static bool eth_connected = false;

String lastUID_I2C = "";
String currentUID_I2C = "";
String lastUID_UART = "";
String currentUID_UART = "";

bool releAtivo = false;
unsigned long tempoInicioAtivacao = 0;

void WiFiEvent(WiFiEvent_t event) {
  switch (event) {
    case SYSTEM_EVENT_ETH_START:
      ETH.setHostname("esp32-ethernet"); // Define o hostname do ETH
      break;
    case SYSTEM_EVENT_ETH_CONNECTED:
      break;
    case SYSTEM_EVENT_ETH_GOT_IP:
      eth_connected = true;
      break;
    case SYSTEM_EVENT_ETH_DISCONNECTED:
      eth_connected = false;
      break;
    case SYSTEM_EVENT_ETH_STOP:
      eth_connected = false;
      break;
    default:
      break;
  }
}

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

void readRFID_I2C() {
  uint8_t success;
  uint8_t uid[] = { 0, 0, 0, 0, 0, 0, 0 };  // Buffer para armazenar o UID lido
  uint8_t uidLength;                        // Comprimento do UID (4 ou 7 bytes dependendo do tipo de cartão)
  success = nfc_i2c.readPassiveTargetID(PN532_MIFARE_ISO14443A, uid, &uidLength, 100);
  
  if (success) {
    currentUID_I2C = "";
    for (uint8_t i = 0; i < uidLength; i++) {
      currentUID_I2C += String(uid[i], HEX);
    }

    if (currentUID_I2C != lastUID_I2C || lastUID_I2C == "") {
      webSocket.broadcastTXT("I2C inserido:" + currentUID_I2C);
      lastUID_I2C = currentUID_I2C;
    }
  } else {
    if (lastUID_I2C != "") {
      webSocket.broadcastTXT("I2C removido:" + lastUID_I2C);
      lastUID_I2C = "";
    }
  }
}

void readRFID_UART() {
  uint8_t success;
  uint8_t uid[] = { 0, 0, 0, 0, 0, 0, 0 };  // Buffer para armazenar o UID lido
  uint8_t uidLength;                        // Comprimento do UID (4 ou 7 bytes dependendo do tipo de cartão)
  success = nfc_uart.readPassiveTargetID(PN532_MIFARE_ISO14443A, uid, &uidLength, 100);
  
  if (success) {
    currentUID_UART = "";
    for (uint8_t i = 0; i < uidLength; i++) {
      currentUID_UART += String(uid[i], HEX);
    }

    if (currentUID_UART != lastUID_UART || lastUID_UART == "") {
      webSocket.broadcastTXT("UART inserido:" + currentUID_UART);
      lastUID_UART = currentUID_UART;
    }
  } else {
    if (lastUID_UART != "") {
      webSocket.broadcastTXT("UART removido:" + lastUID_UART);
      lastUID_UART = "";
    }
  }
}

void acionarRele(int pin) {
  digitalWrite(pin, LOW);
  releAtivo = true;
  tempoInicioAtivacao = millis(); // Salva o tempo de início da ativação
}

void setup() {
  pinMode(RELE_PIN, OUTPUT);
  pinMode(RELE_PIN2, OUTPUT);
  pinMode(ETH_POWER_PIN_ALTERNATIVE, OUTPUT);

  digitalWrite(RELE_PIN, LOW);
  digitalWrite(RELE_PIN2, LOW);
  delay(2000);
  digitalWrite(RELE_PIN, HIGH);
  digitalWrite(RELE_PIN2, HIGH);

  digitalWrite(ETH_POWER_PIN_ALTERNATIVE, HIGH);

  Serial.begin(115200);
  Serial2.begin(115200, SERIAL_8N1, UART_RX_PIN, UART_TX_PIN); // Inicializa a comunicação UART com o PN532
  
  Wire.begin(I2C_SDA, I2C_SCL);
  nfc_i2c.begin();
  nfc_uart.begin();

  uint32_t versiondata_i2c = nfc_i2c.getFirmwareVersion();
  if (!versiondata_i2c) {
    Serial.println("Não foi possível encontrar o PN53x via I2C. Certifique-se de que está conectado corretamente.");
  } else {
    nfc_i2c.SAMConfig();
    Serial.println("ESP32 PN532 I2C Iniciado!");
    Serial.println(versiondata_i2c);
  }

  uint32_t versiondata_uart = nfc_uart.getFirmwareVersion();
  if (!versiondata_uart) {
    Serial.println("Não foi possível encontrar o PN53x via UART. Certifique-se de que está conectado corretamente.");
  } else {
    nfc_uart.SAMConfig();
    Serial.println("ESP32 PN532 UART Iniciado!");
    Serial.println(versiondata_uart);
  }

  btStop();
  
  WiFi.onEvent(WiFiEvent);
  ETH.begin(ETH_ADDR, ETH_POWER_PIN_ALTERNATIVE, ETH_MDC_PIN, ETH_MDIO_PIN, ETH_TYPE, ETH_CLK_MODE);
  webSocket.begin();
  webSocket.onEvent(webSocketEvent);
}

void loop() {
  if (!eth_connected) {
    ETH.begin(ETH_ADDR, ETH_POWER_PIN_ALTERNATIVE, ETH_MDC_PIN, ETH_MDIO_PIN, ETH_TYPE, ETH_CLK_MODE);
  }
  
  webSocket.loop();
  
  unsigned long currentMillis = millis();
  if (currentMillis - previousMillisRFID_I2C >= intervalRFID_I2C) {
    previousMillisRFID_I2C = currentMillis;
    readRFID_I2C();
  }

  if (currentMillis - previousMillisRFID_UART >= intervalRFID_UART) {
    previousMillisRFID_UART = currentMillis;
    readRFID_UART();
  }

  if (releAtivo && (currentMillis - tempoInicioAtivacao >= releDuration)) {
    digitalWrite(RELE_PIN, HIGH);
    digitalWrite(RELE_PIN2, HIGH);
    releAtivo = false;
  }
}
