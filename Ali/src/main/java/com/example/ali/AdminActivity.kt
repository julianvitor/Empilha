package com.example.ali
import android.annotation.SuppressLint
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class AdminActivity : AppCompatActivity() {

    private lateinit var handlerDB: DatabaseHelper // DatabaseHelper deve ser importado do seu pacote

    @SuppressLint("Range")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin) // Defina o layout XML para esta atividade

        // Inicialize o DatabaseHelper
        handlerDB = DatabaseHelper(this)

        // Encontre a TableLayout no layout XML
        val tableLayout: TableLayout = findViewById(R.id.table_layout)

        // Recupere os dados do banco de dados
        val cursor: Cursor = getDataFromDatabase()

        // Adicione os dados à tabela dinamicamente
        if (cursor.moveToFirst()) {
            do {
                val nome = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_USUARIO_NOME))
                val datasRetirada = getDatasRetirada(nome)

                // Crie uma nova linha para cada entrada de dados
                val row = TableRow(this)

                // Adicione o nome do usuário como um TextView à linha
                addTextViewToRow(row, nome)

                // Adicione as datas de retirada como um TextView à linha
                addTextViewToRow(row, datasRetirada)

                // Adicione a linha à TableLayout
                tableLayout.addView(row)

            } while (cursor.moveToNext())
        }
        cursor.close()
    }

    // Função para recuperar os dados do banco de dados
    private fun getDataFromDatabase(): Cursor {
        val db: SQLiteDatabase = handlerDB.readableDatabase

        // Consulta SQL para selecionar nome e datas de retirada
        val query = """
            SELECT ${DatabaseHelper.COLUMN_USUARIO_NOME}, GROUP_CONCAT(${DatabaseHelper.COLUMN_RETIRADA}) AS datas_retirada
            FROM ${DatabaseHelper.TABLE_USUARIOS} LEFT JOIN ${DatabaseHelper.TABLE_USOS}
            ON ${DatabaseHelper.TABLE_USUARIOS}.${DatabaseHelper.COLUMN_USUARIO_APELIDO} = ${DatabaseHelper.TABLE_USOS}.${DatabaseHelper.COLUMN_USUARIO_APELIDO}
            GROUP BY ${DatabaseHelper.COLUMN_USUARIO_NOME}
        """.trimIndent()

        return db.rawQuery(query, null)
    }

    // Função para recuperar as datas de retirada associadas a um nome de usuário
    private fun getDatasRetirada(nomeUsuario: String): String {
        val db: SQLiteDatabase = handlerDB.readableDatabase

        val query = """
            SELECT ${DatabaseHelper.COLUMN_RETIRADA}
            FROM ${DatabaseHelper.TABLE_USOS} 
            WHERE ${DatabaseHelper.COLUMN_USUARIO_APELIDO} = (
                SELECT ${DatabaseHelper.COLUMN_USUARIO_APELIDO} 
                FROM ${DatabaseHelper.TABLE_USUARIOS} 
                WHERE ${DatabaseHelper.COLUMN_USUARIO_NOME} = '$nomeUsuario'
            )
        """.trimIndent()

        val cursor = db.rawQuery(query, null)
        val datas = StringBuilder()

        if (cursor.moveToFirst()) {
            do {
                val dataRetirada = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_RETIRADA))
                datas.append("$dataRetirada\n")
            } while (cursor.moveToNext())
        }

        cursor.close()
        return datas.toString()
    }

    // Função para adicionar um TextView a uma TableRow
    private fun addTextViewToRow(row: TableRow, text: String) {
        val textView = TextView(this)
        textView.text = text
        textView.setPadding(8, 8, 8, 8)
        textView.layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
        row.addView(textView)
    }
}