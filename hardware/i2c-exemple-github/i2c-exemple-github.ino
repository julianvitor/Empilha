// i used Arduino IDE Menu => Tools => Board => 'ESP32 Dev Module'
// powerup WT32-ETH01 with IO0 pulled to ground, remove after programming and powercycle
// used USB FTDI programmer TX->RX0, RX->TX0, 3.3Volt and ground
//tested works with wt32-eth01
//dhcp webserver
//webpage shows button when pressed random data is displayed
// mcu to ajax?     String message = "Random data: ";
//                  message += String(random(10000)); // Get random number
// next i2c data is printed to serial monitor for now
// i2c BME280 temp humid milibar sensor
// i2c DS3231 RTC realtimeclock
// I2C_SCL 32    // WT32-ETH01 CFG    = Gpio 32      non standard i2c adress 
// I2C_SDA 33    // WT32-ETH01 485_EN = Gpio 33      non standard i2c adress
// https://github.com/ldijkman/WT32-ETH01-LAN-8720-RJ45-/blob/main/WT32-ETH01-i2c-BME280-DS3231.ino

/*
    Copyright statement: Part of This article is the original article of the CSDN blogger "Naisu_kun",
    which follows the CC 4.0 BY-SA copyright agreement.
    For reprinting, please attach the original source link and this statement.

    Original link：https://blog.csdn.net/Naisu_kun/article/details/88572129
    - Explanation of the code and other parameters: see the link above.
    - Note: Original language: Chinese (You can use the Google translator to read English, or another language)
    
    Part of This article is added by luberth dijkman
    
   Copyright 2021 Dirk Luberth Dijkman Bangert 30 1619GJ Andijk The Netherlands

  GNU General Public License,
  which basically means that you may freely copy, change, and distribute it,
  but you may not impose any restrictions on further distribution,
  and you must make the source code available.

  All above must be included in any redistribution
*/
#include <ETH.h> // quote to use ETH
#include <WiFi.h>
#include <PN532_I2C.h>
#include <PN532.h>
#include <WebServer.h> // Introduce corresponding libraries

#include <Wire.h>

#define I2C_SCL 32    // WT32-ETH01 CFG    = Gpio 32      non standard i2c adress 
#define I2C_SDA 33    // WT32-ETH01 485_EN = Gpio 33      non standard i2c adress
 

#define ETH_ADDR        1
#define ETH_POWER_PIN   16//-1 //16 // Do not use it, it can cause conflict during the software reset.
#define ETH_POWER_PIN_ALTERNATIVE 16 //17
#define ETH_MDC_PIN    23
#define ETH_MDIO_PIN   18
#define ETH_TYPE       ETH_PHY_LAN8720
#define ETH_CLK_MODE    ETH_CLOCK_GPIO17_OUT // ETH_CLOCK_GPIO0_IN

IPAddress local_ip(192, 168, 1, 150);
IPAddress gateway(192, 168, 1, 1);
IPAddress subnet(255, 255, 255, 0);
IPAddress dns1(8, 8, 8, 8);
IPAddress dns2 = (uint32_t)0x00000000;

PN532_I2C pn532_i2c(Wire);
PN532 nfc(pn532_i2c);

// Web page
String myhtmlPage =
  String("") + "\r\n" +
  "<html>" + "\r\n" +
  "<head>" + "\r\n" +
  "    <title>ESP32 WebServer Test</title>" + "\r\n" +
  "    <script>" + "\r\n" +
  "        function getData() {" + "\r\n" +
  "            var xmlhttp;" + "\r\n" +
  "            if (window.XMLHttpRequest) {" + "\r\n" +
  "                xmlhttp = new XMLHttpRequest();" + "\r\n" +
  "            }" + "\r\n" +
  "            else {" + "\r\n" +
  "                xmlhttp = new ActiveXObject(\"Microsoft.XMLHTTP\");" + "\r\n" +
  "            }" + "\r\n" +
  "            xmlhttp.onreadystatechange = function() {" + "\r\n" +
  "                if (this.readyState == 4 && this.status == 200) {" + "\r\n" +
  "                    document.getElementById(\"txtRandomData\").innerHTML = this.responseText;" + "\r\n" +
  "                }" + "\r\n" +
  "            };" + "\r\n" +
  "            xmlhttp.open(\"GET\", \"getRandomData\", true); " + "\r\n" +
  "            xmlhttp.send();" + "\r\n" +
  "        }" + "\r\n" +
  "    </script>" + "\r\n" +
  "</head>" + "\r\n" +
  "<body>" + "\r\n" +
  "    <div id=\"txtRandomData\">Unkwon</div>" + "\r\n" +
  "    <input type=\"button\" value=\"random\" onclick=\"getData()\">" + "\r\n" +
  "</body>" + "\r\n" +
  "</html>";

WebServer server(80); // Declare the WebServer object

void handleRoot() // Callback
{
  server.send(200, "text/html", myhtmlPage); //!!! Note that returning to the web page requires "text / html" !!!
}

void handleAjax() // Callback
{
  String message = "Random data: ";
  message += String(random(10000)); // Get random number
  server.send(200, "text/plain", message); // Send message back to page
}

static bool eth_connected = false;

void WiFiEvent(WiFiEvent_t event)
{
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

void setup() {
  pinMode(ETH_POWER_PIN_ALTERNATIVE, OUTPUT);
  digitalWrite(ETH_POWER_PIN_ALTERNATIVE, HIGH);
  Serial.begin(115200);
  delay(2500);
  Serial.println("hello");

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

  ETH.begin(ETH_ADDR, ETH_POWER_PIN, ETH_MDC_PIN, ETH_MDIO_PIN, ETH_TYPE, ETH_CLK_MODE); // Enable ETH

  //ETH.config(local_ip, gateway, subnet, dns1, dns2); // Static IP, leave without this line to get IP via DHCP

  while (!((uint32_t)ETH.localIP())) {}; // Waiting for IP (leave this line group to get IP via DHCP)

  server.on("/", handleRoot);                        // Register link and callback function
  server.on("/getRandomData", HTTP_GET, handleAjax); // Request and callback function of the get method sent by ajax in the registration web page

  server.begin(); // Start server
  Serial.println("Web server started");
}

void loop() {
  server.handleClient(); // Handling requests from clients
}

