package com.example.ali

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.*

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, "registro.db", null, 2) {

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
        const val COLUMN_DOCA = "doca"
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
                "$COLUMN_DOCA TEXT)"
        db.execSQL(SQL_CREATE_USOS_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USUARIOS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USOS")
        onCreate(db)
    }

    fun registrarUso(apelido: String, doca: String) {
        val db = writableDatabase
        val calendar = Calendar.getInstance()
        val dataHoraAtual = "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.MONTH) + 1}-${calendar.get(Calendar.DAY_OF_MONTH)} " +
                "${calendar.get(Calendar.HOUR_OF_DAY)}:${calendar.get(Calendar.MINUTE)}:${calendar.get(Calendar.SECOND)}"
        val values = ContentValues().apply {
            put(COLUMN_USUARIO_APELIDO, apelido)
            put(COLUMN_RETIRADA, dataHoraAtual)
            put(COLUMN_DOCA, doca)
        }

        try {
            val newRowId = db.insertOrThrow(TABLE_USOS, null, values)
            if (newRowId != -1L) {
                // Registro bem-sucedido
            } else {
                // Erro ao registrar
            }
        } catch (e: Exception) {
            // Tratar erro ao registrar
        } finally {
            db.close()
        }
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
}
