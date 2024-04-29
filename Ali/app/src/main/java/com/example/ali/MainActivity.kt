package com.example.ali

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.os.Handler
import android.os.Looper
import okhttp3.*

class MainActivity : AppCompatActivity() {

    private lateinit var editTextUsername: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var buttonLogin: Button
    private lateinit var buttonRegister: Button
    private lateinit var dbHelper: DatabaseHelper
    private var mensagemRecebida: String? = null // Variável para armazenar a mensagem recebida
    private lateinit var webSocket: WebSocket
    private val handler = Handler(Looper.getMainLooper())
    private var uid: String? = null // variavel para armazenar o uid extraido

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar as views
        editTextUsername = findViewById(R.id.editTextUsername)
        editTextPassword = findViewById(R.id.editTextPassword)
        buttonLogin = findViewById(R.id.buttonLogin)
        buttonRegister = findViewById(R.id.buttonRegister)
        dbHelper = DatabaseHelper(this)

        // Configurar OnClickListener para o botão Login
        buttonLogin.setOnClickListener {
            val apelido = editTextUsername.text.toString()
            val senha = editTextPassword.text.toString()

            // Limpar os campos de usuário e senha
            editTextUsername.text.clear()
            editTextPassword.text.clear()

            // Verificar se o usuário e a senha estão vazios
            if (apelido.isNotEmpty() && senha.isNotEmpty()) {
                // Verificar se as credenciais são de administrador
                if (apelido == "admin" && senha == "admin") {
                    val intent = Intent(this, AdminActivity::class.java)
                    startActivity(intent)
                } else {
                    // Verificar as credenciais no banco de dados
                    val isValidCredentials = dbHelper.verificarCredenciais(apelido, senha)

                    if (isValidCredentials) {
                        // Passar o nome de usuário para a próxima atividade
                        val intent = Intent(this, DashboardActivity::class.java)
                        intent.putExtra("apelidoUsuario", apelido) // Passando o nome de usuário como extra
                        startActivity(intent)
                    } else {
                        // Se as credenciais forem inválidas, exibir uma mensagem de erro
                        Toast.makeText(this, "Usuário ou senha inválidos", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                // Se o usuário ou a senha estiverem vazios, exibir uma mensagem de erro
                Toast.makeText(this, "Por favor, preencha todos os campos", Toast.LENGTH_SHORT).show()
            }
        }

        // Configurar OnClickListener para o botão Registro
        buttonRegister.setOnClickListener {
            val intent = Intent(this, RegistroActivity::class.java)
            startActivity(intent)

            // Limpar os campos de usuário e senha
            editTextUsername.text.clear()
            editTextPassword.text.clear()
        }
    }

    override fun onResume() {
        super.onResume()
        conectarWebSocket()
    }

    override fun onPause() {
        super.onPause()
        desconectarWebSocket()
    }

    override fun onDestroy() {
        super.onDestroy()
        desconectarWebSocket()
    }

    private fun conectarWebSocket() {
        val request = Request.Builder()
            .url("ws://192.168.1.150:81") // Substitua pela URL do seu servidor WebSocket
            .build()

        val client = OkHttpClient()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                super.onOpen(webSocket, response)
                exibirMensagem("Conexão WebSocket aberta")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                super.onMessage(webSocket, text)
                mensagemRecebida = text // Armazena a mensagem recebida na variável de classe
                //exibirMensagemRecebida(text)

                if (mensagemRecebida!!.startsWith("inserido:")){
                    extrairUid(mensagemRecebida!!)
                    dbHelper?.registrarDevolucao( uid ?: "")
                    exibirToast("Sucesso: devolvido")
                } else if (mensagemRecebida!!.startsWith("removido:")) {
                    exibirToast("Erro: retirada não autorizada")
                } else {
                    // Se a mensagem não estiver no formato esperado, exibir um erro ou lidar de outra forma
                    //exibirToast("Erro: Formato de mensagem inválido: $mensagemRecebida")
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                super.onFailure(webSocket, t, response)
                exibirMensagem("Erro na conexão WebSocket: ${t.message ?: "Erro desconhecido"}")
                reconectarWebsocket()
            }
        })
    }

    private fun desconectarWebSocket() {
        try {
            webSocket.close(1000, "Activity em pausa ou destruída")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun extrairUid(mensagem: String) {
        uid = mensagem.substringAfter(":")
    }

    private fun exibirToast(erro: String) {
        handler.post {
            Toast.makeText(this@MainActivity, "$erro", Toast.LENGTH_SHORT).show()
        }
    }

    private fun exibirMensagem(mensagem: String) {
        handler.post {
            Toast.makeText(this@MainActivity, mensagem, Toast.LENGTH_SHORT).show()
        }
    }

    private fun exibirMensagemRecebida(mensagem: String) {
        handler.post {
            Toast.makeText(this@MainActivity, "Mensagem recebida: $mensagem", Toast.LENGTH_SHORT).show()
        }
    }

    fun reconectarWebsocket() {
        // Conectar ao WebSocket
        handler.postDelayed({
            conectarWebSocket()
        }, 5000) //2 segundos
    }
}
