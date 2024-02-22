package com.example.ali

import android.content.ContentValues
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import java.util.*


class DashboardActivity : AppCompatActivity() {

    private lateinit var webSocket: WebSocket
    private lateinit var dbHelper: DatabaseHelper // Declare a variável dbHelper aqui
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)
        conectarWebsocket()
        //instanciar o helper DB
        dbHelper = DatabaseHelper(this) // Inicialize dbHelper aqui

        // Configurar OnClickListener para os botões
        val bay1Button: Button = findViewById(R.id.bay1)
        val bay2Button: Button = findViewById(R.id.bay2)
        val bay3Button: Button = findViewById(R.id.bay3)
        val bay4Button: Button = findViewById(R.id.bay4)

        bay1Button.setOnClickListener {
            registrarUso("1") // Registrar o uso da doca 1
            enviarMensagem("ativar 1")
        }
        bay2Button.setOnClickListener {
            registrarUso("2") // Registrar o uso da doca 2
            enviarMensagem("ativar 2")
        }
        bay3Button.setOnClickListener {
            registrarUso("3") // Registrar o uso da doca 3
            enviarMensagem("ativar 3")
        }
        bay4Button.setOnClickListener {
            registrarUso("4") // Registrar o uso da doca 4
            enviarMensagem("ativar 4")
        }
        // mais botoes
    }

    private fun conectarWebsocket() {
        val request = Request.Builder()
            .url("ws://192.168.4.1:81")
            .build()

        val client = OkHttpClient()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                super.onOpen(webSocket, response)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                super.onMessage(webSocket, text)
                receberMensagem(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                super.onFailure(webSocket, t, response)
                exibirErro(t.message ?: "Erro desconhecido")
            }
        })
    }

    private fun registrarUso(doca: String) {
        val apelido = intent.getStringExtra("apelidoUsuario")
        val calendar = Calendar.getInstance()
        val dataHoraAtual = "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.MONTH) + 1}-${calendar.get(Calendar.DAY_OF_MONTH)} " +
                "${calendar.get(Calendar.HOUR_OF_DAY)}:${calendar.get(Calendar.MINUTE)}:${calendar.get(Calendar.SECOND)}"

        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_USUARIO_APELIDO, apelido)
            put(DatabaseHelper.COLUMN_RETIRADA, dataHoraAtual)
            put(DatabaseHelper.COLUMN_DOCA, doca)
        }

        try {
            val newRowId = db.insertOrThrow(DatabaseHelper.TABLE_USOS, null, values)
            if (newRowId != -1L) {
                Toast.makeText(this, "Registro bem-sucedido!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Erro ao registrar. Tente novamente.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Erro ao registrar: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun enviarMensagem(mensagem: String) {
        // Enviar mensagem
        webSocket.send(mensagem)
        exibirMensagemEnviada(mensagem)
    }

    private fun exibirMensagemEnviada(mensagem: String) {
        handler.post {
            Toast.makeText(this@DashboardActivity, "Mensagem enviada: $mensagem", Toast.LENGTH_SHORT).show()
        }
    }

    private fun receberMensagem(mensagem: String) {
        handler.post {
            Toast.makeText(this@DashboardActivity, "Mensagem recebida: $mensagem", Toast.LENGTH_SHORT).show()
        }
    }

    private fun exibirErro(erro: String) {
        handler.post {
            Toast.makeText(this@DashboardActivity, "Erro: $erro", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        webSocket.close(1000, "Activity fechada")
    }
}
