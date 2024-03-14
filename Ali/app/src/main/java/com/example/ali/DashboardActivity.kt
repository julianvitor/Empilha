package com.example.ali

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*

class DashboardActivity : AppCompatActivity() {

    private lateinit var webSocket: WebSocket
    private lateinit var dbHelper: DatabaseHelper
    private val handler = Handler(Looper.getMainLooper())
    private var mensagemRecebida: String? = null
    private var uid: String? = null
    private var doca: String? = null
    private var apelido: String? = null
    private var countdownBotao: Int = 25
    private lateinit var countdownTextView: TextView
    private var countdownHandler: Handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)
        apelido = intent.getStringExtra("apelidoUsuario")
        conectarWebSocket()
        dbHelper = DatabaseHelper(this)

        val bay1Button: Button = findViewById(R.id.bay1)
        val bay2Button: Button = findViewById(R.id.bay2)
        countdownTextView = findViewById(R.id.countdownTextView)

        bay1Button.setOnClickListener {
            countdownTextView.visibility = View.VISIBLE
            iniciarContador(countdownBotao)
            enviarMensagem("ativar 1")
            doca = "1"
        }
        bay2Button.setOnClickListener {
            countdownTextView.visibility = View.VISIBLE
            iniciarContador(countdownBotao)
            enviarMensagem("ativar 2")
            doca = "2"
        }
    }

    private fun iniciarContador(countdown: Int) {
        var currentCountdown = countdown
        countdownHandler.removeCallbacksAndMessages(null)
        countdownHandler.postDelayed(object : Runnable {
            override fun run() {
                currentCountdown--
                countdownTextView.text = "Tempo restante: $currentCountdown segundos"
                if (currentCountdown == 0) {
                    finish()
                    return
                }
                countdownHandler.postDelayed(this, 1000)
            }
        }, 1000)
    }

    private fun conectarWebSocket() {
        val request = Request.Builder()
            .url("ws://192.168.15.150:81")
            .build()

        val client = OkHttpClient()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                super.onOpen(webSocket, response)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                super.onMessage(webSocket, text)
                mensagemRecebida = text
                exibirMensagemRecebida(text)

                if (mensagemRecebida!!.startsWith("removido:")) {
                    extrairUid(mensagemRecebida!!)
                    dbHelper?.registrarUso(apelido ?: "", uid ?: "", doca ?: "")
                    exibirToast("Sucesso: registrado")
                    finish()
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                super.onFailure(webSocket, t, response)
                reconectarWebSocket()
            }
        })
    }

    private fun extrairUid(mensagem: String) {
        if (mensagem.startsWith("removido:")) {
            uid = mensagem.substringAfter(":")
        }
    }

    private fun reconectarWebSocket() {
        handler.postDelayed({
            conectarWebSocket()
        }, 5000)
    }

    private fun enviarMensagem(mensagem: String) {
        webSocket.send(mensagem)
        exibirMensagemEnviada(mensagem)
    }

    private fun exibirMensagemEnviada(mensagem: String) {
        handler.post {
            Toast.makeText(this@DashboardActivity, "Mensagem enviada: $mensagem", Toast.LENGTH_SHORT).show()
        }
    }

    private fun exibirMensagemRecebida(mensagem: String) {
        handler.post {
            Toast.makeText(this@DashboardActivity, "Mensagem recebida: $mensagem", Toast.LENGTH_SHORT).show()
        }
    }

    private fun exibirToast(erro: String) {
        handler.post {
            Toast.makeText(this@DashboardActivity, "$erro", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        webSocket.close(1000, "Activity fechada")
        countdownHandler.removeCallbacksAndMessages(null)
    }
}
