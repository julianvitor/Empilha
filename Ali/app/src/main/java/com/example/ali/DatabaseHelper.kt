
package com.example.ali

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.content.ContentValues

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, "registro.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        val SQL_CREATE_ENTRIES = "CREATE TABLE registros (" +
                "_id INTEGER PRIMARY KEY," +
                "nome TEXT," +
                "usuario TEXT," +
                "senha TEXT)"

        db.execSQL(SQL_CREATE_ENTRIES)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS registros"
        db.execSQL(SQL_DELETE_ENTRIES)
        onCreate(db)
    }

    fun verificarCredenciais(usuario: String, senha: String): Boolean {
        val db = readableDatabase
        val projection = arrayOf("_id")
        val selection = "usuario = ? AND senha = ?"
        val selectionArgs = arrayOf(usuario, senha)
        val cursor = db.query("registros", projection, selection, selectionArgs, null, null, null)
        val existeRegistro = cursor.count > 0
        cursor.close()
        return existeRegistro
    }
}
