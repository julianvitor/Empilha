package com.example.ali

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.*

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, "registro.db", null, 2) {

    private val appContext: Context = context.applicationContext

    companion object {
        // Definição de constantes para nome de tabelas e colunas
        const val TABLE_USUARIOS = "usuarios"
        const val TABLE_USOS = "usos"
        const val COLUMN_USUARIO_ID = "usuario_id"
        const val COLUMN_USUARIO_NOME = "nome"
        const val COLUMN_USUARIO_SENHA = "senha"
        const val COLUMN_USUARIO_APELIDO = "apelido"
        const val COLUMN_ID = "_id"
        const val COLUMN_RETIRADA = "retirada"
        const val COLUMN_UID = "uid"
        const val COLUMN_DOCA = "doca"
        const val COLUMN_DEVOLUCAO = "devolucao"

        @SuppressLint("Range")
        private fun getNomeUsuarioPorUID(databaseHelper: DatabaseHelper, uid: String): String? {
            val db = databaseHelper.readableDatabase
    
            // Consulta para obter o nome do usuário associado ao UID usando JOIN entre as tabelas "usuarios" e "usos"
            val query = "SELECT $COLUMN_USUARIO_NOME " +
                    "FROM $TABLE_USUARIOS AS u " +
                    "INNER JOIN $TABLE_USOS AS s ON u.$COLUMN_USUARIO_APELIDO = s.$COLUMN_USUARIO_APELIDO " +
                    "WHERE s.$COLUMN_UID = ?"
    
            val cursor = db.rawQuery(query, arrayOf(uid))
    
            var nomeUsuario: String? = null
    
            if (cursor.moveToFirst()) {
                nomeUsuario = cursor.getString(cursor.getColumnIndex(COLUMN_USUARIO_NOME))
            }
    
            cursor.close()
            db.close()
    
            return nomeUsuario ?: "Nenhum usuário encontrado" // Retorna uma string padrão se nomeUsuario for null
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        val SQL_CREATE_USUARIOS_TABLE = "CREATE TABLE $TABLE_USUARIOS (" +
                "$COLUMN_USUARIO_ID INTEGER PRIMARY KEY," +
                "$COLUMN_USUARIO_NOME TEXT," +
                "$COLUMN_USUARIO_SENHA TEXT," +
                "$COLUMN_USUARIO_APELIDO TEXT)"
        db.execSQL(SQL_CREATE_USUARIOS_TABLE)

        val SQL_CREATE_USOS_TABLE = "CREATE TABLE $TABLE_USOS (" +
                "$COLUMN_ID INTEGER PRIMARY KEY," +
                "$COLUMN_USUARIO_APELIDO TEXT," +
                "$COLUMN_RETIRADA TEXT," +
                "$COLUMN_DEVOLUCAO TEXT," +
                "$COLUMN_DOCA TEXT," +
                "$COLUMN_UID TEXT)"

        db.execSQL(SQL_CREATE_USOS_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USUARIOS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USOS")
        onCreate(db)
    }

    fun registrarDevolucao(uid: String) {
        val db = writableDatabase

        // Consultar o nome associado ao UID fornecido
        val nomeUsuario: String? = Companion.getNomeUsuarioPorUID(this, uid)

        // Se o nome do usuário foi encontrado, registre a devolução
        if (nomeUsuario != null) {
            // Obter a data atual
            val dataHoraAtual = getDataHoraAtual()

            // Atualizar o UID para zero e a coluna devolucao com a data atual
            val values = ContentValues().apply {
                put(COLUMN_UID, "0") // Definir UID como zero
                put(COLUMN_DEVOLUCAO, dataHoraAtual) // Registrar a hora da devolução
            }

            // Executar a atualização no banco de dados
            db.update(
                TABLE_USOS,
                values,
                "$COLUMN_UID = ?",
                arrayOf(uid)
            )
        }

        db.close()
    }


    fun registrarUso(apelido: String, uid: String, doca: String) {
        val db = writableDatabase
        val dataHoraAtual = getDataHoraAtual()

        // Verificar se já existe um registro para o usuário
        val cursor = db.query(
            TABLE_USOS,
            null,
            "$COLUMN_USUARIO_APELIDO = ?",
            arrayOf(apelido),
            null,
            null,
            null
        )

        if (cursor.count > 0) {
            // Já existe um registro para o usuário, então atualize-o
            val values = ContentValues().apply {
                put(COLUMN_RETIRADA, dataHoraAtual)
                put(COLUMN_UID, uid)
                put(COLUMN_DOCA, doca)
            }

            db.update(
                TABLE_USOS,
                values,
                "$COLUMN_USUARIO_APELIDO = ?",
                arrayOf(apelido)
            )

        } else {
            // Não existe um registro para o usuário, então insira um novo
            val values = ContentValues().apply {
                put(COLUMN_USUARIO_APELIDO, apelido)
                put(COLUMN_RETIRADA, dataHoraAtual)
                put(COLUMN_UID, uid)
                put(COLUMN_DOCA, doca)
            }

            db.insert(TABLE_USOS, null, values)
        }

        cursor.close()
        db.close()
    }

    fun verificarCredenciais(apelido: String, senha: String): Boolean {
        val db = readableDatabase
        val projection = arrayOf(COLUMN_USUARIO_ID)
        val selection = "$COLUMN_USUARIO_APELIDO = ? AND $COLUMN_USUARIO_SENHA = ?"
        val selectionArgs = arrayOf(apelido, senha)
        val cursor = db.query(TABLE_USUARIOS, projection, selection, selectionArgs, null, null, null)
        val existeRegistro = cursor.count > 0
        cursor.close()
        return existeRegistro
    }

    private fun getDataHoraAtual(): String {
        val calendar = Calendar.getInstance()
        return "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.MONTH) + 1}-${calendar.get(Calendar.DAY_OF_MONTH)} " +
                "${calendar.get(Calendar.HOUR_OF_DAY)}:${calendar.get(Calendar.MINUTE)}:${calendar.get(Calendar.SECOND)}"
    }
}
