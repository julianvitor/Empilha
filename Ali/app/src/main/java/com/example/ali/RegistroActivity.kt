
package com.example.ali

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class RegistroActivity : AppCompatActivity() {

    private lateinit var editTextNome: EditText
    private lateinit var editTextUsuario: EditText
    private lateinit var editTextSenha: EditText
    private lateinit var buttonRegistrar: Button
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)

        // Inicializar as views
        editTextNome = findViewById(R.id.editTextNome)
        editTextUsuario = findViewById(R.id.editTextUsuario)
        editTextSenha = findViewById(R.id.editTextSenha)
        buttonRegistrar = findViewById(R.id.buttonRegistrar)

        // Inicializar o DBHelper
        dbHelper = DatabaseHelper(this)

        // Configurar OnClickListener para o botão Registrar
        buttonRegistrar.setOnClickListener {
            registrar()
        }
    }

    private fun registrar() {
        val nome = editTextNome.text.toString().trim()
        val apelido = editTextUsuario.text.toString().trim()
        val senha = editTextSenha.text.toString().trim()

        // Validar os campos
        if (nome.isEmpty() || apelido.isEmpty() || senha.isEmpty()) {
            Toast.makeText(this, "Por favor, preencha todos os campos", Toast.LENGTH_SHORT).show()
            return
        }

        // Obter o banco de dados em modo de escrita
        val db: SQLiteDatabase = dbHelper.writableDatabase

        // Criar um ContentValues para inserir os dados no banco de dados
        val values = ContentValues().apply {
            put("nome", nome)
            put("apelido", apelido)
            put("senha", senha)
        }

        try {
            // Inserir os dados na tabela
            val newRowId = db.insertOrThrow("usuarios", null, values)

            // Verificar se a inserção foi bem-sucedida
            if (newRowId != -1L) {
                Toast.makeText(this, "Registro bem-sucedido!", Toast.LENGTH_SHORT).show()
                // Limpar os campos após o registro bem-sucedido
                editTextNome.text.clear()
                editTextUsuario.text.clear()
                editTextSenha.text.clear()
            } else {
                Toast.makeText(this, "Erro ao registrar. Tente novamente.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            // Exibir mensagem de erro personalizada com base na exceção
            Toast.makeText(this, "Erro ao registrar: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
