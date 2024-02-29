#include <Wire.h>
#include <PN532_I2C.h>
#include <PN532.h>

#define I2C_SDA 33  // Defina o pino SDA
#define I2C_SCL 32  // Defina o pino SCL

PN532_I2C pn532_i2c(Wire);
PN532 nfc(pn532_i2c);

void setup(void) {
  Serial.begin(115200);
  Wire.begin(I2C_SDA, I2C_SCL);  
  delay(1000); // Espera para a inicialização da serial

  // Inicializa o PN532
  nfc.begin();

  uint32_t versiondata = nfc.getFirmwareVersion();
  if (!versiondata) {
    Serial.print("Não foi possível encontrar o PN53x. Certifique-se de que está conectado corretamente.");
    while (1); // Trava o código
  }

  // Configuração do PN532 para aguardar tags por até 20 centímetros (esse valor pode variar dependendo do tipo de tag)
  nfc.SAMConfig();
  Serial.println("ESP32 PN532 Iniciado!");
}

void loop(void) {
  uint8_t success;
  uint8_t uid[] = { 0, 0, 0, 0, 0, 0, 0 };  // Buffer para armazenar o UID lido
  uint8_t uidLength; // Comprimento do UID (4 ou 7 bytes dependendo do tipo de tag)

  // Espera por uma tag RFID
  success = nfc.readPassiveTargetID(PN532_MIFARE_ISO14443A, uid, &uidLength);

  if (success) {
    Serial.println("Tag encontrada!");

    Serial.print("UID Length: ");Serial.print(uidLength, DEC);Serial.println(" bytes");
    Serial.print("UID Value: ");
    for (uint8_t i=0; i < uidLength; i++) {
      Serial.print(" 0x");Serial.print(uid[i], HEX);
    }
    Serial.println("");
    delay(1000); // Evita a leitura repetida da tag
  }
}
