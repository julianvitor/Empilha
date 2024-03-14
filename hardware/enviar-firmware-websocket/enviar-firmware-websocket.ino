#include <Arduino.h>
#include <ETH.h>
#include <PN532_I2C.h>
#include <PN532.h>
#include <Wire.h>
#include <WebSocketsServer.h>

#define I2C_SCL 32    // WT32-ETH01 CFG    = Gpio 32      non standard i2c adress 
#define I2C_SDA 33    // WT32-ETH01 485_EN = Gpio 33      non standard i2c adress
 
#define ETH_ADDR        1
#define ETH_POWER_PIN   16//-1 //16 // Do not use it, it can cause conflict during the software reset.
#define ETH_POWER_PIN_ALTERNATIVE 16 //17
#define ETH_MDC_PIN    23
#define ETH_MDIO_PIN   18
#define ETH_TYPE       ETH_PHY_LAN8720
#define ETH_CLK_MODE    ETH_CLOCK_GPIO17_OUT // ETH_CLOCK_GPIO0_IN

PN532_I2C pn532_i2c(Wire);
PN532 nfc(pn532_i2c);

WebSocketsServer webSocket = WebSocketsServer(81); // WebSocket server on port 81

static bool eth_connected = false;

void WiFiEvent(WiFiEvent_t event) {
  switch (event) {
    case SYSTEM_EVENT_ETH_START:
      Serial.println("ETH Started");
      ETH.setHostname("esp32-ethernet"); // Set ETH hostname
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
      Serial.printf("WebSocket message received: %s\n", payload);
      // Check if the message is requesting the PN532 firmware
      if (strcmp((char*)payload, "get_firmware") == 0) {
        // Check if the PN532 is detected
        uint32_t versiondata = nfc.getFirmwareVersion();
        if (versiondata) {
          String firmware = "PN532 Firmware version: ";
          firmware += String(versiondata);
          // Send firmware version to the client
          webSocket.sendTXT(num, firmware);
        } else {
          // PN532 not detected, send error message to the client
          webSocket.sendTXT(num, "Error: PN532 not detected");
        }
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
  Serial.println("apos o begin serial");

    Wire.begin(I2C_SDA, I2C_SCL);                               // start i2c on non stndard i2c pins
  nfc.begin();
  Serial.println("depois do nfc.begin");

  uint32_t versiondata = nfc.getFirmwareVersion();
  if (!versiondata) {
    Serial.println("Não foi possível encontrar o PN53x. Certifique-se de que está conectado corretamente.");
  } else {
    // Configuração do PN532 para aguardar tags por até 20 centímetros (esse valor pode variar dependendo do tipo de tag)
    nfc.SAMConfig();
    Serial.println("ESP32 PN532 Iniciado!");
    Serial.println(versiondata);
  }


  WiFi.onEvent(WiFiEvent);
  ETH.begin(ETH_ADDR, ETH_POWER_PIN, ETH_MDC_PIN, ETH_MDIO_PIN, ETH_TYPE, ETH_CLK_MODE);
  webSocket.begin();
  webSocket.onEvent(webSocketEvent);
}

void loop() {
  webSocket.loop();
  // Your main loop code here
}
