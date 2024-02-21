package com.example.ali

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast

class MainActivity : AppCompatActivity() {

    private lateinit var editTextUsername: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var buttonLogin: Button
    private lateinit var buttonRegister: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar as views
        editTextUsername = findViewById(R.id.editTextUsername)
        editTextPassword = findViewById(R.id.editTextPassword)
        buttonLogin = findViewById(R.id.buttonLogin)
        buttonRegister = findViewById(R.id.buttonRegister)

        // Configurar OnClickListener para o botão Login
        buttonLogin.setOnClickListener {
            val usuario = editTextUsername.text.toString()
            val senha = editTextPassword.text.toString()

            // Verificar se o usuário e a senha estão vazios
            if (usuario.isNotEmpty() && senha.isNotEmpty()) {
                // Verificar se as credenciais são de administrador
                if(usuario == "admin" && senha == "admin"){
                    val intent = Intent(this, AdminActivity::class.java)
                    startActivity(intent)
                } else {
                    // Verificar as credenciais no banco de dados
                    val dbHelper = DatabaseHelper(this)
                    val isValidCredentials = dbHelper.verificarCredenciais(usuario, senha)

                    if (isValidCredentials) {
                        // Se as credenciais forem válidas, iniciar a atividade Dashboard
                        val intent = Intent(this, DashboardActivity::class.java)
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
        }
    }
}
