package com.example.ali

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*

class DashboardActivity : AppCompatActivity() {

    private lateinit var webSocket: WebSocket
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        connectWebSocket()

        // Configurar OnClickListener para os botões
        val bay1Button: Button = findViewById(R.id.bay1)
        val bay2Button: Button = findViewById(R.id.bay2)
        val bay3Button: Button = findViewById(R.id.bay3)
        val bay4Button: Button = findViewById(R.id.bay4)



        bay1Button.setOnClickListener {
            sendMessage("ativar 1")
        }
        bay2Button.setOnClickListener {
            sendMessage("ativar 2")
        }
        bay3Button.setOnClickListener {
            sendMessage("ativar 3")
        }
        bay4Button.setOnClickListener {
            sendMessage("ativar 4")
        }
        // mais botoes
    }

    private fun connectWebSocket() {
        val request = Request.Builder()
            .url("ws://192.168.4.1:81")
            .build()

        val client = OkHttpClient()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                super.onOpen(webSocket, response)
            }
        })
    }

    private fun sendMessage(message: String) {
        // Enviar mensagem
        webSocket.send(message)
        showMessageSentToast(message)
    }

    private fun showMessageSentToast(message: String) {
        handler.post {
            Toast.makeText(this@DashboardActivity, "Mensagem enviada: $message", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        webSocket.close(1000, "Activity fechada")
    }
}
