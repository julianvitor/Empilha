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
    private lateinit var dbHelper: DatabaseHelper // Declare a variável dbHelper aqui
    private val handler = Handler(Looper.getMainLooper())
    private var mensagemRecebida: String? = null // Variável para armazenar a mensagem recebida
    private var uid: String? = null // variavel para armazenar o uid extraido
    private var doca: String? = null // variavel de controle da doca
    private var apelido: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        //exibe a tela
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)
        apelido = intent.getStringExtra("apelidoUsuario")
        //conectar no websocket
        conectarWebsocket()

        //instanciar o helper DB
        dbHelper = DatabaseHelper(this) // Inicialize dbHelper aqui

        // Configurar os botões
        val bay1Button: Button = findViewById(R.id.bay1)
        val bay2Button: Button = findViewById(R.id.bay2)


        //ação dos botoes
        bay1Button.setOnClickListener {
            enviarMensagem("ativar 1")//após enviar a mensagem o fluxo continua no onmensage do websocket quando for retirado da base
            doca = "1"
        }
        bay2Button.setOnClickListener {
            enviarMensagem("ativar 2")
            doca = "2"
        }
    }


    private fun conectarWebsocket() {
        val request = Request.Builder()
            .url("ws://192.168.1.150:81")
            .build()

        val client = OkHttpClient()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                super.onOpen(webSocket, response)
                //exibirMensagemRecebida("websocket aberto")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                super.onMessage(webSocket, text)
                mensagemRecebida = text // Armazena a mensagem recebida na variável de classe
                exibirMensagemRecebida(text)

                if (mensagemRecebida!!.startsWith("removido:")) {
                    extrairUid(mensagemRecebida!!)// agora o uid extraido ja está na memoria e é possivel passar para a função que grava no banco de dados
                    dbHelper?.registrarUso(apelido ?: "", uid ?: "", doca ?: "")
                    exibirToast("Sucesso: registrado")
                    finish()
                }
                else {/* exibirToast("Erro: Formato de mensagem inválido: $mensagemRecebida") */
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                super.onFailure(webSocket, t, response)
                /* exibirToast(t.message ?: "Erro: websocket fail") */
                reconectarWebsocket()
            }
        })
    }

    private fun extrairUid (mensagem: String){
        if (mensagem.startsWith("removido:")) {
            uid = mensagem.substringAfter(":")
        } else {
            // Se a mensagem não estiver no formato esperado, exibir um erro ou lidar de outra forma
            /*exibirToast("Erro: Formato de mensagem inválido: $mensagem")*/
        }
    }


    fun reconectarWebsocket() {
        // Conectar ao WebSocket após 5 segundos
        handler.postDelayed({
            conectarWebsocket()
        }, 5000) // 5000 milissegundos = 5 segundos
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

    private fun exibirMensagemRecebida(mensagem: String) {
        handler.post {
            Toast.makeText(
                this@DashboardActivity,
                "Mensagem recebida: $mensagem",
                Toast.LENGTH_SHORT
            ).show()
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
    }
}
