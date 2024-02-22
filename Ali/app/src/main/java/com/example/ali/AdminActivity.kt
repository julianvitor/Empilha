package com.example.ali

import android.annotation.SuppressLint
import android.database.Cursor
import android.os.Bundle
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class AdminActivity : AppCompatActivity() {

    private lateinit var databaseHelper: DatabaseHelper

    @SuppressLint("Range")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        // Inicializa o DatabaseHelper
        databaseHelper = DatabaseHelper(this)

        // Obtém a referência para o TableLayout do layout
        val tableLayout: TableLayout = findViewById(R.id.table_layout)

        // Obtém um Cursor com os nomes dos usuários e suas informações de retirada
        val cursor: Cursor? = getUsuariosERetiradas()

        // Verifica se o cursor não é nulo e se possui dados
        if (cursor != null && cursor.moveToFirst()) {
            do {
                // Obtém o nome do usuário e sua informação de retirada do cursor
                val nome = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_USUARIO_NOME))
                val retirada = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_RETIRADA))

                // Cria uma nova linha na tabela
                val row = TableRow(this)

                // Adiciona textviews com o nome do usuário e sua informação de retirada à linha
                addTextViewToRow(row, nome)
                addTextViewToRow(row, retirada)

                // Adiciona a linha à tabela
                tableLayout.addView(row)
            } while (cursor.moveToNext())
        }

        // Fecha o cursor após o uso para liberar recursos
        cursor?.close()
    }

    // Função para obter um Cursor com os nomes dos usuários e suas informações de retirada
    private fun getUsuariosERetiradas(): Cursor? {
        val db = databaseHelper.readableDatabase

        // Consulta SQL para obter os nomes dos usuários e suas informações de retirada
        val query = "SELECT ${DatabaseHelper.TABLE_USUARIOS}.${DatabaseHelper.COLUMN_USUARIO_NOME}, " +
                "${DatabaseHelper.TABLE_USOS}.${DatabaseHelper.COLUMN_RETIRADA} " +
                "FROM ${DatabaseHelper.TABLE_USUARIOS} " +
                "LEFT JOIN ${DatabaseHelper.TABLE_USOS} " +
                "ON ${DatabaseHelper.TABLE_USUARIOS}.${DatabaseHelper.COLUMN_USUARIO_APELIDO} = " +
                "${DatabaseHelper.TABLE_USOS}.${DatabaseHelper.COLUMN_USUARIO_APELIDO}"

        // Executa a consulta e retorna o Cursor resultante
        return db.rawQuery(query, null)
    }

    // Função auxiliar para adicionar textview a uma linha da tabela
    private fun addTextViewToRow(row: TableRow, text: String) {
        val textView = TextView(this)
        textView.text = text
        textView.setPadding(8, 8, 8, 8)
        row.addView(textView)
    }
}
