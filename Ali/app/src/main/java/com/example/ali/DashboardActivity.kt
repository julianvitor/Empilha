package com.example.ali

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Socket

class DashboardActivity : AppCompatActivity() {
    private var esp32Socket: Socket? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // Iniciar a comunicação com o ESP32
        iniciarComunicacaoComESP32()
    }

    private fun iniciarComunicacaoComESP32() {
        Thread {
            try {
                // Conectar ao ESP32 (substitua "192.168.4.1" e 80 pela configuração do seu ESP32)
                esp32Socket = Socket("192.168.4.1", 80)

                // Loop para ler mensagens constantes do ESP32
                while (true) {
                    // Ler os dados enviados pelo ESP32
                    val reader = BufferedReader(InputStreamReader(esp32Socket!!.getInputStream()))
                    val mensagemRecebida = reader.readLine()

                    // Exibir a mensagem recebida na tela
                    runOnUiThread {
                        exibirMensagem(mensagemRecebida)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Lidar com erros de conexão ou leitura
            }
        }.start()
    }

    // Função para exibir uma mensagem usando um Toast
    private fun exibirMensagem(mensagem: String?) {
        mensagem?.let {
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Fechar o socket quando a atividade for destruída para liberar recursos
        esp32Socket?.close()
    }
}
