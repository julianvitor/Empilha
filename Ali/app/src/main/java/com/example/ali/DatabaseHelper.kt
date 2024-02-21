package com.example.ali

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, "registro.db", null, 1) {

    companion object {
        // Nome da tabela de usuários
        const val TABLE_USUARIOS = "usuarios"
        // Nome da tabela de registros de uso
        const val TABLE_USOS = "usos"
        // Colunas da tabela de usuários
        const val COLUMN_USUARIO_ID = "usuario_id"
        const val COLUMN_USUARIO_NOME = "nome"
        const val COLUMN_USUARIO_SENHA = "senha"
        const val COLUMN_USUARIO_APELIDO = "apelido" // Alteração para "apelido"
        // Colunas da tabela de registros de uso
        const val COLUMN_ID = "_id"
        const val COLUMN_USUARIO_REGISTRO_ID = "usuario_registro_id" // Chave estrangeira que se relaciona com a tabela de usuários
        const val COLUMN_RETIRADA = "retirada" // Coluna para armazenar a hora de retirada
        const val COLUMN_DEVOLUCAO = "devolucao" // Coluna para armazenar a hora de devolução
        const val COLUMN_DOCA = "doca" // Coluna para armazenar a doca equivalente
        const val COLUMN_UID = "uid" // Coluna para armazenar o valor de UID
        const val COLUMN_DESCRICAO = "descricao"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Verificar se as tabelas já existem
        val usuariosTableExists = tabelaExiste(db, TABLE_USUARIOS)
        val usosTableExists = tabelaExiste(db, TABLE_USOS)

        // Se as tabelas não existirem, criá-las
        if (!usuariosTableExists) {
            // Cria a tabela de usuários
            val SQL_CREATE_USUARIOS_TABLE = "CREATE TABLE $TABLE_USUARIOS (" +
                    "$COLUMN_USUARIO_ID INTEGER PRIMARY KEY," +
                    "$COLUMN_USUARIO_NOME TEXT," +
                    "$COLUMN_USUARIO_SENHA TEXT," +
                    "$COLUMN_USUARIO_APELIDO TEXT)" // Alteração para "apelido"
            db.execSQL(SQL_CREATE_USUARIOS_TABLE)
        }

        if (!usosTableExists) {
            // Cria a tabela de registros de uso
            val SQL_CREATE_USOS_TABLE = "CREATE TABLE $TABLE_USOS (" +
                    "$COLUMN_ID INTEGER PRIMARY KEY," +
                    "$COLUMN_USUARIO_REGISTRO_ID INTEGER," + // Chave estrangeira que se relaciona com a tabela de usuários
                    "$COLUMN_RETIRADA TEXT," + // Coluna para armazenar a hora de retirada
                    "$COLUMN_DEVOLUCAO TEXT," + // Coluna para armazenar a hora de devolução
                    "$COLUMN_DOCA TEXT," + // Coluna para armazenar a doca equivalente
                    "$COLUMN_UID TEXT," + // Coluna para armazenar o valor de UID
                    "$COLUMN_DESCRICAO TEXT," +
                    "$COLUMN_USUARIO_APELIDO TEXT," + // Adição da coluna "apelido"
                    "FOREIGN KEY($COLUMN_USUARIO_REGISTRO_ID) REFERENCES $TABLE_USUARIOS($COLUMN_USUARIO_ID))"
            db.execSQL(SQL_CREATE_USOS_TABLE)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        val SQL_DELETE_USUARIOS_TABLE = "DROP TABLE IF EXISTS $TABLE_USUARIOS"
        val SQL_DELETE_USOS_TABLE = "DROP TABLE IF EXISTS $TABLE_USOS"
        db.execSQL(SQL_DELETE_USUARIOS_TABLE)
        db.execSQL(SQL_DELETE_USOS_TABLE)
        onCreate(db)
    }

    fun verificarCredenciais(apelido: String, senha: String): Boolean {
        val db = readableDatabase
        val projection = arrayOf(COLUMN_USUARIO_ID)
        val selection = "$COLUMN_USUARIO_APELIDO = ? AND $COLUMN_USUARIO_SENHA = ?" // Alteração para "apelido"
        val selectionArgs = arrayOf(apelido, senha) // Alteração para "apelido"
        val cursor = db.query(TABLE_USUARIOS, projection, selection, selectionArgs, null, null, null)
        val existeRegistro = cursor.count > 0
        cursor.close()
        return existeRegistro
    }

    private fun tabelaExiste(db: SQLiteDatabase, tableName: String): Boolean {
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = ?",
            arrayOf(tableName)
        )
        cursor.use { c ->
            c.moveToFirst()
            val count = c.getInt(0)
            return count > 0
        }
    }
}
